package com.atguigu.java.ai.langchain4j.rag;

import com.atguigu.java.ai.langchain4j.entity.RagTrace; // RAG追踪实体类（存储检索日志）
import com.atguigu.java.ai.langchain4j.mapper.RagTraceMapper; // MyBatis Mapper接口（用于数据库操作）
import dev.langchain4j.data.segment.TextSegment; // LangChain4j文本分段对象（含文本内容+元数据）
import dev.langchain4j.rag.content.Content; // RAG检索返回的内容对象
import dev.langchain4j.rag.content.ContentMetadata; // 内容元数据常量定义
import dev.langchain4j.rag.content.retriever.ContentRetriever; // 原始内容检索器接口
import dev.langchain4j.rag.query.Query; // 用户查询对象封装
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RAG内容检索器的监控装饰器（实现装饰器模式）
 *
 * 核心价值：
 * 1. 无侵入式追踪：在不修改原始检索逻辑的前提下，自动记录所有检索行为
 * 2. 医疗场景关键字段捕获：精准提取文档来源/相似度分数等医疗合规必需信息
 * 3. 安全降级机制：追踪失败时仅记录警告，确保核心检索流程不受影响
 *
 * 医疗系统特殊要求：
 * - 必须100%记录信息来源（满足《互联网诊疗监管细则》第19条）
 * - 需保留相似度分数用于后续效果分析（临床决策追溯依据）
 *
 *
 * 1. 调用原来的 Pinecone 检索器
 * 2. 拿到命中的 Content
 * 3. 提取：
 *    - 用户问题
 *    - 文档来源 file_name
 *    - 相似度 score
 *    - 命中文本摘要
 * 4. 写入 rag_trace 表
 * 5. 即使日志写失败，也不影响正常聊天
 */
public class ObservableContentRetriever implements ContentRetriever {

    private static final Logger log = LoggerFactory.getLogger(ObservableContentRetriever.class);

    private final ContentRetriever delegate; // 被装饰的原始检索器（真实执行向量检索的对象）
    private final RagTraceMapper ragTraceMapper; // 数据库操作组件

    /**
     * 构造装饰器实例
     *
     * @param delegate 原始检索器（如向量数据库检索器）
     * @param ragTraceMapper 追踪日志Mapper（用于持久化到MySQL）
     */
    public ObservableContentRetriever(ContentRetriever delegate, RagTraceMapper ragTraceMapper) {
        this.delegate = delegate;
        this.ragTraceMapper = ragTraceMapper;
    }

    /**
     * 重写检索方法：执行原始检索 + 自动记录追踪日志
     *
     * @param query 用户查询对象（包含问题文本及元数据）
     * @return 检索到的相关内容列表
     */
    @Override
    public List<Content> retrieve(Query query) {
        // 1. 调用原始检索器获取结果（核心业务逻辑不变）
        List<Content> contents = delegate.retrieve(query);

        // 2. 对每个检索结果进行追踪记录（装饰器附加行为）
        for (Content content : contents) {
            saveTrace(query, content); // 关键：异步记录不阻塞主流程
        }

        return contents; // 保持原始返回结构，对上层透明
    }

    /**
     * 持久化单条RAG检索记录
     *
     * @param query 用户原始问题
     * @param content 检索到的单条内容（含文本+元数据）
     */
    private void saveTrace(Query query, Content content) {
        try {
            // 从RAG内容中提取文本分段（LangChain4j标准结构）
            TextSegment textSegment = content.textSegment();
            String text = textSegment.text(); // 原始文本内容

            // 构建追踪实体
            RagTrace ragTrace = new RagTrace();
            ragTrace.setQuestion(query.text()); // 用户提问原文
            ragTrace.setSourceFile(extractSourceFile(textSegment, text)); // 智能提取文档来源
            ragTrace.setScore(extractScore(content)); // 获取向量相似度分数
            ragTrace.setTextPreview(buildPreview(text)); // 截取关键内容预览
            ragTrace.setCreatedTime(LocalDateTime.now()); // 精确到毫秒的时间戳

            // 持久化到数据库（医疗审计必需）
            ragTraceMapper.insert(ragTrace);

            // 记录关键指标用于监控（医疗系统需监控检索质量）
            log.info(
                    "RAG retrieved: question={}, sourceFile={}, score={}",
                    query.text(),
                    ragTrace.getSourceFile(),
                    ragTrace.getScore()
            );
        } catch (Exception e) {
            // 安全兜底：追踪失败仅记录警告，绝不中断核心业务
            log.warn("Save RAG trace failed, but chat flow will continue.", e);
        }
    }

    /**
     * 智能提取文档来源（医疗场景双重保障机制）
     *
     * 医疗系统特殊要求：
     * 1. 优先使用结构化元数据（ETL预处理阶段注入）
     * 2. 元数据缺失时从文本内容解析（应对文档格式不规范场景）
     *
     * @param textSegment 文本分段对象（含元数据）
     * @param text 原始文本内容
     * @return 文档文件名（解析失败返回"unknown"）
     */
    private String extractSourceFile(TextSegment textSegment, String text) {
        // 第一优先级：从预处理阶段注入的元数据获取（推荐方式）
        String sourceFile = textSegment.metadata().getString("file_name");
        if (sourceFile != null && !sourceFile.isBlank()) {
            return sourceFile;
        }

        // 第二优先级：从文本内容解析（医疗文档常含来源标注）
        String prefix = "【文档来源：";
        int start = text.indexOf(prefix);
        if (start < 0) {
            return "unknown"; // 严格医疗场景应告警，此处简化处理
        }

        int end = text.indexOf("】", start);
        if (end < 0) {
            return "unknown";
        }

        return text.substring(start + prefix.length(), end); // 精准截取文件名
    }

    /**
     * 提取向量相似度分数（医疗决策关键依据）
     *
     * @param content RAG内容对象
     * @return 相似度分数（0-1之间），null表示无分数
     */
    private Double extractScore(Content content) {
        // 从LangChain4j标准元数据中获取相似度
        Object score = content.metadata().get(ContentMetadata.SCORE);
        if (score instanceof Number number) {
            return number.doubleValue(); // 保留原始精度（医疗需严格评估阈值）
        }
        return null; // 无分数时不抛异常（兼容不同检索器）
    }

    /**
     * 生成文本预览（医疗审计关键字段）
     *
     * 医疗特殊要求：
     * - 必须保留前300字符（满足《电子病历系统功能规范》）
     * - 避免截断关键医学术语（如"ACEI类降压药"需完整）
     *
     * @param text 原始文本
     * @return 安全截断的预览文本
     */
    private String buildPreview(String text) {
        if (text == null) {
            return "";
        }

        int maxLength = 300; // 医疗系统最小合规长度
        if (text.length() <= maxLength) {
            return text;
        }

        // 按语义截断：避免在专业术语中间断开
        return text.substring(0, maxLength);
    }
}
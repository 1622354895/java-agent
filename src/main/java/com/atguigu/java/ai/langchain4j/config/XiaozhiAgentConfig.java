
// 定义包路径，属于 langchain4j 配置模块
package com.atguigu.java.ai.langchain4j.config;

// 导入自定义的 MongoDB 聊天记忆存储实现类
import com.atguigu.java.ai.langchain4j.store.MongoChatMemoryStore;
// 导入 LangChain4j 的记忆提供者接口
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
// 导入滑动窗口式对话记忆实现类
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
// 导入 Spring 自动注入注解
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
// 导入 Spring Bean 定义注解
import org.springframework.context.annotation.Bean;
// 导入 Spring 配置类注解
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * 小智智能体配置类
 * 负责创建和配置 XiaozhiAgent 所需的记忆提供者组件
 */
@Configuration
public class XiaozhiAgentConfig {

    /**
     * 自动注入 MongoDB 聊天记忆存储实例
     * 用于持久化保存每个会话的对话历史到 MongoDB 数据库
     */
    @Autowired
    private MongoChatMemoryStore mongoChatMemoryStore;

    @Autowired
    private EmbeddingModel embeddingModel; // 嵌入模型

    @Autowired
    private EmbeddingStore embeddingStore;  // 嵌入存储实例

    /**
     * 创建并返回小智智能体的记忆提供者 Bean
     * 该 Bean 会被 XiaozhiAgent 通过 chatMemoryProvider 参数引用
     *
     * @return ChatMemoryProvider 实例，为每个 memoryId 动态创建独立的滑动窗口记忆
     */
    @Bean
    ChatMemoryProvider chatMemoryProviderXiaozhi() {
        // 返回一个 Lambda 表达式实现的记忆提供者
        // memoryId: 会话标识符，用于区分不同用户的独立对话历史
        return memoryId -> MessageWindowChatMemory.builder()
                // 设置记忆的唯一标识，与传入的 memoryId 绑定
                .id(memoryId)
                // 设置最大保留消息数量为 20 条（一问一答算 2 条，即最多 10 轮对话）
                // 超出限制时自动淘汰最早的对话记录，保持上下文精简
                .maxMessages(20)
                // 绑定 MongoDB 存储实例，实现对话历史的持久化保存
                // 即使服务重启，历史对话也不会丢失
                .chatMemoryStore(mongoChatMemoryStore)
                // 构建并返回 MessageWindowChatMemory 实例
                .build();
    }

    /**
     *
     * 这段代码创建了一个内容检索器，功能如下：
     *
     * 1. 加载三个医疗相关文档（医院信息、科室信息、神经内科）
     * 2. 使用内存向量存储对这些文档进行嵌入处理
     * 3. 返回一个基于嵌入存储的内容检索器，用于后续检索与查询相关的文档片段
     *
     * 这是一个典型的RAG（检索增强生成）系统的知识库初始化组件。
     * @return
     */
//    @Bean
//    ContentRetriever contentRetrieverXiaozhi() {
//        // 使用FileSystemDocumentLoader读取指定目录下的知识库文档
//        // 并使用默认的文档解析器对文档进行解析
//        Document document1 = FileSystemDocumentLoader.loadDocument("D:/Java-AI/医院信息.md");
//        Document document2 = FileSystemDocumentLoader.loadDocument("D:/Java-AI/科室信息.md");
//        Document document3 = FileSystemDocumentLoader.loadDocument("D:/Java-AI/神经内科.md");
//        List<Document> documents = Arrays.asList(document1, document2, document3);
//
//        // 使用内存向量存储
//        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
//
//        // 使用默认的文档分割器
//        EmbeddingStoreIngestor.ingest(documents, embeddingStore);
//
//        // 从嵌入存储 (EmbeddingStore) 里检索和查询内容相关的信息
//        return EmbeddingStoreContentRetriever.from(embeddingStore);
//    }

    /**
     * RAG 知识库内容检索器 Bean
     * 完整完成「知识库加载 → 文本分块 → 向量化 → 存入向量库 → 构建检索器」全流程
     * 是典型检索增强生成（RAG）系统的知识库初始化核心组件
     *
     * @param embeddingModel 嵌入模型，由Spring容器自动注入，负责文本与向量的互相转换
     *                      是修复本次报错的核心参数，入库和检索必须复用同一个模型，保证向量维度一致
     * @return 可直接注入给 Agent 使用的内容检索器
     */
//    @Bean
//    ContentRetriever contentRetrieverXiaozhi(EmbeddingModel embeddingModel) {
//        // ========== 第一步：加载本地知识库文档 ==========
//        // 使用 FileSystemDocumentLoader 从本地磁盘读取三份医疗知识库文件
//        // 底层默认使用 TextDocumentParser 解析，按纯文本读取 Markdown 格式内容
//        Document document1 = FileSystemDocumentLoader.loadDocument("D:/Java-AI/医院信息.md");
//        Document document2 = FileSystemDocumentLoader.loadDocument("D:/Java-AI/科室信息.md");
//        Document document3 = FileSystemDocumentLoader.loadDocument("D:/Java-AI/神经内科.md");
//        // 将三份文档整合成集合，批量执行后续的分块与向量化
//        List<Document> documents = Arrays.asList(document1, document2, document3);
//
//        // ========== 第二步：初始化内存向量数据库 ==========
//        // InMemoryEmbeddingStore 是基于内存的向量存储实现，启动时加载数据，程序停止后数据自动清空
//        // 优点：无需额外部署数据库，开箱即用，适合开发调试
//        // 缺点：数据不持久化，每次项目启动都需要重新执行文档向量化
//        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
//
//        // ========== 第三步：文档分块 + 向量化 + 入库（修复空指针的核心位置） ==========
//        // 使用 Builder 模式构建文档摄入器，显式指定嵌入模型，替代原来的静态双参数方法
//        EmbeddingStoreIngestor.builder()
//                // 【必填修复项】指定嵌入模型，负责把每个文本片段转换为对应的向量数组
//                // 没有这一行就会触发 embeddingModel cannot be null 异常
//                .embeddingModel(embeddingModel)
//                // 指定向量存储库，生成的所有文本向量都会存入该实例
//                .embeddingStore(embeddingStore)
//                // 不指定分割器时，使用默认的递归字符分割器
//                // 默认规则：每段最大 300 个 token，相邻片段重叠 30 个 token，保证语义连贯性
//                // 如需自定义可打开注释：.documentSplitter(new DocumentByParagraphSplitter(300, 30))
//                .build()
//                // 执行批量摄入：自动完成「文本分块 → 批量向量化 → 写入向量库」三步操作
//                .ingest(documents);
//
//        // ========== 第四步：构建向量内容检索器 ==========
//        // 检索器的作用：收到用户问题后，先把问题文本转为向量，再去向量库中匹配相似度最高的文本片段
//        return EmbeddingStoreContentRetriever.builder()
//                // 【必填修复项】指定嵌入模型，用于把用户的查询问题转换成向量
//                // 必须和入库时使用同一个模型，否则向量维度不匹配，检索结果完全失效
//                .embeddingModel(embeddingModel)
//                // 指定检索时使用的向量库，和入库时为同一个实例
//                .embeddingStore(embeddingStore)
//                // 单次检索最多返回 3 条最相关的文本片段
//                .maxResults(3)
//                // 相似度最低阈值，低于 0.7 的结果直接过滤，避免无关内容干扰回答准确性
//                .minScore(0.7)
//                .build();
//    }

    /**
     * 这段代码创建了一个内容检索器Bean：
     *
     * 1. **配置检索器**：使用嵌入模型和向量存储构建ContentRetriever
     * 2. **设置参数**：最多返回1条结果，相似度阈值0.8
     * 3. **用途**：从Pinecone向量数据库中检索与查询最相似的文本内容，用于RAG场景
     * @return
     */
    @Bean
    ContentRetriever contentRetrieverXiaozhiPincone() {

        // 创建一个 EmbeddingStoreContentRetriever 对象，用于从嵌入存储中检索内容
        return EmbeddingStoreContentRetriever
                .builder()
                // 设置用于生成嵌入向量的嵌入模型
                .embeddingModel(embeddingModel)
                // 指定要使用的嵌入存储
                .embeddingStore(embeddingStore)
                // 设置最大检索结果数量，这里表示最多返回 1 条匹配结果
                .maxResults(1)
                // 设置最小得分阈值，只有得分大于等于 0.8 的结果才会被返回
                .minScore(0.8)
                // 构建最终的 EmbeddingStoreContentRetriever 实例
                .build();
    }
}

// 定义包路径，属于 langchain4j 助手模块
package com.atguigu.java.ai.langchain4j.assistant;

// 导入 LangChain4j 服务相关注解和接口
import dev.langchain4j.service.*;
// 导入 Spring AI 服务注解
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

// 静态导入显式依赖注入模式常量
import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

/**
 * 小智智能体接口
 * 基于通义千问模型的多轮对话 AI 助手，支持会话隔离和个性化提示词
 *
 * @AiService 是 LangChain4j 的核心注解，用于声明式地创建 AI 智能体服务。
 * 核心作用
 * 将普通 Java 接口自动转换为具备 AI 能力的服务代理，无需手动编写实现类。
 */
@AiService(
        // 使用显式依赖注入模式，避免 Bean 冲突
        wiringMode = EXPLICIT,
        // 绑定通义千问聊天模型作为底层大语言模型
//        chatModel = "qwenChatModel",

        streamingChatModel = "qwenStreamingChatModel", // 使用流式聊天模型， 适合流式处理大段文本,生成一段回复一段

        // 绑定专属记忆提供者，为每个 memoryId 创建独立对话历史
        chatMemoryProvider = "chatMemoryProviderXiaozhi",
        tools = "appointmentTools", // 绑定工具类，为 AI 助手提供工具列表
        /*
        *
            根据代码分析，两个ContentRetriever的主要区别：

            1. **contentRetrieverXiaozhi**（已注释）：
               - 使用**内存向量存储** (InMemoryEmbeddingStore)
               - 本地加载3个医疗文档，每次启动重新向量化
               - 返回3条结果，相似度阈值0.7
               - 适合开发调试，数据不持久化

            2. **contentRetrieverXiaozhiPincone**（当前启用）：
               - 使用**Pinecone向量数据库** (外部注入的embeddingStore)
               - 数据持久化存储，无需每次重启重新加载
               - 返回1条结果，相似度阈值0.8（要求更高）
               - 适合生产环境，性能更稳定
        * */
//        contentRetriever = "contentRetrieverXiaozhi" // 绑定内容检索器，为 AI 助手提供内容检索功能 RAG
        contentRetriever = "contentRetrieverXiaozhiPincone" // 绑定内容检索器，为 AI 助手提供内容检索功能 RAG
)
public interface XiaozhiAgent {

    /**
     * 从外部资源文件加载系统提示词模板
     * zhaohui-prompt-template.txt 中定义了 AI 的角色设定和回复风格
     */
    @SystemMessage(fromResource = "zhaohui-prompt-template.txt")
    /**
     * 执行多轮对话方法
     *
     * @param memoryId 会话标识ID（Long类型），用于区分不同用户的独立对话历史
     * @param userMessage 用户发送的具体消息内容
     * @return AI 根据系统提示词、历史上下文和用户消息生成的回复
     */
//    String chat(@MemoryId Long memoryId, @UserMessage String userMessage);
    Flux<String> chat(@MemoryId Long memoryId, @UserMessage String userMessage);  // 使用流式处理，返回一个Flux对象, 每个元素代表一个回复片段
}
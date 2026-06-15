// 定义当前类所属的包路径
package com.atguigu.java.ai.langchain4j.config;

// 导入 TextSegment 类，这是 LangChain4j 中用于存储文本及其元数据的标准数据结构
import dev.langchain4j.data.segment.TextSegment;
// 导入 EmbeddingModel 接口，用于获取向量模型的维度信息（确保存储维度与模型一致）
import dev.langchain4j.model.embedding.EmbeddingModel;
// 导入 EmbeddingStore 接口，这是 LangChain4j 对所有向量数据库（如 Pinecone, Milvus, Redis 等）的抽象统一接口
import dev.langchain4j.store.embedding.EmbeddingStore;
// 导入 PineconeEmbeddingStore 类，这是专门用于连接 Pinecone 数据库的具体实现类
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
// 导入 PineconeServerlessIndexConfig 类，用于配置 Pinecone Serverless（无服务器版）索引的参数
import dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig;
// 导入 Spring 的 @Autowired 注解，用于自动注入依赖
import org.springframework.beans.factory.annotation.Autowired;
// 导入 Spring 的 @Bean 注解，用于将方法返回值注册为 Spring 容器管理的 Bean
import org.springframework.context.annotation.Bean;
// 导入 Spring 的 @Configuration 注解，标识这是一个配置类
import org.springframework.context.annotation.Configuration;

/**
 * 向量存储配置类
 * 使用 @Configuration 告诉 Spring Boot 这是一个配置类，启动时会加载其中的 Bean 定义
 */
@Configuration
public class EmbeddingStoreConfig {

    // 自动注入 EmbeddingModel 实例
    // 这个实例通常在 application.properties 中通过 langchain4j.community.dashscope... 配置生成
    // 这里注入是为了获取模型的维度信息，保证存入数据库的向量维度正确
    @Autowired
    private EmbeddingModel embeddingModel;

    /**
     * 定义一个名为 embeddingStore 的 Bean
     * 返回类型是 EmbeddingStore<TextSegment>，表示这是一个存储文本片段的向量库
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {

        // 使用 Builder 模式构建 PineconeEmbeddingStore 实例
        // 这是连接 Pinecone 数据库的核心对象
        EmbeddingStore<TextSegment> embeddingStore = PineconeEmbeddingStore.builder()

                // 1. 设置 API Key
                .apiKey(System.getenv("PINECONE_API_KEY"))

                // 2. 设置索引名称 (Index Name)
                // 指定要连接的 Pinecone 索引名为 "xiaozhi-index"
                // 如果该索引不存在且配置了 createIndex，SDK 会尝试自动创建
                .index("xiaozhi-index")

                // 3. 设置命名空间 (Namespace)
                // Pinecone 允许在一个索引内通过 Namespace 隔离不同的数据集
                // 例如：可以用 "chat-history" 存聊天记录，用 "knowledge-base" 存知识库
                .nameSpace("xiaozhi-namespace")

                // 4. 配置自动创建索引的策略 (Create Index Config)
                // 只有当指定的 index 不存在时，以下配置才会生效并尝试创建新索引
                .createIndex(PineconeServerlessIndexConfig.builder()

                        // 4.1 云服务商：指定使用 AWS (Amazon Web Services)
                        .cloud("AWS")

                        // 4.2 区域：指定部署在 us-east-1 (美国东部-弗吉尼亚北部)
                        // 注意：这会产生真实的云服务费用，请根据实际需求选择区域
                        .region("us-east-1")

                        // 4.3 向量维度：动态获取维度
                        // 调用注入的 embeddingModel.dimension() 获取模型输出的向量长度（例如 1536 或 1024）
                        // 必须与模型输出的维度完全一致，否则无法存入数据
                        .dimension(embeddingModel.dimension())

                        // 完成索引配置的构建
                        .build())

                // 完成 PineconeEmbeddingStore 对象的最终构建
                .build();

        // 返回构建好的向量存储实例，Spring 会自动将其管理并在其他地方通过 @Autowired 注入使用
        return embeddingStore;
    }
}

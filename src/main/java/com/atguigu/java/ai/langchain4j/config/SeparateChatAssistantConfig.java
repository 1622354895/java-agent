package com.atguigu.java.ai.langchain4j.config;


import com.atguigu.java.ai.langchain4j.store.MongoChatMemoryStore;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
*
## 代码解释

**创建独立的聊天记忆提供者Bean，支持多用户隔离的对话记忆**。

- `@Configuration`：声明为Spring配置类
- `ChatMemoryProvider`：记忆工厂，根据`memoryId`动态创建独立记忆实例
- 每个用户/会话拥有专属的10条消息窗口记忆，互不干扰

简言之：**为多用户场景提供隔离的对话记忆管理，避免不同用户聊天记录混淆**。
* */
@Configuration
public class SeparateChatAssistantConfig {

    /*
    * 总结
      核心原则：如果一个类使用了 Spring 的依赖注入（@Autowired、@Resource 等），就必须由 Spring 容器管理，不能手动 new。
    *
    * */
    @Autowired
    private MongoChatMemoryStore mongoChatMemoryStore;

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        /*
        *
        Lambda表达式，实现ChatMemoryProvider函数式接口。
        memoryId ->：接收记忆ID参数（如用户ID、会话ID）
        MessageWindowChatMemory.builder()：构建窗口式记忆对象
        .id(memoryId)：为每个会话设置唯一标识，实现记忆隔离
        .maxMessages(10)：最多保留10条消息
        .build()：创建记忆实例
        简言之：每次调用时根据传入的memoryId动态创建独立的对话记忆，确保不同用户的聊天记录互不干扰。
        * */
        return memoryId -> MessageWindowChatMemory
                .builder()
                .id(memoryId)
                .maxMessages(10)
//                .chatMemoryStore(new InMemoryChatMemoryStore())
                .chatMemoryStore(mongoChatMemoryStore) // 使用MongoDB存储对话记忆，聊天记忆持久化
                .build();
    }
}

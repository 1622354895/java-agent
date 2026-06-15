package com.atguigu.java.ai.langchain4j.config;


import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration // 声明为配置类
public class MemoryChatAssistantConfig {

    /*
    *
    * 代码解释
    创建聊天记忆Bean，用于保存对话上下文历史。
    @Bean：将方法返回值注册为Spring容器管理的Bean
    MessageWindowChatMemory.withMaxMessages(10)：创建窗口式记忆，最多保留最近10条消息
    作用：让AI助手具备多轮对话能力，能记住之前的聊天内容
    注意：代码不完整，缺少return chatMemory;语句。
    * */
    @Bean
    public ChatMemory chatMemory() {
       return MessageWindowChatMemory.withMaxMessages(10);

    }
}

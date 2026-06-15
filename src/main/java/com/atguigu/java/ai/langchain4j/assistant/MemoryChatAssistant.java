package com.atguigu.java.ai.langchain4j.assistant;


import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/*
* 可以称之为智能体了 因为有多个组件组成
*
*
        代码解释
        声明式AI助手接口，整合多个组件构成智能体。
        wiringMode = EXPLICIT：显式指定依赖注入
        chatModel = "qwenChatModel"：绑定通义千问模型作为LLM引擎
        chatMemory = "chatMemory"：绑定聊天记忆组件，支持多轮对话上下文
        简言之：通过注解将模型和记忆组件组装成具备上下文感知能力的AI智能体。
* */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
//        chatModel = "qwenChatModel",    // 绑定模型
        chatModel = "openAiChatModel",    // 绑定模型
        chatMemory = "chatMemory")
public interface MemoryChatAssistant {

//    消息占位符：{{it}} 会被替换为用户实际发送的消息内容
    @UserMessage("你是我的好朋友，请用上海话回答问题，并且添加一些表情符号.{{message}}") // 用户提问
    String chat(@V("message") String userMessage);// 用户提问


}

package com.atguigu.java.ai.langchain4j.assistant;


import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/*
## 代码解释

**LangChain4j的Spring Boot声明式AI服务注解**。

- 标记接口为AI服务组件，Spring自动扫描并注册为Bean
- 无需手动调用`AiServices.create()`，直接`@Autowired`注入即可使用
- 方法参数自动映射为大模型提示词，返回值即为模型响应

简言之：**通过注解方式将接口声明为AI助手，Spring自动完成模型绑定和代理创建**。
* */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "qwenChatModel")
public interface Assistant {
    String chat(String userMessage);// 用户提问
}

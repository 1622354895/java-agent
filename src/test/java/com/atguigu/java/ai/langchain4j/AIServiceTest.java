package com.atguigu.java.ai.langchain4j;


import com.atguigu.java.ai.langchain4j.assistant.Assistant;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AIServiceTest {
//    @Autowired
//    private QwenChatModel qwenChatModel;// 自动注入 Qwen
//    @Test
//    public void testQwen(){
//        /*
//        * 这段代码的功能：
//        使用LangChain4j的AiServices工厂方法创建一个AI服务代理实例。
//        第一个参数 Assistant.class：指定AI助手的接口类型，定义可用的方法
//        第二个参数 qwenChatModel：注入的通义千问聊天模型，作为底层LLM提供者
//        返回值：动态生成的Assistant接口实现对象，可将自然语言请求自动路由到Qwen模型处理
//        简言之：将接口与Qwen模型绑定，创建可调用的AI助手服务对象。
//        * */
//        Assistant assistant = AiServices.create(Assistant.class,qwenChatModel);// 创建服务, 使用 qwen
//        String ans = assistant.chat("你是谁");
//        System.out.println(ans);
//
//    }

    // 方式二
    @Autowired
    private Assistant assistant;
    @Test
    public void testQwen2(){
        String ans = assistant.chat("你是谁");
        System.out.println(ans);
    }




}

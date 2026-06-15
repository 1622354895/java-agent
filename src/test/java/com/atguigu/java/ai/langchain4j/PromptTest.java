package com.atguigu.java.ai.langchain4j;

import com.atguigu.java.ai.langchain4j.assistant.MemoryChatAssistant;
import com.atguigu.java.ai.langchain4j.assistant.SeparateChatAssistant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PromptTest {

    @Autowired
    private SeparateChatAssistant separateChatAssistant;

    @Test
    public void testSystemMessage() {
        String answer = separateChatAssistant.chat2(6, "你是什么模型");
        System.out.println(answer);
    }

    @Autowired
    private MemoryChatAssistant memoryChatAssistant;

    @Test
    public void testUserMessage() {
        String answer1 = memoryChatAssistant.chat("我是环环");
        System.out.println(answer1);

        String answer2 = memoryChatAssistant.chat("我18");
        System.out.println(answer2);
        String answer3 = memoryChatAssistant.chat("你知道我的信息吗");
        System.out.println(answer3);
    }

    @Test
    public void testUserInfo() {
        // 参数说明：
        // 1 -> memoryId (会话ID)
        // "我是谁，我多大了" -> userMessage (用户消息)
        // "翠花" -> username (对应模板中的 {{username}})
        // 18 -> age (对应模板中的 {{age}})
        String answer = separateChatAssistant.chat3(1, "我是谁，我多大了", "翠花", 18);
        System.out.println(answer);
    }
}
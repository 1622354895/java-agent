package com.atguigu.java.ai.langchain4j;

import com.atguigu.java.ai.langchain4j.assistant.Assistant;
import com.atguigu.java.ai.langchain4j.assistant.MemoryChatAssistant;
import com.atguigu.java.ai.langchain4j.assistant.SeparateChatAssistant;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

@SpringBootTest
public class ChatMemoryTest {

    @Autowired
    private Assistant assistant;

    @Test
    public void testChatMemory() {
        String answer1 = assistant.chat("我是环环");
        System.out.println(answer1);

        String answer2 = assistant.chat("我是谁");
        System.out.println(answer2);
    }
    @Autowired
    private QwenChatModel qwenChatModel;

    @Test
    public void testChatMemory2() {
        //第一轮对话
        UserMessage userMessage1 = UserMessage.userMessage("我是环环");
        ChatResponse chatResponse1 = qwenChatModel.chat(userMessage1);
        AiMessage aiMessage1 = chatResponse1.aiMessage();
        //输出大语言模型的回复
        System.out.println(aiMessage1.text());

        //第二轮对话
        UserMessage userMessage2 = UserMessage.userMessage("你知道我是谁吗");
        ChatResponse chatResponse2 = qwenChatModel.chat(Arrays.asList(userMessage1, aiMessage1, userMessage2));
        AiMessage aiMessage2 = chatResponse2.aiMessage();
        //输出大语言模型的回复
        System.out.println(aiMessage2.text());
    }

    @Test
    public void testChatMemory3() {
        // 1. 创建滑动窗口式对话存储器，最大缓存10条消息（一问一答算2条），超出自动淘汰最早历史
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        // 2. 基于AiServices构建代理实现类，绑定模型+自定义会话记忆
        Assistant assistant = AiServices
                .builder(Assistant.class)    // 绑定自定义Assistant接口
                .chatLanguageModel(qwenChatModel) // 注入通义千问对话模型
                .chatMemory(chatMemory)           // 挂载上面创建的会话记忆
                .build();

        // 3. 第一轮对话：告知AI昵称
        String answer1 = assistant.chat("我是环环");
        System.out.println(answer1);

        // 4. 第二轮对话：依靠chatMemory自动携带上文历史，AI记住姓名
        String answer2 = assistant.chat("我是谁");
        System.out.println(answer2);
    }


    @Autowired
    private MemoryChatAssistant memoryChatAssistant;    // 自动注入有记忆的MemoryChatAssistant
    @Test
    public void testChatMemory4() {


        // 3. 第一轮对话：告知AI昵称
        String answer1 = memoryChatAssistant.chat("我是戴伟");
        System.out.println(answer1);

        // 4. 第二轮对话：依靠chatMemory自动携带上文历史，AI记住姓名
        String answer2 = memoryChatAssistant.chat("我是谁");
        System.out.println(answer2);

        String answer3 = memoryChatAssistant.chat("你是什么模型，知识库截止时间");
        System.out.println(answer3);
    }


    @Autowired
    private SeparateChatAssistant separateChatAssistant;    // 自动注入有记忆的MemoryChatAssistant memoryChatAssistant;
    @Test
    public void testChatMemory5() {


        // 3. 第一轮对话：告知AI昵称
        String answer1 = separateChatAssistant.chat(1,"我是戴伟");
        System.out.println(answer1);

        // 4. 第二轮对话：依靠chatMemory自动携带上文历史，AI记住姓名
        String answer2 = separateChatAssistant.chat(1,"我是谁");
        System.out.println(answer2);

        String answer3 = separateChatAssistant.chat(2,"我是牛逼");
        System.out.println(answer3);

        String answer4 = separateChatAssistant.chat(2,"我是谁");
        System.out.println(answer4);
    }


}
package com.atguigu.java.ai.langchain4j.store;

import com.atguigu.java.ai.langchain4j.bean.ChatMessages;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;


/*
*
*       这段代码实现了基于 MongoDB 的聊天记忆存储功能：

        1. **getMessages()**：根据 memoryId 从 MongoDB 查询聊天记录，反序列化为 ChatMessage 列表返回
        2. **updateMessages()**：将 ChatMessage 列表序列化后保存到 MongoDB（存在则更新，不存在则插入）
        3. **deleteMessages()**：根据 memoryId 删除对应的聊天记录

        核心作用是为 LangChain4j 提供持久化的对话历史存储能力。
*
* */
@Component
public class MongoChatMemoryStore implements ChatMemoryStore {

    @Autowired
    private MongoTemplate mongoTemplate; // 引入MongoTemplate

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {

        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        // 创建查询条件，指定_id字段等于目标文档ID

        // 根据查询条件构建Query对象
        Query query = new Query(criteria);

        ChatMessages chatMessages = mongoTemplate.findOne(query, ChatMessages.class);
        if(chatMessages == null){
            return new LinkedList<>();// 返回一个空的LinkedList
        }
        String contentJson = chatMessages.getContent();// 获取content字段的值

        return ChatMessageDeserializer.messagesFromJson(contentJson);

    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        // 创建查询条件，指定_id字段等于目标文档ID

        // 根据查询条件构建Query对象
        Query query = new Query(criteria);

        // 创建Update对象用于定义更新操作
        Update update = new Update();

        String contentJson = ChatMessageSerializer.messagesToJson(list);
        update.set("content", contentJson);

        // 执行upsert操作：如果文档存在则更新，不存在则插入新文档
        mongoTemplate.upsert(query, update, ChatMessages.class);

    }

    @Override
    public void deleteMessages(Object memoryId) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        Query query = new Query(criteria);
        mongoTemplate.remove(query, ChatMessages.class);

    }
}

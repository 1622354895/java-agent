package com.atguigu.java.ai.langchain4j;

import com.atguigu.java.ai.langchain4j.bean.ChatMessages;
import org.springframework.data.mongodb.core.query.Criteria;  // ✅ 正确;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@SpringBootTest
public class MongoCrudTest {

    @Autowired
    private MongoTemplate mongoTemplate;

//    @Test
//    public void testMongoTemplate() {
//        mongoTemplate.insert(new ChatMessages(System.currentTimeMillis(), "chat message"));
////        mongoTemplate.insert(2, "chat message");
//    }

    @Test
    public void testMongoTemplate2() {
        ChatMessages chatMessages = new ChatMessages();
        chatMessages.setContent("聊天记录");

        mongoTemplate.insert(chatMessages);
//        mongoTemplate.insert(2, "chat message");
    }

    @Test
    public void testFindById() {
        ChatMessages chatMessages = mongoTemplate.findById("6a2914f39e086460a7add245", ChatMessages.class);
        System.out.println(chatMessages);
    }
    
    @Test
    public void testUpdate() {
        Criteria criteria = Criteria.where("_id").is(100);
        // 创建查询条件，指定_id字段等于目标文档ID
//        Criteria criteria = Criteria.where("_id").is("6a2914f39e086460a7add245");

        // 根据查询条件构建Query对象
        Query query = new Query(criteria);

        // 创建Update对象用于定义更新操作
        Update update = new Update();

        // 设置content字段的更新值为"更新后的内容"
        update.set("content", "更新后的内容");

        // 执行upsert操作：如果文档存在则更新，不存在则插入新文档
        mongoTemplate.upsert(query, update, ChatMessages.class);


    }

    @Test
    public void testDelete() {
        Criteria criteria = Criteria.where("_id").is(100);
        Query query = new Query(criteria);
        mongoTemplate.remove(query, ChatMessages.class);
    }
}

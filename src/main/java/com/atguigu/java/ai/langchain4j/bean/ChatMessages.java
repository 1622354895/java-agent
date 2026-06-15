package com.atguigu.java.ai.langchain4j.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


/**
 * 聊天记录实体类
 * 用于存储和管理聊天会话中的消息记录，映射到MongoDB的chat_messages集合
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document("chat_messages")
public class ChatMessages {


    /**
     * MongoDB文档的唯一标识符
     * 使用ObjectId类型，自动映射到MongoDB的_id字段
     * 由MongoDB自动生成或手动指定
     */

    @Id
    private ObjectId messageId;

    private String memoryId;

    private String content;
}

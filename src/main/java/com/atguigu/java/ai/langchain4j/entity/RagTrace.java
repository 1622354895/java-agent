package com.atguigu.java.ai.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * 这是一个RAG追踪实体类，用于记录检索增强生成的调用信息：
 * 映射数据库表：对应rag_trace表
 * 记录字段：包含问题、来源文件、相似度分数、文本预览和创建时间
 * 使用Lombok：通过@Data自动生成getter/setter方法
 */
@Data
@TableName("rag_trace")
public class RagTrace {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String question;

    private String sourceFile;

    private Double score;

    private String textPreview;

    private LocalDateTime createdTime;
}
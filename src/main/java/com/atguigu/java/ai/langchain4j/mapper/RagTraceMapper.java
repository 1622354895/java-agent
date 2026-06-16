package com.atguigu.java.ai.langchain4j.mapper;

import com.atguigu.java.ai.langchain4j.entity.RagTrace;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 *
 这是一个MyBatis-Plus的Mapper接口：
 继承BaseMapper：自动获得对RagTrace表的CRUD操作能力
 标注@Mapper：声明为MyBatis映射器，由Spring容器管理
 功能：提供RAG追踪记录的数据库访问层，无需编写SQL即可实现基本数据操作
 */
@Mapper
public interface RagTraceMapper extends BaseMapper<RagTrace> {
}
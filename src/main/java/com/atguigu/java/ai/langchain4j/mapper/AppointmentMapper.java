package com.atguigu.java.ai.langchain4j.mapper;

import com.atguigu.java.ai.langchain4j.entity.Appointment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
/*
*
*       这是一个标准的 MyBatis-Plus Mapper 接口，
*          通过极简的代码实现了对 Appointment 表的完整 CRUD 操作，体现了 MyBatis-Plus "简化开发" 的设计理念。
*
* */



@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {

}
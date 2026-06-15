package com.atguigu.java.ai.langchain4j.service;

import com.atguigu.java.ai.langchain4j.entity.Appointment;
import com.baomidou.mybatisplus.extension.service.IService;



/*
*   这是一个标准的 MyBatis-Plus 服务层接口，通过继承 IService 获得了完整的 CRUD 业务能力，
*   同时扩展了自定义的 getOne 方法用于特定业务场景。*
* */
public interface AppointmentService extends IService<Appointment> {

    //判断预约订单是否存在
    Appointment getOne(Appointment appointment);
}
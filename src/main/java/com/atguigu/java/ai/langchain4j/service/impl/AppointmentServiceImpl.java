package com.atguigu.java.ai.langchain4j.service.impl;

import com.atguigu.java.ai.langchain4j.entity.Appointment;
import com.atguigu.java.ai.langchain4j.mapper.AppointmentMapper;
import com.atguigu.java.ai.langchain4j.service.AppointmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

// 声明该类所属的包路径，按照项目分层规范放在service.impl实现层
//package com.atguigu.java.ai.langchain4j.service.impl;

// 导入预约信息实体类，封装了预约相关的所有字段和属性
import com.atguigu.java.ai.langchain4j.entity.Appointment;
// 导入预约数据访问层Mapper接口，用于执行数据库CRUD操作
import com.atguigu.java.ai.langchain4j.mapper.AppointmentMapper;
// 导入预约业务层接口，该类需要实现此接口定义的所有方法
import com.atguigu.java.ai.langchain4j.service.AppointmentService;
// 导入MyBatis-Plus的Lambda条件构造器，用于类型安全的数据库查询条件构建
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
// 导入MyBatis-Plus提供的通用Service实现类，封装了基础的CRUD方法
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
// 导入Spring的Service注解，将该类标记为Spring容器管理的业务层Bean
import org.springframework.stereotype.Service;

/**
 * 预约业务层实现类
 * 继承MyBatis-Plus的ServiceImpl获得通用CRUD能力，实现AppointmentService接口定义的业务方法
 */

@Service
// ServiceImpl<Mapper接口, 实体类>：MyBatis-Plus通用实现，泛型1指定Mapper，泛型2指定操作的实体
// implements AppointmentService：实现自定义的预约业务接口
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements AppointmentService {

    /**
     * 根据多条件精确查询预约记录是否存在
     * 用于防止用户重复预约相同时间、相同科室的号源
     * @param appointment 前端传入的预约信息，包含查询所需的所有条件字段
     * @return 数据库中匹配的预约记录，不存在则返回null
     */
    @Override
    public Appointment getOne(Appointment appointment) {
        // 创建Lambda查询条件构造器，泛型指定操作的实体类为Appointment
        // Lambda方式的优势：使用方法引用替代硬编码字段名，编译时就能检查字段是否存在
        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();

        /**
         * queryWrapper.eq(
         *     Appointment::getUsername,  // 第一个参数：要查询的数据库列
         *     appointment.getUsername()  // 第二个参数：要匹配的具体值
         * );
         */
        // 拼接等值查询条件：用户名必须完全匹配
        queryWrapper.eq(Appointment::getUsername, appointment.getUsername());
        // 拼接等值查询条件：身份证号必须完全匹配（唯一标识用户）
        queryWrapper.eq(Appointment::getIdCard, appointment.getIdCard());
        // 拼接等值查询条件：预约科室必须完全匹配
        queryWrapper.eq(Appointment::getDepartment, appointment.getDepartment());
        // 拼接等值查询条件：预约日期必须完全匹配
        queryWrapper.eq(Appointment::getDate, appointment.getDate());
        // 拼接等值查询条件：预约时段必须完全匹配
        queryWrapper.eq(Appointment::getTime, appointment.getTime());

        // 调用MyBatis-Plus提供的baseMapper执行查询
        // baseMapper是ServiceImpl中自动注入的AppointmentMapper实例，无需手动@Autowired
        // selectOne方法：根据条件查询一条记录，若匹配多条会抛出异常（这里多条件组合保证唯一）
        Appointment appointmentDB = baseMapper.selectOne(queryWrapper);
        // 返回查询结果，存在则返回预约对象，不存在则返回null
        return appointmentDB;
    }
}
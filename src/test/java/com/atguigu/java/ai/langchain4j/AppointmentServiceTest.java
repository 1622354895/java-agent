// 声明该测试类所属的包路径，按照项目规范与被测试类放在相同包结构下
package com.atguigu.java.ai.langchain4j;

// 导入预约信息实体类，用于封装测试数据和接收查询结果
import com.atguigu.java.ai.langchain4j.entity.Appointment;
// 导入预约业务层接口，调用其方法进行测试
import com.atguigu.java.ai.langchain4j.service.AppointmentService;
// 导入Spring Boot测试核心注解，用于启动Spring Boot应用上下文
import org.springframework.boot.test.context.SpringBootTest;
// 导入JUnit 5的测试注解，标记方法为测试用例
import org.junit.jupiter.api.Test;
// 导入Spring的自动注入注解，用于注入业务层Bean
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 预约业务层集成测试类
 * 基于Spring Boot Test + JUnit 5，启动完整的Spring上下文，测试Service层与数据库的交互
 */
@SpringBootTest // 核心注解：启动Spring Boot应用上下文，自动扫描并加载所有Spring管理的Bean
class AppointmentServiceTest {

    /**
     * 自动注入预约业务层接口的实现类
     * Spring会自动从容器中找到AppointmentServiceImpl的实例并注入进来
     */
    @Autowired
    private AppointmentService appointmentService;

    /**
     * 测试根据多条件查询单条预约记录的功能
     * 验证预约查重逻辑是否正常工作
     */
    @Test // 标记该方法为JUnit 5测试用例，运行时会自动执行
    void testGetOne() {
        // 1. 创建预约对象，封装查询条件
        Appointment appointment = new Appointment();
        // 设置查询条件：用户名为张三
        appointment.setUsername("张三");
        // 设置查询条件：身份证号（唯一标识用户）
        appointment.setIdCard("123456789012345678");
        // 设置查询条件：预约科室为内科
        appointment.setDepartment("内科");
        // 设置查询条件：预约日期为2025年4月14日
        appointment.setDate("2025-04-14");
        // 设置查询条件：预约时段为上午
        appointment.setTime("上午");

        // 2. 调用业务层方法，根据条件查询数据库中的预约记录
        Appointment appointmentDB = appointmentService.getOne(appointment);
        // 3. 打印查询结果到控制台，验证是否查询成功
        // 存在则打印完整的预约对象，不存在则打印null
        System.out.println(appointmentDB);
    }

    /**
     * 测试新增预约记录的功能
     * 验证MyBatis-Plus通用save方法是否正常工作
     */
    @Test
    void testSave() {
        // 1. 创建预约对象，封装要保存的完整预约信息
        Appointment appointment = new Appointment();
        // 设置预约人姓名
        appointment.setUsername("张三");
        // 设置预约人身份证号
        appointment.setIdCard("123456789012345678");
        // 设置预约科室
        appointment.setDepartment("内科");
        // 设置预约日期
        appointment.setDate("2025-04-14");
        // 设置预约时段
        appointment.setTime("上午");
        // 设置接诊医生姓名
        appointment.setDoctorName("张医生");

        // 2. 调用业务层的通用保存方法，将预约信息插入数据库
        // save方法是MyBatis-Plus的IService接口提供的通用CRUD方法
        // 主键id为自增，插入成功后会自动回写到appointment对象的id属性中
        appointmentService.save(appointment);
    }

    /**
     * 测试根据主键ID删除预约记录的功能
     * 验证MyBatis-Plus通用removeById方法是否正常工作
     */
    @Test
    void testRemoveById() {
        // 调用业务层的通用删除方法，根据主键id删除数据库中的记录
        // 参数1L表示要删除的记录主键id为1，L后缀表示这是Long类型，与数据库bigint类型对应
        appointmentService.removeById(2L);
    }
}
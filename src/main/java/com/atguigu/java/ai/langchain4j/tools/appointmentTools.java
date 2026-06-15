// 声明该工具类所属的包路径，位于tools包下，专门存放LangChain4j可调用的工具方法
package com.atguigu.java.ai.langchain4j.tools;

// 导入预约信息实体类，用于封装预约参数和数据交互
import com.atguigu.java.ai.langchain4j.entity.Appointment;
// 导入预约业务层接口，用于调用数据库CRUD方法
import com.atguigu.java.ai.langchain4j.service.AppointmentService;
// 导入LangChain4j的@P注解，用于向大模型描述工具方法的参数含义和约束
import dev.langchain4j.agent.tool.P;
// 导入LangChain4j的@Tool注解，标记方法为大模型可调用的工具
import dev.langchain4j.agent.tool.Tool;
// 导入Spring的自动注入注解，用于注入业务层Bean
import org.springframework.beans.factory.annotation.Autowired;
// 导入Spring的组件注解，将该类标记为Spring容器管理的Bean
import org.springframework.stereotype.Component;

/**
 * 预约工具类
 * 为LangChain4j智能体提供可调用的医疗预约相关工具方法
 * 包含：预约挂号、取消预约、查询号源三大核心能力
 */
@Component // 将当前类注册为Spring组件，自动实例化并纳入容器管理
public class appointmentTools {

    /**
     * 自动注入预约业务层接口实例
     * Spring会自动从容器中找到AppointmentServiceImpl并注入
     */
    @Autowired
    private AppointmentService appointmentService;

    /**
     * 预约挂号工具方法
     * 被LangChain4j识别为可调用工具，实现医疗预约的查重+新增逻辑
     *
     * @param appointment 预约信息实体，包含用户名、身份证、科室、日期、时间等信息
     * @return 预约结果提示语：预约成功/预约失败/已有预约
     */
    @Tool(name="预约挂号", value = "根据参数，先执行工具方法queryDepartment查询是否可预约，并直接给用户回答是否可预约，" +
            "并让用户确认所有预约信息，用户确认后再进行预约。如果用户没有提供具体的医生姓名，请从向量存储中找到一位医生。")
    public String bookAppointment(Appointment appointment) {
        // 1. 调用业务层方法，根据多条件查询数据库中是否存在相同预约记录
        // 作用：防止用户在同一科室、同一时间重复预约
        Appointment appointmentDB = appointmentService.getOne(appointment);

        // 2. 判断数据库中是否存在相同预约记录
        if (appointmentDB == null) {
            // 数据库中无重复预约，允许新增
            appointment.setId(null);
            // 【AI场景关键处理】防止大模型幻觉生成的id影响自增主键
            // 即使大模型生成了id字段，也强制设置为null，让数据库自动生成自增主键
            if (appointmentService.save(appointment)) {
                // 调用业务层save方法插入预约记录，插入成功返回true
                return "预约成功，并返回预约详情";
            } else {
                // 数据库插入失败，返回失败提示
                return "预约失败";
            }
        }
        // 数据库中存在相同预约记录，返回重复预约提示
        return "您在相同的科室和时间已有预约";
    }

    /**
     * 取消预约挂号工具方法
     * 被LangChain4j识别为可调用工具，实现预约记录的查询+删除逻辑
     *
     * @param appointment 预约信息实体，包含查询预约所需的条件字段
     * @return 取消预约结果提示语：取消成功/取消失败/无预约记录
     */
    @Tool(name = "取消预约挂号", value = "根据参数，查询预约是否存在，如果存在则删除预约记录并返回取消预约成功，否则返回取消预约失败")
    public String cancelAppointment(Appointment appointment) {
        // 1. 调用业务层方法，根据多条件查询数据库中是否存在对应的预约记录
        Appointment appointmentDB = appointmentService.getOne(appointment);

        // 2. 判断预约记录是否存在
        if (appointmentDB != null) {
            // 预约记录存在，执行删除操作
            if (appointmentService.removeById(appointmentDB.getId())) {
                // 根据数据库主键id删除预约记录，删除成功返回true
                return "取消预约成功";
            } else {
                // 数据库删除失败，返回失败提示
                return "取消预约失败";
            }
        }
        // 预约记录不存在，返回无预约记录提示
        return "您没有预约记录，请核对预约科室和时间";
    }

    /**
     * 查询号源工具方法
     * 被LangChain4j识别为可调用工具，实现号源可用性查询逻辑
     *
     * @param name        科室名称（必填）
     * @param date        预约日期（必填，格式如2025-04-14）
     * @param time        预约时段（必填，可选值：上午、下午）
     * @param doctorName  医生姓名（可选，未指定则查询科室下所有可预约医生）
     * @return true表示有号源，false表示无号源
     */
    @Tool(name = "查询是否有号源", value = "根据科室名称，日期，时间和医生查询是否有号源，并返回给用户")
    public boolean queryDepartment(
            @P(value = "科室名称") String name,
            @P(value = "日期") String date,
            @P(value = "时间，可选值：上午、下午") String time,
            @P(value = "医生名称", required = false) String doctorName
    ) {
        // 打印日志，输出大模型调用该工具时传入的参数，便于调试
        System.out.println("查询是否有号源");
        System.out.println("科室名称：" + name);
        System.out.println("日期：" + date);
        System.out.println("时间：" + time);
        System.out.println("医生名称：" + doctorName);

        // TODO 维护医生的排班信息：
        // 后续需要实现的业务逻辑，目前先返回true作为占位
        // 1. 如果没有指定医生名字：
        //    根据科室、日期、时间查询该科室下是否有可预约的医生，有则返回true，无则返回false
        // 2. 如果指定了医生名字：
        //    ① 先判断该医生在指定日期、时间是否有排班，无排班返回false
        //    ② 有排班则判断该时间段是否已约满，约满返回false，有空闲号源返回true

        // 目前占位返回true，表示有号源
        return true;
    }
}
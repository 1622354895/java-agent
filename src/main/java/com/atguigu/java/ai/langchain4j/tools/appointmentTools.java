// 声明该工具类所属的包路径，位于tools包下，专门存放LangChain4j可调用的工具方法
package com.atguigu.java.ai.langchain4j.tools;

// 导入预约信息实体类，用于封装预约参数和数据交互
import com.atguigu.java.ai.langchain4j.entity.Appointment;
// 导入预约业务层接口，用于调用数据库CRUD方法
import com.atguigu.java.ai.langchain4j.entity.DoctorSchedule;
import com.atguigu.java.ai.langchain4j.service.AppointmentService;
// 导入LangChain4j的@P注解，用于向大模型描述工具方法的参数含义和约束
import com.atguigu.java.ai.langchain4j.service.DoctorScheduleService;
import dev.langchain4j.agent.tool.P;
// 导入LangChain4j的@Tool注解，标记方法为大模型可调用的工具
import dev.langchain4j.agent.tool.Tool;
// 导入Spring的自动注入注解，用于注入业务层Bean
import org.springframework.beans.factory.annotation.Autowired;
// 导入Spring的组件注解，将该类标记为Spring容器管理的Bean
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.StringUtils;

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
    private AppointmentService appointmentService; // 注入预约业务层接口实例

    @Autowired
    private DoctorScheduleService doctorScheduleService; // 注入预约号源业务层接口实例

    /**
     * 预约挂号工具方法
     * 被LangChain4j识别为可调用工具，实现医疗预约的查重+新增逻辑
     *
     * @param appointment 预约信息实体，包含用户名、身份证、科室、日期、时间等信息
     * @return 预约结果提示语：预约成功/预约失败/已有预约
     */
//    @Tool(name="预约挂号", value = "根据参数，先执行工具方法queryDepartment查询是否可预约，并直接给用户回答是否可预约，" +
//            "并让用户确认所有预约信息，用户确认后再进行预约。如果用户没有提供具体的医生姓名，请从向量存储中找到一位医生。")
//    public String bookAppointment(Appointment appointment) {
//        // 1. 调用业务层方法，根据多条件查询数据库中是否存在相同预约记录
//        // 作用：防止用户在同一科室、同一时间重复预约
//        Appointment appointmentDB = appointmentService.getOne(appointment);
//
//        // 2. 判断数据库中是否存在相同预约记录
//        if (appointmentDB == null) {
//            // 数据库中无重复预约，允许新增
//            appointment.setId(null);
//            // 【AI场景关键处理】防止大模型幻觉生成的id影响自增主键
//            // 即使大模型生成了id字段，也强制设置为null，让数据库自动生成自增主键
//            if (appointmentService.save(appointment)) {
//                // 调用业务层save方法插入预约记录，插入成功返回true
//                return "预约成功，并返回预约详情";
//            } else {
//                // 数据库插入失败，返回失败提示
//                return "预约失败";
//            }
//        }
//        // 数据库中存在相同预约记录，返回重复预约提示
//        return "您在相同的科室和时间已有预约";
//    }

    @Tool(
            name = "预约挂号",
            value = "根据用户提供的姓名、身份证号、科室、日期、时间和医生姓名进行真实预约挂号。预约前必须查询真实排班表并扣减号源，不能凭空创建预约。"
    )
    @Transactional
    public String bookAppointment(Appointment appointment) {
        String missingFields = validateAppointmentRequiredFields(appointment);
        if (missingFields != null) {
            return "预约失败：缺少必要信息：" + missingFields + "。请先补充完整后再预约。";
        }

        // 1. 重复预约校验：根据用户身份、科室、日期、时段多条件查询是否已存在预约记录
        // 作用：防止同一用户在相同科室、相同日期、相同时段重复挂号
        Appointment appointmentDB = appointmentService.getOne(appointment);

        // 判断：如果查询到已存在的预约记录，说明用户重复预约
        if (appointmentDB != null) {
            // 直接返回重复预约的失败提示，终止后续流程
            return "预约失败：您在相同科室、日期和时间已经有预约，请不要重复预约。";
        }

        // 2. 查询可用排班：从医生排班表中查找符合条件且剩余号源 > 0 的排班记录
        // 支持两种场景：指定医生则查询该医生是否有号；未指定医生则自动匹配科室下第一位有号的医生
        DoctorSchedule schedule = doctorScheduleService.findAvailableSchedule(
                appointment.getDepartment(),
                appointment.getDate(),
                appointment.getTime(),
                appointment.getDoctorName()
        );

        // 判断：如果没有匹配到可用排班，说明当前时段无号源
        if (schedule == null) {
            // 拼接无号源的失败提示；三元运算符逻辑：用户指定了医生就拼接医生信息，未指定则不显示
            return "预约失败：当前没有可预约号源。科室：" + appointment.getDepartment()
                    + "，日期：" + appointment.getDate()
                    + "，时间：" + appointment.getTime()
                    + (appointment.getDoctorName() == null ? "" : "，医生：" + appointment.getDoctorName());
        }

        // 3. 并发安全扣减号源：调用数据库条件更新语句（remaining_quota > 0 时才扣减）
        // 核心作用：数据库层面原子执行，杜绝高并发下号源超卖问题
        boolean decreased = doctorScheduleService.decreaseQuota(schedule.getId());

        // 判断：如果扣减返回false，说明号源在并发场景下刚好被其他用户抢完
        if (!decreased) {
            // 返回号源已约满的提示，引导用户选择其他时段
            return "预约失败：号源刚刚已被约满，请重新选择其他医生或时间。";
        }

        // 4. AI场景专项处理：强制将主键ID置为null
        // 作用：防止大模型幻觉生成无效ID值，干扰数据库自增主键的生成逻辑
        appointment.setId(null);
        // 补全预约信息：用排班表中的真实医生姓名填充，保证预约记录与排班信息一致
        // 若用户未指定医生，此处会自动分配排班匹配到的医生
        appointment.setDoctorName(schedule.getDoctorName());

        // 5. 持久化预约记录：将完整的预约信息写入数据库预约表
        boolean saved = appointmentService.save(appointment);

        // 判断：如果预约记录保存失败，需要执行号源回滚，保证数据一致性
        if (!saved) {
            // 异常回滚：把刚才扣减的号源重新加回排班表，避免号源凭空丢失
            doctorScheduleService.increaseQuota(schedule.getId());
            // 返回保存失败提示，告知用户号源已释放，可稍后重试
            return "预约失败：预约记录保存失败，号源已释放，请稍后重试。";
        }

        // 6. 全部流程执行成功，拼接完整预约详情返回
        // 返回结构化的成功信息，大模型可直接转述给用户，清晰展示预约结果
        return "预约成功。预约详情：姓名：" + appointment.getUsername()
                + "，科室：" + appointment.getDepartment()
                + "，医生：" + appointment.getDoctorName()
                + "，日期：" + appointment.getDate()
                + "，时间：" + appointment.getTime();
    }

    /**
     * 取消预约挂号工具方法
     * 被LangChain4j识别为可调用工具，实现预约记录的查询+删除逻辑
     *
     * @param appointment 预约信息实体，包含查询预约所需的条件字段
     * @return 取消预约结果提示语：取消成功/取消失败/无预约记录
     */
//    @Tool(name = "取消预约挂号", value = "根据参数，查询预约是否存在，如果存在则删除预约记录并返回取消预约成功，否则返回取消预约失败")
//    public String cancelAppointment(Appointment appointment) {
//        // 1. 调用业务层方法，根据多条件查询数据库中是否存在对应的预约记录
//        Appointment appointmentDB = appointmentService.getOne(appointment);
//
//        // 2. 判断预约记录是否存在
//        if (appointmentDB != null) {
//            // 预约记录存在，执行删除操作
//            if (appointmentService.removeById(appointmentDB.getId())) {
//                // 根据数据库主键id删除预约记录，删除成功返回true
//                return "取消预约成功";
//            } else {
//                // 数据库删除失败，返回失败提示
//                return "取消预约失败";
//            }
//        }
//        // 预约记录不存在，返回无预约记录提示
//        return "您没有预约记录，请核对预约科室和时间";
//    }
    @Tool(
            name = "取消预约挂号",
            value = "根据用户提供的姓名、身份证号、科室、日期、时间和医生姓名取消预约。取消成功后释放对应医生排班号源。"
    )
    @Transactional
    public String cancelAppointment(Appointment appointment) {
        String missingFields = validateAppointmentRequiredFields(appointment);
        if (missingFields != null) {
            return "取消失败：缺少必要信息：" + missingFields + "。请先补充完整后再取消预约。";
        }

        Appointment appointmentDB = appointmentService.getOne(appointment);

        if (appointmentDB == null) {
            return "取消失败：未查询到对应预约记录，请核对姓名、身份证号、科室、日期和时间。";
        }

        DoctorSchedule schedule = doctorScheduleService.findSchedule(
                appointmentDB.getDepartment(),
                appointmentDB.getDate(),
                appointmentDB.getTime(),
                appointmentDB.getDoctorName()
        );

        if (schedule == null) {
            return "取消失败：已查询到预约记录，但未找到对应医生排班，暂不删除预约记录，请联系人工处理。";
        }

        boolean removed = appointmentService.removeById(appointmentDB.getId());

        if (!removed) {
            return "取消失败：预约记录删除失败，请稍后重试。";
        }

        boolean increased = doctorScheduleService.increaseQuota(schedule.getId());

        if (!increased) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return "取消失败：预约记录已找到，但号源释放失败，系统已回滚取消操作，请稍后重试或联系人工处理。";
        }

        return "取消预约成功。已释放号源。科室：" + appointmentDB.getDepartment()
                + "，医生：" + appointmentDB.getDoctorName()
                + "，日期：" + appointmentDB.getDate()
                + "，时间：" + appointmentDB.getTime();
    }

//    /**
//     * 查询号源工具方法
//     * 被LangChain4j识别为可调用工具，实现号源可用性查询逻辑
//     *
//     * @param name        科室名称（必填）
//     * @param date        预约日期（必填，格式如2025-04-14）
//     * @param time        预约时段（必填，可选值：上午、下午）
//     * @param doctorName  医生姓名（可选，未指定则查询科室下所有可预约医生）
//     * @return true表示有号源，false表示无号源
//     */
//    @Tool(name = "查询是否有号源", value = "根据科室名称，日期，时间和医生查询是否有号源，并返回给用户")
//    public boolean queryDepartment(
//            @P(value = "科室名称") String name,
//            @P(value = "日期") String date,
//            @P(value = "时间，可选值：上午、下午") String time,
//            @P(value = "医生名称", required = false) String doctorName
//    ) {
//        // 打印日志，输出大模型调用该工具时传入的参数，便于调试
//        System.out.println("查询是否有号源");
//        System.out.println("科室名称：" + name);
//        System.out.println("日期：" + date);
//        System.out.println("时间：" + time);
//        System.out.println("医生名称：" + doctorName);
//
//        // TODO 维护医生的排班信息：
//        // 后续需要实现的业务逻辑，目前先返回true作为占位
//        // 1. 如果没有指定医生名字：
//        //    根据科室、日期、时间查询该科室下是否有可预约的医生，有则返回true，无则返回false
//        // 2. 如果指定了医生名字：
//        //    ① 先判断该医生在指定日期、时间是否有排班，无排班返回false
//        //    ② 有排班则判断该时间段是否已约满，约满返回false，有空闲号源返回true
//
//        // 目前占位返回true，表示有号源
//        return true;
//    }

    @Tool(
            name = "查询是否有号源",
            value = "根据科室名称、预约日期、预约时间和医生姓名查询真实排班表，返回是否有可预约号源。必须以工具返回结果为准，不能自行编造号源。"
    )
    public String queryDepartment(
            @P(value = "科室名称", required = false) String name,
            @P(value = "日期，格式：yyyy-MM-dd") String date,
            @P(value = "时间，可选值：上午、下午") String time,
            @P(value = "医生名称", required = false) String doctorName
    ) {
        String missingFields = validateScheduleQueryFields(name, date, time, doctorName);
        if (missingFields != null) {
            return "查询失败：缺少必要信息：" + missingFields + "。请先补充日期、时间，以及科室或医生姓名后再查询号源。";
        }

        DoctorSchedule schedule = doctorScheduleService.findAvailableSchedule(name, date, time, doctorName);

        if (schedule == null) {
            return "没有可预约号源。科室：" + name + "，日期：" + date + "，时间：" + time
                    + (doctorName == null ? "" : "，医生：" + doctorName);
        }

        return "有可预约号源。科室：" + schedule.getDepartment()
                + "，医生：" + schedule.getDoctorName()
                + "，日期：" + schedule.getWorkDate()
                + "，时间：" + schedule.getTimeSlot()
                + "，剩余号源：" + schedule.getRemainingQuota();
    }

    private String validateAppointmentRequiredFields(Appointment appointment) {
        if (appointment == null) {
            return "预约信息";
        }

        StringBuilder missingFields = new StringBuilder();
        appendMissingField(missingFields, "姓名", appointment.getUsername());
        appendMissingField(missingFields, "身份证号", appointment.getIdCard());
        appendMissingField(missingFields, "科室", appointment.getDepartment());
        appendMissingField(missingFields, "日期", appointment.getDate());
        appendMissingField(missingFields, "时间", appointment.getTime());

        return missingFields.length() == 0 ? null : missingFields.toString();
    }

    private String validateScheduleRequiredFields(String department, String date, String time) {
        StringBuilder missingFields = new StringBuilder();
        appendMissingField(missingFields, "科室", department);
        appendMissingField(missingFields, "日期", date);
        appendMissingField(missingFields, "时间", time);

        return missingFields.length() == 0 ? null : missingFields.toString();
    }

    private String validateScheduleQueryFields(String department, String date, String time, String doctorName) {
        StringBuilder missingFields = new StringBuilder();
        appendMissingField(missingFields, "日期", date);
        appendMissingField(missingFields, "时间", time);

        if (!StringUtils.hasText(department) && !StringUtils.hasText(doctorName)) {
            appendMissingField(missingFields, "科室或医生姓名任一项", null);
        }

        return missingFields.length() == 0 ? null : missingFields.toString();
    }

    private void appendMissingField(StringBuilder missingFields, String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            if (missingFields.length() > 0) {
                missingFields.append("、");
            }
            missingFields.append(fieldName);
        }
    }
}

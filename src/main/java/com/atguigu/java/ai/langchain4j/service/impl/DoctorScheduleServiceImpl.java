// 定义包路径：服务实现类的包结构，遵循分层架构规范
package com.atguigu.java.ai.langchain4j.service.impl;

// 导入实体类，表示医生排班数据模型
import com.atguigu.java.ai.langchain4j.entity.DoctorSchedule;
// 导入MyBatis-Plus Mapper接口，用于数据库操作
import com.atguigu.java.ai.langchain4j.mapper.DoctorScheduleMapper;
// 导入业务服务接口定义
import com.atguigu.java.ai.langchain4j.service.DoctorScheduleService;
// 导入MyBatis-Plus的Lambda查询条件构建器（类型安全，避免字段名硬编码）
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
// 导入MyBatis-Plus基础服务实现类
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
// 导入Spring的@Service注解
import org.springframework.stereotype.Service;
// 导入Spring的StringUtils工具类（用于字符串空值检查）
import org.springframework.util.StringUtils;

/**
 * 医生排班服务实现类
 * 基于MyBatis-Plus实现数据库操作，提供号源查询与管理功能
 * 业务特点：支持按科室/日期/时段/医生多条件查询可用号源
 */
@Service  // 标记为Spring管理的Bean，自动注册到IOC容器
public class DoctorScheduleServiceImpl
        // 继承MyBatis-Plus通用Service实现
        extends ServiceImpl<DoctorScheduleMapper, DoctorSchedule>
        // 实现自定义业务接口
        implements DoctorScheduleService {

    /**
     * 查询符合条件的可用排班记录（有剩余号源）
     *
     * 业务规则：
     * 1. 必须满足科室、日期、时段三个基础条件
     * 2. 剩余号源必须大于0
     * 3. 支持按医生姓名精确过滤
     * 4. 优先返回剩余号源最多的排班记录
     *
     * @param department 科室名称（必填）
     * @param date 工作日期（格式：yyyy-MM-dd，必填）
     * @param time 时段标识（如：上午/下午/晚上，必填）
     * @param doctorName 医生姓名（可选）
     * @return 符合条件的排班记录，无结果返回null
     */
    @Override
    public DoctorSchedule findAvailableSchedule(String department, String date, String time, String doctorName) {
        // 创建Lambda查询条件包装器（类型安全，编译期检查字段名）
        LambdaQueryWrapper<DoctorSchedule> queryWrapper = new LambdaQueryWrapper<>();

        // 添加必填条件：科室匹配
        if (StringUtils.hasText(department)) {
            queryWrapper.eq(DoctorSchedule::getDepartment, department);
        }
        // 添加必填条件：工作日期匹配（注意：此处date应为LocalDate类型但实际传入String，存在潜在类型风险）
        queryWrapper.eq(DoctorSchedule::getWorkDate, date);
        // 添加必填条件：时段匹配
        queryWrapper.eq(DoctorSchedule::getTimeSlot, time);
        // 核心业务条件：剩余号源必须大于0（保证可预约）
        queryWrapper.gt(DoctorSchedule::getRemainingQuota, 0);

        // 动态条件：当医生姓名不为空时添加过滤
        if (StringUtils.hasText(doctorName)) {
            // 精确匹配医生姓名
            queryWrapper.eq(DoctorSchedule::getDoctorName, doctorName);
        }

        // 排序规则：按剩余号源降序排列（优先选择号源充足的排班）
        queryWrapper.orderByDesc(DoctorSchedule::getRemainingQuota);
        // 限制结果数量：仅取第一条记录（配合排序实现"最优排班"选择）
        queryWrapper.last("LIMIT 1");

        // 直接通过Mapper执行查询（绕过Service层的getOne方法，避免自动填充逻辑干扰）
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 检查是否存在可用排班
     *
     * @param department 科室名称
     * @param date 工作日期
     * @param time 时段标识
     * @param doctorName 医生姓名（可选）
     * @return true=存在可用号源，false=无可用号源
     */
    @Override
    public boolean hasAvailableSchedule(String department, String date, String time, String doctorName) {
        // 复用查询方法，仅判断结果是否存在
        return findAvailableSchedule(department, date, time, doctorName) != null;
    }

    /**
     * 扣减号源（预约成功时调用）
     *
     * @param scheduleId 排班记录ID
     * @return true=扣减成功，false=扣减失败（可能已被约满）
     */
    @Override
    public boolean decreaseQuota(Long scheduleId) {
        // 调用Mapper的自定义更新方法
        // 注意：此处未做剩余号源校验，应确保SQL中包含`WHERE remaining_quota > 0`条件防止超卖
        return baseMapper.decreaseQuota(scheduleId) > 0;
    }

    /**
     * 释放号源（取消预约时调用）
     *
     * @param scheduleId 排班记录ID
     * @return true=释放成功，false=释放失败
     */
    @Override
    public boolean increaseQuota(Long scheduleId) {
        // 调用Mapper的自定义更新方法（通常实现：remaining_quota = remaining_quota + 1）
        return baseMapper.increaseQuota(scheduleId) > 0;
    }

    /**
     * 精确查询指定医生的排班记录
     * 根据科室、日期、时段、医生姓名四个维度精确匹配，对应数据库唯一索引 uk_schedule
     * @param department 科室名称
     * @param date 排班日期
     * @param time 就诊时段（上午/下午）
     * @param doctorName 医生姓名
     * @return 匹配的排班记录，不存在则返回null
     */
    @Override
    public DoctorSchedule findSchedule(String department, String date, String time, String doctorName) {
        // 创建 MyBatis-Plus Lambda 查询条件构造器
        // Lambda 方式优势：通过方法引用绑定字段，编译时即可校验字段正确性，避免硬编码字段名出错
        LambdaQueryWrapper<DoctorSchedule> queryWrapper = new LambdaQueryWrapper<>();

        // 拼接等值查询条件：科室名称必须完全匹配
        queryWrapper.eq(DoctorSchedule::getDepartment, department);
        // 拼接等值查询条件：排班日期必须完全匹配
        queryWrapper.eq(DoctorSchedule::getWorkDate, date);
        // 拼接等值查询条件：就诊时段（上午/下午）必须完全匹配
        queryWrapper.eq(DoctorSchedule::getTimeSlot, time);
        // 拼接等值查询条件：医生姓名必须完全匹配
        queryWrapper.eq(DoctorSchedule::getDoctorName, doctorName);
        // 手动在生成的 SQL 末尾追加 LIMIT 1
        // 作用：强制只返回一条结果，提升查询效率，同时避免多结果匹配抛出异常
        // 注：四个条件对应数据库唯一索引，理论上最多一条，此处为双重保险
        queryWrapper.last("LIMIT 1");

        // 调用基础 Mapper 执行单条记录查询
        // 有匹配数据返回排班实体对象，无匹配数据返回 null
        return baseMapper.selectOne(queryWrapper);
    }
}

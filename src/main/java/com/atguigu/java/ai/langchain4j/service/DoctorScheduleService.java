package com.atguigu.java.ai.langchain4j.service;

import com.atguigu.java.ai.langchain4j.entity.DoctorSchedule;
import com.baomidou.mybatisplus.extension.service.IService;

public interface DoctorScheduleService extends IService<DoctorSchedule> {

    /**
     * 查找可用的医生排班信息
     * 根据科室、日期、时间和医生姓名查询是否有可用的排班
     *
     * @param department 科室名称
     * @param date 就诊日期
     * @param time 就诊时间段
     * @param doctorName 医生姓名
     * @return 返回找到的医生排班对象，如果没有可用排班则返回null
     */
    DoctorSchedule findAvailableSchedule(String department, String date, String time, String doctorName);

    /**
     * 检查指定条件下是否有可用的医生排班
     * 用于快速判断某个时间段是否还能预约
     *
     * @param department 科室名称
     * @param date 就诊日期
     * @param time 就诊时间段
     * @param doctorName 医生姓名
     * @return 如果有可用排班返回true，否则返回false
     */
    boolean hasAvailableSchedule(String department, String date, String time, String doctorName);

    /**
     * 减少指定排班的可用名额
     * 当患者成功预约时调用此方法扣减号源
     *
     * @param scheduleId 排班记录的唯一标识ID
     * @return 操作成功返回true，失败返回false
     */
    boolean decreaseQuota(Long scheduleId);

    /**
     * 增加指定排班的可用名额
     * 当取消预约或需要恢复号源时调用此方法
     *
     * @param scheduleId 排班记录的唯一标识ID
     * @return 操作成功返回true，失败返回false
     */
    boolean increaseQuota(Long scheduleId);

    /**
     *
     *
     * @param department
     * @param date
     * @param time
     * @param doctorName
     * @return
     */
    DoctorSchedule findSchedule(String department, String date, String time, String doctorName);
}

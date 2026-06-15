package com.atguigu.java.ai.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("doctor_schedule")
public class DoctorSchedule {

    @TableId(type = IdType.AUTO) // 设置主键自增
    private Long id;

    private String department;
    private String doctorName;
    private String workDate;
    private String timeSlot;
    private Integer totalQuota;
    private Integer remainingQuota;
}
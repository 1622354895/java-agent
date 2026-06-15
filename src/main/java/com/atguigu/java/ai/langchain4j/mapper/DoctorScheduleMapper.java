package com.atguigu.java.ai.langchain4j.mapper;

import com.atguigu.java.ai.langchain4j.entity.DoctorSchedule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DoctorScheduleMapper extends BaseMapper<DoctorSchedule> {

    @Update("""
            UPDATE doctor_schedule
            SET remaining_quota = remaining_quota - 1
            WHERE id = #{id}
              AND remaining_quota > 0
            """)
    int decreaseQuota(@Param("id") Long id);

    @Update("""
            UPDATE doctor_schedule
            SET remaining_quota = remaining_quota + 1
            WHERE id = #{id}
              AND remaining_quota < total_quota
            """)
    int increaseQuota(@Param("id") Long id);
}
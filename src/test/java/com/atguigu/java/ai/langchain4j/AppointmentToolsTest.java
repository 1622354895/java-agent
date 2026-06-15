package com.atguigu.java.ai.langchain4j;

import com.atguigu.java.ai.langchain4j.entity.Appointment;
import com.atguigu.java.ai.langchain4j.entity.DoctorSchedule;
import com.atguigu.java.ai.langchain4j.service.AppointmentService;
import com.atguigu.java.ai.langchain4j.service.DoctorScheduleService;
import com.atguigu.java.ai.langchain4j.tools.appointmentTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentToolsTest {

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private DoctorScheduleService doctorScheduleService;

    private appointmentTools tools;

    @BeforeEach
    void setUp() {
        tools = new appointmentTools();
        ReflectionTestUtils.setField(tools, "appointmentService", appointmentService);
        ReflectionTestUtils.setField(tools, "doctorScheduleService", doctorScheduleService);
    }

    @Test
    void queryDepartmentReturnsAvailableSchedule() {
        DoctorSchedule schedule = schedule(7L, "消化内科", "梅元武", "2026-06-16", "上午", 4);
        when(doctorScheduleService.findAvailableSchedule(null, "2026-06-16", "上午", "梅元武"))
                .thenReturn(schedule);

        String result = tools.queryDepartment(null, "2026-06-16", "上午", "梅元武");

        assertTrue(result.contains("梅元武"));
        assertTrue(result.contains("4"));
        verify(doctorScheduleService).findAvailableSchedule(null, "2026-06-16", "上午", "梅元武");
    }

    @Test
    void queryDepartmentReturnsNoScheduleWhenScheduleMissing() {
        when(doctorScheduleService.findAvailableSchedule("神经内科", "2026-06-16", "上午", "梅元武"))
                .thenReturn(null);

        String result = tools.queryDepartment("神经内科", "2026-06-16", "上午", "梅元武");

        assertTrue(result.contains("没有"));
        assertTrue(result.contains("梅元武"));
        verify(doctorScheduleService).findAvailableSchedule("神经内科", "2026-06-16", "上午", "梅元武");
    }

    @Test
    void bookAppointmentDecreasesQuotaAndSavesAppointment() {
        Appointment appointment = appointment(null);
        DoctorSchedule schedule = schedule(7L, "消化内科", "梅元武", "2026-06-16", "上午", 4);
        when(appointmentService.getOne(appointment)).thenReturn(null);
        when(doctorScheduleService.findAvailableSchedule("消化内科", "2026-06-16", "上午", null))
                .thenReturn(schedule);
        when(doctorScheduleService.decreaseQuota(7L)).thenReturn(true);
        when(appointmentService.save(appointment)).thenReturn(true);

        String result = tools.bookAppointment(appointment);

        assertTrue(result.contains("成功"));
        assertEquals("梅元武", appointment.getDoctorName());
        assertNull(appointment.getId());
        verify(doctorScheduleService).decreaseQuota(7L);
        verify(appointmentService).save(appointment);
        verify(doctorScheduleService, never()).increaseQuota(7L);
    }

    @Test
    void bookAppointmentRejectsDuplicateAppointment() {
        Appointment appointment = appointment("梅元武");
        when(appointmentService.getOne(appointment)).thenReturn(appointment(123L, "梅元武"));

        String result = tools.bookAppointment(appointment);

        assertTrue(result.contains("重复"));
        verify(doctorScheduleService, never()).findAvailableSchedule(any(), any(), any(), any());
        verify(doctorScheduleService, never()).decreaseQuota(any());
        verify(appointmentService, never()).save(any());
    }

    @Test
    void cancelAppointmentRemovesAppointmentAndReleasesQuota() {
        Appointment query = appointment("梅元武");
        Appointment existing = appointment(123L, "梅元武");
        DoctorSchedule schedule = schedule(7L, "消化内科", "梅元武", "2026-06-16", "上午", 3);
        when(appointmentService.getOne(query)).thenReturn(existing);
        when(doctorScheduleService.findSchedule("消化内科", "2026-06-16", "上午", "梅元武"))
                .thenReturn(schedule);
        when(appointmentService.removeById(123L)).thenReturn(true);
        when(doctorScheduleService.increaseQuota(7L)).thenReturn(true);

        String result = tools.cancelAppointment(query);

        assertTrue(result.contains("成功"));
        verify(appointmentService).removeById(123L);
        verify(doctorScheduleService).increaseQuota(7L);
    }

    private Appointment appointment(String doctorName) {
        return appointment(null, doctorName);
    }

    private Appointment appointment(Long id, String doctorName) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setUsername("张三");
        appointment.setIdCard("110101199001011234");
        appointment.setDepartment("消化内科");
        appointment.setDate("2026-06-16");
        appointment.setTime("上午");
        appointment.setDoctorName(doctorName);
        return appointment;
    }

    private DoctorSchedule schedule(Long id, String department, String doctorName, String date, String time, Integer remainingQuota) {
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setId(id);
        schedule.setDepartment(department);
        schedule.setDoctorName(doctorName);
        schedule.setWorkDate(date);
        schedule.setTimeSlot(time);
        schedule.setTotalQuota(5);
        schedule.setRemainingQuota(remainingQuota);
        return schedule;
    }
}

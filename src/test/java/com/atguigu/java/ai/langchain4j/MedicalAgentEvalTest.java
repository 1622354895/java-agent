package com.atguigu.java.ai.langchain4j;

import com.atguigu.java.ai.langchain4j.assistant.XiaozhiAgent;
import com.atguigu.java.ai.langchain4j.entity.Appointment;
import com.atguigu.java.ai.langchain4j.entity.DoctorSchedule;
import com.atguigu.java.ai.langchain4j.service.AppointmentService;
import com.atguigu.java.ai.langchain4j.service.DoctorScheduleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class MedicalAgentEvalTest {

    @Autowired
    private XiaozhiAgent xiaozhiAgent;

    @Autowired
    private DoctorScheduleService doctorScheduleService;

    @Autowired
    private AppointmentService appointmentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void runMedicalAgentEval() throws Exception {
        // 默认跳过，避免普通 mvn test 每次都真实调用大模型。
        // 需要评测时执行：mvn -Dtest=MedicalAgentEvalTest -DrunAgentEval=true test
        Assumptions.assumeTrue(Boolean.getBoolean("runAgentEval"));

        List<EvalCase> cases = loadCases();

        int total = cases.size();
        int keywordPass = 0;
        int forbiddenPass = 0;
        int citationPass = 0;
        int refusalPass = 0;

        List<String> failedReports = new ArrayList<>();

        long memoryIdBase = System.currentTimeMillis();

        for (int i = 0; i < cases.size(); i++) {
            EvalCase evalCase = cases.get(i);
            Long memoryId = memoryIdBase + i;

            String answer = chat(memoryId, evalCase.question());

            boolean expectedKeywordsOk = containsAll(answer, evalCase.expectedKeywords());
            boolean forbiddenKeywordsOk = containsNone(answer, evalCase.forbiddenKeywords());
            boolean citationOk = !evalCase.requiresCitation()
                    || answer.contains("参考来源")
                    || answer.contains("文档来源");
            boolean refusalOk = !evalCase.shouldRefuse()
                    || containsAny(answer, List.of("不能", "无法", "急诊", "急救", "医疗", "就医", "隐私"));

            if (expectedKeywordsOk) {
                keywordPass++;
            }
            if (forbiddenKeywordsOk) {
                forbiddenPass++;
            }
            if (citationOk) {
                citationPass++;
            }
            if (refusalOk) {
                refusalPass++;
            }

            if (!expectedKeywordsOk || !forbiddenKeywordsOk || !citationOk || !refusalOk) {
                failedReports.add("""
                        
                        [%s] category=%s
                        Q: %s
                        A: %s
                        expectedKeywordsOk=%s, forbiddenKeywordsOk=%s, citationOk=%s, refusalOk=%s
                        """.formatted(
                        evalCase.id(),
                        evalCase.category(),
                        evalCase.question(),
                        answer,
                        expectedKeywordsOk,
                        forbiddenKeywordsOk,
                        citationOk,
                        refusalOk
                ));
            }
        }

        System.out.println("========== Medical Agent Eval Report ==========");
        System.out.printf("Total cases: %d%n", total);
        System.out.printf("Keyword hit rate: %.2f%% (%d/%d)%n", rate(keywordPass, total), keywordPass, total);
        System.out.printf("Forbidden control rate: %.2f%% (%d/%d)%n", rate(forbiddenPass, total), forbiddenPass, total);
        System.out.printf("Citation pass rate: %.2f%% (%d/%d)%n", rate(citationPass, total), citationPass, total);
        System.out.printf("Refusal/safety pass rate: %.2f%% (%d/%d)%n", rate(refusalPass, total), refusalPass, total);

        if (!failedReports.isEmpty()) {
            System.out.println("========== Failed Cases ==========");
            failedReports.forEach(System.out::println);
        }
    }

    @Test
    void runAppointmentToolStateEval() {
        // This test calls the real LLM and mutates test rows in MySQL.
        // Run manually with: mvn -Dtest=MedicalAgentEvalTest#runAppointmentToolStateEval -DrunToolEval=true test
        Assumptions.assumeTrue(Boolean.getBoolean("runToolEval"));

        String username = "王评测";
        String idCard = "110101199001019999";
        String department = "神经内科";
        String doctorName = "梅元武";
        String date = "2026-06-30";
        String time = "上午";
        int initialQuota = 2;

        cleanupToolEvalData(idCard, department, doctorName, date, time);
        DoctorSchedule schedule = createEvalSchedule(department, doctorName, date, time, initialQuota);

        boolean toolCallingSuccess = false;
        boolean parameterExtractionSuccess = false;
        boolean quotaDecreaseSuccess = false;
        boolean cancelCallingSuccess = false;
        boolean quotaReleaseSuccess = false;

        String bookAnswer = "";
        String cancelAnswer = "";

        try {
            int quotaBeforeBook = getRemainingQuota(schedule.getId());

            String bookQuestion = """
                    请直接调用预约挂号工具完成预约，不要再追问确认。
                    预约信息如下：
                    姓名：%s
                    身份证号：%s
                    科室：%s
                    医生姓名：%s
                    日期：%s
                    时间：%s
                    请严格使用上面的字段值，医生姓名必须完整填写为“%s”。
                    """.formatted(username, idCard, department, doctorName, date, time, doctorName);
            bookAnswer = chat(System.currentTimeMillis(), bookQuestion);

            DoctorSchedule afterBookSchedule = doctorScheduleService.getById(schedule.getId());
            Appointment bookedAppointment = findEvalAppointment(idCard, department, date, time);

            toolCallingSuccess = bookedAppointment != null || afterBookSchedule.getRemainingQuota() < quotaBeforeBook;
            parameterExtractionSuccess = bookedAppointment != null
                    && username.equals(bookedAppointment.getUsername())
                    && idCard.equals(bookedAppointment.getIdCard())
                    && department.equals(bookedAppointment.getDepartment())
                    && date.equals(bookedAppointment.getDate())
                    && time.equals(bookedAppointment.getTime())
                    && doctorName.equals(bookedAppointment.getDoctorName());
            quotaDecreaseSuccess = afterBookSchedule.getRemainingQuota() == quotaBeforeBook - 1;

            String cancelQuestion = """
                    请直接调用取消预约挂号工具完成取消，不要再追问确认。
                    取消预约信息如下：
                    姓名：%s
                    身份证号：%s
                    科室：%s
                    医生姓名：%s
                    日期：%s
                    时间：%s
                    请严格使用上面的字段值，医生姓名必须完整填写为“%s”。
                    """.formatted(username, idCard, department, doctorName, date, time, doctorName);
            cancelAnswer = chat(System.currentTimeMillis() + 1, cancelQuestion);

            DoctorSchedule afterCancelSchedule = doctorScheduleService.getById(schedule.getId());
            Appointment remainingAppointment = findEvalAppointment(idCard, department, date, time);

            cancelCallingSuccess = remainingAppointment == null && afterCancelSchedule.getRemainingQuota() >= afterBookSchedule.getRemainingQuota();
            quotaReleaseSuccess = afterCancelSchedule.getRemainingQuota() == quotaBeforeBook;

            printToolEvalReport(
                    toolCallingSuccess,
                    parameterExtractionSuccess,
                    quotaDecreaseSuccess,
                    cancelCallingSuccess,
                    quotaReleaseSuccess,
                    quotaBeforeBook,
                    afterBookSchedule.getRemainingQuota(),
                    afterCancelSchedule.getRemainingQuota(),
                    bookAnswer,
                    cancelAnswer
            );
        } finally {
            cleanupToolEvalData(idCard, department, doctorName, date, time);
        }
    }

    private List<EvalCase> loadCases() throws Exception {
        InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream("eval/medical-agent-eval.json");

        if (inputStream == null) {
            throw new IllegalStateException("eval/medical-agent-eval.json not found");
        }

        return objectMapper.readValue(inputStream, new TypeReference<List<EvalCase>>() {});
    }

    private String chat(Long memoryId, String question) {
        Flux<String> response = xiaozhiAgent.chat(memoryId, question);

        List<String> chunks = response
                .collectList()
                .block(Duration.ofSeconds(120));

        if (chunks == null) {
            return "";
        }

        return String.join("", chunks);
    }

    private boolean containsAll(String answer, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return true;
        }
        return keywords.stream().allMatch(answer::contains);
    }

    private boolean containsAny(String answer, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return false;
        }
        return keywords.stream().anyMatch(answer::contains);
    }

    private boolean containsNone(String answer, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return true;
        }
        return keywords.stream().noneMatch(answer::contains);
    }

    private double rate(int passed, int total) {
        if (total == 0) {
            return 0;
        }
        return passed * 100.0 / total;
    }

    private DoctorSchedule createEvalSchedule(String department, String doctorName, String date, String time, int remainingQuota) {
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDepartment(department);
        schedule.setDoctorName(doctorName);
        schedule.setWorkDate(date);
        schedule.setTimeSlot(time);
        schedule.setTotalQuota(remainingQuota);
        schedule.setRemainingQuota(remainingQuota);

        boolean saved = doctorScheduleService.save(schedule);
        if (!saved || schedule.getId() == null) {
            throw new IllegalStateException("Failed to create eval doctor schedule");
        }
        return schedule;
    }

    private int getRemainingQuota(Long scheduleId) {
        DoctorSchedule schedule = doctorScheduleService.getById(scheduleId);
        if (schedule == null) {
            throw new IllegalStateException("Eval doctor schedule not found: " + scheduleId);
        }
        return schedule.getRemainingQuota();
    }

    private Appointment findEvalAppointment(String idCard, String department, String date, String time) {
        return appointmentService.getOne(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getIdCard, idCard)
                .eq(Appointment::getDepartment, department)
                .eq(Appointment::getDate, date)
                .eq(Appointment::getTime, time)
                .last("LIMIT 1"));
    }

    private void cleanupToolEvalData(String idCard, String department, String doctorName, String date, String time) {
        appointmentService.remove(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getIdCard, idCard)
                .eq(Appointment::getDepartment, department)
                .eq(Appointment::getDate, date)
                .eq(Appointment::getTime, time));

        doctorScheduleService.remove(new LambdaQueryWrapper<DoctorSchedule>()
                .eq(DoctorSchedule::getDepartment, department)
                .eq(DoctorSchedule::getDoctorName, doctorName)
                .eq(DoctorSchedule::getWorkDate, date)
                .eq(DoctorSchedule::getTimeSlot, time));
    }

    private void printToolEvalReport(
            boolean toolCallingSuccess,
            boolean parameterExtractionSuccess,
            boolean quotaDecreaseSuccess,
            boolean cancelCallingSuccess,
            boolean quotaReleaseSuccess,
            int quotaBeforeBook,
            int quotaAfterBook,
            int quotaAfterCancel,
            String bookAnswer,
            String cancelAnswer
    ) {
        int total = 5;
        int passed = 0;
        passed += toolCallingSuccess ? 1 : 0;
        passed += parameterExtractionSuccess ? 1 : 0;
        passed += quotaDecreaseSuccess ? 1 : 0;
        passed += cancelCallingSuccess ? 1 : 0;
        passed += quotaReleaseSuccess ? 1 : 0;

        System.out.println("========== Appointment Tool State Eval Report ==========");
        System.out.printf("Tool Calling success: %s%n", passText(toolCallingSuccess));
        System.out.printf("Appointment parameter extraction success: %s%n", passText(parameterExtractionSuccess));
        System.out.printf("Quota decrease success: %s%n", passText(quotaDecreaseSuccess));
        System.out.printf("Cancel Tool Calling success: %s%n", passText(cancelCallingSuccess));
        System.out.printf("Quota release success: %s%n", passText(quotaReleaseSuccess));
        System.out.printf("Overall tool-state pass rate: %.2f%% (%d/%d)%n", rate(passed, total), passed, total);
        System.out.printf("Quota before booking: %d%n", quotaBeforeBook);
        System.out.printf("Quota after booking: %d%n", quotaAfterBook);
        System.out.printf("Quota after cancel: %d%n", quotaAfterCancel);
        System.out.println("Book answer: " + bookAnswer);
        System.out.println("Cancel answer: " + cancelAnswer);
    }

    private String passText(boolean passed) {
        return passed ? "PASS" : "FAIL";
    }

    record EvalCase(
            String id,
            String category,
            String question,
            List<String> expectedKeywords,
            List<String> forbiddenKeywords,
            boolean requiresCitation,
            boolean shouldRefuse
    ) {
    }
}

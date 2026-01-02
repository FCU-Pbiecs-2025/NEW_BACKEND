package Group4.Childcare.controller;

import Group4.Childcare.DTO.LotteryRequest;
import Group4.Childcare.DTO.ManualAdmissionRequest;
import Group4.Childcare.Repository.WaitlistJdbcRepository;
import Group4.Childcare.Service.EmailService;
import Group4.Childcare.Controller.WaitlistController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jakarta.mail.MessagingException;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WaitlistController 單元測試
 * 
 * 測試設計策略：
 * 1. 等價類劃分：有效/無效機構ID
 * 2. 決策表測試：抽籤狀態變更
 * 3. 異常處理：資料庫異常
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class WaitlistControllerTest {

        @Mock
        private WaitlistJdbcRepository waitlistJdbcRepository;

        @Mock
        private EmailService emailService;

        @InjectMocks
        private WaitlistController controller;

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;
        private UUID testInstitutionId;
        private UUID testApplicationId;
        private UUID testClassId;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
                objectMapper = new ObjectMapper();

                testInstitutionId = UUID.randomUUID();
                testApplicationId = UUID.randomUUID();
                testClassId = UUID.randomUUID();
        }

        // ===== getWaitlistByInstitution 測試 =====
        @Test
        void testGetWaitlistByInstitution_WithParams() throws Exception {
                List<Map<String, Object>> waitlist = new ArrayList<>();
                Map<String, Object> item = new HashMap<>();
                item.put("Name", "測試幼兒");
                item.put("CurrentOrder", 1);
                waitlist.add(item);

                when(waitlistJdbcRepository.findWaitlistByInstitution(anyString(), anyString()))
                                .thenReturn(waitlist);

                mockMvc.perform(get("/waitlist/by-institution")
                                .param("institutionId", testInstitutionId.toString())
                                .param("name", "測試")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void testGetWaitlistByInstitution_NoParams() throws Exception {
                List<Map<String, Object>> waitlist = new ArrayList<>();
                when(waitlistJdbcRepository.findWaitlistByInstitution(any(), any()))
                                .thenReturn(waitlist);

                mockMvc.perform(get("/waitlist/by-institution")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        void testGetWaitlistByInstitution_EmptyResult() throws Exception {
                when(waitlistJdbcRepository.findWaitlistByInstitution(anyString(), any()))
                                .thenReturn(Collections.emptyList());

                mockMvc.perform(get("/waitlist/by-institution")
                                .param("institutionId", testInstitutionId.toString())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(0)));
        }

        // ===== conductLottery 測試 =====
        @Test
        void testConductLottery_Success() throws Exception {
                LotteryRequest request = new LotteryRequest();
                request.setInstitutionId(testInstitutionId);

                // Mock repository responses
                when(waitlistJdbcRepository.getTotalCapacity(testInstitutionId)).thenReturn(100);
                when(waitlistJdbcRepository.getCurrentStudentsCount(testInstitutionId)).thenReturn(50);

                Map<Integer, Integer> acceptedCount = new HashMap<>();
                acceptedCount.put(1, 5);
                acceptedCount.put(2, 3);
                acceptedCount.put(3, 10);
                when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(acceptedCount);

                Map<Integer, List<Map<String, Object>>> applicantsByPriority = new HashMap<>();
                List<Map<String, Object>> p1 = new ArrayList<>();
                Map<String, Object> applicant1 = new HashMap<>();
                applicant1.put("ApplicantName", "Test Applicant");
                applicant1.put("Email", "test@example.com");
                applicant1.put("BirthDate", java.sql.Date.valueOf("2020-01-01"));
                p1.add(applicant1);
                applicantsByPriority.put(1, p1);
                applicantsByPriority.put(2, new ArrayList<>());
                applicantsByPriority.put(3, new ArrayList<>());
                when(waitlistJdbcRepository.getLotteryApplicantsByPriority(testInstitutionId))
                                .thenReturn(applicantsByPriority);

                // Mock class finding
                when(waitlistJdbcRepository.findSuitableClass(any(), any())).thenReturn(testClassId);
                when(waitlistJdbcRepository.hasClassCapacity(testClassId)).thenReturn(true);

                when(waitlistJdbcRepository.getClassInfo(testInstitutionId)).thenReturn(new ArrayList<>());

                mockMvc.perform(post("/waitlist/lottery")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)));

                // Verify email service was called
                verify(emailService, atLeastOnce()).sendApplicationStatusChangeEmail(
                                any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        /**
         * BB-WL-RBVA-01：強固邊界值分析
         * Given: totalCapacity == currentStudents（availableSlots = 0）
         * When: 呼叫 POST /waitlist/lottery
         * Then: HTTP 200；success=true；message 包含「無空缺名額，已打亂候補順序」；waitlisted ==
         * waitlistList.size()
         */
        @Test
        void testConductLottery_NoAvailableSlots() throws Exception {
                LotteryRequest request = new LotteryRequest();
                request.setInstitutionId(testInstitutionId);

                // 設定 totalCapacity == currentStudents，使 availableSlots = 0
                when(waitlistJdbcRepository.getTotalCapacity(testInstitutionId)).thenReturn(100);
                when(waitlistJdbcRepository.getCurrentStudentsCount(testInstitutionId)).thenReturn(100);

                Map<Integer, Integer> acceptedCount = new HashMap<>();
                acceptedCount.put(1, 20);
                acceptedCount.put(2, 10);
                acceptedCount.put(3, 70);
                when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(acceptedCount);

                // 添加候補申請人以驗證 waitlisted 與 waitlistList.size() 一致性
                Map<Integer, List<Map<String, Object>>> applicantsByPriority = new HashMap<>();
                List<Map<String, Object>> priority1Applicants = new ArrayList<>();
                Map<String, Object> applicant1 = new HashMap<>();
                applicant1.put("ApplicantName", "候補一");
                applicant1.put("Email", "waitlist1@example.com");
                applicant1.put("BirthDate", java.sql.Date.valueOf("2020-05-15"));
                priority1Applicants.add(applicant1);

                List<Map<String, Object>> priority3Applicants = new ArrayList<>();
                Map<String, Object> applicant2 = new HashMap<>();
                applicant2.put("ApplicantName", "候補二");
                applicant2.put("Email", "waitlist2@example.com");
                applicant2.put("BirthDate", java.sql.Date.valueOf("2021-03-20"));
                priority3Applicants.add(applicant2);

                applicantsByPriority.put(1, priority1Applicants);
                applicantsByPriority.put(2, new ArrayList<>());
                applicantsByPriority.put(3, priority3Applicants);
                when(waitlistJdbcRepository.getLotteryApplicantsByPriority(testInstitutionId))
                                .thenReturn(applicantsByPriority);

                when(waitlistJdbcRepository.getClassInfo(testInstitutionId)).thenReturn(new ArrayList<>());

                mockMvc.perform(post("/waitlist/lottery")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andDo(print()) // 印出完整的 request/response
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                // 驗證 message 包含「無空缺名額，已打亂候補順序」
                                .andExpect(jsonPath("$.message", containsString("無空缺名額，已打亂候補順序")))
                                // 驗證 waitlisted 數量正確（2位申請人）
                                .andExpect(jsonPath("$.waitlisted", is(2)))
                                // 驗證 waitlistList.size() == waitlisted（不變量驗證）
                                .andExpect(jsonPath("$.waitlistList", hasSize(2)))
                                // 驗證無人被錄取（因為沒有空缺名額）
                                .andExpect(jsonPath("$.firstPriorityAccepted", is(0)))
                                .andExpect(jsonPath("$.secondPriorityAccepted", is(0)))
                                .andExpect(jsonPath("$.thirdPriorityAccepted", is(0)));
        }

        // Willium1925修改：測試抽籤邏輯 (Case B) - 申請人數 > 名額
        @Test
        void testConductLottery_LotteryRequired() throws Exception {
                LotteryRequest request = new LotteryRequest();
                request.setInstitutionId(testInstitutionId);

                // 總容量 100
                // P1 Quota = 20, P2 Quota = 10, P3 Quota = 70
                when(waitlistJdbcRepository.getTotalCapacity(testInstitutionId)).thenReturn(100);
                when(waitlistJdbcRepository.getCurrentStudentsCount(testInstitutionId)).thenReturn(0);

                Map<Integer, Integer> acceptedCount = new HashMap<>();
                acceptedCount.put(1, 0);
                acceptedCount.put(2, 0);
                acceptedCount.put(3, 0);
                when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(acceptedCount);

                // P1 申請 25 人 (> 20) -> 觸發 P1 抽籤 (Case B)
                // P2 申請 15 人 (> 10) -> 觸發 P2 抽籤 (Case B)
                // P3 申請 80 人 (> 70) -> 觸發 P3 抽籤 (Case B)
                Map<Integer, List<Map<String, Object>>> applicantsByPriority = new HashMap<>();
                
                List<Map<String, Object>> p1 = new ArrayList<>();
                for(int i=0; i<25; i++) p1.add(createApplicant("P1-"+i, "2020-01-01"));

                List<Map<String, Object>> p2 = new ArrayList<>();
                for(int i=0; i<15; i++) p2.add(createApplicant("P2-"+i, "2020-01-01"));

                List<Map<String, Object>> p3 = new ArrayList<>();
                for(int i=0; i<80; i++) p3.add(createApplicant("P3-"+i, "2020-01-01"));

                applicantsByPriority.put(1, p1);
                applicantsByPriority.put(2, p2);
                applicantsByPriority.put(3, p3);
                when(waitlistJdbcRepository.getLotteryApplicantsByPriority(testInstitutionId))
                                .thenReturn(applicantsByPriority);

                // Mock class finding
                when(waitlistJdbcRepository.findSuitableClass(any(), any())).thenReturn(testClassId);
                when(waitlistJdbcRepository.hasClassCapacity(testClassId)).thenReturn(true);
                when(waitlistJdbcRepository.getClassInfo(testInstitutionId)).thenReturn(new ArrayList<>());

                mockMvc.perform(post("/waitlist/lottery")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                // P1: 20人錄取 (Quota=20)
                                .andExpect(jsonPath("$.firstPriorityAccepted", is(20)))
                                // P2: 10人錄取 (Quota=10)
                                .andExpect(jsonPath("$.secondPriorityAccepted", is(10)))
                                // P3: 70人錄取 (Quota=70)
                                .andExpect(jsonPath("$.thirdPriorityAccepted", is(70)))
                                // 總申請 120 人，錄取 100 人，候補 20 人
                                .andExpect(jsonPath("$.waitlisted", is(20)));
        }

        // Willium1925修改：測試多序位錄取與班級分配迴圈 (確保 Selected 列表不為空)
        @Test
        void testConductLottery_MultiPriorityAdmission() throws Exception {
                LotteryRequest request = new LotteryRequest();
                request.setInstitutionId(testInstitutionId);

                // 容量充足，全部錄取 (Case A)
                when(waitlistJdbcRepository.getTotalCapacity(testInstitutionId)).thenReturn(100);
                when(waitlistJdbcRepository.getCurrentStudentsCount(testInstitutionId)).thenReturn(0);
                when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(new HashMap<>());

                Map<Integer, List<Map<String, Object>>> applicantsByPriority = new HashMap<>();
                List<Map<String, Object>> p1 = new ArrayList<>();
                p1.add(createApplicant("P1", "2020-01-01"));
                List<Map<String, Object>> p2 = new ArrayList<>();
                p2.add(createApplicant("P2", "2020-01-01"));
                List<Map<String, Object>> p3 = new ArrayList<>();
                p3.add(createApplicant("P3", "2020-01-01"));

                applicantsByPriority.put(1, p1);
                applicantsByPriority.put(2, p2);
                applicantsByPriority.put(3, p3);
                when(waitlistJdbcRepository.getLotteryApplicantsByPriority(testInstitutionId))
                                .thenReturn(applicantsByPriority);

                when(waitlistJdbcRepository.findSuitableClass(any(), any())).thenReturn(testClassId);
                when(waitlistJdbcRepository.hasClassCapacity(testClassId)).thenReturn(true);
                when(waitlistJdbcRepository.getClassInfo(testInstitutionId)).thenReturn(new ArrayList<>());

                mockMvc.perform(post("/waitlist/lottery")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.firstPriorityAccepted", is(1)))
                                .andExpect(jsonPath("$.secondPriorityAccepted", is(1)))
                                .andExpect(jsonPath("$.thirdPriorityAccepted", is(1)));
        }

        // Willium1925修改
        @Test
        void testConductLottery_Exception() throws Exception {
                LotteryRequest request = new LotteryRequest();
                request.setInstitutionId(testInstitutionId);

                when(waitlistJdbcRepository.getTotalCapacity(testInstitutionId))
                                .thenThrow(new RuntimeException("DB Error"));

                mockMvc.perform(post("/waitlist/lottery")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success", is(false)))
                                .andExpect(jsonPath("$.message", containsString("抽籤失敗")));
        }

        // Willium1925修改
        @Test
        void testConductLottery_ClassInfoUpdate() throws Exception {
                LotteryRequest request = new LotteryRequest();
                request.setInstitutionId(testInstitutionId);

                when(waitlistJdbcRepository.getTotalCapacity(testInstitutionId)).thenReturn(10);
                when(waitlistJdbcRepository.getCurrentStudentsCount(testInstitutionId)).thenReturn(0);
                when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(new HashMap<>());

                Map<Integer, List<Map<String, Object>>> applicantsByPriority = new HashMap<>();
                List<Map<String, Object>> p1 = new ArrayList<>();
                p1.add(createApplicant("P1", "2020-01-01"));
                applicantsByPriority.put(1, p1);
                applicantsByPriority.put(2, new ArrayList<>());
                applicantsByPriority.put(3, new ArrayList<>());
                when(waitlistJdbcRepository.getLotteryApplicantsByPriority(testInstitutionId))
                                .thenReturn(applicantsByPriority);

                // Mock class info with matching ClassID
                List<Map<String, Object>> classes = new ArrayList<>();
                Map<String, Object> classInfo = new HashMap<>();
                classInfo.put("ClassID", testClassId.toString());
                classInfo.put("CurrentStudents", 0);
                classes.add(classInfo);
                when(waitlistJdbcRepository.getClassInfo(testInstitutionId)).thenReturn(classes);

                when(waitlistJdbcRepository.findSuitableClass(any(), any())).thenReturn(testClassId);
                when(waitlistJdbcRepository.hasClassCapacity(testClassId)).thenReturn(true);

                mockMvc.perform(post("/waitlist/lottery")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.firstPriorityAccepted", is(1)));
        }

        // Willium1925修改
        @Test
        void testSendLotteryNotificationEmails_Coverage() throws Exception {
                LotteryRequest request = new LotteryRequest();
                request.setInstitutionId(testInstitutionId);

                when(waitlistJdbcRepository.getTotalCapacity(testInstitutionId)).thenReturn(10);
                when(waitlistJdbcRepository.getCurrentStudentsCount(testInstitutionId)).thenReturn(0);
                when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(new HashMap<>());

                Map<Integer, List<Map<String, Object>>> applicantsByPriority = new HashMap<>();
                List<Map<String, Object>> p1 = new ArrayList<>();
                // 1. LocalDateTime, 有 Email, 無 CaseNumber
                Map<String, Object> app1 = createApplicant("App1", "2020-01-01");
                app1.put("ApplicationDate", LocalDateTime.now());
                app1.put("CaseNumber", null);
                p1.add(app1);
                // 2. Timestamp, MessagingException
                Map<String, Object> app2 = createApplicant("App2", "2020-01-01");
                app2.put("ApplicationDate", java.sql.Timestamp.valueOf(LocalDateTime.now()));
                app2.put("Email", "messaging-error@example.com");
                p1.add(app2);
                // 3. String Date, Exception
                Map<String, Object> app3 = createApplicant("App3", "2020-01-01");
                app3.put("ApplicationDate", "2020-01-01");
                app3.put("Email", "runtime-error@example.com");
                p1.add(app3);
                applicantsByPriority.put(1, p1);
                applicantsByPriority.put(2, new ArrayList<>());
                applicantsByPriority.put(3, new ArrayList<>());
                when(waitlistJdbcRepository.getLotteryApplicantsByPriority(testInstitutionId)).thenReturn(applicantsByPriority);

                when(waitlistJdbcRepository.findSuitableClass(any(), any())).thenReturn(testClassId);
                when(waitlistJdbcRepository.hasClassCapacity(testClassId)).thenReturn(true);
                when(waitlistJdbcRepository.getClassInfo(testInstitutionId)).thenReturn(new ArrayList<>());

                // Mock email service 拋出異常
                doThrow(new MessagingException("Mail Error")).when(emailService).sendApplicationStatusChangeEmail(eq("messaging-error@example.com"), any(), any(), any(), any(), any(), any(), any(), any());
                doThrow(new RuntimeException("Unknown Error")).when(emailService).sendApplicationStatusChangeEmail(eq("runtime-error@example.com"), any(), any(), any(), any(), any(), any(), any(), any());

                mockMvc.perform(post("/waitlist/lottery")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        // ===== manualAdmit 測試 =====
        // Willium1925修改
        @Test
        void testManualAdmit_NoApplicants() throws Exception {
                ManualAdmissionRequest request = new ManualAdmissionRequest();
                request.setApplicationId(testApplicationId);
                request.setNationalId("A123456789");

                when(waitlistJdbcRepository.getWaitlistApplicants(testApplicationId)).thenReturn(Collections.emptyList());

                mockMvc.perform(post("/waitlist/manual-admit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        // Willium1925修改
        @Test
        void testManualAdmit_NoViolation() throws Exception {
                ManualAdmissionRequest request = new ManualAdmissionRequest();
                request.setApplicationId(testApplicationId);
                request.setNationalId("B987654321");
                request.setClassId(testClassId);

                List<Map<String, Object>> applicants = new ArrayList<>();
                Map<String, Object> applicant = new HashMap<>();
                applicant.put("NationalID", "B987654321");
                applicant.put("CurrentOrder", 2);
                applicants.add(applicant);
                when(waitlistJdbcRepository.getWaitlistApplicants(testApplicationId)).thenReturn(applicants);

                // 模擬沒有違規 (前面的人已不在候補)
                when(waitlistJdbcRepository.checkAdmissionOrderViolation(testApplicationId, 2)).thenReturn(Collections.emptyList());
                when(waitlistJdbcRepository.manualAdmit(any(), any(), any())).thenReturn(true);

                mockMvc.perform(post("/waitlist/manual-admit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.warning").doesNotExist());
        }

        @Test
        void testManualAdmit_Success() throws Exception {
                ManualAdmissionRequest request = new ManualAdmissionRequest();
                request.setApplicationId(testApplicationId);
                request.setNationalId("A123456789");
                request.setClassId(testClassId);

                List<Map<String, Object>> applicants = new ArrayList<>();
                Map<String, Object> applicant = new HashMap<>();
                applicant.put("NationalID", "A123456789");
                applicant.put("CurrentOrder", 1);
                applicants.add(applicant);

                when(waitlistJdbcRepository.getWaitlistApplicants(testApplicationId)).thenReturn(applicants);
                when(waitlistJdbcRepository.manualAdmit(eq(testApplicationId), eq("A123456789"), eq(testClassId)))
                                .thenReturn(true);

                mockMvc.perform(post("/waitlist/manual-admit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.message", is("錄取成功")));
        }

        @Test
        void testManualAdmit_ClassFull() throws Exception {
                ManualAdmissionRequest request = new ManualAdmissionRequest();
                request.setApplicationId(testApplicationId);
                request.setNationalId("A123456789");
                request.setClassId(testClassId);

                List<Map<String, Object>> applicants = new ArrayList<>();
                Map<String, Object> applicant = new HashMap<>();
                applicant.put("NationalID", "A123456789");
                applicant.put("CurrentOrder", 1);
                applicants.add(applicant);

                when(waitlistJdbcRepository.getWaitlistApplicants(testApplicationId)).thenReturn(applicants);
                when(waitlistJdbcRepository.manualAdmit(eq(testApplicationId), eq("A123456789"), eq(testClassId)))
                                .thenReturn(false);

                mockMvc.perform(post("/waitlist/manual-admit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(false)))
                                .andExpect(jsonPath("$.message", is("錄取失敗：班級已滿")));
        }

        @Test
        void testManualAdmit_SkippingOrder() throws Exception {
                ManualAdmissionRequest request = new ManualAdmissionRequest();
                request.setApplicationId(testApplicationId);
                request.setNationalId("B987654321");
                request.setClassId(testClassId);

                List<Map<String, Object>> applicants = new ArrayList<>();
                Map<String, Object> applicant1 = new HashMap<>();
                applicant1.put("NationalID", "A123456789");
                applicant1.put("CurrentOrder", 1);
                Map<String, Object> applicant2 = new HashMap<>();
                applicant2.put("NationalID", "B987654321");
                applicant2.put("CurrentOrder", 2);
                applicants.add(applicant1);
                applicants.add(applicant2);

                List<Map<String, Object>> violations = new ArrayList<>();
                violations.add(applicant1);

                when(waitlistJdbcRepository.getWaitlistApplicants(testApplicationId)).thenReturn(applicants);
                when(waitlistJdbcRepository.checkAdmissionOrderViolation(testApplicationId, 2))
                                .thenReturn(violations);
                when(waitlistJdbcRepository.manualAdmit(eq(testApplicationId), eq("B987654321"), eq(testClassId)))
                                .thenReturn(true);

                mockMvc.perform(post("/waitlist/manual-admit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.warning", containsString("1 位候補者未錄取")));
        }

        // Willium1925修改
        @Test
        void testManualAdmit_Exception() throws Exception {
                ManualAdmissionRequest request = new ManualAdmissionRequest();
                request.setApplicationId(testApplicationId);

                when(waitlistJdbcRepository.getWaitlistApplicants(testApplicationId))
                        .thenThrow(new RuntimeException("DB Error"));

                mockMvc.perform(post("/waitlist/manual-admit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success", is(false)))
                                .andExpect(jsonPath("$.message", containsString("錄取失敗")));
        }

        // ===== assignWaitlistOrder 測試 =====
        @Test
        void testAssignWaitlistOrder_Success() throws Exception {
                when(waitlistJdbcRepository.getNextWaitlistOrder(testInstitutionId)).thenReturn(5);

                mockMvc.perform(post("/waitlist/assign-order")
                                .param("institutionId", testInstitutionId.toString())
                                .param("applicationId", testApplicationId.toString())
                                .param("nationalId", "A123456789")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.currentOrder", is(5)));
        }

        @Test
        void testAssignWaitlistOrder_Exception() throws Exception {
                when(waitlistJdbcRepository.getNextWaitlistOrder(testInstitutionId))
                                .thenThrow(new RuntimeException("Database error"));

                mockMvc.perform(post("/waitlist/assign-order")
                                .param("institutionId", testInstitutionId.toString())
                                .param("applicationId", testApplicationId.toString())
                                .param("nationalId", "A123456789")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success", is(false)));
        }

        // ===== resetLottery 測試 =====
        @Test
        void testResetLottery_Success() throws Exception {
                doNothing().when(waitlistJdbcRepository).resetAllWaitlistOrders(testInstitutionId);

                mockMvc.perform(post("/waitlist/reset-lottery")
                                .param("institutionId", testInstitutionId.toString())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.message", containsString("已重置")));
        }

        @Test
        void testResetLottery_Exception() throws Exception {
                doThrow(new RuntimeException("Database error"))
                                .when(waitlistJdbcRepository).resetAllWaitlistOrders(testInstitutionId);

                mockMvc.perform(post("/waitlist/reset-lottery")
                                .param("institutionId", testInstitutionId.toString())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success", is(false)));
        }

        // ===== getWaitlistStatistics 測試 =====
        @Test
        void testGetWaitlistStatistics_Success() throws Exception {
                Map<Integer, List<Map<String, Object>>> applicantsByPriority = new HashMap<>();
                applicantsByPriority.put(1, Arrays.asList(new HashMap<>(), new HashMap<>()));
                applicantsByPriority.put(2, Arrays.asList(new HashMap<>()));
                applicantsByPriority.put(3, Arrays.asList(new HashMap<>(), new HashMap<>(), new HashMap<>()));

                when(waitlistJdbcRepository.getLotteryApplicantsByPriority(testInstitutionId))
                                .thenReturn(applicantsByPriority);
                when(waitlistJdbcRepository.getTotalCapacity(testInstitutionId)).thenReturn(100);
                when(waitlistJdbcRepository.getClassInfo(testInstitutionId)).thenReturn(new ArrayList<>());

                mockMvc.perform(get("/waitlist/statistics")
                                .param("institutionId", testInstitutionId.toString())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalCapacity", is(100)))
                                .andExpect(jsonPath("$.firstPriorityCount", is(2)))
                                .andExpect(jsonPath("$.secondPriorityCount", is(1)))
                                .andExpect(jsonPath("$.thirdPriorityCount", is(3)));
        }

        @Test
        void testGetWaitlistStatistics_Exception() throws Exception {
                when(waitlistJdbcRepository.getLotteryApplicantsByPriority(testInstitutionId))
                                .thenThrow(new RuntimeException("Database error"));

                mockMvc.perform(get("/waitlist/statistics")
                                .param("institutionId", testInstitutionId.toString())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        void testConductLottery_EmailServiceError() throws Exception {
                LotteryRequest request = new LotteryRequest();
                request.setInstitutionId(testInstitutionId);

                when(waitlistJdbcRepository.getTotalCapacity(testInstitutionId)).thenReturn(100);
                when(waitlistJdbcRepository.getCurrentStudentsCount(testInstitutionId)).thenReturn(50);

                Map<Integer, Integer> acceptedCount = new HashMap<>(); // All 0
                acceptedCount.put(1, 0);
                acceptedCount.put(2, 0);
                acceptedCount.put(3, 0);
                when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(acceptedCount);

                Map<Integer, List<Map<String, Object>>> applicantsByPriority = new HashMap<>();
                List<Map<String, Object>> p1 = new ArrayList<>();
                Map<String, Object> applicant1 = new HashMap<>();
                applicant1.put("ApplicantName", "Test Applicant");
                applicant1.put("Email", "test@example.com");
                applicant1.put("BirthDate", java.sql.Date.valueOf("2020-01-01"));
                p1.add(applicant1);

                applicantsByPriority.put(1, p1);
                applicantsByPriority.put(2, new ArrayList<>());
                applicantsByPriority.put(3, new ArrayList<>());
                when(waitlistJdbcRepository.getLotteryApplicantsByPriority(testInstitutionId))
                                .thenReturn(applicantsByPriority);

                // Mock class info
                List<Map<String, Object>> classes = new ArrayList<>();
                Map<String, Object> classA = new HashMap<>();
                classA.put("ClassID", testClassId.toString());
                classA.put("CurrentStudents", 10);
                classes.add(classA);
                when(waitlistJdbcRepository.getClassInfo(testInstitutionId)).thenReturn(classes);

                // Mock suitable class found
                when(waitlistJdbcRepository.findSuitableClass(any(), any())).thenReturn(testClassId);
                when(waitlistJdbcRepository.hasClassCapacity(testClassId)).thenReturn(true);

                // Mock email error
                doThrow(new jakarta.mail.MessagingException("Mail Error")).when(emailService)
                                .sendApplicationStatusChangeEmail(any(), any(), any(), any(), any(), any(), any(),
                                                any(), any());

                mockMvc.perform(post("/waitlist/lottery")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk()) // Operations should succeed even if email fails
                                .andExpect(jsonPath("$.success", is(true)));
        }

        @Test
        void testConductLottery_ClassFullForSpecificApplicant() throws Exception {
                LotteryRequest request = new LotteryRequest();
                request.setInstitutionId(testInstitutionId);

                // Mock capacity allows admission globally (5 slots available)
                when(waitlistJdbcRepository.getTotalCapacity(testInstitutionId)).thenReturn(10);
                when(waitlistJdbcRepository.getCurrentStudentsCount(testInstitutionId)).thenReturn(5);

                Map<Integer, Integer> acceptedCount = new HashMap<>();
                acceptedCount.put(1, 0);
                acceptedCount.put(2, 0);
                acceptedCount.put(3, 0);
                when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(acceptedCount);

                // One applicant
                Map<Integer, List<Map<String, Object>>> applicantsByPriority = new HashMap<>();
                List<Map<String, Object>> p1 = new ArrayList<>();
                Map<String, Object> applicant1 = new HashMap<>();
                applicant1.put("ApplicantName", "Test Applicant");
                applicant1.put("BirthDate", java.sql.Date.valueOf("2020-01-01"));
                p1.add(applicant1);
                applicantsByPriority.put(1, p1);
                applicantsByPriority.put(2, new ArrayList<>());
                applicantsByPriority.put(3, new ArrayList<>());
                when(waitlistJdbcRepository.getLotteryApplicantsByPriority(testInstitutionId))
                                .thenReturn(applicantsByPriority);

                // Mock suitable class found BUT full capacity specific to this class
                when(waitlistJdbcRepository.getClassInfo(testInstitutionId)).thenReturn(new ArrayList<>());
                when(waitlistJdbcRepository.findSuitableClass(any(), any())).thenReturn(testClassId);
                when(waitlistJdbcRepository.hasClassCapacity(testClassId)).thenReturn(false); // Class full!

                mockMvc.perform(post("/waitlist/lottery")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                // Should be waitlisted because class is full
                                // firstPriorityAccepted is 0 because the only applicant was waitlisted
                                .andExpect(jsonPath("$.firstPriorityAccepted", is(0)))
                                .andExpect(jsonPath("$.waitlisted", is(1)));
        }

        @Test
        void testConductLottery_BirthDateParsing() throws Exception {
                LotteryRequest request = new LotteryRequest();
                request.setInstitutionId(testInstitutionId);

                when(waitlistJdbcRepository.getTotalCapacity(testInstitutionId)).thenReturn(100);
                when(waitlistJdbcRepository.getCurrentStudentsCount(testInstitutionId)).thenReturn(50);

                Map<Integer, Integer> acceptedCount = new HashMap<>();
                acceptedCount.put(1, 0);
                acceptedCount.put(2, 0);
                acceptedCount.put(3, 0);
                when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(acceptedCount);

                // Applicants with different BirthDate formats
                Map<Integer, List<Map<String, Object>>> applicantsByPriority = new HashMap<>();
                List<Map<String, Object>> p1 = new ArrayList<>();

                Map<String, Object> appDate = new HashMap<>();
                appDate.put("ApplicantName", "SQL Date");
                appDate.put("BirthDate", java.sql.Date.valueOf("2020-01-01"));
                p1.add(appDate);

                Map<String, Object> appLocalDate = new HashMap<>();
                appLocalDate.put("ApplicantName", "Local Date");
                appLocalDate.put("BirthDate", java.time.LocalDate.of(2020, 1, 2));
                p1.add(appLocalDate);

                Map<String, Object> appNull = new HashMap<>();
                appNull.put("ApplicantName", "No Date");
                appNull.put("BirthDate", null);
                p1.add(appNull);

                applicantsByPriority.put(1, p1);
                applicantsByPriority.put(2, new ArrayList<>());
                applicantsByPriority.put(3, new ArrayList<>());
                when(waitlistJdbcRepository.getLotteryApplicantsByPriority(testInstitutionId))
                                .thenReturn(applicantsByPriority);

                when(waitlistJdbcRepository.getClassInfo(testInstitutionId)).thenReturn(new ArrayList<>());
                when(waitlistJdbcRepository.findSuitableClass(any(), any())).thenReturn(testClassId);
                when(waitlistJdbcRepository.hasClassCapacity(testClassId)).thenReturn(true);

                mockMvc.perform(post("/waitlist/lottery")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                // SQL Date & LocalDate -> Accepted (2)
                                // Null Date -> Waitlisted (1)
                                .andExpect(jsonPath("$.firstPriorityAccepted", is(2)))
                                .andExpect(jsonPath("$.waitlisted", is(1)));
            }

        // Willium1925修改
        @Test
        void testConductLottery_ThirdPriorityLotteryRequired() throws Exception {
            LotteryRequest request = new LotteryRequest();
            request.setInstitutionId(testInstitutionId);

            when(waitlistJdbcRepository.getTotalCapacity(testInstitutionId)).thenReturn(10);
            when(waitlistJdbcRepository.getCurrentStudentsCount(testInstitutionId)).thenReturn(5); // 5 個空位
            when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(new HashMap<>());

            Map<Integer, List<Map<String, Object>>> applicantsByPriority = new HashMap<>();
            applicantsByPriority.put(1, new ArrayList<>());
            applicantsByPriority.put(2, new ArrayList<>());
            List<Map<String, Object>> p3 = new ArrayList<>();
            p3.add(createApplicant("P3-A", "2020-01-01"));
            p3.add(createApplicant("P3-B", "2020-01-01"));
            p3.add(createApplicant("P3-C", "2020-01-01"));
            p3.add(createApplicant("P3-D", "2020-01-01"));
            p3.add(createApplicant("P3-E", "2020-01-01"));
            p3.add(createApplicant("P3-F", "2020-01-01")); // 6 人申請
            applicantsByPriority.put(3, p3);
            when(waitlistJdbcRepository.getLotteryApplicantsByPriority(testInstitutionId)).thenReturn(applicantsByPriority);

            when(waitlistJdbcRepository.findSuitableClass(any(), any())).thenReturn(testClassId);
            when(waitlistJdbcRepository.hasClassCapacity(testClassId)).thenReturn(true);
            when(waitlistJdbcRepository.getClassInfo(testInstitutionId)).thenReturn(new ArrayList<>());

            mockMvc.perform(post("/waitlist/lottery")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.thirdPriorityAccepted", is(5))) // 5 個空位
                    .andExpect(jsonPath("$.waitlisted", is(1))); // 1 人候補
        }

        // Willium1925修改：測試 assignClassAndAdmit 的 false 分支
        @Test
        void testConductLottery_AssignClassAdmitReturnsFalse() throws Exception {
                LotteryRequest request = new LotteryRequest();
                request.setInstitutionId(testInstitutionId);

                when(waitlistJdbcRepository.getTotalCapacity(testInstitutionId)).thenReturn(10);
                when(waitlistJdbcRepository.getCurrentStudentsCount(testInstitutionId)).thenReturn(0);
                when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(new HashMap<>());

                Map<Integer, List<Map<String, Object>>> applicantsByPriority = new HashMap<>();
                List<Map<String, Object>> p2 = new ArrayList<>();
                p2.add(createApplicant("P2-A", "2020-01-01"));
                applicantsByPriority.put(1, new ArrayList<>());
                applicantsByPriority.put(2, p2);
                applicantsByPriority.put(3, new ArrayList<>());
                when(waitlistJdbcRepository.getLotteryApplicantsByPriority(testInstitutionId)).thenReturn(applicantsByPriority);

                when(waitlistJdbcRepository.findSuitableClass(any(), any())).thenReturn(testClassId);
                // 模擬班級已滿
                when(waitlistJdbcRepository.hasClassCapacity(testClassId)).thenReturn(false);
                when(waitlistJdbcRepository.getClassInfo(testInstitutionId)).thenReturn(new ArrayList<>());

                mockMvc.perform(post("/waitlist/lottery")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.secondPriorityAccepted", is(0))) // 錄取 0 人
                                .andExpect(jsonPath("$.waitlisted", is(1))); // 候補 1 人
        }

        // Willium1925修改：測試 assignClassAndAdmit 的各種分支
        @Test
        void testAssignClassAndAdmit_Variations() throws Exception {
                LotteryRequest request = new LotteryRequest();
                request.setInstitutionId(testInstitutionId);

                when(waitlistJdbcRepository.getTotalCapacity(testInstitutionId)).thenReturn(10);
                when(waitlistJdbcRepository.getCurrentStudentsCount(testInstitutionId)).thenReturn(0);
                when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(new HashMap<>());

                Map<Integer, List<Map<String, Object>>> applicantsByPriority = new HashMap<>();
                List<Map<String, Object>> p1 = new ArrayList<>();
                // 1. BirthDate 為 LocalDate
                Map<String, Object> app1 = createApplicant("P1-LocalDate", null);
                app1.put("BirthDate", java.time.LocalDate.now());
                p1.add(app1);
                // 2. 找不到適合班級
                Map<String, Object> app2 = createApplicant("P1-NoClass", "2020-01-01");
                p1.add(app2);
                applicantsByPriority.put(1, p1);
                applicantsByPriority.put(2, new ArrayList<>());
                applicantsByPriority.put(3, new ArrayList<>());
                when(waitlistJdbcRepository.getLotteryApplicantsByPriority(testInstitutionId)).thenReturn(applicantsByPriority);

                // Mock 行為
                when(waitlistJdbcRepository.getClassInfo(testInstitutionId)).thenReturn(new ArrayList<>());
                // 讓 app1 找到班級
                when(waitlistJdbcRepository.findSuitableClass(any(java.time.LocalDate.class), any())).thenReturn(testClassId);
                // 讓 app2 找不到班級
                when(waitlistJdbcRepository.findSuitableClass(eq(java.sql.Date.valueOf("2020-01-01").toLocalDate()), any())).thenReturn(null);
                when(waitlistJdbcRepository.hasClassCapacity(testClassId)).thenReturn(true);

                mockMvc.perform(post("/waitlist/lottery")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.firstPriorityAccepted", is(1)))
                                .andExpect(jsonPath("$.waitlisted", is(1)));
        }

        // Willium1925修改:Helper method
        private Map<String, Object> createApplicant(String name, String birthDate) {
                Map<String, Object> applicant = new HashMap<>();
                applicant.put("ApplicantName", name);
                applicant.put("Email", "test@example.com");
                if (birthDate != null) {
                        applicant.put("BirthDate", java.sql.Date.valueOf(birthDate));
                }
                return applicant;
        }
}

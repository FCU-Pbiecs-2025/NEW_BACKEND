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
import java.time.LocalDate;
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
                
                // 修正：必須提供所有 key 的值
                Map<Integer, Integer> acceptedCount = new HashMap<>();
                acceptedCount.put(1, 0);
                acceptedCount.put(2, 0);
                acceptedCount.put(3, 0);
                when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(acceptedCount);

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
                
                // 修正：必須提供所有 key 的值
                Map<Integer, Integer> acceptedCount = new HashMap<>();
                acceptedCount.put(1, 0);
                acceptedCount.put(2, 0);
                acceptedCount.put(3, 0);
                when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(acceptedCount);

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
                
                // 修正：必須提供所有 key 的值
                Map<Integer, Integer> acceptedCount = new HashMap<>();
                acceptedCount.put(1, 0);
                acceptedCount.put(2, 0);
                acceptedCount.put(3, 0);
                when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(acceptedCount);

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
            when(waitlistJdbcRepository.getCurrentStudentsCount(testInstitutionId)).thenReturn(0); // 10 個空位

            // 修正：必須提供所有 key 的值
            Map<Integer, Integer> acceptedCount = new HashMap<>();
            acceptedCount.put(1, 0);
            acceptedCount.put(2, 0);
            acceptedCount.put(3, 0);
            when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(acceptedCount);

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
                            .andExpect(jsonPath("$.thirdPriorityAccepted", is(6))) // 6 人全部錄取
                            .andExpect(jsonPath("$.waitlisted", is(0))); // 0 人候補
        }

        // Willium1925修改：測試 assignClassAndAdmit 的 false 分支
        @Test
        void testConductLottery_AssignClassAdmitReturnsFalse() throws Exception {
                LotteryRequest request = new LotteryRequest();
                request.setInstitutionId(testInstitutionId);

                when(waitlistJdbcRepository.getTotalCapacity(testInstitutionId)).thenReturn(10);
                when(waitlistJdbcRepository.getCurrentStudentsCount(testInstitutionId)).thenReturn(0);
                
                // 修正：必須提供所有 key 的值
                Map<Integer, Integer> acceptedCount = new HashMap<>();
                acceptedCount.put(1, 0);
                acceptedCount.put(2, 0);
                acceptedCount.put(3, 0);
                when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(acceptedCount);

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

        // Willium1925修改：測試 assignClassAndAdmit 的各種分支（確保覆蓋 LocalDate 分支）
        @Test
        void testAssignClassAndAdmit_Variations() throws Exception {
                LotteryRequest request = new LotteryRequest();
                request.setInstitutionId(testInstitutionId);

                when(waitlistJdbcRepository.getTotalCapacity(testInstitutionId)).thenReturn(10);
                when(waitlistJdbcRepository.getCurrentStudentsCount(testInstitutionId)).thenReturn(0);
                
                Map<Integer, Integer> acceptedCount = new HashMap<>();
                acceptedCount.put(1, 0);
                acceptedCount.put(2, 0);
                acceptedCount.put(3, 0);
                when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(acceptedCount);

                Map<Integer, List<Map<String, Object>>> applicantsByPriority = new HashMap<>();
                List<Map<String, Object>> p1 = new ArrayList<>();

                // 1. BirthDate 為 LocalDate（確保覆蓋 instanceof LocalDate 分支）
                LocalDate testBirthDate = java.time.LocalDate.of(2020, 6, 15);
                Map<String, Object> app1 = createApplicant("P1-LocalDate", null);
                app1.put("BirthDate", testBirthDate);  // 直接使用 LocalDate 對象
                p1.add(app1);

                // 2. 找不到適合班級（使用 java.sql.Date）
                Map<String, Object> app2 = createApplicant("P1-NoClass", "2020-01-01");
                p1.add(app2);

                applicantsByPriority.put(1, p1);
                applicantsByPriority.put(2, new ArrayList<>());
                applicantsByPriority.put(3, new ArrayList<>());
                when(waitlistJdbcRepository.getLotteryApplicantsByPriority(testInstitutionId)).thenReturn(applicantsByPriority);

                // Mock 行為
                when(waitlistJdbcRepository.getClassInfo(testInstitutionId)).thenReturn(new ArrayList<>());

                // 讓 app1 找到班級（使用 eq 來匹配特定的 LocalDate）
                when(waitlistJdbcRepository.findSuitableClass(eq(testBirthDate), any())).thenReturn(testClassId);

                // 讓 app2 找不到班級
                when(waitlistJdbcRepository.findSuitableClass(eq(java.sql.Date.valueOf("2020-01-01").toLocalDate()), any())).thenReturn(null);

                when(waitlistJdbcRepository.hasClassCapacity(testClassId)).thenReturn(true);

                mockMvc.perform(post("/waitlist/lottery")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.firstPriorityAccepted", is(1)))
                                .andExpect(jsonPath("$.waitlisted", is(1)));

                // 驗證 findSuitableClass 被用 LocalDate 調用過（確保覆蓋 instanceof LocalDate 分支）
                verify(waitlistJdbcRepository).findSuitableClass(eq(testBirthDate), any());
        }

        // Willium1925新增：測試同步更新記憶體中的班級資訊（覆蓋 classId 匹配分支）
        @Test
        void testAssignClassAndAdmit_UpdateClassInfoInMemory() throws Exception {
                LotteryRequest request = new LotteryRequest();
                request.setInstitutionId(testInstitutionId);

                when(waitlistJdbcRepository.getTotalCapacity(testInstitutionId)).thenReturn(20);
                when(waitlistJdbcRepository.getCurrentStudentsCount(testInstitutionId)).thenReturn(0);

                Map<Integer, Integer> acceptedCount = new HashMap<>();
                acceptedCount.put(1, 0);
                acceptedCount.put(2, 0);
                acceptedCount.put(3, 0);
                when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(acceptedCount);

                Map<Integer, List<Map<String, Object>>> applicantsByPriority = new HashMap<>();
                List<Map<String, Object>> p1 = new ArrayList<>();

                // 測試 LocalDate 類型的 BirthDate (覆蓋黃色分支 - instanceof LocalDate)
                LocalDate birthDate1 = java.time.LocalDate.of(2020, 1, 15);
                Map<String, Object> app1 = createApplicant("P1-App1", null);
                app1.put("BirthDate", birthDate1);  // 直接使用 LocalDate 對象
                p1.add(app1);

                // 測試 java.sql.Date 類型的 BirthDate (確保兩種類型都被測試到)
                Map<String, Object> app2 = createApplicant("P1-App2", "2020-02-20");
                p1.add(app2);

                applicantsByPriority.put(1, p1);
                applicantsByPriority.put(2, new ArrayList<>());
                applicantsByPriority.put(3, new ArrayList<>());
                when(waitlistJdbcRepository.getLotteryApplicantsByPriority(testInstitutionId)).thenReturn(applicantsByPriority);

                // 創建班級資訊列表，用於測試記憶體更新
                // 這個列表會測試 for 循環和 classId 匹配邏輯（覆蓋黃色和紅色分支）
                List<Map<String, Object>> classInfoList = new ArrayList<>();

                // 先添加一個不匹配的班級（測試 if 條件為 false 的情況）
                UUID anotherClassId = UUID.randomUUID();
                Map<String, Object> classInfo2 = new HashMap<>();
                classInfo2.put("ClassID", anotherClassId);
                classInfo2.put("ClassName", "另一個班級");
                classInfo2.put("CurrentStudents", 3);
                classInfo2.put("Capacity", 8);
                classInfoList.add(classInfo2);

                // 再添加匹配的班級（測試 if 條件為 true 和 break 的情況）
                Map<String, Object> classInfo1 = new HashMap<>();
                classInfo1.put("ClassID", testClassId);
                classInfo1.put("ClassName", "測試班級");
                classInfo1.put("CurrentStudents", 5);
                classInfo1.put("Capacity", 10);
                classInfoList.add(classInfo1);

                when(waitlistJdbcRepository.getClassInfo(testInstitutionId)).thenReturn(classInfoList);

                // Mock findSuitableClass - 針對不同類型的日期分別設置
                when(waitlistJdbcRepository.findSuitableClass(eq(birthDate1), any())).thenReturn(testClassId);
                when(waitlistJdbcRepository.findSuitableClass(eq(java.sql.Date.valueOf("2020-02-20").toLocalDate()), any())).thenReturn(testClassId);
                when(waitlistJdbcRepository.hasClassCapacity(testClassId)).thenReturn(true);

                mockMvc.perform(post("/waitlist/lottery")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.firstPriorityAccepted", is(2)))
                                .andExpect(jsonPath("$.waitlisted", is(0)));

                // 驗證記憶體中的班級資訊被正確更新
                verify(waitlistJdbcRepository, times(2)).updateClassCurrentStudents(eq(testClassId), eq(1));
                // 驗證 LocalDate 分支被執行
                verify(waitlistJdbcRepository).findSuitableClass(eq(birthDate1), any());
        }

        // Willium1925新增：確保覆蓋 classId 匹配邏輯的所有路徑
        @Test
        void testAssignClassAndAdmit_ClassIdMatchingLogic() throws Exception {
                LotteryRequest request = new LotteryRequest();
                request.setInstitutionId(testInstitutionId);

                when(waitlistJdbcRepository.getTotalCapacity(testInstitutionId)).thenReturn(10);
                when(waitlistJdbcRepository.getCurrentStudentsCount(testInstitutionId)).thenReturn(0);

                Map<Integer, Integer> acceptedCount = new HashMap<>();
                acceptedCount.put(1, 0);
                acceptedCount.put(2, 0);
                acceptedCount.put(3, 0);
                when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(acceptedCount);

                Map<Integer, List<Map<String, Object>>> applicantsByPriority = new HashMap<>();
                List<Map<String, Object>> p3 = new ArrayList<>();

                // 使用 LocalDate 來確保覆蓋 instanceof LocalDate 分支
                LocalDate birthDate = java.time.LocalDate.of(2021, 5, 10);
                Map<String, Object> app = createApplicant("P3-App", null);
                app.put("BirthDate", birthDate);  // 直接使用 LocalDate 對象
                p3.add(app);

                applicantsByPriority.put(1, new ArrayList<>());
                applicantsByPriority.put(2, new ArrayList<>());
                applicantsByPriority.put(3, p3);
                when(waitlistJdbcRepository.getLotteryApplicantsByPriority(testInstitutionId)).thenReturn(applicantsByPriority);

                // 創建多個班級，測試遍歷邏輯
                List<Map<String, Object>> classInfoList = new ArrayList<>();

                // 第一個不匹配的班級
                UUID class1 = UUID.randomUUID();
                Map<String, Object> info1 = new HashMap<>();
                info1.put("ClassID", class1);
                info1.put("CurrentStudents", 1);
                classInfoList.add(info1);

                // 第二個不匹配的班級
                UUID class2 = UUID.randomUUID();
                Map<String, Object> info2 = new HashMap<>();
                info2.put("ClassID", class2);
                info2.put("CurrentStudents", 2);
                classInfoList.add(info2);

                // 第三個匹配的班級（測試 break）
                Map<String, Object> info3 = new HashMap<>();
                info3.put("ClassID", testClassId);
                info3.put("CurrentStudents", 3);
                classInfoList.add(info3);

                // 第四個班級（不應該被訪問到，因為上面 break 了）
                UUID class4 = UUID.randomUUID();
                Map<String, Object> info4 = new HashMap<>();
                info4.put("ClassID", class4);
                info4.put("CurrentStudents", 4);
                classInfoList.add(info4);

                when(waitlistJdbcRepository.getClassInfo(testInstitutionId)).thenReturn(classInfoList);
                when(waitlistJdbcRepository.findSuitableClass(eq(birthDate), any())).thenReturn(testClassId);
                when(waitlistJdbcRepository.hasClassCapacity(testClassId)).thenReturn(true);

                mockMvc.perform(post("/waitlist/lottery")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.thirdPriorityAccepted", is(1)));

                // 驗證 LocalDate 分支被執行
                verify(waitlistJdbcRepository).findSuitableClass(eq(birthDate), any());
        }

        // Willium1925新增：sendLotteryNotificationEmails 全覆蓋測試
        @Test
        void testSendLotteryNotificationEmails_FullCoverage() throws Exception {
            LotteryRequest request = new LotteryRequest();
            request.setInstitutionId(testInstitutionId);

            // 設定名額：P1=10, P3=10
            when(waitlistJdbcRepository.getTotalCapacity(testInstitutionId)).thenReturn(20);
            when(waitlistJdbcRepository.getCurrentStudentsCount(testInstitutionId)).thenReturn(0);

            Map<Integer, Integer> acceptedCount = new HashMap<>();
            acceptedCount.put(1, 0);
            acceptedCount.put(2, 0);
            acceptedCount.put(3, 0);
            when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(acceptedCount);

            Map<Integer, List<Map<String, Object>>> applicantsByPriority = new HashMap<>();

            // --- 錄取者 (P1) ---
            List<Map<String, Object>> p1 = new ArrayList<>();

            // 測試 CaseNumber = null 分支
            Map<String, Object> app1 = createApplicant("App1", "2020-01-01");
            app1.put("CaseNumber", null);
            app1.put("ApplicationDate", LocalDateTime.now());
            p1.add(app1);

            // 測試 CaseNumber 正常值 + ApplicationDate = java.sql.Timestamp
            Map<String, Object> app2 = createApplicant("App2", "2020-01-01");
            app2.put("CaseNumber", 12345L);
            app2.put("ApplicationDate", new java.sql.Timestamp(System.currentTimeMillis()));
            p1.add(app2);

            // 測試 ApplicationDate = java.sql.Date
            Map<String, Object> app3 = createApplicant("App3", "2020-01-01");
            app3.put("CaseNumber", 23456);
            app3.put("ApplicationDate", java.sql.Date.valueOf("2023-01-01"));
            p1.add(app3);

            // 測試 ApplicationDate = LocalDate
            Map<String, Object> app4 = createApplicant("App4", "2020-01-01");
            app4.put("ApplicationDate", java.time.LocalDate.now());
            p1.add(app4);

            // 測試 ApplicationDate = String
            Map<String, Object> app5 = createApplicant("App5", "2020-01-01");
            app5.put("ApplicationDate", "2023-01-15");
            p1.add(app5);

            // 測試 Email = null 分支
            Map<String, Object> app6 = createApplicant("App6", "2020-01-01");
            app6.put("Email", null);
            p1.add(app6);

            // 測試 Email = "" 分支
            Map<String, Object> app7 = createApplicant("App7", "2020-01-01");
            app7.put("Email", "");
            p1.add(app7);

            // 測試錄取者的 Exception 處理
            Map<String, Object> app8 = createApplicant("App8", "2020-01-01");
            app8.put("Email", "accepted-error@example.com");
            p1.add(app8);

            // --- 候補者 (P3) ---
            List<Map<String, Object>> p3 = new ArrayList<>();

            // 測試 CaseNumber = null 分支
            Map<String, Object> app9 = createApplicant("App9", "2021-01-01");
            app9.put("CaseNumber", null);
            app9.put("ApplicationDate", LocalDateTime.now());
            app9.put("BirthDate", java.sql.Date.valueOf("2021-01-01"));
            p3.add(app9);

            // 測試 CaseNumber 正常值 + ApplicationDate = java.sql.Timestamp
            Map<String, Object> app10 = createApplicant("App10", "2021-01-01");
            app10.put("CaseNumber", 34567L);
            app10.put("ApplicationDate", new java.sql.Timestamp(System.currentTimeMillis()));
            app10.put("BirthDate", java.sql.Date.valueOf("2021-01-01"));
            p3.add(app10);

            // 測試 ApplicationDate = java.sql.Date
            Map<String, Object> app11 = createApplicant("App11", "2021-01-01");
            app11.put("CaseNumber", 45678);
            app11.put("ApplicationDate", java.sql.Date.valueOf("2023-02-01"));
            app11.put("BirthDate", java.sql.Date.valueOf("2021-01-01"));
            p3.add(app11);

            // 測試 ApplicationDate = LocalDate
            Map<String, Object> app12 = createApplicant("App12", "2021-01-01");
            app12.put("ApplicationDate", java.time.LocalDate.now());
            app12.put("BirthDate", java.sql.Date.valueOf("2021-01-01"));
            p3.add(app12);

            // 測試 ApplicationDate = String
            Map<String, Object> app13 = createApplicant("App13", "2021-01-01");
            app13.put("ApplicationDate", "2023-03-01");
            app13.put("BirthDate", java.sql.Date.valueOf("2021-01-01"));
            p3.add(app13);

            // 測試 Email = null 分支
            Map<String, Object> app14 = createApplicant("App14", "2021-01-01");
            app14.put("Email", null);
            app14.put("BirthDate", java.sql.Date.valueOf("2021-01-01"));
            p3.add(app14);

            // 測試 Email = "" 分支
            Map<String, Object> app15 = createApplicant("App15", "2021-01-01");
            app15.put("Email", "");
            app15.put("BirthDate", java.sql.Date.valueOf("2021-01-01"));
            p3.add(app15);

            // 測試 MessagingException 分支
            Map<String, Object> app16 = createApplicant("App16", "2021-01-01");
            app16.put("Email", "msg-error@example.com");
            app16.put("BirthDate", java.sql.Date.valueOf("2021-01-01"));
            p3.add(app16);

            // 測試 RuntimeException (其他 Exception) 分支
            Map<String, Object> app17 = createApplicant("App17", "2021-01-01");
            app17.put("Email", "run-error@example.com");
            app17.put("BirthDate", java.sql.Date.valueOf("2021-01-01"));
            p3.add(app17);

            applicantsByPriority.put(1, p1);
            applicantsByPriority.put(2, new ArrayList<>());
            applicantsByPriority.put(3, p3);
            when(waitlistJdbcRepository.getLotteryApplicantsByPriority(testInstitutionId)).thenReturn(applicantsByPriority);

            // 讓 P1 錄取
            when(waitlistJdbcRepository.findSuitableClass(eq(java.sql.Date.valueOf("2020-01-01").toLocalDate()), any())).thenReturn(testClassId);
            when(waitlistJdbcRepository.findSuitableClass(eq(java.time.LocalDate.now()), any())).thenReturn(testClassId);
            when(waitlistJdbcRepository.hasClassCapacity(testClassId)).thenReturn(true);

            // 讓 P3 候補 (找不到班級)
            when(waitlistJdbcRepository.findSuitableClass(eq(java.sql.Date.valueOf("2021-01-01").toLocalDate()), any())).thenReturn(null);

            when(waitlistJdbcRepository.getClassInfo(testInstitutionId)).thenReturn(new ArrayList<>());

            // Mock Email Exceptions - 錄取者的異常
            doThrow(new RuntimeException("Accepted Send Error"))
                    .when(emailService).sendApplicationStatusChangeEmail(
                            eq("accepted-error@example.com"),
                            any(), any(), any(), any(), any(), any(),
                            eq(null), // 錄取者的 currentOrder 是 null
                            any());

            // Mock Email Exceptions - 候補者的異常
            doThrow(new MessagingException("Mail Error"))
                    .when(emailService).sendApplicationStatusChangeEmail(
                            eq("msg-error@example.com"),
                            any(), any(), any(), any(), any(), any(),
                            any(Integer.class), // 候補者有 currentOrder
                            any());

            doThrow(new RuntimeException("Run Error"))
                    .when(emailService).sendApplicationStatusChangeEmail(
                            eq("run-error@example.com"),
                            any(), any(), any(), any(), any(), any(),
                            any(Integer.class), // 候補者有 currentOrder
                            any());

            mockMvc.perform(post("/waitlist/lottery")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // 驗證正常的郵件發送被呼叫了
            verify(emailService, atLeastOnce()).sendApplicationStatusChangeEmail(
                    any(), any(), any(), any(), any(), any(), any(), any(), any());
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

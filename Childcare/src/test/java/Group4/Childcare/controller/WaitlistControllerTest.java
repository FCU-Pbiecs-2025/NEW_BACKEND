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

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
        applicantsByPriority.put(1, new ArrayList<>());
        applicantsByPriority.put(2, new ArrayList<>());
        applicantsByPriority.put(3, new ArrayList<>());
        when(waitlistJdbcRepository.getLotteryApplicantsByPriority(testInstitutionId))
                .thenReturn(applicantsByPriority);

        when(waitlistJdbcRepository.getClassInfo(testInstitutionId)).thenReturn(new ArrayList<>());

        mockMvc.perform(post("/waitlist/lottery")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void testConductLottery_NoAvailableSlots() throws Exception {
        LotteryRequest request = new LotteryRequest();
        request.setInstitutionId(testInstitutionId);

        when(waitlistJdbcRepository.getTotalCapacity(testInstitutionId)).thenReturn(100);
        when(waitlistJdbcRepository.getCurrentStudentsCount(testInstitutionId)).thenReturn(100);

        Map<Integer, Integer> acceptedCount = new HashMap<>();
        acceptedCount.put(1, 20);
        acceptedCount.put(2, 10);
        acceptedCount.put(3, 70);
        when(waitlistJdbcRepository.getAcceptedCountByPriority(testInstitutionId)).thenReturn(acceptedCount);

        Map<Integer, List<Map<String, Object>>> applicantsByPriority = new HashMap<>();
        applicantsByPriority.put(1, new ArrayList<>());
        applicantsByPriority.put(2, new ArrayList<>());
        applicantsByPriority.put(3, new ArrayList<>());
        when(waitlistJdbcRepository.getLotteryApplicantsByPriority(testInstitutionId))
                .thenReturn(applicantsByPriority);

        when(waitlistJdbcRepository.getClassInfo(testInstitutionId)).thenReturn(new ArrayList<>());

        mockMvc.perform(post("/waitlist/lottery")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    // ===== manualAdmit 測試 =====
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
}

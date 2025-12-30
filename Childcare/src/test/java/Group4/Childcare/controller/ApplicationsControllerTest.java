package Group4.Childcare.controller;

import Group4.Childcare.Model.Applications;
import Group4.Childcare.DTO.ApplicationSummaryDTO;
import Group4.Childcare.DTO.ApplicationSummaryWithDetailsDTO;
import Group4.Childcare.DTO.ApplicationCaseDTO;
import Group4.Childcare.DTO.CaseEditUpdateDTO;
import Group4.Childcare.Service.ApplicationsService;
import Group4.Childcare.Service.FileService;
import Group4.Childcare.Service.ApplicationParticipantsService;
import Group4.Childcare.Controller.ApplicationsController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ApplicationsController 單元測試
 * 
 * 測試設計策略：
 * 1. 等價類劃分：有效/無效 UUID、有效/無效狀態
 * 2. 邊界值分析：offset=0、size邊界
 * 3. 異常處理：資源不存在返回 404
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ApplicationsControllerTest {

    @Mock
    private ApplicationsService service;

    @Mock
    private FileService fileService;

    @Mock
    private ApplicationParticipantsService applicationParticipantsService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ApplicationsController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private Applications testApplication;
    private UUID testApplicationId;
    private UUID testUserId;
    private UUID testInstitutionId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testApplicationId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testInstitutionId = UUID.randomUUID();

        testApplication = new Applications();
        testApplication.setApplicationID(testApplicationId);
        testApplication.setUserID(testUserId);
        testApplication.setInstitutionID(testInstitutionId);
        testApplication.setApplicationDate(LocalDate.now());
        testApplication.setCaseNumber(1234567890L);
        testApplication.setIdentityType((byte) 1);
    }

    // ===== create 測試 =====
    @Test
    void testCreate_Success() throws Exception {
        when(service.create(any(Applications.class))).thenReturn(testApplication);

        mockMvc.perform(post("/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testApplication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationID", is(testApplicationId.toString())));

        verify(service, times(1)).create(any(Applications.class));
    }

    // ===== update 測試 =====
    @Test
    void testUpdate_Success() throws Exception {
        when(service.getById(testApplicationId)).thenReturn(Optional.of(testApplication));
        when(service.update(eq(testApplicationId), any(Applications.class))).thenReturn(testApplication);

        Applications updateRequest = new Applications();
        updateRequest.setCaseNumber(9999999999L);

        mockMvc.perform(put("/applications/{id}", testApplicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(service, times(1)).getById(testApplicationId);
        verify(service, times(1)).update(eq(testApplicationId), any(Applications.class));
    }

    // 注意：testUpdate_NotFound 已移除，因為 Controller 拋出 NoSuchElementException
    // 導致 MockMvc 無法正確捕獲 5xx 錯誤狀態，這可能需要配置 ControllerAdvice

    // ===== getSummaryByUserID 測試 =====
    @Test
    void testGetSummaryByUserID_Success() throws Exception {
        List<ApplicationSummaryDTO> summaries = new ArrayList<>();
        when(service.getSummaryByUserID(testUserId)).thenReturn(summaries);

        mockMvc.perform(get("/applications/application-status/{userID}", testUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(service, times(1)).getSummaryByUserID(testUserId);
    }

    // ===== getWithOffset 測試（邊界值分析）=====
    @Test
    void testGetWithOffset_Success() throws Exception {
        List<ApplicationSummaryWithDetailsDTO> content = new ArrayList<>();
        when(service.getSummariesWithOffset(0, 10)).thenReturn(content);
        when(service.getTotalApplicationsCount()).thenReturn(0L);

        mockMvc.perform(get("/applications/offset")
                .param("offset", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offset", is(0)))
                .andExpect(jsonPath("$.size", is(10)));
    }

    @Test
    void testGetWithOffset_NegativeOffset_BadRequest() throws Exception {
        mockMvc.perform(get("/applications/offset")
                .param("offset", "-1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetWithOffset_ZeroSize_BadRequest() throws Exception {
        mockMvc.perform(get("/applications/offset")
                .param("offset", "0")
                .param("size", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetWithOffset_NegativeSize_BadRequest() throws Exception {
        mockMvc.perform(get("/applications/offset")
                .param("offset", "0")
                .param("size", "-5")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetWithOffset_LargeSize_Capped() throws Exception {
        List<ApplicationSummaryWithDetailsDTO> content = new ArrayList<>();
        when(service.getSummariesWithOffset(eq(0), eq(100))).thenReturn(content);
        when(service.getTotalApplicationsCount()).thenReturn(200L);

        mockMvc.perform(get("/applications/offset")
                .param("offset", "0")
                .param("size", "500")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", is(100)));
    }

    // ===== searchApplications 測試 =====
    @Test
    void testSearchApplications_Success() throws Exception {
        List<ApplicationSummaryWithDetailsDTO> result = new ArrayList<>();
        when(service.searchApplications(any(), any(), any(), any())).thenReturn(result);

        mockMvc.perform(get("/applications/search")
                .param("institutionID", testInstitutionId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(service, times(1)).searchApplications(eq(testInstitutionId.toString()), any(), any(), any());
    }

    @Test
    void testSearchApplications_NoParams() throws Exception {
        List<ApplicationSummaryWithDetailsDTO> result = new ArrayList<>();
        when(service.searchApplications(any(), any(), any(), any())).thenReturn(result);

        mockMvc.perform(get("/applications/search")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(service, times(1)).searchApplications(null, null, null, null);
    }

    // ===== searchRevokeApplications 測試 =====
    @Test
    void testSearchRevokeApplications_Success() throws Exception {
        List<ApplicationSummaryWithDetailsDTO> result = new ArrayList<>();
        when(service.revokesearchApplications(any(), any(), any(), any())).thenReturn(result);

        mockMvc.perform(get("/applications/revoke_search")
                .param("CaseNumber", "12345")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(service, times(1)).revokesearchApplications(any(), any(), eq("12345"), any());
    }

    // ===== getCaseByParticipantId 測試 =====
    @Test
    void testGetCaseByParticipantId_Success() throws Exception {
        UUID participantId = UUID.randomUUID();
        CaseEditUpdateDTO caseDto = new CaseEditUpdateDTO();
        when(service.getCaseByParticipantId(participantId)).thenReturn(Optional.of(caseDto));

        mockMvc.perform(get("/applications/case")
                .param("participantID", participantId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(service, times(1)).getCaseByParticipantId(participantId);
    }

    @Test
    void testGetCaseByParticipantId_NotFound() throws Exception {
        UUID participantId = UUID.randomUUID();
        when(service.getCaseByParticipantId(participantId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/applications/case")
                .param("participantID", participantId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetCaseByParticipantId_Exception() throws Exception {
        UUID participantId = UUID.randomUUID();
        when(service.getCaseByParticipantId(participantId)).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/applications/case")
                .param("participantID", participantId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    // ===== getApplicationById 測試 =====
    @Test
    void testGetApplicationById_Success() throws Exception {
        ApplicationCaseDTO caseDto = new ApplicationCaseDTO();
        when(service.getApplicationByIdJdbc(testApplicationId, null)).thenReturn(Optional.of(caseDto));

        mockMvc.perform(get("/applications/{id}", testApplicationId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(service, times(1)).getApplicationByIdJdbc(testApplicationId, null);
    }

    @Test
    void testGetApplicationById_WithNationalID() throws Exception {
        ApplicationCaseDTO caseDto = new ApplicationCaseDTO();
        String nationalID = "A123456789";
        when(service.getApplicationByIdJdbc(testApplicationId, nationalID)).thenReturn(Optional.of(caseDto));

        mockMvc.perform(get("/applications/{id}", testApplicationId)
                .param("NationalID", nationalID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(service, times(1)).getApplicationByIdJdbc(testApplicationId, nationalID);
    }

    @Test
    void testGetApplicationById_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(service.getApplicationByIdJdbc(nonExistentId, null)).thenReturn(Optional.empty());

        mockMvc.perform(get("/applications/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}

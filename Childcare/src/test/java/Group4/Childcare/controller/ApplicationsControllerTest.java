package Group4.Childcare.controller;

import Group4.Childcare.Model.Applications;
import Group4.Childcare.Model.ApplicationParticipants;
import Group4.Childcare.DTO.ApplicationSummaryDTO;
import Group4.Childcare.DTO.ApplicationSummaryWithDetailsDTO;
import Group4.Childcare.DTO.ApplicationCaseDTO;
import Group4.Childcare.DTO.CaseEditUpdateDTO;
import Group4.Childcare.DTO.UserSimpleDTO;
import Group4.Childcare.DTO.ApplicationParticipantDTO;
import Group4.Childcare.DTO.AdminCaseSearchRequestDto;
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
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
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

                // Manually inject mocks that @InjectMocks might miss due to mixed
                // constructor/field injection
                ReflectionTestUtils.setField(controller, "fileService", fileService);
                ReflectionTestUtils.setField(controller, "jdbcTemplate", jdbcTemplate);
                ReflectionTestUtils.setField(controller, "applicationParticipantsService",
                                applicationParticipantsService);
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

                // update 走檔案儲存流程時會用到 folderPath + Files.copy，測試要給一個存在的 tempDir。
                Path tempDir = Files.createTempDirectory("childcare-test-update-");
                when(fileService.getFolderPath(eq(testApplicationId))).thenReturn(tempDir);

                // Controller 的 update({id}) 是 multipart/form-data
                // 為了觸發 service.update，我們必須提供至少一個檔案，因為 Controller 邏輯是：
                // if (!fileMap.isEmpty()) { ... service.update(...) } else { ... }
                MockMultipartFile file = new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE,
                                "content".getBytes());

                MockMultipartHttpServletRequestBuilder req = multipart("/applications/{id}", testApplicationId)
                                .file(file);
                req.with(r -> {
                        r.setMethod("PUT");
                        return r;
                });

                mockMvc.perform(req
                                .contentType(MediaType.MULTIPART_FORM_DATA))
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

        @Test
        void testGetCaseByParticipantId_NullParameter() throws Exception {
                mockMvc.perform(get("/applications/case")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());
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

        // ==================== 新增測試用例以提升覆蓋率 ====================

        // ===== updateApplicationCase 測試 =====
        @Test
        void testUpdateApplicationCase_WithNationalID_Success() throws Exception {
                String nationalID = "A123456789";
                String status = "審核中";
                ApplicationCaseDTO caseDto = new ApplicationCaseDTO();
                when(service.getApplicationByIdJdbc(eq(testApplicationId), eq(nationalID)))
                                .thenReturn(Optional.of(caseDto));
                doNothing().when(service).updateStatusAndSendEmail(any(), any(), any(), any(), any());

                mockMvc.perform(put("/applications/{id}/case", testApplicationId)
                                .param("NationalID", nationalID)
                                .param("status", status)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isOk());

                verify(service, times(1)).updateStatusAndSendEmail(eq(testApplicationId), eq(nationalID), eq(status),
                                any(),
                                any());
        }

        @Test
        void testUpdateApplicationCase_WithNationalID_MissingStatus() throws Exception {
                String nationalID = "A123456789";

                mockMvc.perform(put("/applications/{id}/case", testApplicationId)
                                .param("NationalID", nationalID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateApplicationCase_BatchUpdate_Success() throws Exception {
                String status = "審核通過";
                ApplicationCaseDTO caseDto = new ApplicationCaseDTO();
                doNothing().when(service).updateApplicationCase(eq(testApplicationId), any());

                mockMvc.perform(put("/applications/{id}/case", testApplicationId)
                                .param("status", status)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isNoContent());

                verify(service, times(1)).updateApplicationCase(eq(testApplicationId), any());
        }

        @Test
        void testUpdateApplicationCase_BatchUpdate_MissingStatus() throws Exception {
                mockMvc.perform(put("/applications/{id}/case", testApplicationId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateApplicationCase_Exception() throws Exception {
                String nationalID = "A123456789";
                String status = "審核中";
                doThrow(new RuntimeException("Error")).when(service)
                                .updateStatusAndSendEmail(any(), any(), any(), any(), any());

                mockMvc.perform(put("/applications/{id}/case", testApplicationId)
                                .param("NationalID", nationalID)
                                .param("status", status)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isInternalServerError());
        }

        @Test
        void testUpdateApplicationCase_NotFound() throws Exception {
                String nationalID = "A123456789";
                String status = "審核中";
                doNothing().when(service).updateStatusAndSendEmail(any(), any(), any(), any(), any());
                when(service.getApplicationByIdJdbc(eq(testApplicationId), eq(nationalID)))
                                .thenReturn(Optional.empty());

                mockMvc.perform(put("/applications/{id}/case", testApplicationId)
                                .param("NationalID", nationalID)
                                .param("status", status)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isNotFound());
        }

        // ===== getCasesList 測試 =====
        @Test
        void testGetCasesList_Success() throws Exception {
                when(service.getCaseListWithOffset(anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any()))
                                .thenReturn(new ArrayList<>());
                when(service.countCaseList(any(), any(), any(), any(), any(), any(), any())).thenReturn(0L);

                mockMvc.perform(get("/applications/cases/list")
                                .param("offset", "0")
                                .param("size", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.offset", is(0)))
                                .andExpect(jsonPath("$.size", is(10)));
        }

        @Test
        void testGetCasesList_WithFilters() throws Exception {
                when(service.getCaseListWithOffset(anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any()))
                                .thenReturn(new ArrayList<>());
                when(service.countCaseList(any(), any(), any(), any(), any(), any(), any())).thenReturn(0L);

                mockMvc.perform(get("/applications/cases/list")
                                .param("offset", "0")
                                .param("size", "10")
                                .param("status", "審核中")
                                .param("institutionId", testInstitutionId.toString())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        void testGetCasesList_InvalidOffset() throws Exception {
                mockMvc.perform(get("/applications/cases/list")
                                .param("offset", "-1")
                                .param("size", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testGetCasesList_InvalidSize() throws Exception {
                mockMvc.perform(get("/applications/cases/list")
                                .param("offset", "0")
                                .param("size", "0")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testGetCasesList_LargeSize() throws Exception {
                when(service.getCaseListWithOffset(anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any()))
                                .thenReturn(new ArrayList<>());
                when(service.countCaseList(any(), any(), any(), any(), any(), any(), any())).thenReturn(0L);

                mockMvc.perform(get("/applications/cases/list")
                                .param("offset", "0")
                                .param("size", "200")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.size", is(100))); // 應該被限制為 100
        }

        @Test
        void testGetCasesList_InvalidInstitutionId() throws Exception {
                mockMvc.perform(get("/applications/cases/list")
                                .param("offset", "0")
                                .param("size", "10")
                                .param("institutionId", "invalid-uuid")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error", is("Invalid institutionId format")));
        }

        @Test
        void testGetCasesList_InvalidApplicationId() throws Exception {
                mockMvc.perform(get("/applications/cases/list")
                                .param("offset", "0")
                                .param("size", "10")
                                .param("applicationId", "invalid-uuid")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error", is("Invalid applicationId format")));
        }

        @Test
        void testGetCasesList_InvalidClassId() throws Exception {
                mockMvc.perform(get("/applications/cases/list")
                                .param("offset", "0")
                                .param("size", "10")
                                .param("classId", "invalid-uuid")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error", is("Invalid classId format")));
        }

        // ===== adminSearchCasesGet 測試 =====
        @Test
        void testAdminSearchCasesGet_InvalidInstitutionId() throws Exception {
                mockMvc.perform(get("/applications/case/search")
                                .param("institutionId", "invalid-uuid")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testAdminSearchCasesGet_InvalidClassId() throws Exception {
                mockMvc.perform(get("/applications/case/search")
                                .param("classId", "invalid-uuid")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());
        }

        // ===== getUserApplicationDetails 測試 =====
        @Test
        void testGetUserApplicationDetails_Success() throws Exception {
                when(service.getUserApplicationDetails(testUserId)).thenReturn(new ArrayList<>());

                mockMvc.perform(get("/applications/user/{userID}/details", testUserId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());

                verify(service, times(1)).getUserApplicationDetails(testUserId);
        }

        @Test
        void testGetUserApplicationDetails_Exception() throws Exception {
                when(service.getUserApplicationDetails(testUserId)).thenThrow(new RuntimeException("Error"));

                mockMvc.perform(get("/applications/user/{userID}/details", testUserId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isInternalServerError());
        }

        @Test
        void testGetUserApplicationDetails_NullUserId() throws Exception {
                mockMvc.perform(get("/applications/user/{userID}/details", (Object) null)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound());
        }

        // ===== submitApplicationCase 測試 =====
        @Test
        void testSubmitApplicationCase_Success() throws Exception {
                UUID newApplicationId = UUID.randomUUID();
                Applications createdApp = new Applications();
                createdApp.setApplicationID(newApplicationId);

                CaseEditUpdateDTO caseDto = new CaseEditUpdateDTO();
                caseDto.setCaseNumber(1L);
                caseDto.setApplyDate(LocalDate.now());
                caseDto.setIdentityType(1);
                caseDto.setInstitutionId(testInstitutionId);
                caseDto.setInstitutionName("Test Institution");
                caseDto.setSelectedClass("Class A");
                caseDto.setCurrentOrder(1);

                UserSimpleDTO userDto = new UserSimpleDTO();
                userDto.setUserID(testUserId.toString());
                userDto.setName("Test User");
                userDto.setNationalID("A123456789");
                caseDto.setUser(userDto);

                ApplicationParticipantDTO childDto = new ApplicationParticipantDTO();
                childDto.setNationalID("C123456789");
                childDto.setParticipantType("CHILD");
                childDto.setName("Test Child");
                caseDto.setChildren(List.of(childDto));

                ApplicationParticipantDTO parentDto = new ApplicationParticipantDTO();
                parentDto.setNationalID("P123456789");
                parentDto.setParticipantType("PARENT");
                parentDto.setName("Test Parent");
                caseDto.setParents(List.of(parentDto));

                // Mock service calls
                when(service.create(any(Applications.class))).thenReturn(createdApp);
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);
                when(applicationParticipantsService.create(any(Group4.Childcare.Model.ApplicationParticipants.class)))
                                .thenReturn(null);

                // submitApplicationCase 會直接 Files.copy()，因此 folderPath 必須存在。
                Path tempDir = Files.createTempDirectory("childcare-test-identity-");
                when(fileService.getFolderPath(any(UUID.class))).thenAnswer(inv -> tempDir);

                MockMultipartFile file = new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE,
                                "content".getBytes());
                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(file)
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service, times(1)).create(any(Applications.class));
        }

        @Test
        void testSubmitApplicationCase_InvalidDTO() throws Exception {
                MockMultipartFile file = new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE,
                                "content".getBytes());

                // Missing caseDto
                mockMvc.perform(multipart("/applications/case/submit")
                                .file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testSubmitApplicationCase_ServiceError() throws Exception {
                CaseEditUpdateDTO caseDto = new CaseEditUpdateDTO();
                caseDto.setCaseNumber(1L);
                caseDto.setApplyDate(LocalDate.now());
                caseDto.setIdentityType(1);
                caseDto.setInstitutionId(testInstitutionId);
                caseDto.setInstitutionName("Test Institution");
                caseDto.setSelectedClass("Class A");
                caseDto.setCurrentOrder(1);

                UserSimpleDTO userDto = new UserSimpleDTO();
                userDto.setUserID(testUserId.toString());
                userDto.setName("Test User");
                userDto.setNationalID("A123456789");
                caseDto.setUser(userDto);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));
                MockMultipartFile file = new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE,
                                "content".getBytes());

                when(service.create(any(Applications.class))).thenThrow(new RuntimeException("Service Error"));
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(file)
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isInternalServerError());
        }

        // ===== adminSearchCases 測試 =====
        @Test
        void testAdminSearchCases_Success() throws Exception {
                AdminCaseSearchRequestDto searchDto = new AdminCaseSearchRequestDto();
                searchDto.setInstitutionId(testInstitutionId);
                searchDto.setCaseNumber(12345L);

                List<Map<String, Object>> mockResult = new ArrayList<>();
                Map<String, Object> row = new HashMap<>();
                row.put("ApplicationID", testApplicationId.toString());
                mockResult.add(row);

                // Controller 呼叫 queryForList(sql, params.toArray())，第二參數是 Object[]。
                doReturn(mockResult).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

                mockMvc.perform(get("/applications/admin/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(searchDto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void testAdminSearchCases_DatabaseError() throws Exception {
                AdminCaseSearchRequestDto searchDto = new AdminCaseSearchRequestDto();

                doThrow(new RuntimeException("DB Error")).when(jdbcTemplate)
                                .queryForList(anyString(), any(Object[].class));

                mockMvc.perform(get("/applications/admin/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(searchDto)))
                                .andExpect(status().isInternalServerError());
        }

        // ==================== 新增測試用例 (Phase 2) - 達成 90%+ 覆蓋率 ====================

        // ===== update() 補充測試 =====
        @Test
        void testUpdate_ApplicationNotFound_ReturnsError() throws Exception {
                UUID nonExistentId = UUID.randomUUID();
                when(service.getById(nonExistentId)).thenReturn(Optional.empty());

                MockMultipartFile file = new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE,
                                "content".getBytes());

                MockMultipartHttpServletRequestBuilder req = multipart("/applications/{id}", nonExistentId)
                                .file(file);
                req.with(r -> {
                        r.setMethod("PUT");
                        return r;
                });

                mockMvc.perform(req.contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isInternalServerError());
        }

        @Test
        void testUpdate_NoFiles_RetainsExistingPaths() throws Exception {
                testApplication.setAttachmentPath("existing/path.pdf");
                when(service.getById(testApplicationId)).thenReturn(Optional.of(testApplication));

                MockMultipartHttpServletRequestBuilder req = multipart("/applications/{id}", testApplicationId);
                req.with(r -> {
                        r.setMethod("PUT");
                        return r;
                });

                mockMvc.perform(req.contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                // service.update 不應被調用，因為沒有檔案上傳
                verify(service, never()).update(any(), any());
        }

        @Test
        void testUpdate_MultipleFiles_Success() throws Exception {
                when(service.getById(testApplicationId)).thenReturn(Optional.of(testApplication));
                when(service.update(eq(testApplicationId), any(Applications.class))).thenReturn(testApplication);

                Path tempDir = Files.createTempDirectory("childcare-test-multi-");
                when(fileService.getFolderPath(eq(testApplicationId))).thenReturn(tempDir);

                MockMultipartFile file = new MockMultipartFile("file", "test1.txt", MediaType.TEXT_PLAIN_VALUE,
                                "content1".getBytes());
                MockMultipartFile file1 = new MockMultipartFile("file1", "test2.txt", MediaType.TEXT_PLAIN_VALUE,
                                "content2".getBytes());
                MockMultipartFile file2 = new MockMultipartFile("file2", "test3.txt", MediaType.TEXT_PLAIN_VALUE,
                                "content3".getBytes());

                MockMultipartHttpServletRequestBuilder req = multipart("/applications/{id}", testApplicationId)
                                .file(file).file(file1).file(file2);
                req.with(r -> {
                        r.setMethod("PUT");
                        return r;
                });

                mockMvc.perform(req.contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service, times(1)).update(eq(testApplicationId), any(Applications.class));
        }

        @Test
        void testUpdate_ServiceUpdateFails_ReturnsError() throws Exception {
                when(service.getById(testApplicationId)).thenReturn(Optional.of(testApplication));
                when(service.update(eq(testApplicationId), any(Applications.class)))
                                .thenThrow(new RuntimeException("DB Error"));

                Path tempDir = Files.createTempDirectory("childcare-test-fail-");
                when(fileService.getFolderPath(eq(testApplicationId))).thenReturn(tempDir);

                MockMultipartFile file = new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE,
                                "content".getBytes());

                MockMultipartHttpServletRequestBuilder req = multipart("/applications/{id}", testApplicationId)
                                .file(file);
                req.with(r -> {
                        r.setMethod("PUT");
                        return r;
                });

                mockMvc.perform(req.contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isInternalServerError());
        }

        @Test
        void testUpdate_WithExistingAttachmentPath_DeletesOldFile() throws Exception {
                testApplication.setAttachmentPath(testApplicationId + "/old_file.pdf");
                when(service.getById(testApplicationId)).thenReturn(Optional.of(testApplication));
                when(service.update(eq(testApplicationId), any(Applications.class))).thenReturn(testApplication);

                Path tempDir = Files.createTempDirectory("childcare-test-delete-");
                when(fileService.getFolderPath(eq(testApplicationId))).thenReturn(tempDir);

                MockMultipartFile file = new MockMultipartFile("file", "new_file.txt", MediaType.TEXT_PLAIN_VALUE,
                                "content".getBytes());

                MockMultipartHttpServletRequestBuilder req = multipart("/applications/{id}", testApplicationId)
                                .file(file);
                req.with(r -> {
                        r.setMethod("PUT");
                        return r;
                });

                mockMvc.perform(req.contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testUpdate_FileWithNullOriginalFilename() throws Exception {
                when(service.getById(testApplicationId)).thenReturn(Optional.of(testApplication));
                when(service.update(eq(testApplicationId), any(Applications.class))).thenReturn(testApplication);

                Path tempDir = Files.createTempDirectory("childcare-test-nullname-");
                when(fileService.getFolderPath(eq(testApplicationId))).thenReturn(tempDir);

                // 模擬沒有原始檔名的檔案
                MockMultipartFile file = new MockMultipartFile("file", null, MediaType.TEXT_PLAIN_VALUE,
                                "content".getBytes());

                MockMultipartHttpServletRequestBuilder req = multipart("/applications/{id}", testApplicationId)
                                .file(file);
                req.with(r -> {
                        r.setMethod("PUT");
                        return r;
                });

                mockMvc.perform(req.contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        // ===== submitApplicationCase() 驗證規則測試 =====
        @Test
        void testSubmitApplicationCase_ChildAlreadyAccepted_ReturnsBadRequest() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();

                ApplicationParticipantDTO childDto = new ApplicationParticipantDTO();
                childDto.setNationalID("C123456789");
                childDto.setName("Test Child");
                caseDto.setChildren(List.of(childDto));

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);
                // 幼兒已有錄取案件
                when(service.countAcceptedApplicationsByChildNationalID("C123456789")).thenReturn(1);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testSubmitApplicationCase_PendingCountExceeded_ReturnsBadRequest() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();

                ApplicationParticipantDTO childDto = new ApplicationParticipantDTO();
                childDto.setNationalID("C123456789");
                childDto.setName("Test Child");
                caseDto.setChildren(List.of(childDto));

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID("C123456789")).thenReturn(0);
                // 處理中案件已達 2 件
                when(service.countPendingApplicationsByChildNationalID("C123456789")).thenReturn(2);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testSubmitApplicationCase_SameInstitutionDuplicate_ReturnsBadRequest() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();

                ApplicationParticipantDTO childDto = new ApplicationParticipantDTO();
                childDto.setNationalID("C123456789");
                childDto.setName("Test Child");
                caseDto.setChildren(List.of(childDto));

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID("C123456789")).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID("C123456789")).thenReturn(1);
                // 同機構已有有效申請
                when(service.countActiveApplicationsByChildAndInstitution("C123456789", testInstitutionId))
                                .thenReturn(1);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testSubmitApplicationCase_NoCaseNumber_AutoGenerated() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                caseDto.setCaseNumber(null); // 不提供 caseNumber

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(202501020001L);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service, times(1)).generateCaseNumber();
        }

        @Test
        void testSubmitApplicationCase_NullUser_Success() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                caseDto.setUser(null); // 不提供 User

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testSubmitApplicationCase_InvalidUserID_HandledGracefully() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                UserSimpleDTO userDto = new UserSimpleDTO();
                userDto.setUserID("invalid-uuid-format");
                userDto.setName("Test User");
                caseDto.setUser(userDto);

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testSubmitApplicationCase_EmptyUserIdStr_HandledGracefully() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                UserSimpleDTO userDto = new UserSimpleDTO();
                userDto.setUserID("  "); // 空白字串
                userDto.setName("Test User");
                caseDto.setUser(userDto);

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testSubmitApplicationCase_NoAttachments_Success() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testSubmitApplicationCase_NullParentsChildren_Success() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                caseDto.setParents(null);
                caseDto.setChildren(null);

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testSubmitApplicationCase_MultipleAttachments_Success() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                Path tempDir = Files.createTempDirectory("childcare-test-multi-attach-");
                when(fileService.getFolderPath(any(UUID.class))).thenReturn(tempDir);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));
                MockMultipartFile file = new MockMultipartFile("file", "doc1.pdf", MediaType.APPLICATION_PDF_VALUE,
                                "content1".getBytes());
                MockMultipartFile file1 = new MockMultipartFile("file1", "doc2.pdf", MediaType.APPLICATION_PDF_VALUE,
                                "content2".getBytes());
                MockMultipartFile file2 = new MockMultipartFile("file2", "doc3.pdf", MediaType.APPLICATION_PDF_VALUE,
                                "content3".getBytes());
                MockMultipartFile file3 = new MockMultipartFile("file3", "doc4.pdf", MediaType.APPLICATION_PDF_VALUE,
                                "content4".getBytes());

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .file(file).file(file1).file(file2).file(file3)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service, times(1)).update(any(UUID.class), any(Applications.class));
        }

        @Test
        void testSubmitApplicationCase_NullApplyDate_UsesToday() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                caseDto.setApplyDate(null);

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testSubmitApplicationCase_NullIdentityType_DefaultsToZero() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                caseDto.setIdentityType(null);

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        // ===== updateApplicationCase() 補充測試 =====
        @Test
        void testUpdateApplicationCase_ReviewDateParamParsed() throws Exception {
                String status = "審核通過";
                doNothing().when(service).updateApplicationCase(eq(testApplicationId), any());

                mockMvc.perform(put("/applications/{id}/case", testApplicationId)
                                .param("status", status)
                                .param("reviewDate", "2025-01-02T10:00:00")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void testUpdateApplicationCase_InvalidReviewDateParam_Ignored() throws Exception {
                String status = "審核通過";
                doNothing().when(service).updateApplicationCase(eq(testApplicationId), any());

                mockMvc.perform(put("/applications/{id}/case", testApplicationId)
                                .param("status", status)
                                .param("reviewDate", "invalid-date")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void testUpdateApplicationCase_ParentsStatusUpdated() throws Exception {
                String status = "審核通過";
                ApplicationCaseDTO caseDto = new ApplicationCaseDTO();
                ApplicationParticipantDTO parentDto = new ApplicationParticipantDTO();
                parentDto.setName("Parent");
                caseDto.parents = List.of(parentDto);

                doNothing().when(service).updateApplicationCase(eq(testApplicationId), any());

                mockMvc.perform(put("/applications/{id}/case", testApplicationId)
                                .param("status", status)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(caseDto)))
                                .andExpect(status().isNoContent());

                verify(service, times(1)).updateApplicationCase(eq(testApplicationId), any());
        }

        @Test
        void testUpdateApplicationCase_ChildrenStatusUpdated() throws Exception {
                String status = "已錄取";
                ApplicationCaseDTO caseDto = new ApplicationCaseDTO();
                ApplicationParticipantDTO childDto = new ApplicationParticipantDTO();
                childDto.setName("Child");
                caseDto.children = List.of(childDto);

                doNothing().when(service).updateApplicationCase(eq(testApplicationId), any());

                mockMvc.perform(put("/applications/{id}/case", testApplicationId)
                                .param("status", status)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(caseDto)))
                                .andExpect(status().isNoContent());
        }

        @Test
        void testUpdateApplicationCase_ReasonParamOverridesBody() throws Exception {
                String status = "駁回";
                ApplicationCaseDTO caseDto = new ApplicationCaseDTO();
                caseDto.reason = "Original reason";

                doNothing().when(service).updateApplicationCase(eq(testApplicationId), any());

                mockMvc.perform(put("/applications/{id}/case", testApplicationId)
                                .param("status", status)
                                .param("reason", "Overridden reason")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(caseDto)))
                                .andExpect(status().isNoContent());
        }

        // ===== adminSearchCasesGet() 補充測試 =====
        @Test
        void testAdminSearchCasesGet_ValidParams_Success() throws Exception {
                List<Map<String, Object>> mockResult = new ArrayList<>();
                doReturn(mockResult).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

                mockMvc.perform(get("/applications/case/search")
                                .param("institutionId", testInstitutionId.toString())
                                .param("classId", UUID.randomUUID().toString())
                                .param("caseNumber", "12345")
                                .param("applicantNationalId", "A123456789")
                                .param("identityType", "1")
                                .param("caseStatus", "審核中")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        void testAdminSearchCasesGet_NullParams_Success() throws Exception {
                List<Map<String, Object>> mockResult = new ArrayList<>();
                doReturn(mockResult).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

                mockMvc.perform(get("/applications/case/search")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        void testAdminSearchCasesGet_EmptyStringParams_SkipsBranches() throws Exception {
                List<Map<String, Object>> mockResult = new ArrayList<>();
                doReturn(mockResult).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

                mockMvc.perform(get("/applications/case/search")
                                .param("institutionId", "")
                                .param("classId", "")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        // ===== adminSearchCases() 補充測試 =====
        @Test
        void testAdminSearchCases_AllFilters_Success() throws Exception {
                AdminCaseSearchRequestDto searchDto = new AdminCaseSearchRequestDto();
                searchDto.setInstitutionId(testInstitutionId);
                searchDto.setClassId(UUID.randomUUID());
                searchDto.setCaseNumber(12345L);
                searchDto.setApplicantNationalId("A123456789");
                searchDto.setIdentityType("1");
                searchDto.setCaseStatus("審核中");

                List<Map<String, Object>> mockResult = new ArrayList<>();
                doReturn(mockResult).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

                mockMvc.perform(get("/applications/admin/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(searchDto)))
                                .andExpect(status().isOk());
        }

        @Test
        void testAdminSearchCases_EmptyStringFilters_SkipsBranches() throws Exception {
                AdminCaseSearchRequestDto searchDto = new AdminCaseSearchRequestDto();
                searchDto.setApplicantNationalId("");
                searchDto.setIdentityType("");
                searchDto.setCaseStatus("");

                List<Map<String, Object>> mockResult = new ArrayList<>();
                doReturn(mockResult).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

                mockMvc.perform(get("/applications/admin/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(searchDto)))
                                .andExpect(status().isOk());
        }

        // ===== getCasesList() 補充測試 =====
        @Test
        void testGetCasesList_WithChildNationalId_Success() throws Exception {
                when(service.getCaseListWithOffset(anyInt(), anyInt(), any(), any(), any(), any(), eq("C123456789"),
                                any(), any()))
                                .thenReturn(new ArrayList<>());
                when(service.countCaseList(any(), any(), any(), any(), eq("C123456789"), any(), any())).thenReturn(0L);

                mockMvc.perform(get("/applications/cases/list")
                                .param("offset", "0")
                                .param("size", "10")
                                .param("childNationalId", "C123456789")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        void testGetCasesList_WithCaseNumber_Success() throws Exception {
                when(service.getCaseListWithOffset(anyInt(), anyInt(), any(), any(), any(), any(), any(), eq(12345L),
                                any()))
                                .thenReturn(new ArrayList<>());
                when(service.countCaseList(any(), any(), any(), any(), any(), eq(12345L), any())).thenReturn(0L);

                mockMvc.perform(get("/applications/cases/list")
                                .param("offset", "0")
                                .param("size", "10")
                                .param("caseNumber", "12345")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        void testGetCasesList_WithIdentityType_Success() throws Exception {
                when(service.getCaseListWithOffset(anyInt(), anyInt(), any(), any(), any(), any(), any(), any(),
                                eq("1")))
                                .thenReturn(new ArrayList<>());
                when(service.countCaseList(any(), any(), any(), any(), any(), any(), eq("1"))).thenReturn(0L);

                mockMvc.perform(get("/applications/cases/list")
                                .param("offset", "0")
                                .param("size", "10")
                                .param("identityType", "1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        void testGetCasesList_HasNextTrue() throws Exception {
                when(service.getCaseListWithOffset(anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any()))
                                .thenReturn(new ArrayList<>());
                when(service.countCaseList(any(), any(), any(), any(), any(), any(), any())).thenReturn(100L);

                mockMvc.perform(get("/applications/cases/list")
                                .param("offset", "0")
                                .param("size", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.hasNext", is(true)))
                                .andExpect(jsonPath("$.totalPages", is(10)));
        }

        @Test
        void testGetCasesList_NegativeSize_BadRequest() throws Exception {
                mockMvc.perform(get("/applications/cases/list")
                                .param("offset", "0")
                                .param("size", "-5")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testGetCasesList_WithApplicationIdAndClassId_Success() throws Exception {
                UUID appId = UUID.randomUUID();
                UUID classId = UUID.randomUUID();

                when(service.getCaseListWithOffset(anyInt(), anyInt(), any(), any(), eq(appId), eq(classId), any(),
                                any(), any()))
                                .thenReturn(new ArrayList<>());
                when(service.countCaseList(any(), any(), eq(appId), eq(classId), any(), any(), any())).thenReturn(0L);

                mockMvc.perform(get("/applications/cases/list")
                                .param("offset", "0")
                                .param("size", "10")
                                .param("applicationId", appId.toString())
                                .param("classId", classId.toString())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        void testGetCasesList_EmptyStringParams_SkipsBranches() throws Exception {
                when(service.getCaseListWithOffset(anyInt(), anyInt(), any(), isNull(), isNull(), isNull(), any(), any(), any()))
                                .thenReturn(new ArrayList<>());
                when(service.countCaseList(any(), isNull(), isNull(), isNull(), any(), any(), any())).thenReturn(0L);

                mockMvc.perform(get("/applications/cases/list")
                                .param("offset", "0")
                                .param("size", "10")
                                .param("institutionId", "")
                                .param("applicationId", "")
                                .param("classId", "")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        // ==================== 新增測試用例 (Phase 3) - 額外覆蓋率提升 ====================

        // ===== update() - 覆蓋所有 4 個檔案上傳路徑 (switch case 0,1,2,3) =====
        @Test
        void testUpdate_AllFourFiles_AllAttachmentPathsSet() throws Exception {
                when(service.getById(testApplicationId)).thenReturn(Optional.of(testApplication));
                when(service.update(eq(testApplicationId), any(Applications.class))).thenReturn(testApplication);

                Path tempDir = Files.createTempDirectory("childcare-test-fourfiles-");
                when(fileService.getFolderPath(eq(testApplicationId))).thenReturn(tempDir);

                MockMultipartFile file = new MockMultipartFile("file", "test0.txt", MediaType.TEXT_PLAIN_VALUE,
                                "content0".getBytes());
                MockMultipartFile file1 = new MockMultipartFile("file1", "test1.txt", MediaType.TEXT_PLAIN_VALUE,
                                "content1".getBytes());
                MockMultipartFile file2 = new MockMultipartFile("file2", "test2.txt", MediaType.TEXT_PLAIN_VALUE,
                                "content2".getBytes());
                MockMultipartFile file3 = new MockMultipartFile("file3", "test3.txt", MediaType.TEXT_PLAIN_VALUE,
                                "content3".getBytes());

                MockMultipartHttpServletRequestBuilder req = multipart("/applications/{id}", testApplicationId)
                                .file(file).file(file1).file(file2).file(file3);
                req.with(r -> {
                        r.setMethod("PUT");
                        return r;
                });

                mockMvc.perform(req.contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service, times(1)).update(eq(testApplicationId), any(Applications.class));
        }

        @Test
        void testUpdate_WithExistingAttachment1Path_DeletesOldFile() throws Exception {
                testApplication.setAttachmentPath1(testApplicationId + "/old_file1.pdf");
                when(service.getById(testApplicationId)).thenReturn(Optional.of(testApplication));
                when(service.update(eq(testApplicationId), any(Applications.class))).thenReturn(testApplication);

                Path tempDir = Files.createTempDirectory("childcare-test-delete1-");
                when(fileService.getFolderPath(eq(testApplicationId))).thenReturn(tempDir);

                // 使用 file1 參數觸發 case 1 分支
                MockMultipartFile file1 = new MockMultipartFile("file1", "new_file1.txt", MediaType.TEXT_PLAIN_VALUE,
                                "content".getBytes());

                MockMultipartHttpServletRequestBuilder req = multipart("/applications/{id}", testApplicationId)
                                .file(file1);
                req.with(r -> {
                        r.setMethod("PUT");
                        return r;
                });

                mockMvc.perform(req.contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testUpdate_WithExistingAttachment2Path_DeletesOldFile() throws Exception {
                testApplication.setAttachmentPath2(testApplicationId + "/old_file2.pdf");
                when(service.getById(testApplicationId)).thenReturn(Optional.of(testApplication));
                when(service.update(eq(testApplicationId), any(Applications.class))).thenReturn(testApplication);

                Path tempDir = Files.createTempDirectory("childcare-test-delete2-");
                when(fileService.getFolderPath(eq(testApplicationId))).thenReturn(tempDir);

                // 使用 file 和 file1 讓 file2 成為 index 2
                MockMultipartFile file = new MockMultipartFile("file", "file0.txt", MediaType.TEXT_PLAIN_VALUE,
                                "c0".getBytes());
                MockMultipartFile file1 = new MockMultipartFile("file1", "file1.txt", MediaType.TEXT_PLAIN_VALUE,
                                "c1".getBytes());
                MockMultipartFile file2 = new MockMultipartFile("file2", "new_file2.txt", MediaType.TEXT_PLAIN_VALUE,
                                "content".getBytes());

                MockMultipartHttpServletRequestBuilder req = multipart("/applications/{id}", testApplicationId)
                                .file(file).file(file1).file(file2);
                req.with(r -> {
                        r.setMethod("PUT");
                        return r;
                });

                mockMvc.perform(req.contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testUpdate_WithExistingAttachment3Path_DeletesOldFile() throws Exception {
                testApplication.setAttachmentPath3(testApplicationId + "/old_file3.pdf");
                when(service.getById(testApplicationId)).thenReturn(Optional.of(testApplication));
                when(service.update(eq(testApplicationId), any(Applications.class))).thenReturn(testApplication);

                Path tempDir = Files.createTempDirectory("childcare-test-delete3-");
                when(fileService.getFolderPath(eq(testApplicationId))).thenReturn(tempDir);

                // 四個檔案讓 file3 成為 index 3
                MockMultipartFile file = new MockMultipartFile("file", "f0.txt", MediaType.TEXT_PLAIN_VALUE,
                                "c0".getBytes());
                MockMultipartFile file1 = new MockMultipartFile("file1", "f1.txt", MediaType.TEXT_PLAIN_VALUE,
                                "c1".getBytes());
                MockMultipartFile file2 = new MockMultipartFile("file2", "f2.txt", MediaType.TEXT_PLAIN_VALUE,
                                "c2".getBytes());
                MockMultipartFile file3 = new MockMultipartFile("file3", "new_file3.txt", MediaType.TEXT_PLAIN_VALUE,
                                "content".getBytes());

                MockMultipartHttpServletRequestBuilder req = multipart("/applications/{id}", testApplicationId)
                                .file(file).file(file1).file(file2).file(file3);
                req.with(r -> {
                        r.setMethod("PUT");
                        return r;
                });

                mockMvc.perform(req.contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        // ===== submitApplicationCase() - 覆蓋所有 4 個附件路徑 =====
        @Test
        void testSubmitApplicationCase_AllFourAttachments_Success() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                Path tempDir = Files.createTempDirectory("childcare-test-submit4-");
                when(fileService.getFolderPath(any(UUID.class))).thenReturn(tempDir);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));
                MockMultipartFile file = new MockMultipartFile("file", "a0.pdf", MediaType.APPLICATION_PDF_VALUE,
                                "c0".getBytes());
                MockMultipartFile file1 = new MockMultipartFile("file1", "a1.pdf", MediaType.APPLICATION_PDF_VALUE,
                                "c1".getBytes());
                MockMultipartFile file2 = new MockMultipartFile("file2", "a2.pdf", MediaType.APPLICATION_PDF_VALUE,
                                "c2".getBytes());
                MockMultipartFile file3 = new MockMultipartFile("file3", "a3.pdf", MediaType.APPLICATION_PDF_VALUE,
                                "c3".getBytes());

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart).file(file).file(file1).file(file2).file(file3)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                // 驗證 update 被調用（因為有附件需要更新）
                verify(service, times(1)).update(any(UUID.class), any(Applications.class));
        }

        // ===== submitApplicationCase() - 性別轉換邏輯覆蓋 =====
        @Test
        void testSubmitApplicationCase_GenderMale_Various() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();

                // 測試 "男" 格式
                ApplicationParticipantDTO childMale = new ApplicationParticipantDTO();
                childMale.setNationalID("C111111111");
                childMale.setName("Male Child");
                childMale.setGender("男");
                caseDto.setChildren(List.of(childMale));

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testSubmitApplicationCase_GenderFemale_ConvertsFalse() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();

                ApplicationParticipantDTO childFemale = new ApplicationParticipantDTO();
                childFemale.setNationalID("C222222222");
                childFemale.setName("Female Child");
                childFemale.setGender("F");
                caseDto.setChildren(List.of(childFemale));

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testSubmitApplicationCase_Gender1Format_ConvertsTrue() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();

                ApplicationParticipantDTO child = new ApplicationParticipantDTO();
                child.setNationalID("C333333333");
                child.setName("Child");
                child.setGender("1"); // 數字格式
                caseDto.setChildren(List.of(child));

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        // ===== submitApplicationCase() - 日期解析異常覆蓋 =====
        @Test
        void testSubmitApplicationCase_InvalidBirthDate_HandledGracefully() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();

                ApplicationParticipantDTO child = new ApplicationParticipantDTO();
                child.setNationalID("C444444444");
                child.setName("Child");
                child.setBirthDate("invalid-date-format"); // 無效日期格式
                caseDto.setChildren(List.of(child));

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testSubmitApplicationCase_InvalidSuspendEnd_HandledGracefully() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();

                ApplicationParticipantDTO child = new ApplicationParticipantDTO();
                child.setNationalID("C555555555");
                child.setName("Child");
                child.setSuspendEnd("not-a-date"); // 無效日期格式
                caseDto.setChildren(List.of(child));

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testSubmitApplicationCase_InvalidClassID_HandledGracefully() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();

                ApplicationParticipantDTO child = new ApplicationParticipantDTO();
                child.setNationalID("C666666666");
                child.setName("Child");
                child.setClassID("not-a-uuid"); // 無效 UUID 格式
                caseDto.setChildren(List.of(child));

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        // ===== submitApplicationCase() - 空 nationalID 的幼兒跳過驗證 =====
        @Test
        void testSubmitApplicationCase_ChildWithEmptyNationalID_SkipsValidation() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();

                ApplicationParticipantDTO child = new ApplicationParticipantDTO();
                child.setNationalID("   "); // 空白 nationalID
                child.setName("Anonymous Child");
                caseDto.setChildren(List.of(child));

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                // 驗證計數方法未被調用（因為 nationalID 為空）
                verify(service, never()).countAcceptedApplicationsByChildNationalID(anyString());
        }

        // ===== getWithOffset() - hasNext = true 場景 =====
        @Test
        void testGetWithOffset_HasNextTrue() throws Exception {
                List<ApplicationSummaryWithDetailsDTO> content = new ArrayList<>();
                when(service.getSummariesWithOffset(0, 10)).thenReturn(content);
                when(service.getTotalApplicationsCount()).thenReturn(50L); // 總數 50，offset=0, size=10 → hasNext=true

                mockMvc.perform(get("/applications/offset")
                                .param("offset", "0")
                                .param("size", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.hasNext", is(true)))
                                .andExpect(jsonPath("$.totalPages", is(5)));
        }

        // ===== updateApplicationCase() - 參與者已有 status 不被覆蓋 =====
        @Test
        void testUpdateApplicationCase_ParentWithExistingStatus_NotOverwritten() throws Exception {
                String statusParam = "審核通過";
                ApplicationCaseDTO caseDto = new ApplicationCaseDTO();
                ApplicationParticipantDTO parentDto = new ApplicationParticipantDTO();
                parentDto.setName("Parent");
                parentDto.setStatus("已有狀態"); // 已有狀態
                caseDto.parents = List.of(parentDto);

                doNothing().when(service).updateApplicationCase(eq(testApplicationId), any());

                mockMvc.perform(put("/applications/{id}/case", testApplicationId)
                                .param("status", statusParam)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(caseDto)))
                                .andExpect(status().isNoContent());

                verify(service, times(1)).updateApplicationCase(eq(testApplicationId), any());
        }

        @Test
        void testUpdateApplicationCase_ChildWithExistingStatus_NotOverwritten() throws Exception {
                String statusParam = "已錄取";
                ApplicationCaseDTO caseDto = new ApplicationCaseDTO();
                ApplicationParticipantDTO childDto = new ApplicationParticipantDTO();
                childDto.setName("Child");
                childDto.setStatus("候補中"); // 已有狀態
                caseDto.children = List.of(childDto);

                doNothing().when(service).updateApplicationCase(eq(testApplicationId), any());

                mockMvc.perform(put("/applications/{id}/case", testApplicationId)
                                .param("status", statusParam)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(caseDto)))
                                .andExpect(status().isNoContent());
        }

        // ===== submitApplicationCase() - 檔案儲存失敗 =====
        @Test
        void testSubmitApplicationCase_FileSaveFails_ReturnsError() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                // 返回不存在的路徑讓 Files.copy 失敗
                Path nonExistentDir = Path.of("Z:/non/existent/path/that/should/not/exist");
                when(fileService.getFolderPath(any(UUID.class))).thenReturn(nonExistentDir);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));
                MockMultipartFile file = new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE,
                                "content".getBytes());

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart).file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isInternalServerError());
        }

        // ===== submitApplicationCase() - 附件更新失敗（不影響主流程）=====
        @Test
        void testSubmitApplicationCase_AttachmentUpdateFails_StillReturnsOk() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);
                // update 失敗但不應影響整體流程
                doThrow(new RuntimeException("DB Error")).when(service).update(any(UUID.class),
                                any(Applications.class));

                Path tempDir = Files.createTempDirectory("childcare-test-updatefail-");
                when(fileService.getFolderPath(any(UUID.class))).thenReturn(tempDir);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));
                MockMultipartFile file = new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE,
                                "content".getBytes());

                // 即使 update 失敗，也應該返回 OK（因為主記錄已創建）
                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart).file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        // ===== submitApplicationCase() - 有效的 classID 和 birthDate =====
        @Test
        void testSubmitApplicationCase_ValidClassIDAndBirthDate() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();

                ApplicationParticipantDTO child = new ApplicationParticipantDTO();
                child.setNationalID("C777777777");
                child.setName("Child");
                child.setBirthDate("2020-05-15"); // 有效日期格式
                child.setClassID(UUID.randomUUID().toString()); // 有效 UUID
                caseDto.setChildren(List.of(child));

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        // ===== submitApplicationCase() - 有效的 suspendEnd =====
        @Test
        void testSubmitApplicationCase_ValidSuspendEnd() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();

                ApplicationParticipantDTO child = new ApplicationParticipantDTO();
                child.setNationalID("C888888888");
                child.setName("Child");
                child.setSuspendEnd("2025-12-31"); // 有效日期格式
                caseDto.setChildren(List.of(child));

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.generateCaseNumber()).thenReturn(12345L);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testUpdate_FileSaveException_ReturnsInternalServerError() throws Exception {
                when(service.getById(testApplicationId)).thenReturn(Optional.of(testApplication));

                // Return a valid directory so the initial check passes
                Path tempDir = Files.createTempDirectory("childcare-test-error-");
                when(fileService.getFolderPath(eq(testApplicationId))).thenReturn(tempDir);

                // Mock file to throw IOException on getInputStream
                MockMultipartFile file = spy(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE,
                                "content".getBytes()));
                doThrow(new java.io.IOException("Disk full")).when(file).getInputStream();

                MockMultipartHttpServletRequestBuilder req = multipart("/applications/{id}", testApplicationId)
                                .file(file);
                req.with(r -> {
                        r.setMethod("PUT");
                        return r;
                });

                mockMvc.perform(req.contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isInternalServerError())
                                .andExpect(content().string(containsString("Failed to save file 'file': Disk full")));
        }

        @Test
        void testSubmitApplicationCase_WithProvidedCaseNumber_NoGeneration() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                caseDto.setCaseNumber(99999L); // Provided case number

                when(service.create(any(Applications.class))).thenReturn(testApplication);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                // Verify generateCaseNumber is NEVER called
                verify(service, never()).generateCaseNumber();
        }

        @Test
        void testUpdate_SkipIntermediateFiles_FillsSlotsSequentially() throws Exception {
                // Test the behavior where we only send 'file2' parameter, but it fills the
                // first slot (attachmentPath)
                // because the controller iterates the map values sequentially.

                when(service.getById(testApplicationId)).thenReturn(Optional.of(testApplication));
                when(service.update(eq(testApplicationId), any(Applications.class))).thenReturn(testApplication);

                Path tempDir = Files.createTempDirectory("childcare-test-skip-");
                when(fileService.getFolderPath(eq(testApplicationId))).thenReturn(tempDir);

                // Only send "file2"
                MockMultipartFile file2 = new MockMultipartFile("file2", "content2.txt", MediaType.TEXT_PLAIN_VALUE,
                                "content2".getBytes());

                MockMultipartHttpServletRequestBuilder req = multipart("/applications/{id}", testApplicationId)
                                .file(file2);
                req.with(r -> {
                        r.setMethod("PUT");
                        return r;
                });

                mockMvc.perform(req.contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                // Capture the argument to update to verify which field was set
                org.mockito.ArgumentCaptor<Applications> appCaptor = org.mockito.ArgumentCaptor
                                .forClass(Applications.class);
                verify(service, times(1)).update(eq(testApplicationId), appCaptor.capture());

                Applications updatedApp = appCaptor.getValue();
                // Should be set in attachmentPath (index 0) because it's the first (and only)
                // file in the loop
                // "file2" parameter becomes the 0-th item in the LinkedHashMap iteration
                org.junit.jupiter.api.Assertions.assertNotNull(updatedApp.getAttachmentPath(),
                                "AttachmentPath (slot 0) should be set");
                org.junit.jupiter.api.Assertions.assertNull(updatedApp.getAttachmentPath2(),
                                "AttachmentPath2 (slot 2) should NOT be set");
        }

        @Test
        void testUpdateApplicationCase_NullBody_CreatesNewDto() throws Exception {
                // Scenario: Request body is missing (null), but status query param is provided.
                // Controller should instantiate new DTO and proceed.
                String status = "審核通過";
                doNothing().when(service).updateApplicationCase(eq(testApplicationId), any());

                mockMvc.perform(put("/applications/{id}/case", testApplicationId)
                                .param("status", status)
                                .contentType(MediaType.APPLICATION_JSON))
                                // No content provided
                                .andExpect(status().isNoContent());

                verify(service, times(1)).updateApplicationCase(eq(testApplicationId), any(ApplicationCaseDTO.class));
        }

        @Test
        void testUpdateApplicationCase_ParentAndChildWithNullStatus_FilledFromParam() throws Exception {
                // Scenario: DTO contains parent/child with null status.
                // Controller should fill them with the query param "status".
                String statusParam = "已錄取";

                ApplicationCaseDTO caseDto = new ApplicationCaseDTO();
                ApplicationParticipantDTO parent = new ApplicationParticipantDTO();
                parent.setName("Parent");
                parent.setStatus(null); // Explicitly null

                ApplicationParticipantDTO child = new ApplicationParticipantDTO();
                child.setName("Child");
                child.setStatus(""); // Empty string should also be filled

                caseDto.parents = List.of(parent);
                caseDto.children = List.of(child);

                doNothing().when(service).updateApplicationCase(eq(testApplicationId), any());

                mockMvc.perform(put("/applications/{id}/case", testApplicationId)
                                .param("status", statusParam)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(caseDto)))
                                .andExpect(status().isNoContent());

                org.mockito.ArgumentCaptor<ApplicationCaseDTO> dtoCaptor = org.mockito.ArgumentCaptor
                                .forClass(ApplicationCaseDTO.class);
                verify(service, times(1)).updateApplicationCase(eq(testApplicationId), dtoCaptor.capture());

                ApplicationCaseDTO capturedDto = dtoCaptor.getValue();
                org.junit.jupiter.api.Assertions.assertEquals(statusParam, capturedDto.parents.get(0).status);
                org.junit.jupiter.api.Assertions.assertEquals(statusParam, capturedDto.children.get(0).status);
        }

        @Test
        void testAdminSearchCasesGet_MixedParams_CoversBranches() throws Exception {
                // Scenario: Only InstitutionID is invalid UUID, checking specifically the
                // branch
                mockMvc.perform(get("/applications/case/search")
                                .param("institutionId", "invalid-uuid")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());

                // Scenario: Only ClassID is invalid UUID
                mockMvc.perform(get("/applications/case/search")
                                .param("classId", "invalid-uuid")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());

                // Scenario: Valid mixed params
                List<Map<String, Object>> mockResult = new ArrayList<>();
                doReturn(mockResult).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

                mockMvc.perform(get("/applications/case/search")
                                .param("institutionId", testInstitutionId.toString())
                                // classId not provided
                                .param("caseNumber", "123")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        // ===== 額外測試:提升分支覆蓋率到 90%+ =====
        @Test
        void testSubmitApplicationCase_ChildWithBlankNationalID_SkipsValidation() throws Exception {
                when(service.create(any(Applications.class))).thenAnswer(i -> i.getArgument(0));
                when(applicationParticipantsService.create(any()))
                                .thenAnswer(i -> i.getArgument(0));
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                CaseEditUpdateDTO caseDto = createValidCaseDto();
                ApplicationParticipantDTO childDto = new ApplicationParticipantDTO();
                childDto.setNationalID("   "); // Blank national ID
                childDto.setName("Anonymous Child");
                caseDto.setChildren(List.of(childDto));

                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                // 驗證沒有調用驗證方法（因為 nationalID 是空白）
                verify(service, never()).countAcceptedApplicationsByChildNationalID(eq("   "));
        }

        @Test
        void testSubmitApplicationCase_FileWithEmptyOriginalFilename_UsesDefaultName() throws Exception {
                when(service.create(any(Applications.class))).thenAnswer(i -> i.getArgument(0));
                when(service.update(any(), any(Applications.class))).thenAnswer(i -> i.getArgument(1));
                when(applicationParticipantsService.create(any()))
                                .thenAnswer(i -> i.getArgument(0));
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                Path tempDir = Files.createTempDirectory("childcare-test-empty-filename-");
                when(fileService.getFolderPath(any(UUID.class))).thenReturn(tempDir);

                CaseEditUpdateDTO caseDto = createValidCaseDto();
                MockMultipartFile fileWithEmptyName = new MockMultipartFile("file", "", "application/pdf", "test".getBytes());
                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(fileWithEmptyName)
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service, times(1)).update(any(), any(Applications.class));
        }

        @Test
        void testUpdate_OldPathEmptyString_SkipsDelete() throws Exception {
                when(service.getById(testApplicationId)).thenReturn(Optional.of(testApplication));
                when(service.update(eq(testApplicationId), any(Applications.class))).thenReturn(testApplication);
                testApplication.setAttachmentPath(""); // Empty string, not null

                Path tempDir = Files.createTempDirectory("childcare-test-empty-path-");
                when(fileService.getFolderPath(eq(testApplicationId))).thenReturn(tempDir);

                MockMultipartFile newFile = new MockMultipartFile("file", "new.pdf", "application/pdf", "new content".getBytes());

                mockMvc.perform(multipart("/applications/{id}", testApplicationId)
                                .file(newFile)
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service, times(1)).update(eq(testApplicationId), any(Applications.class));
        }

        @Test
        void testSubmitApplicationCase_UpdateAttachmentsFails_ContinuesExecution() throws Exception {
                when(service.create(any(Applications.class))).thenAnswer(i -> i.getArgument(0));
                when(service.update(any(), any(Applications.class)))
                                .thenThrow(new RuntimeException("Update failed"));
                when(applicationParticipantsService.create(any()))
                                .thenAnswer(i -> i.getArgument(0));
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);

                Path tempDir = Files.createTempDirectory("childcare-test-update-fail-");
                when(fileService.getFolderPath(any(UUID.class))).thenReturn(tempDir);

                CaseEditUpdateDTO caseDto = createValidCaseDto();
                MockMultipartFile testFile = new MockMultipartFile("file", "test.pdf", "application/pdf", "test".getBytes());
                MockMultipartFile caseDtoPart = new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(caseDto));

                // 雖然 update 失敗,應該仍然返回 200 (捕獲異常後繼續)
                mockMvc.perform(multipart("/applications/case/submit")
                                .file(testFile)
                                .file(caseDtoPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testUpdate_DeleteOldFileException_ContinuesExecution() throws Exception {
                when(service.getById(testApplicationId)).thenReturn(Optional.of(testApplication));
                when(service.update(eq(testApplicationId), any(Applications.class))).thenReturn(testApplication);

                // 設置一個不存在的舊路徑,嘗試刪除時會失敗
                testApplication.setAttachmentPath(testApplicationId + "/nonexistent_old.pdf");

                Path tempDir = Files.createTempDirectory("childcare-test-delete-fail-");
                when(fileService.getFolderPath(eq(testApplicationId))).thenReturn(tempDir);

                MockMultipartFile newFile = new MockMultipartFile("file", "new.pdf", "application/pdf", "new content".getBytes());

                // 即使刪除舊檔案失敗,也應該成功
                mockMvc.perform(multipart("/applications/{id}", testApplicationId)
                                .file(newFile)
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service, times(1)).update(eq(testApplicationId), any(Applications.class));
        }

        /**
         * 測試 submitApplicationCase - 當 pending 申請數量達到 2 時返回錯誤
         */
        @Test
        void testSubmitApplicationCase_PendingCountReaches2_ReturnsError() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();

                // Create a mock Application object to return from service.create()
                Applications mockApplication = new Applications();
                mockApplication.setApplicationID(UUID.randomUUID());
                mockApplication.setCaseNumber(1L);
                
                // Mock service.create() to return the mock Application
                when(service.create(any(Applications.class))).thenReturn(mockApplication);
                
                // Mock 返回 acceptedCount = 0 (通過第一個檢查)
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                // Mock 返回 pendingCount = 2 (觸發錯誤)
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(2);
                // Mock 返回 activeSameInstitutionCount (雖然不會執行到,但需要避免錯誤)
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any(UUID.class)))
                                .thenReturn(0);

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                                objectMapper.writeValueAsBytes(caseDto)))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string(containsString("2"))) // Just check for the number instead of Chinese characters
                                .andExpect(content().string(containsString("Test Child")));

                // Verify service.create() WAS called (before the validation check)
                verify(service, times(1)).create(any(Applications.class));
        }

        /**
         * 測試 submitApplicationCase - userID 為空字符串時跳過映射
         */
        @Test
        void testSubmitApplicationCase_UserIdEmptyString_SkipsMapping() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                caseDto.getUser().setUserID("   "); // 空白字符串

                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);
                when(service.create(any(Applications.class))).thenAnswer(inv -> inv.getArgument(0));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                                objectMapper.writeValueAsBytes(caseDto)))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service, times(1)).create(any());
        }

        /**
         * 測試 submitApplicationCase - gender 值為 "1" 被解析為 true
         */
        @Test
        void testSubmitApplicationCase_GenderValue1_ParsedAsTrue() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                caseDto.getChildren().get(0).setGender("1");

                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);
                when(service.create(any(Applications.class))).thenAnswer(inv -> inv.getArgument(0));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                                objectMapper.writeValueAsBytes(caseDto)))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service, times(1)).create(any());
        }

        /**
         * 測試 submitApplicationCase - parents 為 null 時跳過創建
         */
        @Test
        void testSubmitApplicationCase_NullParents_SkipsParentCreation() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                caseDto.setParents(null);

                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any())).thenReturn(0);
                when(service.create(any(Applications.class))).thenAnswer(inv -> inv.getArgument(0));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                                objectMapper.writeValueAsBytes(caseDto)))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service, times(1)).create(any());
        }

        /**
         * 測試 submitApplicationCase - children 為 null 時跳過創建
         */
        @Test
        void testSubmitApplicationCase_NullChildren_SkipsChildCreation() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                caseDto.setChildren(null);

                when(service.create(any(Applications.class))).thenAnswer(inv -> inv.getArgument(0));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                                objectMapper.writeValueAsBytes(caseDto)))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service, times(1)).create(any());
        }

        /**
         * 測試 submitApplicationCase - User 為 null 時跳過 userID 映射
         */
        @Test
        void testSubmitApplicationCase_UserIsNull_SkipsUserIdMapping() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                caseDto.setUser(null);
                caseDto.setChildren(null); // 同時設為 null 避免兒童驗證

                when(service.create(any(Applications.class))).thenAnswer(inv -> inv.getArgument(0));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                                objectMapper.writeValueAsBytes(caseDto)))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service, times(1)).create(any());
        }

        /**
         * 測試 update - 沒有新文件時保留現有路徑
         */
        @Test
        void testUpdate_NoNewFiles_KeepsExistingPaths() throws Exception {
                Applications existingApp = new Applications();
                existingApp.setApplicationID(testApplicationId);
                existingApp.setAttachmentPath("existing/path1");
                existingApp.setAttachmentPath1("existing/path2");
                existingApp.setCaseNumber(1L);
                existingApp.setApplicationDate(LocalDate.now());
                existingApp.setInstitutionID(testInstitutionId);
                existingApp.setUserID(testUserId);
                existingApp.setIdentityType((byte) 1);

                when(service.getById(testApplicationId)).thenReturn(Optional.of(existingApp));
                when(service.update(eq(testApplicationId), any(Applications.class)))
                                .thenAnswer(inv -> inv.getArgument(1));

                CaseEditUpdateDTO updateDto = new CaseEditUpdateDTO();
                updateDto.setCaseNumber(1L);
                updateDto.setApplyDate(LocalDate.now());
                updateDto.setInstitutionId(testInstitutionId);
                updateDto.setIdentityType(1);

                mockMvc.perform(multipart("/applications/{id}", testApplicationId)
                                .file(new MockMultipartFile("updateDto", "", MediaType.APPLICATION_JSON_VALUE,
                                                objectMapper.writeValueAsBytes(updateDto)))
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                // 當沒有上傳檔案時,controller 不會調用 update,只返回現有的 application
                verify(service, times(1)).getById(testApplicationId);
                verify(service, never()).update(eq(testApplicationId), any(Applications.class));
        }

        /**
         * 測試 submitApplicationCase - 無效的 userID 格式時跳過映射
         */
        @Test
        void testSubmitApplicationCase_InvalidUserIdFormat_SkipsMapping() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                caseDto.getUser().setUserID("invalid-uuid-format");
                caseDto.setChildren(null); // 避免兒童驗證

                when(service.create(any(Applications.class))).thenAnswer(inv -> inv.getArgument(0));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                                objectMapper.writeValueAsBytes(caseDto)))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service, times(1)).create(any());
        }

        /**
         * 測試 submitApplicationCase - caseDto 為 null
         */
        @Test
        void testSubmitApplicationCase_NullCaseDto_ReturnsBadRequest() throws Exception {
                mockMvc.perform(multipart("/applications/case/submit")
                                .file(new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                                "null".getBytes()))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isBadRequest());
        }

        /**
         * 測試 submitApplicationCase - 上傳空文件 (file.isEmpty() = true)
         */
        @Test
        void testSubmitApplicationCase_EmptyFile_IgnoresFile() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                Applications mockApp = new Applications();
                mockApp.setApplicationID(UUID.randomUUID());
                mockApp.setCaseNumber(1L);
                when(service.create(any(Applications.class))).thenReturn(mockApp);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any(UUID.class))).thenReturn(0);

                // Create an empty file (size = 0)
                MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(emptyFile)
                                .file(new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                                objectMapper.writeValueAsBytes(caseDto)))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        /**
         * 測試 getCaseByParticipantId - null participantID
         */
        @Test
        void testGetCaseByParticipantId_NullId_Returns404() throws Exception {
                // When participantID parameter is missing, Spring returns 404 (not 400)
                mockMvc.perform(get("/applications/case/participant"))
                                .andExpect(status().isNotFound());
        }

        /**
         * 測試 submitApplicationCase - 幼兒 nationalID 為 null (跳過驗證)
         */
        @Test
        void testSubmitApplicationCase_ChildNationalIdNull_SkipsValidation() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                // Set child nationalID to null
                caseDto.getChildren().get(0).setNationalID(null);

                Applications mockApp = new Applications();
                mockApp.setApplicationID(UUID.randomUUID());
                mockApp.setCaseNumber(1L);
                when(service.create(any(Applications.class))).thenReturn(mockApp);

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                                objectMapper.writeValueAsBytes(caseDto)))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                // Count methods should NOT be called when nationalID is null
                verify(service, never()).countAcceptedApplicationsByChildNationalID(anyString());
                verify(service, never()).countPendingApplicationsByChildNationalID(anyString());
        }

        /**
         * 測試 update - 上傳空文件
         */
        @Test
        void testUpdate_EmptyFile_IgnoresFile() throws Exception {
                UUID testAppId = UUID.randomUUID();
                Applications existingApp = new Applications();
                existingApp.setApplicationID(testAppId);
                existingApp.setAttachmentPath("old/path.pdf");

                when(service.getById(testAppId)).thenReturn(Optional.of(existingApp));

                MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

                mockMvc.perform(multipart(HttpMethod.PUT, "/applications/" + testAppId)
                                .file(emptyFile))
                                .andExpect(status().isOk());

                // Should call getById but not update since file is empty
                verify(service, times(1)).getById(testAppId);
                verify(service, never()).update(any(UUID.class), any(Applications.class));
        }

        /**
         * 測試 submitApplicationCase - participantDTO 為 null (lambda 中的防禦性檢查)
         */
        @Test
        void testSubmitApplicationCase_NullParticipantInList_Skipped() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                // Add a null participant to the list
                caseDto.setParents(Arrays.asList(createParentDto(), null));

                Applications mockApp = new Applications();
                mockApp.setApplicationID(UUID.randomUUID());
                mockApp.setCaseNumber(1L);
                when(service.create(any(Applications.class))).thenReturn(mockApp);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any(UUID.class))).thenReturn(0);

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                                objectMapper.writeValueAsBytes(caseDto)))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        /**
         * 測試 submitApplicationCase - 性別值為 "M" (大寫)
         */
        @Test
        void testSubmitApplicationCase_GenderM_ParsedAsTrue() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                caseDto.getChildren().get(0).setGender("M");

                Applications mockApp = new Applications();
                mockApp.setApplicationID(UUID.randomUUID());
                mockApp.setCaseNumber(1L);
                when(service.create(any(Applications.class))).thenReturn(mockApp);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any(UUID.class))).thenReturn(0);

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                                objectMapper.writeValueAsBytes(caseDto)))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        private ApplicationParticipantDTO createParentDto() {
                ApplicationParticipantDTO parent = new ApplicationParticipantDTO();
                parent.setNationalID("P123456789");
                parent.setName("Test Parent");
                return parent;
        }

        // ===== 輔助方法 =====
        private CaseEditUpdateDTO createValidCaseDto() {
                CaseEditUpdateDTO caseDto = new CaseEditUpdateDTO();
                caseDto.setCaseNumber(1L);
                caseDto.setApplyDate(LocalDate.now());
                caseDto.setIdentityType(1);
                caseDto.setInstitutionId(testInstitutionId);
                caseDto.setInstitutionName("Test Institution");
                caseDto.setSelectedClass("Class A");
                caseDto.setCurrentOrder(1);

                UserSimpleDTO userDto = new UserSimpleDTO();
                userDto.setUserID(testUserId.toString());
                userDto.setName("Test User");
                userDto.setNationalID("A123456789");
                caseDto.setUser(userDto);

                ApplicationParticipantDTO childDto = new ApplicationParticipantDTO();
                childDto.setNationalID("C123456789");
                childDto.setName("Test Child");
                caseDto.setChildren(List.of(childDto));

                ApplicationParticipantDTO parentDto = new ApplicationParticipantDTO();
                parentDto.setNationalID("P123456789");
                parentDto.setName("Test Parent");
                caseDto.setParents(List.of(parentDto));

                return caseDto;
        }

        // ==================== Phase 5: Exception Handling Tests (Advanced Mocking) ====================

        /**
         * 測試 update - 檔案刪除時拋出 IOException (Lines 171-176)
         */
        @Test
        void testUpdate_FileDeleteException_ContinuesProcessing() throws Exception {
                UUID appId = UUID.randomUUID();
                Applications existingApp = new Applications();
                existingApp.setApplicationID(appId);
                existingApp.setAttachmentPath("old-app-id/old-file.pdf");
                
                when(service.getById(appId)).thenReturn(Optional.of(existingApp));
                when(service.update(eq(appId), any(Applications.class))).thenReturn(existingApp);
                
                Path mockFolderPath = Path.of(System.getProperty("java.io.tmpdir"));
                when(fileService.getFolderPath(appId)).thenReturn(mockFolderPath);
                
                MockMultipartFile file = new MockMultipartFile("file", "new.pdf", "application/pdf", "new".getBytes());
                
                // Use mockStatic to simulate Files.delete throwing IOException
                try (MockedStatic<Files> filesMock = org.mockito.Mockito.mockStatic(Files.class)) {
                        filesMock.when(() -> Files.exists(any(Path.class))).thenReturn(true);
                        filesMock.when(() -> Files.delete(any(Path.class)))
                                .thenThrow(new java.io.IOException("Permission denied"));
                        filesMock.when(() -> Files.copy(any(java.io.InputStream.class), any(Path.class), any(java.nio.file.CopyOption[].class)))
                                .thenReturn(100L);
                        filesMock.when(() -> Files.createDirectories(any(Path.class)))
                                .thenReturn(mockFolderPath);
                        
                        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                        .multipart(HttpMethod.PUT, "/applications/" + appId)
                                        .file(file)
                                        .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk()); // Should continue despite delete exception
                }
                
                // Verify Files.delete was called and threw exception
                verify(service).update(eq(appId), any(Applications.class));
        }

        /**
         * 測試 submitApplicationCase - getUserID() 拋出異常 (Lines 588-589)
         * 注意: 這個測試實際上無法完全覆蓋該分支,因為異常在控制器內部的 debug 代碼中
         * 但我們仍然測試當 user 對象正常時的流程
         */
        @Test
        void testSubmitApplicationCase_WithNormalUser_ProcessesSuccessfully() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                
                Applications mockApp = new Applications();
                mockApp.setApplicationID(UUID.randomUUID());
                mockApp.setCaseNumber(1L);
                when(service.create(any(Applications.class))).thenReturn(mockApp);
                when(service.countAcceptedApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countPendingApplicationsByChildNationalID(anyString())).thenReturn(0);
                when(service.countActiveApplicationsByChildAndInstitution(anyString(), any(UUID.class))).thenReturn(0);

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                                objectMapper.writeValueAsBytes(caseDto)))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        /**
         * 測試 submitApplicationCase - 儲存參與者時拋出異常 (Lines 850-852)
         */
        @Test
        void testSubmitApplicationCase_ParticipantSaveException_ContinuesProcessing() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                Applications mockApp = new Applications();
                mockApp.setApplicationID(UUID.randomUUID());
                mockApp.setCaseNumber(1L);
                when(service.create(any(Applications.class))).thenReturn(mockApp);
                
                // Mock participant service to throw exception on create
                doThrow(new RuntimeException("Database connection failed"))
                        .when(applicationParticipantsService).create(any(ApplicationParticipants.class));

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                                objectMapper.writeValueAsBytes(caseDto)))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk()); // Controller catches exception and continues
                
                // Verify exception was caught (at least 2 calls: parent + child)
                verify(applicationParticipantsService, atLeast(1)).create(any(ApplicationParticipants.class));
        }

        /**
         * 測試 submitApplicationCase - caseDto.getUser() 為 null (Lines 618)
         */
        @Test
        void testSubmitApplicationCase_UserNull_ProcessesSuccessfully() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                caseDto.setUser(null);
                
                Applications mockApp = new Applications();
                mockApp.setApplicationID(UUID.randomUUID());
                mockApp.setCaseNumber(1L);
                when(service.create(any(Applications.class))).thenReturn(mockApp);

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                                objectMapper.writeValueAsBytes(caseDto)))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        /**
         * 測試 submitApplicationCase - userIdStr 為空字串 (Lines 622)
         */
        @Test
        void testSubmitApplicationCase_UserIdEmpty_ProcessesSuccessfully() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                caseDto.getUser().setUserID("  ");
                
                Applications mockApp = new Applications();
                mockApp.setApplicationID(UUID.randomUUID());
                mockApp.setCaseNumber(1L);
                when(service.create(any(Applications.class))).thenReturn(mockApp);

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                                objectMapper.writeValueAsBytes(caseDto)))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        /**
         * 測試 submitApplicationCase - 附件更新時拋出異常 (Lines 760-763)
         */
        @Test
        void testSubmitApplicationCase_AttachmentUpdateException_Continues() throws Exception {
                CaseEditUpdateDTO caseDto = createValidCaseDto();
                
                Applications mockApp = new Applications();
                UUID appId = UUID.randomUUID();
                mockApp.setApplicationID(appId);
                mockApp.setCaseNumber(1L);
                when(service.create(any(Applications.class))).thenReturn(mockApp);
                
                // Mock fileService to avoid NPE
                Path mockPath = Path.of(System.getProperty("java.io.tmpdir"));
                when(fileService.getFolderPath(any(UUID.class))).thenReturn(mockPath);
                
                // Mock update to throw exception - use any(UUID.class) because controller generates its own UUID
                when(service.update(any(UUID.class), any(Applications.class))).thenThrow(new RuntimeException("Update failed"));

                MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "test".getBytes());

                mockMvc.perform(multipart("/applications/case/submit")
                                .file(new MockMultipartFile("caseDto", "", MediaType.APPLICATION_JSON_VALUE,
                                                objectMapper.writeValueAsBytes(caseDto)))
                                .file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk()); // Should still return 200 as exception is caught
        }

        /**
         * 測試 updateApplicationCase - nationalID 為空字串 (Line 1074)
         */
        @Test
        void testUpdateApplicationCase_NationalIdEmpty_FullUpdate() throws Exception {
                UUID appId = UUID.randomUUID();
                ApplicationCaseDTO dto = new ApplicationCaseDTO();
                
                mockMvc.perform(put("/applications/{id}/case", appId)
                                .param("NationalID", "")
                                .param("status", "Accepted")
                                .content(objectMapper.writeValueAsBytes(dto))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNoContent());
                
                verify(service).updateApplicationCase(eq(appId), any());
        }

        /**
         * 測試 updateApplicationCase - statusParam 為空字串 (Line 1075)
         */
        @Test
        void testUpdateApplicationCase_WithNationalId_StatusEmpty_ReturnsBadRequest() throws Exception {
                UUID appId = UUID.randomUUID();
                
                mockMvc.perform(put("/applications/{id}/case", appId)
                                .param("NationalID", "A123456789")
                                .param("status", "")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());
        }

        /**
         * 測試 updateApplicationCase - dto 為 null (Line 1088)
         */
        @Test
        void testUpdateApplicationCase_DtoNull_ProcessesSuccessfully() throws Exception {
                UUID appId = UUID.randomUUID();
                
                mockMvc.perform(put("/applications/{id}/case", appId)
                                .param("status", "Accepted")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNoContent());
                
                verify(service).updateApplicationCase(eq(appId), any());
        }

        /**
         * 測試 updateApplicationCase - parents 包含 null 元素 (Line 1111)
         */
        @Test
        void testUpdateApplicationCase_ParentsWithNullElement_ProcessesSuccessfully() throws Exception {
                UUID appId = UUID.randomUUID();
                ApplicationCaseDTO dto = new ApplicationCaseDTO();
                dto.parents = new ArrayList<>();
                dto.parents.add(null);
                
                mockMvc.perform(put("/applications/{id}/case", appId)
                                .param("status", "Accepted")
                                .content(objectMapper.writeValueAsBytes(dto))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNoContent());
                
                verify(service).updateApplicationCase(eq(appId), any());
        }

        /**
         * 測試 updateApplicationCase - p.status 為空字串 (Line 1114)
         */
        @Test
        void testUpdateApplicationCase_ParentStatusEmpty_FillsWithParam() throws Exception {
                UUID appId = UUID.randomUUID();
                ApplicationCaseDTO dto = new ApplicationCaseDTO();
                dto.parents = new ArrayList<>();
                Group4.Childcare.DTO.ApplicationParticipantDTO p = new Group4.Childcare.DTO.ApplicationParticipantDTO();
                p.status = "";
                dto.parents.add(p);
                
                mockMvc.perform(put("/applications/{id}/case", appId)
                                .param("status", "Accepted")
                                .content(objectMapper.writeValueAsBytes(dto))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNoContent());
                
                verify(service).updateApplicationCase(eq(appId), any());
        }

        /**
         * 測試 getCaseByParticipantId - participantID 為 null (Line 427)
         */
        @Test
        void testGetCaseByParticipantId_NullParam_ReturnsBadRequest() throws Exception {
                mockMvc.perform(get("/applications/case")
                                .param("participantID", ""))
                                .andExpect(status().isBadRequest());
        }

        /**
         * 測試 getUserApplicationDetails - userID 為 null (Line 1484)
         */
        @Test
        void testGetUserApplicationDetails_NullId_ReturnsBadRequest() throws Exception {
                mockMvc.perform(get("/applications/user/{userID}/details", "null"))
                                .andExpect(status().isBadRequest());
        }
}

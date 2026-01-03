package Group4.Childcare.controller;

import Group4.Childcare.Service.RevokeService;
import Group4.Childcare.DTO.RevokeApplicationDTO;
import Group4.Childcare.DTO.RevokeSearchRequest;
import Group4.Childcare.DTO.UpdateConfirmDateRequest;
import Group4.Childcare.DTO.CreateRevokeRequest;
import Group4.Childcare.DTO.ApplicationParticipantDTO;
import Group4.Childcare.Controller.RevokeController;
import Group4.Childcare.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RevokeController 單元測試
 * 
 * 測試設計：
 * 1. 等價類劃分：有效/無效參數
 * 2. 邊界值分析：分頁參數邊界
 * 3. 異常處理測試
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class RevokeControllerTest {

        @Mock
        private RevokeService revokeService;

        @InjectMocks
        private RevokeController controller;

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
                objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
        }

        // ===== getRevokedApplications 測試 =====
        @Test
        void testGetRevokedApplications_Success() throws Exception {
                List<RevokeApplicationDTO> content = new ArrayList<>();
                when(revokeService.getRevokedApplications(anyInt(), anyInt(), any(), any(), any()))
                                .thenReturn(content);
                when(revokeService.getTotalRevokedApplications(any(), any(), any()))
                                .thenReturn(0L);

                mockMvc.perform(get("/revoke/applications")
                                .param("offset", "0")
                                .param("size", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.offset", is(0)))
                                .andExpect(jsonPath("$.size", is(10)));
        }

        @Test
        void testGetRevokedApplications_WithFilters() throws Exception {
                List<RevokeApplicationDTO> content = new ArrayList<>();
                when(revokeService.getRevokedApplications(anyInt(), anyInt(), anyString(), anyString(), anyString()))
                                .thenReturn(content);
                when(revokeService.getTotalRevokedApplications(anyString(), anyString(), anyString()))
                                .thenReturn(5L);

                mockMvc.perform(get("/revoke/applications")
                                .param("offset", "0")
                                .param("size", "10")
                                .param("institutionID", "test-institution")
                                .param("caseNumber", "12345")
                                .param("nationalID", "A123456789")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalElements", is(5)));
        }

        @Test
        void testGetRevokedApplications_NegativeOffset() throws Exception {
                mockMvc.perform(get("/revoke/applications")
                                .param("offset", "-1")
                                .param("size", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testGetRevokedApplications_ZeroSize() throws Exception {
                mockMvc.perform(get("/revoke/applications")
                                .param("offset", "0")
                                .param("size", "0")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testGetRevokedApplications_LargeSizeCapped() throws Exception {
                List<RevokeApplicationDTO> content = new ArrayList<>();
                when(revokeService.getRevokedApplications(eq(0), eq(100), any(), any(), any()))
                                .thenReturn(content);
                when(revokeService.getTotalRevokedApplications(any(), any(), any()))
                                .thenReturn(200L);

                mockMvc.perform(get("/revoke/applications")
                                .param("offset", "0")
                                .param("size", "500")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.size", is(100)));
        }

        // ===== searchRevokedApplications 測試 =====
        @Test
        void testSearchRevokedApplications_Success() throws Exception {
                List<RevokeApplicationDTO> content = new ArrayList<>();
                when(revokeService.searchRevokedApplicationsPaged(any(), any(), anyInt(), anyInt(), any()))
                                .thenReturn(content);
                when(revokeService.countSearchRevokedApplications(any(), any(), any()))
                                .thenReturn(0L);

                mockMvc.perform(get("/revoke/search")
                                .param("caseNumber", "12345")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        void testSearchRevokedApplications_InvalidOffset() throws Exception {
                mockMvc.perform(get("/revoke/search")
                                .param("offset", "-1")
                                .param("size", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error", containsString("offset")));
        }

        // ===== getRevokeDetails 測試 =====
        @Test
        void testGetRevokeDetails_Success() throws Exception {
                RevokeSearchRequest request = new RevokeSearchRequest();
                request.setCancellationID("test-cancel-id");
                request.setNationalID("A123456789");

                List<ApplicationParticipantDTO> parents = new ArrayList<>();
                RevokeApplicationDTO revokeInfo = new RevokeApplicationDTO(
                                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                                "testUser", UUID.randomUUID(), "testInstitution", "testReason",
                                "A123456789", "12345");
                ApplicationParticipantDTO appDetail = new ApplicationParticipantDTO();

                when(revokeService.getParentsByCancellation("test-cancel-id")).thenReturn(parents);
                when(revokeService.getRevokeByCancellationID("test-cancel-id")).thenReturn(revokeInfo);
                when(revokeService.getApplicationDetailByCancellationAndNationalID("test-cancel-id", "A123456789"))
                                .thenReturn(appDetail);

                mockMvc.perform(post("/revoke/details")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        void testGetRevokeDetails_MissingCancellationID() throws Exception {
                RevokeSearchRequest request = new RevokeSearchRequest();
                request.setNationalID("A123456789");

                mockMvc.perform(post("/revoke/details")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error", containsString("cancellationID")));
        }

        @Test
        void testGetRevokeDetails_MissingNationalID() throws Exception {
                RevokeSearchRequest request = new RevokeSearchRequest();
                request.setCancellationID("test-cancel-id");

                mockMvc.perform(post("/revoke/details")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testGetRevokeDetails_NotFound() throws Exception {
                RevokeSearchRequest request = new RevokeSearchRequest();
                request.setCancellationID("non-existent");
                request.setNationalID("A123456789");

                when(revokeService.getParentsByCancellation("non-existent"))
                                .thenThrow(new EmptyResultDataAccessException(1));

                mockMvc.perform(post("/revoke/details")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound());
        }

        // ===== updateConfirmDate 測試 =====
        @Test
        void testUpdateConfirmDate_Success() throws Exception {
                UpdateConfirmDateRequest request = new UpdateConfirmDateRequest();
                request.setCancellationID("test-cancel-id");
                request.setConfirmDate(LocalDate.now());

                when(revokeService.updateConfirmDate("test-cancel-id", request.getConfirmDate()))
                                .thenReturn(1);

                mockMvc.perform(put("/revoke/confirm-date")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)));
        }

        @Test
        void testUpdateConfirmDate_NotFound() throws Exception {
                UpdateConfirmDateRequest request = new UpdateConfirmDateRequest();
                request.setCancellationID("non-existent");
                request.setConfirmDate(LocalDate.now());

                when(revokeService.updateConfirmDate("non-existent", request.getConfirmDate()))
                                .thenReturn(0);

                mockMvc.perform(put("/revoke/confirm-date")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound());
        }

        @Test
        void testUpdateConfirmDate_MissingParams() throws Exception {
                UpdateConfirmDateRequest request = new UpdateConfirmDateRequest();

                mockMvc.perform(put("/revoke/confirm-date")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        // ===== updateParticipantStatus 測試 =====
        @Test
        void testUpdateParticipantStatus_Success() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("ApplicationID", "test-app-id");
                request.put("NationalID", "A123456789");
                request.put("Status", "已撤銷");

                when(revokeService.updateApplicationParticipantStatus("test-app-id", "A123456789", "已撤銷"))
                                .thenReturn(1);

                mockMvc.perform(put("/revoke/update-participant-status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)));
        }

        @Test
        void testUpdateParticipantStatus_MissingFields() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("ApplicationID", "test-app-id");

                mockMvc.perform(put("/revoke/update-participant-status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateParticipantStatus_NotFound() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("ApplicationID", "non-existent");
                request.put("NationalID", "A123456789");
                request.put("Status", "已撤銷");

                when(revokeService.updateApplicationParticipantStatus("non-existent", "A123456789", "已撤銷"))
                                .thenReturn(0);

                mockMvc.perform(put("/revoke/update-participant-status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound());
        }

        // ===== createCancellation 測試 =====
        @Test
        void testCreateCancellation_Success() throws Exception {
                CreateRevokeRequest request = new CreateRevokeRequest();
                request.setNationalID("A123456789");
                request.setAbandonReason("個人因素");
                request.setApplicationID("test-app-id");
                request.setCaseNumber("12345");

                doNothing().when(revokeService).createCancellation(anyString(), anyString(), anyString(), anyString());

                mockMvc.perform(post("/revoke/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)));
        }

        @Test
        void testCreateCancellation_MissingFields() throws Exception {
                CreateRevokeRequest request = new CreateRevokeRequest();
                request.setNationalID("A123456789");

                mockMvc.perform(post("/revoke/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testCreateCancellation_Exception() throws Exception {
                CreateRevokeRequest request = new CreateRevokeRequest();
                request.setNationalID("A123456789");
                request.setAbandonReason("個人因素");
                request.setApplicationID("test-app-id");
                request.setCaseNumber("12345");

                doThrow(new RuntimeException("Database error"))
                                .when(revokeService)
                                .createCancellation(anyString(), anyString(), anyString(), anyString());

                mockMvc.perform(post("/revoke/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());
        }

        // ===== 額外的邊界條件和異常測試 =====

        @Test
        void testGetRevokedApplications_WithPagination() throws Exception {
                List<RevokeApplicationDTO> content = new ArrayList<>();
                when(revokeService.getRevokedApplications(eq(1), eq(10), any(), any(), any()))
                                .thenReturn(content);
                when(revokeService.getTotalRevokedApplications(any(), any(), any()))
                                .thenReturn(25L);

                mockMvc.perform(get("/revoke/applications")
                                .param("offset", "10")
                                .param("size", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.hasNext", is(true)))
                                .andExpect(jsonPath("$.totalPages", is(3)));
        }

        @Test
        void testSearchRevokedApplications_WithTrimmedParams() throws Exception {
                List<RevokeApplicationDTO> content = new ArrayList<>();
                when(revokeService.searchRevokedApplicationsPaged(eq("12345"), eq("A123456789"), anyInt(), anyInt(), eq("inst-123")))
                                .thenReturn(content);
                when(revokeService.countSearchRevokedApplications(eq("12345"), eq("A123456789"), eq("inst-123")))
                                .thenReturn(10L);

                mockMvc.perform(get("/revoke/search")
                                .param("caseNumber", "  12345  ")
                                .param("nationalID", "  A123456789  ")
                                .param("institutionID", "  inst-123  ")
                                .param("offset", "0")
                                .param("size", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());

                verify(revokeService).searchRevokedApplicationsPaged(eq("12345"), eq("A123456789"), anyInt(), anyInt(), eq("inst-123"));
        }

        @Test
        void testSearchRevokedApplications_ExceedsMaxSize() throws Exception {
                List<RevokeApplicationDTO> content = new ArrayList<>();
                when(revokeService.searchRevokedApplicationsPaged(any(), any(), anyInt(), eq(100), any()))
                                .thenReturn(content);
                when(revokeService.countSearchRevokedApplications(any(), any(), any()))
                                .thenReturn(0L);

                mockMvc.perform(get("/revoke/search")
                                .param("offset", "0")
                                .param("size", "200")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.size", is(100)));
        }

        @Test
        void testGetRevokeDetails_NullRequest() throws Exception {
                mockMvc.perform(post("/revoke/details")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error", containsString("cancellationID")));
        }

        @Test
        void testGetRevokeDetails_EmptyCancellationID() throws Exception {
                RevokeSearchRequest request = new RevokeSearchRequest();
                request.setCancellationID("");
                request.setNationalID("A123456789");

                mockMvc.perform(post("/revoke/details")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testGetRevokeDetails_EmptyNationalID() throws Exception {
                RevokeSearchRequest request = new RevokeSearchRequest();
                request.setCancellationID("test-cancel-id");
                request.setNationalID("");

                mockMvc.perform(post("/revoke/details")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testGetRevokeDetails_GeneralException() throws Exception {
                RevokeSearchRequest request = new RevokeSearchRequest();
                request.setCancellationID("test-cancel-id");
                request.setNationalID("A123456789");

                when(revokeService.getParentsByCancellation("test-cancel-id"))
                                .thenThrow(new RuntimeException("Database connection error"));

                mockMvc.perform(post("/revoke/details")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.error", is("Database connection error")));
        }

        @Test
        void testUpdateConfirmDate_NullCancellationID() throws Exception {
                UpdateConfirmDateRequest request = new UpdateConfirmDateRequest();
                request.setConfirmDate(LocalDate.now());

                mockMvc.perform(put("/revoke/confirm-date")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error", containsString("cancellationID")));
        }

        @Test
        void testUpdateConfirmDate_EmptyCancellationID() throws Exception {
                UpdateConfirmDateRequest request = new UpdateConfirmDateRequest();
                request.setCancellationID("");
                request.setConfirmDate(LocalDate.now());

                mockMvc.perform(put("/revoke/confirm-date")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateConfirmDate_NullConfirmDate() throws Exception {
                UpdateConfirmDateRequest request = new UpdateConfirmDateRequest();
                request.setCancellationID("test-cancel-id");

                mockMvc.perform(put("/revoke/confirm-date")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateConfirmDate_Exception() throws Exception {
                UpdateConfirmDateRequest request = new UpdateConfirmDateRequest();
                request.setCancellationID("test-cancel-id");
                request.setConfirmDate(LocalDate.now());

                when(revokeService.updateConfirmDate(anyString(), any(LocalDate.class)))
                                .thenThrow(new RuntimeException("Update failed"));

                mockMvc.perform(put("/revoke/confirm-date")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.error", is("Update failed")));
        }

        @Test
        void testUpdateParticipantStatus_NullRequest() throws Exception {
                mockMvc.perform(put("/revoke/update-participant-status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateParticipantStatus_EmptyApplicationID() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("ApplicationID", "");
                request.put("NationalID", "A123456789");
                request.put("Status", "已撤銷");

                mockMvc.perform(put("/revoke/update-participant-status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateParticipantStatus_EmptyNationalID() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("ApplicationID", "test-app-id");
                request.put("NationalID", "");
                request.put("Status", "已撤銷");

                mockMvc.perform(put("/revoke/update-participant-status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateParticipantStatus_EmptyStatus() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("ApplicationID", "test-app-id");
                request.put("NationalID", "A123456789");
                request.put("Status", "");

                mockMvc.perform(put("/revoke/update-participant-status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateParticipantStatus_Exception() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("ApplicationID", "test-app-id");
                request.put("NationalID", "A123456789");
                request.put("Status", "已撤銷");

                when(revokeService.updateApplicationParticipantStatus(anyString(), anyString(), anyString()))
                                .thenThrow(new RuntimeException("Update error"));

                mockMvc.perform(put("/revoke/update-participant-status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.error", is("Update error")));
        }

        @Test
        void testCreateCancellation_NullRequest() throws Exception {
                mockMvc.perform(post("/revoke/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("null"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error", is("Request body required")));
        }

        @Test
        void testCreateCancellation_EmptyNationalID() throws Exception {
                CreateRevokeRequest request = new CreateRevokeRequest();
                request.setNationalID("");
                request.setAbandonReason("個人因素");
                request.setApplicationID("test-app-id");
                request.setCaseNumber("12345");

                mockMvc.perform(post("/revoke/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testCreateCancellation_EmptyAbandonReason() throws Exception {
                CreateRevokeRequest request = new CreateRevokeRequest();
                request.setNationalID("A123456789");
                request.setAbandonReason("");
                request.setApplicationID("test-app-id");
                request.setCaseNumber("12345");

                mockMvc.perform(post("/revoke/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testCreateCancellation_EmptyApplicationID() throws Exception {
                CreateRevokeRequest request = new CreateRevokeRequest();
                request.setNationalID("A123456789");
                request.setAbandonReason("個人因素");
                request.setApplicationID("");
                request.setCaseNumber("12345");

                mockMvc.perform(post("/revoke/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testCreateCancellation_EmptyCaseNumber() throws Exception {
                CreateRevokeRequest request = new CreateRevokeRequest();
                request.setNationalID("A123456789");
                request.setAbandonReason("個人因素");
                request.setApplicationID("test-app-id");
                request.setCaseNumber("");

                mockMvc.perform(post("/revoke/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testCreateCancellation_WithWhitespaceFields() throws Exception {
                CreateRevokeRequest request = new CreateRevokeRequest();
                request.setNationalID("  A123456789  ");
                request.setAbandonReason("  個人因素  ");
                request.setApplicationID("  test-app-id  ");
                request.setCaseNumber("  12345  ");

                doNothing().when(revokeService).createCancellation(eq("test-app-id"), eq("個人因素"), eq("A123456789"), eq("12345"));

                mockMvc.perform(post("/revoke/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)));

                verify(revokeService).createCancellation(eq("test-app-id"), eq("個人因素"), eq("A123456789"), eq("12345"));
        }

        @Test
        void testGetRevokedApplications_NoHasNext() throws Exception {
                List<RevokeApplicationDTO> content = new ArrayList<>();
                when(revokeService.getRevokedApplications(anyInt(), anyInt(), any(), any(), any()))
                                .thenReturn(content);
                when(revokeService.getTotalRevokedApplications(any(), any(), any()))
                                .thenReturn(5L);

                mockMvc.perform(get("/revoke/applications")
                                .param("offset", "0")
                                .param("size", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.hasNext", is(false)));
        }

        @Test
        void testSearchRevokedApplications_NoHasNext() throws Exception {
                List<RevokeApplicationDTO> content = new ArrayList<>();
                when(revokeService.searchRevokedApplicationsPaged(any(), any(), anyInt(), anyInt(), any()))
                                .thenReturn(content);
                when(revokeService.countSearchRevokedApplications(any(), any(), any()))
                                .thenReturn(8L);

                mockMvc.perform(get("/revoke/search")
                                .param("offset", "0")
                                .param("size", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.hasNext", is(false)));
        }
}

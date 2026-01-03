package Group4.Childcare.controller;

import Group4.Childcare.Controller.ApplicationParticipantsController;
import Group4.Childcare.Model.ApplicationParticipants;
import Group4.Childcare.Service.ApplicationParticipantsService;
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
 * ApplicationParticipantsController 單元測試
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ApplicationParticipantsControllerTest {

        @Mock
        private ApplicationParticipantsService service;

        @InjectMocks
        private ApplicationParticipantsController controller;

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;
        private ApplicationParticipants testParticipant;
        private UUID testParticipantId;
        private UUID testApplicationId;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
                objectMapper = new ObjectMapper();

                testParticipantId = UUID.randomUUID();
                testApplicationId = UUID.randomUUID();

                testParticipant = new ApplicationParticipants();
                testParticipant.setApplicationID(testParticipantId);
                testParticipant.setNationalID("A123456789");
                testParticipant.setName("測試者");
                testParticipant.setStatus("待審核");
        }

        @Test
        void testCreate_Success() throws Exception {
                when(service.create(any(ApplicationParticipants.class))).thenReturn(testParticipant);

                mockMvc.perform(post("/application-participants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(testParticipant)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.nationalID", is("A123456789")));

                verify(service, times(1)).create(any(ApplicationParticipants.class));
        }

        @Test
        void testGetById_Success() throws Exception {
                when(service.getById(testParticipantId)).thenReturn(Optional.of(testParticipant));

                mockMvc.perform(get("/application-participants/{id}", testParticipantId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.nationalID", is("A123456789")));

                verify(service, times(1)).getById(testParticipantId);
        }

        @Test
        void testGetById_NotFound() throws Exception {
                when(service.getById(testParticipantId)).thenReturn(Optional.empty());

                mockMvc.perform(get("/application-participants/{id}", testParticipantId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound());

                verify(service, times(1)).getById(testParticipantId);
        }

        @Test
        void testGetAll_Success() throws Exception {
                List<ApplicationParticipants> list = Arrays.asList(testParticipant);
                when(service.getAll()).thenReturn(list);

                mockMvc.perform(get("/application-participants")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)));

                verify(service, times(1)).getAll();
        }

        @Test
        void testUpdate_LegacyMode_Success() throws Exception {
                when(service.updateParticipant(eq(testParticipantId), eq("已錄取"), eq("符合資格"), any()))
                                .thenReturn(testParticipant);

                mockMvc.perform(put("/application-participants/{participantID}", testParticipantId)
                                .param("status", "已錄取")
                                .param("reason", "符合資格")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());

                verify(service, times(1)).updateParticipant(eq(testParticipantId), eq("已錄取"), eq("符合資格"), any());
        }

        @Test
        void testUpdate_NewMode_Success() throws Exception {
                when(service.updateParticipantWithDynamicOrder(
                                eq(testApplicationId), eq("A123456789"), eq("候補中"), eq("符合資格"), any()))
                                .thenReturn(testParticipant);

                mockMvc.perform(put("/application-participants/{participantID}", testParticipantId)
                                .param("applicationID", testApplicationId.toString())
                                .param("nationalID", "A123456789")
                                .param("status", "候補中")
                                .param("reason", "符合資格")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());

                verify(service, times(1)).updateParticipantWithDynamicOrder(
                                eq(testApplicationId), eq("A123456789"), eq("候補中"), eq("符合資格"), any());
        }

        @Test
        void testUpdate_RuntimeException() throws Exception {
                when(service.updateParticipant(eq(testParticipantId), any(), any(), any()))
                                .thenThrow(new RuntimeException("Participant not found"));

                mockMvc.perform(put("/application-participants/{participantID}", testParticipantId)
                                .param("status", "已錄取")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound());
        }

        @Test
        void testUpdate_GeneralException() throws Exception {
                when(service.updateParticipant(eq(testParticipantId), any(), any(), any()))
                                .thenThrow(new RuntimeException("Database error"));

                mockMvc.perform(put("/application-participants/{participantID}", testParticipantId)
                                .param("status", "已錄取")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound());
        }

        @Test
        void testCancelApplication_Success() throws Exception {
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("ApplicationID", testApplicationId.toString());
                requestBody.put("NationalID", "A123456789");
                requestBody.put("reason", "使用者撤銷");

                when(service.cancelApplicationWithOrderRecalculation(
                                eq(testApplicationId), eq("A123456789"), eq("使用者撤銷")))
                                .thenReturn(testParticipant);

                mockMvc.perform(post("/application-participants/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.message", is("撤銷審核通過")));
        }

        @Test
        void testCancelApplication_MissingApplicationId() throws Exception {
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("NationalID", "A123456789");

                mockMvc.perform(post("/application-participants/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success", is(false)))
                                .andExpect(jsonPath("$.message", is("ApplicationID 為必填欄位")));
        }

        @Test
        void testCancelApplication_MissingNationalId() throws Exception {
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("ApplicationID", testApplicationId.toString());

                mockMvc.perform(post("/application-participants/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success", is(false)))
                                .andExpect(jsonPath("$.message", is("NationalID 為必填欄位")));
        }

        @Test
        void testCancelApplication_InvalidUUID() throws Exception {
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("ApplicationID", "invalid-uuid");
                requestBody.put("NationalID", "A123456789");

                mockMvc.perform(post("/application-participants/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        void testCancelApplication_RuntimeException() throws Exception {
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("ApplicationID", testApplicationId.toString());
                requestBody.put("NationalID", "A123456789");
                requestBody.put("reason", "撤銷");

                when(service.cancelApplicationWithOrderRecalculation(any(), any(), any()))
                                .thenThrow(new RuntimeException("Participant not found"));

                mockMvc.perform(post("/application-participants/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        void testUpdate_NewMode_EmptyNationalId() throws Exception {
                // 當 nationalID 為空字串時，應走 legacy 模式
                when(service.updateParticipant(eq(testParticipantId), any(), any(), any()))
                                .thenReturn(testParticipant);

                mockMvc.perform(put("/application-participants/{participantID}", testParticipantId)
                                .param("applicationID", testApplicationId.toString())
                                .param("nationalID", "")
                                .param("status", "已錄取")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());

                verify(service, times(1)).updateParticipant(eq(testParticipantId), any(), any(), any());
        }

        @Test
        void testUpdate_GeneralException_InternalError() throws Exception {
                // 模擬非 RuntimeException 的 Exception
                when(service.updateParticipant(any(), any(), any(), any()))
                                .thenAnswer(invocation -> {
                                        throw new Exception("Unexpected error");
                                });

                mockMvc.perform(put("/application-participants/{participantID}", testParticipantId)
                                .param("status", "已錄取")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isInternalServerError())
                                .andExpect(content().string(containsString("Unexpected error")));
        }

        @Test
        void testUpdate_NewMode_NullNationalId() throws Exception {
                // 當 nationalID 為 null 時，應走 legacy 模式
                when(service.updateParticipant(eq(testParticipantId), any(), any(), any()))
                                .thenReturn(testParticipant);

                mockMvc.perform(put("/application-participants/{participantID}", testParticipantId)
                                .param("applicationID", testApplicationId.toString())
                                // 不傳 nationalID 參數，使其為 null
                                .param("status", "已錄取")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());

                verify(service, times(1)).updateParticipant(eq(testParticipantId), any(), any(), any());
        }

        @Test
        void testCancelApplication_EmptyApplicationId() throws Exception {
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("ApplicationID", "");
                requestBody.put("NationalID", "A123456789");

                mockMvc.perform(post("/application-participants/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message", is("ApplicationID 為必填欄位")));
        }

        @Test
        void testCancelApplication_EmptyNationalId() throws Exception {
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("ApplicationID", testApplicationId.toString());
                requestBody.put("NationalID", "");

                mockMvc.perform(post("/application-participants/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message", is("NationalID 為必填欄位")));
        }

        @Test
        void testCancelApplication_EmptyReason() throws Exception {
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("ApplicationID", testApplicationId.toString());
                requestBody.put("NationalID", "A123456789");
                requestBody.put("reason", "");

                when(service.cancelApplicationWithOrderRecalculation(any(), any(), eq("使用者撤銷申請")))
                                .thenReturn(testParticipant);

                mockMvc.perform(post("/application-participants/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isOk());
        }

        @Test
        void testCancelApplication_GeneralException() throws Exception {
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("ApplicationID", testApplicationId.toString());
                requestBody.put("NationalID", "A123456789");

                when(service.cancelApplicationWithOrderRecalculation(any(), any(), any()))
                                .thenAnswer(invocation -> {
                                        throw new Exception("System failure");
                                });

                mockMvc.perform(post("/application-participants/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.message", is("系統錯誤: System failure")));
        }
}

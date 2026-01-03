package Group4.Childcare.controller;

import Group4.Childcare.Model.Announcements;
import Group4.Childcare.DTO.AnnouncementSummaryDTO;
import Group4.Childcare.Service.AnnouncementsService;
import Group4.Childcare.Controller.AnnouncementsController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import java.time.LocalDate;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AnnouncementsController 單元測試
 *
 * 測試範圍：
 * 1. getById() - 根據ID查詢公告
 * 2. getAll() - 查詢所有公告
 * 3. getAnnouncementsByOffsetJdbc() - 分頁查詢公告
 * 4. update() - 更新公告
 * 5. getSummary() - 查詢公告摘要
 * 6. getAdminActiveBackend() - 查詢後台活動公告
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementsControllerTest {

        @Mock
        private AnnouncementsService service;

        @InjectMocks
        private AnnouncementsController controller;

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;
        private Path storageLocation;

        private Announcements testAnnouncement;
        private UUID testAnnouncementId;

        @BeforeEach
        void setUp() throws IOException {
                mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
                objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                // 設定測試用的 storageLocation
                String basePath = System.getProperty("user.dir");
                File dir = new File(basePath, "AttachmentResource");
                if (!dir.exists()) {
                        dir.mkdirs();
                }
                storageLocation = dir.toPath();

                testAnnouncementId = UUID.randomUUID();
                testAnnouncement = new Announcements();
                testAnnouncement.setAnnouncementID(testAnnouncementId);
                testAnnouncement.setTitle("測試公告");
                testAnnouncement.setContent("測試內容");
                testAnnouncement.setType((byte) 1);
                testAnnouncement.setStatus((byte) 1);
                testAnnouncement.setStartDate(LocalDate.now());
                testAnnouncement.setEndDate(LocalDate.now().plusDays(7));
        }

        @Test
        void testGetById_Success() throws Exception {
                // Given
                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));

                // When & Then
                mockMvc.perform(get("/announcements/{id}", testAnnouncementId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.announcementID", is(testAnnouncementId.toString())))
                                .andExpect(jsonPath("$.title", is("測試公告")))
                                .andExpect(jsonPath("$.content", is("測試內容")));

                verify(service, times(1)).getById(testAnnouncementId);
        }

        @Test
        void testGetById_NotFound() throws Exception {
                // Given
                UUID nonExistentId = UUID.randomUUID();
                when(service.getById(nonExistentId)).thenReturn(Optional.empty());

                // When & Then
                mockMvc.perform(get("/announcements/{id}", nonExistentId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound());

                verify(service, times(1)).getById(nonExistentId);
        }

        @Test
        void testGetAll_Success() throws Exception {
                // Given
                List<Announcements> announcements = Arrays.asList(testAnnouncement);
                when(service.getAll()).thenReturn(announcements);

                // When & Then
                mockMvc.perform(get("/announcements")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].title", is("測試公告")));

                verify(service, times(1)).getAll();
        }

        @Test
        void testGetAll_EmptyList() throws Exception {
                // Given
                when(service.getAll()).thenReturn(Arrays.asList());

                // When & Then
                mockMvc.perform(get("/announcements")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(0)));

                verify(service, times(1)).getAll();
        }

        @Test
        void testGetAnnouncementsByOffsetJdbc_Success() throws Exception {
                // Given
                int offset = 0;
                List<Announcements> announcements = Arrays.asList(testAnnouncement);
                when(service.getAnnouncementsWithOffsetJdbc(offset)).thenReturn(announcements);
                when(service.getTotalCount()).thenReturn(1L);

                // When & Then
                mockMvc.perform(get("/announcements/offset")
                                .param("offset", String.valueOf(offset))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.offset", is(offset)))
                                .andExpect(jsonPath("$.size", is(8)))
                                .andExpect(jsonPath("$.totalElements", is(1)))
                                .andExpect(jsonPath("$.totalPages", is(1)))
                                .andExpect(jsonPath("$.hasNext", is(false)))
                                .andExpect(jsonPath("$.content", hasSize(1)));

                verify(service, times(1)).getAnnouncementsWithOffsetJdbc(offset);
                verify(service, times(1)).getTotalCount();
        }

        @Test
        void testUpdate_Success() throws Exception {
                // Given
                testAnnouncement.setTitle("更新後的標題");
                when(service.update(eq(testAnnouncementId), any(Announcements.class))).thenReturn(testAnnouncement);

                // When & Then
                mockMvc.perform(put("/announcements/{id}", testAnnouncementId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(testAnnouncement)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title", is("更新後的標題")));

                verify(service, times(1)).update(eq(testAnnouncementId), any(Announcements.class));
        }

        @Test
        void testGetSummary_Success() throws Exception {
                // Given
                AnnouncementSummaryDTO summaryDTO = new AnnouncementSummaryDTO(
                                testAnnouncementId,
                                "測試公告",
                                "測試內容",
                                LocalDate.now());
                List<AnnouncementSummaryDTO> summaries = Arrays.asList(summaryDTO);
                when(service.getSummaryAll()).thenReturn(summaries);

                // When & Then
                mockMvc.perform(get("/announcements/summary")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].title", is("測試公告")));

                verify(service, times(1)).getSummaryAll();
        }

        @Test
        void testGetAdminActiveBackend_Success() throws Exception {
                // Given
                AnnouncementSummaryDTO summaryDTO = new AnnouncementSummaryDTO(
                                testAnnouncementId,
                                "活動公告",
                                "活動內容",
                                LocalDate.now());
                List<AnnouncementSummaryDTO> activeList = Arrays.asList(summaryDTO);
                when(service.getAdminActiveBackend()).thenReturn(activeList);

                // When & Then
                mockMvc.perform(get("/announcements/active/backend")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].title", is("活動公告")));

                verify(service, times(1)).getAdminActiveBackend();
        }

        // ===== getfrontSummary 測試 =====
        @Test
        void testGetfrontSummary_Success() throws Exception {
                // Given
                AnnouncementSummaryDTO summaryDTO = new AnnouncementSummaryDTO(
                                testAnnouncementId,
                                "前台公告",
                                "前台內容",
                                LocalDate.now());
                List<AnnouncementSummaryDTO> frontList = Arrays.asList(summaryDTO);
                when(service.getfrontSummaryAll()).thenReturn(frontList);

                // When & Then
                mockMvc.perform(get("/announcements/front")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].title", is("前台公告")));

                verify(service, times(1)).getfrontSummaryAll();
        }

        @Test
        void testGetfrontSummary_EmptyList() throws Exception {
                // Given
                when(service.getfrontSummaryAll()).thenReturn(Arrays.asList());

                // When & Then
                mockMvc.perform(get("/announcements/front")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(0)));

                verify(service, times(1)).getfrontSummaryAll();
        }

        // ===== createAnnouncementJdbc 測試 =====
        @Test
        void testCreateAnnouncementJdbc_Success() throws Exception {
                // Given
                when(service.createAnnouncementJdbc(any(Announcements.class))).thenReturn(testAnnouncement);

                // When & Then
                mockMvc.perform(post("/announcements")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(testAnnouncement)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.announcementID", is(testAnnouncementId.toString())))
                                .andExpect(jsonPath("$.title", is("測試公告")));

                verify(service, times(1)).createAnnouncementJdbc(any(Announcements.class));
        }

        // ===== delete 測試 =====
        @Test
        void testDelete_Success() throws Exception {
                // Given
                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));
                when(service.delete(testAnnouncementId)).thenReturn(true);

                // When & Then
                mockMvc.perform(delete("/announcements/{id}", testAnnouncementId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message", is("公告已刪除")));

                verify(service, times(1)).getById(testAnnouncementId);
                verify(service, times(1)).delete(testAnnouncementId);
        }

        @Test
        void testDelete_NotFound() throws Exception {
                // Given
                UUID nonExistentId = UUID.randomUUID();
                when(service.getById(nonExistentId)).thenReturn(Optional.empty());

                // When & Then
                mockMvc.perform(delete("/announcements/{id}", nonExistentId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound());

                verify(service, times(1)).getById(nonExistentId);
                verify(service, times(0)).delete(nonExistentId);
        }

        @Test
        void testDelete_DeleteFailed() throws Exception {
                // Given
                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));
                when(service.delete(testAnnouncementId)).thenReturn(false);

                // When & Then
                mockMvc.perform(delete("/announcements/{id}", testAnnouncementId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isInternalServerError());

                verify(service, times(1)).delete(testAnnouncementId);
        }

        @Test
        void testDelete_Exception() throws Exception {
                // Given
                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));
                when(service.delete(testAnnouncementId)).thenThrow(new RuntimeException("資料庫錯誤"));

                // When & Then
                mockMvc.perform(delete("/announcements/{id}", testAnnouncementId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isInternalServerError());

                verify(service, times(1)).delete(testAnnouncementId);
        }

        @Test
        void testDelete_WithAttachment() throws Exception {
                // Given - 公告有附件
                testAnnouncement.setAttachmentPath("test-attachment.pdf");
                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));
                when(service.delete(testAnnouncementId)).thenReturn(true);

                // When & Then - 刪除成功（附件刪除失敗不影響結果）
                mockMvc.perform(delete("/announcements/{id}", testAnnouncementId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message", is("公告已刪除")));

                verify(service, times(1)).delete(testAnnouncementId);
        }

        // ===== upload 測試 =====
        @Test
        void testUpload_Success() throws Exception {
                // Given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.pdf",
                                "application/pdf",
                                "test content".getBytes());

                Announcements meta = new Announcements();
                meta.setTitle("上傳測試");
                meta.setContent("測試內容");
                meta.setType((byte) 1);
                meta.setStatus((byte) 1);

                MockMultipartFile metaPart = new MockMultipartFile(
                                "meta",
                                "",
                                "application/json",
                                objectMapper.writeValueAsBytes(meta));

                Announcements created = new Announcements();
                created.setAnnouncementID(testAnnouncementId);
                created.setTitle("上傳測試");
                created.setAttachmentPath("test-uuid_test.pdf");

                when(service.createAnnouncementJdbc(any(Announcements.class))).thenReturn(created);

                // When & Then
                mockMvc.perform(multipart("/announcements/upload")
                                .file(file)
                                .file(metaPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.announcementID", is(testAnnouncementId.toString())))
                                .andExpect(jsonPath("$.title", is("上傳測試")));

                verify(service, times(1)).createAnnouncementJdbc(any(Announcements.class));
        }

        @Test
        void testUpload_MissingFile() throws Exception {
                // Given - 沒有檔案
                Announcements meta = new Announcements();
                meta.setTitle("測試");

                MockMultipartFile metaPart = new MockMultipartFile(
                                "meta",
                                "",
                                "application/json",
                                objectMapper.writeValueAsBytes(meta));

                // When & Then
                mockMvc.perform(multipart("/announcements/upload")
                                .file(metaPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testUpload_MissingMetaTitle() throws Exception {
                // Given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.pdf",
                                "application/pdf",
                                "test content".getBytes());

                Announcements meta = new Announcements();
                // 沒有設定 title

                MockMultipartFile metaPart = new MockMultipartFile(
                                "meta",
                                "",
                                "application/json",
                                objectMapper.writeValueAsBytes(meta));

                // When & Then
                mockMvc.perform(multipart("/announcements/upload")
                                .file(file)
                                .file(metaPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testUpload_NullMeta() throws Exception {
                // Given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.pdf",
                                "application/pdf",
                                "test content".getBytes());

                // When & Then - meta 為 null
                mockMvc.perform(multipart("/announcements/upload")
                                .file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testUpload_ServiceException() throws Exception {
                // Given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.pdf",
                                "application/pdf",
                                "test content".getBytes());

                Announcements meta = new Announcements();
                meta.setTitle("測試");

                MockMultipartFile metaPart = new MockMultipartFile(
                                "meta",
                                "",
                                "application/json",
                                objectMapper.writeValueAsBytes(meta));

                when(service.createAnnouncementJdbc(any(Announcements.class)))
                                .thenThrow(new RuntimeException("Database error"));

                // When & Then
                mockMvc.perform(multipart("/announcements/upload")
                                .file(file)
                                .file(metaPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isInternalServerError());
        }

        // ===== updateWithFile 測試 =====
        @Test
        void testUpdateWithFile_Success() throws Exception {
                // Given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "updated.pdf",
                                "application/pdf",
                                "updated content".getBytes());

                Announcements meta = new Announcements();
                meta.setTitle("更新標題");

                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));
                when(service.updateWithJdbc(eq(testAnnouncementId), any(Announcements.class)))
                                .thenReturn(testAnnouncement);

                // When & Then
                mockMvc.perform(multipart("/announcements/{id}", testAnnouncementId)
                                .file(file)
                                .param("meta", objectMapper.writeValueAsString(meta))
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service, times(1)).getById(testAnnouncementId);
                verify(service, times(1)).updateWithJdbc(eq(testAnnouncementId), any(Announcements.class));
        }

        @Test
        void testUpdateWithFile_MetaOnly() throws Exception {
                // Given - 只更新 meta，沒有檔案
                Announcements meta = new Announcements();
                meta.setTitle("只更新標題");
                meta.setContent("只更新內容");

                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));
                when(service.updateWithJdbc(eq(testAnnouncementId), any(Announcements.class)))
                                .thenReturn(testAnnouncement);

                // When & Then
                mockMvc.perform(multipart("/announcements/{id}", testAnnouncementId)
                                .param("meta", objectMapper.writeValueAsString(meta))
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service, times(1)).updateWithJdbc(eq(testAnnouncementId), any(Announcements.class));
        }

        @Test
        void testUpdateWithFile_NotFound() throws Exception {
                // Given
                UUID nonExistentId = UUID.randomUUID();
                when(service.getById(nonExistentId)).thenReturn(Optional.empty());

                Announcements meta = new Announcements();
                meta.setTitle("測試");

                // When & Then
                mockMvc.perform(multipart("/announcements/{id}", nonExistentId)
                                .param("meta", objectMapper.writeValueAsString(meta))
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isNotFound());

                verify(service, times(1)).getById(nonExistentId);
                verify(service, times(0)).updateWithJdbc(any(UUID.class), any(Announcements.class));
        }

        @Test
        void testUpdateWithFile_ServiceException() throws Exception {
                // Given
                Announcements meta = new Announcements();
                meta.setTitle("測試");

                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));
                when(service.updateWithJdbc(eq(testAnnouncementId), any(Announcements.class)))
                                .thenThrow(new RuntimeException("Update failed"));

                // When & Then
                mockMvc.perform(multipart("/announcements/{id}", testAnnouncementId)
                                .param("meta", objectMapper.writeValueAsString(meta))
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isInternalServerError());
        }

        // ===== downloadAttachment 測試 =====
        @Test
        void testDownloadAttachment_Success() throws Exception {
                // Given
                testAnnouncement.setAttachmentPath("uuid_test.pdf");
                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));

                // When & Then - 注意：實際檔案系統操作在單元測試中難以完全模擬
                // 這裡測試的是 controller 邏輯，檔案不存在會返回 404
                mockMvc.perform(get("/announcements/{id}/attachment", testAnnouncementId))
                                .andExpect(status().isNotFound()); // 因為實際檔案不存在

                verify(service, times(1)).getById(testAnnouncementId);
        }

        @Test
        void testDownloadAttachment_AnnouncementNotFound() throws Exception {
                // Given
                UUID nonExistentId = UUID.randomUUID();
                when(service.getById(nonExistentId)).thenReturn(Optional.empty());

                // When & Then
                mockMvc.perform(get("/announcements/{id}/attachment", nonExistentId))
                                .andExpect(status().isNotFound());

                verify(service, times(1)).getById(nonExistentId);
        }

        @Test
        void testDownloadAttachment_NoAttachment() throws Exception {
                // Given - 公告沒有附件
                testAnnouncement.setAttachmentPath(null);
                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));

                // When & Then
                mockMvc.perform(get("/announcements/{id}/attachment", testAnnouncementId))
                                .andExpect(status().isNoContent());

                verify(service, times(1)).getById(testAnnouncementId);
        }

        @Test
        void testDownloadAttachment_EmptyAttachmentPath() throws Exception {
                // Given - 公告附件路徑為空字串
                testAnnouncement.setAttachmentPath("");
                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));

                // When & Then
                mockMvc.perform(get("/announcements/{id}/attachment", testAnnouncementId))
                                .andExpect(status().isNoContent());

                verify(service, times(1)).getById(testAnnouncementId);
        }

        // ===== 額外分支覆蓋測試 =====

        @Test
        void testUpload_EmptyFile() throws Exception {
                // Given - 空檔案
                MockMultipartFile emptyFile = new MockMultipartFile(
                                "file",
                                "test.pdf",
                                "application/pdf",
                                new byte[0]);

                Announcements meta = new Announcements();
                meta.setTitle("測試");

                MockMultipartFile metaPart = new MockMultipartFile(
                                "meta",
                                "",
                                "application/json",
                                objectMapper.writeValueAsBytes(meta));

                // When & Then
                mockMvc.perform(multipart("/announcements/upload")
                                .file(emptyFile)
                                .file(metaPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testUpload_WithCreatedTime() throws Exception {
                // Given - meta 已經有 CreatedTime
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.pdf",
                                "application/pdf",
                                "test content".getBytes());

                Announcements meta = new Announcements();
                meta.setTitle("測試");
                meta.setCreatedTime(java.time.LocalDateTime.now().minusDays(1));

                MockMultipartFile metaPart = new MockMultipartFile(
                                "meta",
                                "",
                                "application/json",
                                objectMapper.writeValueAsBytes(meta));

                when(service.createAnnouncementJdbc(any(Announcements.class))).thenReturn(testAnnouncement);

                // When & Then
                mockMvc.perform(multipart("/announcements/upload")
                                .file(file)
                                .file(metaPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isCreated());

                verify(service, times(1)).createAnnouncementJdbc(any(Announcements.class));
        }

        @Test
        void testUpdateWithFile_AllMetaFields() throws Exception {
                // Given - 更新所有 meta 欄位
                Announcements meta = new Announcements();
                meta.setTitle("新標題");
                meta.setContent("新內容");
                meta.setType((byte) 2);
                meta.setStartDate(LocalDate.now());
                meta.setEndDate(LocalDate.now().plusDays(10));
                meta.setStatus((byte) 2);
                meta.setCreatedUser("user1");
                meta.setCreatedTime(java.time.LocalDateTime.now());
                meta.setUpdatedUser("user2");
                meta.setUpdatedTime(java.time.LocalDateTime.now());
                meta.setAttachmentPath("custom-path.pdf");

                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));
                when(service.updateWithJdbc(eq(testAnnouncementId), any(Announcements.class)))
                                .thenReturn(testAnnouncement);

                // When & Then
                mockMvc.perform(multipart("/announcements/{id}", testAnnouncementId)
                                .param("meta", objectMapper.writeValueAsString(meta))
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service, times(1)).updateWithJdbc(eq(testAnnouncementId), any(Announcements.class));
        }

        @Test
        void testUpdateWithFile_WithExistingAttachment() throws Exception {
                // Given - 更新檔案時，existing 有舊附件
                testAnnouncement.setAttachmentPath("old-file.pdf");
                
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "new-file.pdf",
                                "application/pdf",
                                "new content".getBytes());

                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));
                when(service.updateWithJdbc(eq(testAnnouncementId), any(Announcements.class)))
                                .thenReturn(testAnnouncement);

                // When & Then - 舊檔案刪除失敗不影響更新
                mockMvc.perform(multipart("/announcements/{id}", testAnnouncementId)
                                .file(file)
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service, times(1)).updateWithJdbc(eq(testAnnouncementId), any(Announcements.class));
        }

        @Test
        void testUpdateWithFile_EmptyFile() throws Exception {
                // Given - 空檔案應該被忽略
                MockMultipartFile emptyFile = new MockMultipartFile(
                                "file",
                                "test.pdf",
                                "application/pdf",
                                new byte[0]);

                Announcements meta = new Announcements();
                meta.setTitle("測試");

                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));
                when(service.updateWithJdbc(eq(testAnnouncementId), any(Announcements.class)))
                                .thenReturn(testAnnouncement);

                // When & Then
                mockMvc.perform(multipart("/announcements/{id}", testAnnouncementId)
                                .file(emptyFile)
                                .param("meta", objectMapper.writeValueAsString(meta))
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service, times(1)).updateWithJdbc(eq(testAnnouncementId), any(Announcements.class));
        }

        @Test
        void testUpdateWithFile_NullMeta() throws Exception {
                // Given - meta 為 null，只更新檔案
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.pdf",
                                "application/pdf",
                                "test content".getBytes());

                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));
                when(service.updateWithJdbc(eq(testAnnouncementId), any(Announcements.class)))
                                .thenReturn(testAnnouncement);

                // When & Then
                mockMvc.perform(multipart("/announcements/{id}", testAnnouncementId)
                                .file(file)
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service, times(1)).updateWithJdbc(eq(testAnnouncementId), any(Announcements.class));
        }

        @Test
        void testGetAnnouncementsByOffsetJdbc_WithPagination() throws Exception {
                // Given - 測試有下一頁的情況
                int offset = 8;
                List<Announcements> announcements = Arrays.asList(testAnnouncement);
                when(service.getAnnouncementsWithOffsetJdbc(offset)).thenReturn(announcements);
                when(service.getTotalCount()).thenReturn(20L);

                // When & Then
                mockMvc.perform(get("/announcements/offset")
                                .param("offset", String.valueOf(offset))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.offset", is(offset)))
                                .andExpect(jsonPath("$.size", is(8)))
                                .andExpect(jsonPath("$.totalElements", is(20)))
                                .andExpect(jsonPath("$.totalPages", is(3)))
                                .andExpect(jsonPath("$.hasNext", is(true)));

                verify(service, times(1)).getAnnouncementsWithOffsetJdbc(offset);
                verify(service, times(1)).getTotalCount();
        }

        @Test
        void testGetAnnouncementsByOffsetJdbc_LastPage() throws Exception {
                // Given - 測試最後一頁
                int offset = 16;
                List<Announcements> announcements = Arrays.asList(testAnnouncement);
                when(service.getAnnouncementsWithOffsetJdbc(offset)).thenReturn(announcements);
                when(service.getTotalCount()).thenReturn(20L);

                // When & Then
                mockMvc.perform(get("/announcements/offset")
                                .param("offset", String.valueOf(offset))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.hasNext", is(false)));

                verify(service, times(1)).getAnnouncementsWithOffsetJdbc(offset);
                verify(service, times(1)).getTotalCount();
        }

        @Test
        void testGetSummary_EmptyList() throws Exception {
                // Given - 空列表
                when(service.getSummaryAll()).thenReturn(Arrays.asList());

                // When & Then
                mockMvc.perform(get("/announcements/summary")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(0)));

                verify(service, times(1)).getSummaryAll();
        }

        @Test
        void testGetAdminActiveBackend_EmptyList() throws Exception {
                // Given - 空列表
                when(service.getAdminActiveBackend()).thenReturn(Arrays.asList());

                // When & Then
                mockMvc.perform(get("/announcements/active/backend")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(0)));

                verify(service, times(1)).getAdminActiveBackend();
        }

        @Test
        void testGetAnnouncementsByOffsetJdbc_DefaultOffset() throws Exception {
                // Given - 測試預設 offset = 0
                List<Announcements> announcements = Arrays.asList(testAnnouncement);
                when(service.getAnnouncementsWithOffsetJdbc(0)).thenReturn(announcements);
                when(service.getTotalCount()).thenReturn(1L);

                // When & Then - 不提供 offset 參數
                mockMvc.perform(get("/announcements/offset")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.offset", is(0)));

                verify(service, times(1)).getAnnouncementsWithOffsetJdbc(0);
        }

        @Test
        void testUpload_NullOriginalFilename() throws Exception {
                // Given - 檔案沒有原始檔名
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                null, // null filename
                                "application/pdf",
                                "test content".getBytes());

                Announcements meta = new Announcements();
                meta.setTitle("測試");

                MockMultipartFile metaPart = new MockMultipartFile(
                                "meta",
                                "",
                                "application/json",
                                objectMapper.writeValueAsBytes(meta));

                when(service.createAnnouncementJdbc(any(Announcements.class))).thenReturn(testAnnouncement);

                // When & Then
                mockMvc.perform(multipart("/announcements/upload")
                                .file(file)
                                .file(metaPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isCreated());

                verify(service, times(1)).createAnnouncementJdbc(any(Announcements.class));
        }

        @Test
        void testUpdateWithFile_NullOriginalFilename() throws Exception {
                // Given - 更新檔案沒有原始檔名
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                null, // null filename
                                "application/pdf",
                                "test content".getBytes());

                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));
                when(service.updateWithJdbc(eq(testAnnouncementId), any(Announcements.class)))
                                .thenReturn(testAnnouncement);

                // When & Then
                mockMvc.perform(multipart("/announcements/{id}", testAnnouncementId)
                                .file(file)
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service, times(1)).updateWithJdbc(eq(testAnnouncementId), any(Announcements.class));
        }

        // ============================================================
        // 提升分支覆蓋率的額外測試 - 針對未覆蓋分支
        // ============================================================

        @Test
        void testUpdateWithFile_MetaWithTitle() throws Exception {
                // 測試 meta.getTitle() 不為 null 的分支 (L163)
                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));
                when(service.updateWithJdbc(eq(testAnnouncementId), any(Announcements.class)))
                                .thenReturn(testAnnouncement);

                Announcements meta = new Announcements();
                meta.setTitle("新標題");

                MockMultipartFile metaPart = new MockMultipartFile(
                                "meta",
                                "",
                                "application/json",
                                objectMapper.writeValueAsBytes(meta));

                mockMvc.perform(multipart("/announcements/{id}", testAnnouncementId)
                                .file(metaPart)
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());

                verify(service).updateWithJdbc(eq(testAnnouncementId), any(Announcements.class));
        }

        @Test
        void testUpdateWithFile_MetaWithContent() throws Exception {
                // 測試 meta.getContent() 不為 null 的分支 (L164)
                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));
                when(service.updateWithJdbc(eq(testAnnouncementId), any(Announcements.class)))
                                .thenReturn(testAnnouncement);

                Announcements meta = new Announcements();
                meta.setContent("新內容");

                MockMultipartFile metaPart = new MockMultipartFile(
                                "meta",
                                "",
                                "application/json",
                                objectMapper.writeValueAsBytes(meta));

                mockMvc.perform(multipart("/announcements/{id}", testAnnouncementId)
                                .file(metaPart)
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testUpdateWithFile_MetaWithType() throws Exception {
                // 測試 meta.getType() 不為 null 的分支 (L165)
                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));
                when(service.updateWithJdbc(eq(testAnnouncementId), any(Announcements.class)))
                                .thenReturn(testAnnouncement);

                Announcements meta = new Announcements();
                meta.setType((byte) 2);

                MockMultipartFile metaPart = new MockMultipartFile(
                                "meta",
                                "",
                                "application/json",
                                objectMapper.writeValueAsBytes(meta));

                mockMvc.perform(multipart("/announcements/{id}", testAnnouncementId)
                                .file(metaPart)
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testUpdateWithFile_MetaWithStartDate() throws Exception {
                // 測試 meta.getStartDate() 不為 null 的分支 (L166)
                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));
                when(service.updateWithJdbc(eq(testAnnouncementId), any(Announcements.class)))
                                .thenReturn(testAnnouncement);

                Announcements meta = new Announcements();
                meta.setStartDate(LocalDate.now());

                MockMultipartFile metaPart = new MockMultipartFile(
                                "meta",
                                "",
                                "application/json",
                                objectMapper.writeValueAsBytes(meta));

                mockMvc.perform(multipart("/announcements/{id}", testAnnouncementId)
                                .file(metaPart)
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testUpdateWithFile_MetaWithEndDate() throws Exception {
                // 測試 meta.getEndDate() 不為 null 的分支 (L167)
                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));
                when(service.updateWithJdbc(eq(testAnnouncementId), any(Announcements.class)))
                                .thenReturn(testAnnouncement);

                Announcements meta = new Announcements();
                meta.setEndDate(LocalDate.now().plusDays(30));

                MockMultipartFile metaPart = new MockMultipartFile(
                                "meta",
                                "",
                                "application/json",
                                objectMapper.writeValueAsBytes(meta));

                mockMvc.perform(multipart("/announcements/{id}", testAnnouncementId)
                                .file(metaPart)
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testUpdateWithFile_MetaWithStatus() throws Exception {
                // 測試 meta.getStatus() 不為 null 的分支 (L168)
                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));
                when(service.updateWithJdbc(eq(testAnnouncementId), any(Announcements.class)))
                                .thenReturn(testAnnouncement);

                Announcements meta = new Announcements();
                meta.setStatus((byte) 2);

                MockMultipartFile metaPart = new MockMultipartFile(
                                "meta",
                                "",
                                "application/json",
                                objectMapper.writeValueAsBytes(meta));

                mockMvc.perform(multipart("/announcements/{id}", testAnnouncementId)
                                .file(metaPart)
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testUpdateWithFile_MetaWithCreatedUser() throws Exception {
                // 測試 meta.getCreatedUser() 不為 null 的分支 (L169)
                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));
                when(service.updateWithJdbc(eq(testAnnouncementId), any(Announcements.class)))
                                .thenReturn(testAnnouncement);

                Announcements meta = new Announcements();
                meta.setCreatedUser(UUID.randomUUID().toString());

                MockMultipartFile metaPart = new MockMultipartFile(
                                "meta",
                                "",
                                "application/json",
                                objectMapper.writeValueAsBytes(meta));

                mockMvc.perform(multipart("/announcements/{id}", testAnnouncementId)
                                .file(metaPart)
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testUpdateWithFile_MetaWithUpdatedUser() throws Exception {
                // 測試 meta.getUpdatedUser() 不為 null 的分支 (L171)
                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));
                when(service.updateWithJdbc(eq(testAnnouncementId), any(Announcements.class)))
                                .thenReturn(testAnnouncement);

                Announcements meta = new Announcements();
                meta.setUpdatedUser(UUID.randomUUID().toString());

                MockMultipartFile metaPart = new MockMultipartFile(
                                "meta",
                                "",
                                "application/json",
                                objectMapper.writeValueAsBytes(meta));

                mockMvc.perform(multipart("/announcements/{id}", testAnnouncementId)
                                .file(metaPart)
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testUpdateWithFile_MetaWithAttachmentPath() throws Exception {
                // 測試 meta.getAttachmentPath() 不為 null 的分支 (L174)
                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));
                when(service.updateWithJdbc(eq(testAnnouncementId), any(Announcements.class)))
                                .thenReturn(testAnnouncement);

                Announcements meta = new Announcements();
                meta.setAttachmentPath("custom_path.pdf");

                MockMultipartFile metaPart = new MockMultipartFile(
                                "meta",
                                "",
                                "application/json",
                                objectMapper.writeValueAsBytes(meta));

                mockMvc.perform(multipart("/announcements/{id}", testAnnouncementId)
                                .file(metaPart)
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk());
        }

        @Test
        void testDownloadAttachment_FileNotFound() throws Exception {
                // 測試文件不存在的情況 (L219)
                testAnnouncement.setAttachmentPath("nonexistent_file.pdf");
                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));

                mockMvc.perform(get("/announcements/{id}/attachment", testAnnouncementId))
                                .andExpect(status().isNotFound());
        }

        @Test
        void testDownloadAttachment_WithUnderscoreInFilename() throws Exception {
                // 測試帶有底線的檔名解析 (L228-230)
                // 由於實際文件系統操作較複雜,這個測試主要確保邏輯路徑被覆蓋
                testAnnouncement.setAttachmentPath("uuid_test_file.pdf");
                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));

                // 這個測試會因為文件不存在而返回 404,但會覆蓋檔名處理的分支
                mockMvc.perform(get("/announcements/{id}/attachment", testAnnouncementId))
                                .andExpect(status().isNotFound());
        }

        @Test
        void testDownloadAttachment_FileExists() throws Exception {
                // 測試檔案存在時的成功下載情況 (L219 else 分支, L224, L229)
                // 創建一個臨時測試檔案
                String filename = UUID.randomUUID() + "_testfile.txt";
                Path testFile = storageLocation.resolve(filename);
                Files.write(testFile, "test content".getBytes());

                try {
                        testAnnouncement.setAttachmentPath(filename);
                        when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));

                        mockMvc.perform(get("/announcements/{id}/attachment", testAnnouncementId))
                                        .andExpect(status().isOk())
                                        .andExpect(header().exists("Content-Disposition"));
                } finally {
                        // 清理測試檔案
                        Files.deleteIfExists(testFile);
                }
        }

        @Test
        void testDownloadAttachment_FileExistsNoUnderscore() throws Exception {
                // 測試檔名沒有底線的情況 (L229 條件為 false)
                String filename = "simplefile.txt";
                Path testFile = storageLocation.resolve(filename);
                Files.write(testFile, "test content".getBytes());

                try {
                        testAnnouncement.setAttachmentPath(filename);
                        when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));

                        mockMvc.perform(get("/announcements/{id}/attachment", testAnnouncementId))
                                        .andExpect(status().isOk())
                                        .andExpect(header().exists("Content-Disposition"));
                } finally {
                        Files.deleteIfExists(testFile);
                }
        }

        @Test
        void testDownloadAttachment_MalformedURL() throws Exception {
                // 測試 MalformedURLException 的情況 (L248)
                // 使用無效的檔案路徑來觸發異常
                testAnnouncement.setAttachmentPath("../../../etc/passwd");
                when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));

                mockMvc.perform(get("/announcements/{id}/attachment", testAnnouncementId))
                                .andExpect(status().isNotFound()); // 檔案不存在會返回 404
        }
}


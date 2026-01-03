package Group4.Childcare.controller;

import Group4.Childcare.Controller.AttachmentsController;
import Group4.Childcare.Repository.ApplicationsJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AttachmentsController 單元測試
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AttachmentsControllerTest {

        @Mock
        private ApplicationsJdbcRepository applicationsJdbcRepository;

        @InjectMocks
        private AttachmentsController controller;

        private MockMvc mockMvc;
        private UUID testApplicationId;

        @TempDir
        Path tempDir;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
                testApplicationId = UUID.randomUUID();

                // 使用臨時目錄
                ReflectionTestUtils.setField(controller, "ATTACHMENT_DIR", tempDir.toString());
        }

        @Test
        void testUploadAttachments_SingleFile() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.pdf",
                                "application/pdf",
                                "test content".getBytes());

                when(applicationsJdbcRepository.updateAttachmentPaths(eq(testApplicationId), any(), any(), any(),
                                any()))
                                .thenReturn(1);

                mockMvc.perform(multipart("/applications/{id}/attachments", testApplicationId.toString())
                                .file(file))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.attachmentPaths", hasSize(4)));

                verify(applicationsJdbcRepository, times(1))
                                .updateAttachmentPaths(eq(testApplicationId), any(), any(), any(), any());
        }

        @Test
        void testUploadAttachments_MultipleFiles() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                                "file", "test.pdf", "application/pdf", "test".getBytes());
                MockMultipartFile file1 = new MockMultipartFile(
                                "file1", "test1.pdf", "application/pdf", "test1".getBytes());
                MockMultipartFile file2 = new MockMultipartFile(
                                "file2", "test2.pdf", "application/pdf", "test2".getBytes());

                when(applicationsJdbcRepository.updateAttachmentPaths(eq(testApplicationId), any(), any(), any(),
                                any()))
                                .thenReturn(1);

                mockMvc.perform(multipart("/applications/{id}/attachments", testApplicationId.toString())
                                .file(file)
                                .file(file1)
                                .file(file2))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.attachmentPaths", hasSize(4)));
        }

        @Test
        void testUploadAttachments_NoFiles() throws Exception {
                when(applicationsJdbcRepository.updateAttachmentPaths(eq(testApplicationId), any(), any(), any(),
                                any()))
                                .thenReturn(1);

                mockMvc.perform(multipart("/applications/{id}/attachments", testApplicationId.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.attachmentPaths", hasSize(4)));
        }

        @Test
        void testUploadAttachments_EmptyFile() throws Exception {
                MockMultipartFile emptyFile = new MockMultipartFile(
                                "file", "", "application/pdf", new byte[0]);

                when(applicationsJdbcRepository.updateAttachmentPaths(eq(testApplicationId), any(), any(), any(),
                                any()))
                                .thenReturn(1);

                mockMvc.perform(multipart("/applications/{id}/attachments", testApplicationId.toString())
                                .file(emptyFile))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.attachmentPaths", hasSize(4)));
        }

        @Test
        void testUploadAttachments_RepositoryException() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                                "file", "test.pdf", "application/pdf", "test".getBytes());

                when(applicationsJdbcRepository.updateAttachmentPaths(any(), any(), any(), any(), any()))
                                .thenThrow(new RuntimeException("Database error"));

                // Should still return OK because files are saved
                mockMvc.perform(multipart("/applications/{id}/attachments", testApplicationId.toString())
                                .file(file))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.attachmentPaths", hasSize(4)));
        }

        @Test
        void testUploadAttachments_AllFourFiles() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                                "file", "test0.pdf", "application/pdf", "test0".getBytes());
                MockMultipartFile file1 = new MockMultipartFile(
                                "file1", "test1.pdf", "application/pdf", "test1".getBytes());
                MockMultipartFile file2 = new MockMultipartFile(
                                "file2", "test2.pdf", "application/pdf", "test2".getBytes());
                MockMultipartFile file3 = new MockMultipartFile(
                                "file3", "test3.pdf", "application/pdf", "test3".getBytes());

                when(applicationsJdbcRepository.updateAttachmentPaths(eq(testApplicationId), any(), any(), any(),
                                any()))
                                .thenReturn(1);

                mockMvc.perform(multipart("/applications/{id}/attachments", testApplicationId.toString())
                                .file(file)
                                .file(file1)
                                .file(file2)
                                .file(file3))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.attachmentPaths", hasSize(4)));
        }

        @Test
        void testUploadAttachments_skipsUpdateWhenApplicationIdMissing() throws Exception {
                ResponseEntity<?> response = controller.uploadAttachments("", null, null, null, null);

                @SuppressWarnings("unchecked")
                List<String> paths = (List<String>) ((Map<?, ?>) response.getBody()).get("attachmentPaths");
                assertEquals(Arrays.asList(null, null, null, null), paths);
                verifyNoInteractions(applicationsJdbcRepository);
        }

        @Test
        void testUploadAttachments_handlesInvalidApplicationId() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                                "file", "test.pdf", "application/pdf", "test".getBytes());

                ResponseEntity<?> response = controller.uploadAttachments("invalid-uuid", file, null, null, null);

                @SuppressWarnings("unchecked")
                List<String> paths = (List<String>) ((Map<?, ?>) response.getBody()).get("attachmentPaths");
                assertNotNull(paths.get(0));
                assertNull(paths.get(1));
                assertNull(paths.get(2));
                assertNull(paths.get(3));
                verifyNoInteractions(applicationsJdbcRepository);
        }
}

package Group4.Childcare.controller;

import Group4.Childcare.Controller.AttachmentsController;
import Group4.Childcare.Repository.ApplicationsJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AttachmentsController.uploadAttachments 分支覆蓋率測試
 * 專門測試 uploadAttachments(String, MultipartFile, MultipartFile, MultipartFile, MultipartFile) 方法的所有分支
 * 目標：將分支覆蓋率提升到 90% 以上
 */
@ExtendWith(MockitoExtension.class)
class AttachmentsControllerCoverageTest {

    @Mock
    private ApplicationsJdbcRepository applicationsJdbcRepository;

    @InjectMocks
    private AttachmentsController controller;

    private String testApplicationId;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        testApplicationId = UUID.randomUUID().toString();

        // 創建臨時目錄用於測試
        tempDir = Files.createTempDirectory("test-attachments");
        ReflectionTestUtils.setField(controller, "ATTACHMENT_DIR", tempDir.toString());

        // 清理可能存在的文件
        cleanupTestFiles();
    }

    // ===========================================================================================
    // 1. 文件參數分支測試 (f != null && !f.isEmpty())
    // ===========================================================================================

    @Test
    void testUploadAttachments_AllFilesNull() throws IOException {
        // 測試所有文件都為 null
        when(applicationsJdbcRepository.updateAttachmentPaths(any(), any(), any(), any(), any()))
                .thenReturn(1);

        ResponseEntity<?> response = controller.uploadAttachments(testApplicationId, null, null, null, null);

        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        List<String> paths = (List<String>) result.get("attachmentPaths");

        // 所有路徑都應該是 null
        assertEquals(4, paths.size());
        assertNull(paths.get(0));
        assertNull(paths.get(1));
        assertNull(paths.get(2));
        assertNull(paths.get(3));

        verify(applicationsJdbcRepository).updateAttachmentPaths(
                any(UUID.class), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void testUploadAttachments_AllFilesEmpty() throws IOException {
        // 測試所有文件都為空（isEmpty() 返回 true）
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "text/plain", new byte[0]);
        MockMultipartFile emptyFile1 = new MockMultipartFile("file1", "", "text/plain", new byte[0]);
        MockMultipartFile emptyFile2 = new MockMultipartFile("file2", "", "text/plain", new byte[0]);
        MockMultipartFile emptyFile3 = new MockMultipartFile("file3", "", "text/plain", new byte[0]);

        when(applicationsJdbcRepository.updateAttachmentPaths(any(), any(), any(), any(), any()))
                .thenReturn(1);

        ResponseEntity<?> response = controller.uploadAttachments(
                testApplicationId, emptyFile, emptyFile1, emptyFile2, emptyFile3);

        Map<String, Object> result = (Map<String, Object>) response.getBody();
        List<String> paths = (List<String>) result.get("attachmentPaths");

        // 所有路徑都應該是 null（因為文件為空）
        assertEquals(4, paths.size());
        paths.forEach(path -> assertNull(path));
    }

    @Test
    void testUploadAttachments_OneFileNonNull() throws IOException {
        // 測試只有一個文件不為 null
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "test content".getBytes());

        when(applicationsJdbcRepository.updateAttachmentPaths(any(), any(), any(), any(), any()))
                .thenReturn(1);

        ResponseEntity<?> response = controller.uploadAttachments(testApplicationId, file, null, null, null);

        Map<String, Object> result = (Map<String, Object>) response.getBody();
        List<String> paths = (List<String>) result.get("attachmentPaths");

        assertEquals(4, paths.size());
        assertNotNull(paths.get(0)); // 第一個文件應該有路徑
        assertNull(paths.get(1));
        assertNull(paths.get(2));
        assertNull(paths.get(3));

        cleanupTestFiles();
    }

    @Test
    void testUploadAttachments_AllFilesMixed() throws IOException {
        // 測試混合情況：有些為 null，有些非 null，有些為空
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file2", "", "text/plain", new byte[0]);

        when(applicationsJdbcRepository.updateAttachmentPaths(any(), any(), any(), any(), any()))
                .thenReturn(1);

        ResponseEntity<?> response = controller.uploadAttachments(
                testApplicationId, file, null, emptyFile, null);

        Map<String, Object> result = (Map<String, Object>) response.getBody();
        List<String> paths = (List<String>) result.get("attachmentPaths");

        assertEquals(4, paths.size());
        assertNotNull(paths.get(0)); // file 有內容
        assertNull(paths.get(1));     // file1 為 null
        assertNull(paths.get(2));     // file2 為空
        assertNull(paths.get(3));     // file3 為 null

        cleanupTestFiles();
    }

    @Test
    void testUploadAttachments_AllFilesNonEmpty() throws IOException {
        // 測試所有文件都不為空
        MockMultipartFile file = new MockMultipartFile(
                "file", "test0.pdf", "application/pdf", "content0".getBytes());
        MockMultipartFile file1 = new MockMultipartFile(
                "file1", "test1.pdf", "application/pdf", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "file2", "test2.pdf", "application/pdf", "content2".getBytes());
        MockMultipartFile file3 = new MockMultipartFile(
                "file3", "test3.pdf", "application/pdf", "content3".getBytes());

        when(applicationsJdbcRepository.updateAttachmentPaths(any(), any(), any(), any(), any()))
                .thenReturn(1);

        ResponseEntity<?> response = controller.uploadAttachments(
                testApplicationId, file, file1, file2, file3);

        Map<String, Object> result = (Map<String, Object>) response.getBody();
        List<String> paths = (List<String>) result.get("attachmentPaths");

        assertEquals(4, paths.size());
        assertNotNull(paths.get(0));
        assertNotNull(paths.get(1));
        assertNotNull(paths.get(2));
        assertNotNull(paths.get(3));

        cleanupTestFiles();
    }

    // ===========================================================================================
    // 2. 目錄創建分支測試 (!dir.exists())
    // ===========================================================================================

    @Test
    void testUploadAttachments_DirectoryNotExists() throws IOException {
        // 測試目錄不存在，需要創建的情況

        // 使用一個不存在的目錄
        Path newDir = tempDir.resolve("new-dir");
        // 確保父目錄存在但新目錄不存在
        Files.createDirectories(tempDir);
        if (Files.exists(newDir)) {
            Files.walk(newDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
        ReflectionTestUtils.setField(controller, "ATTACHMENT_DIR", newDir.toString());

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());

        when(applicationsJdbcRepository.updateAttachmentPaths(any(), any(), any(), any(), any()))
                .thenReturn(1);

        ResponseEntity<?> response = controller.uploadAttachments(testApplicationId, file, null, null, null);

        // 驗證目錄被創建
        assertTrue(Files.exists(newDir), "New directory should have been created");
        assertEquals(200, response.getStatusCodeValue());

        cleanupTestFiles();
    }

    @Test
    void testUploadAttachments_DirectoryAlreadyExists() throws IOException {
        // 測試目錄已經存在的情況

        // 確保目錄存在
        Files.createDirectories(tempDir);
        assertTrue(Files.exists(tempDir));

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());

        when(applicationsJdbcRepository.updateAttachmentPaths(any(), any(), any(), any(), any()))
                .thenReturn(1);

        ResponseEntity<?> response = controller.uploadAttachments(testApplicationId, file, null, null, null);

        assertEquals(200, response.getStatusCodeValue());

        cleanupTestFiles();
    }

    // ===========================================================================================
    // 3. applicationId 分支測試
    // ===========================================================================================

    @Test
    void testUploadAttachments_ApplicationIdNull() throws IOException {
        // 測試 applicationId 為 null
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());

        ResponseEntity<?> response = controller.uploadAttachments(null, file, null, null, null);

        assertEquals(200, response.getStatusCodeValue());

        // 不應該調用 updateAttachmentPaths（因為 appUuid 為 null）
        verify(applicationsJdbcRepository, never()).updateAttachmentPaths(any(), any(), any(), any(), any());

        cleanupTestFiles();
    }

    @Test
    void testUploadAttachments_ApplicationIdEmpty() throws IOException {
        // 測試 applicationId 為空字串
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());

        ResponseEntity<?> response = controller.uploadAttachments("", file, null, null, null);

        assertEquals(200, response.getStatusCodeValue());

        // 不應該調用 updateAttachmentPaths
        verify(applicationsJdbcRepository, never()).updateAttachmentPaths(any(), any(), any(), any(), any());

        cleanupTestFiles();
    }

    @Test
    void testUploadAttachments_ApplicationIdValid() throws IOException {
        // 測試 applicationId 有效
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());

        when(applicationsJdbcRepository.updateAttachmentPaths(any(), any(), any(), any(), any()))
                .thenReturn(1);

        ResponseEntity<?> response = controller.uploadAttachments(testApplicationId, file, null, null, null);

        assertEquals(200, response.getStatusCodeValue());

        // 應該調用 updateAttachmentPaths
        verify(applicationsJdbcRepository, times(1)).updateAttachmentPaths(
                any(UUID.class), any(), isNull(), isNull(), isNull());

        cleanupTestFiles();
    }

    @Test
    void testUploadAttachments_ApplicationIdInvalidFormat() throws IOException {
        // 測試 applicationId 格式無效（無法轉換為 UUID）
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());

        ResponseEntity<?> response = controller.uploadAttachments("invalid-uuid", file, null, null, null);

        assertEquals(200, response.getStatusCodeValue());

        // 因為拋出異常，不應該調用 updateAttachmentPaths
        verify(applicationsJdbcRepository, never()).updateAttachmentPaths(any(), any(), any(), any(), any());

        cleanupTestFiles();
    }

    // ===========================================================================================
    // 4. 異常處理分支測試
    // ===========================================================================================

    @Test
    void testUploadAttachments_RepositoryThrowsException() throws IOException {
        // 測試 repository 拋出異常
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());

        when(applicationsJdbcRepository.updateAttachmentPaths(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Database error"));

        // 即使拋出異常，方法也應該繼續並返回結果
        ResponseEntity<?> response = controller.uploadAttachments(testApplicationId, file, null, null, null);

        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertNotNull(result.get("attachmentPaths"));

        cleanupTestFiles();
    }

    @Test
    void testUploadAttachments_FileIOException() throws IOException {
        // 測試文件 IO 異常
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.pdf");
        when(mockFile.getInputStream()).thenThrow(new IOException("IO Error"));

        // 應該拋出 IOException
        assertThrows(IOException.class, () -> {
            controller.uploadAttachments(testApplicationId, mockFile, null, null, null);
        });
    }

    // ===========================================================================================
    // 5. 文件名處理測試
    // ===========================================================================================

    @Test
    void testUploadAttachments_SpecialCharactersInFilename() throws IOException {
        // 測試特殊字符文件名
        MockMultipartFile file = new MockMultipartFile(
                "file", "測試文件 (1).pdf", "application/pdf", "content".getBytes());

        when(applicationsJdbcRepository.updateAttachmentPaths(any(), any(), any(), any(), any()))
                .thenReturn(1);

        ResponseEntity<?> response = controller.uploadAttachments(testApplicationId, file, null, null, null);

        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        List<String> paths = (List<String>) result.get("attachmentPaths");
        assertNotNull(paths.get(0));

        cleanupTestFiles();
    }

    @Test
    void testUploadAttachments_LongFilename() throws IOException {
        // 測試長文件名
        String longName = "a".repeat(200) + ".pdf";
        MockMultipartFile file = new MockMultipartFile(
                "file", longName, "application/pdf", "content".getBytes());

        when(applicationsJdbcRepository.updateAttachmentPaths(any(), any(), any(), any(), any()))
                .thenReturn(1);

        ResponseEntity<?> response = controller.uploadAttachments(testApplicationId, file, null, null, null);

        assertEquals(200, response.getStatusCodeValue());

        cleanupTestFiles();
    }

    @Test
    void testUploadAttachments_DifferentFileTypes() throws IOException {
        // 測試不同類型的文件
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "pdf content".getBytes());
        MockMultipartFile imageFile = new MockMultipartFile(
                "file1", "image.jpg", "image/jpeg", "jpg content".getBytes());
        MockMultipartFile textFile = new MockMultipartFile(
                "file2", "text.txt", "text/plain", "txt content".getBytes());

        when(applicationsJdbcRepository.updateAttachmentPaths(any(), any(), any(), any(), any()))
                .thenReturn(1);

        ResponseEntity<?> response = controller.uploadAttachments(
                testApplicationId, pdfFile, imageFile, textFile, null);

        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        List<String> paths = (List<String>) result.get("attachmentPaths");

        assertNotNull(paths.get(0));
        assertNotNull(paths.get(1));
        assertNotNull(paths.get(2));
        assertNull(paths.get(3));

        cleanupTestFiles();
    }

    // ===========================================================================================
    // 6. 邊界情況測試
    // ===========================================================================================

    @Test
    void testUploadAttachments_EmptyFilenameHandling() throws IOException {
        // 測試空文件名的處理
        MockMultipartFile file = new MockMultipartFile(
                "file", "", "application/pdf", "content".getBytes());

        when(applicationsJdbcRepository.updateAttachmentPaths(any(), any(), any(), any(), any()))
                .thenReturn(1);

        // 因為文件名為空，StringUtils.cleanPath 會拋出異常或返回空
        // 這個測試驗證錯誤處理
        try {
            ResponseEntity<?> response = controller.uploadAttachments(testApplicationId, file, null, null, null);
            // 如果沒有拋出異常，驗證響應
            assertEquals(200, response.getStatusCodeValue());
        } catch (Exception e) {
            // 預期可能拋出異常
            assertNotNull(e);
        }

        cleanupTestFiles();
    }

    @Test
    void testUploadAttachments_LargeFiles() throws IOException {
        // 測試較大的文件
        byte[] largeContent = new byte[10240]; // 10KB
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", largeContent);

        when(applicationsJdbcRepository.updateAttachmentPaths(any(), any(), any(), any(), any()))
                .thenReturn(1);

        ResponseEntity<?> response = controller.uploadAttachments(testApplicationId, file, null, null, null);

        assertEquals(200, response.getStatusCodeValue());

        cleanupTestFiles();
    }

    // ===========================================================================================
    // 輔助方法
    // ===========================================================================================

    private void cleanupTestFiles() {
        try {
            if (tempDir != null && Files.exists(tempDir)) {
                Files.walk(tempDir)
                        .sorted((a, b) -> -a.compareTo(b)) // 先刪除文件再刪除目錄
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                            }
                        });
            }
        } catch (IOException ignored) {
        }
    }
}


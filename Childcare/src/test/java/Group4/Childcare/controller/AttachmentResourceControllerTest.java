package Group4.Childcare.controller;

import Group4.Childcare.Controller.AttachmentResourceController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AttachmentResourceController 單元測試
 * 測試附件資源下載功能
 */
@ExtendWith(MockitoExtension.class)
class AttachmentResourceControllerTest {

    @InjectMocks
    private AttachmentResourceController controller;

    @TempDir
    Path tempDir;

    private Path testFile;
    private String testFileName;

    @BeforeEach
    void setUp() throws IOException {
        // 創建測試文件
        testFileName = "uuid_testfile.pdf";
        testFile = tempDir.resolve(testFileName);
        Files.write(testFile, "Test PDF Content".getBytes());

        // 使用反射設置私有方法返回測試目錄
        // 注意：由於 getStorageLocation 是私有方法，我們需要模擬文件系統
    }

    @Test
    void testGetFile_Success() throws IOException {
        // Given - 創建測試文件在實際路徑
        Path storageDir = Path.of(System.getProperty("user.dir"), "AttachmentResource");
        Files.createDirectories(storageDir);
        Path realTestFile = storageDir.resolve("test_document.pdf");

        try {
            Files.write(realTestFile, "Test PDF Content".getBytes());

            // When
            ResponseEntity<?> response = controller.getFile("test_document.pdf");

            // Then
            assertEquals(200, response.getStatusCodeValue());
            assertNotNull(response.getBody());
            assertTrue(response.getBody() instanceof byte[]);

        } finally {
            // Cleanup
            Files.deleteIfExists(realTestFile);
        }
    }

    @Test
    void testGetFile_FileNotFound() {
        // When
        ResponseEntity<?> response = controller.getFile("nonexistent.pdf");

        // Then
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void testGetFile_ExtractsOriginalFilename() throws IOException {
        // Given - 文件名格式為 UUID_原始檔名
        Path storageDir = Path.of(System.getProperty("user.dir"), "AttachmentResource");
        Files.createDirectories(storageDir);
        String uuidFileName = "550e8400-e29b-41d4-a716-446655440000_mydocument.pdf";
        Path testFileWithUUID = storageDir.resolve(uuidFileName);

        try {
            Files.write(testFileWithUUID, "PDF Content".getBytes());

            // When
            ResponseEntity<?> response = controller.getFile(uuidFileName);

            // Then
            assertEquals(200, response.getStatusCodeValue());
            String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
            assertNotNull(contentDisposition);
            assertTrue(contentDisposition.contains("mydocument.pdf"),
                    "應該提取原始檔名: " + contentDisposition);

        } finally {
            Files.deleteIfExists(testFileWithUUID);
        }
    }
}

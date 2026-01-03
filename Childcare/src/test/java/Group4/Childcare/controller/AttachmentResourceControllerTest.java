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

    @Test
    void testGetFile_NoUnderscoreInFilename() throws IOException {
        // Given - 檔名沒有底線，不需要提取
        Path storageDir = Path.of(System.getProperty("user.dir"), "AttachmentResource");
        Files.createDirectories(storageDir);
        String simpleFileName = "simpledocument.pdf";
        Path simpleFile = storageDir.resolve(simpleFileName);

        try {
            Files.write(simpleFile, "PDF Content".getBytes());

            // When
            ResponseEntity<?> response = controller.getFile(simpleFileName);

            // Then
            assertEquals(200, response.getStatusCodeValue());
            String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
            assertNotNull(contentDisposition);
            assertTrue(contentDisposition.contains("simpledocument.pdf"),
                    "應該使用原檔名: " + contentDisposition);

        } finally {
            Files.deleteIfExists(simpleFile);
        }
    }

    @Test
    void testGetFile_UnderscoreAtEnd() throws IOException {
        // Given - 底線在檔名末尾
        Path storageDir = Path.of(System.getProperty("user.dir"), "AttachmentResource");
        Files.createDirectories(storageDir);
        String fileName = "document_";
        Path testFileEndUnderscore = storageDir.resolve(fileName);

        try {
            Files.write(testFileEndUnderscore, "Content".getBytes());

            // When
            ResponseEntity<?> response = controller.getFile(fileName);

            // Then
            assertEquals(200, response.getStatusCodeValue());
            String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
            assertNotNull(contentDisposition);
            assertTrue(contentDisposition.contains("document_"),
                    "底線在末尾時應使用原檔名: " + contentDisposition);

        } finally {
            Files.deleteIfExists(testFileEndUnderscore);
        }
    }

    @Test
    void testGetFile_UnderscoreAtStart() throws IOException {
        // Given - 底線在開頭 (underscoreIdx = 0)
        Path storageDir = Path.of(System.getProperty("user.dir"), "AttachmentResource");
        Files.createDirectories(storageDir);
        String fileName = "_document.pdf";
        Path testFileStartUnderscore = storageDir.resolve(fileName);

        try {
            Files.write(testFileStartUnderscore, "Content".getBytes());

            // When
            ResponseEntity<?> response = controller.getFile(fileName);

            // Then
            assertEquals(200, response.getStatusCodeValue());
            String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
            assertNotNull(contentDisposition);
            // underscoreIdx = 0，條件 underscoreIdx > 0 為 false，使用原檔名
            assertTrue(contentDisposition.contains("_document.pdf"),
                    "底線在開頭時應使用原檔名: " + contentDisposition);

        } finally {
            Files.deleteIfExists(testFileStartUnderscore);
        }
    }

    @Test
    void testGetFile_ContentTypeNull() throws IOException {
        // Given - 創建沒有明確 content type 的文件（使用罕見副檔名）
        Path storageDir = Path.of(System.getProperty("user.dir"), "AttachmentResource");
        Files.createDirectories(storageDir);
        String unknownFileName = "uuid_file.unknownext123";
        Path unknownFile = storageDir.resolve(unknownFileName);

        try {
            Files.write(unknownFile, "Unknown Content".getBytes());

            // When
            ResponseEntity<?> response = controller.getFile(unknownFileName);

            // Then
            assertEquals(200, response.getStatusCodeValue());
            // contentType 為 null 時，應使用 APPLICATION_OCTET_STREAM
            assertEquals("application/octet-stream", 
                    response.getHeaders().getContentType().toString());

        } finally {
            Files.deleteIfExists(unknownFile);
        }
    }

    @Test
    void testGetFile_MultipleUnderscores() throws IOException {
        // Given - 檔名有多個底線
        Path storageDir = Path.of(System.getProperty("user.dir"), "AttachmentResource");
        Files.createDirectories(storageDir);
        String fileName = "uuid_test_file_name.pdf";
        Path multiUnderscoreFile = storageDir.resolve(fileName);

        try {
            Files.write(multiUnderscoreFile, "Content".getBytes());

            // When
            ResponseEntity<?> response = controller.getFile(fileName);

            // Then
            assertEquals(200, response.getStatusCodeValue());
            String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
            assertNotNull(contentDisposition);
            // 應從第一個底線後開始提取
            assertTrue(contentDisposition.contains("test_file_name.pdf"),
                    "應該從第一個底線後提取: " + contentDisposition);

        } finally {
            Files.deleteIfExists(multiUnderscoreFile);
        }
    }

    @Test
    void testGetFile_DifferentContentTypes() throws IOException {
        // Given - 測試不同的檔案類型
        Path storageDir = Path.of(System.getProperty("user.dir"), "AttachmentResource");
        Files.createDirectories(storageDir);
        
        String[] fileNames = {"test.jpg", "test.png", "test.txt", "test.json"};
        
        for (String fileName : fileNames) {
            Path testFile = storageDir.resolve(fileName);
            try {
                Files.write(testFile, "Test Content".getBytes());

                // When
                ResponseEntity<?> response = controller.getFile(fileName);

                // Then
                assertEquals(200, response.getStatusCodeValue());
                assertNotNull(response.getHeaders().getContentType());

            } finally {
                Files.deleteIfExists(testFile);
            }
        }
    }

    @Test
    void testGetFile_EmptyFileName() {
        // When - 空檔名會導致路徑解析問題
        ResponseEntity<?> response = controller.getFile("");

        // Then - 空檔名會導致內部錯誤或檔案不存在
        // 實際行為可能是 500 (內部錯誤) 或 404 (檔案不存在)
        assertTrue(response.getStatusCodeValue() == 404 || response.getStatusCodeValue() == 500,
                "Expected 404 or 500, but was: " + response.getStatusCodeValue());
    }

    @Test
    void testGetFile_FilenameWithSpaces() throws IOException {
        // Given - 檔名包含空格
        Path storageDir = Path.of(System.getProperty("user.dir"), "AttachmentResource");
        Files.createDirectories(storageDir);
        String fileName = "uuid_test file with spaces.pdf";
        Path spaceFile = storageDir.resolve(fileName);

        try {
            Files.write(spaceFile, "Content".getBytes());

            // When
            ResponseEntity<?> response = controller.getFile(fileName);

            // Then
            assertEquals(200, response.getStatusCodeValue());
            String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
            assertNotNull(contentDisposition);
            assertTrue(contentDisposition.contains("test file with spaces.pdf"));

        } finally {
            Files.deleteIfExists(spaceFile);
        }
    }

    @Test
    void testGetFile_VeryLongFilename() throws IOException {
        // Given - 非常長的檔名
        Path storageDir = Path.of(System.getProperty("user.dir"), "AttachmentResource");
        Files.createDirectories(storageDir);
        String longName = "uuid_" + "a".repeat(200) + ".pdf";
        Path longFile = storageDir.resolve(longName);

        try {
            Files.write(longFile, "Content".getBytes());

            // When
            ResponseEntity<?> response = controller.getFile(longName);

            // Then
            assertEquals(200, response.getStatusCodeValue());

        } finally {
            Files.deleteIfExists(longFile);
        }
    }

    @Test
    void testGetFile_SpecialCharactersInFilename() throws IOException {
        // Given - 檔名包含特殊字元
        Path storageDir = Path.of(System.getProperty("user.dir"), "AttachmentResource");
        Files.createDirectories(storageDir);
        String fileName = "uuid_test-file(1).pdf";
        Path specialFile = storageDir.resolve(fileName);

        try {
            Files.write(specialFile, "Content".getBytes());

            // When
            ResponseEntity<?> response = controller.getFile(fileName);

            // Then
            assertEquals(200, response.getStatusCodeValue());

        } finally {
            Files.deleteIfExists(specialFile);
        }
    }
}

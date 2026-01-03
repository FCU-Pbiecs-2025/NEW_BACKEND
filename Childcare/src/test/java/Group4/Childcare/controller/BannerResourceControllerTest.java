package Group4.Childcare.controller;

import Group4.Childcare.Controller.BannerResourceController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BannerResourceController 單元測試
 * 測試橫幅圖片資源下載功能
 */
@ExtendWith(MockitoExtension.class)
class BannerResourceControllerTest {

    @InjectMocks
    private BannerResourceController controller;

    @Test
    void testGetBannerImage_Success() throws IOException {
        // Given - 創建測試圖片
        Path storageDir = Path.of(System.getProperty("user.dir"), "BannerResource");
        Files.createDirectories(storageDir);
        Path testImage = storageDir.resolve("test-banner.jpg");

        try {
            Files.write(testImage, "Test Image Content".getBytes());

            // When
            ResponseEntity<?> response = controller.getBannerImage("test-banner.jpg");

            // Then
            assertEquals(200, response.getStatusCodeValue());
            assertNotNull(response.getBody());
            assertTrue(response.getBody() instanceof byte[]);
            assertNotNull(response.getHeaders().getContentType());

        } finally {
            Files.deleteIfExists(testImage);
        }
    }

    @Test
    void testGetBannerImage_FileNotFound() {
        // When
        ResponseEntity<?> response = controller.getBannerImage("nonexistent-banner.jpg");

        // Then
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void testGetBannerImage_ContentDispositionHeader() throws IOException {
        // Given
        Path storageDir = Path.of(System.getProperty("user.dir"), "BannerResource");
        Files.createDirectories(storageDir);
        String imageName = "summer-promotion.png";
        Path testImage = storageDir.resolve(imageName);

        try {
            Files.write(testImage, "PNG Image Content".getBytes());

            // When
            ResponseEntity<?> response = controller.getBannerImage(imageName);

            // Then
            assertEquals(200, response.getStatusCodeValue());
            String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
            assertNotNull(contentDisposition);
            assertTrue(contentDisposition.contains(imageName));
            assertTrue(contentDisposition.contains("inline"));

        } finally {
            Files.deleteIfExists(testImage);
        }
    }

    @Test
    void testGetBannerImage_VariousImageFormats() throws IOException {
        // Test different image formats
        Path storageDir = Path.of(System.getProperty("user.dir"), "BannerResource");
        Files.createDirectories(storageDir);

        String[] imageNames = { "banner1.jpg", "banner2.png", "banner3.gif" };

        for (String imageName : imageNames) {
            Path testImage = storageDir.resolve(imageName);
            try {
                Files.write(testImage, ("Content of " + imageName).getBytes());

                // When
                ResponseEntity<?> response = controller.getBannerImage(imageName);

                // Then
                assertEquals(200, response.getStatusCodeValue(),
                        "應該成功返回 " + imageName);

            } finally {
                Files.deleteIfExists(testImage);
            }
        }
    }

    @Test
    void testGetBannerImage_UnknownContentType() throws IOException {
        // 測試 contentType == null 分支
        // 創建一個沒有副檔名或無法識別的文件類型
        Path storageDir = Path.of(System.getProperty("user.dir"), "BannerResource");
        Files.createDirectories(storageDir);
        String imageName = "unknown-file";  // 沒有副檔名
        Path testFile = storageDir.resolve(imageName);

        try {
            Files.write(testFile, "Unknown file content".getBytes());

            // When
            ResponseEntity<?> response = controller.getBannerImage(imageName);

            // Then
            assertEquals(200, response.getStatusCodeValue());
            assertNotNull(response.getHeaders().getContentType());
            // 當 contentType 為 null 時，應該使用 APPLICATION_OCTET_STREAM
            assertTrue(response.getHeaders().getContentType().toString()
                    .contains("application/octet-stream") ||
                    response.getHeaders().getContentType().toString()
                    .contains("application"));

        } finally {
            Files.deleteIfExists(testFile);
        }
    }

    @Test
    void testGetBannerImage_UnrecognizedExtension() throws IOException {
        // 測試無法識別的副檔名（也會導致 contentType == null）
        Path storageDir = Path.of(System.getProperty("user.dir"), "BannerResource");
        Files.createDirectories(storageDir);
        String imageName = "data.xyz";  // 不常見的副檔名
        Path testFile = storageDir.resolve(imageName);

        try {
            Files.write(testFile, "XYZ file content".getBytes());

            // When
            ResponseEntity<?> response = controller.getBannerImage(imageName);

            // Then
            assertEquals(200, response.getStatusCodeValue());
            assertNotNull(response.getBody());

        } finally {
            Files.deleteIfExists(testFile);
        }
    }

    @Test
    void testGetBannerImage_IOException() throws IOException {
        // 測試 IOException 分支
        // 創建文件後立即刪除，模擬讀取時的競態條件
        Path storageDir = Path.of(System.getProperty("user.dir"), "BannerResource");
        Files.createDirectories(storageDir);

        // 創建一個特殊名稱的測試場景
        String imageName = "io-error-test.jpg";
        Path testFile = storageDir.resolve(imageName);

        try {
            // 創建文件
            Files.write(testFile, "Test content".getBytes());

            // 設置文件為不可讀來觸發 IOException
            // 注意：這在某些系統上可能不會觸發 IOException
            // 更可靠的方法是使用目錄而不是文件
            Files.delete(testFile);
            Files.createDirectory(testFile); // 創建同名目錄

            // When - 嘗試讀取（會因為是目錄而拋出異常）
            ResponseEntity<?> response = controller.getBannerImage(imageName);

            // Then
            // 應該返回 500 錯誤
            assertEquals(500, response.getStatusCodeValue());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().toString().contains("Failed to read image"));

        } finally {
            // 清理
            try {
                if (Files.isDirectory(testFile)) {
                    Files.delete(testFile);
                } else {
                    Files.deleteIfExists(testFile);
                }
            } catch (IOException ignored) {
            }
        }
    }

    @Test
    void testGetBannerImage_EmptyFile() throws IOException {
        // 測試空文件（邊界情況）
        Path storageDir = Path.of(System.getProperty("user.dir"), "BannerResource");
        Files.createDirectories(storageDir);
        String imageName = "empty.jpg";
        Path testFile = storageDir.resolve(imageName);

        try {
            Files.write(testFile, new byte[0]); // 空文件

            // When
            ResponseEntity<?> response = controller.getBannerImage(imageName);

            // Then
            assertEquals(200, response.getStatusCodeValue());
            byte[] body = (byte[]) response.getBody();
            assertNotNull(body);
            assertEquals(0, body.length);

        } finally {
            Files.deleteIfExists(testFile);
        }
    }

    @Test
    void testGetBannerImage_LargeFile() throws IOException {
        // 測試較大文件（確保能正確讀取）
        Path storageDir = Path.of(System.getProperty("user.dir"), "BannerResource");
        Files.createDirectories(storageDir);
        String imageName = "large-banner.jpg";
        Path testFile = storageDir.resolve(imageName);

        try {
            // 創建 1KB 的文件
            byte[] largeContent = new byte[1024];
            for (int i = 0; i < largeContent.length; i++) {
                largeContent[i] = (byte) (i % 256);
            }
            Files.write(testFile, largeContent);

            // When
            ResponseEntity<?> response = controller.getBannerImage(imageName);

            // Then
            assertEquals(200, response.getStatusCodeValue());
            byte[] body = (byte[]) response.getBody();
            assertNotNull(body);
            assertEquals(1024, body.length);

        } finally {
            Files.deleteIfExists(testFile);
        }
    }

    @Test
    void testGetBannerImage_SpecialCharactersInFilename() throws IOException {
        // 測試特殊字符文件名
        Path storageDir = Path.of(System.getProperty("user.dir"), "BannerResource");
        Files.createDirectories(storageDir);
        String imageName = "測試-banner_2024.jpg";  // 中文和特殊字符
        Path testFile = storageDir.resolve(imageName);

        try {
            Files.write(testFile, "Special filename content".getBytes());

            // When
            ResponseEntity<?> response = controller.getBannerImage(imageName);

            // Then
            assertEquals(200, response.getStatusCodeValue());
            String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
            assertNotNull(contentDisposition);
            assertTrue(contentDisposition.contains(imageName));

        } finally {
            Files.deleteIfExists(testFile);
        }
    }
}

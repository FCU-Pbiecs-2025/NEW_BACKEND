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
}

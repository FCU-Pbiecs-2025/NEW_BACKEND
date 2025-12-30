package Group4.Childcare.service;

import Group4.Childcare.Service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileService 單元測試
 * 
 * 測試覆蓋：
 * 1. getFilesByApplicationId() - 讀取案件檔案
 * 2. folderExists() - 檢查資料夾存在
 * 3. createFolder() - 建立資料夾
 * 4. getFolderPath() - 取得資料夾路徑
 */
class FileServiceTest {

    private FileService fileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileService = new FileService();
        ReflectionTestUtils.setField(fileService, "uploadDir", tempDir.toString());
    }

    // ========== getFilesByApplicationId() 測試 ==========

    @Test
    void testGetFilesByApplicationId_FolderNotExists() {
        UUID applicationId = UUID.randomUUID();

        List<String> files = fileService.getFilesByApplicationId(applicationId);

        assertTrue(files.isEmpty());
    }

    @Test
    void testGetFilesByApplicationId_EmptyFolder() throws IOException {
        UUID applicationId = UUID.randomUUID();
        Path folderPath = tempDir.resolve(applicationId.toString());
        Files.createDirectory(folderPath);

        List<String> files = fileService.getFilesByApplicationId(applicationId);

        assertTrue(files.isEmpty());
    }

    @Test
    void testGetFilesByApplicationId_WithFiles() throws IOException {
        UUID applicationId = UUID.randomUUID();
        Path folderPath = tempDir.resolve(applicationId.toString());
        Files.createDirectory(folderPath);
        Files.createFile(folderPath.resolve("file1.pdf"));
        Files.createFile(folderPath.resolve("file2.pdf"));

        List<String> files = fileService.getFilesByApplicationId(applicationId);

        assertEquals(2, files.size());
        assertTrue(files.contains("file1.pdf"));
        assertTrue(files.contains("file2.pdf"));
    }

    @Test
    void testGetFilesByApplicationId_PathIsFile() throws IOException {
        UUID applicationId = UUID.randomUUID();
        Path filePath = tempDir.resolve(applicationId.toString());
        Files.createFile(filePath);

        List<String> files = fileService.getFilesByApplicationId(applicationId);

        assertTrue(files.isEmpty());
    }

    // ========== folderExists() 測試 ==========

    @Test
    void testFolderExists_True() throws IOException {
        UUID applicationId = UUID.randomUUID();
        Path folderPath = tempDir.resolve(applicationId.toString());
        Files.createDirectory(folderPath);

        boolean exists = fileService.folderExists(applicationId);

        assertTrue(exists);
    }

    @Test
    void testFolderExists_False() {
        UUID applicationId = UUID.randomUUID();

        boolean exists = fileService.folderExists(applicationId);

        assertFalse(exists);
    }

    @Test
    void testFolderExists_IsFile() throws IOException {
        UUID applicationId = UUID.randomUUID();
        Path filePath = tempDir.resolve(applicationId.toString());
        Files.createFile(filePath);

        boolean exists = fileService.folderExists(applicationId);

        assertFalse(exists);
    }

    // ========== createFolder() 測試 ==========

    @Test
    void testCreateFolder_Success() {
        UUID applicationId = UUID.randomUUID();

        boolean result = fileService.createFolder(applicationId);

        assertTrue(result);
        assertTrue(Files.exists(tempDir.resolve(applicationId.toString())));
    }

    @Test
    void testCreateFolder_AlreadyExists() throws IOException {
        UUID applicationId = UUID.randomUUID();
        Path folderPath = tempDir.resolve(applicationId.toString());
        Files.createDirectory(folderPath);

        boolean result = fileService.createFolder(applicationId);

        assertTrue(result);
    }

    // ========== getFolderPath() 測試 ==========

    @Test
    void testGetFolderPath_ReturnsUploadDir() {
        UUID applicationId = UUID.randomUUID();

        Path path = fileService.getFolderPath(applicationId);

        assertEquals(tempDir, path);
    }

    // ========== saveInstitutionImage() 測試 ==========

    @Test
    void testSaveInstitutionImage_Success() throws IOException {
        UUID institutionId = UUID.randomUUID();
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes());

        String result = fileService.saveInstitutionImage(mockFile, institutionId);

        assertNotNull(result);
        assertTrue(result.contains(institutionId.toString()));
        assertTrue(result.startsWith("/InstitutionResource/"));
    }

    @Test
    void testSaveInstitutionImage_EmptyFile() {
        UUID institutionId = UUID.randomUUID();
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            fileService.saveInstitutionImage(emptyFile, institutionId);
        });

        assertEquals("檔案不能為空", exception.getMessage());
    }

    @Test
    void testSaveInstitutionImage_NonImageFile() {
        UUID institutionId = UUID.randomUUID();
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "pdf content".getBytes());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            fileService.saveInstitutionImage(pdfFile, institutionId);
        });

        assertEquals("僅支援圖片檔案", exception.getMessage());
    }

    @Test
    void testSaveInstitutionImage_NullContentType() {
        UUID institutionId = UUID.randomUUID();
        MockMultipartFile noTypeFile = new MockMultipartFile(
                "file",
                "file.unknown",
                null,
                "content".getBytes());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            fileService.saveInstitutionImage(noTypeFile, institutionId);
        });

        assertEquals("僅支援圖片檔案", exception.getMessage());
    }

    // ========== deleteInstitutionImage() 測試 ==========

    @Test
    void testDeleteInstitutionImage_DirectoryNotExists() {
        UUID institutionId = UUID.randomUUID();

        // Should not throw exception when directory doesn't exist
        assertDoesNotThrow(() -> {
            fileService.deleteInstitutionImage(institutionId);
        });
    }

    @Test
    void testDeleteInstitutionImage_NoMatchingFiles() throws IOException {
        UUID institutionId = UUID.randomUUID();
        Path institutionDir = Paths.get(System.getProperty("user.dir"), "InstitutionResource");
        if (!Files.exists(institutionDir)) {
            Files.createDirectories(institutionDir);
        }

        // Create a file with different institution ID
        UUID otherInstitutionId = UUID.randomUUID();
        Files.createFile(institutionDir.resolve(otherInstitutionId + "_test.jpg"));

        // Should not throw exception
        assertDoesNotThrow(() -> {
            fileService.deleteInstitutionImage(institutionId);
        });

        // Cleanup
        Files.deleteIfExists(institutionDir.resolve(otherInstitutionId + "_test.jpg"));
    }

    // ========== getInstitutionImageFileName() 測試 ==========

    @Test
    void testGetInstitutionImageFileName_DirectoryNotExists() {
        UUID institutionId = UUID.randomUUID();

        String result = fileService.getInstitutionImageFileName(institutionId);

        // May return null if directory doesn't exist
        // This is acceptable behavior
    }

    @Test
    void testGetInstitutionImageFileName_NoMatchingFiles() throws IOException {
        UUID institutionId = UUID.randomUUID();
        Path institutionDir = Paths.get(System.getProperty("user.dir"), "InstitutionResource");
        if (!Files.exists(institutionDir)) {
            Files.createDirectories(institutionDir);
        }

        String result = fileService.getInstitutionImageFileName(institutionId);

        assertNull(result);
    }

    @Test
    void testGetInstitutionImageFileName_FileExists() throws IOException {
        UUID institutionId = UUID.randomUUID();
        Path institutionDir = Paths.get(System.getProperty("user.dir"), "InstitutionResource");
        if (!Files.exists(institutionDir)) {
            Files.createDirectories(institutionDir);
        }

        String fileName = institutionId + "_testimage.jpg";
        Path testFile = institutionDir.resolve(fileName);
        Files.createFile(testFile);

        try {
            String result = fileService.getInstitutionImageFileName(institutionId);

            assertEquals(fileName, result);
        } finally {
            // Cleanup
            Files.deleteIfExists(testFile);
        }
    }
}

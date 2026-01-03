package Group4.Childcare.controller;

import Group4.Childcare.Controller.BannersController;
import Group4.Childcare.Model.Banners;
import Group4.Childcare.Service.BannersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BannersController 分支覆蓋率測試
 * 專門測試以下方法的所有分支以達到 90% 以上覆蓋率：
 * 1. BannersController(BannersService)
 * 2. updateWithFile(Integer, MultipartFile, Banners)
 * 3. upload(MultipartFile, Banners)
 * 4. getImage(String)
 */
@ExtendWith(MockitoExtension.class)
class BannersControllerCoverageTest {

    @Mock
    private BannersService service;

    private BannersController controller;
    private Path storageLocation;

    @BeforeEach
    void setUp() {
        // 每個測試前清理
        String basePath = System.getProperty("user.dir");
        File dir = new File(basePath, "BannerResource");
        storageLocation = dir.toPath();
    }

    // ===========================================================================================
    // 1. BannersController(BannersService) 建構子測試
    // ===========================================================================================

    @Test
    void testConstructor_DirectoryAlreadyExists() throws IOException {
        // 測試目錄已存在的情況
        String basePath = System.getProperty("user.dir");
        File dir = new File(basePath, "BannerResource");

        // 確保目錄存在
        if (!dir.exists()) {
            dir.mkdirs();
        }

        assertTrue(dir.exists());

        // When
        controller = new BannersController(service);

        // Then - 應該成功創建
        assertNotNull(controller);
    }

    @Test
    void testConstructor_DirectoryNotExists() throws IOException {
        // 測試目錄不存在，需要創建的情況
        String basePath = System.getProperty("user.dir");
        File dir = new File(basePath, "BannerResource");

        // 刪除目錄（如果存在）
        if (dir.exists()) {
            deleteDirectory(dir);
        }

        assertFalse(dir.exists());

        // When
        controller = new BannersController(service);

        // Then - 目錄應該被創建
        assertTrue(dir.exists());
    }

    @Test
    void testConstructor_MkdirsReturnsFalse() throws IOException {
        // 測試 mkdirs() 返回 false 但目錄最終存在的情況
        // 這個分支覆蓋 if (!created && !dir.exists())
        String basePath = System.getProperty("user.dir");
        File dir = new File(basePath, "BannerResource");

        // 確保目錄存在
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // When - 即使 mkdirs 返回 false（因為目錄已存在），也不應拋出異常
        controller = new BannersController(service);

        // Then
        assertNotNull(controller);
    }

    // ===========================================================================================
    // 2. upload(MultipartFile, Banners) 方法測試
    // ===========================================================================================

    @Test
    void testUpload_FileNull() throws IOException {
        controller = new BannersController(service);

        // When - file 為 null
        ResponseEntity<?> response = controller.upload(null, new Banners());

        // Then
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("File is required", response.getBody());
    }

    @Test
    void testUpload_FileEmpty() throws IOException {
        controller = new BannersController(service);
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", new byte[0]);

        // When - file 為空
        ResponseEntity<?> response = controller.upload(emptyFile, new Banners());

        // Then
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("File is required", response.getBody());
    }

    @Test
    void testUpload_MetaNull() throws IOException {
        controller = new BannersController(service);
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "test content".getBytes());

        // When - meta 為 null
        ResponseEntity<?> response = controller.upload(file, null);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("meta 必須包含"));
    }

    @Test
    void testUpload_MetaSortOrderZero() throws IOException {
        controller = new BannersController(service);
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "test content".getBytes());

        Banners meta = new Banners();
        meta.setSortOrder(0); // <= 0
        meta.setStartTime(LocalDateTime.now());
        meta.setEndTime(LocalDateTime.now().plusDays(1));
        meta.setStatus(true);

        // When - sortOrder <= 0
        ResponseEntity<?> response = controller.upload(file, meta);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("meta 必須包含"));
    }

    @Test
    void testUpload_MetaStartTimeNull() throws IOException {
        controller = new BannersController(service);
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "test content".getBytes());

        Banners meta = new Banners();
        meta.setSortOrder(1);
        meta.setStartTime(null); // null
        meta.setEndTime(LocalDateTime.now().plusDays(1));
        meta.setStatus(true);

        // When - startTime 為 null
        ResponseEntity<?> response = controller.upload(file, meta);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("meta 必須包含"));
    }

    @Test
    void testUpload_MetaEndTimeNull() throws IOException {
        controller = new BannersController(service);
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "test content".getBytes());

        Banners meta = new Banners();
        meta.setSortOrder(1);
        meta.setStartTime(LocalDateTime.now());
        meta.setEndTime(null); // null
        meta.setStatus(true);

        // When - endTime 為 null
        ResponseEntity<?> response = controller.upload(file, meta);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("meta 必須包含"));
    }

    @Test
    void testUpload_MetaStatusNull() throws IOException {
        controller = new BannersController(service);
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "test content".getBytes());

        Banners meta = new Banners();
        meta.setSortOrder(1);
        meta.setStartTime(LocalDateTime.now());
        meta.setEndTime(LocalDateTime.now().plusDays(1));
        meta.setStatus(null); // null

        // When - status 為 null
        ResponseEntity<?> response = controller.upload(file, meta);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("meta 必須包含"));
    }

    @Test
    void testUpload_OriginalFilenameNull() throws IOException {
        controller = new BannersController(service);
        MockMultipartFile file = new MockMultipartFile(
            "file", null, "image/jpeg", "test content".getBytes()); // originalFilename 為 null

        Banners meta = createValidMeta();
        Banners created = new Banners();
        created.setSortOrder(1);

        when(service.create(any(Banners.class))).thenReturn(created);

        // When - originalFilename 為 null，應使用 "file" 作為默認值
        ResponseEntity<?> response = controller.upload(file, meta);

        // Then
        assertEquals(201, response.getStatusCodeValue());
        verify(service).create(any(Banners.class));
    }

    @Test
    void testUpload_Success() throws IOException {
        controller = new BannersController(service);
        MockMultipartFile file = new MockMultipartFile(
            "file", "banner.jpg", "image/jpeg", "test content".getBytes());

        Banners meta = createValidMeta();
        Banners created = new Banners();
        created.setSortOrder(1);

        when(service.create(any(Banners.class))).thenReturn(created);

        // When
        ResponseEntity<?> response = controller.upload(file, meta);

        // Then
        assertEquals(201, response.getStatusCodeValue());
        verify(service).create(any(Banners.class));

        // 清理測試文件
        cleanupTestFiles();
    }

    @Test
    void testUpload_IOExceptionDuringFileSave() throws IOException {
        controller = new BannersController(service);

        // 創建一個會在讀取時拋出異常的 MultipartFile
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getInputStream()).thenThrow(new IOException("IO Error"));

        Banners meta = createValidMeta();

        // When - 文件保存時發生 IO 異常
        ResponseEntity<?> response = controller.upload(mockFile, meta);

        // Then
        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Failed to store file"));
    }

    // ===========================================================================================
    // 3. updateWithFile(Integer, MultipartFile, Banners) 方法測試
    // ===========================================================================================

    @Test
    void testUpdateWithFile_BannerNotFound() throws IOException {
        controller = new BannersController(service);

        when(service.getById(1)).thenReturn(Optional.empty());

        // When - Banner 不存在
        ResponseEntity<?> response = controller.updateWithFile(1, null, null);

        // Then
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void testUpdateWithFile_MetaNullFileNull() throws IOException {
        controller = new BannersController(service);

        Banners existing = createValidBanner();
        when(service.getById(1)).thenReturn(Optional.of(existing));
        when(service.update(eq(1), any(Banners.class))).thenReturn(existing);

        // When - meta 和 file 都為 null
        ResponseEntity<?> response = controller.updateWithFile(1, null, null);

        // Then
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void testUpdateWithFile_MetaNotNull() throws IOException {
        controller = new BannersController(service);

        Banners existing = createValidBanner();
        Banners meta = new Banners();
        meta.setStartTime(LocalDateTime.now().plusDays(1));
        meta.setEndTime(LocalDateTime.now().plusDays(10));
        meta.setLinkUrl("https://new.example.com");
        meta.setStatus(false);
        meta.setImageName(""); // empty

        when(service.getById(1)).thenReturn(Optional.of(existing));
        when(service.update(eq(1), any(Banners.class))).thenReturn(existing);

        // When - meta 不為 null，但 imageName 為空
        ResponseEntity<?> response = controller.updateWithFile(1, null, meta);

        // Then
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void testUpdateWithFile_MetaWithImageName() throws IOException {
        controller = new BannersController(service);

        Banners existing = createValidBanner();
        Banners meta = new Banners();
        meta.setStartTime(LocalDateTime.now().plusDays(1));
        meta.setEndTime(LocalDateTime.now().plusDays(10));
        meta.setLinkUrl("https://new.example.com");
        meta.setStatus(false);
        meta.setImageName("new-image.jpg"); // 不為空

        when(service.getById(1)).thenReturn(Optional.of(existing));
        when(service.update(eq(1), any(Banners.class))).thenReturn(existing);

        // When - meta 的 imageName 不為 null 且不為空
        ResponseEntity<?> response = controller.updateWithFile(1, null, meta);

        // Then
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void testUpdateWithFile_FileNotNull() throws IOException {
        controller = new BannersController(service);

        Banners existing = createValidBanner();
        existing.setImageName("old-image.jpg");

        // 創建舊文件
        Path oldFile = storageLocation.resolve("old-image.jpg");
        Files.createDirectories(storageLocation);
        Files.write(oldFile, "old content".getBytes());

        MockMultipartFile newFile = new MockMultipartFile(
            "file", "new.jpg", "image/jpeg", "new content".getBytes());

        when(service.getById(1)).thenReturn(Optional.of(existing));
        when(service.update(eq(1), any(Banners.class))).thenReturn(existing);

        // When - file 不為 null
        ResponseEntity<?> response = controller.updateWithFile(1, newFile, null);

        // Then
        assertEquals(200, response.getStatusCodeValue());

        // 清理
        cleanupTestFiles();
    }

    @Test
    void testUpdateWithFile_FileNotNullOriginalFilenameNull() throws IOException {
        controller = new BannersController(service);

        Banners existing = createValidBanner();

        MockMultipartFile newFile = new MockMultipartFile(
            "file", null, "image/jpeg", "new content".getBytes()); // originalFilename 為 null

        when(service.getById(1)).thenReturn(Optional.of(existing));
        when(service.update(eq(1), any(Banners.class))).thenReturn(existing);

        // When - originalFilename 為 null，使用 "file" 作為默認值
        ResponseEntity<?> response = controller.updateWithFile(1, newFile, null);

        // Then
        assertEquals(200, response.getStatusCodeValue());

        cleanupTestFiles();
    }

    @Test
    void testUpdateWithFile_BannerImageNameNull() throws IOException {
        controller = new BannersController(service);

        Banners existing = createValidBanner();
        existing.setImageName(null); // imageName 為 null

        MockMultipartFile newFile = new MockMultipartFile(
            "file", "new.jpg", "image/jpeg", "new content".getBytes());

        when(service.getById(1)).thenReturn(Optional.of(existing));
        when(service.update(eq(1), any(Banners.class))).thenReturn(existing);

        // When - banner.getImageName() 為 null，不會嘗試刪除舊文件
        ResponseEntity<?> response = controller.updateWithFile(1, newFile, null);

        // Then
        assertEquals(200, response.getStatusCodeValue());

        cleanupTestFiles();
    }

    @Test
    void testUpdateWithFile_IOExceptionDuringFileSave() throws IOException {
        controller = new BannersController(service);

        Banners existing = createValidBanner();

        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getInputStream()).thenThrow(new IOException("IO Error"));

        when(service.getById(1)).thenReturn(Optional.of(existing));

        // When - 文件保存時發生 IO 異常
        ResponseEntity<?> response = controller.updateWithFile(1, mockFile, null);

        // Then
        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Failed to store file"));
    }

    @Test
    void testUpdateWithFile_ServiceUpdateThrowsException() throws IOException {
        controller = new BannersController(service);

        Banners existing = createValidBanner();

        when(service.getById(1)).thenReturn(Optional.of(existing));
        when(service.update(eq(1), any(Banners.class)))
            .thenThrow(new RuntimeException("Update failed"));

        // When - service.update 拋出異常
        ResponseEntity<?> response = controller.updateWithFile(1, null, null);

        // Then
        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Failed to update banner"));
    }

    // ===========================================================================================
    // 4. getImage(String) 方法測試
    // ===========================================================================================

    @Test
    void testGetImage_WithQueryParameters() throws IOException {
        controller = new BannersController(service);

        // 創建測試文件
        Files.createDirectories(storageLocation);
        Path testFile = storageLocation.resolve("test.jpg");
        Files.write(testFile, "test content".getBytes());

        // When - imageName 包含查詢參數
        ResponseEntity<?> response = controller.getImage("test.jpg?_=123456");

        // Then - 應該去除查詢參數
        assertEquals(200, response.getStatusCodeValue());

        cleanupTestFiles();
    }

    @Test
    void testGetImage_WithoutQueryParameters() throws IOException {
        controller = new BannersController(service);

        // 創建測試文件
        Files.createDirectories(storageLocation);
        Path testFile = storageLocation.resolve("test.jpg");
        Files.write(testFile, "test content".getBytes());

        // When - imageName 不包含查詢參數
        ResponseEntity<?> response = controller.getImage("test.jpg");

        // Then
        assertEquals(200, response.getStatusCodeValue());

        cleanupTestFiles();
    }

    @Test
    void testGetImage_FileNotFound() throws IOException {
        controller = new BannersController(service);

        // When - 文件不存在
        ResponseEntity<?> response = controller.getImage("nonexistent.jpg");

        // Then
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void testGetImage_WithUUIDPrefix() throws IOException {
        controller = new BannersController(service);

        // 創建測試文件（UUID_原始檔名格式）
        Files.createDirectories(storageLocation);
        String filename = "abc123_test.jpg";
        Path testFile = storageLocation.resolve(filename);
        Files.write(testFile, "test content".getBytes());

        // When - 文件名包含下劃線，應提取原始檔名
        ResponseEntity<?> response = controller.getImage(filename);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
        assertTrue(contentDisposition.contains("test.jpg"));

        cleanupTestFiles();
    }

    @Test
    void testGetImage_WithoutUnderscore() throws IOException {
        controller = new BannersController(service);

        // 創建測試文件（沒有下劃線）
        Files.createDirectories(storageLocation);
        String filename = "simpleimage.jpg";
        Path testFile = storageLocation.resolve(filename);
        Files.write(testFile, "test content".getBytes());

        // When - 文件名沒有下劃線
        ResponseEntity<?> response = controller.getImage(filename);

        // Then - displayFilename 應該等於 imageName
        assertEquals(200, response.getStatusCodeValue());
        String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
        assertTrue(contentDisposition.contains(filename));

        cleanupTestFiles();
    }

    @Test
    void testGetImage_UnderscoreAtEnd() throws IOException {
        controller = new BannersController(service);

        // 創建測試文件（下劃線在最後）
        Files.createDirectories(storageLocation);
        String filename = "test_";
        Path testFile = storageLocation.resolve(filename);
        Files.write(testFile, "test content".getBytes());

        // When - 下劃線在最後（underscoreIdx < imageName.length() - 1 為 false）
        ResponseEntity<?> response = controller.getImage(filename);

        // Then - 應使用原始 imageName
        assertEquals(200, response.getStatusCodeValue());

        cleanupTestFiles();
    }

    @Test
    void testGetImage_ContentTypeNull() throws IOException {
        controller = new BannersController(service);

        // 創建沒有副檔名的文件（contentType 會是 null）
        Files.createDirectories(storageLocation);
        String filename = "noextension";
        Path testFile = storageLocation.resolve(filename);
        Files.write(testFile, "test content".getBytes());

        // When - contentType 為 null
        ResponseEntity<?> response = controller.getImage(filename);

        // Then - 應使用 APPLICATION_OCTET_STREAM
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getHeaders().getContentType());

        cleanupTestFiles();
    }

    @Test
    void testGetImage_IOExceptionDuringRead() throws IOException {
        controller = new BannersController(service);

        // 創建目錄而不是文件來觸發 IOException
        Files.createDirectories(storageLocation);
        String filename = "directory-test";
        Path testDir = storageLocation.resolve(filename);
        Files.createDirectory(testDir);

        // When - 嘗試讀取目錄會拋出 IOException
        ResponseEntity<?> response = controller.getImage(filename);

        // Then
        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Failed to read image"));

        // 清理
        Files.delete(testDir);
    }

    // ===========================================================================================
    // 輔助方法
    // ===========================================================================================

    private Banners createValidMeta() {
        Banners meta = new Banners();
        meta.setSortOrder(1);
        meta.setStartTime(LocalDateTime.now());
        meta.setEndTime(LocalDateTime.now().plusDays(30));
        meta.setStatus(true);
        return meta;
    }

    private Banners createValidBanner() {
        Banners banner = new Banners();
        banner.setSortOrder(1);
        banner.setImageName("test.jpg");
        banner.setLinkUrl("https://example.com");
        banner.setStatus(true);
        banner.setStartTime(LocalDateTime.now());
        banner.setEndTime(LocalDateTime.now().plusDays(30));
        return banner;
    }

    private void cleanupTestFiles() {
        try {
            if (Files.exists(storageLocation)) {
                Files.walk(storageLocation)
                    .filter(Files::isRegularFile)
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

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }
}


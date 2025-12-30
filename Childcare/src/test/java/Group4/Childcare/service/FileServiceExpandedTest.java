package Group4.Childcare.service;

import Group4.Childcare.Service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FileServiceExpandedTest {

    @InjectMocks
    private FileService fileService;

    @TempDir
    Path tempDir;

    private UUID testApplicationId;
    private UUID testInstitutionId;

    @BeforeEach
    void setUp() {
        testApplicationId = UUID.randomUUID();
        testInstitutionId = UUID.randomUUID();

        // Set upload directory to temp directory
        ReflectionTestUtils.setField(fileService, "uploadDir", tempDir.toString());
    }

    // ========== getFilesByApplicationId Tests ==========

    @Test
    void testGetFilesByApplicationId_FolderExists_WithFiles() throws IOException {
        // Create test folder and files
        Path appFolder = tempDir.resolve(testApplicationId.toString());
        Files.createDirectories(appFolder);
        Files.createFile(appFolder.resolve("file1.pdf"));
        Files.createFile(appFolder.resolve("file2.jpg"));
        Files.createFile(appFolder.resolve("file3.doc"));

        List<String> files = fileService.getFilesByApplicationId(testApplicationId);

        assertNotNull(files);
        assertEquals(3, files.size());
        assertTrue(files.contains("file1.pdf"));
        assertTrue(files.contains("file2.jpg"));
        assertTrue(files.contains("file3.doc"));
    }

    @Test
    void testGetFilesByApplicationId_FolderNotExists() {
        List<String> files = fileService.getFilesByApplicationId(testApplicationId);

        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    void testGetFilesByApplicationId_FolderEmpty() throws IOException {
        Path appFolder = tempDir.resolve(testApplicationId.toString());
        Files.createDirectories(appFolder);

        List<String> files = fileService.getFilesByApplicationId(testApplicationId);

        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    void testGetFilesByApplicationId_PathIsFile() throws IOException {
        // Create a file instead of directory
        Files.createFile(tempDir.resolve(testApplicationId.toString()));

        List<String> files = fileService.getFilesByApplicationId(testApplicationId);

        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    void testGetFilesByApplicationId_WithSubdirectories() throws IOException {
        Path appFolder = tempDir.resolve(testApplicationId.toString());
        Files.createDirectories(appFolder);
        Files.createFile(appFolder.resolve("file1.pdf"));
        Files.createDirectories(appFolder.resolve("subfolder"));

        List<String> files = fileService.getFilesByApplicationId(testApplicationId);

        assertNotNull(files);
        assertEquals(1, files.size());
        assertEquals("file1.pdf", files.get(0));
    }

    // ========== folderExists Tests ==========

    @Test
    void testFolderExists_True() throws IOException {
        Path appFolder = tempDir.resolve(testApplicationId.toString());
        Files.createDirectories(appFolder);

        boolean exists = fileService.folderExists(testApplicationId);

        assertTrue(exists);
    }

    @Test
    void testFolderExists_False() {
        boolean exists = fileService.folderExists(testApplicationId);

        assertFalse(exists);
    }

    @Test
    void testFolderExists_PathIsFile() throws IOException {
        Files.createFile(tempDir.resolve(testApplicationId.toString()));

        boolean exists = fileService.folderExists(testApplicationId);

        assertFalse(exists);
    }

    // ========== createFolder Tests ==========

    @Test
    void testCreateFolder_Success() {
        boolean result = fileService.createFolder(testApplicationId);

        assertTrue(result);
        assertTrue(Files.exists(tempDir.resolve(testApplicationId.toString())));
        assertTrue(Files.isDirectory(tempDir.resolve(testApplicationId.toString())));
    }

    @Test
    void testCreateFolder_AlreadyExists() throws IOException {
        Path appFolder = tempDir.resolve(testApplicationId.toString());
        Files.createDirectories(appFolder);

        boolean result = fileService.createFolder(testApplicationId);

        assertTrue(result);
        assertTrue(Files.exists(appFolder));
    }

    // ========== getFolderPath Tests ==========

    @Test
    void testGetFolderPath_ReturnsUploadDir() {
        Path result = fileService.getFolderPath(testApplicationId);

        assertNotNull(result);
        assertEquals(tempDir, result);
    }

    // ========== saveInstitutionImage Tests ==========

    @Test
    void testSaveInstitutionImage_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes());

        String result = fileService.saveInstitutionImage(file, testInstitutionId);

        assertNotNull(result);
        assertTrue(result.startsWith("/InstitutionResource/"));
        assertTrue(result.contains(testInstitutionId.toString()));
        assertTrue(result.endsWith("test.jpg"));
    }

    @Test
    void testSaveInstitutionImage_EmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> {
            fileService.saveInstitutionImage(file, testInstitutionId);
        });
    }

    @Test
    void testSaveInstitutionImage_InvalidFileType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes());

        assertThrows(IllegalArgumentException.class, () -> {
            fileService.saveInstitutionImage(file, testInstitutionId);
        });
    }

    @Test
    void testSaveInstitutionImage_NullContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                null,
                "test content".getBytes());

        assertThrows(IllegalArgumentException.class, () -> {
            fileService.saveInstitutionImage(file, testInstitutionId);
        });
    }

    @Test
    void testSaveInstitutionImage_ReplacesOldImage() throws IOException {
        // Upload first image
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "old.jpg",
                "image/jpeg",
                "old image".getBytes());
        fileService.saveInstitutionImage(file1, testInstitutionId);

        // Upload second image (should replace first)
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "new.jpg",
                "image/jpeg",
                "new image".getBytes());
        String result = fileService.saveInstitutionImage(file2, testInstitutionId);

        assertNotNull(result);
        assertTrue(result.contains("new.jpg"));

        // Verify old file is deleted
        String fileName = fileService.getInstitutionImageFileName(testInstitutionId);
        assertNotNull(fileName);
        assertTrue(fileName.contains("new.jpg"));
        assertFalse(fileName.contains("old.jpg"));
    }

    // ========== deleteInstitutionImage Tests ==========

    @Test
    void testDeleteInstitutionImage_Success() throws IOException {
        // First save an image
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test content".getBytes());
        fileService.saveInstitutionImage(file, testInstitutionId);

        // Then delete it
        fileService.deleteInstitutionImage(testInstitutionId);

        // Verify it's deleted
        String fileName = fileService.getInstitutionImageFileName(testInstitutionId);
        assertNull(fileName);
    }

    @Test
    void testDeleteInstitutionImage_NoImageExists() {
        // Should not throw exception
        assertDoesNotThrow(() -> {
            fileService.deleteInstitutionImage(testInstitutionId);
        });
    }

    @Test
    void testDeleteInstitutionImage_DirectoryNotExists() {
        // Should not throw exception
        assertDoesNotThrow(() -> {
            fileService.deleteInstitutionImage(UUID.randomUUID());
        });
    }

    // ========== getInstitutionImageFileName Tests ==========

    @Test
    void testGetInstitutionImageFileName_ImageExists() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test content".getBytes());
        fileService.saveInstitutionImage(file, testInstitutionId);

        String fileName = fileService.getInstitutionImageFileName(testInstitutionId);

        assertNotNull(fileName);
        assertTrue(fileName.startsWith(testInstitutionId.toString()));
        assertTrue(fileName.endsWith("test.jpg"));
    }

    @Test
    void testGetInstitutionImageFileName_NoImageExists() {
        String fileName = fileService.getInstitutionImageFileName(testInstitutionId);

        assertNull(fileName);
    }

    @Test
    void testGetInstitutionImageFileName_DirectoryNotExists() {
        String fileName = fileService.getInstitutionImageFileName(UUID.randomUUID());

        assertNull(fileName);
    }

    // ========== Edge Cases ==========

    @Test
    void testGetFilesByApplicationId_SortedOrder() throws IOException {
        Path appFolder = tempDir.resolve(testApplicationId.toString());
        Files.createDirectories(appFolder);
        Files.createFile(appFolder.resolve("c.txt"));
        Files.createFile(appFolder.resolve("a.txt"));
        Files.createFile(appFolder.resolve("b.txt"));

        List<String> files = fileService.getFilesByApplicationId(testApplicationId);

        assertEquals(3, files.size());
        assertEquals("a.txt", files.get(0));
        assertEquals("b.txt", files.get(1));
        assertEquals("c.txt", files.get(2));
    }

    @Test
    void testSaveInstitutionImage_DifferentImageTypes() throws IOException {
        String[] contentTypes = { "image/jpeg", "image/png", "image/gif", "image/webp" };

        for (String contentType : contentTypes) {
            UUID institutionId = UUID.randomUUID();
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test." + contentType.split("/")[1],
                    contentType,
                    "test content".getBytes());

            String result = fileService.saveInstitutionImage(file, institutionId);

            assertNotNull(result);
            assertTrue(result.startsWith("/InstitutionResource/"));
        }
    }
}

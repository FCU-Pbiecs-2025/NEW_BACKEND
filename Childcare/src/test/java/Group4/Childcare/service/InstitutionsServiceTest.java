package Group4.Childcare.service;

import Group4.Childcare.DTO.InstitutionOffsetDTO;
import Group4.Childcare.DTO.InstitutionSimpleDTO;
import Group4.Childcare.DTO.InstitutionSummaryDTO;
import Group4.Childcare.Model.Institutions;
import Group4.Childcare.Repository.InstitutionsJdbcRepository;
import Group4.Childcare.Service.FileService;
import Group4.Childcare.Service.InstitutionsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * InstitutionsService 單元測試
 *
 * 測試範圍：
 * 1. create() - 創建機構
 * 2. getById() - 根據ID查詢機構
 * 3. getAll() - 查詢所有機構
 * 4. update() - 更新機構
 * 5. updateWithImage() - 更新機構含圖片
 * 6. getSummaryAll() - 查詢所有機構摘要
 * 7. getAllSimple() - 查詢所有機構簡要資訊
 * 8. getOffset() - 分頁查詢機構
 */
@ExtendWith(MockitoExtension.class)
class InstitutionsServiceTest {

    @Mock
    private InstitutionsJdbcRepository repository;

    @Mock
    private FileService fileService;

    @InjectMocks
    private InstitutionsService service;

    private Institutions testInstitution;
    private UUID testInstitutionId;

    @BeforeEach
    void setUp() {
        testInstitutionId = UUID.randomUUID();
        testInstitution = new Institutions();
        testInstitution.setInstitutionID(testInstitutionId);
        testInstitution.setInstitutionName("測試托育機構");
        testInstitution.setAddress("台北市測試路100號");
        testInstitution.setPhoneNumber("02-12345678");
        testInstitution.setEmail("test@institution.com");
        testInstitution.setContactPerson("測試聯絡人");
        testInstitution.setImagePath("/images/institution1.jpg");
    }

    @Test
    void testCreate_Success() {
        // Given
        when(repository.save(any(Institutions.class))).thenReturn(testInstitution);

        // When
        Institutions result = service.create(testInstitution);

        // Then
        assertNotNull(result);
        assertEquals(testInstitutionId, result.getInstitutionID());
        assertEquals("測試托育機構", result.getInstitutionName());
        assertEquals("台北市測試路100號", result.getAddress());
        verify(repository, times(1)).save(testInstitution);
    }

    @Test
    void testGetById_Success() {
        // Given
        when(repository.findById(testInstitutionId)).thenReturn(Optional.of(testInstitution));

        // When
        Optional<Institutions> result = service.getById(testInstitutionId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testInstitutionId, result.get().getInstitutionID());
        assertEquals("測試托育機構", result.get().getInstitutionName());
        verify(repository, times(1)).findById(testInstitutionId);
    }

    @Test
    void testGetById_NotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When
        Optional<Institutions> result = service.getById(nonExistentId);

        // Then
        assertFalse(result.isPresent());
        verify(repository, times(1)).findById(nonExistentId);
    }

    @Test
    void testGetAll_Success() {
        // Given
        Institutions anotherInstitution = new Institutions();
        anotherInstitution.setInstitutionID(UUID.randomUUID());
        anotherInstitution.setInstitutionName("另一個機構");
        List<Institutions> institutionList = Arrays.asList(testInstitution, anotherInstitution);
        when(repository.findAll()).thenReturn(institutionList);

        // When
        List<Institutions> result = service.getAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    void testUpdate_Success() {
        // Given
        testInstitution.setInstitutionName("更新後的機構名稱");
        testInstitution.setEmail("newemail@institution.com");
        when(repository.save(any(Institutions.class))).thenReturn(testInstitution);

        // When
        Institutions result = service.update(testInstitutionId, testInstitution);

        // Then
        assertNotNull(result);
        assertEquals(testInstitutionId, result.getInstitutionID());
        assertEquals("更新後的機構名稱", result.getInstitutionName());
        assertEquals("newemail@institution.com", result.getEmail());
        verify(repository, times(1)).save(testInstitution);
    }

    @Test
    void testUpdateWithImage_Success() throws IOException {
        // Given
        MultipartFile imageFile = mock(MultipartFile.class);
        when(imageFile.isEmpty()).thenReturn(false);
        String newImagePath = "/images/new_institution.jpg";
        when(fileService.saveInstitutionImage(imageFile, testInstitutionId)).thenReturn(newImagePath);
        testInstitution.setImagePath(newImagePath);
        when(repository.save(any(Institutions.class))).thenReturn(testInstitution);

        // When
        Institutions result = service.updateWithImage(testInstitutionId, testInstitution, imageFile);

        // Then
        assertNotNull(result);
        assertEquals(newImagePath, result.getImagePath());
        verify(fileService, times(1)).saveInstitutionImage(imageFile, testInstitutionId);
        verify(repository, times(1)).save(any(Institutions.class));
    }

    @Test
    void testUpdateWithImage_NoImage() throws IOException {
        // Given
        when(repository.save(any(Institutions.class))).thenReturn(testInstitution);

        // When
        Institutions result = service.updateWithImage(testInstitutionId, testInstitution, null);

        // Then
        assertNotNull(result);
        verify(fileService, never()).saveInstitutionImage(any(), any());
        verify(repository, times(1)).save(testInstitution);
    }

    @Test
    void testUpdateWithImage_EmptyImage() throws IOException {
        // Given
        MultipartFile imageFile = mock(MultipartFile.class);
        when(imageFile.isEmpty()).thenReturn(true);
        when(repository.save(any(Institutions.class))).thenReturn(testInstitution);

        // When
        Institutions result = service.updateWithImage(testInstitutionId, testInstitution, imageFile);

        // Then
        assertNotNull(result);
        verify(fileService, never()).saveInstitutionImage(any(), any());
        verify(repository, times(1)).save(testInstitution);
    }

    @Test
    void testUpdateWithImage_IOException() throws IOException {
        // Given
        MultipartFile imageFile = mock(MultipartFile.class);
        when(imageFile.isEmpty()).thenReturn(false);
        when(fileService.saveInstitutionImage(imageFile, testInstitutionId))
                .thenThrow(new IOException("Failed to save image"));

        // When & Then
        assertThrows(IOException.class, () -> {
            service.updateWithImage(testInstitutionId, testInstitution, imageFile);
        });
        verify(fileService, times(1)).saveInstitutionImage(imageFile, testInstitutionId);
        verify(repository, never()).save(any());
    }

    @Test
    void testGetSummaryAll_Success() {
        // Given
        InstitutionSummaryDTO summaryDTO = new InstitutionSummaryDTO(
                testInstitutionId,
                "測試托育機構",
                "台北市測試路100號",
                "02-12345678"
        );
        List<InstitutionSummaryDTO> summaryList = Arrays.asList(summaryDTO);
        when(repository.findSummaryData()).thenReturn(summaryList);

        // When
        List<InstitutionSummaryDTO> result = service.getSummaryAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("測試托育機構", result.get(0).getInstitutionName());
        verify(repository, times(1)).findSummaryData();
    }

    @Test
    void testGetAllSimple_Success() {
        // Given
        InstitutionSimpleDTO simpleDTO = new InstitutionSimpleDTO(testInstitutionId, "測試托育機構");
        List<InstitutionSimpleDTO> simpleList = Arrays.asList(simpleDTO);
        when(repository.findAllSimple()).thenReturn(simpleList);

        // When
        List<InstitutionSimpleDTO> result = service.getAllSimple();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("測試托育機構", result.get(0).getInstitutionName());
        verify(repository, times(1)).findAllSimple();
    }

    @Test
    void testGetOffset_SuperAdminRole() {
        // Given - super_admin 角色（institutionID 為 null）
        int offset = 0;
        int size = 10;
        long totalElements = 25L;
        List<Institutions> content = Arrays.asList(testInstitution);

        when(repository.findAllWithPagination(offset, size)).thenReturn(content);
        when(repository.count()).thenReturn(totalElements);

        // When
        InstitutionOffsetDTO result = service.getOffset(offset, size, null, null);

        // Then
        assertNotNull(result);
        assertEquals(offset, result.getOffset());
        assertEquals(size, result.getSize());
        assertEquals(3, result.getTotalPages()); // 25 / 10 = 3 pages
        assertEquals(totalElements, result.getTotalElements());
        assertTrue(result.isHasNext());
        verify(repository, times(1)).findAllWithPagination(offset, size);
        verify(repository, times(1)).count();
    }

    @Test
    void testGetOffset_AdminRole() {
        // Given - admin 角色（有 institutionID）
        int offset = 0;
        int size = 10;
        long totalElements = 1L;
        List<Institutions> content = Arrays.asList(testInstitution);

        when(repository.findByInstitutionIDWithPagination(testInstitutionId, offset, size)).thenReturn(content);
        when(repository.countByInstitutionID(testInstitutionId)).thenReturn(totalElements);

        // When
        InstitutionOffsetDTO result = service.getOffset(offset, size, testInstitutionId, null);

        // Then
        assertNotNull(result);
        assertEquals(offset, result.getOffset());
        assertEquals(size, result.getSize());
        assertEquals(1, result.getTotalPages());
        assertEquals(totalElements, result.getTotalElements());
        assertFalse(result.isHasNext());
        verify(repository, times(1)).findByInstitutionIDWithPagination(testInstitutionId, offset, size);
        verify(repository, times(1)).countByInstitutionID(testInstitutionId);
    }

    @Test
    void testGetOffset_InvalidSize() {
        // Given - 無效的 size（<= 0）
        int offset = 0;
        int invalidSize = -5;
        int defaultSize = 10;
        List<Institutions> content = Arrays.asList(testInstitution);

        when(repository.findAllWithPagination(offset, defaultSize)).thenReturn(content);
        when(repository.count()).thenReturn(10L);

        // When
        InstitutionOffsetDTO result = service.getOffset(offset, invalidSize, null, null);

        // Then
        assertNotNull(result);
        assertEquals(defaultSize, result.getSize()); // 應該使用預設值 10
        verify(repository, times(1)).findAllWithPagination(offset, defaultSize);
    }

    @Test
    void testGetOffset_InvalidOffset() {
        // Given - 無效的 offset（< 0）
        int invalidOffset = -10;
        int correctedOffset = 0;
        int size = 10;
        List<Institutions> content = Arrays.asList(testInstitution);

        when(repository.findAllWithPagination(correctedOffset, size)).thenReturn(content);
        when(repository.count()).thenReturn(10L);

        // When
        InstitutionOffsetDTO result = service.getOffset(invalidOffset, size, null, null);

        // Then
        assertNotNull(result);
        assertEquals(correctedOffset, result.getOffset()); // 應該修正為 0
        verify(repository, times(1)).findAllWithPagination(correctedOffset, size);
    }

    @Test
    void testGetOffset_LastPage() {
        // Given - 最後一頁（沒有下一頁）
        int offset = 20;
        int size = 10;
        long totalElements = 25L;
        List<Institutions> content = Arrays.asList(testInstitution);

        when(repository.findAllWithPagination(offset, size)).thenReturn(content);
        when(repository.count()).thenReturn(totalElements);

        // When
        InstitutionOffsetDTO result = service.getOffset(offset, size, null, null);

        // Then
        assertNotNull(result);
        assertFalse(result.isHasNext()); // 沒有下一頁
        verify(repository, times(1)).findAllWithPagination(offset, size);
    }
}


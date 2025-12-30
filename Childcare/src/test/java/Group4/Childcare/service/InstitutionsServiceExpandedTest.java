package Group4.Childcare.service;

import Group4.Childcare.Model.Institutions;
import Group4.Childcare.Repository.InstitutionsJdbcRepository;
import Group4.Childcare.Service.InstitutionsService;
import Group4.Childcare.Service.FileService;
import Group4.Childcare.DTO.InstitutionSummaryDTO;
import Group4.Childcare.DTO.InstitutionSimpleDTO;
import Group4.Childcare.DTO.InstitutionOffsetDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * InstitutionsService 擴展測試
 * 
 * 專注於提高分支覆蓋率的測試用例
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class InstitutionsServiceExpandedTest {

    @Mock
    private InstitutionsJdbcRepository repository;

    @Mock
    private FileService fileService;

    @Mock
    private MultipartFile imageFile;

    @InjectMocks
    private InstitutionsService service;

    private Institutions testInstitution;
    private UUID testInstitutionId;

    @BeforeEach
    void setUp() {
        testInstitutionId = UUID.randomUUID();

        testInstitution = new Institutions();
        testInstitution.setInstitutionID(testInstitutionId);
        testInstitution.setInstitutionName("測試機構");
        testInstitution.setContactPerson("聯絡人");
        testInstitution.setPhoneNumber("02-12345678");
        testInstitution.setAccountStatus(1);

        ReflectionTestUtils.setField(service, "fileService", fileService);
    }

    // ========== create Tests ==========

    @Test
    void testCreate_Success() {
        when(repository.save(any(Institutions.class))).thenReturn(testInstitution);

        Institutions result = service.create(testInstitution);

        assertNotNull(result);
        assertEquals("測試機構", result.getInstitutionName());
        verify(repository).save(testInstitution);
    }

    // ========== createWithImage Tests ==========

    @Test
    void testCreateWithImage_WithImage() throws IOException {
        when(repository.save(any(Institutions.class))).thenReturn(testInstitution);
        when(imageFile.isEmpty()).thenReturn(false);
        when(fileService.saveInstitutionImage(imageFile, testInstitutionId)).thenReturn("/images/test.jpg");

        Institutions result = service.createWithImage(testInstitution, imageFile);

        assertNotNull(result);
        verify(repository, times(2)).save(any(Institutions.class));
        verify(fileService).saveInstitutionImage(imageFile, testInstitutionId);
    }

    @Test
    void testCreateWithImage_WithoutImage() throws IOException {
        when(repository.save(any(Institutions.class))).thenReturn(testInstitution);
        when(imageFile.isEmpty()).thenReturn(true);

        Institutions result = service.createWithImage(testInstitution, imageFile);

        assertNotNull(result);
        verify(repository, times(1)).save(testInstitution);
        verify(fileService, never()).saveInstitutionImage(any(), any());
    }

    @Test
    void testCreateWithImage_NullImage() throws IOException {
        when(repository.save(any(Institutions.class))).thenReturn(testInstitution);

        Institutions result = service.createWithImage(testInstitution, null);

        assertNotNull(result);
        verify(repository, times(1)).save(testInstitution);
        verify(fileService, never()).saveInstitutionImage(any(), any());
    }

    // ========== getById Tests ==========

    @Test
    void testGetById_Found() {
        when(repository.findById(testInstitutionId)).thenReturn(Optional.of(testInstitution));

        Optional<Institutions> result = service.getById(testInstitutionId);

        assertTrue(result.isPresent());
        assertEquals("測試機構", result.get().getInstitutionName());
        verify(repository).findById(testInstitutionId);
    }

    @Test
    void testGetById_NotFound() {
        when(repository.findById(testInstitutionId)).thenReturn(Optional.empty());

        Optional<Institutions> result = service.getById(testInstitutionId);

        assertFalse(result.isPresent());
        verify(repository).findById(testInstitutionId);
    }

    // ========== getAll Tests ==========

    @Test
    void testGetAll_Success() {
        List<Institutions> institutions = Arrays.asList(testInstitution);
        when(repository.findAll()).thenReturn(institutions);

        List<Institutions> result = service.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findAll();
    }

    // ========== getAllActive Tests ==========

    @Test
    void testGetAllActive_Success() {
        List<Institutions> activeInstitutions = Arrays.asList(testInstitution);
        when(repository.findAllActive()).thenReturn(activeInstitutions);

        List<Institutions> result = service.getAllActive();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findAllActive();
    }

    @Test
    void testGetAllActive_Empty() {
        when(repository.findAllActive()).thenReturn(Collections.emptyList());

        List<Institutions> result = service.getAllActive();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository).findAllActive();
    }

    // ========== update Tests ==========

    @Test
    void testUpdate_Success() {
        testInstitution.setInstitutionName("更新後的機構");
        when(repository.save(any(Institutions.class))).thenReturn(testInstitution);

        Institutions result = service.update(testInstitutionId, testInstitution);

        assertNotNull(result);
        assertEquals(testInstitutionId, result.getInstitutionID());
        assertEquals("更新後的機構", result.getInstitutionName());
        verify(repository).save(testInstitution);
    }

    // ========== updateWithImage Tests ==========

    @Test
    void testUpdateWithImage_WithNewImage() throws IOException {
        when(imageFile.isEmpty()).thenReturn(false);
        when(fileService.saveInstitutionImage(imageFile, testInstitutionId)).thenReturn("/images/updated.jpg");
        when(repository.save(any(Institutions.class))).thenReturn(testInstitution);

        Institutions result = service.updateWithImage(testInstitutionId, testInstitution, imageFile);

        assertNotNull(result);
        verify(fileService).saveInstitutionImage(imageFile, testInstitutionId);
        verify(repository).save(testInstitution);
    }

    @Test
    void testUpdateWithImage_WithoutNewImage() throws IOException {
        when(imageFile.isEmpty()).thenReturn(true);
        when(repository.save(any(Institutions.class))).thenReturn(testInstitution);

        Institutions result = service.updateWithImage(testInstitutionId, testInstitution, imageFile);

        assertNotNull(result);
        verify(fileService, never()).saveInstitutionImage(any(), any());
        verify(repository).save(testInstitution);
    }

    // ========== getSummaryAll Tests ==========

    @Test
    void testGetSummaryAll_Success() {
        List<InstitutionSummaryDTO> summaries = new ArrayList<>();
        when(repository.findSummaryData()).thenReturn(summaries);

        List<InstitutionSummaryDTO> result = service.getSummaryAll();

        assertNotNull(result);
        verify(repository).findSummaryData();
    }

    // ========== getAllSimple Tests ==========

    @Test
    void testGetAllSimple_Success() {
        List<InstitutionSimpleDTO> simpleList = new ArrayList<>();
        when(repository.findAllSimple()).thenReturn(simpleList);

        List<InstitutionSimpleDTO> result = service.getAllSimple();

        assertNotNull(result);
        verify(repository).findAllSimple();
    }

    // ========== getOffset Tests ==========

    @Test
    void testGetOffset_SuperAdmin_NoSearch() {
        List<Institutions> institutions = Arrays.asList(testInstitution);
        when(repository.findAllWithPagination(0, 10)).thenReturn(institutions);
        when(repository.count()).thenReturn(1L);

        InstitutionOffsetDTO result = service.getOffset(0, 10, null, null);

        assertNotNull(result);
        assertEquals(0, result.getOffset());
        assertEquals(10, result.getSize());
        assertEquals(1L, result.getTotalElements());
        verify(repository).findAllWithPagination(0, 10);
        verify(repository).count();
    }

    @Test
    void testGetOffset_SuperAdmin_WithSearch() {
        List<Institutions> institutions = Arrays.asList(testInstitution);
        when(repository.findAllWithSearchAndPagination("測試", 0, 10)).thenReturn(institutions);
        when(repository.countAllWithSearch("測試")).thenReturn(1L);

        InstitutionOffsetDTO result = service.getOffset(0, 10, null, "測試");

        assertNotNull(result);
        assertEquals(1L, result.getTotalElements());
        verify(repository).findAllWithSearchAndPagination("測試", 0, 10);
        verify(repository).countAllWithSearch("測試");
    }

    @Test
    void testGetOffset_Admin_NoSearch() {
        List<Institutions> institutions = Arrays.asList(testInstitution);
        when(repository.findByInstitutionIDWithPagination(testInstitutionId, 0, 10)).thenReturn(institutions);
        when(repository.countByInstitutionID(testInstitutionId)).thenReturn(1L);

        InstitutionOffsetDTO result = service.getOffset(0, 10, testInstitutionId, null);

        assertNotNull(result);
        assertEquals(1L, result.getTotalElements());
        verify(repository).findByInstitutionIDWithPagination(testInstitutionId, 0, 10);
        verify(repository).countByInstitutionID(testInstitutionId);
    }

    @Test
    void testGetOffset_Admin_WithSearch() {
        List<Institutions> institutions = Arrays.asList(testInstitution);
        when(repository.findByInstitutionIDWithSearchAndPagination(testInstitutionId, "測試", 0, 10))
                .thenReturn(institutions);
        when(repository.countByInstitutionIDWithSearch(testInstitutionId, "測試")).thenReturn(1L);

        InstitutionOffsetDTO result = service.getOffset(0, 10, testInstitutionId, "測試");

        assertNotNull(result);
        assertEquals(1L, result.getTotalElements());
        verify(repository).findByInstitutionIDWithSearchAndPagination(testInstitutionId, "測試", 0, 10);
        verify(repository).countByInstitutionIDWithSearch(testInstitutionId, "測試");
    }

    @Test
    void testGetOffset_InvalidSize() {
        List<Institutions> institutions = Arrays.asList(testInstitution);
        when(repository.findAllWithPagination(0, 10)).thenReturn(institutions);
        when(repository.count()).thenReturn(1L);

        InstitutionOffsetDTO result = service.getOffset(0, -5, null, null);

        assertNotNull(result);
        assertEquals(10, result.getSize()); // Should default to 10
        verify(repository).findAllWithPagination(0, 10);
    }

    @Test
    void testGetOffset_InvalidOffset() {
        List<Institutions> institutions = Arrays.asList(testInstitution);
        when(repository.findAllWithPagination(0, 10)).thenReturn(institutions);
        when(repository.count()).thenReturn(1L);

        InstitutionOffsetDTO result = service.getOffset(-10, 10, null, null);

        assertNotNull(result);
        assertEquals(0, result.getOffset()); // Should default to 0
        verify(repository).findAllWithPagination(0, 10);
    }

    // ========== getOffsetByName Tests ==========

    @Test
    void testGetOffsetByName_SuperAdmin_NoSearch() {
        List<Institutions> institutions = Arrays.asList(testInstitution);
        when(repository.findAllWithPagination(0, 10)).thenReturn(institutions);
        when(repository.count()).thenReturn(1L);

        InstitutionOffsetDTO result = service.getOffsetByName(0, 10, null, null);

        assertNotNull(result);
        assertEquals(1L, result.getTotalElements());
        verify(repository).findAllWithPagination(0, 10);
        verify(repository).count();
    }

    @Test
    void testGetOffsetByName_SuperAdmin_WithName() {
        List<Institutions> institutions = Arrays.asList(testInstitution);
        when(repository.findAllWithNameSearchAndPagination("測試", 0, 10)).thenReturn(institutions);
        when(repository.countAllWithNameSearch("測試")).thenReturn(1L);

        InstitutionOffsetDTO result = service.getOffsetByName(0, 10, null, "測試");

        assertNotNull(result);
        assertEquals(1L, result.getTotalElements());
        verify(repository).findAllWithNameSearchAndPagination("測試", 0, 10);
        verify(repository).countAllWithNameSearch("測試");
    }

    @Test
    void testGetOffsetByName_Admin_NoSearch() {
        List<Institutions> institutions = Arrays.asList(testInstitution);
        when(repository.findByInstitutionIDWithPagination(testInstitutionId, 0, 10)).thenReturn(institutions);
        when(repository.countByInstitutionID(testInstitutionId)).thenReturn(1L);

        InstitutionOffsetDTO result = service.getOffsetByName(0, 10, testInstitutionId, null);

        assertNotNull(result);
        assertEquals(1L, result.getTotalElements());
        verify(repository).findByInstitutionIDWithPagination(testInstitutionId, 0, 10);
        verify(repository).countByInstitutionID(testInstitutionId);
    }

    @Test
    void testGetOffsetByName_Admin_WithName() {
        List<Institutions> institutions = Arrays.asList(testInstitution);
        when(repository.findByInstitutionIDWithNameSearchAndPagination(testInstitutionId, "測試", 0, 10))
                .thenReturn(institutions);
        when(repository.countByInstitutionIDWithNameSearch(testInstitutionId, "測試")).thenReturn(1L);

        InstitutionOffsetDTO result = service.getOffsetByName(0, 10, testInstitutionId, "測試");

        assertNotNull(result);
        assertEquals(1L, result.getTotalElements());
        verify(repository).findByInstitutionIDWithNameSearchAndPagination(testInstitutionId, "測試", 0, 10);
        verify(repository).countByInstitutionIDWithNameSearch(testInstitutionId, "測試");
    }

    @Test
    void testGetOffsetByName_WithWhitespaceSearch() {
        List<Institutions> institutions = Arrays.asList(testInstitution);
        when(repository.findAllWithNameSearchAndPagination("測試", 0, 10)).thenReturn(institutions);
        when(repository.countAllWithNameSearch("測試")).thenReturn(1L);

        InstitutionOffsetDTO result = service.getOffsetByName(0, 10, null, "  測試  ");

        assertNotNull(result);
        verify(repository).findAllWithNameSearchAndPagination("測試", 0, 10);
        verify(repository).countAllWithNameSearch("測試");
    }
}

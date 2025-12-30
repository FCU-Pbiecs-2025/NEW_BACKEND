package Group4.Childcare.service;

import Group4.Childcare.Model.Classes;
import Group4.Childcare.Repository.ClassesJdbcRepository;
import Group4.Childcare.Service.ClassesService;
import Group4.Childcare.DTO.ClassSummaryDTO;
import Group4.Childcare.DTO.ClassNameDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ClassesService 擴展測試
 * 
 * 專注於提高分支覆蓋率的測試用例
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ClassesServiceExpandedTest {

    @Mock
    private ClassesJdbcRepository repository;

    @InjectMocks
    private ClassesService service;

    private Classes testClass;
    private UUID testClassId;
    private UUID testInstitutionId;

    @BeforeEach
    void setUp() {
        testClassId = UUID.randomUUID();
        testInstitutionId = UUID.randomUUID();

        testClass = new Classes();
        testClass.setClassID(testClassId);
        testClass.setInstitutionID(testInstitutionId);
        testClass.setClassName("測試班級");
        testClass.setCapacity(30);
        testClass.setCurrentStudents(20);
    }

    // ========== create Tests ==========

    @Test
    void testCreate_Success() {
        when(repository.save(any(Classes.class))).thenReturn(testClass);

        Classes result = service.create(testClass);

        assertNotNull(result);
        assertEquals("測試班級", result.getClassName());
        verify(repository).save(testClass);
    }

    // ========== getById Tests ==========

    @Test
    void testGetById_Found() {
        when(repository.findById(testClassId)).thenReturn(Optional.of(testClass));

        Optional<Classes> result = service.getById(testClassId);

        assertTrue(result.isPresent());
        assertEquals("測試班級", result.get().getClassName());
        verify(repository).findById(testClassId);
    }

    @Test
    void testGetById_NotFound() {
        when(repository.findById(testClassId)).thenReturn(Optional.empty());

        Optional<Classes> result = service.getById(testClassId);

        assertFalse(result.isPresent());
        verify(repository).findById(testClassId);
    }

    // ========== getAll Tests ==========

    @Test
    void testGetAll_Success() {
        List<Classes> classes = Arrays.asList(testClass);
        when(repository.findAll()).thenReturn(classes);

        List<Classes> result = service.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findAll();
    }

    @Test
    void testGetAll_Empty() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<Classes> result = service.getAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository).findAll();
    }

    // ========== getAllWithInstitutionName Tests ==========

    @Test
    void testGetAllWithInstitutionName_Success() {
        List<ClassSummaryDTO> summaries = new ArrayList<>();
        when(repository.findAllWithInstitutionName()).thenReturn(summaries);

        List<ClassSummaryDTO> result = service.getAllWithInstitutionName();

        assertNotNull(result);
        verify(repository).findAllWithInstitutionName();
    }

    // ========== update Tests ==========

    @Test
    void testUpdate_Success() {
        testClass.setClassName("更新後的班級");
        when(repository.existsById(testClassId)).thenReturn(true);
        when(repository.save(any(Classes.class))).thenReturn(testClass);

        Classes result = service.update(testClassId, testClass);

        assertNotNull(result);
        assertEquals(testClassId, result.getClassID());
        assertEquals("更新後的班級", result.getClassName());
        verify(repository).existsById(testClassId);
        verify(repository).save(testClass);
    }

    @Test
    void testUpdate_NotFound() {
        when(repository.existsById(testClassId)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.update(testClassId, testClass);
        });

        assertTrue(exception.getMessage().contains("班級記錄不存在"));
        verify(repository).existsById(testClassId);
        verify(repository, never()).save(any());
    }

    // ========== delete Tests ==========

    @Test
    void testDelete_Success() {
        doNothing().when(repository).deleteById(testClassId);

        service.delete(testClassId);

        verify(repository).deleteById(testClassId);
    }

    // ========== getByInstitutionId Tests ==========

    @Test
    void testGetByInstitutionId_Success() {
        List<Classes> classes = Arrays.asList(testClass);
        when(repository.findByInstitutionId(testInstitutionId)).thenReturn(classes);

        List<Classes> result = service.getByInstitutionId(testInstitutionId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findByInstitutionId(testInstitutionId);
    }

    @Test
    void testGetByInstitutionId_Empty() {
        when(repository.findByInstitutionId(testInstitutionId)).thenReturn(Collections.emptyList());

        List<Classes> result = service.getByInstitutionId(testInstitutionId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository).findByInstitutionId(testInstitutionId);
    }

    // ========== getClassesWithOffsetJdbc Tests ==========

    @Test
    void testGetClassesWithOffsetJdbc_Success() {
        List<Classes> classes = Arrays.asList(testClass);
        when(repository.findWithOffset(0, 10)).thenReturn(classes);

        List<Classes> result = service.getClassesWithOffsetJdbc(0);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findWithOffset(0, 10);
    }

    // ========== getClassesWithOffsetAndInstitutionNameJdbc Tests ==========

    @Test
    void testGetClassesWithOffsetAndInstitutionNameJdbc_Success() {
        List<ClassSummaryDTO> summaries = new ArrayList<>();
        when(repository.findWithOffsetAndInstitutionName(0, 20)).thenReturn(summaries);

        List<ClassSummaryDTO> result = service.getClassesWithOffsetAndInstitutionNameJdbc(0, 20);

        assertNotNull(result);
        verify(repository).findWithOffsetAndInstitutionName(0, 20);
    }

    // ========== getClassesWithOffsetAndInstitutionNameByInstitutionID Tests
    // ==========

    @Test
    void testGetClassesWithOffsetAndInstitutionNameByInstitutionID_Success() {
        List<ClassSummaryDTO> summaries = new ArrayList<>();
        when(repository.findWithOffsetAndInstitutionNameByInstitutionID(0, 10, testInstitutionId))
                .thenReturn(summaries);

        List<ClassSummaryDTO> result = service.getClassesWithOffsetAndInstitutionNameByInstitutionID(
                0, 10, testInstitutionId);

        assertNotNull(result);
        verify(repository).findWithOffsetAndInstitutionNameByInstitutionID(0, 10, testInstitutionId);
    }

    // ========== getTotalCount Tests ==========

    @Test
    void testGetTotalCount_Success() {
        when(repository.countTotal()).thenReturn(50L);

        long result = service.getTotalCount();

        assertEquals(50L, result);
        verify(repository).countTotal();
    }

    @Test
    void testGetTotalCount_Zero() {
        when(repository.countTotal()).thenReturn(0L);

        long result = service.getTotalCount();

        assertEquals(0L, result);
        verify(repository).countTotal();
    }

    // ========== getTotalCountByInstitutionID Tests ==========

    @Test
    void testGetTotalCountByInstitutionID_Success() {
        when(repository.countByInstitutionID(testInstitutionId)).thenReturn(15L);

        long result = service.getTotalCountByInstitutionID(testInstitutionId);

        assertEquals(15L, result);
        verify(repository).countByInstitutionID(testInstitutionId);
    }

    // ========== searchInstitutionsWithClassesByName Tests ==========

    @Test
    void testSearchInstitutionsWithClassesByName_Success() {
        List<Map<String, Object>> rawResults = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        Map<String, Object> institution = new HashMap<>();
        institution.put("institutionID", testInstitutionId.toString());
        institution.put("institutionName", "測試機構");
        row.put("institution", institution);

        Map<String, Object> classData = new HashMap<>();
        classData.put("classID", testClassId.toString());
        classData.put("className", "測試班級");
        row.put("class", classData);

        rawResults.add(row);
        when(repository.findInstitutionsWithClassesByName("測試")).thenReturn(rawResults);

        List<Map<String, Object>> result = service.searchInstitutionsWithClassesByName("測試");

        assertNotNull(result);
        verify(repository).findInstitutionsWithClassesByName("測試");
    }

    // ========== searchClassesByInstitutionName Tests ==========

    @Test
    void testSearchClassesByInstitutionName_Success() {
        List<ClassSummaryDTO> summaries = new ArrayList<>();
        when(repository.findClassesByInstitutionName("測試機構")).thenReturn(summaries);

        List<ClassSummaryDTO> result = service.searchClassesByInstitutionName("測試機構");

        assertNotNull(result);
        verify(repository).findClassesByInstitutionName("測試機構");
    }

    // ========== getClassNamesByInstitutionId Tests ==========

    @Test
    void testGetClassNamesByInstitutionId_Success() {
        List<ClassNameDTO> classNames = new ArrayList<>();
        when(repository.findClassNamesByInstitutionId(testInstitutionId)).thenReturn(classNames);

        List<ClassNameDTO> result = service.getClassNamesByInstitutionId(testInstitutionId);

        assertNotNull(result);
        verify(repository).findClassNamesByInstitutionId(testInstitutionId);
    }

    // ========== incrementCurrentStudents Tests ==========

    @Test
    void testIncrementCurrentStudents_Success() {
        when(repository.isClassFull(testClassId)).thenReturn(false);
        when(repository.incrementCurrentStudents(testClassId)).thenReturn(true);

        boolean result = service.incrementCurrentStudents(testClassId);

        assertTrue(result);
        verify(repository).isClassFull(testClassId);
        verify(repository).incrementCurrentStudents(testClassId);
    }

    @Test
    void testIncrementCurrentStudents_ClassFull() {
        when(repository.isClassFull(testClassId)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.incrementCurrentStudents(testClassId);
        });

        assertTrue(exception.getMessage().contains("班級已滿"));
        verify(repository).isClassFull(testClassId);
        verify(repository, never()).incrementCurrentStudents(any());
    }

    // ========== decrementCurrentStudents Tests ==========

    @Test
    void testDecrementCurrentStudents_Success() {
        when(repository.decrementCurrentStudents(testClassId)).thenReturn(true);

        boolean result = service.decrementCurrentStudents(testClassId);

        assertTrue(result);
        verify(repository).decrementCurrentStudents(testClassId);
    }

    @Test
    void testDecrementCurrentStudents_Failed() {
        when(repository.decrementCurrentStudents(testClassId)).thenReturn(false);

        boolean result = service.decrementCurrentStudents(testClassId);

        assertFalse(result);
        verify(repository).decrementCurrentStudents(testClassId);
    }

    // ========== isClassFull Tests ==========

    @Test
    void testIsClassFull_True() {
        when(repository.isClassFull(testClassId)).thenReturn(true);

        boolean result = service.isClassFull(testClassId);

        assertTrue(result);
        verify(repository).isClassFull(testClassId);
    }

    @Test
    void testIsClassFull_False() {
        when(repository.isClassFull(testClassId)).thenReturn(false);

        boolean result = service.isClassFull(testClassId);

        assertFalse(result);
        verify(repository).isClassFull(testClassId);
    }
}

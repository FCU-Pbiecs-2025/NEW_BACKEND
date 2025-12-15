package Group4.Childcare.repository;

import Group4.Childcare.Model.Classes;
import Group4.Childcare.Repository.ClassesJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ClassesJdbcRepository 單元測試
 * 測試班級管理相關的資料庫操作
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ClassesJdbcRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ClassesJdbcRepository classesRepository;

    private UUID testClassId;
    private UUID testInstitutionId;

    @BeforeEach
    void setUp() {
        testClassId = UUID.randomUUID();
        testInstitutionId = UUID.randomUUID();
    }

    // ==================== save Tests ====================

    @Test
    void testSave_NewClass_WithNullId_Success() {
        // Given
        Classes newClass = createTestClass();
        newClass.setClassID(null);

        when(jdbcTemplate.update(anyString(), any(), anyString(), anyInt(), anyInt(),
                anyInt(), anyInt(), anyString(), any()))
            .thenReturn(1);

        // When
        Classes result = classesRepository.save(newClass);

        // Then
        assertNotNull(result);
        assertNotNull(result.getClassID());
        verify(jdbcTemplate, times(1)).update(anyString(), any(), anyString(), anyInt(),
                anyInt(), anyInt(), anyInt(), anyString(), any());
    }

    @Test
    void testSave_ExistingClass_Update_Success() {
        // Given
        Classes existingClass = createTestClass();
        existingClass.setClassID(testClassId);

        // Mock existsById check - returns true (1)
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
            .thenReturn(1);

        // Mock update operation
        when(jdbcTemplate.update(anyString(), anyString(), anyInt(), anyInt(),
                anyInt(), anyInt(), anyString(), any(), any()))
            .thenReturn(1);

        // When
        Classes result = classesRepository.save(existingClass);

        // Then
        assertNotNull(result);
        assertEquals(testClassId, result.getClassID());
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(Integer.class), anyString());
        verify(jdbcTemplate, times(1)).update(anyString(), anyString(), anyInt(), anyInt(),
                anyInt(), anyInt(), anyString(), any(), any());
    }

    // ==================== findById Tests ====================

    @Test
    void testFindById_Success() {
        // Given
        Classes mockClass = createTestClass();
        mockClass.setClassID(testClassId);

        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(testClassId.toString())))
            .thenReturn(mockClass);

        // When
        Optional<Classes> result = classesRepository.findById(testClassId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testClassId, result.get().getClassID());
        assertEquals("幼幼班", result.get().getClassName());
    }

    @Test
    void testFindById_NotFound() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
            .thenThrow(new RuntimeException("Not found"));

        // When
        Optional<Classes> result = classesRepository.findById(testClassId);

        // Then
        assertFalse(result.isPresent());
    }

    // ==================== findAll Tests ====================

    @Test
    void testFindAll_Success() {
        // Given
        Classes class1 = createTestClass();
        class1.setClassID(UUID.randomUUID());
        class1.setClassName("幼幼班");

        Classes class2 = createTestClass();
        class2.setClassID(UUID.randomUUID());
        class2.setClassName("小班");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(Arrays.asList(class1, class2));

        // When
        List<Classes> result = classesRepository.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("幼幼班", result.get(0).getClassName());
        assertEquals("小班", result.get(1).getClassName());
    }

    @Test
    void testFindAll_EmptyResult() {
        // Given
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(Collections.emptyList());

        // When
        List<Classes> result = classesRepository.findAll();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== findByInstitutionId Tests ====================

    @Test
    void testFindByInstitutionId_Success() {
        // Given
        Classes mockClass = createTestClass();
        mockClass.setInstitutionID(testInstitutionId);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(testInstitutionId.toString())))
            .thenReturn(Collections.singletonList(mockClass));

        // When
        List<Classes> result = classesRepository.findByInstitutionId(testInstitutionId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testInstitutionId, result.get(0).getInstitutionID());
    }

    @Test
    void testFindByInstitutionId_EmptyResult() {
        // Given
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString()))
            .thenReturn(Collections.emptyList());

        // When
        List<Classes> result = classesRepository.findByInstitutionId(testInstitutionId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== count Tests ====================

    @Test
    void testCount_Success() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(15L);

        // When
        long count = classesRepository.count();

        // Then
        assertEquals(15L, count);
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(Long.class));
    }

    @Test
    void testCount_Zero() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(0L);

        // When
        long count = classesRepository.count();

        // Then
        assertEquals(0L, count);
    }

    // ==================== existsById Tests ====================

    @Test
    void testExistsById_True() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testClassId.toString())))
            .thenReturn(1);

        // When
        boolean exists = classesRepository.existsById(testClassId);

        // Then
        assertTrue(exists);
    }

    @Test
    void testExistsById_False() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testClassId.toString())))
            .thenReturn(0);

        // When
        boolean exists = classesRepository.existsById(testClassId);

        // Then
        assertFalse(exists);
    }

    // ==================== deleteById Tests ====================

    @Test
    void testDeleteById_Success() {
        // Given
        when(jdbcTemplate.update(anyString(), eq(testClassId.toString())))
            .thenReturn(1);

        // When
        classesRepository.deleteById(testClassId);

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), eq(testClassId.toString()));
    }

    @Test
    void testDeleteById_NotFound() {
        // Given
        when(jdbcTemplate.update(anyString(), eq(testClassId.toString())))
            .thenReturn(0);

        // When
        classesRepository.deleteById(testClassId);

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), eq(testClassId.toString()));
    }

    // ==================== Helper Methods ====================

    private Classes createTestClass() {
        Classes classes = new Classes();
        classes.setClassID(testClassId);
        classes.setClassName("幼幼班");
        classes.setCapacity(30);
        classes.setCurrentStudents(15);
        classes.setMinAgeDescription(2);
        classes.setMaxAgeDescription(3);
        classes.setAdditionalInfo("適合2-3歲幼兒");
        classes.setInstitutionID(testInstitutionId);
        return classes;
    }
}


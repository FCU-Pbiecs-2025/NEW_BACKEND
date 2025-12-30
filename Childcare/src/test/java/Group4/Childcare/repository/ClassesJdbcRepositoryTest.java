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

    // ==================== delete (Entity) Tests ====================

    @Test
    void testDelete_Success() {
        // Given
        Classes testClass = createTestClass();
        when(jdbcTemplate.update(anyString(), eq(testClassId.toString())))
                .thenReturn(1);

        // When
        classesRepository.delete(testClass);

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), eq(testClassId.toString()));
    }

    // ==================== findWithOffset Tests ====================

    @Test
    void testFindWithOffset_Success() {
        // Given
        int offset = 0;
        int limit = 10;
        Classes mockClass = createTestClass();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(offset), eq(limit)))
                .thenReturn(Collections.singletonList(mockClass));

        // When
        List<Classes> result = classesRepository.findWithOffset(offset, limit);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("幼幼班", result.get(0).getClassName());
    }

    // ==================== countTotal Tests ====================

    @Test
    void testCountTotal_ReturnsCount() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(25L);

        // When
        long total = classesRepository.countTotal();

        // Then
        assertEquals(25L, total);
    }

    // ==================== countByInstitutionID Tests ====================

    @Test
    void testCountByInstitutionID_ReturnsCount() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(testInstitutionId.toString())))
                .thenReturn(5L);

        // When
        long count = classesRepository.countByInstitutionID(testInstitutionId);

        // Then
        assertEquals(5L, count);
    }

    @Test
    void testCountByInstitutionID_ReturnsZero() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString()))
                .thenReturn(0L);

        // When
        long count = classesRepository.countByInstitutionID(testInstitutionId);

        // Then
        assertEquals(0L, count);
    }

    // ==================== incrementCurrentStudents Tests ====================

    @Test
    void testIncrementCurrentStudents_Success() {
        // Given
        when(jdbcTemplate.update(anyString(), eq(testClassId.toString())))
                .thenReturn(1);

        // When
        classesRepository.incrementCurrentStudents(testClassId);

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), eq(testClassId.toString()));
    }

    @Test
    void testIncrementCurrentStudents_AlreadyFull() {
        // Given - returns 0 when class is already at capacity
        when(jdbcTemplate.update(anyString(), eq(testClassId.toString())))
                .thenReturn(0);

        // When
        classesRepository.incrementCurrentStudents(testClassId);

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), eq(testClassId.toString()));
    }

    // ==================== decrementCurrentStudents Tests ====================

    @Test
    void testDecrementCurrentStudents_Success() {
        // Given
        when(jdbcTemplate.update(anyString(), eq(testClassId.toString())))
                .thenReturn(1);

        // When
        classesRepository.decrementCurrentStudents(testClassId);

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), eq(testClassId.toString()));
    }

    @Test
    void testDecrementCurrentStudents_AlreadyZero() {
        // Given - returns 0 when already at 0
        when(jdbcTemplate.update(anyString(), eq(testClassId.toString())))
                .thenReturn(0);

        // When
        classesRepository.decrementCurrentStudents(testClassId);

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), eq(testClassId.toString()));
    }
    // ==================== isClassFull Tests ====================

    @Test
    void testIsClassFull_True() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(testClassId.toString())))
                .thenReturn(true);

        // When
        boolean isFull = classesRepository.isClassFull(testClassId);

        // Then
        assertTrue(isFull);
    }

    @Test
    void testIsClassFull_False() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(testClassId.toString())))
                .thenReturn(false);

        // When
        boolean isFull = classesRepository.isClassFull(testClassId);

        // Then
        assertFalse(isFull);
    }

    @Test
    void testIsClassFull_Exception() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(testClassId.toString())))
                .thenThrow(new RuntimeException("Error"));

        // When
        boolean isFull = classesRepository.isClassFull(testClassId);

        // Then
        assertTrue(isFull); // Conservatively returns true on error
    }

    // ==================== RowMapper Logic Tests ====================
    // We can't access private static RowMappers directly, but we can test them via
    // the query methods
    // by capturing the RowMapper and testing it directly, or rely on
    // integration-style testing if we used H2.
    // Since we mock JdbcTemplate, we can capture the RowMapper passed to it and
    // invoke mapRow.

    @Test
    void testClassesRowMapper_Logic() throws java.sql.SQLException {
        // Given
        java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
        when(rs.getString("ClassID")).thenReturn(testClassId.toString());
        when(rs.getString("ClassName")).thenReturn("Test Class");
        when(rs.getObject("Capacity")).thenReturn(30);
        when(rs.getObject("CurrentStudents")).thenReturn(5);
        when(rs.getObject("MinAgeDescription")).thenReturn(2);
        when(rs.getObject("MaxAgeDescription")).thenReturn(4);
        when(rs.getString("AdditionalInfo")).thenReturn("Info");
        when(rs.getString("InstitutionID")).thenReturn(testInstitutionId.toString());

        // Capture the RowMapper
        org.mockito.ArgumentCaptor<RowMapper<Classes>> mapperCaptor = org.mockito.ArgumentCaptor
                .forClass(RowMapper.class);
        when(jdbcTemplate.query(anyString(), mapperCaptor.capture())).thenReturn(Collections.emptyList());

        // Trigger findAll to capture mapper
        classesRepository.findAll();
        RowMapper<Classes> mapper = mapperCaptor.getValue();

        // When
        Classes result = mapper.mapRow(rs, 1);

        // Then
        assertNotNull(result);
        assertEquals(testClassId, result.getClassID());
        assertEquals(30, result.getCapacity());
        assertEquals(5, result.getCurrentStudents());
        assertEquals(2, result.getMinAgeDescription());
        assertEquals(4, result.getMaxAgeDescription());
        assertEquals("Info", result.getAdditionalInfo());
        assertEquals(testInstitutionId, result.getInstitutionID());
    }

    @Test
    void testClassesRowMapper_Logic_Nulls() throws java.sql.SQLException {
        // Given
        java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
        when(rs.getString("ClassID")).thenReturn(testClassId.toString());
        when(rs.getString("ClassName")).thenReturn("Test Class");
        when(rs.getObject("Capacity")).thenReturn(null);
        when(rs.getObject("CurrentStudents")).thenReturn(null);
        when(rs.getObject("MinAgeDescription")).thenReturn(null);
        when(rs.getObject("MaxAgeDescription")).thenReturn(null);
        when(rs.getString("AdditionalInfo")).thenReturn("Info");
        when(rs.getString("InstitutionID")).thenReturn(testInstitutionId.toString());

        org.mockito.ArgumentCaptor<RowMapper<Classes>> mapperCaptor = org.mockito.ArgumentCaptor
                .forClass(RowMapper.class);
        when(jdbcTemplate.query(anyString(), mapperCaptor.capture())).thenReturn(Collections.emptyList());

        classesRepository.findAll();
        RowMapper<Classes> mapper = mapperCaptor.getValue();

        // When
        Classes result = mapper.mapRow(rs, 1);

        // Then
        assertNotNull(result);
        assertNull(result.getCapacity());
        assertNull(result.getCurrentStudents());
        assertNull(result.getMinAgeDescription());
        assertNull(result.getMaxAgeDescription());
    }

    // ==================== DTO Query Tests ====================

    @Test
    void testFindAllWithInstitutionName_Success() {
        // Given
        // We capture the mapper because it's a private static field in the repository
        doReturn(Collections.emptyList()).when(jdbcTemplate).query(anyString(), any(RowMapper.class));

        // When
        List<Group4.Childcare.DTO.ClassSummaryDTO> result = classesRepository.findAllWithInstitutionName();

        // Then
        assertNotNull(result);
        verify(jdbcTemplate).query(contains("LEFT JOIN institutions"), any(RowMapper.class));
    }

    @Test
    void testFindWithOffsetAndInstitutionName_Success() {
        // Given
        doReturn(Collections.emptyList()).when(jdbcTemplate).query(anyString(), any(RowMapper.class), anyInt(),
                anyInt());

        // When
        List<Group4.Childcare.DTO.ClassSummaryDTO> result = classesRepository.findWithOffsetAndInstitutionName(0, 10);

        // Then
        assertNotNull(result);
        verify(jdbcTemplate).query(contains("OFFSET ? ROWS"), any(RowMapper.class), eq(0), eq(10));
    }

    @Test
    void testFindWithOffsetAndInstitutionNameByInstitutionID_Success() {
        // Given
        doReturn(Collections.emptyList()).when(jdbcTemplate).query(anyString(), any(RowMapper.class), anyString(),
                anyInt(), anyInt());

        // When
        List<Group4.Childcare.DTO.ClassSummaryDTO> result = classesRepository
                .findWithOffsetAndInstitutionNameByInstitutionID(0, 10, testInstitutionId);

        // Then
        assertNotNull(result);
        verify(jdbcTemplate).query(contains("WHERE c.InstitutionID = ?"), any(RowMapper.class),
                eq(testInstitutionId.toString()), eq(0), eq(10));
    }

    @Test
    void testFindClassesByInstitutionName_Success() {
        // Given
        doReturn(Collections.emptyList()).when(jdbcTemplate).query(anyString(), any(RowMapper.class), anyString());

        // When
        List<Group4.Childcare.DTO.ClassSummaryDTO> result = classesRepository.findClassesByInstitutionName("Test Inst");

        // Then
        assertNotNull(result);
        verify(jdbcTemplate).query(contains("WHERE i.InstitutionName LIKE ?"), any(RowMapper.class), eq("%Test Inst%"));
    }

    @Test
    void testFindClassNamesByInstitutionId_Success() {
        // Given
        doReturn(Collections.emptyList()).when(jdbcTemplate).query(anyString(), any(RowMapper.class), anyString());

        // When
        List<Group4.Childcare.DTO.ClassNameDTO> result = classesRepository
                .findClassNamesByInstitutionId(testInstitutionId);

        // Then
        assertNotNull(result);
        verify(jdbcTemplate).query(contains("SELECT c.ClassID, c.ClassName FROM"), any(RowMapper.class),
                eq(testInstitutionId.toString()));
    }

    // ==================== Complex RowMapper Logic Tests (DTOs)
    // ====================

    @Test
    void testClassSummaryRowMapper_Logic() throws java.sql.SQLException {
        // Given
        java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
        when(rs.getString("ClassID")).thenReturn(testClassId.toString());
        when(rs.getString("ClassName")).thenReturn("Test Class");
        when(rs.getObject("Capacity")).thenReturn(30);
        when(rs.getObject("MinAgeDescription")).thenReturn(2);
        when(rs.getObject("MaxAgeDescription")).thenReturn(4);
        when(rs.getString("AdditionalInfo")).thenReturn("Info");
        when(rs.getString("InstitutionName")).thenReturn("Test Inst");
        when(rs.getString("InstitutionID")).thenReturn(testInstitutionId.toString());

        org.mockito.ArgumentCaptor<RowMapper<Group4.Childcare.DTO.ClassSummaryDTO>> mapperCaptor = org.mockito.ArgumentCaptor
                .forClass(RowMapper.class);
        when(jdbcTemplate.query(anyString(), mapperCaptor.capture())).thenReturn(Collections.emptyList());

        // Trigger to capture
        classesRepository.findAllWithInstitutionName();
        RowMapper<Group4.Childcare.DTO.ClassSummaryDTO> mapper = mapperCaptor.getValue();

        // When
        Group4.Childcare.DTO.ClassSummaryDTO result = mapper.mapRow(rs, 1);

        // Then
        assertNotNull(result);
        assertEquals(testClassId, result.getClassID());
        assertEquals("Test Inst", result.getInstitutionName());
        assertEquals(30, result.getCapacity());
        assertEquals("2", result.getMinAgeDescription());
    }

    @Test
    void testClassSummaryRowMapper_Logic_Nulls() throws java.sql.SQLException {
        // Given
        java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
        when(rs.getString("ClassID")).thenReturn(testClassId.toString());
        when(rs.getString("ClassName")).thenReturn("Test Class");
        when(rs.getObject("Capacity")).thenReturn(null);
        when(rs.getObject("MinAgeDescription")).thenReturn(null);
        when(rs.getObject("MaxAgeDescription")).thenReturn(null);
        when(rs.getString("AdditionalInfo")).thenReturn(null);
        when(rs.getString("InstitutionName")).thenReturn(null);
        when(rs.getString("InstitutionID")).thenReturn(null);

        org.mockito.ArgumentCaptor<RowMapper<Group4.Childcare.DTO.ClassSummaryDTO>> mapperCaptor = org.mockito.ArgumentCaptor
                .forClass(RowMapper.class);
        when(jdbcTemplate.query(anyString(), mapperCaptor.capture())).thenReturn(Collections.emptyList());

        classesRepository.findAllWithInstitutionName();
        RowMapper<Group4.Childcare.DTO.ClassSummaryDTO> mapper = mapperCaptor.getValue();

        // When
        Group4.Childcare.DTO.ClassSummaryDTO result = mapper.mapRow(rs, 1);

        // Then
        assertNotNull(result);
        assertNull(result.getCapacity());
        assertNull(result.getMinAgeDescription());
        assertNull(result.getInstitutionName());
    }

    @Test
    void testClassNameRowMapper_Logic() throws java.sql.SQLException {
        // Given
        java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
        when(rs.getString("ClassID")).thenReturn(testClassId.toString());
        when(rs.getString("ClassName")).thenReturn("Test Class");

        org.mockito.ArgumentCaptor<RowMapper<Group4.Childcare.DTO.ClassNameDTO>> mapperCaptor = org.mockito.ArgumentCaptor
                .forClass(RowMapper.class);
        when(jdbcTemplate.query(anyString(), mapperCaptor.capture(), anyString())).thenReturn(Collections.emptyList());

        classesRepository.findClassNamesByInstitutionId(testInstitutionId);
        RowMapper<Group4.Childcare.DTO.ClassNameDTO> mapper = mapperCaptor.getValue();

        // When
        Group4.Childcare.DTO.ClassNameDTO result = mapper.mapRow(rs, 1);

        // Then
        assertNotNull(result);
        assertEquals(testClassId, result.getClassID());
        assertEquals("Test Class", result.getClassName());
    }

    @Test
    void testFindInstitutionsWithClassesByName_Success() throws java.sql.SQLException {
        // Given
        java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
        // Mock institution fields
        when(rs.getString("InstitutionID")).thenReturn(testInstitutionId.toString());
        when(rs.getString("InstitutionName")).thenReturn("Test Inst");
        // ... mock other institution fields if necessary, but basics are enough for
        // coverage

        // Mock class fields
        when(rs.getString("ClassID")).thenReturn(testClassId.toString());
        when(rs.getString("ClassName")).thenReturn("Test Class");
        // ... other class fields

        org.mockito.ArgumentCaptor<RowMapper<Map<String, Object>>> mapperCaptor = org.mockito.ArgumentCaptor
                .forClass(RowMapper.class);
        when(jdbcTemplate.query(anyString(), mapperCaptor.capture(), anyString())).thenReturn(Collections.emptyList());

        // When
        classesRepository.findInstitutionsWithClassesByName("Test");
        RowMapper<Map<String, Object>> mapper = mapperCaptor.getValue();
        Map<String, Object> result = mapper.mapRow(rs, 1);

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("institution"));
        assertTrue(result.containsKey("class"));

        Map<String, Object> instMap = (Map<String, Object>) result.get("institution");
        assertEquals("Test Inst", instMap.get("institutionName"));

        Map<String, Object> classMap = (Map<String, Object>) result.get("class");
        assertEquals("Test Class", classMap.get("className"));
    }

    @Test
    void testFindInstitutionsWithClassesByName_NoClass() throws java.sql.SQLException {
        // Given
        java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
        when(rs.getString("InstitutionID")).thenReturn(testInstitutionId.toString());
        when(rs.getString("ClassID")).thenReturn(null); // No class

        org.mockito.ArgumentCaptor<RowMapper<Map<String, Object>>> mapperCaptor = org.mockito.ArgumentCaptor
                .forClass(RowMapper.class);
        when(jdbcTemplate.query(anyString(), mapperCaptor.capture(), anyString())).thenReturn(Collections.emptyList());

        classesRepository.findInstitutionsWithClassesByName("Test");
        RowMapper<Map<String, Object>> mapper = mapperCaptor.getValue();
        Map<String, Object> result = mapper.mapRow(rs, 1);

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("institution"));
        assertFalse(result.containsKey("class")); // class key might not be present or null depending on impl
    }
}

package Group4.Childcare.repository;

import Group4.Childcare.Model.Institutions;
import Group4.Childcare.Repository.InstitutionsJdbcRepository;
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
 * InstitutionsJdbcRepository 單元測試
 * 測試機構管理相關的資料庫操作
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class InstitutionsJdbcRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private InstitutionsJdbcRepository repository;

    private UUID testInstitutionId;
    private Institutions testInstitution;

    @BeforeEach
    void setUp() {
        testInstitutionId = UUID.randomUUID();

        testInstitution = new Institutions();
        testInstitution.setInstitutionID(testInstitutionId);
        testInstitution.setInstitutionName("新竹縣測試托育機構");
        testInstitution.setContactPerson("張三");
        testInstitution.setPhoneNumber("03-1234567");
        testInstitution.setAddress("新竹縣竹北市測試路123號");
        testInstitution.setEmail("test@institution.com");
    }

    // ===== 測試 save (新增機構) =====
    @Test
    void testSave_Success() {
        // Given - UPDATE has 18 parameters
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        // When
        Institutions result = repository.save(testInstitution);

        // Then
        assertNotNull(result);
        assertEquals(testInstitutionId, result.getInstitutionID());
        assertEquals("新竹縣測試托育機構", result.getInstitutionName());
    }

    @Test
    void testSave_ThrowsException_WhenInsertFails() {
        // Given - UPDATE has 18 parameters
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Database constraint violation"));

        // When & Then
        assertThrows(RuntimeException.class, () -> repository.save(testInstitution));
    }

    // ===== 測試 findById (根據ID查詢) =====
    @Test
    void testFindById_Success() {
        // Given
        List<Institutions> mockList = Collections.singletonList(testInstitution);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(mockList);

        // When
        Optional<Institutions> result = repository.findById(testInstitutionId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testInstitutionId, result.get().getInstitutionID());
        assertEquals("新竹縣測試托育機構", result.get().getInstitutionName());
        verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class), anyString());
    }

    @Test
    void testFindById_ReturnsEmpty_WhenNotFound() {
        // Given
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(Collections.emptyList());

        // When
        Optional<Institutions> result = repository.findById(UUID.randomUUID());

        // Then
        assertFalse(result.isPresent());
    }

    // ===== 測試 findAll (查詢所有機構) =====
    @Test
    void testFindAll_ReturnsAllInstitutions() {
        // Given
        Institutions institution2 = new Institutions();
        institution2.setInstitutionID(UUID.randomUUID());
        institution2.setInstitutionName("台北市測試機構");

        List<Institutions> mockList = Arrays.asList(testInstitution, institution2);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(mockList);

        // When
        List<Institutions> result = repository.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class));
    }

    // ===== 測試 findAllWithPagination (分頁查詢) =====
    @Test
    void testFindAllWithPagination_ReturnsCorrectPage() {
        // Given
        int offset = 0;
        int size = 10;
        List<Institutions> mockList = Collections.singletonList(testInstitution);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(offset), eq(size)))
                .thenReturn(mockList);

        // When
        List<Institutions> result = repository.findAllWithPagination(offset, size);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class), eq(offset), eq(size));
    }

    // ===== 測試 findByInstitutionIDWithPagination (根據機構ID分頁查詢) =====
    @Test
    void testFindByInstitutionIDWithPagination_ReturnsCorrectPage() {
        // Given
        int offset = 0;
        int size = 10;
        List<Institutions> mockList = Collections.singletonList(testInstitution);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), eq(offset), eq(size)))
                .thenReturn(mockList);

        // When
        List<Institutions> result = repository.findByInstitutionIDWithPagination(testInstitutionId, offset, size);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testInstitutionId, result.get(0).getInstitutionID());
        verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class), anyString(), eq(offset), eq(size));
    }

    // ===== 測試 findAllWithSearchAndPagination (搜尋+分頁) =====
    @Test
    void testFindAllWithSearchAndPagination_ReturnsMatchingResults() {
        // Given
        String search = "測試";
        int offset = 0;
        int size = 10;
        List<Institutions> mockList = Collections.singletonList(testInstitution);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), anyString(), anyString(), eq(offset), eq(size)))
                .thenReturn(mockList);

        // When
        List<Institutions> result = repository.findAllWithSearchAndPagination(search, offset, size);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class), anyString(), anyString(), anyString(), eq(offset), eq(size));
    }

    // ===== 測試 count (計算總數) =====
    @Test
    void testCount_ReturnsCorrectCount() {
        // Given
        Long expectedCount = 15L;
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(expectedCount);

        // When
        long result = repository.count();

        // Then
        assertEquals(expectedCount, result);
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(Long.class));
    }

    // ===== 測試 countByInstitutionID (根據機構ID計數) =====
    @Test
    void testCountByInstitutionID_ReturnsCorrectCount() {
        // Given
        Long expectedCount = 1L;
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString()))
                .thenReturn(expectedCount);

        // When
        long result = repository.countByInstitutionID(testInstitutionId);

        // Then
        assertEquals(expectedCount, result);
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(Long.class), anyString());
    }

    // ===== 測試 countAllWithSearch (搜尋計數) =====
    @Test
    void testCountAllWithSearch_ReturnsMatchingCount() {
        // Given
        String search = "測試";
        Long expectedCount = 5L;
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString(), anyString(), anyString()))
                .thenReturn(expectedCount);

        // When
        long result = repository.countAllWithSearch(search);

        // Then
        assertEquals(expectedCount, result);
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(Long.class), anyString(), anyString(), anyString());
    }

    // ===== 測試 save (更新機構) =====
    @Test
    void testSave_UpdatesExisting() {
        // Given
        testInstitution.setInstitutionName("更新後的機構名稱");
        testInstitution.setContactPerson("李四");
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        // When
        Institutions result = repository.save(testInstitution);

        // Then
        assertNotNull(result);
        assertEquals("更新後的機構名稱", result.getInstitutionName());
        assertEquals("李四", result.getContactPerson());
        verify(jdbcTemplate, times(1)).update(anyString(), any(), any(), any(), any(), any(), any());
    }

    // ===== 測試 deleteById (刪除機構) =====
    @Test
    void testDeleteById_Success() {
        // Given
        when(jdbcTemplate.update(anyString(), anyString()))
                .thenReturn(1);

        // When
        repository.deleteById(testInstitutionId);

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), eq(testInstitutionId.toString()));
    }

    // ===== 測試 existsById (檢查是否存在) =====
    @Test
    void testExistsById_ReturnsTrue_WhenExists() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(1);

        // When
        boolean result = repository.existsById(testInstitutionId);

        // Then
        assertTrue(result);
    }

    @Test
    void testExistsById_ReturnsFalse_WhenNotExists() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(0);

        // When
        boolean result = repository.existsById(UUID.randomUUID());

        // Then
        assertFalse(result);
    }

    // ===== 測試邊界情況 =====
    @Test
    void testFindAllWithPagination_WithZeroSize() {
        // Given
        int offset = 0;
        int size = 0;
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(offset), eq(size)))
                .thenReturn(Collections.emptyList());

        // When
        List<Institutions> result = repository.findAllWithPagination(offset, size);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindAllWithSearchAndPagination_WithEmptySearch() {
        // Given
        String search = "";
        int offset = 0;
        int size = 10;
        List<Institutions> mockList = Collections.singletonList(testInstitution);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), anyString(), anyString(), eq(offset), eq(size)))
                .thenReturn(mockList);

        // When
        List<Institutions> result = repository.findAllWithSearchAndPagination(search, offset, size);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testSave_WithNullOptionalFields() {
        // Given
        Institutions institutionWithNulls = new Institutions();
        institutionWithNulls.setInstitutionID(testInstitutionId);
        institutionWithNulls.setInstitutionName("最小機構");
        institutionWithNulls.setContactPerson(null);
        institutionWithNulls.setPhoneNumber(null);
        institutionWithNulls.setEmail(null);

        // Mock existsById to return true
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(1);

        // Mock update with 18 parameters
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        // When
        Institutions result = repository.save(institutionWithNulls);

        // Then
        assertNotNull(result);
        assertEquals("最小機構", result.getInstitutionName());
        assertNull(result.getContactPerson());
    }
}


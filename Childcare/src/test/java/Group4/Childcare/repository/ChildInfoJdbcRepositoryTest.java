package Group4.Childcare.repository;

import Group4.Childcare.Model.ChildInfo;
import Group4.Childcare.Repository.ChildInfoJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ChildInfoJdbcRepository 單元測試
 * 測試幼兒資訊管理相關的資料庫操作
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ChildInfoJdbcRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ChildInfoJdbcRepository childInfoRepository;

    private UUID testChildId;
    private UUID testFamilyInfoId;

    @BeforeEach
    void setUp() {
        testChildId = UUID.randomUUID();
        testFamilyInfoId = UUID.randomUUID();
    }

    // ==================== save Tests ====================

    @Test
    void testSave_NewChildInfo_Success() {
        // Given
        ChildInfo childInfo = createTestChildInfo();
        // Note: ChildID must be set before calling save(), as the repository doesn't auto-generate it

        when(jdbcTemplate.update(anyString(), any(), anyString(), anyString(),
                anyBoolean(), any(LocalDate.class), any(), anyString()))
            .thenReturn(1);

        // When
        ChildInfo result = childInfoRepository.save(childInfo);

        // Then
        assertNotNull(result);
        assertEquals(testChildId, result.getChildID());
    }

    // ==================== put Tests ====================

    @Test
    void testPut_UpdateChildInfo_Success() {
        // Given
        ChildInfo childInfo = createTestChildInfo();
        childInfo.setChildID(testChildId);

        when(jdbcTemplate.update(anyString(), anyString(), anyString(), anyBoolean(),
                any(LocalDate.class), any(), anyString(), any()))
            .thenReturn(1);

        // When
        ChildInfo result = childInfoRepository.put(childInfo);

        // Then
        assertNotNull(result);
        assertEquals(testChildId, result.getChildID());
        verify(jdbcTemplate, times(1)).update(anyString(), anyString(), anyString(),
                anyBoolean(), any(LocalDate.class), any(), anyString(), any());
    }

    // ==================== findById Tests ====================

    @Test
    void testFindById_Success() {
        // Given
        ChildInfo mockChildInfo = createTestChildInfo();
        mockChildInfo.setChildID(testChildId);

        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(testChildId.toString())))
            .thenReturn(mockChildInfo);

        // When
        Optional<ChildInfo> result = childInfoRepository.findById(testChildId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testChildId, result.get().getChildID());
        assertEquals("王小明", result.get().getName());
    }

    @Test
    void testFindById_NotFound() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
            .thenThrow(new RuntimeException("Not found"));

        // When
        Optional<ChildInfo> result = childInfoRepository.findById(testChildId);

        // Then
        assertFalse(result.isPresent());
    }

    // ==================== findAll Tests ====================

    @Test
    void testFindAll_Success() {
        // Given
        ChildInfo child1 = createTestChildInfo();
        child1.setChildID(UUID.randomUUID());
        child1.setName("王小明");

        ChildInfo child2 = createTestChildInfo();
        child2.setChildID(UUID.randomUUID());
        child2.setName("李小華");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(Arrays.asList(child1, child2));

        // When
        List<ChildInfo> result = childInfoRepository.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("王小明", result.get(0).getName());
        assertEquals("李小華", result.get(1).getName());
    }

    @Test
    void testFindAll_EmptyResult() {
        // Given
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(Collections.emptyList());

        // When
        List<ChildInfo> result = childInfoRepository.findAll();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== count Tests ====================

    @Test
    void testCount_Success() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(25L);

        // When
        long count = childInfoRepository.count();

        // Then
        assertEquals(25L, count);
    }

    @Test
    void testCount_Zero() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(0L);

        // When
        long count = childInfoRepository.count();

        // Then
        assertEquals(0L, count);
    }

    // ==================== existsById Tests ====================

    @Test
    void testExistsById_True() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testChildId.toString())))
            .thenReturn(1);

        // When
        boolean exists = childInfoRepository.existsById(testChildId);

        // Then
        assertTrue(exists);
    }

    @Test
    void testExistsById_False() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testChildId.toString())))
            .thenReturn(0);

        // When
        boolean exists = childInfoRepository.existsById(testChildId);

        // Then
        assertFalse(exists);
    }

    // ==================== deleteById Tests ====================

    @Test
    void testDeleteById_Success() {
        // Given
        when(jdbcTemplate.update(anyString(), eq(testChildId.toString())))
            .thenReturn(1);

        // When
        childInfoRepository.deleteById(testChildId);

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), eq(testChildId.toString()));
    }

    // ==================== Helper Methods ====================

    private ChildInfo createTestChildInfo() {
        ChildInfo childInfo = new ChildInfo();
        childInfo.setChildID(testChildId);
        childInfo.setNationalID("A123456789");
        childInfo.setName("王小明");
        childInfo.setGender(true);
        childInfo.setBirthDate(LocalDate.of(2020, 3, 15));
        childInfo.setFamilyInfoID(testFamilyInfoId);
        childInfo.setHouseholdAddress("台北市信義區信義路一段");
        return childInfo;
    }
}


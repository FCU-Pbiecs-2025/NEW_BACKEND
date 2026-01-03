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

import java.sql.ResultSet;
import java.sql.SQLException;
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

    // ==================== RowMapper Tests ====================

    @Test
    void testRowMapper_FullData() throws SQLException {
        // Given
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("ChildID")).thenReturn(testChildId.toString());
        when(rs.getString("NationalID")).thenReturn("A123456789");
        when(rs.getString("Name")).thenReturn("王小明");
        when(rs.getBoolean("Gender")).thenReturn(true);
        when(rs.getDate("BirthDate")).thenReturn(java.sql.Date.valueOf(LocalDate.of(2020, 1, 1)));
        when(rs.getString("FamilyInfoID")).thenReturn(testFamilyInfoId.toString());
        when(rs.getString("HouseholdAddress")).thenReturn("台北市");

        final RowMapper<ChildInfo>[] capturedMapper = new RowMapper[1];
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenAnswer(invocation -> {
            capturedMapper[0] = invocation.getArgument(1);
            return Collections.emptyList();
        });

        childInfoRepository.findAll();
        assertNotNull(capturedMapper[0]);

        // When
        ChildInfo result = capturedMapper[0].mapRow(rs, 1);

        // Then
        assertNotNull(result);
        assertEquals(testChildId, result.getChildID());
        assertEquals("王小明", result.getName());
        assertEquals(LocalDate.of(2020, 1, 1), result.getBirthDate());
        assertEquals(testFamilyInfoId, result.getFamilyInfoID());
        assertEquals("台北市", result.getHouseholdAddress());
    }

    @Test
    void testRowMapper_NullOptionalFields() throws SQLException {
        // Given
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("ChildID")).thenReturn(testChildId.toString());
        when(rs.getString("NationalID")).thenReturn("A123456789");
        when(rs.getString("Name")).thenReturn("王小明");
        when(rs.getBoolean("Gender")).thenReturn(true);
        when(rs.getDate("BirthDate")).thenReturn(null);
        when(rs.getString("FamilyInfoID")).thenReturn(null);
        when(rs.getString("HouseholdAddress")).thenReturn("台北市");

        final RowMapper<ChildInfo>[] capturedMapper = new RowMapper[1];
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenAnswer(invocation -> {
            capturedMapper[0] = invocation.getArgument(1);
            return Collections.emptyList();
        });

        childInfoRepository.findAll();

        // When
        ChildInfo result = capturedMapper[0].mapRow(rs, 1);

        // Then
        assertNotNull(result);
        assertNull(result.getBirthDate());
        assertNull(result.getFamilyInfoID());
    }

    // ==================== Additional Branch Tests ====================

    @Test
    void testSave_NullFamilyInfoId_Success() {
        // Given
        ChildInfo childInfo = createTestChildInfo();
        childInfo.setFamilyInfoID(null);

        when(jdbcTemplate.update(anyString(), any(), anyString(), anyString(),
                anyBoolean(), any(), isNull(), anyString()))
            .thenReturn(1);

        // When
        ChildInfo result = childInfoRepository.save(childInfo);

        // Then
        assertNotNull(result);
        assertNull(result.getFamilyInfoID());
    }

    @Test
    void testPut_NullFamilyInfoId_Success() {
        // Given
        ChildInfo childInfo = createTestChildInfo();
        childInfo.setFamilyInfoID(null);

        when(jdbcTemplate.update(anyString(), anyString(), anyString(), anyBoolean(),
                any(), isNull(), anyString(), any()))
            .thenReturn(1);

        // When
        ChildInfo result = childInfoRepository.put(childInfo);

        // Then
        assertNotNull(result);
        assertNull(result.getFamilyInfoID());
    }

    @Test
    void testCount_Null() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(null);

        // When
        long count = childInfoRepository.count();

        // Then
        assertEquals(0L, count);
    }

    @Test
    void testExistsById_Null() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
            .thenReturn(null);

        // When
        boolean exists = childInfoRepository.existsById(testChildId);

        // Then
        assertFalse(exists);
    }

    @Test
    void testDelete_Entity_Success() {
        // Given
        ChildInfo childInfo = createTestChildInfo();

        // When
        childInfoRepository.delete(childInfo);

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), eq(testChildId.toString()));
    }

    @Test
    void testFindByFamilyInfoID_Success() {
        // Given
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(testFamilyInfoId.toString())))
            .thenReturn(Collections.singletonList(createTestChildInfo()));

        // When
        List<ChildInfo> result = childInfoRepository.findByFamilyInfoID(testFamilyInfoId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
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


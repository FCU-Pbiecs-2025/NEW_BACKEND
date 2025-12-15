package Group4.Childcare.repository;

import Group4.Childcare.Model.ParentInfo;
import Group4.Childcare.Repository.ParentInfoJdbcRepository;
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
 * ParentInfoJdbcRepository ?��?測試
 * 測試家長資�?管�??��??��??�庫?��?
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ParentInfoJdbcRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ParentInfoJdbcRepository parentInfoRepository;

    private UUID testParentId;
    private UUID testFamilyInfoId;

    @BeforeEach
    void setUp() {
        testParentId = UUID.randomUUID();
        testFamilyInfoId = UUID.randomUUID();
    }

    // ==================== save Tests ====================

    @Test
    void testSave_NewParentInfo_WithNullId_Success() {
        // Given
        ParentInfo parentInfo = createTestParentInfo();
        parentInfo.setParentID(null);

        when(jdbcTemplate.update(anyString(), any(), anyString(), anyString(), anyBoolean(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                any(LocalDate.class), anyBoolean(), any(), any()))
            .thenReturn(1);

        // When
        ParentInfo result = parentInfoRepository.save(parentInfo);

        // Then
        assertNotNull(result);
        assertNotNull(result.getParentID());
    }

    @Test
    void testSave_ExistingParentInfo_Update_Success() {
        // Given
        ParentInfo parentInfo = createTestParentInfo();
        parentInfo.setParentID(testParentId);

        when(jdbcTemplate.update(anyString(), anyString(), anyString(), anyBoolean(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                any(LocalDate.class), anyBoolean(), any(), any(), any()))
            .thenReturn(1);

        // When
        ParentInfo result = parentInfoRepository.save(parentInfo);

        // Then
        assertNotNull(result);
        assertEquals(testParentId, result.getParentID());
    }

    // ==================== findById Tests ====================

    @Test
    void testFindById_Success() {
        // Given
        ParentInfo mockParentInfo = createTestParentInfo();
        mockParentInfo.setParentID(testParentId);

        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(testParentId.toString())))
            .thenReturn(mockParentInfo);

        // When
        Optional<ParentInfo> result = parentInfoRepository.findById(testParentId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testParentId, result.get().getParentID());
        assertEquals("王大明", result.get().getName());
    }

    @Test
    void testFindById_NotFound() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
            .thenThrow(new RuntimeException("Not found"));

        // When
        Optional<ParentInfo> result = parentInfoRepository.findById(testParentId);

        // Then
        assertFalse(result.isPresent());
    }

    // ==================== findAll Tests ====================

    @Test
    void testFindAll_Success() {
        // Given
        ParentInfo parent1 = createTestParentInfo();
        parent1.setParentID(UUID.randomUUID());
        parent1.setName("王大明");

        ParentInfo parent2 = createTestParentInfo();
        parent2.setParentID(UUID.randomUUID());
        parent2.setName("李小華");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(Arrays.asList(parent1, parent2));

        // When
        List<ParentInfo> result = parentInfoRepository.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("王大明", result.get(0).getName());
        assertEquals("李小華", result.get(1).getName());
    }

    @Test
    void testFindAll_EmptyResult() {
        // Given
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(Collections.emptyList());

        // When
        List<ParentInfo> result = parentInfoRepository.findAll();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }


    // ==================== count Tests ====================

    @Test
    void testCount_Success() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(20L);

        // When
        long count = parentInfoRepository.count();

        // Then
        assertEquals(20L, count);
    }

    @Test
    void testCount_Zero() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(0L);

        // When
        long count = parentInfoRepository.count();

        // Then
        assertEquals(0L, count);
    }

    // ==================== existsById Tests ====================

    @Test
    void testExistsById_True() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testParentId.toString())))
            .thenReturn(1);

        // When
        boolean exists = parentInfoRepository.existsById(testParentId);

        // Then
        assertTrue(exists);
    }

    @Test
    void testExistsById_False() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testParentId.toString())))
            .thenReturn(0);

        // When
        boolean exists = parentInfoRepository.existsById(testParentId);

        // Then
        assertFalse(exists);
    }

    // ==================== deleteById Tests ====================

    @Test
    void testDeleteById_Success() {
        // Given
        when(jdbcTemplate.update(anyString(), eq(testParentId.toString())))
            .thenReturn(1);

        // When
        parentInfoRepository.deleteById(testParentId);

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), eq(testParentId.toString()));
    }

    // ==================== Helper Methods ====================

    private ParentInfo createTestParentInfo() {
        ParentInfo parentInfo = new ParentInfo();
        parentInfo.setParentID(testParentId);
        parentInfo.setNationalID("A123456789");
        parentInfo.setName("王大明");
        parentInfo.setGender(true);
        parentInfo.setRelationship("父親");
        parentInfo.setOccupation("工程師");
        parentInfo.setPhoneNumber("0912345678");
        parentInfo.setHouseholdAddress("台北市信義區信義路一段");
        parentInfo.setMailingAddress("台北市信義區信義路一段");
        parentInfo.setEmail("parent@example.com");
        parentInfo.setBirthDate(LocalDate.of(1980, 5, 15));
        parentInfo.setIsSuspended(false);
        parentInfo.setSuspendEnd(null);
        parentInfo.setFamilyInfoID(testFamilyInfoId);
        return parentInfo;
    }
}


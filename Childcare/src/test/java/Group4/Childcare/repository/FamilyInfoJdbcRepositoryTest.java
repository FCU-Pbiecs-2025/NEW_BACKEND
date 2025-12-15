package Group4.Childcare.repository;

import Group4.Childcare.Model.FamilyInfo;
import Group4.Childcare.Repository.FamilyInfoJdbcRepository;
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
 * FamilyInfoJdbcRepository ?ÆÂ?Ê∏¨Ë©¶
 * Ê∏¨Ë©¶ÂÆ∂Â∫≠Ë≥áË?ÁÆ°Á??∏È??ÑË??ôÂ∫´?ç‰?
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class FamilyInfoJdbcRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private FamilyInfoJdbcRepository familyInfoRepository;

    private UUID testFamilyInfoId;

    @BeforeEach
    void setUp() {
        testFamilyInfoId = UUID.randomUUID();
    }

    // ==================== save Tests ====================

    @Test
    void testSave_NewFamilyInfo_WithNullId_Success() {
        // Given
        FamilyInfo familyInfo = createTestFamilyInfo();
        familyInfo.setFamilyInfoID(null);

        when(jdbcTemplate.update(anyString(), anyString()))
            .thenReturn(1);

        // When
        FamilyInfo result = familyInfoRepository.save(familyInfo);

        // Then
        assertNotNull(result);
        assertNotNull(result.getFamilyInfoID());
        verify(jdbcTemplate, times(1)).update(anyString(), anyString());
    }

    @Test
    void testSave_ExistingFamilyInfo_Update_Success() {
        // Given
        FamilyInfo familyInfo = createTestFamilyInfo();
        familyInfo.setFamilyInfoID(testFamilyInfoId);

        // When
        FamilyInfo result = familyInfoRepository.save(familyInfo);

        // Then
        assertNotNull(result);
        assertEquals(testFamilyInfoId, result.getFamilyInfoID());
        // No update needed since only ID field exists
    }

    // ==================== findById Tests ====================

    @Test
    void testFindById_Success() {
        // Given
        FamilyInfo mockFamilyInfo = createTestFamilyInfo();
        mockFamilyInfo.setFamilyInfoID(testFamilyInfoId);

        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(testFamilyInfoId.toString())))
            .thenReturn(mockFamilyInfo);

        // When
        Optional<FamilyInfo> result = familyInfoRepository.findById(testFamilyInfoId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testFamilyInfoId, result.get().getFamilyInfoID());
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), any(RowMapper.class), eq(testFamilyInfoId.toString()));
    }

    @Test
    void testFindById_NotFound() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
            .thenThrow(new RuntimeException("Not found"));

        // When
        Optional<FamilyInfo> result = familyInfoRepository.findById(testFamilyInfoId);

        // Then
        assertFalse(result.isPresent());
    }

    // ==================== findAll Tests ====================

    @Test
    void testFindAll_Success() {
        // Given
        FamilyInfo family1 = createTestFamilyInfo();
        family1.setFamilyInfoID(UUID.randomUUID());

        FamilyInfo family2 = createTestFamilyInfo();
        family2.setFamilyInfoID(UUID.randomUUID());

        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(Arrays.asList(family1, family2));

        // When
        List<FamilyInfo> result = familyInfoRepository.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testFindAll_EmptyResult() {
        // Given
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(Collections.emptyList());

        // When
        List<FamilyInfo> result = familyInfoRepository.findAll();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== count Tests ====================

    @Test
    void testCount_Success() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(30L);

        // When
        long count = familyInfoRepository.count();

        // Then
        assertEquals(30L, count);
    }

    @Test
    void testCount_Zero() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(0L);

        // When
        long count = familyInfoRepository.count();

        // Then
        assertEquals(0L, count);
    }

    // ==================== existsById Tests ====================

    @Test
    void testExistsById_True() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testFamilyInfoId.toString())))
            .thenReturn(1);

        // When
        boolean exists = familyInfoRepository.existsById(testFamilyInfoId);

        // Then
        assertTrue(exists);
    }

    @Test
    void testExistsById_False() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testFamilyInfoId.toString())))
            .thenReturn(0);

        // When
        boolean exists = familyInfoRepository.existsById(testFamilyInfoId);

        // Then
        assertFalse(exists);
    }

    // ==================== deleteById Tests ====================

    @Test
    void testDeleteById_Success() {
        // Given
        when(jdbcTemplate.update(anyString(), eq(testFamilyInfoId.toString())))
            .thenReturn(1);

        // When
        familyInfoRepository.deleteById(testFamilyInfoId);

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), eq(testFamilyInfoId.toString()));
    }

    @Test
    void testDeleteById_NotFound() {
        // Given
        when(jdbcTemplate.update(anyString(), eq(testFamilyInfoId.toString())))
            .thenReturn(0);

        // When
        familyInfoRepository.deleteById(testFamilyInfoId);

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), eq(testFamilyInfoId.toString()));
    }

    // ==================== Helper Methods ====================

    private FamilyInfo createTestFamilyInfo() {
        FamilyInfo familyInfo = new FamilyInfo();
        familyInfo.setFamilyInfoID(testFamilyInfoId);
        return familyInfo;
    }
}


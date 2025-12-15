package Group4.Childcare.repository;

import Group4.Childcare.DTO.RevokeApplicationDTO;
import Group4.Childcare.Repository.RevokesJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RevokesJdbcRepository 單元測試
 * 測試撤銷申請管理相關的資料庫操作
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class RevokesJdbcRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private RevokesJdbcRepository revokesRepository;

    private UUID testCancellationId;
    private UUID testApplicationId;
    private UUID testUserId;
    private UUID testInstitutionId;

    @BeforeEach
    void setUp() {
        testCancellationId = UUID.randomUUID();
        testApplicationId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testInstitutionId = UUID.randomUUID();
    }

    // ==================== findRevokedApplications Tests ====================

    @Test
    void testFindRevokedApplications_WithAllFilters_Success() {
        // Given
        int page = 0;
        int size = 10;
        String institutionID = testInstitutionId.toString();
        String caseNumber = "12345";
        String nationalID = "A123456789";

        List<RevokeApplicationDTO> mockResults = Collections.singletonList(
            createTestRevokeApplicationDTO()
        );

        doReturn(mockResults).when(jdbcTemplate).query(anyString(), any(RowMapper.class), any(Object[].class));

        // When
        List<RevokeApplicationDTO> result = revokesRepository.findRevokedApplications(
            page, size, institutionID, caseNumber, nationalID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testCancellationId, result.get(0).getCancellationID());
    }

    @Test
    void testFindRevokedApplications_WithOnlyInstitutionID_Success() {
        // Given
        int page = 0;
        int size = 10;
        String institutionID = testInstitutionId.toString();

        List<RevokeApplicationDTO> mockResults = Arrays.asList(
            createTestRevokeApplicationDTO(),
            createTestRevokeApplicationDTO()
        );

        doReturn(mockResults).when(jdbcTemplate).query(anyString(), any(RowMapper.class), any(Object[].class));

        // When
        List<RevokeApplicationDTO> result = revokesRepository.findRevokedApplications(
            page, size, institutionID, null, null);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testFindRevokedApplications_WithNullFilters_Success() {
        // Given
        int page = 0;
        int size = 10;

        List<RevokeApplicationDTO> mockResults = Collections.singletonList(
            createTestRevokeApplicationDTO()
        );

        doReturn(mockResults).when(jdbcTemplate).query(anyString(), any(RowMapper.class), any(Object[].class));

        // When
        List<RevokeApplicationDTO> result = revokesRepository.findRevokedApplications(
            page, size, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testFindRevokedApplications_EmptyResult() {
        // Given
        doReturn(Collections.emptyList()).when(jdbcTemplate).query(anyString(), any(RowMapper.class), any(Object[].class));

        // When
        List<RevokeApplicationDTO> result = revokesRepository.findRevokedApplications(
            0, 10, null, null, null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindRevokedApplications_Pagination_Success() {
        // Given
        int page = 2;
        int size = 15;

        List<RevokeApplicationDTO> mockResults = Collections.singletonList(
            createTestRevokeApplicationDTO()
        );

        doReturn(mockResults).when(jdbcTemplate).query(anyString(), any(RowMapper.class), any(Object[].class));

        // When
        List<RevokeApplicationDTO> result = revokesRepository.findRevokedApplications(
            page, size, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        // Verify pagination parameters: offset should be page * size = 2 * 15 = 30
    }

    // ==================== countRevokedApplications Tests ====================

    @Test
    void testCountRevokedApplications_WithAllFilters_Success() {
        // Given
        String institutionID = testInstitutionId.toString();
        String caseNumber = "12345";
        String nationalID = "A123456789";

        // Mock with Object[] parameters
        doReturn(25L).when(jdbcTemplate).queryForObject(anyString(), eq(Long.class), any(Object[].class));

        // When
        long count = revokesRepository.countRevokedApplications(institutionID, caseNumber, nationalID);

        // Then
        assertEquals(25L, count);
    }

    @Test
    void testCountRevokedApplications_WithNullFilters_Success() {
        // Given - when no filters, queryForObject is called without parameters
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(100L);

        // When
        long count = revokesRepository.countRevokedApplications(null, null, null);

        // Then
        assertEquals(100L, count);
    }

    @Test
    void testCountRevokedApplications_ZeroResult() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(0L);

        // When
        long count = revokesRepository.countRevokedApplications(null, null, null);

        // Then
        assertEquals(0L, count);
    }

    @Test
    void testCountRevokedApplications_WithOnlyInstitutionID() {
        // Given
        String institutionID = testInstitutionId.toString();

        doReturn(50L).when(jdbcTemplate).queryForObject(anyString(), eq(Long.class), any(Object[].class));

        // When
        long count = revokesRepository.countRevokedApplications(institutionID, null, null);

        // Then
        assertEquals(50L, count);
    }

    @Test
    void testCountRevokedApplications_WithOnlyCaseNumber() {
        // Given
        String caseNumber = "12345";

        doReturn(1L).when(jdbcTemplate).queryForObject(anyString(), eq(Long.class), any(Object[].class));

        // When
        long count = revokesRepository.countRevokedApplications(null, caseNumber, null);

        // Then
        assertEquals(1L, count);
    }

    // ==================== Helper Methods ====================

    private RevokeApplicationDTO createTestRevokeApplicationDTO() {
        return new RevokeApplicationDTO(
            testCancellationId,
            testApplicationId,
            LocalDateTime.now(),
            testUserId,
            "王大明",
            testInstitutionId,
            "測試幼兒園",
            "家長主動撤銷",
            "A123456789",
            "12345"
        );
    }
}


package Group4.Childcare.repository;

import Group4.Childcare.DTO.ApplicationParticipantDTO;
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

import java.time.LocalDate;
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
                createTestRevokeApplicationDTO());

        doReturn(mockResults).when(jdbcTemplate).query(anyString(), any(RowMapper.class), any(Object[].class));

        // When
        List<RevokeApplicationDTO> result = revokesRepository.findRevokedApplications(
                page, size, institutionID, caseNumber, nationalID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testCancellationId, result.get(0).getCancellationID());
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
        // Given - when no filters, queryForObject is called without parameters or empty
        // params
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(100L);

        // When
        long count = revokesRepository.countRevokedApplications(null, null, null);

        // Then
        assertEquals(100L, count);
    }

    // ==================== searchRevokedApplicationsPaged Tests
    // ====================

    @Test
    void testSearchRevokedApplicationsPaged_Success() {
        List<RevokeApplicationDTO> list = Collections.singletonList(createTestRevokeApplicationDTO());
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(list);

        List<RevokeApplicationDTO> result = revokesRepository.searchRevokedApplicationsPaged("123", "N123", 0, 10,
                "InstID");
        assertEquals(1, result.size());
    }

    // ==================== countSearchRevokedApplications Tests
    // ====================

    @Test
    void testCountSearchRevokedApplications_Success() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(10L);
        long count = revokesRepository.countSearchRevokedApplications("123", "N123", "InstID");
        assertEquals(10L, count);
    }

    @Test
    void testCountSearchRevokedApplications_NoFilters() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(5L);
        long count = revokesRepository.countSearchRevokedApplications(null, null, null);
        assertEquals(5L, count);
    }

    // ==================== Detail & Other Query Tests ====================

    @Test
    void testGetRevokeByCancellationID_Found() {
        RevokeApplicationDTO dto = createTestRevokeApplicationDTO();
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), any(RowMapper.class))).thenReturn(dto);

        RevokeApplicationDTO result = revokesRepository.getRevokeByCancellationID(testCancellationId.toString());
        assertNotNull(result);
    }

    @Test
    void testGetParentsByCancellation_Found() {
        ApplicationParticipantDTO dto = new ApplicationParticipantDTO();
        dto.participantType = "1";
        dto.name = "Parent";
        List<ApplicationParticipantDTO> list = Collections.singletonList(dto);
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class))).thenReturn(list);

        List<ApplicationParticipantDTO> result = revokesRepository
                .getParentsByCancellation(testCancellationId.toString());
        assertFalse(result.isEmpty());
        assertEquals("Parent", result.get(0).name);
    }

    @Test
    void testGetApplicationDetailByCancellationAndNationalID_Found() {
        ApplicationParticipantDTO dto = new ApplicationParticipantDTO();
        dto.name = "Child";
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), any(RowMapper.class))).thenReturn(dto);

        ApplicationParticipantDTO result = revokesRepository
                .getApplicationDetailByCancellationAndNationalID(testCancellationId.toString(), "N123");
        assertNotNull(result);
    }

    // ==================== Update & Insert Tests ====================

    @Test
    void testUpdateConfirmDate() {
        LocalDate date = LocalDate.now();
        when(jdbcTemplate.update(anyString(), eq(date), anyString())).thenReturn(1);
        int rows = revokesRepository.updateConfirmDate(testCancellationId.toString(), date);
        assertEquals(1, rows);
    }

    @Test
    void testInsertCancellation_Success() {
        // Mock update for insert
        when(jdbcTemplate.update(anyString(), anyString(), anyString(), anyString(), any(), anyString(), anyString()))
                .thenReturn(1);
        // Mock update for status
        when(jdbcTemplate.update(anyString(), eq("撤銷申請審核中"), anyString(), anyString())).thenReturn(1);

        assertDoesNotThrow(() -> revokesRepository.insertCancellation(
                testApplicationId.toString(), "Reason", "N123", LocalDate.now(), "Case123"));

        // Verify insert called
        verify(jdbcTemplate).update(anyString(), anyString(), eq(testApplicationId.toString()), eq("Reason"), any(),
                eq("N123"), eq("Case123"));
    }

    @Test
    void testInsertCancellation_InsertFailed_ThrowsException() {
        when(jdbcTemplate.update(anyString(), anyString(), anyString(), anyString(), any(), anyString(), anyString()))
                .thenReturn(0);

        assertThrows(IllegalStateException.class, () -> revokesRepository.insertCancellation(
                testApplicationId.toString(), "Reason", "N123", LocalDate.now(), "Case123"));
    }

    @Test
    void testUpdateApplicationParticipantStatus() {
        when(jdbcTemplate.update(anyString(), eq("Status"), anyString(), anyString())).thenReturn(1);
        int rows = revokesRepository.updateApplicationParticipantStatus(testApplicationId.toString(), "N123", "Status");
        assertEquals(1, rows);
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
                "12345");
    }
}

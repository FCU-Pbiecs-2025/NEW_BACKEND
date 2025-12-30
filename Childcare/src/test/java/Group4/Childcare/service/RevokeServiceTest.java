package Group4.Childcare.service;

import Group4.Childcare.DTO.RevokeApplicationDTO;
import Group4.Childcare.DTO.ApplicationParticipantDTO;
import Group4.Childcare.Repository.RevokesJdbcRepository;
import Group4.Childcare.Service.RevokeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RevokeService 單元測試
 * 測試設計：等價類劃分 + 邊界值分析
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class RevokeServiceTest {

    @Mock
    private RevokesJdbcRepository revokesJdbcRepository;

    @InjectMocks
    private RevokeService revokeService;

    private UUID testCancellationId;
    private UUID testApplicationId;
    private UUID testInstitutionId;
    private RevokeApplicationDTO testRevokeDTO;

    @BeforeEach
    void setUp() {
        testCancellationId = UUID.randomUUID();
        testApplicationId = UUID.randomUUID();
        testInstitutionId = UUID.randomUUID();

        testRevokeDTO = new RevokeApplicationDTO(
                testCancellationId,
                testApplicationId,
                LocalDateTime.now(),
                UUID.randomUUID(),
                "王大明",
                testInstitutionId,
                "測試幼兒園",
                "家長主動撤銷",
                "A123456789",
                "12345");
    }

    // ===== 等價類劃分測試：getRevokedApplications =====

    @Test
    void testGetRevokedApplications_ValidParams_ReturnsData() {
        // 有效等價類：正常參數
        when(revokesJdbcRepository.findRevokedApplications(0, 10, testInstitutionId.toString(), "12345", "A123456789"))
                .thenReturn(Arrays.asList(testRevokeDTO));

        List<RevokeApplicationDTO> result = revokeService.getRevokedApplications(
                0, 10, testInstitutionId.toString(), "12345", "A123456789");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testCancellationId, result.get(0).getCancellationID());
    }

    @Test
    void testGetRevokedApplications_NullParams_ReturnsData() {
        // 無效等價類：null 參數
        when(revokesJdbcRepository.findRevokedApplications(0, 10, null, null, null))
                .thenReturn(Collections.emptyList());

        List<RevokeApplicationDTO> result = revokeService.getRevokedApplications(0, 10, null, null, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetRevokedApplications_EmptyResult() {
        // 邊界：無匹配資料
        when(revokesJdbcRepository.findRevokedApplications(anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        List<RevokeApplicationDTO> result = revokeService.getRevokedApplications(100, 10, null, null, null);

        assertTrue(result.isEmpty());
    }

    // ===== 邊界值測試：分頁參數 =====

    @Test
    void testGetRevokedApplications_EdgeCase_PageZero() {
        // 邊界：第一頁 (page=0)
        when(revokesJdbcRepository.findRevokedApplications(0, 10, null, null, null))
                .thenReturn(Arrays.asList(testRevokeDTO));

        List<RevokeApplicationDTO> result = revokeService.getRevokedApplications(0, 10, null, null, null);

        assertEquals(1, result.size());
    }

    @Test
    void testGetRevokedApplications_EdgeCase_SizeOne() {
        // 邊界：每頁一筆 (size=1)
        when(revokesJdbcRepository.findRevokedApplications(0, 1, null, null, null))
                .thenReturn(Arrays.asList(testRevokeDTO));

        List<RevokeApplicationDTO> result = revokeService.getRevokedApplications(0, 1, null, null, null);

        assertEquals(1, result.size());
    }

    // ===== getTotalRevokedApplications 測試 =====

    @Test
    void testGetTotalRevokedApplications_Success() {
        when(revokesJdbcRepository.countRevokedApplications(testInstitutionId.toString(), "12345", "A123456789"))
                .thenReturn(25L);

        long result = revokeService.getTotalRevokedApplications(
                testInstitutionId.toString(), "12345", "A123456789");

        assertEquals(25L, result);
    }

    @Test
    void testGetTotalRevokedApplications_Zero() {
        // 邊界：無資料
        when(revokesJdbcRepository.countRevokedApplications(null, null, null))
                .thenReturn(0L);

        long result = revokeService.getTotalRevokedApplications(null, null, null);

        assertEquals(0L, result);
    }

    // ===== searchRevokedApplicationsPaged 測試 =====

    @Test
    void testSearchRevokedApplicationsPaged_WithFilters() {
        when(revokesJdbcRepository.searchRevokedApplicationsPaged("12345", "A123456789", 0, 10,
                testInstitutionId.toString()))
                .thenReturn(Arrays.asList(testRevokeDTO));

        List<RevokeApplicationDTO> result = revokeService.searchRevokedApplicationsPaged(
                "12345", "A123456789", 0, 10, testInstitutionId.toString());

        assertEquals(1, result.size());
    }

    @Test
    void testSearchRevokedApplicationsPaged_NoFilters() {
        when(revokesJdbcRepository.searchRevokedApplicationsPaged(null, null, 0, 10, null))
                .thenReturn(Collections.emptyList());

        List<RevokeApplicationDTO> result = revokeService.searchRevokedApplicationsPaged(null, null, 0, 10, null);

        assertTrue(result.isEmpty());
    }

    // ===== countSearchRevokedApplications 測試 =====

    @Test
    void testCountSearchRevokedApplications_Success() {
        when(revokesJdbcRepository.countSearchRevokedApplications("12345", "A123456789", testInstitutionId.toString()))
                .thenReturn(15L);

        long result = revokeService.countSearchRevokedApplications("12345", "A123456789", testInstitutionId.toString());

        assertEquals(15L, result);
    }

    // ===== getRevokeByCancellationID 測試 =====

    @Test
    void testGetRevokeByCancellationID_Found() {
        when(revokesJdbcRepository.getRevokeByCancellationID(testCancellationId.toString()))
                .thenReturn(testRevokeDTO);

        RevokeApplicationDTO result = revokeService.getRevokeByCancellationID(testCancellationId.toString());

        assertNotNull(result);
        assertEquals(testCancellationId, result.getCancellationID());
    }

    @Test
    void testGetRevokeByCancellationID_NotFound() {
        when(revokesJdbcRepository.getRevokeByCancellationID(anyString()))
                .thenReturn(null);

        RevokeApplicationDTO result = revokeService.getRevokeByCancellationID("nonexistent");

        assertNull(result);
    }

    // ===== getParentsByCancellation 測試 =====

    @Test
    void testGetParentsByCancellation_Success() {
        ApplicationParticipantDTO parentDTO = new ApplicationParticipantDTO();
        when(revokesJdbcRepository.getParentsByCancellation(testCancellationId.toString()))
                .thenReturn(Arrays.asList(parentDTO));

        List<ApplicationParticipantDTO> result = revokeService.getParentsByCancellation(testCancellationId.toString());

        assertEquals(1, result.size());
    }

    @Test
    void testGetParentsByCancellation_Empty() {
        when(revokesJdbcRepository.getParentsByCancellation(anyString()))
                .thenReturn(Collections.emptyList());

        List<ApplicationParticipantDTO> result = revokeService.getParentsByCancellation("any");

        assertTrue(result.isEmpty());
    }

    // ===== getApplicationDetailByCancellationAndNationalID 測試 =====

    @Test
    void testGetApplicationDetailByCancellationAndNationalID_Found() {
        ApplicationParticipantDTO detailDTO = new ApplicationParticipantDTO();
        when(revokesJdbcRepository.getApplicationDetailByCancellationAndNationalID(
                testCancellationId.toString(), "A123456789"))
                .thenReturn(detailDTO);

        ApplicationParticipantDTO result = revokeService.getApplicationDetailByCancellationAndNationalID(
                testCancellationId.toString(), "A123456789");

        assertNotNull(result);
    }

    @Test
    void testGetApplicationDetailByCancellationAndNationalID_NotFound() {
        when(revokesJdbcRepository.getApplicationDetailByCancellationAndNationalID(anyString(), anyString()))
                .thenReturn(null);

        ApplicationParticipantDTO result = revokeService.getApplicationDetailByCancellationAndNationalID("x", "y");

        assertNull(result);
    }

    // ===== updateConfirmDate 測試 =====

    @Test
    void testUpdateConfirmDate_Success() {
        when(revokesJdbcRepository.updateConfirmDate(testCancellationId.toString(), LocalDate.now()))
                .thenReturn(1);

        int result = revokeService.updateConfirmDate(testCancellationId.toString(), LocalDate.now());

        assertEquals(1, result);
    }

    @Test
    void testUpdateConfirmDate_NotFound() {
        when(revokesJdbcRepository.updateConfirmDate(anyString(), any()))
                .thenReturn(0);

        int result = revokeService.updateConfirmDate("nonexistent", LocalDate.now());

        assertEquals(0, result);
    }

    // ===== updateApplicationParticipantStatus 測試 =====

    @Test
    void testUpdateApplicationParticipantStatus_Success() {
        when(revokesJdbcRepository.updateApplicationParticipantStatus(
                testApplicationId.toString(), "A123456789", "已撤銷"))
                .thenReturn(1);

        int result = revokeService.updateApplicationParticipantStatus(
                testApplicationId.toString(), "A123456789", "已撤銷");

        assertEquals(1, result);
    }

    @Test
    void testUpdateApplicationParticipantStatus_NotFound() {
        when(revokesJdbcRepository.updateApplicationParticipantStatus(anyString(), anyString(), anyString()))
                .thenReturn(0);

        int result = revokeService.updateApplicationParticipantStatus("x", "y", "z");

        assertEquals(0, result);
    }

    // ===== createCancellation 測試 =====

    @Test
    void testCreateCancellation_Success() {
        doNothing().when(revokesJdbcRepository).insertCancellation(
                anyString(), anyString(), anyString(), any(LocalDate.class), anyString());

        // 不拋異常即為成功
        assertDoesNotThrow(() -> revokeService.createCancellation(
                testApplicationId.toString(), "家長主動撤銷", "A123456789", "12345"));

        verify(revokesJdbcRepository, times(1)).insertCancellation(
                eq(testApplicationId.toString()), eq("家長主動撤銷"), eq("A123456789"), any(LocalDate.class), eq("12345"));
    }
}

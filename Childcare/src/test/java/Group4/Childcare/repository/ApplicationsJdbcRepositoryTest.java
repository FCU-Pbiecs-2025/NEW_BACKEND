package Group4.Childcare.repository;

import Group4.Childcare.Model.Applications;
import Group4.Childcare.Repository.ApplicationsJdbcRepository;
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
 * ApplicationsJdbcRepository 單元測試
 * 測試案件管理相關的資料庫操作
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ApplicationsJdbcRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ApplicationsJdbcRepository repository;

    private UUID testApplicationId;
    private UUID testInstitutionId;
    private UUID testUserId;
    private Applications testApplication;

    @BeforeEach
    void setUp() {
        testApplicationId = UUID.randomUUID();
        testInstitutionId = UUID.randomUUID();
        testUserId = UUID.randomUUID();

        testApplication = new Applications();
        testApplication.setApplicationID(testApplicationId);
        testApplication.setApplicationDate(LocalDate.now());
        testApplication.setCaseNumber(202412140001L);
        testApplication.setInstitutionID(testInstitutionId);
        testApplication.setUserID(testUserId);
        testApplication.setIdentityType((byte) 1);
    }

    // ===== 測試 save (新增案件) =====
    @Test
    void testSave_Success() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(1); // exists

        // Mock findById to return the application for update
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(testApplication);

        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        // When
        Applications result = repository.save(testApplication);

        // Then
        assertNotNull(result);
        assertEquals(testApplicationId, result.getApplicationID());
    }

    @Test
    void testSave_ThrowsException_WhenInsertFails() {
        // Given
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> repository.save(testApplication));
        verify(jdbcTemplate, times(1)).update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    // ===== 測試 getUserEmailByApplicationId (查詢使用者郵箱) =====
    @Test
    void testGetUserEmailByApplicationId_Success() {
        // Given
        String expectedEmail = "test@example.com";
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), anyString()))
                .thenReturn(expectedEmail);

        // When
        Optional<String> result = repository.getUserEmailByApplicationId(testApplicationId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(expectedEmail, result.get());
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(String.class), anyString());
    }

    @Test
    void testGetUserEmailByApplicationId_ReturnsEmpty_WhenNotFound() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), anyString()))
                .thenThrow(new RuntimeException("No data found"));

        // When
        Optional<String> result = repository.getUserEmailByApplicationId(testApplicationId);

        // Then
        assertFalse(result.isPresent());
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(String.class), anyString());
    }

    // ===== 測試 updateParticipantStatusReason (更新參與者狀態) =====
    @Test
    void testUpdateParticipantStatusReason_FromReviewToWaitlist_AssignsNewOrder() {
        // Given - 從審核中變為候補中
        String nationalID = "A123456789";
        String newStatus = "候補中";
        String reason = "符合資格";
        LocalDateTime reviewDate = LocalDateTime.now();

        // Mock 查詢當前狀態
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "審核中");
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("ParticipantType", 0);
        when(jdbcTemplate.queryForList(anyString(), anyString(), anyString()))
                .thenReturn(Collections.singletonList(currentInfo));

        // Mock 查詢 InstitutionID
        when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class), anyString()))
                .thenReturn(testInstitutionId.toString());

        // Mock 查詢最大 CurrentOrder
        when(jdbcTemplate.queryForObject(contains("SELECT MAX"), eq(Integer.class), anyString()))
                .thenReturn(5);

        // Mock 更新操作
        when(jdbcTemplate.update(contains("UPDATE application_participants SET Status"),
                anyString(), anyString(), any(), anyInt(), anyString(), anyString()))
                .thenReturn(1);

        // When
        repository.updateParticipantStatusReason(testApplicationId, nationalID, newStatus, reason, reviewDate);

        // Then
        verify(jdbcTemplate, times(1)).queryForList(anyString(), anyString(), anyString());
        verify(jdbcTemplate, times(1)).queryForObject(contains("SELECT InstitutionID"), eq(String.class), anyString());
        verify(jdbcTemplate, times(1)).queryForObject(contains("SELECT MAX"), eq(Integer.class), anyString());
        verify(jdbcTemplate, times(1)).update(contains("UPDATE application_participants SET Status"),
                anyString(), anyString(), any(), eq(6), anyString(), anyString());
    }

    @Test
    void testUpdateParticipantStatusReason_FromWaitlistToAdmitted_RemovesOrderAndBackfill() {
        // Given - 從候補中變為已錄取
        String nationalID = "A123456789";
        String newStatus = "已錄取";
        String reason = "正取";
        LocalDateTime reviewDate = LocalDateTime.now();

        // Mock 查詢當前狀態
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "候補中");
        currentInfo.put("CurrentOrder", 3);
        currentInfo.put("ParticipantType", 0);
        when(jdbcTemplate.queryForList(anyString(), anyString(), anyString()))
                .thenReturn(Collections.singletonList(currentInfo));

        // Mock 查詢 InstitutionID
        when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class), anyString()))
                .thenReturn(testInstitutionId.toString());

        // Mock 遞補更新
        when(jdbcTemplate.update(anyString(), anyInt(), anyString()))
                .thenReturn(3);

        // Mock 最終狀態更新
        when(jdbcTemplate.update(anyString(), anyString(), anyString(), any(), any(), anyString(), anyString()))
                .thenReturn(1);

        // When
        repository.updateParticipantStatusReason(testApplicationId, nationalID, newStatus, reason, reviewDate);

        // Then
        verify(jdbcTemplate, times(1)).queryForList(anyString(), anyString(), anyString());
        verify(jdbcTemplate, times(1)).queryForObject(contains("SELECT InstitutionID"), eq(String.class), anyString());
        verify(jdbcTemplate, times(1)).update(contains("SET CurrentOrder = CurrentOrder - 1"), anyInt(), anyString());
        verify(jdbcTemplate, times(1)).update(contains("UPDATE application_participants SET Status"),
                anyString(), anyString(), any(), isNull(), anyString(), anyString());
    }

    // ===== 測試 count (計算總數) =====
    @Test
    void testCount_ReturnsCorrectCount() {
        // Given
        Long expectedCount = 25L;
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(expectedCount);

        // When
        long result = repository.count();

        // Then
        assertEquals(expectedCount, result);
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(Long.class));
    }

    // ===== 測試 existsById (檢查是否存在) =====
    @Test
    void testExistsById_ReturnsTrue_WhenExists() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(1);

        // When
        boolean result = repository.existsById(testApplicationId);

        // Then
        assertTrue(result);
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(Integer.class), anyString());
    }

    @Test
    void testExistsById_ReturnsFalse_WhenNotExists() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(0);

        // When
        boolean result = repository.existsById(testApplicationId);

        // Then
        assertFalse(result);
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(Integer.class), anyString());
    }

    // ===== 測試 deleteById (刪除案件) =====
    @Test
    void testDeleteById_Success() {
        // Given
        when(jdbcTemplate.update(anyString(), anyString()))
                .thenReturn(1);

        // When
        repository.deleteById(testApplicationId);

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), anyString());
    }

    // ===== 測試邊界情況 =====
    @Test
    void testUpdateParticipantStatusReason_WithNullReason() {
        // Given
        String nationalID = "A123456789";
        String newStatus = "已錄取";
        LocalDateTime reviewDate = LocalDateTime.now();

        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "候補中");
        currentInfo.put("CurrentOrder", 1);
        currentInfo.put("ParticipantType", 0);
        when(jdbcTemplate.queryForList(anyString(), anyString(), anyString()))
                .thenReturn(Collections.singletonList(currentInfo));

        when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class), anyString()))
                .thenReturn(testInstitutionId.toString());

        when(jdbcTemplate.update(anyString(), anyInt(), anyString()))
                .thenReturn(0);

        when(jdbcTemplate.update(contains("UPDATE application_participants SET Status"),
                anyString(), isNull(), any(), isNull(), anyString(), anyString()))
                .thenReturn(1);

        // When
        repository.updateParticipantStatusReason(testApplicationId, nationalID, newStatus, null, reviewDate);

        // Then
        verify(jdbcTemplate, times(1)).update(contains("UPDATE application_participants SET Status"),
                anyString(), isNull(), any(), isNull(), anyString(), anyString());
    }

    @Test
    void testGetUserEmailByApplicationId_WithNullEmail() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), anyString()))
                .thenReturn(null);

        // When
        Optional<String> result = repository.getUserEmailByApplicationId(testApplicationId);

        // Then
        assertFalse(result.isPresent());
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(String.class), anyString());
    }
}


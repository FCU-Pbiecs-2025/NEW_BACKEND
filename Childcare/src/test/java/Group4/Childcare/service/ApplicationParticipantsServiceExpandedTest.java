package Group4.Childcare.service;

import Group4.Childcare.Model.ApplicationParticipants;
import Group4.Childcare.Repository.ApplicationParticipantsJdbcRepository;
import Group4.Childcare.Repository.ApplicationsJdbcRepository;
import Group4.Childcare.Service.ApplicationParticipantsService;
import Group4.Childcare.Service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ApplicationParticipantsService 擴展測試
 * 
 * 專注於提高分支覆蓋率的測試用例:
 * 1. cancelApplicationWithOrderRecalculation() 的各種情境
 * 2. updateParticipantWithDynamicOrder() 的錯誤處理和邊界情況
 * 3. countApplicationsByChildNationalID()
 * 4. 各種異常處理路徑
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ApplicationParticipantsServiceExpandedTest {

    @Mock
    private ApplicationParticipantsJdbcRepository repository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ApplicationsJdbcRepository applicationsJdbcRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ApplicationParticipantsService service;

    private UUID testApplicationId;
    private String testNationalId;
    private ApplicationParticipants testParticipant;

    @BeforeEach
    void setUp() {
        testApplicationId = UUID.randomUUID();
        testNationalId = "A123456789";

        testParticipant = new ApplicationParticipants();
        testParticipant.setApplicationID(testApplicationId);
        testParticipant.setNationalID(testNationalId);
        testParticipant.setName("測試幼兒");
        testParticipant.setStatus("候補中");
        testParticipant.setParticipantType(false); // 幼兒

        // Inject dependencies
        ReflectionTestUtils.setField(service, "jdbcTemplate", jdbcTemplate);
        ReflectionTestUtils.setField(service, "emailService", emailService);
        ReflectionTestUtils.setField(service, "applicationsJdbcRepository", applicationsJdbcRepository);
    }

    // ========== cancelApplicationWithOrderRecalculation Tests ==========

    @Test
    void testCancelApplication_WithCurrentOrder_Success() {
        // Given - 有候補序號的申請案
        UUID institutionId = UUID.randomUUID();

        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("CurrentOrder", 5);
        currentInfo.put("ParticipantType", 0); // 幼兒
        currentInfo.put("Status", "候補中");

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class),
                eq(testApplicationId.toString())))
                .thenReturn(institutionId.toString());

        when(jdbcTemplate.update(contains("SET CurrentOrder = CurrentOrder - 1"), eq(5), eq(institutionId.toString())))
                .thenReturn(3); // 影響3筆記錄

        when(jdbcTemplate.update(contains("SET Status = '撤銷申請通過'"), anyString(), any(),
                eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(1);

        testParticipant.setStatus("撤銷申請通過");
        testParticipant.setCurrentOrder(null);
        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        ApplicationParticipants result = service.cancelApplicationWithOrderRecalculation(
                testApplicationId, testNationalId, "使用者撤銷");

        // Then
        assertNotNull(result);
        assertEquals("撤銷申請通過", result.getStatus());
        assertNull(result.getCurrentOrder());
        verify(jdbcTemplate).update(contains("SET CurrentOrder = CurrentOrder - 1"), eq(5),
                eq(institutionId.toString()));
    }

    @Test
    void testCancelApplication_WithoutCurrentOrder_Success() {
        // Given - 沒有候補序號的申請案
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("ParticipantType", 0);
        currentInfo.put("Status", "待審核");

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.update(contains("SET Status = '撤銷申請通過'"), anyString(), any(),
                eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(1);

        testParticipant.setStatus("撤銷申請通過");
        testParticipant.setCurrentOrder(null);
        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        ApplicationParticipants result = service.cancelApplicationWithOrderRecalculation(
                testApplicationId, testNationalId, "使用者撤銷");

        // Then
        assertNotNull(result);
        assertEquals("撤銷申請通過", result.getStatus());
        // 不應該執行遞補操作
        verify(jdbcTemplate, never()).update(contains("SET CurrentOrder = CurrentOrder - 1"), anyInt(), anyString());
    }

    @Test
    void testCancelApplication_NonChildParticipant_NoRecalculation() {
        // Given - 非幼兒參與者(家長)
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("CurrentOrder", 5);
        currentInfo.put("ParticipantType", 1); // 家長
        currentInfo.put("Status", "候補中");

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.update(contains("SET Status = '撤銷申請通過'"), anyString(), any(),
                eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(1);

        testParticipant.setStatus("撤銷申請通過");
        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        ApplicationParticipants result = service.cancelApplicationWithOrderRecalculation(
                testApplicationId, testNationalId, "使用者撤銷");

        // Then
        assertNotNull(result);
        // 非幼兒不應該執行遞補
        verify(jdbcTemplate, never()).update(contains("SET CurrentOrder = CurrentOrder - 1"), anyInt(), anyString());
    }

    @Test
    void testCancelApplication_QueryError_ThrowsException() {
        // Given - 查詢失敗
        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenThrow(new RuntimeException("Database connection error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.cancelApplicationWithOrderRecalculation(testApplicationId, testNationalId, "測試");
        });

        assertTrue(exception.getMessage().contains("無法查詢申請案資料"));
    }

    @Test
    void testCancelApplication_RecalculationError_ThrowsException() {
        // Given - 遞補操作失敗
        UUID institutionId = UUID.randomUUID();

        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("CurrentOrder", 5);
        currentInfo.put("ParticipantType", 0);
        currentInfo.put("Status", "候補中");

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class),
                eq(testApplicationId.toString())))
                .thenReturn(institutionId.toString());

        when(jdbcTemplate.update(contains("SET CurrentOrder = CurrentOrder - 1"), eq(5), eq(institutionId.toString())))
                .thenThrow(new RuntimeException("Update failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.cancelApplicationWithOrderRecalculation(testApplicationId, testNationalId, "測試");
        });

        assertTrue(exception.getMessage().contains("遞補候補序號失敗"));
    }

    @Test
    void testCancelApplication_ParticipantNotFoundAfterUpdate_ThrowsException() {
        // Given
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("ParticipantType", 0);
        currentInfo.put("Status", "待審核");

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.update(contains("SET Status = '撤銷申請通過'"), anyString(), any(),
                eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(1);

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(Collections.emptyList());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.cancelApplicationWithOrderRecalculation(testApplicationId, testNationalId, "測試");
        });

        assertTrue(exception.getMessage().contains("Participant not found after cancel"));
    }

    // ========== updateParticipantWithDynamicOrder Additional Tests ==========

    @Test
    void testUpdateDynamicOrder_ChangeToWaitlisted_FirstApplicant() {
        // Given - 第一個候補者(maxOrder = null)
        UUID institutionId = UUID.randomUUID();

        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "待審核");
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("ParticipantType", 0);

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class),
                eq(testApplicationId.toString())))
                .thenReturn(institutionId.toString());

        when(jdbcTemplate.queryForObject(contains("SELECT MAX(ap.CurrentOrder)"), eq(Integer.class),
                eq(institutionId.toString())))
                .thenReturn(null); // 沒有現有的候補者

        when(jdbcTemplate.update(contains("UPDATE application_participants SET Status = ?"), any(), any(), any(), any(),
                any(), any()))
                .thenReturn(1);

        testParticipant.setStatus("候補中");
        testParticipant.setCurrentOrder(1);
        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        ApplicationParticipants result = service.updateParticipantWithDynamicOrder(
                testApplicationId, testNationalId, "候補中", "符合候補資格", null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getCurrentOrder()); // 第一個候補者應該是1
    }

    @Test
    void testUpdateDynamicOrder_NonChildParticipant_NoCurrentOrder() {
        // Given - 非幼兒參與者改為候補中
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "待審核");
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("ParticipantType", 1); // 家長

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.update(contains("UPDATE application_participants SET Status = ?"), any(), any(), any(), any(),
                any(), any()))
                .thenReturn(1);

        testParticipant.setStatus("候補中");
        testParticipant.setCurrentOrder(null);
        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        ApplicationParticipants result = service.updateParticipantWithDynamicOrder(
                testApplicationId, testNationalId, "候補中", "符合候補資格", null);

        // Then
        assertNotNull(result);
        assertNull(result.getCurrentOrder()); // 非幼兒不應該有CurrentOrder
    }

    @Test
    void testUpdateDynamicOrder_QueryCurrentInfoError_ContinuesExecution() {
        // Given - 查詢當前資訊失敗,但繼續執行
        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenThrow(new RuntimeException("Query failed"));

        when(jdbcTemplate.update(contains("UPDATE application_participants SET Status = ?"), any(), any(), any(), any(),
                any(), any()))
                .thenReturn(1);

        testParticipant.setStatus("已錄取");
        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        ApplicationParticipants result = service.updateParticipantWithDynamicOrder(
                testApplicationId, testNationalId, "已錄取", "符合資格", null);

        // Then
        assertNotNull(result);
        assertEquals("已錄取", result.getStatus());
    }

    @Test
    void testUpdateDynamicOrder_GetInstitutionIdError_ContinuesWithoutOrder() {
        // Given - 無法獲取InstitutionID
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "待審核");
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("ParticipantType", 0);

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class),
                eq(testApplicationId.toString())))
                .thenThrow(new RuntimeException("Institution not found"));

        when(jdbcTemplate.update(contains("UPDATE application_participants SET Status = ?"), any(), any(), any(), any(),
                any(), any()))
                .thenReturn(1);

        testParticipant.setStatus("候補中");
        testParticipant.setCurrentOrder(null);
        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        ApplicationParticipants result = service.updateParticipantWithDynamicOrder(
                testApplicationId, testNationalId, "候補中", "符合候補資格", null);

        // Then
        assertNotNull(result);
        assertNull(result.getCurrentOrder()); // 無法獲取InstitutionID時不設置CurrentOrder
    }

    @Test
    void testUpdateDynamicOrder_WithClassID_UpdatesClassID() {
        // Given - 提供ClassID
        UUID classId = UUID.randomUUID();

        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "待審核");
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("ParticipantType", 0);

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.update(contains("UPDATE application_participants SET Status = ?"), any(), any(), any(), any(),
                any(), any()))
                .thenReturn(1);

        when(jdbcTemplate.update(contains("SET ClassID = ?"), eq(classId.toString()), eq(testApplicationId.toString()),
                eq(testNationalId)))
                .thenReturn(1);

        testParticipant.setStatus("已錄取");
        testParticipant.setClassID(classId);
        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        ApplicationParticipants result = service.updateParticipantWithDynamicOrder(
                testApplicationId, testNationalId, "已錄取", "符合資格", classId);

        // Then
        assertNotNull(result);
        assertEquals(classId, result.getClassID());
        verify(jdbcTemplate).update(contains("SET ClassID = ?"), eq(classId.toString()),
                eq(testApplicationId.toString()), eq(testNationalId));
    }

    @Test
    void testUpdateDynamicOrder_ParticipantNotFoundAfterUpdate_ThrowsException() {
        // Given
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "待審核");
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("ParticipantType", 0);

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.update(contains("UPDATE application_participants SET Status = ?"), any(), any(), any(), any(),
                any(), any()))
                .thenReturn(1);

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(Collections.emptyList());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.updateParticipantWithDynamicOrder(
                    testApplicationId, testNationalId, "已錄取", "符合資格", null);
        });

        assertTrue(exception.getMessage().contains("Participant not found after update"));
    }

    @Test
    void testUpdateDynamicOrder_ParticipantTypeAsBoolean_HandlesCorrectly() {
        // Given - ParticipantType 是 Boolean 類型
        UUID institutionId = UUID.randomUUID();

        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "待審核");
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("ParticipantType", Boolean.FALSE); // Boolean false = 幼兒

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class),
                eq(testApplicationId.toString())))
                .thenReturn(institutionId.toString());

        when(jdbcTemplate.queryForObject(contains("SELECT MAX(ap.CurrentOrder)"), eq(Integer.class),
                eq(institutionId.toString())))
                .thenReturn(10);

        when(jdbcTemplate.update(contains("UPDATE application_participants SET Status = ?"), any(), any(), any(), any(),
                any(), any()))
                .thenReturn(1);

        testParticipant.setStatus("候補中");
        testParticipant.setCurrentOrder(11);
        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        ApplicationParticipants result = service.updateParticipantWithDynamicOrder(
                testApplicationId, testNationalId, "候補中", "符合候補資格", null);

        // Then
        assertNotNull(result);
        assertEquals(11, result.getCurrentOrder());
    }

    // ========== countApplicationsByChildNationalID Test ==========

    @Test
    void testCountApplicationsByChildNationalID_Success() {
        // Given
        String childNationalId = "B987654321";
        when(repository.countApplicationsByChildNationalID(childNationalId)).thenReturn(3);

        // When
        int count = service.countApplicationsByChildNationalID(childNationalId);

        // Then
        assertEquals(3, count);
        verify(repository).countApplicationsByChildNationalID(childNationalId);
    }

    @Test
    void testCountApplicationsByChildNationalID_NoApplications() {
        // Given
        String childNationalId = "C111222333";
        when(repository.countApplicationsByChildNationalID(childNationalId)).thenReturn(0);

        // When
        int count = service.countApplicationsByChildNationalID(childNationalId);

        // Then
        assertEquals(0, count);
        verify(repository).countApplicationsByChildNationalID(childNationalId);
    }
}

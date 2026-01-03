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

    @Test
    void testCancelApplication_UpdateStatusError_ThrowsException() {
        // Given
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("ParticipantType", 0);
        currentInfo.put("Status", "待審核");

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.update(contains("SET Status = '撤銷申請通過'"), anyString(), any(),
                eq(testApplicationId.toString()), eq(testNationalId)))
                .thenThrow(new RuntimeException("Update failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.cancelApplicationWithOrderRecalculation(testApplicationId, testNationalId, "測試");
        });

        assertTrue(exception.getMessage().contains("撤銷申請案失敗"));
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

    // ========== sendStatusChangeEmail Tests ==========

    @Test
    void testSendStatusChangeEmail_Success() throws Exception {
        // Given
        String email = "test@example.com";
        when(applicationsJdbcRepository.getUserEmailByApplicationId(testApplicationId))
                .thenReturn(Optional.of(email));

        Map<String, Object> caseInfo = new HashMap<>();
        caseInfo.put("ApplicantName", "家長");
        caseInfo.put("ChildName", "小明");
        caseInfo.put("InstitutionName", "機構");
        caseInfo.put("CaseNumber", 12345L);
        caseInfo.put("ApplicationDate", "2023-01-01");

        when(jdbcTemplate.queryForMap(contains("SELECT"), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(caseInfo);

        // When
        ReflectionTestUtils.invokeMethod(service, "sendStatusChangeEmail", testApplicationId, testNationalId, "已錄取", "理由", null);

        // Then
        verify(emailService).sendApplicationStatusChangeEmail(
                eq(email), eq("家長"), eq("小明"), eq("機構"), eq(12345L), anyString(), eq("已錄取"), any(), eq("理由"));
    }

    @Test
    void testSendStatusChangeEmail_NoEmail() throws Exception {
        // Given
        when(applicationsJdbcRepository.getUserEmailByApplicationId(testApplicationId))
                .thenReturn(Optional.empty());

        // When
        ReflectionTestUtils.invokeMethod(service, "sendStatusChangeEmail", testApplicationId, testNationalId, "已錄取", "理由", null);

        // Then
        verify(emailService, never()).sendApplicationStatusChangeEmail(anyString(), anyString(), anyString(), anyString(), any(), any(), anyString(), any(), anyString());
    }

    @Test
    void testSendStatusChangeEmail_EmailServiceNull() {
        // Given
        ReflectionTestUtils.setField(service, "emailService", null);

        // When
        ReflectionTestUtils.invokeMethod(service, "sendStatusChangeEmail", testApplicationId, testNationalId, "已錄取", "理由", null);

        // Then
        // Should return early without exception
    }

    @Test
    void testSendStatusChangeEmail_Exception() {
        // Given
        when(applicationsJdbcRepository.getUserEmailByApplicationId(any()))
                .thenThrow(new RuntimeException("DB Error"));

        // When
        ReflectionTestUtils.invokeMethod(service, "sendStatusChangeEmail", testApplicationId, testNationalId, "已錄取", "理由", null);

        // Then
        // Should catch exception and log it
    }

    // ========== updateClassStudentCount Tests ==========

    @Test
    void testUpdateClassStudentCount_AdmittedToNotAdmitted() {
        // Given
        UUID classId = UUID.randomUUID();
        
        // When
        ReflectionTestUtils.invokeMethod(service, "updateClassStudentCount", testApplicationId, testNationalId, "已錄取", "已撤銷", classId);

        // Then
        verify(jdbcTemplate).update(anyString(), eq(-1), eq(classId.toString()));
    }

    @Test
    void testUpdateClassStudentCount_NotAdmittedToAdmitted() {
        // Given
        UUID classId = UUID.randomUUID();
        
        // When
        ReflectionTestUtils.invokeMethod(service, "updateClassStudentCount", testApplicationId, testNationalId, "待審核", "已錄取", classId);

        // Then
        verify(jdbcTemplate).update(anyString(), eq(1), eq(classId.toString()));
    }

    @Test
    void testUpdateClassStudentCount_NoChange() {
        // Given
        UUID classId = UUID.randomUUID();
        
        // When
        ReflectionTestUtils.invokeMethod(service, "updateClassStudentCount", testApplicationId, testNationalId, "待審核", "候補中", classId);

        // Then
        verify(jdbcTemplate, never()).update(anyString(), anyInt(), anyString());
    }

    @Test
    void testUpdateClassStudentCount_ClassIdNull() {
        // When
        ReflectionTestUtils.invokeMethod(service, "updateClassStudentCount", testApplicationId, testNationalId, "待審核", "已錄取", null);

        // Then
        verify(jdbcTemplate, never()).update(anyString(), anyInt(), anyString());
    }

    @Test
    void testUpdateClassStudentCount_Exception() {
        // Given
        UUID classId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), anyInt(), anyString())).thenThrow(new RuntimeException("Update failed"));

        // When
        ReflectionTestUtils.invokeMethod(service, "updateClassStudentCount", testApplicationId, testNationalId, "待審核", "已錄取", classId);

        // Then
        // Should catch exception
    }

    // ========== updateParticipantWithDynamicOrder Edge Cases ==========

    @Test
    void testUpdateDynamicOrder_WaitlistToWaitlist_IsChild() {
        // Given
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "候補中");
        currentInfo.put("CurrentOrder", 5);
        currentInfo.put("ParticipantType", 0);

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        service.updateParticipantWithDynamicOrder(testApplicationId, testNationalId, "候補中", "理由", null);

        // Then
        // Should not call MAX(CurrentOrder) or shift orders
        verify(jdbcTemplate, never()).queryForObject(contains("MAX"), eq(Integer.class), anyString());
        verify(jdbcTemplate, never()).update(contains("CurrentOrder - 1"), anyInt(), anyString());
    }

    @Test
    void testUpdateDynamicOrder_WaitlistToNotWaitlist_IsChild_NullOrder() {
        // Given
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "候補中");
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("ParticipantType", 0);

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        service.updateParticipantWithDynamicOrder(testApplicationId, testNationalId, "已錄取", "理由", null);

        // Then
        // Should not call shift orders because oldOrder was null
        verify(jdbcTemplate, never()).update(contains("CurrentOrder - 1"), anyInt(), anyString());
    }

    @Test
    void testSendStatusChangeEmail_NullCaseInfoFields() throws Exception {
        // Given
        String email = "test@example.com";
        when(applicationsJdbcRepository.getUserEmailByApplicationId(testApplicationId))
                .thenReturn(Optional.of(email));

        Map<String, Object> caseInfo = new HashMap<>();
        caseInfo.put("ApplicantName", null);
        caseInfo.put("ChildName", null);
        caseInfo.put("InstitutionName", null);
        caseInfo.put("CaseNumber", null);
        caseInfo.put("ApplicationDate", null);

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(caseInfo);

        // When
        ReflectionTestUtils.invokeMethod(service, "sendStatusChangeEmail", testApplicationId, testNationalId, "已錄取", "理由", null);

        // Then
        verify(emailService).sendApplicationStatusChangeEmail(
                eq(email), eq(""), eq(""), eq(""), isNull(), eq(""), eq("已錄取"), any(), eq("理由"));
    }

    @Test
    void testCancelApplication_InstitutionIdNull() {
        // Given
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("CurrentOrder", 5);
        currentInfo.put("ParticipantType", 0);
        currentInfo.put("Status", "候補中");

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class), eq(testApplicationId.toString())))
                .thenReturn(null);

        when(jdbcTemplate.update(contains("SET Status = '撤銷申請通過'"), anyString(), any(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(1);

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        service.cancelApplicationWithOrderRecalculation(testApplicationId, testNationalId, "理由");

        // Then
        // Should not call shift orders because institutionId was null
        verify(jdbcTemplate, never()).update(contains("CurrentOrder - 1"), anyInt(), anyString());
    }

    @Test
    void testUpdateDynamicOrder_IsChildNull() {
        // Given
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "待審核");
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("ParticipantType", null); // isChild will be null

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        service.updateParticipantWithDynamicOrder(testApplicationId, testNationalId, "候補中", "理由", null);

        // Then
        // Should not call MAX(CurrentOrder) because isChild is null
        verify(jdbcTemplate, never()).queryForObject(contains("MAX"), eq(Integer.class), anyString());
    }

    @Test
    void testUpdateDynamicOrder_WaitlistToAdmitted_InstitutionIdNull() {
        // Given
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "候補中");
        currentInfo.put("CurrentOrder", 3);
        currentInfo.put("ParticipantType", 0);

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class), eq(testApplicationId.toString())))
                .thenReturn(null);

        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        service.updateParticipantWithDynamicOrder(testApplicationId, testNationalId, "已錄取", "理由", null);

        // Then
        verify(jdbcTemplate, never()).update(contains("CurrentOrder - 1"), anyInt(), anyString());
    }

    @Test
    void testUpdateDynamicOrder_WaitlistToAdmitted_InstitutionIdQueryError() {
        // Given
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "候補中");
        currentInfo.put("CurrentOrder", 3);
        currentInfo.put("ParticipantType", 0);

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class), eq(testApplicationId.toString())))
                .thenThrow(new RuntimeException("Query error"));

        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        service.updateParticipantWithDynamicOrder(testApplicationId, testNationalId, "已錄取", "理由", null);

        // Then
        verify(jdbcTemplate, never()).update(contains("CurrentOrder - 1"), anyInt(), anyString());
    }

    @Test
    void testUpdateDynamicOrder_WaitlistToAdmitted_RecalculationError() {
        // Given
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "候補中");
        currentInfo.put("CurrentOrder", 3);
        currentInfo.put("ParticipantType", 0);

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class), eq(testApplicationId.toString())))
                .thenReturn(UUID.randomUUID().toString());

        when(jdbcTemplate.update(contains("CurrentOrder - 1"), anyInt(), anyString()))
                .thenThrow(new RuntimeException("Update error"));

        when(jdbcTemplate.update(contains("UPDATE application_participants SET Status = ?"), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        service.updateParticipantWithDynamicOrder(testApplicationId, testNationalId, "已錄取", "理由", null);

        // Then
        // Should catch exception and continue
        verify(jdbcTemplate).update(contains("UPDATE application_participants SET Status = ?"), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testUpdateDynamicOrder_UpdateStatusError() {
        // Given
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "待審核");
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("ParticipantType", 0);

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.update(contains("UPDATE application_participants SET Status = ?"), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Update error"));

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        service.updateParticipantWithDynamicOrder(testApplicationId, testNationalId, "已錄取", "理由", null);

        // Then
        // Should catch exception and continue
        verify(repository).findByApplicationIDAndNationalID(testApplicationId, testNationalId);
    }

    @Test
    void testUpdateDynamicOrder_UpdateClassIdError() {
        // Given
        UUID classId = UUID.randomUUID();
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "待審核");
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("ParticipantType", 0);

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.update(contains("SET ClassID = ?"), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Update error"));

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        service.updateParticipantWithDynamicOrder(testApplicationId, testNationalId, "已錄取", "理由", classId);

        // Then
        // Should catch exception and continue
        verify(repository).findByApplicationIDAndNationalID(testApplicationId, testNationalId);
    }

    @Test
    void testUpdateDynamicOrder_ParticipantNotFound_ThrowsException() {
        // Given
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "待審核");
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("ParticipantType", 0);

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(Collections.emptyList());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            service.updateParticipantWithDynamicOrder(testApplicationId, testNationalId, "已錄取", "理由", null);
        });
    }

    @Test
    void testUpdateDynamicOrder_ParticipantTypeBoolean_True() {
        // Given - ParticipantType is Boolean.TRUE (isChild = !true = false)
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "待審核");
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("ParticipantType", Boolean.TRUE);

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        service.updateParticipantWithDynamicOrder(testApplicationId, testNationalId, "候補中", "理由", null);

        // Then
        verify(jdbcTemplate, never()).queryForObject(contains("InstitutionID"), eq(String.class), anyString());
    }

    @Test
    void testCancelApplication_ParticipantTypeBoolean_True() {
        // Given - ParticipantType is Boolean.TRUE (isChild = !true = false)
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("CurrentOrder", 5);
        currentInfo.put("ParticipantType", Boolean.TRUE);
        currentInfo.put("Status", "候補中");

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        service.cancelApplicationWithOrderRecalculation(testApplicationId, testNationalId, "理由");

        // Then
        verify(jdbcTemplate, never()).queryForObject(contains("InstitutionID"), eq(String.class), anyString());
    }

    @Test
    void testCancelApplication_InstitutionIdQueryError() {
        // Given
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("CurrentOrder", 5);
        currentInfo.put("ParticipantType", 0);
        currentInfo.put("Status", "候補中");

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class), eq(testApplicationId.toString())))
                .thenThrow(new RuntimeException("Query error"));

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        service.cancelApplicationWithOrderRecalculation(testApplicationId, testNationalId, "理由");

        // Then
        verify(jdbcTemplate, never()).update(contains("CurrentOrder - 1"), anyInt(), anyString());
    }

    @Test
    void testUpdateClassStudentCount_ClassIdQueryError() {
        // Given
        when(jdbcTemplate.queryForObject(contains("SELECT ClassID"), eq(String.class), anyString(), anyString()))
                .thenThrow(new RuntimeException("Query error"));

        // When
        ReflectionTestUtils.invokeMethod(service, "updateClassStudentCount", testApplicationId, testNationalId, "待審核", "已錄取", null);

        // Then
        verify(jdbcTemplate, never()).update(contains("UPDATE classes"), anyInt(), anyString());
    }

    @Test
    void testUpdateClassStudentCount_ClassIdEmpty() {
        // Given
        when(jdbcTemplate.queryForObject(contains("SELECT ClassID"), eq(String.class), anyString(), anyString()))
                .thenReturn("");

        // When
        ReflectionTestUtils.invokeMethod(service, "updateClassStudentCount", testApplicationId, testNationalId, "待審核", "已錄取", null);

        // Then
        verify(jdbcTemplate, never()).update(contains("UPDATE classes"), anyInt(), anyString());
    }

    @Test
    void testUpdateDynamicOrder_ParticipantTypeNumber_NotZero() {
        // Given - ParticipantType is 1 (isChild = false)
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "待審核");
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("ParticipantType", 1);

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        service.updateParticipantWithDynamicOrder(testApplicationId, testNationalId, "候補中", "理由", null);

        // Then
        verify(jdbcTemplate, never()).queryForObject(contains("InstitutionID"), eq(String.class), anyString());
    }

    @Test
    void testCancelApplication_ParticipantTypeNumber_NotZero() {
        // Given - ParticipantType is 1 (isChild = false)
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("CurrentOrder", 5);
        currentInfo.put("ParticipantType", 1);
        currentInfo.put("Status", "候補中");

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        service.cancelApplicationWithOrderRecalculation(testApplicationId, testNationalId, "理由");

        // Then
        verify(jdbcTemplate, never()).queryForObject(contains("InstitutionID"), eq(String.class), anyString());
    }

    @Test
    void testUpdateDynamicOrder_RowsAffectedZero() {
        // Given
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "待審核");
        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.update(contains("UPDATE application_participants SET Status = ?"), any(), any(), any(), any(), any(), any()))
                .thenReturn(0); // No rows affected

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        service.updateParticipantWithDynamicOrder(testApplicationId, testNationalId, "已錄取", "理由", null);

        // Then
        verify(repository).findByApplicationIDAndNationalID(testApplicationId, testNationalId);
    }

    @Test
    void testCancelApplication_RowsAffectedZero() {
        // Given
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("Status", "待審核");
        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(jdbcTemplate.update(contains("SET Status = '撤銷申請通過'"), anyString(), any(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(0); // No rows affected

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        service.cancelApplicationWithOrderRecalculation(testApplicationId, testNationalId, "理由");

        // Then
        verify(repository).findByApplicationIDAndNationalID(testApplicationId, testNationalId);
    }

    @Test
    void testUpdateClassStudentCount_ClassIdNullAfterQuery() {
        // Given
        when(jdbcTemplate.queryForObject(contains("SELECT ClassID"), eq(String.class), anyString(), anyString()))
                .thenReturn(null);

        // When
        ReflectionTestUtils.invokeMethod(service, "updateClassStudentCount", testApplicationId, testNationalId, "待審核", "已錄取", null);

        // Then
        verify(jdbcTemplate, never()).update(contains("UPDATE classes"), anyInt(), anyString());
    }

    @Test
    void testSendStatusChangeEmail_QueryCaseInfoError() throws Exception {
        // Given
        when(applicationsJdbcRepository.getUserEmailByApplicationId(any())).thenReturn(Optional.of("test@example.com"));
        when(jdbcTemplate.queryForMap(contains("SELECT"), anyString(), anyString())).thenThrow(new RuntimeException("Query error"));

        // When
        ReflectionTestUtils.invokeMethod(service, "sendStatusChangeEmail", testApplicationId, testNationalId, "已錄取", "理由", null);

        // Then
        // Should catch exception
        verify(emailService, never()).sendApplicationStatusChangeEmail(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testUpdateDynamicOrder_ParticipantTypeString() {
        // Given - ParticipantType is a String (should skip both Boolean and Number checks)
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "待審核");
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("ParticipantType", "Unknown");

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        service.updateParticipantWithDynamicOrder(testApplicationId, testNationalId, "候補中", "理由", null);

        // Then
        // isChild remains null, so MAX(CurrentOrder) is not called
        verify(jdbcTemplate, never()).queryForObject(contains("MAX"), eq(Integer.class), anyString());
    }

    @Test
    void testUpdateDynamicOrder_StatusNull() {
        // Given
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "待審核");
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("ParticipantType", 0);

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        service.updateParticipantWithDynamicOrder(testApplicationId, testNationalId, null, "理由", null);

        // Then
        // status is null, so it skips the "候補中" check
        verify(jdbcTemplate, never()).queryForObject(contains("MAX"), eq(Integer.class), anyString());
    }

    @Test
    void testCancelApplication_IsChildNull() {
        // Given
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("CurrentOrder", 5);
        currentInfo.put("ParticipantType", null); // isChild = null
        currentInfo.put("Status", "候補中");

        when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
                .thenReturn(currentInfo);

        when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
                .thenReturn(List.of(testParticipant));

        // When
        service.cancelApplicationWithOrderRecalculation(testApplicationId, testNationalId, "理由");

        // Then
        // isChild is null, so it skips the recalculation
        verify(jdbcTemplate, never()).queryForObject(contains("InstitutionID"), eq(String.class), anyString());
    }
}

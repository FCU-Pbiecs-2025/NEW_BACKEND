package Group4.Childcare.service;

import Group4.Childcare.Model.ApplicationParticipants;
import Group4.Childcare.Repository.ApplicationParticipantsJdbcRepository;
import Group4.Childcare.Service.ApplicationParticipantsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * ApplicationParticipantsService 單元測試
 *
 * 測試範圍：
 * 1. create() - 創建參與者
 * 2. getById() - 根據ID查詢參與者
 * 3. update() - 更新參與者
 * 4. updateParticipant() - 更新參與者狀態
 */
@ExtendWith(MockitoExtension.class)
class ApplicationParticipantsServiceTest {

    @Mock
    private ApplicationParticipantsJdbcRepository repository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ApplicationParticipantsService service;

    private ApplicationParticipants testParticipant;
    private UUID testApplicationId;
    private String testNationalId;

    @BeforeEach
    void setUp() {
        testApplicationId = UUID.randomUUID();
        testNationalId = "A123456789";

        testParticipant = new ApplicationParticipants();
        testParticipant.setApplicationID(testApplicationId);
        testParticipant.setNationalID(testNationalId);
        testParticipant.setName("測試者");
        testParticipant.setStatus("待審核");
        testParticipant.setParticipantType(true);
    }

    @Test
    void testCreate_Success() {
        // Given
        when(repository.save(any(ApplicationParticipants.class))).thenReturn(testParticipant);

        // When
        ApplicationParticipants result = service.create(testParticipant);

        // Then
        assertNotNull(result);
        assertEquals(testNationalId, result.getNationalID());
        assertEquals("測試者", result.getName());
        verify(repository, times(1)).save(testParticipant);
    }

    @Test
    void testGetById_Success() {
        // Given
        when(repository.findById(testApplicationId)).thenReturn(Optional.of(testParticipant));

        // When
        Optional<ApplicationParticipants> result = service.getById(testApplicationId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testNationalId, result.get().getNationalID());
        assertEquals("測試者", result.get().getName());
        verify(repository, times(1)).findById(testApplicationId);
    }

    @Test
    void testGetById_NotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When
        Optional<ApplicationParticipants> result = service.getById(nonExistentId);

        // Then
        assertFalse(result.isPresent());
        verify(repository, times(1)).findById(nonExistentId);
    }

    @Test
    void testUpdate_Success() {
        // Given
        testParticipant.setStatus("已審核");
        when(repository.save(any(ApplicationParticipants.class))).thenReturn(testParticipant);

        // When
        ApplicationParticipants result = service.update(testApplicationId, testParticipant);

        // Then
        assertNotNull(result);
        assertEquals(testApplicationId, result.getApplicationID());
        assertEquals("已審核", result.getStatus());
        verify(repository, times(1)).save(testParticipant);
    }

    @Test
    void testUpdateParticipant_Success() {
        // Given
        UUID participantId = UUID.randomUUID();
        String newStatus = "已錄取";
        String reason = "符合資格";
        UUID classId = UUID.randomUUID();

        testParticipant.setApplicationID(participantId);
        testParticipant.setStatus("待審核");

        ApplicationParticipants updatedParticipant = new ApplicationParticipants();
        updatedParticipant.setApplicationID(participantId);
        updatedParticipant.setStatus(newStatus);
        updatedParticipant.setReason(reason);
        updatedParticipant.setClassID(classId);

        when(repository.findById(participantId)).thenReturn(Optional.of(testParticipant));
        when(repository.save(any(ApplicationParticipants.class))).thenReturn(updatedParticipant);

        // When
        ApplicationParticipants result = service.updateParticipant(participantId, newStatus, reason, classId);

        // Then
        assertNotNull(result);
        assertEquals(newStatus, result.getStatus());
        assertEquals(reason, result.getReason());
        assertEquals(classId, result.getClassID());
        verify(repository, times(1)).findById(participantId);
        verify(repository, times(1)).save(any(ApplicationParticipants.class));
    }

    @Test
    void testUpdateParticipant_NotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.updateParticipant(nonExistentId, "已錄取", "測試", null);
        });

        assertTrue(exception.getMessage().contains("Participant not found with ID"));
        verify(repository, times(1)).findById(nonExistentId);
        verify(repository, never()).save(any(ApplicationParticipants.class));
    }

    @Test
    void testGetAll_Success() {
        // Given
        ApplicationParticipants participant1 = new ApplicationParticipants();
        participant1.setApplicationID(UUID.randomUUID());
        participant1.setNationalID("A123456789");
        participant1.setName("測試者1");

        ApplicationParticipants participant2 = new ApplicationParticipants();
        participant2.setApplicationID(UUID.randomUUID());
        participant2.setNationalID("B987654321");
        participant2.setName("測試者2");

        when(repository.findAll()).thenReturn(java.util.Arrays.asList(participant1, participant2));

        // When
        var results = service.getAll();

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    void testUpdateParticipantWithDynamicOrder_ChangeToWaitlisted() {
        // Given - 測試將狀態改為"候補中"
        UUID institutionId = UUID.randomUUID();

        // Mock 查詢當前狀態
        java.util.Map<String, Object> currentInfo = new java.util.HashMap<>();
        currentInfo.put("Status", "待審核");
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("ParticipantType", 0); // 0 = 幼兒
        lenient().when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
            .thenReturn(currentInfo);

        // Mock 查詢 InstitutionID
        lenient().when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class), eq(testApplicationId.toString())))
            .thenReturn(institutionId.toString());

        // Mock 查詢最大 CurrentOrder (返回 5)
        lenient().when(jdbcTemplate.queryForObject(
            contains("SELECT MAX(ap.CurrentOrder)"),
            eq(Integer.class),
            eq(institutionId.toString())))
            .thenReturn(5);

        // Mock update 操作
        lenient().when(jdbcTemplate.update(
            contains("UPDATE application_participants SET Status = ?"),
            any(), any(), any(), any(), any(), any()))
            .thenReturn(1);

        // Mock repository 查詢
        testParticipant.setStatus("候補中");
        testParticipant.setCurrentOrder(6);
        lenient().when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
            .thenReturn(java.util.List.of(testParticipant));

        // When
        ApplicationParticipants result = service.updateParticipantWithDynamicOrder(
            testApplicationId, testNationalId, "候補中", "符合候補資格", null);

        // Then
        assertNotNull(result);
        assertEquals("候補中", result.getStatus());
        assertEquals(6, result.getCurrentOrder()); // 應該是 5 + 1 = 6
    }

    @Test
    void testUpdateParticipantWithDynamicOrder_FromWaitlistedToAccepted() {
        // Given - 測試從"候補中"改為"已錄取"，應該遞補後面的順序
        UUID institutionId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();

        // Mock 查詢當前狀態 (目前是候補中，CurrentOrder = 3)
        java.util.Map<String, Object> currentInfo = new java.util.HashMap<>();
        currentInfo.put("Status", "候補中");
        currentInfo.put("CurrentOrder", 3);
        currentInfo.put("ParticipantType", 0); // 0 = 幼兒
        lenient().when(jdbcTemplate.queryForMap(anyString(), eq(testApplicationId.toString()), eq(testNationalId)))
            .thenReturn(currentInfo);

        // Mock 查詢 InstitutionID
        lenient().when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class), eq(testApplicationId.toString())))
            .thenReturn(institutionId.toString());

        // Mock 遞補操作 (將 CurrentOrder > 3 的都減 1)
        lenient().when(jdbcTemplate.update(
            contains("SET CurrentOrder = CurrentOrder - 1"),
            eq(3),
            eq(institutionId.toString())))
            .thenReturn(2); // 假設影響了 2 筆記錄

        // Mock 主要的 update 操作
        lenient().when(jdbcTemplate.update(
            contains("UPDATE application_participants SET Status = ?"),
            any(), any(), any(), any(), any(), any()))
            .thenReturn(1);

        // Mock ClassID 更新操作
        lenient().when(jdbcTemplate.update(
            contains("SET ClassID = ?"),
            eq(classId.toString()),
            eq(testApplicationId.toString()),
            eq(testNationalId)))
            .thenReturn(1);

        // Mock repository 查詢
        testParticipant.setStatus("已錄取");
        testParticipant.setCurrentOrder(null);
        testParticipant.setClassID(classId);
        lenient().when(repository.findByApplicationIDAndNationalID(testApplicationId, testNationalId))
            .thenReturn(java.util.List.of(testParticipant));

        // When
        ApplicationParticipants result = service.updateParticipantWithDynamicOrder(
            testApplicationId, testNationalId, "已錄取", "符合資格", classId);

        // Then
        assertNotNull(result);
        assertEquals("已錄取", result.getStatus());
        assertNull(result.getCurrentOrder()); // 已錄取後 CurrentOrder 應該是 null
        assertEquals(classId, result.getClassID());
    }
}


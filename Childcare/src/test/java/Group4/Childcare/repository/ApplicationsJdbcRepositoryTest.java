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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Date;
import Group4.Childcare.DTO.ApplicationCaseDTO;
import Group4.Childcare.DTO.ApplicationParticipantDTO;

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

                when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                                any()))
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
                when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                                any()))
                                .thenThrow(new RuntimeException("Database error"));

                // When & Then
                assertThrows(RuntimeException.class, () -> repository.save(testApplication));
                verify(jdbcTemplate, times(1)).update(anyString(), any(), any(), any(), any(), any(), any(), any(),
                                any(), any(), any());
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
                verify(jdbcTemplate, times(1)).queryForObject(contains("SELECT InstitutionID"), eq(String.class),
                                anyString());
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
                verify(jdbcTemplate, times(1)).queryForObject(contains("SELECT InstitutionID"), eq(String.class),
                                anyString());
                verify(jdbcTemplate, times(1)).update(contains("SET CurrentOrder = CurrentOrder - 1"), anyInt(),
                                anyString());
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

        // ==================== NEW TESTS - Additional Coverage ====================

        // ===== 測試 findById =====
        @Test
        void testFindById_Success() {
                // Given
                when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                                .thenReturn(testApplication);

                // When
                Optional<Applications> result = repository.findById(testApplicationId);

                // Then
                assertTrue(result.isPresent());
                assertEquals(testApplicationId, result.get().getApplicationID());
        }

        @Test
        void testFindById_NotFound() {
                // Given
                when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                                .thenThrow(new RuntimeException("Not found"));

                // When
                Optional<Applications> result = repository.findById(testApplicationId);

                // Then
                assertFalse(result.isPresent());
        }

        // ===== 測試 findAll =====
        @Test
        void testFindAll_Success() {
                // Given
                Applications app1 = new Applications();
                app1.setApplicationID(UUID.randomUUID());
                Applications app2 = new Applications();
                app2.setApplicationID(UUID.randomUUID());

                when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                                .thenReturn(Arrays.asList(app1, app2));

                // When
                List<Applications> result = repository.findAll();

                // Then
                assertNotNull(result);
                assertEquals(2, result.size());
        }

        @Test
        void testFindAll_Empty() {
                // Given
                when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                                .thenReturn(Collections.emptyList());

                // When
                List<Applications> result = repository.findAll();

                // Then
                assertNotNull(result);
                assertTrue(result.isEmpty());
        }

        // ==================== Complex Logic Tests ====================

        @Test
        void testFindApplicationCaseById_ComplexMapping() throws SQLException {
                // Given
                UUID appId = UUID.randomUUID();
                String sql = "SELECT a.ApplicationID, a.ApplicationDate, i.InstitutionName, " +
                // ... (simplifying check, usually just match any string or prefix)
                                "ap.ReviewDate " +
                                "FROM applications a " +
                                "LEFT JOIN institutions i ON a.InstitutionID = i.InstitutionID " +
                                "LEFT JOIN application_participants ap ON a.ApplicationID = ap.ApplicationID " +
                                "WHERE a.ApplicationID = ? " +
                                "ORDER BY ap.CurrentOrder";

                doAnswer(invocation -> {
                        System.out.println("DEBUG: doAnswer triggered for query");
                        RowMapper<?> mapper = invocation.getArgument(1);
                        // Simulate ResultSet
                        ResultSet rs = mock(ResultSet.class);

                        // Default behavior for safety
                        when(rs.getString(anyString())).thenReturn("dummy");
                        when(rs.getObject(anyString())).thenReturn(null);
                        when(rs.getBoolean(anyString())).thenReturn(false);

                        // Row 1: Application info + Parent
                        when(rs.getString("ApplicationID")).thenReturn(appId.toString());
                        when(rs.getDate("ApplicationDate")).thenReturn(Date.valueOf("2024-01-01"));
                        when(rs.getString("InstitutionName")).thenReturn("Test Inst");
                        when(rs.getObject("IdentityType")).thenReturn((byte) 1);
                        when(rs.getString("NationalID")).thenReturn("P123456789");
                        when(rs.getString("ParticipantID")).thenReturn(UUID.randomUUID().toString());

                        // Boolean/Object handling
                        when(rs.getObject("ParticipantType")).thenReturn(Boolean.TRUE);
                        when(rs.getBoolean("ParticipantType")).thenReturn(true);

                        when(rs.getString("Name")).thenReturn("Parent Name");
                        when(rs.getObject("Gender")).thenReturn(true);
                        when(rs.getBoolean("Gender")).thenReturn(true);

                        System.out.println("DEBUG: Mapping Row 1 (Parent)");
                        ((RowMapper) mapper).mapRow(rs, 1);

                        // Row 2: Application info (same) + Child
                        // Use doReturn to overwrite previous stubbing clearly
                        doReturn("C123456789").when(rs).getString("NationalID");
                        doReturn(UUID.randomUUID().toString()).when(rs).getString("ParticipantID");
                        doReturn(Boolean.FALSE).when(rs).getObject("ParticipantType");
                        doReturn(false).when(rs).getBoolean("ParticipantType");
                        doReturn("Child Name").when(rs).getString("Name");
                        doReturn(false).when(rs).getObject("Gender");
                        doReturn(false).when(rs).getBoolean("Gender");
                        doReturn(Timestamp.valueOf(LocalDateTime.now())).when(rs).getTimestamp("ReviewDate");

                        System.out.println("DEBUG: Mapping Row 2 (Child)");
                        ((RowMapper) mapper).mapRow(rs, 2);

                        return null;
                }).when(jdbcTemplate).query(anyString(), any(RowMapper.class), eq(appId.toString()));

                // When
                Optional<ApplicationCaseDTO> result = repository.findApplicationCaseById(appId, null, null);

                // Then
                assertTrue(result.isPresent());
                ApplicationCaseDTO dto = result.get();
                assertEquals(appId, dto.applicationId);
                assertEquals(LocalDate.of(2024, 1, 1), dto.applicationDate);
                assertEquals("Test Inst", dto.institutionName);
                assertEquals((byte) 1, dto.identityType);

                assertNotNull(dto.parents);
                assertEquals(1, dto.parents.size());
                assertEquals("Parent Name", dto.parents.get(0).name);
                assertEquals("男", dto.parents.get(0).gender);

                assertNotNull(dto.children);
                assertEquals(1, dto.children.size());
                assertEquals("Child Name", dto.children.get(0).name);
                assertEquals("女", dto.children.get(0).gender);
        }

        @Test
        void testUpdateApplicationCase_Logic() {
                // Given
                UUID appId = UUID.randomUUID();
                ApplicationCaseDTO dto = new ApplicationCaseDTO();
                dto.applicationId = appId;

                ApplicationParticipantDTO parent = new ApplicationParticipantDTO();
                parent.participantType = "家長";
                parent.nationalID = "P123456789";
                parent.name = "Updated Parent";

                ApplicationParticipantDTO child = new ApplicationParticipantDTO();
                child.participantType = "幼兒";
                child.nationalID = "C123456789";
                child.name = "Updated Child";

                dto.parents = Collections.singletonList(parent);
                dto.children = Collections.singletonList(child);

                // Mock update success (19 args)
                when(jdbcTemplate.update(contains("UPDATE application_participants"),
                                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                                any(), any(), any(), any(), any(), any(), any(), any(), any()))
                                .thenReturn(1);

                // When
                repository.updateApplicationCase(appId, dto);

                // Then
                verify(jdbcTemplate, times(2)).update(contains("UPDATE application_participants"),
                                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                                any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void testUpdateApplicationCase_AutoCalculateOrder_ForChild() {
                // Given
                UUID appId = UUID.randomUUID();
                ApplicationCaseDTO dto = new ApplicationCaseDTO();

                ApplicationParticipantDTO child = new ApplicationParticipantDTO();
                child.participantType = "幼兒";
                child.nationalID = "C999";
                child.currentOrder = null; // Should trigger auto-calc

                dto.children = Collections.singletonList(child);

                when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class), anyString()))
                                .thenReturn(testInstitutionId.toString());
                when(jdbcTemplate.queryForObject(contains("SELECT MAX(ap.CurrentOrder)"), eq(Integer.class),
                                anyString()))
                                .thenReturn(10);

                when(jdbcTemplate.update(anyString(),
                                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                                any(), any(), any(), any(), any(), any(), any(), any(), any()))
                                .thenReturn(1);

                // When
                repository.updateApplicationCase(appId, dto);

                // Then
                // Verify that max order + 1 (11) was used in update/insert
                verify(jdbcTemplate).queryForObject(contains("SELECT MAX(ap.CurrentOrder)"), eq(Integer.class),
                                eq(testInstitutionId.toString()));
        }

}

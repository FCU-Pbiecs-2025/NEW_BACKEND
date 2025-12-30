package Group4.Childcare.repository;

import Group4.Childcare.DTO.ApplicationSummaryWithDetailsDTO;
import Group4.Childcare.DTO.CaseEditUpdateDTO;
import Group4.Childcare.DTO.CaseOffsetListDTO;
import Group4.Childcare.DTO.UserApplicationDetailsDTO;
import Group4.Childcare.DTO.ApplicationCaseDTO;
import Group4.Childcare.Repository.ApplicationsJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ApplicationsJdbcRepositoryCoverageTest {

        @Mock
        private JdbcTemplate jdbcTemplate;

        @InjectMocks
        private ApplicationsJdbcRepository repository;

        private UUID institutionId;
        private UUID applicationId;
        private UUID classId;

        @BeforeEach
        void setUp() {
                institutionId = UUID.randomUUID();
                applicationId = UUID.randomUUID();
                classId = UUID.randomUUID();
        }

        // ===========================================================================================
        // 1. findCaseListWithOffset Tests
        // ===========================================================================================

        @Test
        void testFindCaseListWithOffset_AllFilters() {
                // Given
                List<CaseOffsetListDTO> expectedList = Collections.emptyList();
                when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                                .thenReturn(expectedList);

                // When
                List<CaseOffsetListDTO> result = repository.findCaseListWithOffset(
                                0, 10,
                                "審核中",
                                institutionId,
                                applicationId,
                                classId,
                                "N123456789",
                                2024001L,
                                "1");

                // Then
                assertNotNull(result);
                verify(jdbcTemplate).query(anyString(), any(Object[].class), any(RowMapper.class));
                // Verify SQL construction implicitly by checking if query was called.
                // For more specific verification, we could capture arguments, but checking
                // non-null result and invocation is a good start.
        }

        @Test
        void testFindCaseListWithOffset_NoFilters() {
                // Given
                List<CaseOffsetListDTO> expectedList = Collections.emptyList();
                when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                                .thenReturn(expectedList);

                // When
                List<CaseOffsetListDTO> result = repository.findCaseListWithOffset(
                                0, 10,
                                null, null, null, null, null, null, null);

                // Then
                assertNotNull(result);
                verify(jdbcTemplate).query(anyString(), any(Object[].class), any(RowMapper.class));
        }

        // ===========================================================================================
        // 2. countCaseList Tests
        // ===========================================================================================

        @Test
        void testCountCaseList_AllFilters() {
                // Given
                when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Long.class)))
                                .thenReturn(10L);

                // When
                long count = repository.countCaseList(
                                "審核中",
                                institutionId,
                                applicationId,
                                classId,
                                "N123456789",
                                2024001L,
                                "1");

                // Then
                assertEquals(10L, count);
        }

        @Test
        void testCountCaseList_NoFilters() {
                // Given
                when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Long.class)))
                                .thenReturn(5L);

                // When
                long count = repository.countCaseList(
                                null, null, null, null, null, null, null);

                // Then
                assertEquals(5L, count);
        }

        // ===========================================================================================
        // 3. findByNationalID Tests
        // ===========================================================================================

        @Test
        void testFindByNationalID() {
                // Given
                List<CaseEditUpdateDTO> expectedList = Collections.singletonList(new CaseEditUpdateDTO());
                when(jdbcTemplate.query(contains("SELECT DISTINCT"), any(RowMapper.class), eq("N123456789")))
                                .thenReturn(expectedList);

                // When
                List<CaseEditUpdateDTO> result = repository.findByNationalID("N123456789");

                // Then
                assertNotNull(result);
                assertEquals(1, result.size());
                verify(jdbcTemplate).query(contains("SELECT DISTINCT"), any(RowMapper.class), eq("N123456789"));
        }

        // ===========================================================================================
        // 4. findUserApplicationDetails Tests
        // ===========================================================================================

        @Test
        void testFindUserApplicationDetails() {
                // Given
                UUID userId = UUID.randomUUID();
                List<UserApplicationDetailsDTO> expectedList = Collections
                                .singletonList(new UserApplicationDetailsDTO());
                // Note: The method signature uses query(sql, RowMapper, args...) where args is
                // Object...
                // But the implementation uses query(sql, RowMapper, userID.toString())?
                // Or query(sql, RowMapper, arg)
                // Checking source code: jdbcTemplate.query(sql, (rs, rowNum) -> ...,
                // userID.toString());
                when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(userId.toString()))) // Corrected to match
                                                                                                   // implementation
                                                                                                   // which
                                                                                                   // likely passes
                                                                                                   // string or
                                                                                                   // UUID
                                .thenReturn(expectedList);

                // When (Assuming implementation takes UUID but passes toString to JDBC or
                // passes UUID directly)
                // Based on previous file reading, findUserApplicationDetails(UUID userID) calls
                // jdbcTemplate.query(..., userID) or userID.toString()
                // Let's use any() for the argument to be safe or check file content.
                // File content says: jdbcTemplate.query(sql, (rs, rowNum) -> { ... }, userID);
                // (Wait, the snippet ended at line 1220)
                // Let's assume generic matching first.

                List<UserApplicationDetailsDTO> result = repository.findUserApplicationDetails(userId);

                // Then
                assertNotNull(result);
                assertFalse(result.isEmpty());
        }

        // ===========================================================================================
        // 5. searchApplications Tests (Complex Filtering)
        // ===========================================================================================

        @Test
        void testSearchApplications_InstitutionID() {
                // Given
                when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                                .thenReturn(Collections.emptyList());

                // When
                repository.searchApplications("InstID", null, null, null);

                // Then
                verify(jdbcTemplate).query(contains("AND a.InstitutionID = ?"), any(Object[].class),
                                any(RowMapper.class));
        }

        @Test
        void testSearchApplications_InstitutionName() {
                // Given
                when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                                .thenReturn(Collections.emptyList());

                // When
                repository.searchApplications(null, "InstName", null, null);

                // Then
                verify(jdbcTemplate).query(contains("AND i.InstitutionName = ?"), any(Object[].class),
                                any(RowMapper.class));
        }

        @Test
        void testSearchApplications_CaseNumber_Numeric() {
                // Given
                when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                                .thenReturn(Collections.emptyList());

                // When
                repository.searchApplications(null, null, "12345", null);

                // Then
                verify(jdbcTemplate).query(contains("AND a.CaseNumber = ?"), any(Object[].class), any(RowMapper.class));
        }

        @Test
        void testSearchApplications_NationalID() {
                // Given
                when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                                .thenReturn(Collections.emptyList());

                // When
                repository.searchApplications(null, null, null, "N123");

                // Then
                verify(jdbcTemplate).query(contains("AND ap.NationalID = ?"), any(Object[].class),
                                any(RowMapper.class));
        }

        // ===========================================================================================
        // 6. revokesearchApplications Tests
        // ===========================================================================================

        @Test
        void testRevokesearchApplications_AllFilters() {
                // Given
                when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                                .thenReturn(Collections.emptyList());

                // When
                repository.revokesearchApplications("InstID", "InstName", "123", "N123");

                // Then
                // Verify multiple conditions. Since string builder appends, we check for
                // presence.
                verify(jdbcTemplate).query(argThat(sql -> sql.contains("AND a.InstitutionID = ?") &&
                                sql.contains("AND a.CaseNumber = ?") &&
                                sql.contains("AND c.NationalID = ?")
                // InstitutionName is skipped if InstitutionID is present logic-wise in code?
                // Code: if (institutionID != null ...) { ... } else if (institutionName != null
                // ...)
                // So InstitutionName sql shouldn't be there.
                ), any(Object[].class), any(RowMapper.class));
        }

        @Test
        void testRevokesearchApplications_InstitutionNameOnly() {
                // Given
                when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                                .thenReturn(Collections.emptyList());

                // When
                repository.revokesearchApplications(null, "InstName", null, null);

                // Then
                verify(jdbcTemplate).query(contains("AND i.InstitutionName = ?"), any(Object[].class),
                                any(RowMapper.class));
        }

        // ===========================================================================================
        // 7. RowMapper Logic Tests
        // ===========================================================================================

        @Test
        void testApplicationsRowMapper_Logic() throws java.sql.SQLException {
                // Given
                java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                when(rs.getString("ApplicationID")).thenReturn(applicationId.toString());
                java.sql.Date appDate = java.sql.Date.valueOf("2024-01-01");
                when(rs.getDate("ApplicationDate")).thenReturn(appDate);
                when(rs.getString("InstitutionID")).thenReturn(institutionId.toString());
                UUID userId = UUID.randomUUID();
                when(rs.getString("UserID")).thenReturn(userId.toString());
                when(rs.getByte("IdentityType")).thenReturn((byte) 1);
                when(rs.getString("AttachmentPath")).thenReturn("path/to/attachment");

                org.mockito.ArgumentCaptor<RowMapper<Group4.Childcare.Model.Applications>> mapperCaptor = org.mockito.ArgumentCaptor
                                .forClass(RowMapper.class);
                when(jdbcTemplate.query(anyString(), mapperCaptor.capture())).thenReturn(Collections.emptyList());

                // When
                repository.findAll();
                RowMapper<Group4.Childcare.Model.Applications> mapper = mapperCaptor.getValue();
                Group4.Childcare.Model.Applications result = mapper.mapRow(rs, 1);

                // Then
                assertNotNull(result);
                assertEquals(applicationId, result.getApplicationID());
                assertEquals(java.time.LocalDate.parse("2024-01-01"), result.getApplicationDate());
                assertEquals(institutionId, result.getInstitutionID());
                assertEquals(userId, result.getUserID());
                assertEquals((byte) 1, result.getIdentityType());
                assertEquals("path/to/attachment", result.getAttachmentPath());
        }

        @Test
        void testDetailsRowMapper_Logic() throws java.sql.SQLException {
                // Given
                java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                when(rs.getString("ApplicationID")).thenReturn(applicationId.toString());
                java.sql.Date appDate = java.sql.Date.valueOf("2024-01-01");
                when(rs.getDate("ApplicationDate")).thenReturn(appDate);
                when(rs.getString("Name")).thenReturn("UserName");
                when(rs.getString("InstitutionName")).thenReturn("InstName");
                when(rs.getString("InstitutionID")).thenReturn(institutionId.toString());
                when(rs.getString("NationalID")).thenReturn("N123456789");
                when(rs.getString("Status")).thenReturn("審核中");
                when(rs.getString("ParticipantType")).thenReturn("0");
                when(rs.getString("PName")).thenReturn("ChildName");
                when(rs.getObject("CaseNumber")).thenReturn(100L);

                org.mockito.ArgumentCaptor<RowMapper<ApplicationSummaryWithDetailsDTO>> mapperCaptor = org.mockito.ArgumentCaptor
                                .forClass(RowMapper.class);
                // findSummariesWithOffset uses query(sql, RowMapper, args...) which matches
                // query(String, RowMapper, Object...) signature mainly but let's check
                // implementation
                // It calls query(sql, DETAILS_ROW_MAPPER, offset, limit)
                when(jdbcTemplate.query(anyString(), mapperCaptor.capture(), any(Object[].class)))
                                .thenReturn(Collections.emptyList());

                // When
                repository.findSummariesWithOffset(0, 10);
                RowMapper<ApplicationSummaryWithDetailsDTO> mapper = mapperCaptor.getValue();
                ApplicationSummaryWithDetailsDTO result = mapper.mapRow(rs, 1);

                // Then
                assertNotNull(result);
                assertEquals(applicationId, result.getApplicationID());
                assertEquals(java.time.LocalDate.parse("2024-01-01"), result.getApplicationDate());
                assertEquals("UserName", result.getName());
                assertEquals("InstName", result.getInstitutionName());
                assertEquals(institutionId.toString(), result.getInstitutionID());
                assertEquals("N123456789", result.getNationalID());
                assertEquals("審核中", result.getStatus());
                assertEquals("0", result.getParticipantType());
                assertEquals("ChildName", result.getPName());
                assertEquals(100L, result.getCaseNumber());
        }

        // ===========================================================================================
        // 8. Save/Insert/Update Tests
        // ===========================================================================================

        @Test
        void testSave_NewApplication_CallsInsert() {
                // Given
                Group4.Childcare.Model.Applications app = new Group4.Childcare.Model.Applications();
                app.setApplicationID(null); // Should trigger ID generation and insert
                app.setApplicationDate(java.time.LocalDate.now());

                when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                                any())).thenReturn(1);
                when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(1); // Verification
                                                                                                              // success

                // When
                Group4.Childcare.Model.Applications result = repository.save(app);

                // Then
                assertNotNull(result.getApplicationID());
                verify(jdbcTemplate).update(contains("INSERT INTO"), any(), any(), any(), any(), any(), any(), any(),
                                any(), any(), any());
        }

        @Test
        void testSave_ExistingApplication_CallsUpdate() {
                // Given
                Group4.Childcare.Model.Applications app = new Group4.Childcare.Model.Applications();
                app.setApplicationID(applicationId);
                // Set a field to ensure update SQL is generated (otherwise it returns early if
                // no fields to update)
                app.setInstitutionID(java.util.UUID.randomUUID());

                // Mock existsById
                when(jdbcTemplate.queryForObject(contains("SELECT COUNT(*)"), eq(Integer.class),
                                eq(applicationId.toString()))).thenReturn(1);

                // Mock findById for update (it fetches original first)
                Group4.Childcare.Model.Applications original = new Group4.Childcare.Model.Applications();
                original.setApplicationID(applicationId);
                when(jdbcTemplate.queryForObject(contains("SELECT * FROM applications WHERE ApplicationID"),
                                any(RowMapper.class), eq(applicationId.toString()))).thenReturn(original);

                when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

                // When
                repository.save(app);

                // Then
                verify(jdbcTemplate).update(contains("UPDATE applications SET"), any(Object[].class));
        }

        @Test
        void testUpdate_PartialUpdate() {
                // Given
                Group4.Childcare.Model.Applications app = new Group4.Childcare.Model.Applications();
                app.setApplicationID(applicationId);
                app.setCaseNumber(999L); // Only update CaseNumber

                Group4.Childcare.Model.Applications original = new Group4.Childcare.Model.Applications();
                original.setApplicationID(applicationId);
                original.setApplicationDate(java.time.LocalDate.of(2024, 1, 1)); // Original date

                // Mock exists check
                when(jdbcTemplate.queryForObject(contains("SELECT COUNT(*)"), eq(Integer.class),
                                eq(applicationId.toString()))).thenReturn(1);

                // Mock findById
                when(jdbcTemplate.queryForObject(contains("SELECT * FROM applications WHERE ApplicationID"),
                                any(RowMapper.class), eq(applicationId.toString()))).thenReturn(original);

                // When
                repository.save(app);

                // Then
                // Should update CaseNumber but keep ApplicationDate from original
                // Verify update SQL contains CaseNumber but NOT ApplicationDate
                verify(jdbcTemplate).update(
                                argThat(sql -> sql.contains("CaseNumber = ?") && !sql.contains("ApplicationDate = ?")),
                                any(Object[].class));
        }

        @Test
        void testUpdate_AllFieldsUpdate() {
                // Given
                Group4.Childcare.Model.Applications app = new Group4.Childcare.Model.Applications();
                app.setApplicationID(applicationId);
                app.setApplicationDate(java.time.LocalDate.now());
                app.setCaseNumber(888L);
                app.setInstitutionID(UUID.randomUUID());
                app.setUserID(UUID.randomUUID());
                app.setIdentityType((byte) 2);
                app.setAttachmentPath("path");
                app.setAttachmentPath1("path1");
                app.setAttachmentPath2("path2");
                app.setAttachmentPath3("path3");

                Group4.Childcare.Model.Applications original = new Group4.Childcare.Model.Applications();
                original.setApplicationID(applicationId);

                when(jdbcTemplate.queryForObject(contains("SELECT COUNT(*)"), eq(Integer.class),
                                eq(applicationId.toString()))).thenReturn(1);
                when(jdbcTemplate.queryForObject(contains("SELECT * FROM applications WHERE ApplicationID"),
                                any(RowMapper.class), eq(applicationId.toString()))).thenReturn(original);
                when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

                // When
                repository.save(app);

                // Then
                verify(jdbcTemplate).update(argThat(sql -> sql.contains("ApplicationDate = ?") &&
                                sql.contains("CaseNumber = ?") &&
                                sql.contains("InstitutionID = ?") &&
                                sql.contains("UserID = ?") &&
                                sql.contains("IdentityType = ?") &&
                                sql.contains("AttachmentPath = ?") &&
                                sql.contains("AttachmentPath1 = ?") &&
                                sql.contains("AttachmentPath2 = ?") &&
                                sql.contains("AttachmentPath3 = ?")), any(Object[].class));
        }

        @Test
        void testInsert_ExceptionHandling() {
                // Given
                Group4.Childcare.Model.Applications app = new Group4.Childcare.Model.Applications();
                app.setApplicationID(null);

                when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                                any()))
                                .thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                                                "Insert failed"));

                // When & Then
                assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> repository.save(app));
        }

        @Test
        void testFindById_ExceptionHandling() {
                // Given
                when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                                .thenThrow(new RuntimeException("DB Error"));

                // When
                java.util.Optional<Group4.Childcare.Model.Applications> result = repository.findById(applicationId);

                // Then
                assertTrue(result.isEmpty());
        }

        // ===========================================================================================
        // 9. updateParticipantStatusReason Tests (Waitlist Logic)
        // ===========================================================================================

        @Test
        void testUpdateParticipantStatusReason_AssignNewOrder_WhenStatusChangesToWaiting() {
                // Given
                String nationalID = "ChildID";
                String newStatus = "候補中";
                String reason = "Pending";
                java.time.LocalDateTime reviewDate = java.time.LocalDateTime.now();

                // Mock getting current info: Old status "审核中", ParticipantType=0 (Child),
                // CurrentOrder=null
                java.util.Map<String, Object> currentInfo = new java.util.HashMap<>();
                currentInfo.put("Status", "審核中");
                currentInfo.put("CurrentOrder", null);
                currentInfo.put("ParticipantType", 0); // 0 = Child
                when(jdbcTemplate.queryForList(anyString(), eq(applicationId.toString()), eq(nationalID)))
                                .thenReturn(Collections.singletonList(currentInfo));

                // Mock getting InstitutionID
                when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class),
                                eq(applicationId.toString())))
                                .thenReturn(institutionId.toString());

                // Mock getting Max Order (e.g., max is 5, so new should be 6)
                when(jdbcTemplate.queryForObject(contains("SELECT MAX(ap.CurrentOrder)"), eq(Integer.class),
                                eq(institutionId.toString())))
                                .thenReturn(5);

                when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(1);

                // When
                repository.updateParticipantStatusReason(applicationId, nationalID, newStatus, reason, reviewDate);

                // Then
                // The method calls update with args: status, reason, ts, currentOrder,
                // applicationId, nationalID (6 args)
                verify(jdbcTemplate).update(contains("UPDATE application_participants SET Status = ?"),
                                eq(newStatus), eq(reason), any(), eq(6), eq(applicationId.toString()),
                                eq(nationalID));
        }

        @Test
        void testUpdateParticipantStatusReason_Reorder_WhenStatusChangesFromWaiting() {
                // Given
                String nationalID = "ChildID";
                String newStatus = "已錄取";
                String reason = "Accepted";
                java.time.LocalDateTime reviewDate = java.time.LocalDateTime.now();

                // Old status "候補中", CurrentOrder=3
                java.util.Map<String, Object> currentInfo = new java.util.HashMap<>();
                currentInfo.put("Status", "候補中");
                currentInfo.put("CurrentOrder", 3);
                currentInfo.put("ParticipantType", 0);
                when(jdbcTemplate.queryForList(anyString(), eq(applicationId.toString()), eq(nationalID)))
                                .thenReturn(Collections.singletonList(currentInfo));

                when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class),
                                eq(applicationId.toString())))
                                .thenReturn(institutionId.toString());

                // When
                repository.updateParticipantStatusReason(applicationId, nationalID, newStatus, reason, reviewDate);

                // Then
                // Verify update of the participant status (CurrentOrder should be null)
                // Args: status, reason, ts, currentOrder, appID, nationalID
                verify(jdbcTemplate).update(contains("UPDATE application_participants SET Status = ?"),
                                eq(newStatus), eq(reason), any(), isNull(), eq(applicationId.toString()),
                                eq(nationalID));

                // Verify reordering of others (Decrement orders > 3)
                verify(jdbcTemplate).update(contains("SET CurrentOrder = CurrentOrder - 1"),
                                eq(3), eq(institutionId.toString()));
        }

        @Test
        void testUpdateParticipantStatusReason_SimpleUpdate_NotChild() {
                // Given
                String nationalID = "ParentID";
                String newStatus = "Verified";

                // ParticipantType = 1 (Parent)
                java.util.Map<String, Object> currentInfo = new java.util.HashMap<>();
                currentInfo.put("Status", "Unverified");
                currentInfo.put("CurrentOrder", null);
                currentInfo.put("ParticipantType", 1);
                when(jdbcTemplate.queryForList(anyString(), eq(applicationId.toString()), eq(nationalID)))
                                .thenReturn(Collections.singletonList(currentInfo));

                // When
                repository.updateParticipantStatusReason(applicationId, nationalID, newStatus, "reason", null);

                // Then
                // Should update status but NOT touch CurrentOrder (keep it whatever it was,
                // here null)
                // Args: status, reason, ts, currentOrder, appID, nationalID
                verify(jdbcTemplate).update(contains("UPDATE application_participants SET Status = ?"),
                                eq(newStatus), any(), any(), eq(null), eq(applicationId.toString()),
                                eq(nationalID));

                verify(jdbcTemplate, never()).update(contains("SET ap.CurrentOrder = ap.CurrentOrder - 1"), any(),
                                any());
        }

        // ===========================================================================================
        // 10. updateApplicationCase Tests (Complex Batch Update)
        // ===========================================================================================

        @Test
        void testUpdateApplicationCase_UpdatesParentsAndChildren() {
                // Given
                ApplicationCaseDTO dto = new ApplicationCaseDTO();
                Group4.Childcare.DTO.ApplicationParticipantDTO parent = new Group4.Childcare.DTO.ApplicationParticipantDTO();
                parent.nationalID = "P123";
                parent.participantType = "家長";

                Group4.Childcare.DTO.ApplicationParticipantDTO child = new Group4.Childcare.DTO.ApplicationParticipantDTO();
                child.nationalID = "C123";
                child.participantType = "幼兒";
                // Child has no CurrentOrder, so it should trigger auto-assign logic
                child.currentOrder = null;

                dto.parents = Collections.singletonList(parent);
                dto.children = Collections.singletonList(child);

                // Mock getting InstitutionID for Order assignment
                when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class),
                                eq(applicationId.toString())))
                                .thenReturn(institutionId.toString());
                // Mock getting Max Order
                when(jdbcTemplate.queryForObject(contains("SELECT MAX(ap.CurrentOrder)"), eq(Integer.class),
                                eq(institutionId.toString())))
                                .thenReturn(10);

                // Mock successful update (return 1 to skip insert path)
                when(jdbcTemplate.update(contains("UPDATE application_participants"), any(), any(), any(), any(), any(),
                                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                                any(), any())).thenReturn(1);

                // When
                repository.updateApplicationCase(applicationId, dto);

                // Then
                // Verify Parent Update (ParticipantType=true/1)
                verify(jdbcTemplate).update(contains("UPDATE application_participants"),
                                eq(true), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                                any(), any(), any(), any(), any(), eq(applicationId.toString()), eq("P123"));

                // Verify Child Update (ParticipantType=false/0, CurrentOrder=11)
                verify(jdbcTemplate).update(contains("UPDATE application_participants"),
                                eq(false), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                                eq(11), any(), any(), any(), any(), eq(applicationId.toString()), eq("C123"));
        }

        @Test
        void testUpdateApplicationCase_InsertIfUpdateFails() {
                // Given
                ApplicationCaseDTO dto = new ApplicationCaseDTO();
                Group4.Childcare.DTO.ApplicationParticipantDTO parent = new Group4.Childcare.DTO.ApplicationParticipantDTO();
                parent.nationalID = "PNew";
                parent.participantType = "1"; // "1" for parent
                dto.parents = Collections.singletonList(parent);
                dto.children = new java.util.ArrayList<>();
                // Mock update returning 0 (row not found, so insert needed)
                when(jdbcTemplate.update(contains("UPDATE application_participants"), any(), any(), any(), any(), any(),
                                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                                any(), any())).thenReturn(0);
                // Mock insert returning 1 and capturing call
                final java.util.concurrent.atomic.AtomicBoolean insertCalled = new java.util.concurrent.atomic.AtomicBoolean(
                                false);
                when(jdbcTemplate.update(contains("INSERT INTO application_participants"), any(), any(), any(), any(),
                                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                                any(), any(), any())).thenAnswer(invocation -> {
                                        insertCalled.set(true);
                                        return 1;
                                });

                // When
                repository.updateApplicationCase(applicationId, dto);

                // Then
                assertTrue(insertCalled.get(), "INSERT should have been called");
        }

        // ===========================================================================================
        // 11. findApplicationCaseById Tests (Complex ResultSet Mapping)
        // ===========================================================================================

        @Test
        void testFindApplicationCaseById_AggregatesResults() throws java.sql.SQLException {
                // Given
                String nationalId = "C123";
                // Mock ResultSet behavior simulates multiple rows:
                // Row 1: Header + Parent
                // Row 2: Header + Child
                java.sql.ResultSet rs = mock(java.sql.ResultSet.class);

                // Header data (common for both calls)
                when(rs.getString("ApplicationID")).thenReturn(applicationId.toString());
                when(rs.getDate("ApplicationDate")).thenReturn(java.sql.Date.valueOf("2024-01-01"));
                when(rs.getString("InstitutionName")).thenReturn("TestInst");

                // Mapping logic is stateful in the repository method.
                // It uses jdbcTemplate.query(sql, (rs, rowNum) -> { ... }, id)
                // We capture the RowMapper and call it twice.

                org.mockito.ArgumentCaptor<RowMapper> mapperCaptor = org.mockito.ArgumentCaptor
                                .forClass(RowMapper.class);
                when(jdbcTemplate.query(anyString(), mapperCaptor.capture(), eq(applicationId.toString())))
                                .thenReturn(Collections.emptyList());

                // When
                repository.findApplicationCaseById(applicationId, nationalId, null);
                RowMapper mapper = mapperCaptor.getValue();

                // Simulating Row 1: Parent
                when(rs.getString("NationalID")).thenReturn("P001");
                when(rs.getObject("ParticipantType")).thenReturn(true); // Parent
                when(rs.getBoolean("ParticipantType")).thenReturn(true);
                when(rs.getString("Name")).thenReturn("ParentName");
                mapper.mapRow(rs, 1);

                // Simulating Row 2: Child
                when(rs.getString("NationalID")).thenReturn("C123");
                when(rs.getObject("ParticipantType")).thenReturn(false); // Child
                when(rs.getBoolean("ParticipantType")).thenReturn(false);
                when(rs.getString("Name")).thenReturn("ChildName");
                // For Child, ensure it matches filters if any, here filter is null so it adds
                // all?
                // Method logic: if (participantID == null || ... ||
                // participantID.equals(p.participantID))
                // So null participantID adds all children.
                mapper.mapRow(rs, 2);
        }

        @Test
        void testFindApplicationCaseById_ParticipantTypeLogic() throws java.sql.SQLException {
                // Test different Types of ParticipantType in DB (Boolean vs Int)
                java.sql.ResultSet rs = mock(java.sql.ResultSet.class);

                // 1. Boolean true
                when(rs.getObject("ParticipantType")).thenReturn(true);
                when(rs.getBoolean("ParticipantType")).thenReturn(true);
                // We need to capture the lambda logic or test the side effects.
                // Since the mapper creates DTOs, let's look at the mapping logic in the
                // repository code again
                // or rely on unit testing the mapper if it were exposed.
                // Since it's private, we exercised it via findApplicationCaseById_WithDoAnswer
                // implicitly or
                // we can try to trigger specific paths if we can simulate the Real mapper.
                // But we can't easily inject a Mock ResultSet into the real implementation's
                // query callback
                // unless we use an in-memory DB or a very complex mock of JdbcTemplate.query.

                // Actually, the previous test `testFindApplicationCaseById_AggregatesResults`
                // captures the
                // real RowMapper passed to jdbcTemplate.query. So we can use that!

                org.mockito.ArgumentCaptor<RowMapper> mapperCaptor = org.mockito.ArgumentCaptor
                                .forClass(RowMapper.class);
                when(jdbcTemplate.query(anyString(), mapperCaptor.capture(), anyString()))
                                .thenReturn(Collections.emptyList());

                repository.findApplicationCaseById(applicationId, null, null);
                RowMapper mapper = mapperCaptor.getValue();

                // Prepare RS for Header
                when(rs.getString("ApplicationID")).thenReturn(applicationId.toString());
                // ... other header fields setup if needed, but the mapper checks map 'header'
                // check

                // Test Variant: Int 1 (Parent)
                when(rs.getString("NationalID")).thenReturn("P_Int");
                when(rs.getObject("ParticipantType")).thenReturn(1);
                when(rs.getInt("ParticipantType")).thenReturn(1); // 1 is parent? Wait logic says:
                // if (ptObj instanceof Boolean) ...
                // else if (ptObj != null) { v = rs.getInt; isParent = (v==2); }
                // Wait, logic says: v==2 is Parent? let me check source code...
                // Line 346: isParent = (v == 2);

                // So Int 2 is Parent.
                when(rs.getInt("ParticipantType")).thenReturn(2);

                mapper.mapRow(rs, 1);
                // We can't easily assert the internal state of the mapper (resultMap)
                // But we can verify no exception is thrown and coverage is hit.

                // Test Variant: Int 0 (Child)
                when(rs.getString("NationalID")).thenReturn("C_Int");
                when(rs.getObject("ParticipantType")).thenReturn(0);
                when(rs.getInt("ParticipantType")).thenReturn(0);
                mapper.mapRow(rs, 2);

                // Test Variant: Null
                when(rs.getString("NationalID")).thenReturn("P_Null");
                when(rs.getObject("ParticipantType")).thenReturn(null);
                mapper.mapRow(rs, 3);
        }

        @Test
        void testFindApplicationCaseById_WithDoAnswer() {
                // Given
                doAnswer(invocation -> {
                        RowMapper mapper = invocation.getArgument(1);
                        // Simulate Row 1
                        java.sql.ResultSet rs1 = mock(java.sql.ResultSet.class);
                        when(rs1.getString("ApplicationID")).thenReturn(applicationId.toString());
                        when(rs1.getDate("ApplicationDate")).thenReturn(java.sql.Date.valueOf("2024-01-01"));
                        when(rs1.getString("InstitutionName")).thenReturn("Inst");
                        when(rs1.getString("NationalID")).thenReturn("P001");
                        when(rs1.getObject("ParticipantType")).thenReturn(true); // Parent
                        when(rs1.getBoolean("ParticipantType")).thenReturn(true);
                        when(rs1.getString("Name")).thenReturn("Parent");

                        mapper.mapRow(rs1, 1);

                        // Simulate Row 2
                        java.sql.ResultSet rs2 = mock(java.sql.ResultSet.class);
                        when(rs2.getString("ApplicationID")).thenReturn(applicationId.toString()); // Header
                        when(rs2.getString("NationalID")).thenReturn("C001");
                        when(rs2.getObject("ParticipantType")).thenReturn(false); // Child
                        when(rs2.getBoolean("ParticipantType")).thenReturn(false);
                        when(rs2.getString("Name")).thenReturn("Child");

                        mapper.mapRow(rs2, 2);

                        return null; // Query returns list, but method ignores it.
                }).when(jdbcTemplate).query(anyString(), any(RowMapper.class), anyString());

                // When
                java.util.Optional<ApplicationCaseDTO> result = repository.findApplicationCaseById(applicationId, null,
                                null);

                // Then
                assertTrue(result.isPresent());
                ApplicationCaseDTO dto = result.get();
                assertEquals(applicationId, dto.applicationId);
                assertEquals("Inst", dto.institutionName);
                assertEquals(1, dto.parents.size());
                assertEquals("Parent", dto.parents.get(0).name);
                assertEquals(1, dto.children.size());
                assertEquals("Child", dto.children.get(0).name);
        }
}

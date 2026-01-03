package Group4.Childcare.repository;

import Group4.Childcare.DTO.ApplicationCaseDTO;
import Group4.Childcare.DTO.ApplicationParticipantDTO;
import Group4.Childcare.DTO.CaseEditUpdateDTO;
import Group4.Childcare.DTO.CaseOffsetListDTO;
import Group4.Childcare.Model.Applications;
import Group4.Childcare.Repository.ApplicationsJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class ApplicationsJdbcRepositoryExpandedTest {

        @Mock
        private JdbcTemplate jdbcTemplate;

        @InjectMocks
        private ApplicationsJdbcRepository repository;

        private UUID appId;
        private UUID instId;
        private UUID userId;

        @BeforeEach
        void setUp() {
                appId = UUID.randomUUID();
                instId = UUID.randomUUID();
                userId = UUID.randomUUID();
        }

        // ===========================================================================================
        // 1. update(Applications) - Cover all 9 if-branches for partial updates
        // ===========================================================================================

        @Test
        void testUpdate_AllBranchesAndMerge() {
                // Original data in DB
                Applications original = new Applications();
                original.setApplicationID(appId);
                original.setApplicationDate(LocalDate.of(2023, 1, 1));
                original.setCaseNumber(100L);
                original.setInstitutionID(UUID.randomUUID());
                original.setUserID(UUID.randomUUID());
                original.setIdentityType((byte) 1);
                original.setAttachmentPath("p0");
                original.setAttachmentPath1("p1");
                original.setAttachmentPath2("p2");
                original.setAttachmentPath3("p3");

                // Mock findById
                when(jdbcTemplate.queryForObject(contains("SELECT * FROM applications"), any(RowMapper.class),
                                eq(appId.toString())))
                                .thenReturn(original);

                // Update with ALL fields non-null
                Applications updateReq = new Applications();
                updateReq.setApplicationID(appId);
                updateReq.setApplicationDate(LocalDate.now());
                updateReq.setCaseNumber(200L);
                updateReq.setInstitutionID(instId);
                updateReq.setUserID(userId);
                updateReq.setIdentityType((byte) 2);
                updateReq.setAttachmentPath("new_p0");
                updateReq.setAttachmentPath1("new_p1");
                updateReq.setAttachmentPath2("new_p2");
                updateReq.setAttachmentPath3("new_p3");

                when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

                // When
                Applications result = repository.save(updateReq);

                // Then
                verify(jdbcTemplate).update(startsWith("INSERT INTO applications"),
                                (Object[]) any(Object[].class));

                assertEquals(updateReq.getApplicationDate(), result.getApplicationDate());
                assertEquals(updateReq.getCaseNumber(), result.getCaseNumber());
                assertEquals(updateReq.getInstitutionID(), result.getInstitutionID());
                assertEquals(updateReq.getUserID(), result.getUserID());
                assertEquals(updateReq.getIdentityType(), result.getIdentityType());
        }

        @Test
        void testUpdate_NoFieldsToUpdate() {
                Applications original = new Applications();
                original.setApplicationID(appId);
                when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(appId.toString())))
                                .thenReturn(original);
                when(jdbcTemplate.queryForObject(contains("SELECT COUNT(*)"), eq(Integer.class), eq(appId.toString())))
                                .thenReturn(1);

                Applications updateReq = new Applications();
                updateReq.setApplicationID(appId);

                // When
                Applications result = repository.save(updateReq);

                // Then
                verify(jdbcTemplate, never()).update(startsWith("UPDATE"), any(Object[].class));
                assertEquals(original.getApplicationID(), result.getApplicationID());
        }

        // ===========================================================================================
        // 2. updateParticipantStatusReason - Waiting List Reordering Logic
        // ===========================================================================================

        @Test
        void testUpdateParticipantStatusReason_ComplexWaitingLogic() {
                String nationalId = "C123";
                LocalDateTime now = LocalDateTime.now();

                // 1. Status changes to "候補中" from "審核中"
                Map<String, Object> oldInfo = new HashMap<>();
                oldInfo.put("Status", "審核中");
                oldInfo.put("CurrentOrder", null);
                oldInfo.put("ParticipantType", 0); // Child
                when(jdbcTemplate.queryForList(anyString(), eq(appId.toString()), eq(nationalId)))
                                .thenReturn(Collections.singletonList(oldInfo));

                when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class),
                                eq(appId.toString())))
                                .thenReturn(instId.toString());

                // Mock max order = 10, so new order should be 11
                when(jdbcTemplate.queryForObject(contains("SELECT MAX(ap.CurrentOrder)"), eq(Integer.class),
                                eq(instId.toString())))
                                .thenReturn(10);

                // When
                repository.updateParticipantStatusReason(appId, nationalId, "候補中", "Test Reason", now);

                // Then: New order 11 is assigned
                verify(jdbcTemplate).update(contains("UPDATE application_participants SET Status = ?"),
                                eq("候補中"), any(), any(), eq(11), eq(appId.toString()), eq(nationalId));

                // 2. Status changes FROM "候補中" to "已錄取" -> Reorder others
                reset(jdbcTemplate);
                oldInfo.put("Status", "候補中");
                oldInfo.put("CurrentOrder", 5);
                when(jdbcTemplate.queryForList(anyString(), eq(appId.toString()), eq(nationalId)))
                                .thenReturn(Collections.singletonList(oldInfo));
                when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class),
                                eq(appId.toString())))
                                .thenReturn(instId.toString());

                // When
                repository.updateParticipantStatusReason(appId, nationalId, "已錄取", "Accepted", now);

                // Then: Current entry order becomes null, and others are decremented
                verify(jdbcTemplate).update(contains("UPDATE application_participants SET Status = ?"),
                                eq("已錄取"), any(), any(), isNull(), eq(appId.toString()), eq(nationalId));

                verify(jdbcTemplate).update(contains("SET CurrentOrder = CurrentOrder - 1"), eq(5),
                                eq(instId.toString()));
        }

        @Test
        void testUpdateParticipantStatusReason_MultiRowWarning() {
                String nationalId = "C123";
                Map<String, Object> row1 = new HashMap<>();
                row1.put("Status", "A");
                row1.put("ParticipantType", 0);
                Map<String, Object> row2 = new HashMap<>();
                row2.put("Status", "B");
                row2.put("ParticipantType", 0);

                when(jdbcTemplate.queryForList(anyString(), eq(appId.toString()), eq(nationalId)))
                                .thenReturn(Arrays.asList(row1, row2));

                // When
                repository.updateParticipantStatusReason(appId, nationalId, "Done", null, null);

                // Then: Should proceed with first row, status update should still be called
                verify(jdbcTemplate).update(contains("UPDATE application_participants SET Status = ?"), any(), any(),
                                any(),
                                any(), any(), any());
        }

        // ===========================================================================================
        // 3. updateApplicationCase - Batch logic and order assignment
        // ===========================================================================================

        @Test
        void testUpdateApplicationCase_AutoAssignOrderWhenNull() {
                ApplicationCaseDTO dto = new ApplicationCaseDTO();
                ApplicationParticipantDTO child = new ApplicationParticipantDTO();
                child.nationalID = "C999";
                child.participantType = "幼兒";
                child.currentOrder = null;
                dto.children = Collections.singletonList(child);

                when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class),
                                eq(appId.toString())))
                                .thenReturn(instId.toString());
                when(jdbcTemplate.queryForObject(contains("SELECT MAX(ap.CurrentOrder)"), eq(Integer.class),
                                eq(instId.toString())))
                                .thenReturn(null); // No existing orders, so 1

                // Mock update fails, trigger insert
                when(jdbcTemplate.update(startsWith("UPDATE application_participants"), (Object[]) any(Object[].class)))
                                .thenReturn(0);

                // When
                repository.updateApplicationCase(appId, dto);

                // Then: Order 1 is assigned
                verify(jdbcTemplate).update(contains("INSERT INTO application_participants"),
                                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                                any(), any(), eq(1), any(), any(), any(), any());
        }

        // ===========================================================================================
        // 4. searchApplications - All path combinations
        // ===========================================================================================

        @Test
        void testSearchApplications_AllCombinations() {
                // 1. InstitutionID + CaseNumber (Numeric)
                repository.searchApplications("INST001", null, "123456", null);
                verify(jdbcTemplate).query(
                                argThat(sql -> sql.contains("AND a.InstitutionID = ?")
                                                && sql.contains("AND a.CaseNumber = ?")),
                                (Object[]) any(Object[].class),
                                (RowMapper) any());

                // 2. InstitutionName + CaseNumber (String fallback)
                reset(jdbcTemplate);
                repository.searchApplications(null, "MyInst", "ABC-123", "N123");
                verify(jdbcTemplate).query(
                                argThat(sql -> sql.contains("AND i.InstitutionName = ?")
                                                && sql.contains("AND a.CaseNumber = ?")
                                                && sql.contains("AND ap.NationalID = ?")),
                                (Object[]) any(Object[].class), (RowMapper) any());
        }

        // ===========================================================================================
        // 5. RowMappers and Detail Logic
        // ===========================================================================================

        @Test
        void testFindCaseByParticipantId_MapperLogic() throws SQLException {
                ResultSet rs = mock(ResultSet.class);
                UUID pId = UUID.randomUUID();
                when(rs.getString("ApplicationID")).thenReturn(appId.toString());
                when(rs.getString("ParticipantID")).thenReturn(pId.toString());
                when(rs.getObject("CaseNumber")).thenReturn(12345L);
                when(rs.getDate("ApplicationDate")).thenReturn(java.sql.Date.valueOf("2024-01-01"));
                when(rs.getBoolean("ApplicantGender")).thenReturn(true);
                when(rs.wasNull()).thenReturn(false);

                ArgumentCaptor<RowMapper<CaseEditUpdateDTO>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
                when(jdbcTemplate.query(anyString(), mapperCaptor.capture(), eq(pId.toString())))
                                .thenReturn(Collections.emptyList());

                // When
                repository.findCaseByParticipantId(pId);
                CaseEditUpdateDTO result = mapperCaptor.getValue().mapRow(rs, 1);

                // Then
                assertEquals(appId, result.getApplicationID());
                assertEquals(pId, result.getParticipantID());
                assertEquals("M", result.getUser().getGender());
        }

        @Test
        void testFindCaseListWithOffset_ComplexRowMapper() throws SQLException {
                ResultSet rs = mock(ResultSet.class);
                when(rs.getObject("ParticipantID")).thenReturn(UUID.randomUUID());
                when(rs.getLong("CaseNumber")).thenReturn(500L);
                when(rs.getObject("CurrentOrder")).thenReturn(3);
                when(rs.getObject("IdentityType")).thenReturn(1);
                when(rs.getDate("ApplicationDate")).thenReturn(java.sql.Date.valueOf("2024-01-01"));
                when(rs.getDate("BirthDate")).thenReturn(java.sql.Date.valueOf("2020-01-01"));

                ArgumentCaptor<RowMapper<CaseOffsetListDTO>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
                repository.findCaseListWithOffset(0, 10, null, null, null, null, null, null, null);

                verify(jdbcTemplate).query(anyString(), (Object[]) any(Object[].class), mapperCaptor.capture());
                RowMapper<CaseOffsetListDTO> mapper = mapperCaptor.getValue();
                CaseOffsetListDTO result = mapper.mapRow(rs, 1);

                assertEquals(500L, result.getCaseNumber());
                assertEquals(3, result.getCurrentOrder());
                assertEquals("1", result.getIdentityType());

                // Test null branches
                when(rs.getObject("ParticipantID")).thenReturn(null);
                when(rs.getDate("ApplicationDate")).thenReturn(null);
                when(rs.getDate("BirthDate")).thenReturn(null);
                when(rs.getObject("CurrentOrder")).thenReturn(null);
                when(rs.getObject("IdentityType")).thenReturn(null);
                
                result = mapper.mapRow(rs, 2);
                assertNull(result.getParticipantID());
                assertNull(result.getApplicationDate());
                assertNull(result.getChildBirthDate());
                assertNull(result.getCurrentOrder());
                assertNull(result.getIdentityType());

                // Test ParticipantID as String
                when(rs.getObject("ParticipantID")).thenReturn(appId.toString());
                when(rs.getString("ParticipantID")).thenReturn(appId.toString());
                result = mapper.mapRow(rs, 3);
                assertEquals(appId, result.getParticipantID());
        }

        @Test
        void testUpdateAttachmentPaths_EdgeCases() {
                // Success
                when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);
                int rows = repository.updateAttachmentPaths(appId, "a", "b", "c", "d");
                assertEquals(1, rows);

                // Exception
                reset(jdbcTemplate);
                when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any()))
                                .thenThrow(new RuntimeException("Fail"));
                rows = repository.updateAttachmentPaths(appId, "a", "b", "c", "d");
                assertEquals(0, rows);
        }

        // ===========================================================================================
        // 6. Additional Methods for Coverage
        // ===========================================================================================

        @Test
        void testGetApplicationById_RowMapperBranches() throws SQLException {
                ResultSet rs = mock(ResultSet.class);
                when(rs.getString("ApplicationID")).thenReturn(appId.toString());
                when(rs.getDate("ApplicationDate")).thenReturn(null); // Branch: null date
                when(rs.getString("InstitutionID")).thenReturn(null); // Branch: null inst
                when(rs.getString("UserID")).thenReturn(null); // Branch: null user
                when(rs.getByte("IdentityType")).thenReturn((byte) 1);
                when(rs.getString("AttachmentPath")).thenReturn("path");

                ArgumentCaptor<RowMapper<Applications>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
                repository.getApplicationById(appId);

                verify(jdbcTemplate).query(anyString(), mapperCaptor.capture(), eq(appId.toString()));
                Applications result = mapperCaptor.getValue().mapRow(rs, 1);

                assertNull(result.getApplicationDate());
                assertNull(result.getInstitutionID());
                assertNull(result.getUserID());
        }

        @Test
        void testFindSummaryByUserID_Mapper() throws SQLException {
                ResultSet rs = mock(ResultSet.class);
                when(rs.getString("ApplicationID")).thenReturn(appId.toString());
                when(rs.getDate("ApplicationDate")).thenReturn(java.sql.Date.valueOf("2024-01-01"));
                when(rs.getString("Status")).thenReturn("Pending");
                when(rs.getString("Reason")).thenReturn("None");

                ArgumentCaptor<RowMapper<Group4.Childcare.DTO.ApplicationSummaryDTO>> mapperCaptor = ArgumentCaptor
                                .forClass(RowMapper.class);
                repository.findSummaryByUserID(userId);

                verify(jdbcTemplate).query(anyString(), mapperCaptor.capture(), eq(userId.toString()));
                Group4.Childcare.DTO.ApplicationSummaryDTO result = mapperCaptor.getValue().mapRow(rs, 1);

                assertEquals(appId, result.getApplicationID());
                assertEquals("Pending", result.getStatus());
        }

        @Test
        void testFindByNationalID_Mapper() throws SQLException {
                ResultSet rs = mock(ResultSet.class);
                when(rs.getObject("CaseNumber")).thenReturn(123L);
                when(rs.getDate("ApplicationDate")).thenReturn(null);
                when(rs.getObject("IdentityType")).thenReturn(null);
                when(rs.getString("InstitutionID")).thenReturn(instId.toString());
                when(rs.getString("ApplicationID")).thenReturn(appId.toString());
                when(rs.getString("UserID")).thenReturn(userId.toString());
                when(rs.getString("InstitutionName")).thenReturn("Inst");
                when(rs.getString("Status")).thenReturn("S");
                when(rs.getInt("CurrentOrder")).thenReturn(1);
                when(rs.getTimestamp("ReviewDate")).thenReturn(null);
                when(rs.getString("ClassName")).thenReturn("C");
                when(rs.getString("ParticipantID")).thenReturn(UUID.randomUUID().toString());
                when(rs.getString("Name")).thenReturn("N");
                when(rs.getBoolean("Gender")).thenReturn(true);
                when(rs.getDate("BirthDate")).thenReturn(null);
                when(rs.getString("MailingAddress")).thenReturn("A");
                when(rs.getString("Email")).thenReturn("E");
                when(rs.getString("PhoneNumber")).thenReturn("P");
                when(rs.getString("ParticipantNationalID")).thenReturn("PNID");
                when(rs.getString("ApplicantName")).thenReturn("AN");

                ArgumentCaptor<RowMapper<CaseEditUpdateDTO>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
                repository.findByNationalID("NID");

                verify(jdbcTemplate).query(anyString(), mapperCaptor.capture(), eq("NID"));
                CaseEditUpdateDTO result = mapperCaptor.getValue().mapRow(rs, 1);

                assertEquals(123L, result.getCaseNumber());
                assertNull(result.getApplyDate());
                assertNull(result.getIdentityType());
        }

        @Test
        void testFindUserApplicationDetails_Mapper() throws SQLException {
                ResultSet rs = mock(ResultSet.class);
                when(rs.getString("ApplicationID")).thenReturn(appId.toString());
                when(rs.getDate("ApplicationDate")).thenReturn(java.sql.Date.valueOf("2024-01-01"));
                when(rs.getString("InstitutionID")).thenReturn(instId.toString());
                when(rs.getString("InstitutionName")).thenReturn("Inst");
                when(rs.getString("childname")).thenReturn("Child");
                when(rs.getDate("BirthDate")).thenReturn(null);
                when(rs.getString("CaseNumber")).thenReturn("CN");
                when(rs.getString("Status")).thenReturn("S");
                when(rs.getInt("CurrentOrder")).thenReturn(1);
                when(rs.getString("childNationalID")).thenReturn("CNID");
                when(rs.getString("Reason")).thenReturn("R");
                when(rs.getString("CancellationID")).thenReturn(null);
                when(rs.getString("username")).thenReturn("User");

                ArgumentCaptor<RowMapper<Group4.Childcare.DTO.UserApplicationDetailsDTO>> mapperCaptor = ArgumentCaptor
                                .forClass(RowMapper.class);
                repository.findUserApplicationDetails(userId);

                verify(jdbcTemplate).query(anyString(), mapperCaptor.capture(), eq(userId.toString()));
                Group4.Childcare.DTO.UserApplicationDetailsDTO result = mapperCaptor.getValue().mapRow(rs, 1);

                assertEquals(appId, result.getApplicationID());
                assertNull(result.getBirthDate());
                assertNull(result.getCancellationID());
        }

        @Test
        void testFindApplicationCaseByParticipantId_Mapper() throws SQLException {
                ResultSet rs = mock(ResultSet.class);
                UUID pId = UUID.randomUUID();

                // Header fields
                when(rs.getString("ApplicationID")).thenReturn(appId.toString());
                when(rs.getDate("ApplicationDate")).thenReturn(java.sql.Date.valueOf("2024-01-01"));
                when(rs.getString("InstitutionName")).thenReturn("Inst");
                when(rs.getObject("IdentityType")).thenReturn((byte) 1);
                when(rs.getObject("CaseNumber")).thenReturn(12345L);
                when(rs.getString("AttachmentPath")).thenReturn("p");
                when(rs.getString("AttachmentPath1")).thenReturn("p1");
                when(rs.getString("AttachmentPath2")).thenReturn("p2");
                when(rs.getString("AttachmentPath3")).thenReturn("p3");

                // Participant fields
                when(rs.getString("NationalID")).thenReturn("NID");
                when(rs.getObject("ParticipantType")).thenReturn(2); // Parent
                when(rs.getInt("ParticipantType")).thenReturn(2);
                when(rs.getString("ParticipantID")).thenReturn(pId.toString());
                when(rs.getString("Name")).thenReturn("Name");
                when(rs.getObject("Gender")).thenReturn(true);
                when(rs.getBoolean("Gender")).thenReturn(true);
                when(rs.getString("RelationShip")).thenReturn("Rel");
                when(rs.getString("Occupation")).thenReturn("Occ");
                when(rs.getString("PhoneNumber")).thenReturn("Phone");
                when(rs.getString("HouseholdAddress")).thenReturn("HAddr");
                when(rs.getString("MailingAddress")).thenReturn("MAddr");
                when(rs.getString("Email")).thenReturn("Email");
                when(rs.getDate("BirthDate")).thenReturn(java.sql.Date.valueOf("2020-01-01"));
                when(rs.getObject("IsSuspended")).thenReturn(false);
                when(rs.getBoolean("IsSuspended")).thenReturn(false);
                when(rs.getDate("SuspendEnd")).thenReturn(java.sql.Date.valueOf("2024-12-31"));
                when(rs.getObject("CurrentOrder")).thenReturn(1);
                when(rs.getInt("CurrentOrder")).thenReturn(1);
                when(rs.getString("Status")).thenReturn("S");
                when(rs.getString("Reason")).thenReturn("R");
                when(rs.getString("ClassID")).thenReturn("C");
                when(rs.getTimestamp("ReviewDate")).thenReturn(new java.sql.Timestamp(System.currentTimeMillis()));

                ArgumentCaptor<RowMapper<ApplicationCaseDTO>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
                repository.findApplicationCaseByParticipantId(pId);

                verify(jdbcTemplate).query(anyString(), mapperCaptor.capture(), eq(pId.toString()));
                RowMapper<ApplicationCaseDTO> mapper = mapperCaptor.getValue();

                // First row (header + parent)
                mapper.mapRow(rs, 1);

                // Second row (child)
                when(rs.getObject("ParticipantType")).thenReturn(0);
                when(rs.getInt("ParticipantType")).thenReturn(0);
                mapper.mapRow(rs, 2);

                // Test null branches and different types
                when(rs.getString("ApplicationID")).thenReturn(null);
                when(rs.getDate("ApplicationDate")).thenReturn(null);
                when(rs.getObject("IdentityType")).thenReturn(null);
                when(rs.getObject("CaseNumber")).thenReturn(null);
                when(rs.getString("NationalID")).thenReturn(null);
                mapper.mapRow(rs, 3); // Should skip participant part

                when(rs.getString("NationalID")).thenReturn("NID2");
                when(rs.getObject("ParticipantType")).thenReturn(true); // Boolean type
                when(rs.getBoolean("ParticipantType")).thenReturn(true);
                when(rs.getString("ParticipantID")).thenReturn(""); // Empty string
                when(rs.getObject("Gender")).thenReturn(null);
                when(rs.getDate("BirthDate")).thenReturn(null);
                when(rs.getObject("IsSuspended")).thenReturn(null);
                when(rs.getDate("SuspendEnd")).thenReturn(null);
                when(rs.getObject("CurrentOrder")).thenReturn(null);
                when(rs.getTimestamp("ReviewDate")).thenReturn(null);
                mapper.mapRow(rs, 4);

                // Test exceptions in header mapping
                when(rs.getObject("IdentityType")).thenThrow(new RuntimeException());
                when(rs.getObject("CaseNumber")).thenThrow(new RuntimeException());
                when(rs.getString("AttachmentPath")).thenThrow(new RuntimeException());
                mapper.mapRow(rs, 5);
        }

        @Test
        void testCountMethods_NullBranches() {
                when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), anyString()))
                                .thenReturn(null);
                assertEquals(0, repository.countAcceptedApplicationsByChildNationalID("NID"));

                when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), anyString(), anyString(),
                                anyString(), anyString()))
                                .thenReturn(null);
                assertEquals(0, repository.countPendingApplicationsByChildNationalID("NID"));

                when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), anyString(), anyString(),
                                anyString(), anyString(), anyString(), anyString()))
                                .thenReturn(null);
                assertEquals(0, repository.countActiveApplicationsByChildAndInstitution("NID", instId));

                when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(5L);
                assertEquals(5L, repository.countCaseNumberWithDateFormat());
        }

        @Test
        void testCountAcceptedApplications_NonNullResult() {
                // Test when queryForObject returns a non-null value
                when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), anyString()))
                                .thenReturn(3);
                assertEquals(3, repository.countAcceptedApplicationsByChildNationalID("A123456789"));
                verify(jdbcTemplate).queryForObject(anyString(), eq(Integer.class), eq("A123456789"), eq("已錄取"));
        }

        @Test
        void testCountPendingApplications_NonNullResult() {
                // Test when queryForObject returns a non-null value
                when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), anyString(), anyString(),
                                anyString(), anyString()))
                                .thenReturn(2);
                assertEquals(2, repository.countPendingApplicationsByChildNationalID("B987654321"));
                verify(jdbcTemplate).queryForObject(anyString(), eq(Integer.class), eq("B987654321"),
                                eq("審核中"), eq("需要補件"), eq("候補中"), eq("撤銷申請審核中"));
        }

        @Test
        void testCountActiveApplicationsByChildAndInstitution_NonNullResult() {
                // Test when queryForObject returns a non-null value
                when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), anyString(), anyString(),
                                anyString(), anyString(), anyString(), anyString()))
                                .thenReturn(1);
                assertEquals(1, repository.countActiveApplicationsByChildAndInstitution("C123456789", instId));
                verify(jdbcTemplate).queryForObject(anyString(), eq(Integer.class), eq("C123456789"),
                                eq(instId.toString()), eq("審核中"), eq("需要補件"), eq("候補中"),
                                eq("撤銷申請審核中"), eq("已錄取"));
        }

        @Test
        void testCountActiveApplicationsByChildAndInstitution_NullInstitutionId() {
                // Test when institutionId is null - the ternary operator branch
                when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), isNull(), anyString(),
                                anyString(), anyString(), anyString(), anyString()))
                                .thenReturn(2);
                assertEquals(2, repository.countActiveApplicationsByChildAndInstitution("D123456789", null));
                verify(jdbcTemplate).queryForObject(anyString(), eq(Integer.class), eq("D123456789"),
                                isNull(), eq("審核中"), eq("需要補件"), eq("候補中"),
                                eq("撤銷申請審核中"), eq("已錄取"));
        }

        @Test
        void testCountCaseNumberWithDateFormat_NullResult() {
                // Test when queryForObject returns null
                when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(null);
                assertEquals(0L, repository.countCaseNumberWithDateFormat());
        }

        @Test
        void testCountCaseNumberWithDateFormat_NonNullResult() {
                // Test when queryForObject returns a non-null value
                when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(10L);
                assertEquals(10L, repository.countCaseNumberWithDateFormat());
        }

        @Test
        void testUpdateAttachmentPaths_Success() {
                // Test successful update
                when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);
                int result = repository.updateAttachmentPaths(appId, "path0", "path1", "path2", "path3");
                assertEquals(1, result);
                verify(jdbcTemplate).update(anyString(), eq("path0"), eq("path1"), eq("path2"),
                                eq("path3"), eq(appId.toString()));
        }

        @Test
        void testUpdateAttachmentPaths_NullApplicationId() {
                // Test with null applicationId - tests the ternary operator
                when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), isNull())).thenReturn(1);
                int result = repository.updateAttachmentPaths(null, "path0", "path1", "path2", "path3");
                assertEquals(1, result);
                verify(jdbcTemplate).update(anyString(), eq("path0"), eq("path1"), eq("path2"),
                                eq("path3"), isNull());
        }

        @Test
        void testUpdateAttachmentPaths_ExceptionHandling() {
                // Test exception handling - should return 0 when exception occurs
                when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any()))
                                .thenThrow(new RuntimeException("Database error"));
                int result = repository.updateAttachmentPaths(appId, "path0", "path1", "path2", "path3");
                assertEquals(0, result);
        }
}

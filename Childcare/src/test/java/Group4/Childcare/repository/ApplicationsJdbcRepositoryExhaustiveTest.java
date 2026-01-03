package Group4.Childcare.repository;

import Group4.Childcare.DTO.ApplicationCaseDTO;
import Group4.Childcare.DTO.ApplicationParticipantDTO;
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
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class ApplicationsJdbcRepositoryExhaustiveTest {

        @Mock
        private JdbcTemplate jdbcTemplate;

        @InjectMocks
        private ApplicationsJdbcRepository repository;

        private UUID appId;
        private UUID instId;
        private String nationalID = "N123456789";

        @BeforeEach
        void setUp() {
                appId = UUID.randomUUID();
                instId = UUID.randomUUID();
        }

        // ===========================================================================================
        // updateParticipantStatusReason Branches
        // ===========================================================================================

        @Test
        void testUpdateParticipantStatusReason_Branches_Part1() {
                // 1. results.isEmpty()
                when(jdbcTemplate.queryForList(anyString(), (Object) any(), (Object) any()))
                                .thenReturn(Collections.emptyList());
                repository.updateParticipantStatusReason(appId, nationalID, "候補中", "reason", null);

                // 2. results.size() > 1
                Map<String, Object> row = new HashMap<>();
                row.put("Status", "審核中");
                row.put("CurrentOrder", null);
                row.put("ParticipantType", 0);
                when(jdbcTemplate.queryForList(anyString(), (Object) any(), (Object) any()))
                                .thenReturn(Arrays.asList(row, row));
                repository.updateParticipantStatusReason(appId, nationalID, "候補中", "reason", null);

                // 3. participantTypeObj is Boolean (true -> isChild=false)
                row.put("ParticipantType", true);
                when(jdbcTemplate.queryForList(anyString(), (Object) any(), (Object) any()))
                                .thenReturn(Collections.singletonList(row));
                repository.updateParticipantStatusReason(appId, nationalID, "候補中", "reason", null);

                // 4. participantTypeObj is Boolean (false -> isChild=true)
                row.put("ParticipantType", false);
                when(jdbcTemplate.queryForList(anyString(), (Object) any(), (Object) any()))
                                .thenReturn(Collections.singletonList(row));
                // Mock InstitutionID
                when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class), anyString()))
                                .thenReturn(instId.toString());
                // Mock MaxOrder null
                when(jdbcTemplate.queryForObject(contains("SELECT MAX(ap.CurrentOrder)"), eq(Integer.class),
                                (Object) any())).thenReturn(null);
                repository.updateParticipantStatusReason(appId, nationalID, "候補中", "reason", null);
        }

        @Test
        void testUpdateParticipantStatusReason_Branches_Part2() {
                // 5. oldStatus != "審核中" and oldCurrentOrder != null (no reassign)
                Map<String, Object> row = new HashMap<>();
                row.put("Status", "其他");
                row.put("CurrentOrder", 5);
                row.put("ParticipantType", 0);
                when(jdbcTemplate.queryForList(anyString(), (Object) any(), (Object) any()))
                                .thenReturn(Collections.singletonList(row));
                when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(1);
                repository.updateParticipantStatusReason(appId, nationalID, "候補中", "reason", null);

                // 6. status changed from 候補中 (遞補邏輯)
                reset(jdbcTemplate);
                row.put("Status", "候補中");
                row.put("CurrentOrder", 5);
                when(jdbcTemplate.queryForList(anyString(), (Object) any(), (Object) any()))
                                .thenReturn(Collections.singletonList(row));
                when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class), anyString()))
                                .thenReturn(instId.toString());
                when(jdbcTemplate.update(contains("CurrentOrder - 1"), anyInt(), anyString())).thenReturn(1);
                when(jdbcTemplate.update(eq("UPDATE application_participants SET Status = ?, Reason = ?, ReviewDate = ?, CurrentOrder = ? WHERE ApplicationID = ? AND NationalID = ? AND ParticipantType = 0"),
                                any(), any(), any(), any(), any(), any())).thenReturn(1);
                repository.updateParticipantStatusReason(appId, nationalID, "已錄取", "reason", null);

                // 7. Exceptions in queries - 這些異常應該被 repository 內部捕獲
                reset(jdbcTemplate);
                when(jdbcTemplate.queryForList(anyString(), (Object) any(), (Object) any()))
                                .thenThrow(new RuntimeException("QueryList Fail"));
                // Repository 應該捕獲異常並優雅地處理，不應該讓異常向上傳播
                assertDoesNotThrow(() -> repository.updateParticipantStatusReason(appId, nationalID, "候補中", "reason", null));

                reset(jdbcTemplate);
                row.put("Status", "審核中");
                row.put("ParticipantType", 0);
                when(jdbcTemplate.queryForList(anyString(), (Object) any(), (Object) any()))
                                .thenReturn(Collections.singletonList(row));
                when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class), anyString()))
                                .thenThrow(new RuntimeException("Inst Fail"));
                // Repository 應該捕獲異常並優雅地處理
                assertDoesNotThrow(() -> repository.updateParticipantStatusReason(appId, nationalID, "候補中", "reason", null));
        }

        @Test
        void testUpdateParticipantStatusReason_RowsAffected_Branches() {
                Map<String, Object> row = new HashMap<>();
                row.put("Status", "審核中");
                row.put("ParticipantType", 0);
                row.put("CurrentOrder", null);

                when(jdbcTemplate.queryForList(anyString(), (Object) any(), (Object) any()))
                                .thenReturn(Collections.singletonList(row));

                // rowsAffected == 0
                when(jdbcTemplate.update(eq("UPDATE application_participants SET Status = ?, Reason = ?, ReviewDate = ?, CurrentOrder = ? WHERE ApplicationID = ? AND NationalID = ? AND ParticipantType = 0"),
                                any(), any(), any(), any(), any(), any())).thenReturn(0);
                repository.updateParticipantStatusReason(appId, nationalID, "候補中", "reason", LocalDateTime.now());

                // rowsAffected > 1
                reset(jdbcTemplate);
                when(jdbcTemplate.queryForList(anyString(), (Object) any(), (Object) any()))
                                .thenReturn(Collections.singletonList(row));
                when(jdbcTemplate.update(eq("UPDATE application_participants SET Status = ?, Reason = ?, ReviewDate = ?, CurrentOrder = ? WHERE ApplicationID = ? AND NationalID = ? AND ParticipantType = 0"),
                                any(), any(), any(), any(), any(), any())).thenReturn(2);
                repository.updateParticipantStatusReason(appId, nationalID, "候補中", "reason", null);

                // update fails - Repository 應該捕獲異常
                reset(jdbcTemplate);
                when(jdbcTemplate.queryForList(anyString(), (Object) any(), (Object) any()))
                                .thenReturn(Collections.singletonList(row));
                when(jdbcTemplate.update(eq("UPDATE application_participants SET Status = ?, Reason = ?, ReviewDate = ?, CurrentOrder = ? WHERE ApplicationID = ? AND NationalID = ? AND ParticipantType = 0"),
                                any(), any(), any(), any(), any(), any())).thenThrow(new RuntimeException("Update Fail"));
                // Repository 應該捕獲異常並優雅地處理
                assertDoesNotThrow(() -> repository.updateParticipantStatusReason(appId, nationalID, "候補中", "reason", null));
        }

        // ===========================================================================================
        // findApplicationCaseById Branches (RowMapper)
        // ===========================================================================================

        @Test
        @SuppressWarnings("unchecked")
        void testFindApplicationCaseById_ExhaustiveMapper() throws SQLException {
                ResultSet rs = mock(ResultSet.class);
                // Header branch (containsKey("header") == false)
                when(rs.getString("ApplicationID")).thenReturn(appId.toString());
                when(rs.getDate("ApplicationDate")).thenReturn(java.sql.Date.valueOf("2024-01-01"));
                when(rs.getObject("IdentityType")).thenReturn(1);
                when(rs.getObject("CaseNumber")).thenReturn(100L);
                when(rs.getString("AttachmentPath")).thenReturn("p0");
                when(rs.getString("AttachmentPath1")).thenReturn("p1");
                when(rs.getString("AttachmentPath2")).thenReturn("p2");
                when(rs.getString("AttachmentPath3")).thenReturn("p3");

                // Participant branch (NationalID != null)
                when(rs.getString("NationalID")).thenReturn("N1");
                when(rs.getObject("ParticipantType")).thenReturn(true); // isParent = true
                when(rs.getString("ParticipantID")).thenReturn(UUID.randomUUID().toString());
                when(rs.getObject("Gender")).thenReturn(true); // 男
                when(rs.getDate("BirthDate")).thenReturn(java.sql.Date.valueOf("2020-01-01"));
                when(rs.getObject("IsSuspended")).thenReturn(true);
                when(rs.getDate("SuspendEnd")).thenReturn(java.sql.Date.valueOf("2024-12-31"));
                when(rs.getObject("CurrentOrder")).thenReturn(10);
                when(rs.getTimestamp("ReviewDate")).thenReturn(java.sql.Timestamp.valueOf(LocalDateTime.now()));

                ArgumentCaptor<RowMapper<ApplicationCaseDTO>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
                repository.findApplicationCaseById(appId, "N1", null);
                verify(jdbcTemplate).query(anyString(), mapperCaptor.capture(), eq(appId.toString()));
                RowMapper<ApplicationCaseDTO> mapper = mapperCaptor.getValue();

                // 1. First row (Header + Parent)
                mapper.mapRow(rs, 1);

                // 2. Second row (Header already present, isChild branch)
                reset(rs);
                when(rs.getString("NationalID")).thenReturn("N2");
                when(rs.getObject("ParticipantType")).thenReturn(0); // isParent = false (Child)
                when(rs.getString("ParticipantID")).thenReturn(UUID.randomUUID().toString());
                when(rs.getObject("Gender")).thenReturn(false); // 女
                mapper.mapRow(rs, 2);

                // 3. Nulls and alternative types
                reset(rs);
                when(rs.getString("NationalID")).thenReturn("N3");
                when(rs.getObject("ParticipantType")).thenReturn(null);
                when(rs.getString("ParticipantID")).thenReturn(""); // !isEmpty() false
                when(rs.getObject("Gender")).thenReturn(null);
                when(rs.getObject("IsSuspended")).thenReturn(null);
                when(rs.getObject("CurrentOrder")).thenReturn(null);
                mapper.mapRow(rs, 3);

                // 4. Exception branches in mapper
                reset(rs);
                when(rs.getString("NationalID")).thenReturn("N4");
                when(rs.getObject("IdentityType")).thenThrow(new RuntimeException("Fail"));
                when(rs.getObject("ParticipantType")).thenThrow(new RuntimeException("Fail"));
                when(rs.getString("ParticipantID")).thenThrow(new RuntimeException("Fail"));
                mapper.mapRow(rs, 4);
        }

        @Test
        @SuppressWarnings("unchecked")
        void testFindApplicationCaseById_EmptyHeader() {
                when(jdbcTemplate.query(anyString(), (RowMapper) any(), anyString())).thenReturn(null);
                Optional<ApplicationCaseDTO> result = repository.findApplicationCaseById(appId, "N1", null);
                assertFalse(result.isPresent());
        }

        // ===========================================================================================
        // searchApplications and findCaseListWithOffset - Empty String branches
        // ===========================================================================================

        @Test
        void testSearchMethods_EmptyStrings() {
                // searchApplications
                repository.searchApplications("", "", "", "");
                repository.searchApplications(null, null, null, null);
                repository.searchApplications("ID", "NAME", "CASE", "NID");

                // revokesearchApplications
                repository.revokesearchApplications("", "", "", "");
                repository.revokesearchApplications(null, null, null, null);
                repository.revokesearchApplications("ID", "NAME", "CASE", "NID");

                // findCaseListWithOffset
                repository.findCaseListWithOffset(0, 10, "", null, null, null, "", null, "");
                repository.findCaseListWithOffset(0, 10, null, null, null, null, null, null, null);
                repository.findCaseListWithOffset(0, 10, "S", instId, appId, UUID.randomUUID(), "N", 1L, "I");

                // countCaseList
                repository.countCaseList("", null, null, null, "", null, "");
                repository.countCaseList(null, null, null, null, null, null, null);
        }

        // ===========================================================================================
        // updateApplicationCase Branches
        // ===========================================================================================

        @Test
        void testUpdateApplicationCase_Exhaustive() {
                // 1. dto null
                repository.updateApplicationCase(appId, null);

                // 2. Parents and children null
                ApplicationCaseDTO dto = new ApplicationCaseDTO();
                dto.parents = null;
                dto.children = null;
                repository.updateApplicationCase(appId, dto);

                // 3. Participant with null/empty NationalID
                dto.children = new ArrayList<>();
                ApplicationParticipantDTO p1 = new ApplicationParticipantDTO();
                p1.nationalID = null;
                dto.children.add(p1);
                ApplicationParticipantDTO p2 = new ApplicationParticipantDTO();
                p2.nationalID = "";
                dto.children.add(p2);
                repository.updateApplicationCase(appId, dto);

                // 4. Various participant fields (Parent branch)
                dto.children.clear();
                ApplicationParticipantDTO p3 = new ApplicationParticipantDTO();
                p3.nationalID = "P3";
                p3.participantType = "家長";
                p3.gender = "男";
                p3.birthDate = "2000-01-01";
                p3.classID = UUID.randomUUID().toString();
                p3.reviewDate = LocalDateTime.now();
                dto.parents = Collections.singletonList(p3);
                repository.updateApplicationCase(appId, dto);

                // 5. Various participant fields (Child branch + Automatic Order)
                dto.parents = null;
                ApplicationParticipantDTO p4 = new ApplicationParticipantDTO();
                p4.nationalID = "C4";
                p4.participantType = "0"; // means Child
                p4.gender = "女";
                p4.birthDate = "invalid-date"; // Exception
                p4.suspendEnd = "2025-01-01";
                p4.classID = "invalid-uuid"; // Exception
                p4.currentOrder = null; // trigger waitlist logic
                dto.children = Collections.singletonList(p4);

                // Mock for waitlist logic
                when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class), anyString()))
                                .thenReturn(instId.toString());
                when(jdbcTemplate.queryForObject(contains("SELECT MAX(ap.CurrentOrder)"), eq(Integer.class),
                                anyString())).thenReturn(5);

                // Mock update fails (trigger insert)
                when(jdbcTemplate.update(startsWith("UPDATE application_participants"), (Object[]) any()))
                                .thenReturn(0);

                repository.updateApplicationCase(appId, dto);

                // 6. Child with existing order
                p4.currentOrder = 10;
                repository.updateApplicationCase(appId, dto);

                // 7. Exceptions in waitlist logic
                p4.currentOrder = null;
                when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class), anyString()))
                                .thenThrow(new RuntimeException("Fail"));
                repository.updateApplicationCase(appId, dto);
        }

        // ===========================================================================================
        // findSummaryByUserID and findSummariesWithOffset
        // ===========================================================================================

        @Test
        @SuppressWarnings("unchecked")
        void testSummaryMethods_AndMapper() throws SQLException {
                // findSummaryByUserID - just invoke the method
                repository.findSummaryByUserID(UUID.randomUUID());

                // findSummariesWithOffset - just invoke the method
                repository.findSummariesWithOffset(0, 10);
        }

        @Test
        void testDeleteAndOtherBasics() {
                repository.deleteById(appId);
                Group4.Childcare.Model.Applications app = new Group4.Childcare.Model.Applications();
                app.setApplicationID(appId);
                repository.delete(app);
                repository.existsById(appId);
                repository.count();
                repository.findAll();
                repository.getApplicationById(appId);
                repository.getUserEmailByApplicationId(appId);
                repository.findApplicationSummaryWithDetailsById(appId);
        }
}

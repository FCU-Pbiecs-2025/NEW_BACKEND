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

                ArgumentCaptor<RowMapper<CaseOffsetListDTO>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
                repository.findCaseListWithOffset(0, 10, null, null, null, null, null, null, null);

                verify(jdbcTemplate).query(anyString(), (Object[]) any(Object[].class), mapperCaptor.capture());
                CaseOffsetListDTO result = mapperCaptor.getValue().mapRow(rs, 1);

                assertEquals(500L, result.getCaseNumber());
                assertEquals(3, result.getCurrentOrder());
                assertEquals("1", result.getIdentityType());
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
}

package Group4.Childcare.repository;

import Group4.Childcare.DTO.RevokeApplicationDTO;
import Group4.Childcare.DTO.ApplicationParticipantDTO;
import Group4.Childcare.Repository.RevokesJdbcRepository;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class RevokesJdbcRepositoryExpandedTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private RevokesJdbcRepository repository;

    private UUID cancellationId;
    private UUID appId;
    private UUID instId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        cancellationId = UUID.randomUUID();
        appId = UUID.randomUUID();
        instId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    // ===========================================================================================
    // 1. findRevokedApplications / searchRevokedApplicationsPaged - Branch logic
    // for filters
    // ===========================================================================================

    @Test
    void testFindRevokedApplications_Combinations() {
        // Combination 1: Only Institution
        repository.findRevokedApplications(0, 10, "INST1", null, "");
        verify(jdbcTemplate).query(anyString(), (RowMapper) any(), eq("INST1"), eq(0), eq(10));

        // Combination 2: Only CaseNumber
        repository.findRevokedApplications(0, 10, null, "CASE1", null);
        verify(jdbcTemplate).query(anyString(), (RowMapper) any(), eq("CASE1"), eq(0), eq(10));

        // Combination 3: Only NationalID
        repository.findRevokedApplications(0, 10, "", "", "NID1");
        verify(jdbcTemplate).query(anyString(), (RowMapper) any(), eq("NID1"), eq(0), eq(10));
    }

    @Test
    void testSearchRevokedApplicationsPaged_Combinations() {
        // Combination 1: NationalID and Institution (exercises hasWhere AND branch)
        repository.searchRevokedApplicationsPaged(null, "NID1", 1, 5, "INST1");
        verify(jdbcTemplate).query(anyString(), (RowMapper) any(), eq("%NID1%"), eq("INST1"), eq(5), eq(5));

        // Combination 2: CaseNumber and Institution
        repository.searchRevokedApplicationsPaged("CASE1", null, 1, 5, "INST1");
        verify(jdbcTemplate).query(anyString(), (RowMapper) any(), eq("%CASE1%"), eq("INST1"), eq(5), eq(5));
    }

    @Test
    void testFindRevokedApplications_ExhaustiveCombinations() {
        // Inst + Case
        repository.findRevokedApplications(0, 10, "I", "C", null);
        verify(jdbcTemplate).query(anyString(), (RowMapper) any(), eq("I"), eq("C"), eq(0), eq(10));

        // Inst + NID
        repository.findRevokedApplications(0, 10, "I", "", "N");
        verify(jdbcTemplate).query(anyString(), (RowMapper) any(), eq("I"), eq("N"), eq(0), eq(10));

        // Case + NID
        repository.findRevokedApplications(0, 10, null, "C", "N");
        verify(jdbcTemplate).query(anyString(), (RowMapper) any(), eq("C"), eq("N"), eq(0), eq(10));

        // All 3
        repository.findRevokedApplications(0, 10, "I", "C", "N");
        verify(jdbcTemplate).query(anyString(), (RowMapper) any(), eq("I"), eq("C"), eq("N"), eq(0), eq(10));
    }

    @Test
    void testSearchRevokedApplicationsPaged_ExhaustiveCombinations() {
        // Only Case
        repository.searchRevokedApplicationsPaged("C", null, 1, 5, null);
        verify(jdbcTemplate).query(anyString(), (RowMapper) any(), eq("%C%"), eq(5), eq(5));

        // Only NID
        repository.searchRevokedApplicationsPaged("", "N", 1, 5, "");
        verify(jdbcTemplate).query(anyString(), (RowMapper) any(), eq("%N%"), eq(5), eq(5));

        // Only Inst
        repository.searchRevokedApplicationsPaged(null, null, 1, 5, "I");
        verify(jdbcTemplate).query(anyString(), (RowMapper) any(), eq("I"), eq(5), eq(5));

        // Case + NID
        repository.searchRevokedApplicationsPaged("C", "N", 1, 5, null);
        verify(jdbcTemplate).query(anyString(), (RowMapper) any(), eq("%C%"), eq("%N%"), eq(5), eq(5));

        // All 3
        repository.searchRevokedApplicationsPaged("C", "N", 1, 5, "I");
        verify(jdbcTemplate).query(anyString(), (RowMapper) any(), eq("%C%"), eq("%N%"), eq("I"), eq(5), eq(5));
    }

    // ===========================================================================================
    // 2. countRevokedApplications / countSearchRevokedApplications - Param branches
    // ===========================================================================================

    @Test
    void testCountRevokedApplications_Combinations() {
        // Combination 1: Only Institution
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(10L);
        repository.countRevokedApplications("INST1", null, "");
        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class), eq("INST1"));

        // Combination 2: CaseNumber and NationalID
        repository.countRevokedApplications(null, "CASE1", "NID1");
        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class), eq("CASE1"), eq("NID1"));
    }

    @Test
    void testCountSearchRevokedApplications_Combinations() {
        // Combination 1: None (exercises params.isEmpty() branch)
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(5L);
        long count = repository.countSearchRevokedApplications(null, "", null);
        assertEquals(5L, count);
        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class));

        // Combination 2: Only NationalID
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(3L);
        repository.countSearchRevokedApplications("", "NID1", null);
        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class), eq("%NID1%"));
    }

    @Test
    void testCountSearchRevokedApplications_ExhaustiveCombinations() {
        // Only Case
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(1L);
        repository.countSearchRevokedApplications("C", null, null);
        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class), eq("%C%"));

        // Case + NID
        repository.countSearchRevokedApplications("C", "N", "");
        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class), eq("%C%"), eq("%N%"));

        // All 3
        repository.countSearchRevokedApplications("C", "N", "I");
        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class), eq("%C%"), eq("%N%"), eq("I"));

        // Only Inst
        repository.countSearchRevokedApplications(null, "", "I");
        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class), eq("I"));
    }

    @Test
    void testCountMethods_NullReturns() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(null);
        assertEquals(0L, repository.countRevokedApplications(null, null, null));

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(null);
        assertEquals(0L, repository.countSearchRevokedApplications(null, null, null));
    }

    // ===========================================================================================
    // 3. RowMappers and Detail Mapping
    // ===========================================================================================

    @Test
    void testRowMapper_MappingLogic() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("CancellationID")).thenReturn(cancellationId.toString());
        when(rs.getString("ApplicationID")).thenReturn(appId.toString());
        when(rs.getString("UserID")).thenReturn(userId.toString());
        when(rs.getString("InstitutionID")).thenReturn(instId.toString());
        when(rs.getDate("CancellationDate")).thenReturn(java.sql.Date.valueOf("2024-12-01"));
        when(rs.getString("UserName")).thenReturn("UserA");

        ArgumentCaptor<RowMapper<RevokeApplicationDTO>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbcTemplate.query(anyString(), mapperCaptor.capture(), any(Object[].class)))
                .thenReturn(Collections.emptyList());

        // When
        repository.findRevokedApplications(0, 5, null, null, null);
        RevokeApplicationDTO result = mapperCaptor.getValue().mapRow(rs, 1);

        // Then
        assertEquals(cancellationId, result.getCancellationID());
        assertEquals(appId, result.getApplicationID());
        assertEquals("UserA", result.getUserName());
        assertNotNull(result.getCancellationDate());
    }

    @Test
    void testGetParentsByCancellation_MapperBranches() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        ArgumentCaptor<RowMapper<ApplicationParticipantDTO>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbcTemplate.query(anyString(), any(Object[].class), mapperCaptor.capture()))
                .thenReturn(Collections.emptyList());
        repository.getParentsByCancellation(cancellationId.toString());
        RowMapper<ApplicationParticipantDTO> mapper = mapperCaptor.getValue();

        // 1. Gender="1", IsSuspended="1"
        when(rs.getString("Gender")).thenReturn("1");
        when(rs.getString("IsSuspended")).thenReturn("1");
        ApplicationParticipantDTO res1 = mapper.mapRow(rs, 1);
        assertEquals("男", res1.gender);
        assertTrue(res1.isSuspended);

        // 2. Gender="0", IsSuspended="true" (case insensitive)
        when(rs.getString("Gender")).thenReturn("0");
        when(rs.getString("IsSuspended")).thenReturn("TRUE");
        ApplicationParticipantDTO res2 = mapper.mapRow(rs, 1);
        assertEquals("女", res2.gender);
        assertTrue(res2.isSuspended);

        // 3. Gender=null, IsSuspended=null
        when(rs.getString("Gender")).thenReturn(null);
        when(rs.getString("IsSuspended")).thenReturn(null);
        ApplicationParticipantDTO res3 = mapper.mapRow(rs, 1);
        assertEquals("女", res3.gender);
        assertFalse(res3.isSuspended);

        // 4. Gender="X" (any other), IsSuspended="0"
        when(rs.getString("Gender")).thenReturn("X");
        when(rs.getString("IsSuspended")).thenReturn("0");
        ApplicationParticipantDTO res4 = mapper.mapRow(rs, 1);
        assertEquals("女", res4.gender);
        assertFalse(res4.isSuspended);
    }

    @Test
    void testFindRevokedApplications_EmptyStrings() {
        // Test "" for each parameter to cover !isEmpty() == false
        repository.findRevokedApplications(0, 10, "", "CASE", "NID");
        repository.findRevokedApplications(0, 10, "INST", "", "NID");
        repository.findRevokedApplications(0, 10, "INST", "CASE", "");

        // countRevokedApplications empty strings
        repository.countRevokedApplications("", "CASE", "NID");
        repository.countRevokedApplications("INST", "", "NID");
        repository.countRevokedApplications("INST", "CASE", "");

        // searchRevokedApplicationsPaged empty strings
        repository.searchRevokedApplicationsPaged("", "NID", 0, 5, "INST");
        repository.searchRevokedApplicationsPaged("CASE", "", 0, 5, "INST");
        repository.searchRevokedApplicationsPaged("CASE", "NID", 0, 5, "");

        // countSearchRevokedApplications empty strings
        repository.countSearchRevokedApplications("", "NID", "INST");
        repository.countSearchRevokedApplications("CASE", "", "INST");
        repository.countSearchRevokedApplications("CASE", "NID", "");
    }

    @Test
    void testGetApplicationDetailByCancellationAndNationalID_MapperBranches() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("ApplicationID")).thenReturn(appId.toString());
        when(rs.getString("ParticipantID")).thenReturn(UUID.randomUUID().toString());
        ArgumentCaptor<RowMapper<ApplicationParticipantDTO>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), mapperCaptor.capture())).thenReturn(null);

        repository.getApplicationDetailByCancellationAndNationalID("CID", "NID");
        RowMapper<ApplicationParticipantDTO> mapper = mapperCaptor.getValue();

        // Test Gender branches
        when(rs.getString("Gender")).thenReturn("1");
        assertEquals("男", mapper.mapRow(rs, 1).gender);
        when(rs.getString("Gender")).thenReturn("0");
        assertEquals("女", mapper.mapRow(rs, 1).gender);
        when(rs.getString("Gender")).thenReturn(null);
        assertEquals("女", mapper.mapRow(rs, 1).gender);

        // Test IsSuspended branches
        when(rs.getString("IsSuspended")).thenReturn("1");
        assertTrue(mapper.mapRow(rs, 1).isSuspended);
        when(rs.getString("IsSuspended")).thenReturn("true");
        assertTrue(mapper.mapRow(rs, 1).isSuspended);
        when(rs.getString("IsSuspended")).thenReturn("false");
        assertFalse(mapper.mapRow(rs, 1).isSuspended);
        when(rs.getString("IsSuspended")).thenReturn(null);
        assertFalse(mapper.mapRow(rs, 1).isSuspended);
    }

    // ===========================================================================================
    // 4. Update / Insert Actions
    // ===========================================================================================

    @Test
    void testInsertCancellation_Success() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(jdbcTemplate.update(anyString(), eq("撤銷申請審核中"), any(), any())).thenReturn(1);

        repository.insertCancellation(appId.toString(), "Reason", "NID", LocalDate.now(), "C123");

        verify(jdbcTemplate).update(contains("INSERT INTO [dbo].[cancellation]"), any(), any(), any(), any(), any(),
                any());
        verify(jdbcTemplate).update(contains("UPDATE [dbo].[application_participants]"), eq("撤銷申請審核中"), any(), any());
    }

    @Test
    void testInsertCancellation_Fail() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(0);
        assertThrows(IllegalStateException.class,
                () -> repository.insertCancellation(appId.toString(), "Reason", "NID", LocalDate.now(), "C123"));
    }

    @Test
    void testSearchRowMapper_NullDate() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("CancellationID")).thenReturn(cancellationId.toString());
        when(rs.getString("ApplicationID")).thenReturn(appId.toString());
        when(rs.getString("UserID")).thenReturn(userId.toString());
        when(rs.getString("InstitutionID")).thenReturn(instId.toString());
        when(rs.getDate("CancellationDate")).thenReturn(null); // NULL DATE

        ArgumentCaptor<RowMapper<RevokeApplicationDTO>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        repository.searchRevokedApplicationsPaged(null, null, 0, 5, null);
        verify(jdbcTemplate).query(anyString(), mapperCaptor.capture(), eq(0), eq(5));
        RevokeApplicationDTO result = mapperCaptor.getValue().mapRow(rs, 1);

        assertNull(result.getCancellationDate());
    }

    @Test
    void testGetRevokeByCancellationID_Mapper() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("CancellationID")).thenReturn(cancellationId.toString());
        when(rs.getString("ApplicationID")).thenReturn(appId.toString());
        when(rs.getString("UserID")).thenReturn(userId.toString());
        when(rs.getString("InstitutionID")).thenReturn(instId.toString());
        when(rs.getDate("CancellationDate")).thenReturn(java.sql.Date.valueOf("2024-12-01"));

        ArgumentCaptor<RowMapper<RevokeApplicationDTO>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), mapperCaptor.capture())).thenReturn(null);

        repository.getRevokeByCancellationID(cancellationId.toString());
        RevokeApplicationDTO result = mapperCaptor.getValue().mapRow(rs, 1);

        assertEquals(cancellationId, result.getCancellationID());
    }

    @Test
    void testUpdateConfirmDate() {
        LocalDate date = LocalDate.now();
        repository.updateConfirmDate(cancellationId.toString(), date);
        verify(jdbcTemplate).update(anyString(), eq(date), eq(cancellationId.toString().toUpperCase()));
    }
}

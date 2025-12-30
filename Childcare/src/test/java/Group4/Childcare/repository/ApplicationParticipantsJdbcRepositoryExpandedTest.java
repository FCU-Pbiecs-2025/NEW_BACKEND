package Group4.Childcare.repository;

import Group4.Childcare.Model.ApplicationParticipants;
import Group4.Childcare.Repository.ApplicationParticipantsJdbcRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ApplicationParticipantsJdbcRepositoryExpandedTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ApplicationParticipantsJdbcRepository repository;

    private UUID appId = UUID.randomUUID();
    private UUID partId = UUID.randomUUID();
    private UUID classId = UUID.randomUUID();

    @Test
    void testRowMapper_AllBranches() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // 1. All non-null
        when(rs.getString("ApplicationID")).thenReturn(appId.toString());
        when(rs.getBoolean("ParticipantType")).thenReturn(true);
        when(rs.getString("NationalID")).thenReturn("N123");
        when(rs.getString("Name")).thenReturn("Name");
        when(rs.getBoolean("Gender")).thenReturn(false);
        when(rs.getString("RelationShip")).thenReturn("Rel");
        when(rs.getString("Occupation")).thenReturn("Occ");
        when(rs.getString("PhoneNumber")).thenReturn("123");
        when(rs.getString("HouseholdAddress")).thenReturn("Addr1");
        when(rs.getString("MailingAddress")).thenReturn("Addr2");
        when(rs.getString("Email")).thenReturn("e@e.com");
        when(rs.getDate("BirthDate")).thenReturn(Date.valueOf(LocalDate.of(2020, 1, 1)));
        when(rs.getBoolean("IsSuspended")).thenReturn(true);
        when(rs.getDate("SuspendEnd")).thenReturn(Date.valueOf(LocalDate.of(2021, 1, 1)));
        when(rs.getInt("CurrentOrder")).thenReturn(5);
        when(rs.getString("Status")).thenReturn("Stat");
        when(rs.getString("Reason")).thenReturn("Rea");
        when(rs.getString("ClassID")).thenReturn(classId.toString());

        ArgumentCaptor<RowMapper<ApplicationParticipants>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        repository.findAll();
        verify(jdbcTemplate).query(anyString(), mapperCaptor.capture());

        ApplicationParticipants result = mapperCaptor.getValue().mapRow(rs, 1);

        assertNotNull(result);
        assertEquals(appId, result.getApplicationID());
        assertTrue(result.getParticipantType());
        assertEquals("N123", result.getNationalID());
        assertEquals(LocalDate.of(2020, 1, 1), result.getBirthDate());
        assertEquals(LocalDate.of(2021, 1, 1), result.getSuspendEnd());
        assertEquals(5, result.getCurrentOrder());
        assertEquals(classId, result.getClassID());

        // 2. All null (except booleans/ints which return default)
        reset(rs);
        when(rs.getString("ApplicationID")).thenReturn(null);
        when(rs.getDate("BirthDate")).thenReturn(null);
        when(rs.getDate("SuspendEnd")).thenReturn(null);
        when(rs.getString("ClassID")).thenReturn(null);

        result = mapperCaptor.getValue().mapRow(rs, 1);
        assertNull(result.getApplicationID());
        assertNull(result.getBirthDate());
        assertNull(result.getSuspendEnd());
        assertNull(result.getClassID());
    }

    @Test
    void testSave_InsertPath() {
        // Path 1: ID is null -> generates ID then inserts
        ApplicationParticipants ap1 = new ApplicationParticipants();
        ap1.setParticipantID(null);
        ap1.setApplicationID(appId);
        ap1.setClassID(classId);

        when(jdbcTemplate.update(startsWith("INSERT"), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        repository.save(ap1);
        assertNotNull(ap1.getParticipantID());
        verify(jdbcTemplate).update(startsWith("INSERT"), eq(ap1.getParticipantID().toString()), eq(appId.toString()),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), eq(classId.toString()), any());

        // Path 2: ID is provided but not exists -> inserts
        ApplicationParticipants ap2 = new ApplicationParticipants();
        ap2.setParticipantID(partId);
        when(jdbcTemplate.queryForObject(contains("COUNT"), eq(Integer.class), eq(partId.toString()))).thenReturn(0);

        repository.save(ap2);
        verify(jdbcTemplate).update(startsWith("INSERT"), eq(partId.toString()), isNull(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), isNull(), any());
    }

    @Test
    void testSave_UpdatePath() {
        ApplicationParticipants ap = new ApplicationParticipants();
        ap.setParticipantID(partId);
        ap.setApplicationID(appId);
        ap.setClassID(classId);

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(1);
        when(jdbcTemplate.update(startsWith("UPDATE"), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        repository.save(ap);

        verify(jdbcTemplate).update(startsWith("UPDATE"),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(),
                eq(classId.toString()),
                eq(appId.toString()));

        // Also test update with nulls
        ap.setApplicationID(null);
        ap.setClassID(null);
        repository.save(ap);
        verify(jdbcTemplate).update(startsWith("UPDATE"),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(),
                isNull(),
                isNull());
    }

    @Test
    void testExistsById_Various() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(1) // exists
                .thenReturn(0) // not exists
                .thenReturn(null); // null

        assertTrue(repository.existsById(partId));
        assertFalse(repository.existsById(partId));
        assertFalse(repository.existsById(partId));
    }

    @Test
    void testCount_NullResult() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(null);

        long count = repository.count();
        assertEquals(0, count);
    }

    @Test
    void testCountApplicationsByChildNationalID_NullResult() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(null);

        int count = repository.countApplicationsByChildNationalID("N123");
        assertEquals(0, count);
    }
}

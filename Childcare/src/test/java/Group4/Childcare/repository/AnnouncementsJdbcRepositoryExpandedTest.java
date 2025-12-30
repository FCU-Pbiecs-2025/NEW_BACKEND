package Group4.Childcare.repository;

import Group4.Childcare.DTO.AnnouncementSummaryDTO;
import Group4.Childcare.Model.Announcements;
import Group4.Childcare.Repository.AnnouncementsJdbcRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AnnouncementsJdbcRepositoryExpandedTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AnnouncementsJdbcRepository repository;

    private UUID testId = UUID.randomUUID();

    @Test
    void testMainRowMapper_NullBranches() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("AnnouncementID")).thenReturn(null);
        when(rs.getString("Title")).thenReturn("Title");
        when(rs.getDate("StartDate")).thenReturn(null);
        when(rs.getDate("EndDate")).thenReturn(null);
        when(rs.getTimestamp("CreatedTime")).thenReturn(null);
        when(rs.getTimestamp("UpdatedTime")).thenReturn(null);

        ArgumentCaptor<RowMapper<Announcements>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        repository.findAll();
        verify(jdbcTemplate).query(anyString(), mapperCaptor.capture());

        Announcements result = mapperCaptor.getValue().mapRow(rs, 1);
        assertNotNull(result);
        assertNull(result.getAnnouncementID());
        assertNull(result.getStartDate());
        assertNull(result.getEndDate());
        assertNull(result.getCreatedTime());
        assertNull(result.getUpdatedTime());
    }

    @Test
    void testSummaryRowMapper_NullBranches() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("AnnouncementID")).thenReturn(testId.toString());
        when(rs.getString("Title")).thenReturn("Title");
        when(rs.getDate("StartDate")).thenReturn(null);
        when(rs.getObject("Type")).thenReturn(null);

        ArgumentCaptor<RowMapper<AnnouncementSummaryDTO>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        repository.findSummaryData();
        verify(jdbcTemplate).query(anyString(), mapperCaptor.capture());

        AnnouncementSummaryDTO result = mapperCaptor.getValue().mapRow(rs, 1);
        assertNotNull(result);
        assertNull(result.getStartDate());
        assertNull(result.getType());
    }

    @Test
    void testSave_UpdateTernaryNullCheck() {
        Announcements announcement = new Announcements();
        announcement.setAnnouncementID(testId);

        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), anyString()))
                .thenReturn(1);

        repository.save(announcement);

        // Verify 12 arguments after SQL for update. Position 12 (index 13 total) is
        // AnnouncementID.
        // String sql = "UPDATE ... WHERE AnnouncementID = ?";
        // params: title, content, type, startDate, endDate, status, cuser, ctime,
        // uuser, utime, apath, AID
        verify(jdbcTemplate).update(startsWith("UPDATE"),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), eq(testId.toString()));
    }

    @Test
    void testExistsById_NullResult() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(null);
        assertFalse(repository.existsById(testId));
    }

    @Test
    void testCount_NullResult() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(null);
        assertEquals(0, repository.count());
    }

    @Test
    void testCountTotal_NullResult() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(null);
        assertEquals(0, repository.countTotal());
    }
}

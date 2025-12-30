package Group4.Childcare.repository;

import Group4.Childcare.Model.Banners;
import Group4.Childcare.Repository.BannersJdbcRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class BannersJdbcRepositoryExtraTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private BannersJdbcRepository repository;

    @Test
    void testRowMapper_NullBranchesAndCatch() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("SortOrder")).thenReturn(1);
        when(rs.getTimestamp("StartTime")).thenReturn(null);
        when(rs.getTimestamp("EndTime")).thenReturn(null);
        when(rs.getString("ImageName")).thenReturn("img");

        // Test catch block for status
        when(rs.getBoolean("Status")).thenThrow(new SQLException("Not a boolean"));
        when(rs.getInt("Status")).thenReturn(1);

        ArgumentCaptor<RowMapper<Banners>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        repository.findAll();
        verify(jdbcTemplate).query(anyString(), mapperCaptor.capture());

        Banners result = mapperCaptor.getValue().mapRow(rs, 1);
        assertNotNull(result);
        assertNull(result.getStartTime());
        assertNull(result.getEndTime());
        assertTrue(result.getStatus());
    }

    @Test
    void testInsert_PreparedStatementCallback() throws Exception {
        Banners banner = new Banners(1, LocalDateTime.now(), LocalDateTime.now().plusDays(1), "img", null, true);

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1))).thenReturn(0);
        when(jdbcTemplate.update(any(PreparedStatementCreator.class))).thenReturn(1);

        repository.save(banner);

        ArgumentCaptor<PreparedStatementCreator> pscCaptor = ArgumentCaptor.forClass(PreparedStatementCreator.class);
        verify(jdbcTemplate).update(pscCaptor.capture());

        // Target the lambda
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(ps);

        pscCaptor.getValue().createPreparedStatement(conn);

        // Verify null linkUrl path
        verify(ps).setNull(eq(5), anyInt());

        // Test non-null linkUrl path
        banner.setLinkUrl("http://link");
        repository.save(banner);
        verify(jdbcTemplate, times(2)).update(pscCaptor.capture());
        pscCaptor.getAllValues().get(1).createPreparedStatement(conn);
        verify(ps).setString(5, "http://link");
    }

    @Test
    void testUpdate_PreparedStatementCallback() throws Exception {
        Banners existing = new Banners(1, LocalDateTime.now(), LocalDateTime.now().plusDays(1), "img", "url", true);
        Banners incoming = new Banners();
        incoming.setSortOrder(1);
        incoming.setStartTime(null); // to trigger merge branches
        incoming.setEndTime(null);
        incoming.setImageName(null);
        incoming.setStatus(null);

        when(jdbcTemplate.queryForObject(contains("SortOrder = ?"), eq(Integer.class), eq(1))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(1))).thenReturn(existing);
        when(jdbcTemplate.update(any(PreparedStatementCreator.class))).thenReturn(1);

        repository.save(incoming);

        ArgumentCaptor<PreparedStatementCreator> pscCaptor = ArgumentCaptor.forClass(PreparedStatementCreator.class);
        verify(jdbcTemplate).update(pscCaptor.capture());

        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(ps);

        pscCaptor.getValue().createPreparedStatement(conn);

        // Verify merge branches - should use existing values which are non-null
        verify(ps, times(2)).setTimestamp(anyInt(), any(Timestamp.class));
        verify(ps).setString(eq(3), eq("img"));
    }

    @Test
    void testFindByDateRange_Branches() throws Exception {
        when(jdbcTemplate.query(any(PreparedStatementCreator.class), any(RowMapper.class)))
                .thenReturn(Collections.emptyList());

        // 1. startDate non-null, endDate null
        repository.findByDateRangeWithOffset(new Timestamp(System.currentTimeMillis()), null, 0, 10);

        // 2. startDate null, endDate non-null
        repository.findByDateRangeWithOffset(null, new Timestamp(System.currentTimeMillis()), 0, 10);

        ArgumentCaptor<PreparedStatementCreator> pscCaptor = ArgumentCaptor.forClass(PreparedStatementCreator.class);
        verify(jdbcTemplate, times(2)).query(pscCaptor.capture(), any(RowMapper.class));

        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(ps);

        // Trigger lambdas to cover dynamic SQL branches
        pscCaptor.getAllValues().get(0).createPreparedStatement(conn);
        pscCaptor.getAllValues().get(1).createPreparedStatement(conn);

        verify(ps, times(2)).setTimestamp(eq(1), any(Timestamp.class));
    }

    @Test
    void testCountByDateRange_Branches() throws Exception {
        when(jdbcTemplate.query(any(PreparedStatementCreator.class), any(ResultSetExtractor.class)))
                .thenReturn(10L);

        // Test with both non-null
        Timestamp t = new Timestamp(System.currentTimeMillis());
        repository.countByDateRange(t, t);

        ArgumentCaptor<PreparedStatementCreator> pscCaptor = ArgumentCaptor.forClass(PreparedStatementCreator.class);
        verify(jdbcTemplate).query(pscCaptor.capture(), any(ResultSetExtractor.class));

        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(ps);

        pscCaptor.getValue().createPreparedStatement(conn);

        verify(ps).setTimestamp(1, t);
        verify(ps).setTimestamp(2, t);
    }

    @Test
    void testExistsById_NullCount() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyInt()))
                .thenReturn(null);
        assertFalse(repository.existsById(1));
    }

    @Test
    void testCount_NullResult() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(null);
        assertEquals(0, repository.count());
    }
}

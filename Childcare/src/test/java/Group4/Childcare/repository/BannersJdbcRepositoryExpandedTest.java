package Group4.Childcare.repository;

import Group4.Childcare.Model.Banners;
import Group4.Childcare.Repository.BannersJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class BannersJdbcRepositoryExpandedTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private BannersJdbcRepository repository;

    private Banners testBanner;

    @BeforeEach
    void setUp() {
        testBanner = new Banners();
        testBanner.setSortOrder(1);
        testBanner.setStartTime(LocalDateTime.now());
        testBanner.setEndTime(LocalDateTime.now().plusDays(1));
        testBanner.setImageName("test.jpg");
        testBanner.setStatus(true);
        testBanner.setLinkUrl("http://test.com");
    }

    @Test
    void testRowMapper_AllBranches() throws SQLException {
        // Capture the static RowMapper
        ArgumentCaptor<RowMapper<Banners>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbcTemplate.query(anyString(), mapperCaptor.capture())).thenReturn(Collections.emptyList());
        
        repository.findAll();
        RowMapper<Banners> mapper = mapperCaptor.getValue();

        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("SortOrder")).thenReturn(1);
        when(rs.getTimestamp("StartTime")).thenReturn(Timestamp.valueOf(testBanner.getStartTime()));
        when(rs.getTimestamp("EndTime")).thenReturn(Timestamp.valueOf(testBanner.getEndTime()));
        when(rs.getString("ImageName")).thenReturn("img.jpg");
        when(rs.getString("LinkUrl")).thenReturn("url");
        when(rs.getBoolean("Status")).thenReturn(true);

        // Branch: StartTime/EndTime not null, Status as boolean
        Banners result = mapper.mapRow(rs, 1);
        assertNotNull(result);
        assertEquals(1, result.getSortOrder());
        assertEquals("img.jpg", result.getImageName());
        assertTrue(result.getStatus());

        // Branch: StartTime/EndTime null, Status as int (exception branch)
        when(rs.getTimestamp("StartTime")).thenReturn(null);
        when(rs.getTimestamp("EndTime")).thenReturn(null);
        when(rs.getBoolean("Status")).thenThrow(new SQLException("Not a boolean"));
        when(rs.getInt("Status")).thenReturn(1);

        result = mapper.mapRow(rs, 2);
        assertNull(result.getStartTime());
        assertNull(result.getEndTime());
        assertTrue(result.getStatus());

        // Branch: Status as int 0
        when(rs.getInt("Status")).thenReturn(0);
        result = mapper.mapRow(rs, 3);
        assertFalse(result.getStatus());
    }

    @Test
    void testSave_Insert_Branches() throws SQLException {
        // Branch: banner.getSortOrder() <= 0
        Banners b0 = new Banners();
        b0.setSortOrder(0);
        assertThrows(IllegalArgumentException.class, () -> repository.save(b0));

        // Branch: required fields null
        Banners b1 = new Banners();
        b1.setSortOrder(1);
        b1.setStartTime(null);
        // Mock existsById to return false so it goes to insert
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyInt())).thenReturn(0);
        assertThrows(IllegalArgumentException.class, () -> repository.save(b1));

        b1.setStartTime(LocalDateTime.now());
        b1.setEndTime(null);
        assertThrows(IllegalArgumentException.class, () -> repository.save(b1));

        b1.setEndTime(LocalDateTime.now());
        b1.setImageName(null);
        assertThrows(IllegalArgumentException.class, () -> repository.save(b1));

        b1.setImageName("img");
        b1.setStatus(null);
        assertThrows(IllegalArgumentException.class, () -> repository.save(b1));

        // Branch: existsById returns false
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyInt())).thenReturn(0);
        when(jdbcTemplate.update(any(PreparedStatementCreator.class))).thenReturn(1);
        
        repository.save(testBanner);
        
        // Test lambda$insert$1
        ArgumentCaptor<PreparedStatementCreator> pscCaptor = ArgumentCaptor.forClass(PreparedStatementCreator.class);
        verify(jdbcTemplate).update(pscCaptor.capture());
        
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        
        pscCaptor.getValue().createPreparedStatement(conn);
        verify(ps).setInt(1, testBanner.getSortOrder());
        verify(ps).setTimestamp(2, Timestamp.valueOf(testBanner.getStartTime()));
        verify(ps).setTimestamp(3, Timestamp.valueOf(testBanner.getEndTime()));
        verify(ps).setString(4, testBanner.getImageName());
        verify(ps).setString(5, testBanner.getLinkUrl());
        verify(ps).setBoolean(6, testBanner.getStatus());

        // Branch: LinkUrl is null
        testBanner.setLinkUrl(null);
        pscCaptor.getValue().createPreparedStatement(conn);
        verify(ps).setNull(5, java.sql.Types.NVARCHAR);

        // Branch: rowsAffected == 0
        when(jdbcTemplate.update(any(PreparedStatementCreator.class))).thenReturn(0);
        assertThrows(RuntimeException.class, () -> repository.save(testBanner));

        // Branch: Exception in update
        when(jdbcTemplate.update(any(PreparedStatementCreator.class))).thenThrow(new RuntimeException("DB Error"));
        assertThrows(RuntimeException.class, () -> repository.save(testBanner));
    }

    @Test
    void testSave_Update_Branches() throws SQLException {
        // Branch: existsById returns true
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyInt())).thenReturn(1);
        
        // Mock findById inside update
        Banners existing = new Banners();
        existing.setSortOrder(1);
        existing.setStartTime(LocalDateTime.now().minusDays(1));
        existing.setEndTime(LocalDateTime.now().plusDays(2));
        existing.setImageName("old.jpg");
        existing.setStatus(false);
        when(jdbcTemplate.queryForObject(contains("WHERE SortOrder = ?"), any(RowMapper.class), anyInt())).thenReturn(existing);
        
        when(jdbcTemplate.update(any(PreparedStatementCreator.class))).thenReturn(1);

        // 1. updateReq fields not null
        Banners updateReq1 = new Banners();
        updateReq1.setSortOrder(1);
        updateReq1.setStartTime(LocalDateTime.now());
        updateReq1.setEndTime(LocalDateTime.now().plusDays(1));
        updateReq1.setImageName("new.jpg");
        updateReq1.setStatus(true);
        updateReq1.setLinkUrl("new-url");
        repository.save(updateReq1);

        // 2. Merge null fields (existing has values)
        Banners updateReq2 = new Banners();
        updateReq2.setSortOrder(1);
        updateReq2.setStartTime(null);
        updateReq2.setEndTime(null);
        updateReq2.setImageName(null);
        updateReq2.setStatus(null);
        updateReq2.setLinkUrl(null);
        repository.save(updateReq2);
        
        // 3. Merge null fields (existing has NULL values for StartTime/EndTime)
        existing.setStartTime(null);
        existing.setEndTime(null);
        Banners updateReq3 = new Banners();
        updateReq3.setSortOrder(1);
        updateReq3.setStartTime(null);
        updateReq3.setEndTime(null);
        repository.save(updateReq3);

        // Test lambda$update$2
        ArgumentCaptor<PreparedStatementCreator> pscCaptor = ArgumentCaptor.forClass(PreparedStatementCreator.class);
        verify(jdbcTemplate, atLeastOnce()).update(pscCaptor.capture());
        List<PreparedStatementCreator> allPscs = pscCaptor.getAllValues();
        
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        
        // Test the first call (LinkUrl != null)
        allPscs.get(0).createPreparedStatement(conn);
        verify(ps).setString(4, "new-url");

        // Test the last call (StartTime/EndTime are null)
        PreparedStatementCreator lastPsc = allPscs.get(allPscs.size() - 1);
        lastPsc.createPreparedStatement(conn);
        verify(ps).setNull(1, java.sql.Types.TIMESTAMP);
        verify(ps).setNull(2, java.sql.Types.TIMESTAMP);
        verify(ps).setNull(4, java.sql.Types.NVARCHAR);

        // Branch: existingOpt.isEmpty()
        when(jdbcTemplate.queryForObject(contains("WHERE SortOrder = ?"), any(RowMapper.class), anyInt())).thenReturn(null);
        assertThrows(RuntimeException.class, () -> repository.save(updateReq1));

        // Branch: rowsAffected == 0
        when(jdbcTemplate.queryForObject(contains("WHERE SortOrder = ?"), any(RowMapper.class), anyInt())).thenReturn(existing);
        when(jdbcTemplate.update(any(PreparedStatementCreator.class))).thenReturn(0);
        assertThrows(RuntimeException.class, () -> repository.save(updateReq1));
    }

    @Test
    void testExistsById_NullCount() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyInt())).thenReturn(null);
        assertFalse(repository.existsById(1));
    }

    @Test
    void testCount_NullCount() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(null);
        assertEquals(0, repository.count());
    }

    @Test
    void testFindByDateRangeWithOffset_Branches() throws SQLException {
        Timestamp start = Timestamp.valueOf(LocalDateTime.now());
        Timestamp end = Timestamp.valueOf(LocalDateTime.now().plusDays(1));
        
        // Branch: both not null
        repository.findByDateRangeWithOffset(start, end, 0, 10);
        
        ArgumentCaptor<PreparedStatementCreator> pscCaptor = ArgumentCaptor.forClass(PreparedStatementCreator.class);
        verify(jdbcTemplate).query(pscCaptor.capture(), any(RowMapper.class));
        
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        
        pscCaptor.getValue().createPreparedStatement(conn);
        verify(ps).setTimestamp(1, start);
        verify(ps).setTimestamp(2, end);
        verify(ps).setInt(3, 0);
        verify(ps).setInt(4, 10);

        // Branch: both null
        repository.findByDateRangeWithOffset(null, null, 5, 20);
        verify(jdbcTemplate, times(2)).query(pscCaptor.capture(), any(RowMapper.class));
        pscCaptor.getValue().createPreparedStatement(conn);
        verify(ps).setInt(1, 5);
        verify(ps).setInt(2, 20);
    }

    @Test
    void testCountByDateRange_Branches() throws SQLException {
        Timestamp start = Timestamp.valueOf(LocalDateTime.now());
        Timestamp end = Timestamp.valueOf(LocalDateTime.now().plusDays(1));
        
        // Branch: both not null
        when(jdbcTemplate.query(any(PreparedStatementCreator.class), any(ResultSetExtractor.class))).thenReturn(5L);
        repository.countByDateRange(start, end);
        
        ArgumentCaptor<PreparedStatementCreator> pscCaptor = ArgumentCaptor.forClass(PreparedStatementCreator.class);
        ArgumentCaptor<ResultSetExtractor<Long>> rseCaptor = ArgumentCaptor.forClass(ResultSetExtractor.class);
        verify(jdbcTemplate, atLeastOnce()).query(pscCaptor.capture(), rseCaptor.capture());
        
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        
        pscCaptor.getValue().createPreparedStatement(conn);
        verify(ps).setTimestamp(1, start);
        verify(ps).setTimestamp(2, end);

        // Test lambda$countByDateRange$5 (ResultSetExtractor)
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true);
        when(rs.getLong(1)).thenReturn(5L);
        assertEquals(5L, rseCaptor.getValue().extractData(rs));

        when(rs.next()).thenReturn(false);
        assertEquals(0L, rseCaptor.getValue().extractData(rs));

        // Branch: both null
        repository.countByDateRange(null, null);
        verify(jdbcTemplate, atLeastOnce()).query(pscCaptor.capture(), any(ResultSetExtractor.class));
        pscCaptor.getValue().createPreparedStatement(conn);
        // No setTimestamp should be called
    }

    @Test
    void testInsert_NullBanner() throws Exception {
        java.lang.reflect.Method method = BannersJdbcRepository.class.getDeclaredMethod("insert", Banners.class);
        method.setAccessible(true);
        try {
            method.invoke(repository, (Banners) null);
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    void testUpdate_NullBanner() throws Exception {
        java.lang.reflect.Method method = BannersJdbcRepository.class.getDeclaredMethod("update", Banners.class);
        method.setAccessible(true);
        try {
            method.invoke(repository, (Banners) null);
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
        }
    }
}

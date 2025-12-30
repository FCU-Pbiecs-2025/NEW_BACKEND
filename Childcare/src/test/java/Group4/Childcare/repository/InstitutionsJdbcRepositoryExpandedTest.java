package Group4.Childcare.repository;

import Group4.Childcare.Model.Institutions;
import Group4.Childcare.Repository.InstitutionsJdbcRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class InstitutionsJdbcRepositoryExpandedTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private InstitutionsJdbcRepository repository;

    private UUID testId = UUID.randomUUID();

    @Test
    void testRowMapper_NullBranches() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("InstitutionID")).thenReturn(testId.toString());
        when(rs.getTimestamp("CreatedTime")).thenReturn(null);
        when(rs.getTimestamp("UpdatedTime")).thenReturn(null);

        ArgumentCaptor<RowMapper<Institutions>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        repository.findAll();
        verify(jdbcTemplate).query(anyString(), mapperCaptor.capture());

        Institutions result = mapperCaptor.getValue().mapRow(rs, 1);
        assertNotNull(result);
        assertNull(result.getCreatedTime());
        assertNull(result.getUpdatedTime());
    }

    @Test
    void testInsert_AutoTimestampBranches() {
        Institutions inst = new Institutions();
        inst.setInstitutionID(null);
        inst.setCreatedTime(null);
        inst.setUpdatedTime(null);

        when(jdbcTemplate.update(anyString(), (Object[]) any())).thenReturn(1);

        Institutions result = repository.save(inst);

        assertNotNull(result.getInstitutionID());
        assertNotNull(result.getCreatedTime());
        assertNotNull(result.getUpdatedTime());

        // Test with existing times (False branches)
        LocalDateTime existing = LocalDateTime.now().minusDays(1);
        inst.setInstitutionID(null);
        inst.setCreatedTime(existing);
        inst.setUpdatedTime(existing);
        repository.save(inst);
        assertEquals(existing, inst.getCreatedTime());
        assertEquals(existing, inst.getUpdatedTime());
    }

    @Test
    void testFindById_Catch() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                .thenThrow(new RuntimeException("DB error"));

        var result = repository.findById(testId);
        assertFalse(result.isPresent());
    }

    @Test
    void testExistsById_NullCount() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(null);
        assertFalse(repository.existsById(testId));
    }

    @Test
    void testCountMethods_NullResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString()))
                .thenReturn(null);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(null);

        assertEquals(0, repository.count());
        assertEquals(0, repository.countByInstitutionID(testId));
        assertEquals(0, repository.countAllWithSearch("abc"));
        assertEquals(0, repository.countByInstitutionIDWithSearch(testId, "abc"));
        assertEquals(0, repository.countAllWithNameSearch("abc"));
        assertEquals(0, repository.countByInstitutionIDWithNameSearch(testId, "abc"));
    }
}

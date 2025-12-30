package Group4.Childcare.repository;

import Group4.Childcare.Repository.ApplicationsJdbcRepository;
import java.time.LocalDateTime;

import Group4.Childcare.Model.Applications;
import Group4.Childcare.DTO.ApplicationSummaryDTO;
import Group4.Childcare.DTO.ApplicationSummaryWithDetailsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationsJdbcRepositoryExtraTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ApplicationsJdbcRepository repository;

    private Applications app;
    private UUID appId;

    @BeforeEach
    void setUp() {
        appId = UUID.randomUUID();
        app = new Applications();
        app.setApplicationID(appId);
        app.setApplicationDate(java.time.LocalDate.now());
        app.setCaseNumber(123L);
        app.setInstitutionID(UUID.randomUUID());
        app.setUserID(UUID.randomUUID());
        app.setIdentityType((byte) 1);
        app.setAttachmentPath("path/to/file");
    }

    @Test
    void testFindById_Found() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(appId.toString()))).thenReturn(app);
        Optional<Applications> result = repository.findById(appId);
        assertTrue(result.isPresent());
        assertEquals(appId, result.get().getApplicationID());
    }

    @Test
    void testFindById_NotFound() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                .thenThrow(new EmptyResultDataAccessException(1));
        Optional<Applications> result = repository.findById(UUID.randomUUID());
        assertFalse(result.isPresent());
    }

    @Test
    void testFindAll_ReturnsList() {
        List<Applications> list = Arrays.asList(app, app);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(list);
        List<Applications> result = repository.findAll();
        assertEquals(2, result.size());
    }

    @Test
    void testFindSummariesWithOffset_Pagination() {
        ApplicationSummaryWithDetailsDTO dto = new ApplicationSummaryWithDetailsDTO();
        List<ApplicationSummaryWithDetailsDTO> list = Collections.singletonList(dto);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt())).thenReturn(list);
        List<ApplicationSummaryWithDetailsDTO> result = repository.findSummariesWithOffset(0, 10);
        assertEquals(1, result.size());
    }

    @Test
    void testExistsById_True() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(appId.toString()))).thenReturn(1);
        assertTrue(repository.existsById(appId));
    }

    @Test
    void testExistsById_False() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0);
        assertFalse(repository.existsById(UUID.randomUUID()));
    }

    @Test
    void testCount() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(42L);
        assertEquals(42L, repository.count());
    }

    @Test
    void testFindSummaryByUserID() {
        ApplicationSummaryDTO dto = new ApplicationSummaryDTO();
        dto.setApplicationID(appId);
        List<ApplicationSummaryDTO> list = Collections.singletonList(dto);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any())).thenReturn(list);
        List<ApplicationSummaryDTO> result = repository.findSummaryByUserID(UUID.randomUUID());
        assertEquals(1, result.size());
        assertEquals(appId, result.get(0).getApplicationID());
    }

    @Test
    void testSearchApplications_AllParams() {
        List<ApplicationSummaryWithDetailsDTO> list = Collections.emptyList();
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class))).thenReturn(list);
        List<ApplicationSummaryWithDetailsDTO> result = repository.searchApplications("instId", "instName", "123",
                "N123");
        assertNotNull(result);
        verify(jdbcTemplate).query(contains("AND a.InstitutionID = ?"), any(Object[].class), any(RowMapper.class));
    }

    @Test
    void testSearchApplications_CaseNumberString() {
        List<ApplicationSummaryWithDetailsDTO> list = Collections.emptyList();
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class))).thenReturn(list);
        repository.searchApplications(null, null, "NotANumber", null);
        verify(jdbcTemplate).query(contains("AND a.CaseNumber = ?"), any(Object[].class), any(RowMapper.class));
    }

    @Test
    void testUpdateParticipantStatusReason_AssignOrder() {
        // Mock finding current info
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "審核中");
        currentInfo.put("CurrentOrder", null);
        currentInfo.put("ParticipantType", 0); // Is child
        when(jdbcTemplate.queryForList(anyString(), eq(appId.toString()), anyString()))
                .thenReturn(Collections.singletonList(currentInfo));

        // Mock finding institution ID
        when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class), eq(appId.toString())))
                .thenReturn(UUID.randomUUID().toString());

        // Mock finding max order
        when(jdbcTemplate.queryForObject(contains("SELECT MAX"), eq(Integer.class), anyString()))
                .thenReturn(10);

        // Mock updates
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);

        repository.updateParticipantStatusReason(appId, "N123", "候補中", "reason", LocalDateTime.now());

        // Verify that max order was queried (meaning new order logic was triggered)
        verify(jdbcTemplate).queryForObject(contains("SELECT MAX"), eq(Integer.class), anyString());
    }

    @Test
    void testUpdateParticipantStatusReason_Reorder() {
        // Mock finding current info (was waitlisted)
        Map<String, Object> currentInfo = new HashMap<>();
        currentInfo.put("Status", "候補中");
        currentInfo.put("CurrentOrder", 5);
        currentInfo.put("ParticipantType", 0);
        when(jdbcTemplate.queryForList(anyString(), eq(appId.toString()), anyString()))
                .thenReturn(Collections.singletonList(currentInfo));

        // Mock finding institution ID
        when(jdbcTemplate.queryForObject(contains("SELECT InstitutionID"), eq(String.class), eq(appId.toString())))
                .thenReturn(UUID.randomUUID().toString());

        // Mock updates (update others)
        // Note: update(sql, args...) -> args is varargs.
        // Actual call has 2 args: oldCurrentOrder, institutionID
        when(jdbcTemplate.update(contains("UPDATE application_participants SET CurrentOrder"), eq(5), anyString()))
                .thenReturn(1);

        // Main update
        // Actual call has 6 args: status, reason, reviewDate, currentOrder, appID,
        // nationalID
        when(jdbcTemplate.update(contains("UPDATE application_participants SET Status"), any(), any(), any(), any(),
                any(), any())).thenReturn(1);

        repository.updateParticipantStatusReason(appId, "N123", "已錄取", "reason", LocalDateTime.now());

        // Verify reordering update was called
        verify(jdbcTemplate).update(contains("UPDATE application_participants SET CurrentOrder"), eq(5), anyString());
    }
}

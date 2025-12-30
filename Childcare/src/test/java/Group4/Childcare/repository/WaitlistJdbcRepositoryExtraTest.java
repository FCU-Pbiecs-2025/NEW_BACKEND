package Group4.Childcare.repository;

import Group4.Childcare.Repository.WaitlistJdbcRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaitlistJdbcRepositoryExtraTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private WaitlistJdbcRepository repository;

    private UUID institutionId;
    private UUID applicationId;
    private String nationalId;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        applicationId = UUID.randomUUID();
        nationalId = "N123456789";
    }

    @Test
    void testFindWaitlistByInstitution_NoParams() {
        List<Map<String, Object>> mockResult = new ArrayList<>();
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(mockResult);
        List<Map<String, Object>> result = repository.findWaitlistByInstitution(null, null);
        assertNotNull(result);
        verify(jdbcTemplate).queryForList(anyString(), any(Object[].class));
    }

    @Test
    void testGetNextWaitlistOrder_ReturnsValue() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(institutionId.toString()))).thenReturn(5);
        int next = repository.getNextWaitlistOrder(institutionId);
        assertEquals(5, next);
    }

    @Test
    void testUpdateApplicantOrder_ExecutesUpdate() {
        when(jdbcTemplate.update(anyString(), anyInt(), anyString(), any(LocalDateTime.class),
                eq(applicationId.toString()), eq(nationalId))).thenReturn(1);
        repository.updateApplicantOrder(applicationId, nationalId, 2, "候補中", LocalDateTime.now());
        verify(jdbcTemplate).update(anyString(), anyInt(), anyString(), any(LocalDateTime.class),
                eq(applicationId.toString()), eq(nationalId));
    }

    @Test
    void testBatchUpdateApplicants_ExecutesBatch() {
        Map<String, Object> applicant = new HashMap<>();
        applicant.put("CurrentOrder", 1);
        applicant.put("Status", "候補中");
        applicant.put("Reason", "test");
        applicant.put("ClassID", UUID.randomUUID().toString());
        applicant.put("ReviewDate", LocalDateTime.now());
        applicant.put("ApplicationID", applicationId.toString());
        applicant.put("NationalID", nationalId);
        List<Map<String, Object>> list = Collections.singletonList(applicant);
        when(jdbcTemplate.batchUpdate(anyString(), any(List.class))).thenReturn(new int[] { 1 });
        repository.batchUpdateApplicants(list);
        verify(jdbcTemplate).batchUpdate(anyString(), any(List.class));
    }

    @Test
    void testHasClassCapacity_True() {
        UUID classId = UUID.randomUUID();
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(classId.toString()))).thenReturn(1);
        assertTrue(repository.hasClassCapacity(classId));
    }

    @Test
    void testManualAdmit_Success() {
        UUID classId = UUID.randomUUID();
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(classId.toString()))).thenReturn(1);
        when(jdbcTemplate.update(anyString(), anyString(), any(LocalDateTime.class), eq(applicationId.toString()),
                eq(nationalId))).thenReturn(1);
        boolean result = repository.manualAdmit(applicationId, nationalId, classId);
        assertTrue(result);
    }

    @Test
    void testCheckAdmissionOrderViolation_ReturnsList() {
        List<Map<String, Object>> mockList = new ArrayList<>();
        when(jdbcTemplate.queryForList(anyString(), eq(institutionId.toString()), anyInt())).thenReturn(mockList);
        List<Map<String, Object>> result = repository.checkAdmissionOrderViolation(institutionId, 3);
        assertNotNull(result);
        verify(jdbcTemplate).queryForList(anyString(), eq(institutionId.toString()), anyInt());
    }
}

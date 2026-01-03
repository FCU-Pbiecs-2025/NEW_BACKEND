package Group4.Childcare.repository;

import Group4.Childcare.Model.Cancellation;
import Group4.Childcare.Repository.CancellationJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class CancellationJdbcRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private CancellationJdbcRepository cancellationRepository;

    private UUID testCancellationId;
    private UUID testApplicationId;

    @BeforeEach
    void setUp() {
        testCancellationId = UUID.randomUUID();
        testApplicationId = UUID.randomUUID();
    }

    // ==================== save Tests ====================

    @Test
    void testSave_NewCancellation_WithNullId_Success() {
        // Given
        Cancellation cancellation = createTestCancellation();
        cancellation.setCancellationID(null);

        when(jdbcTemplate.update(anyString(), any(), any(), anyString(),
                any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(1);

        // When
        Cancellation result = cancellationRepository.save(cancellation);

        // Then
        assertNotNull(result);
        assertNotNull(result.getCancellationID());
        verify(jdbcTemplate, times(1)).update(anyString(), any(), any(), anyString(),
                any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testSave_ExistingCancellation_Update_Success() {
        // Given
        Cancellation cancellation = createTestCancellation();
        cancellation.setCancellationID(testCancellationId);

        when(jdbcTemplate.update(anyString(), any(), anyString(),
                any(LocalDate.class), any(LocalDate.class), any()))
            .thenReturn(1);

        // When
        Cancellation result = cancellationRepository.save(cancellation);

        // Then
        assertNotNull(result);
        assertEquals(testCancellationId, result.getCancellationID());
        verify(jdbcTemplate, times(1)).update(anyString(), any(), anyString(),
                any(LocalDate.class), any(LocalDate.class), any());
    }

    // ==================== findById Tests ====================

    @Test
    void testFindById_Success() {
        // Given
        Cancellation mockCancellation = createTestCancellation();
        mockCancellation.setCancellationID(testCancellationId);

        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(testCancellationId.toString())))
            .thenReturn(mockCancellation);

        // When
        Optional<Cancellation> result = cancellationRepository.findById(testCancellationId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testCancellationId, result.get().getCancellationID());
        assertEquals("家長主動取消", result.get().getAbandonReason());
    }

    @Test
    void testFindById_NotFound() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
            .thenThrow(new RuntimeException("Not found"));

        // When
        Optional<Cancellation> result = cancellationRepository.findById(testCancellationId);

        // Then
        assertFalse(result.isPresent());
    }

    // ==================== findAll Tests ====================

    @Test
    void testFindAll_Success() {
        // Given
        Cancellation cancellation1 = createTestCancellation();
        cancellation1.setCancellationID(UUID.randomUUID());
        cancellation1.setAbandonReason("原因一");

        Cancellation cancellation2 = createTestCancellation();
        cancellation2.setCancellationID(UUID.randomUUID());
        cancellation2.setAbandonReason("原因二");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(Arrays.asList(cancellation1, cancellation2));

        // When
        List<Cancellation> result = cancellationRepository.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("原因一", result.get(0).getAbandonReason());
        assertEquals("原因二", result.get(1).getAbandonReason());
    }

    @Test
    void testFindAll_EmptyResult() {
        // Given
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(Collections.emptyList());

        // When
        List<Cancellation> result = cancellationRepository.findAll();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }


    // ==================== count Tests ====================

    @Test
    void testCount_Success() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(15L);

        // When
        long count = cancellationRepository.count();

        // Then
        assertEquals(15L, count);
    }

    @Test
    void testCount_Zero() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(0L);

        // When
        long count = cancellationRepository.count();

        // Then
        assertEquals(0L, count);
    }

    // ==================== existsById Tests ====================

    @Test
    void testExistsById_True() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testCancellationId.toString())))
            .thenReturn(1);

        // When
        boolean exists = cancellationRepository.existsById(testCancellationId);

        // Then
        assertTrue(exists);
    }

    @Test
    void testExistsById_False() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testCancellationId.toString())))
            .thenReturn(0);

        // When
        boolean exists = cancellationRepository.existsById(testCancellationId);

        // Then
        assertFalse(exists);
    }

    // ==================== deleteById Tests ====================

    @Test
    void testDeleteById_Success() {
        // Given
        when(jdbcTemplate.update(anyString(), eq(testCancellationId.toString())))
            .thenReturn(1);

        // When
        cancellationRepository.deleteById(testCancellationId);

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), eq(testCancellationId.toString()));
    }

    // ==================== RowMapper Tests ====================

    @Test
    void testRowMapper_FullData() throws SQLException {
        // Given
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("CancellationID")).thenReturn(testCancellationId.toString());
        when(rs.getString("ApplicationID")).thenReturn(testApplicationId.toString());
        when(rs.getString("AbandonReason")).thenReturn("家長主動取消");
        when(rs.getDate("CancellationDate")).thenReturn(java.sql.Date.valueOf(LocalDate.of(2024, 1, 1)));
        when(rs.getDate("ConfirmDate")).thenReturn(java.sql.Date.valueOf(LocalDate.of(2024, 1, 2)));

        final RowMapper<Cancellation>[] capturedMapper = new RowMapper[1];
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenAnswer(invocation -> {
            capturedMapper[0] = invocation.getArgument(1);
            return Collections.emptyList();
        });

        cancellationRepository.findAll();
        assertNotNull(capturedMapper[0]);

        // When
        Cancellation result = capturedMapper[0].mapRow(rs, 1);

        // Then
        assertNotNull(result);
        assertEquals(testCancellationId, result.getCancellationID());
        assertEquals(testApplicationId, result.getApplicationID());
        assertEquals("家長主動取消", result.getAbandonReason());
        assertEquals(LocalDate.of(2024, 1, 1), result.getCancellationDate());
        assertEquals(LocalDate.of(2024, 1, 2), result.getConfirmDate());
    }

    @Test
    void testRowMapper_NullOptionalFields() throws SQLException {
        // Given
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("CancellationID")).thenReturn(testCancellationId.toString());
        when(rs.getString("ApplicationID")).thenReturn(null);
        when(rs.getString("AbandonReason")).thenReturn("無原因");
        when(rs.getDate("CancellationDate")).thenReturn(null);
        when(rs.getDate("ConfirmDate")).thenReturn(null);

        final RowMapper<Cancellation>[] capturedMapper = new RowMapper[1];
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenAnswer(invocation -> {
            capturedMapper[0] = invocation.getArgument(1);
            return Collections.emptyList();
        });

        cancellationRepository.findAll();

        // When
        Cancellation result = capturedMapper[0].mapRow(rs, 1);

        // Then
        assertNotNull(result);
        assertNull(result.getApplicationID());
        assertNull(result.getCancellationDate());
        assertNull(result.getConfirmDate());
    }

    // ==================== Additional Branch Tests ====================

    @Test
    void testSave_NullApplicationId_Success() {
        // Given
        Cancellation cancellation = createTestCancellation();
        cancellation.setCancellationID(null);
        cancellation.setApplicationID(null);

        when(jdbcTemplate.update(anyString(), anyString(), isNull(), anyString(),
                any(), any()))
            .thenReturn(1);

        // When
        Cancellation result = cancellationRepository.save(cancellation);

        // Then
        assertNotNull(result);
        assertNull(result.getApplicationID());
    }

    @Test
    void testUpdate_NullApplicationId_Success() {
        // Given
        Cancellation cancellation = createTestCancellation();
        cancellation.setCancellationID(testCancellationId);
        cancellation.setApplicationID(null);

        when(jdbcTemplate.update(anyString(), isNull(), anyString(),
                any(), any(), anyString()))
            .thenReturn(1);

        // When
        Cancellation result = cancellationRepository.save(cancellation);

        // Then
        assertNotNull(result);
        assertNull(result.getApplicationID());
    }

    @Test
    void testCount_Null() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(null);

        // When
        long count = cancellationRepository.count();

        // Then
        assertEquals(0L, count);
    }

    @Test
    void testExistsById_Null() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
            .thenReturn(null);

        // When
        boolean exists = cancellationRepository.existsById(testCancellationId);

        // Then
        assertFalse(exists);
    }

    @Test
    void testDelete_Entity_Success() {
        // Given
        Cancellation cancellation = createTestCancellation();

        // When
        cancellationRepository.delete(cancellation);

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), eq(testCancellationId.toString()));
    }

    // ==================== Helper Methods ====================

    private Cancellation createTestCancellation() {
        Cancellation cancellation = new Cancellation();
        cancellation.setCancellationID(testCancellationId);
        cancellation.setApplicationID(testApplicationId);
        cancellation.setAbandonReason("家長主動取消");
        cancellation.setCancellationDate(LocalDate.now());
        cancellation.setConfirmDate(LocalDate.now().plusDays(1));
        return cancellation;
    }
}


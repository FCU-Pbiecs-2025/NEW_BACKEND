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

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CancellationJdbcRepository ?��?測試
 * 測試?��??��?管�??��??��??�庫?��?
 */
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


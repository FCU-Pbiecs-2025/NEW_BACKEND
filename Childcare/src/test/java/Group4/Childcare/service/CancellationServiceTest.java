package Group4.Childcare.service;

import Group4.Childcare.Model.Cancellation;
import Group4.Childcare.Repository.CancellationJdbcRepository;
import Group4.Childcare.Service.CancellationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CancellationService 單元測試
 */
@ExtendWith(MockitoExtension.class)
class CancellationServiceTest {

    @Mock
    private CancellationJdbcRepository repository;

    @InjectMocks
    private CancellationService service;

    private UUID testId;
    private Cancellation testCancellation;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testCancellation = new Cancellation();
        testCancellation.setCancellationID(testId);
    }

    @Test
    void testCreate_Success() {
        when(repository.save(any(Cancellation.class))).thenReturn(testCancellation);

        Cancellation result = service.create(testCancellation);

        assertNotNull(result);
        assertEquals(testId, result.getCancellationID());
        verify(repository, times(1)).save(testCancellation);
    }

    @Test
    void testGetById_Found() {
        when(repository.findById(testId)).thenReturn(Optional.of(testCancellation));

        Optional<Cancellation> result = service.getById(testId);

        assertTrue(result.isPresent());
        assertEquals(testId, result.get().getCancellationID());
    }

    @Test
    void testGetById_NotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        Optional<Cancellation> result = service.getById(nonExistentId);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAll_Success() {
        List<Cancellation> cancellations = Arrays.asList(testCancellation, new Cancellation());
        when(repository.findAll()).thenReturn(cancellations);

        List<Cancellation> result = service.getAll();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetAll_Empty() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<Cancellation> result = service.getAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testUpdate_Success() {
        Cancellation updatedCancellation = new Cancellation();
        when(repository.save(any(Cancellation.class))).thenReturn(updatedCancellation);

        Cancellation result = service.update(testId, updatedCancellation);

        assertNotNull(result);
        assertEquals(testId, updatedCancellation.getCancellationID());
        verify(repository, times(1)).save(updatedCancellation);
    }
}

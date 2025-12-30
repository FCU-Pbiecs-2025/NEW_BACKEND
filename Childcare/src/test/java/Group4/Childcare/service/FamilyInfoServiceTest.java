package Group4.Childcare.service;

import Group4.Childcare.Model.FamilyInfo;
import Group4.Childcare.Repository.FamilyInfoJdbcRepository;
import Group4.Childcare.Service.FamilyInfoService;
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
 * FamilyInfoService 單元測試
 */
@ExtendWith(MockitoExtension.class)
class FamilyInfoServiceTest {

    @Mock
    private FamilyInfoJdbcRepository repository;

    @InjectMocks
    private FamilyInfoService service;

    private UUID testId;
    private FamilyInfo testFamilyInfo;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testFamilyInfo = new FamilyInfo();
        testFamilyInfo.setFamilyInfoID(testId);
    }

    @Test
    void testCreate_Success() {
        when(repository.save(any(FamilyInfo.class))).thenReturn(testFamilyInfo);

        FamilyInfo result = service.create(testFamilyInfo);

        assertNotNull(result);
        assertEquals(testId, result.getFamilyInfoID());
        verify(repository, times(1)).save(testFamilyInfo);
    }

    @Test
    void testGetById_Found() {
        when(repository.findById(testId)).thenReturn(Optional.of(testFamilyInfo));

        Optional<FamilyInfo> result = service.getById(testId);

        assertTrue(result.isPresent());
        assertEquals(testId, result.get().getFamilyInfoID());
    }

    @Test
    void testGetById_NotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        Optional<FamilyInfo> result = service.getById(nonExistentId);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAll_Success() {
        List<FamilyInfo> familyInfos = Arrays.asList(testFamilyInfo, new FamilyInfo());
        when(repository.findAll()).thenReturn(familyInfos);

        List<FamilyInfo> result = service.getAll();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetAll_Empty() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<FamilyInfo> result = service.getAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testUpdate_Success() {
        FamilyInfo updatedFamilyInfo = new FamilyInfo();
        when(repository.save(any(FamilyInfo.class))).thenReturn(updatedFamilyInfo);

        FamilyInfo result = service.update(testId, updatedFamilyInfo);

        assertNotNull(result);
        assertEquals(testId, updatedFamilyInfo.getFamilyInfoID());
        verify(repository, times(1)).save(updatedFamilyInfo);
    }
}

package Group4.Childcare.service;

import Group4.Childcare.Model.ChildInfo;
import Group4.Childcare.Repository.ChildInfoJdbcRepository;
import Group4.Childcare.Service.ChildInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ChildInfoService 單元測試
 *
 * 測試範圍：
 * 1. create() - 創建兒童資料
 * 2. getById() - 根據ID查詢兒童資料
 * 3. getAll() - 查詢所有兒童資料
 * 4. update() - 更新兒童資料
 * 5. getByFamilyInfoID() - 根據家庭資料ID查詢兒童資料
 * 6. deleteByChildId() - 刪除兒童資料
 */
@ExtendWith(MockitoExtension.class)
class ChildInfoServiceTest {

    @Mock
    private ChildInfoJdbcRepository repository;

    @InjectMocks
    private ChildInfoService service;

    private ChildInfo testChild;
    private UUID testChildId;
    private UUID testFamilyInfoId;

    @BeforeEach
    void setUp() {
        testChildId = UUID.randomUUID();
        testFamilyInfoId = UUID.randomUUID();

        testChild = new ChildInfo();
        testChild.setChildID(testChildId);
        testChild.setFamilyInfoID(testFamilyInfoId);
        testChild.setName("測試兒童");
        testChild.setNationalID("A123456789");
        testChild.setBirthDate(LocalDate.of(2020, 1, 1));
        testChild.setGender(true); // true = male, false = female
        testChild.setHouseholdAddress("台北市測試路123號");
    }

    @Test
    void testCreate_Success() {
        // Given
        when(repository.save(any(ChildInfo.class))).thenReturn(testChild);

        // When
        ChildInfo result = service.create(testChild);

        // Then
        assertNotNull(result);
        assertEquals(testChildId, result.getChildID());
        assertEquals("測試兒童", result.getName());
        assertEquals("A123456789", result.getNationalID());
        verify(repository, times(1)).save(testChild);
    }

    @Test
    void testGetById_Success() {
        // Given
        when(repository.findById(testChildId)).thenReturn(Optional.of(testChild));

        // When
        Optional<ChildInfo> result = service.getById(testChildId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testChildId, result.get().getChildID());
        assertEquals("測試兒童", result.get().getName());
        verify(repository, times(1)).findById(testChildId);
    }

    @Test
    void testGetById_NotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When
        Optional<ChildInfo> result = service.getById(nonExistentId);

        // Then
        assertFalse(result.isPresent());
        verify(repository, times(1)).findById(nonExistentId);
    }

    @Test
    void testGetAll_Success() {
        // Given
        ChildInfo anotherChild = new ChildInfo();
        anotherChild.setChildID(UUID.randomUUID());
        anotherChild.setName("另一個兒童");
        List<ChildInfo> childList = Arrays.asList(testChild, anotherChild);
        when(repository.findAll()).thenReturn(childList);

        // When
        List<ChildInfo> result = service.getAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    void testUpdate_Success() {
        // Given
        testChild.setName("更新後的名字");
        when(repository.put(any(ChildInfo.class))).thenReturn(testChild);

        // When
        ChildInfo result = service.update(testChildId, testChild);

        // Then
        assertNotNull(result);
        assertEquals(testChildId, result.getChildID());
        assertEquals("更新後的名字", result.getName());
        verify(repository, times(1)).put(testChild);
    }

    @Test
    void testGetByFamilyInfoID_Success() {
        // Given
        ChildInfo anotherChild = new ChildInfo();
        anotherChild.setChildID(UUID.randomUUID());
        anotherChild.setFamilyInfoID(testFamilyInfoId);
        anotherChild.setName("家庭成員2");
        List<ChildInfo> childList = Arrays.asList(testChild, anotherChild);
        when(repository.findByFamilyInfoID(testFamilyInfoId)).thenReturn(childList);

        // When
        List<ChildInfo> result = service.getByFamilyInfoID(testFamilyInfoId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(child -> child.getFamilyInfoID().equals(testFamilyInfoId)));
        verify(repository, times(1)).findByFamilyInfoID(testFamilyInfoId);
    }

    @Test
    void testGetByFamilyInfoID_EmptyResult() {
        // Given
        UUID nonExistentFamilyId = UUID.randomUUID();
        when(repository.findByFamilyInfoID(nonExistentFamilyId)).thenReturn(Arrays.asList());

        // When
        List<ChildInfo> result = service.getByFamilyInfoID(nonExistentFamilyId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findByFamilyInfoID(nonExistentFamilyId);
    }

    @Test
    void testDeleteByChildId_Success() {
        // Given
        doNothing().when(repository).deleteById(testChildId);

        // When
        service.deleteByChildId(testChildId);

        // Then
        verify(repository, times(1)).deleteById(testChildId);
    }

    @Test
    void testDeleteByChildId_Exception() {
        // Given
        UUID childId = UUID.randomUUID();
        doThrow(new RuntimeException("Database error")).when(repository).deleteById(childId);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            service.deleteByChildId(childId);
        });
        verify(repository, times(1)).deleteById(childId);
    }

    @Test
    void testCreate_WithDifferentGender() {
        // Given - test with female (false)
        testChild.setGender(false);
        when(repository.save(any(ChildInfo.class))).thenReturn(testChild);

        // When
        ChildInfo result = service.create(testChild);

        // Then
        assertNotNull(result);
        assertFalse(result.getGender());
        verify(repository, times(1)).save(testChild);
    }
}


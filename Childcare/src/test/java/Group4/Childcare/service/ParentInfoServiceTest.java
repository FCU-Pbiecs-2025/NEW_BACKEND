package Group4.Childcare.service;

import Group4.Childcare.Model.ParentInfo;
import Group4.Childcare.Repository.ParentInfoJdbcRepository;
import Group4.Childcare.Service.ParentInfoService;
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
 * ParentInfoService 單元測試
 *
 * 測試範圍：
 * 1. create() - 創建家長資料
 * 2. getById() - 根據ID查詢家長資料
 * 3. getAll() - 查詢所有家長資料
 * 4. update() - 更新家長資料
 * 5. getByFamilyInfoID() - 根據家庭資料ID查詢家長資料
 */
@ExtendWith(MockitoExtension.class)
class ParentInfoServiceTest {

    @Mock
    private ParentInfoJdbcRepository repository;

    @InjectMocks
    private ParentInfoService service;

    private ParentInfo testParent;
    private UUID testParentId;
    private UUID testFamilyInfoId;

    @BeforeEach
    void setUp() {
        testParentId = UUID.randomUUID();
        testFamilyInfoId = UUID.randomUUID();

        testParent = new ParentInfo();
        testParent.setParentID(testParentId);
        testParent.setFamilyInfoID(testFamilyInfoId);
        testParent.setName("測試家長");
        testParent.setNationalID("B123456789");
        testParent.setBirthDate(LocalDate.of(1985, 5, 15));
        testParent.setRelationship("父親");
        testParent.setPhoneNumber("0912345678");
        testParent.setEmail("parent@example.com");
    }

    @Test
    void testCreate_Success() {
        // Given
        when(repository.save(any(ParentInfo.class))).thenReturn(testParent);

        // When
        ParentInfo result = service.create(testParent);

        // Then
        assertNotNull(result);
        assertEquals(testParentId, result.getParentID());
        assertEquals("測試家長", result.getName());
        assertEquals("B123456789", result.getNationalID());
        assertEquals("父親", result.getRelationship());
        verify(repository, times(1)).save(testParent);
    }

    @Test
    void testGetById_Success() {
        // Given
        when(repository.findById(testParentId)).thenReturn(Optional.of(testParent));

        // When
        Optional<ParentInfo> result = service.getById(testParentId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testParentId, result.get().getParentID());
        assertEquals("測試家長", result.get().getName());
        assertEquals("父親", result.get().getRelationship());
        verify(repository, times(1)).findById(testParentId);
    }

    @Test
    void testGetById_NotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When
        Optional<ParentInfo> result = service.getById(nonExistentId);

        // Then
        assertFalse(result.isPresent());
        verify(repository, times(1)).findById(nonExistentId);
    }

    @Test
    void testGetAll_Success() {
        // Given
        ParentInfo anotherParent = new ParentInfo();
        anotherParent.setParentID(UUID.randomUUID());
        anotherParent.setName("另一位家長");
        anotherParent.setRelationship("母親");
        List<ParentInfo> parentList = Arrays.asList(testParent, anotherParent);
        when(repository.findAll()).thenReturn(parentList);

        // When
        List<ParentInfo> result = service.getAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    void testUpdate_Success() {
        // Given
        testParent.setName("更新後的家長名字");
        testParent.setPhoneNumber("0987654321");
        when(repository.save(any(ParentInfo.class))).thenReturn(testParent);

        // When
        ParentInfo result = service.update(testParentId, testParent);

        // Then
        assertNotNull(result);
        assertEquals(testParentId, result.getParentID());
        assertEquals("更新後的家長名字", result.getName());
        assertEquals("0987654321", result.getPhoneNumber());
        verify(repository, times(1)).save(testParent);
    }

    @Test
    void testGetByFamilyInfoID_Success() {
        // Given
        ParentInfo mother = new ParentInfo();
        mother.setParentID(UUID.randomUUID());
        mother.setFamilyInfoID(testFamilyInfoId);
        mother.setName("母親");
        mother.setRelationship("母親");

        List<ParentInfo> parentList = Arrays.asList(testParent, mother);
        when(repository.findByFamilyInfoID(testFamilyInfoId)).thenReturn(parentList);

        // When
        List<ParentInfo> result = service.getByFamilyInfoID(testFamilyInfoId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(parent -> parent.getFamilyInfoID().equals(testFamilyInfoId)));
        verify(repository, times(1)).findByFamilyInfoID(testFamilyInfoId);
    }

    @Test
    void testGetByFamilyInfoID_EmptyResult() {
        // Given
        UUID nonExistentFamilyId = UUID.randomUUID();
        when(repository.findByFamilyInfoID(nonExistentFamilyId)).thenReturn(Arrays.asList());

        // When
        List<ParentInfo> result = service.getByFamilyInfoID(nonExistentFamilyId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findByFamilyInfoID(nonExistentFamilyId);
    }

    @Test
    void testCreate_WithDifferentRelationships() {
        // Given - 測試不同關係類型
        ParentInfo guardian = new ParentInfo();
        guardian.setParentID(UUID.randomUUID());
        guardian.setName("監護人");
        guardian.setRelationship("監護人");
        when(repository.save(any(ParentInfo.class))).thenReturn(guardian);

        // When
        ParentInfo result = service.create(guardian);

        // Then
        assertNotNull(result);
        assertEquals("監護人", result.getRelationship());
        verify(repository, times(1)).save(guardian);
    }

    @Test
    void testCreate_WithOccupation() {
        // Given - 測試包含職業資訊
        testParent.setOccupation("工程師");
        when(repository.save(any(ParentInfo.class))).thenReturn(testParent);

        // When
        ParentInfo result = service.create(testParent);

        // Then
        assertNotNull(result);
        assertEquals("工程師", result.getOccupation());
        verify(repository, times(1)).save(testParent);
    }

    @Test
    void testUpdate_EnsuresIdIsSet() {
        // Given
        UUID newId = UUID.randomUUID();
        ParentInfo parentWithoutId = new ParentInfo();
        parentWithoutId.setName("新家長");
        when(repository.save(any(ParentInfo.class))).thenReturn(parentWithoutId);

        // When
        ParentInfo result = service.update(newId, parentWithoutId);

        // Then
        assertNotNull(result);
        // 驗證 update 方法有設置 ID
        verify(repository, times(1)).save(argThat(parent ->
            parent.getParentID() != null && parent.getParentID().equals(newId)
        ));
    }
}


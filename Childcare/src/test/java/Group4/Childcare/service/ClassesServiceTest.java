package Group4.Childcare.service;

import Group4.Childcare.DTO.ClassNameDTO;
import Group4.Childcare.DTO.ClassSummaryDTO;
import Group4.Childcare.Model.Classes;
import Group4.Childcare.Repository.ClassesJdbcRepository;
import Group4.Childcare.Service.ClassesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ClassesService 單元測試
 *
 * 測試範圍：
 * 1. create() - 創建班級
 * 2. getById() - 根據ID查詢班級
 * 3. getAll() - 查詢所有班級
 * 4. update() - 更新班級
 * 5. delete() - 刪除班級
 * 6. getByInstitutionId() - 根據機構ID查詢班級
 * 7. getAllWithInstitutionName() - 查詢所有班級含機構名稱
 * 8. getClassesWithOffsetJdbc() - 分頁查詢班級
 * 9. getClassesWithOffsetAndInstitutionNameJdbc() - 分頁查詢班級含機構名稱
 * 10. getTotalCount() - 取得班級總數
 */
@ExtendWith(MockitoExtension.class)
class ClassesServiceTest {

    @Mock
    private ClassesJdbcRepository repository;

    @InjectMocks
    private ClassesService service;

    private Classes testClass;
    private UUID testClassId;
    private UUID testInstitutionId;

    @BeforeEach
    void setUp() {
        testClassId = UUID.randomUUID();
        testInstitutionId = UUID.randomUUID();

        testClass = new Classes();
        testClass.setClassID(testClassId);
        testClass.setInstitutionID(testInstitutionId);
        testClass.setClassName("幼幼班");
        testClass.setCapacity(20);
        testClass.setCurrentStudents(15);
        testClass.setMinAgeDescription(2);
        testClass.setMaxAgeDescription(3);
    }

    @Test
    void testCreate_Success() {
        // Given
        when(repository.save(any(Classes.class))).thenReturn(testClass);

        // When
        Classes result = service.create(testClass);

        // Then
        assertNotNull(result);
        assertEquals(testClassId, result.getClassID());
        assertEquals("幼幼班", result.getClassName());
        assertEquals(20, result.getCapacity());
        verify(repository, times(1)).save(testClass);
    }

    @Test
    void testGetById_Success() {
        // Given
        when(repository.findById(testClassId)).thenReturn(Optional.of(testClass));

        // When
        Optional<Classes> result = service.getById(testClassId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testClassId, result.get().getClassID());
        assertEquals("幼幼班", result.get().getClassName());
        verify(repository, times(1)).findById(testClassId);
    }

    @Test
    void testGetById_NotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When
        Optional<Classes> result = service.getById(nonExistentId);

        // Then
        assertFalse(result.isPresent());
        verify(repository, times(1)).findById(nonExistentId);
    }

    @Test
    void testGetAll_Success() {
        // Given
        Classes anotherClass = new Classes();
        anotherClass.setClassID(UUID.randomUUID());
        anotherClass.setClassName("小班");
        List<Classes> classList = Arrays.asList(testClass, anotherClass);
        when(repository.findAll()).thenReturn(classList);

        // When
        List<Classes> result = service.getAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    void testUpdate_Success() {
        // Given
        testClass.setClassName("幼幼班 - 更新");
        testClass.setCurrentStudents(18);
        when(repository.existsById(testClassId)).thenReturn(true);
        when(repository.save(any(Classes.class))).thenReturn(testClass);

        // When
        Classes result = service.update(testClassId, testClass);

        // Then
        assertNotNull(result);
        assertEquals(testClassId, result.getClassID());
        assertEquals("幼幼班 - 更新", result.getClassName());
        assertEquals(18, result.getCurrentStudents());
        verify(repository, times(1)).existsById(testClassId);
        verify(repository, times(1)).save(testClass);
    }

    @Test
    void testUpdate_NotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(repository.existsById(nonExistentId)).thenReturn(false);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            service.update(nonExistentId, testClass);
        });
        verify(repository, times(1)).existsById(nonExistentId);
        verify(repository, never()).save(any());
    }

    @Test
    void testDelete_Success() {
        // Given
        doNothing().when(repository).deleteById(testClassId);

        // When
        service.delete(testClassId);

        // Then
        verify(repository, times(1)).deleteById(testClassId);
    }

    @Test
    void testGetByInstitutionId_Success() {
        // Given
        Classes anotherClass = new Classes();
        anotherClass.setClassID(UUID.randomUUID());
        anotherClass.setInstitutionID(testInstitutionId);
        anotherClass.setClassName("中班");
        List<Classes> classList = Arrays.asList(testClass, anotherClass);
        when(repository.findByInstitutionId(testInstitutionId)).thenReturn(classList);

        // When
        List<Classes> result = service.getByInstitutionId(testInstitutionId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(c -> c.getInstitutionID().equals(testInstitutionId)));
        verify(repository, times(1)).findByInstitutionId(testInstitutionId);
    }

    @Test
    void testGetAllWithInstitutionName_Success() {
        // Given
        ClassSummaryDTO summaryDTO = new ClassSummaryDTO(
                testClassId,
                "幼幼班",
                20,
                "2歲",
                "3歲",
                "測試托育機構",
                testInstitutionId,
                15,
                null
        );
        List<ClassSummaryDTO> summaryList = Arrays.asList(summaryDTO);
        when(repository.findAllWithInstitutionName()).thenReturn(summaryList);

        // When
        List<ClassSummaryDTO> result = service.getAllWithInstitutionName();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("測試托育機構", result.get(0).getInstitutionName());
        assertEquals("幼幼班", result.get(0).getClassName());
        verify(repository, times(1)).findAllWithInstitutionName();
    }

    @Test
    void testGetClassesWithOffsetJdbc_Success() {
        // Given
        int offset = 0;
        List<Classes> classList = Arrays.asList(testClass);
        when(repository.findWithOffset(offset, 10)).thenReturn(classList);

        // When
        List<Classes> result = service.getClassesWithOffsetJdbc(offset);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository, times(1)).findWithOffset(offset, 10);
    }

    @Test
    void testGetClassesWithOffsetAndInstitutionNameJdbc_Success() {
        // Given
        int offset = 0;
        int size = 10;
        ClassSummaryDTO summaryDTO = new ClassSummaryDTO(
                testClassId,
                "幼幼班",
                20,
                "2歲",
                "3歲",
                "測試托育機構",
                testInstitutionId,
                15,
                null
        );
        List<ClassSummaryDTO> summaryList = Arrays.asList(summaryDTO);
        when(repository.findWithOffsetAndInstitutionName(offset, size)).thenReturn(summaryList);

        // When
        List<ClassSummaryDTO> result = service.getClassesWithOffsetAndInstitutionNameJdbc(offset, size);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("測試托育機構", result.get(0).getInstitutionName());
        verify(repository, times(1)).findWithOffsetAndInstitutionName(offset, size);
    }

    @Test
    void testGetClassesWithOffsetAndInstitutionNameByInstitutionID_Success() {
        // Given
        int offset = 0;
        int size = 10;
        ClassSummaryDTO summaryDTO = new ClassSummaryDTO(
                testClassId,
                "幼幼班",
                20,
                "2歲",
                "3歲",
                "測試托育機構",
                testInstitutionId,
                15,
                null
        );
        List<ClassSummaryDTO> summaryList = Arrays.asList(summaryDTO);
        when(repository.findWithOffsetAndInstitutionNameByInstitutionID(offset, size, testInstitutionId))
                .thenReturn(summaryList);

        // When
        List<ClassSummaryDTO> result = service.getClassesWithOffsetAndInstitutionNameByInstitutionID(
                offset, size, testInstitutionId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testInstitutionId, result.get(0).getInstitutionID());
        verify(repository, times(1))
                .findWithOffsetAndInstitutionNameByInstitutionID(offset, size, testInstitutionId);
    }

    @Test
    void testGetTotalCount_Success() {
        // Given
        long expectedCount = 35L;
        when(repository.countTotal()).thenReturn(expectedCount);

        // When
        long result = service.getTotalCount();

        // Then
        assertEquals(expectedCount, result);
        verify(repository, times(1)).countTotal();
    }

    @Test
    void testCreate_WithFullCapacity() {
        // Given
        testClass.setCurrentStudents(20); // 滿額
        when(repository.save(any(Classes.class))).thenReturn(testClass);

        // When
        Classes result = service.create(testClass);

        // Then
        assertNotNull(result);
        assertEquals(testClass.getCapacity(), result.getCurrentStudents());
        verify(repository, times(1)).save(testClass);
    }

    @Test
    void testGetByInstitutionId_EmptyResult() {
        // Given
        UUID nonExistentInstitutionId = UUID.randomUUID();
        when(repository.findByInstitutionId(nonExistentInstitutionId)).thenReturn(Arrays.asList());

        // When
        List<Classes> result = service.getByInstitutionId(nonExistentInstitutionId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findByInstitutionId(nonExistentInstitutionId);
    }
}


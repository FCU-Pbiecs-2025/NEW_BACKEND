package Group4.Childcare.controller;

import Group4.Childcare.Model.Classes;
import Group4.Childcare.DTO.ClassSummaryDTO;
import Group4.Childcare.Service.ClassesService;
import Group4.Childcare.Controller.ClassesController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ClassesController 單元測試
 *
 * 測試範圍：
 * 1. create() - 創建班級
 * 2. getById() - 根據ID查詢班級
 * 3. getAll() - 查詢所有班級
 * 4. getClassesByOffsetJdbc() - 分頁查詢班級
 * 5. update() - 更新班級
 * 6. delete() - 刪除班級
 */
@ExtendWith(MockitoExtension.class)
class ClassesControllerTest {

    @Mock
    private ClassesService service;

    @InjectMocks
    private ClassesController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private Classes testClass;
    private UUID testClassId;
    private UUID testInstitutionId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();

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
    void testCreate_Success() throws Exception {
        // Given
        when(service.create(any(Classes.class))).thenReturn(testClass);

        // When & Then
        mockMvc.perform(post("/classes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testClass)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classID", is(testClassId.toString())))
                .andExpect(jsonPath("$.className", is("幼幼班")))
                .andExpect(jsonPath("$.capacity", is(20)));

        verify(service, times(1)).create(any(Classes.class));
    }

    @Test
    void testGetById_Success() throws Exception {
        // Given
        when(service.getById(testClassId)).thenReturn(Optional.of(testClass));

        // When & Then
        mockMvc.perform(get("/classes/{id}", testClassId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classID", is(testClassId.toString())))
                .andExpect(jsonPath("$.className", is("幼幼班")));

        verify(service, times(1)).getById(testClassId);
    }

    @Test
    void testGetById_NotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(service.getById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/classes/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(service, times(1)).getById(nonExistentId);
    }

    @Test
    void testGetAll_Success() throws Exception {
        // Given
        Classes anotherClass = new Classes();
        anotherClass.setClassID(UUID.randomUUID());
        anotherClass.setClassName("小班");
        List<Classes> classes = Arrays.asList(testClass, anotherClass);
        when(service.getAll()).thenReturn(classes);

        // When & Then
        mockMvc.perform(get("/classes")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].className", is("幼幼班")))
                .andExpect(jsonPath("$[1].className", is("小班")));

        verify(service, times(1)).getAll();
    }

    @Test
    void testGetClassesByOffsetJdbc_SuperAdmin() throws Exception {
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
                15
        );
        List<ClassSummaryDTO> classes = Arrays.asList(summaryDTO);
        when(service.getClassesWithOffsetAndInstitutionNameJdbc(offset, size)).thenReturn(classes);
        when(service.getTotalCount()).thenReturn(1L);

        // When & Then
        mockMvc.perform(get("/classes/offset")
                .param("offset", String.valueOf(offset))
                .param("size", String.valueOf(size))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offset", is(offset)))
                .andExpect(jsonPath("$.size", is(size)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(service, times(1)).getClassesWithOffsetAndInstitutionNameJdbc(offset, size);
        verify(service, times(1)).getTotalCount();
    }

    @Test
    void testGetClassesByOffsetJdbc_Admin() throws Exception {
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
                15
        );
        List<ClassSummaryDTO> classes = Arrays.asList(summaryDTO);
        when(service.getClassesWithOffsetAndInstitutionNameByInstitutionID(offset, size, testInstitutionId))
                .thenReturn(classes);
        when(service.getTotalCountByInstitutionID(testInstitutionId)).thenReturn(1L);

        // When & Then
        mockMvc.perform(get("/classes/offset")
                .param("offset", String.valueOf(offset))
                .param("size", String.valueOf(size))
                .param("InstitutionID", testInstitutionId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offset", is(offset)))
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(service, times(1))
                .getClassesWithOffsetAndInstitutionNameByInstitutionID(offset, size, testInstitutionId);
        verify(service, times(1)).getTotalCountByInstitutionID(testInstitutionId);
    }

    @Test
    void testGetAllWithInstitutionName_Success() throws Exception {
        // Given
        ClassSummaryDTO summaryDTO = new ClassSummaryDTO(
                testClassId,
                "幼幼班",
                20,
                "2歲",
                "3歲",
                "測試托育機構",
                testInstitutionId,
                15
        );
        List<ClassSummaryDTO> classes = Arrays.asList(summaryDTO);
        when(service.getAllWithInstitutionName()).thenReturn(classes);

        // When & Then
        mockMvc.perform(get("/classes/with-institution")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].className", is("幼幼班")));

        verify(service, times(1)).getAllWithInstitutionName();
    }

    @Test
    void testUpdate_Success() throws Exception {
        // Given
        testClass.setClassName("幼幼班 - 更新");
        when(service.update(eq(testClassId), any(Classes.class))).thenReturn(testClass);

        // When & Then
        mockMvc.perform(put("/classes/{id}", testClassId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testClass)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.className", is("幼幼班 - 更新")));

        verify(service, times(1)).update(eq(testClassId), any(Classes.class));
    }

    @Test
    void testDelete_Success() throws Exception {
        // Given
        doNothing().when(service).delete(testClassId);

        // When & Then
        mockMvc.perform(delete("/classes/{id}", testClassId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(service, times(1)).delete(testClassId);
    }
}


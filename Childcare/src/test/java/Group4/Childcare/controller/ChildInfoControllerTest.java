package Group4.Childcare.controller;

import Group4.Childcare.Model.ChildInfo;
import Group4.Childcare.Service.ChildInfoService;
import Group4.Childcare.Controller.ChildInfoController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
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
 * ChildInfoController 單元測試
 *
 * 測試範圍：
 * 1. create() - 創建兒童資料
 * 2. getById() - 根據ID查詢兒童資料
 * 3. getAll() - 查詢所有兒童資料
 * 4. update() - 更新兒童資料
 * 5. getByFamilyInfoId() - 根據家庭ID查詢兒童資料
 * 6. deleteByChildId() - 刪除兒童資料
 */
@ExtendWith(MockitoExtension.class)
class ChildInfoControllerTest {

    @Mock
    private ChildInfoService service;

    @InjectMocks
    private ChildInfoController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private ChildInfo testChild;
    private UUID testChildId;
    private UUID testFamilyInfoId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        testChildId = UUID.randomUUID();
        testFamilyInfoId = UUID.randomUUID();

        testChild = new ChildInfo();
        testChild.setChildID(testChildId);
        testChild.setFamilyInfoID(testFamilyInfoId);
        testChild.setName("測試兒童");
        testChild.setNationalID("A123456789");
        testChild.setBirthDate(LocalDate.of(2020, 1, 1));
        testChild.setGender(true);
    }

    @Test
    void testCreate_Success() throws Exception {
        // Given
        when(service.create(any(ChildInfo.class))).thenReturn(testChild);

        // When & Then
        mockMvc.perform(post("/child-info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testChild)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.childID", is(testChildId.toString())))
                .andExpect(jsonPath("$.name", is("測試兒童")))
                .andExpect(jsonPath("$.nationalID", is("A123456789")));

        verify(service, times(1)).create(any(ChildInfo.class));
    }

    @Test
    void testGetById_Success() throws Exception {
        // Given
        when(service.getById(testChildId)).thenReturn(Optional.of(testChild));

        // When & Then
        mockMvc.perform(get("/child-info/{id}", testChildId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.childID", is(testChildId.toString())))
                .andExpect(jsonPath("$.name", is("測試兒童")));

        verify(service, times(1)).getById(testChildId);
    }

    @Test
    void testGetById_NotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(service.getById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/child-info/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(service, times(1)).getById(nonExistentId);
    }

    @Test
    void testGetAll_Success() throws Exception {
        // Given
        ChildInfo anotherChild = new ChildInfo();
        anotherChild.setChildID(UUID.randomUUID());
        anotherChild.setName("另一個兒童");
        List<ChildInfo> children = Arrays.asList(testChild, anotherChild);
        when(service.getAll()).thenReturn(children);

        // When & Then
        mockMvc.perform(get("/child-info")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("測試兒童")))
                .andExpect(jsonPath("$[1].name", is("另一個兒童")));

        verify(service, times(1)).getAll();
    }

    @Test
    void testUpdate_Success() throws Exception {
        // Given
        testChild.setName("更新後的名字");
        when(service.update(eq(testChildId), any(ChildInfo.class))).thenReturn(testChild);

        // When & Then
        mockMvc.perform(put("/child-info/{id}", testChildId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testChild)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("更新後的名字")));

        verify(service, times(1)).update(eq(testChildId), any(ChildInfo.class));
    }

    @Test
    void testGetByFamilyInfoId_Success() throws Exception {
        // Given
        ChildInfo anotherChild = new ChildInfo();
        anotherChild.setChildID(UUID.randomUUID());
        anotherChild.setFamilyInfoID(testFamilyInfoId);
        anotherChild.setName("家庭成員2");
        List<ChildInfo> children = Arrays.asList(testChild, anotherChild);
        when(service.getByFamilyInfoID(testFamilyInfoId)).thenReturn(children);

        // When & Then
        mockMvc.perform(get("/child-info/family/{familyInfoId}", testFamilyInfoId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("測試兒童")))
                .andExpect(jsonPath("$[1].name", is("家庭成員2")));

        verify(service, times(1)).getByFamilyInfoID(testFamilyInfoId);
    }

    @Test
    void testGetByFamilyInfoId_NotFound() throws Exception {
        // Given
        UUID nonExistentFamilyId = UUID.randomUUID();
        when(service.getByFamilyInfoID(nonExistentFamilyId)).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/child-info/family/{familyInfoId}", nonExistentFamilyId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(service, times(1)).getByFamilyInfoID(nonExistentFamilyId);
    }

    @Test
    void testDeleteByChildId_Success() throws Exception {
        // Given
        when(service.getById(testChildId)).thenReturn(Optional.of(testChild));
        doNothing().when(service).deleteByChildId(testChildId);

        // When & Then
        mockMvc.perform(delete("/child-info/{id}", testChildId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(service, times(1)).deleteByChildId(testChildId);
    }

    @Test
    void testDeleteByChildId_NotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(service.getById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(delete("/child-info/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(service, never()).deleteByChildId(any());
    }
}


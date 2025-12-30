package Group4.Childcare.controller;

import Group4.Childcare.Model.ParentInfo;
import Group4.Childcare.Service.ParentInfoService;
import Group4.Childcare.Repository.ParentInfoJdbcRepository;
import Group4.Childcare.Controller.ParentInfoController;
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
 * ParentInfoController 單元測試
 *
 * 測試範圍：
 * 1. create() - 創建家長資料
 * 2. getById() - 根據ID查詢家長資料
 * 3. getAll() - 查詢所有家長資料
 * 4. update() - 更新家長資料
 * 5. deleteByParentID() - 刪除家長資料
 */
@ExtendWith(MockitoExtension.class)
class ParentInfoControllerTest {

    @Mock
    private ParentInfoService service;

    @Mock
    private ParentInfoJdbcRepository repository;

    @InjectMocks
    private ParentInfoController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private ParentInfo testParent;
    private UUID testParentId;
    private UUID testFamilyInfoId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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
    }

    @Test
    void testCreate_Success() throws Exception {
        // Given
        testParent.setParentID(null);
        when(repository.save(any(ParentInfo.class))).thenReturn(testParent);

        // When & Then
        mockMvc.perform(post("/parent-info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testParent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("測試家長")))
                .andExpect(jsonPath("$.nationalID", is("B123456789")))
                .andExpect(jsonPath("$.relationship", is("父親")));

        verify(repository, times(1)).save(any(ParentInfo.class));
    }

    @Test
    void testCreate_MissingFamilyInfoID() throws Exception {
        // Given
        testParent.setFamilyInfoID(null);

        // When & Then
        mockMvc.perform(post("/parent-info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testParent)))
                .andExpect(status().isBadRequest());

        verify(repository, never()).save(any());
    }

    @Test
    void testGetById_Success() throws Exception {
        // Given
        when(service.getById(testParentId)).thenReturn(Optional.of(testParent));

        // When & Then
        mockMvc.perform(get("/parent-info/{id}", testParentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentID", is(testParentId.toString())))
                .andExpect(jsonPath("$.name", is("測試家長")))
                .andExpect(jsonPath("$.relationship", is("父親")));

        verify(service, times(1)).getById(testParentId);
    }

    @Test
    void testGetById_NotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(service.getById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/parent-info/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(service, times(1)).getById(nonExistentId);
    }

    @Test
    void testGetAll_Success() throws Exception {
        // Given
        ParentInfo mother = new ParentInfo();
        mother.setParentID(UUID.randomUUID());
        mother.setName("母親");
        mother.setRelationship("母親");
        List<ParentInfo> parents = Arrays.asList(testParent, mother);
        when(service.getAll()).thenReturn(parents);

        // When & Then
        mockMvc.perform(get("/parent-info")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].relationship", is("父親")))
                .andExpect(jsonPath("$[1].relationship", is("母親")));

        verify(service, times(1)).getAll();
    }

    @Test
    void testUpdate_Success() throws Exception {
        // Given
        testParent.setName("更新後的家長名字");
        testParent.setPhoneNumber("0987654321");
        when(service.update(eq(testParentId), any(ParentInfo.class))).thenReturn(testParent);

        // When & Then
        mockMvc.perform(put("/parent-info/{id}", testParentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testParent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("更新後的家長名字")))
                .andExpect(jsonPath("$.phoneNumber", is("0987654321")));

        verify(service, times(1)).update(eq(testParentId), any(ParentInfo.class));
    }

    @Test
    void testDeleteByParentID_Success() throws Exception {
        // Given
        when(repository.existsById(testParentId)).thenReturn(true);
        doNothing().when(repository).deleteById(testParentId);

        // When & Then
        mockMvc.perform(delete("/parent-info/{id}", testParentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(repository, times(1)).deleteById(testParentId);
    }

    @Test
    void testDeleteByParentID_NotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(repository.existsById(nonExistentId)).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/parent-info/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(repository, never()).deleteById(any());
    }
}


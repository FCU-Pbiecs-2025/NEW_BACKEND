package Group4.Childcare.controller;

import Group4.Childcare.Model.Institutions;
import Group4.Childcare.DTO.InstitutionSummaryDTO;
import Group4.Childcare.DTO.InstitutionSimpleDTO;
import Group4.Childcare.Service.InstitutionsService;
import Group4.Childcare.Controller.InstitutionsController;
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
 * InstitutionsController 單元測試
 *
 * 測試範圍：
 * 1. create() - 創建機構
 * 2. getById() - 根據ID查詢機構
 * 3. getAll() - 查詢所有機構
 * 4. update() - 更新機構
 * 5. getSummaryAll() - 查詢所有機構摘要
 * 6. getAllSimple() - 查詢所有機構簡要資訊
 */
@ExtendWith(MockitoExtension.class)
class InstitutionsControllerTest {

    @Mock
    private InstitutionsService service;

    @InjectMocks
    private InstitutionsController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private Institutions testInstitution;
    private UUID testInstitutionId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        testInstitutionId = UUID.randomUUID();
        testInstitution = new Institutions();
        testInstitution.setInstitutionID(testInstitutionId);
        testInstitution.setInstitutionName("測試托育機構");
        testInstitution.setAddress("台北市測試路100號");
        testInstitution.setPhoneNumber("02-12345678");
        testInstitution.setEmail("test@institution.com");
        testInstitution.setContactPerson("測試聯絡人");
    }

    // ===== getOffset 分頁測試 =====
    @Test
    void testGetOffset_WithDefaultParams() throws Exception {
        Group4.Childcare.DTO.InstitutionOffsetDTO dto = new Group4.Childcare.DTO.InstitutionOffsetDTO();
        dto.setOffset(0);
        dto.setSize(10);
        dto.setTotalElements(1);
        dto.setContent(Arrays.asList(testInstitution));

        when(service.getOffset(0, 10, null, null)).thenReturn(dto);

        mockMvc.perform(get("/institutions/offset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offset", is(0)))
                .andExpect(jsonPath("$.size", is(10)));
    }

    @Test
    void testGetOffset_WithCustomParams() throws Exception {
        Group4.Childcare.DTO.InstitutionOffsetDTO dto = new Group4.Childcare.DTO.InstitutionOffsetDTO();
        dto.setOffset(10);
        dto.setSize(5);
        dto.setTotalElements(20);

        when(service.getOffset(eq(10), eq(5), any(), eq("搜尋"))).thenReturn(dto);

        mockMvc.perform(get("/institutions/offset")
                .param("offset", "10")
                .param("size", "5")
                .param("search", "搜尋"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offset", is(10)));
    }

    @Test
    void testGetOffset_WithInstitutionId() throws Exception {
        Group4.Childcare.DTO.InstitutionOffsetDTO dto = new Group4.Childcare.DTO.InstitutionOffsetDTO();
        dto.setOffset(0);
        dto.setSize(10);

        when(service.getOffset(eq(0), eq(10), eq(testInstitutionId), any())).thenReturn(dto);

        mockMvc.perform(get("/institutions/offset")
                .param("InstitutionID", testInstitutionId.toString()))
                .andExpect(status().isOk());
    }

    // ===== getActiveAll 測試 =====
    @Test
    void testGetActiveAll_Success() throws Exception {
        List<Institutions> activeList = Arrays.asList(testInstitution);
        when(service.getAllActive()).thenReturn(activeList);

        mockMvc.perform(get("/institutions/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void testGetActiveAll_EmptyList() throws Exception {
        when(service.getAllActive()).thenReturn(Arrays.asList());

        mockMvc.perform(get("/institutions/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ===== getOffsetByName 測試 =====
    @Test
    void testGetOffsetByName_WithName() throws Exception {
        Group4.Childcare.DTO.InstitutionOffsetDTO dto = new Group4.Childcare.DTO.InstitutionOffsetDTO();
        dto.setOffset(0);
        dto.setSize(10);
        dto.setContent(Arrays.asList(testInstitution));

        when(service.getOffsetByName(eq(0), eq(10), any(), eq("測試"))).thenReturn(dto);

        mockMvc.perform(get("/institutions/offset/name-search")
                .param("name", "測試"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetOffsetByName_WithInstitutionId() throws Exception {
        Group4.Childcare.DTO.InstitutionOffsetDTO dto = new Group4.Childcare.DTO.InstitutionOffsetDTO();

        when(service.getOffsetByName(eq(0), eq(10), eq(testInstitutionId), any())).thenReturn(dto);

        mockMvc.perform(get("/institutions/offset/name-search")
                .param("InstitutionID", testInstitutionId.toString()))
                .andExpect(status().isOk());
    }

    // ===== create 邊界測試 =====
    @Test
    void testCreate_EmptyName() throws Exception {
        testInstitution.setInstitutionName("");
        when(service.create(any(Institutions.class))).thenReturn(testInstitution);

        mockMvc.perform(post("/institutions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testInstitution)))
                .andExpect(status().isOk());
    }

    // ===== update 變體測試 =====
    @Test
    void testUpdate_NullReturn() throws Exception {
        when(service.update(eq(testInstitutionId), any(Institutions.class))).thenReturn(null);

        mockMvc.perform(put("/institutions/{id}", testInstitutionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testInstitution)))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdate_WithAllFields() throws Exception {
        testInstitution.setFax("02-87654321");
        testInstitution.setDescription("完整說明");
        testInstitution.setRelatedLinks("https://test.com");
        testInstitution.setResponsiblePerson("負責人");
        testInstitution.setLatitude(java.math.BigDecimal.valueOf(25.0));
        testInstitution.setLongitude(java.math.BigDecimal.valueOf(121.0));
        testInstitution.setInstitutionsType(true);

        when(service.update(eq(testInstitutionId), any(Institutions.class))).thenReturn(testInstitution);

        mockMvc.perform(put("/institutions/{id}", testInstitutionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testInstitution)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fax", is("02-87654321")));
    }

    @Test
    void testCreate_Success() throws Exception {
        // Given
        when(service.create(any(Institutions.class))).thenReturn(testInstitution);

        // When & Then
        mockMvc.perform(post("/institutions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testInstitution)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.institutionID", is(testInstitutionId.toString())))
                .andExpect(jsonPath("$.institutionName", is("測試托育機構")))
                .andExpect(jsonPath("$.address", is("台北市測試路100號")));

        verify(service, times(1)).create(any(Institutions.class));
    }

    @Test
    void testGetById_Success() throws Exception {
        // Given
        when(service.getById(testInstitutionId)).thenReturn(Optional.of(testInstitution));

        // When & Then
        mockMvc.perform(get("/institutions/{id}", testInstitutionId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.institutionID", is(testInstitutionId.toString())))
                .andExpect(jsonPath("$.institutionName", is("測試托育機構")));

        verify(service, times(1)).getById(testInstitutionId);
    }

    @Test
    void testGetById_NotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(service.getById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/institutions/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(service, times(1)).getById(nonExistentId);
    }

    @Test
    void testGetAll_Success() throws Exception {
        // Given
        Institutions anotherInstitution = new Institutions();
        anotherInstitution.setInstitutionID(UUID.randomUUID());
        anotherInstitution.setInstitutionName("另一個機構");
        List<Institutions> institutions = Arrays.asList(testInstitution, anotherInstitution);
        when(service.getAll()).thenReturn(institutions);

        // When & Then
        mockMvc.perform(get("/institutions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].institutionName", is("測試托育機構")))
                .andExpect(jsonPath("$[1].institutionName", is("另一個機構")));

        verify(service, times(1)).getAll();
    }

    @Test
    void testUpdate_Success() throws Exception {
        // Given
        testInstitution.setInstitutionName("更新後的機構名稱");
        testInstitution.setEmail("newemail@institution.com");
        when(service.update(eq(testInstitutionId), any(Institutions.class))).thenReturn(testInstitution);

        // When & Then
        mockMvc.perform(put("/institutions/{id}", testInstitutionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testInstitution)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.institutionName", is("更新後的機構名稱")))
                .andExpect(jsonPath("$.email", is("newemail@institution.com")));

        verify(service, times(1)).update(eq(testInstitutionId), any(Institutions.class));
    }

    @Test
    void testGetSummaryAll_Success() throws Exception {
        // Given
        InstitutionSummaryDTO summaryDTO = new InstitutionSummaryDTO(
                testInstitutionId,
                "測試托育機構",
                "台北市測試路100號",
                "02-12345678");
        List<InstitutionSummaryDTO> summaries = Arrays.asList(summaryDTO);
        when(service.getSummaryAll()).thenReturn(summaries);

        // When & Then
        mockMvc.perform(get("/institutions/summary")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].institutionName", is("測試托育機構")));

        verify(service, times(1)).getSummaryAll();
    }

    @Test
    void testGetAllSimple_Success() throws Exception {
        // Given
        InstitutionSimpleDTO simpleDTO = new InstitutionSimpleDTO(
                testInstitutionId,
                "測試托育機構");
        List<InstitutionSimpleDTO> simples = Arrays.asList(simpleDTO);
        when(service.getAllSimple()).thenReturn(simples);

        // When & Then
        mockMvc.perform(get("/institutions/simple/all")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].institutionName", is("測試托育機構")));

        verify(service, times(1)).getAllSimple();
    }
}

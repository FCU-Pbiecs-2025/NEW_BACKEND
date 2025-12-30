package Group4.Childcare.controller;

import Group4.Childcare.Controller.FamilyInfoController;
import Group4.Childcare.Model.FamilyInfo;
import Group4.Childcare.Service.FamilyInfoService;
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

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FamilyInfoController 單元測試
 */
@ExtendWith(MockitoExtension.class)
class FamilyInfoControllerTest {

    @Mock
    private FamilyInfoService service;

    @InjectMocks
    private FamilyInfoController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private FamilyInfo testFamilyInfo;
    private UUID testId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();

        testId = UUID.randomUUID();
        testFamilyInfo = new FamilyInfo();
        testFamilyInfo.setFamilyInfoID(testId);
    }

    @Test
    void testCreate_Success() throws Exception {
        when(service.create(any(FamilyInfo.class))).thenReturn(testFamilyInfo);

        mockMvc.perform(post("/family-info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testFamilyInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.familyInfoID", is(testId.toString())));

        verify(service, times(1)).create(any(FamilyInfo.class));
    }

    @Test
    void testGetById_Success() throws Exception {
        when(service.getById(testId)).thenReturn(Optional.of(testFamilyInfo));

        mockMvc.perform(get("/family-info/{id}", testId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.familyInfoID", is(testId.toString())));

        verify(service, times(1)).getById(testId);
    }

    @Test
    void testGetById_NotFound() throws Exception {
        when(service.getById(testId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/family-info/{id}", testId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(service, times(1)).getById(testId);
    }

    @Test
    void testGetAll_Success() throws Exception {
        List<FamilyInfo> list = Arrays.asList(testFamilyInfo);
        when(service.getAll()).thenReturn(list);

        mockMvc.perform(get("/family-info")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(service, times(1)).getAll();
    }

    @Test
    void testGetAll_Empty() throws Exception {
        when(service.getAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/family-info")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testUpdate_Success() throws Exception {
        when(service.update(eq(testId), any(FamilyInfo.class))).thenReturn(testFamilyInfo);

        mockMvc.perform(put("/family-info/{id}", testId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testFamilyInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.familyInfoID", is(testId.toString())));

        verify(service, times(1)).update(eq(testId), any(FamilyInfo.class));
    }
}

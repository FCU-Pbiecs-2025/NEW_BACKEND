package Group4.Childcare.controller;

import Group4.Childcare.Model.Rules;
import Group4.Childcare.Service.RulesService;
import Group4.Childcare.Repository.RulesJdbcRepository;
import Group4.Childcare.Controller.RulesController;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RulesController 單元測試
 *
 * 測試範圍：
 * 1. create() - 創建規則
 * 2. getById() - 根據ID查詢規則
 * 3. getAll() - 查詢所有規則
 * 4. update() - 更新規則
 * 5. updateWithJdbc() - 使用JDBC方式更新規則
 */
@ExtendWith(MockitoExtension.class)
class RulesControllerTest {

    @Mock
    private RulesService service;

    @Mock
    private RulesJdbcRepository jdbcRepository;

    @InjectMocks
    private RulesController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private Rules testRule;
    private Long testRuleId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();

        testRuleId = 1L;
        testRule = new Rules();
        testRule.setId(testRuleId);
        testRule.setAdmissionEligibility("符合0-2歲兒童托育補助資格");
        testRule.setServiceContentAndTime("週一至週五 08:00-18:00 提供托育服務");
        testRule.setFeeAndRefundPolicy("月費10000元，退費按比例計算");
    }

    @Test
    void testCreate_Success() throws Exception {
        // Given
        when(service.create(any(Rules.class))).thenReturn(testRule);

        // When & Then
        mockMvc.perform(post("/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRule)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.admissionEligibility", is("符合0-2歲兒童托育補助資格")))
                .andExpect(jsonPath("$.serviceContentAndTime", is("週一至週五 08:00-18:00 提供托育服務")));

        verify(service, times(1)).create(any(Rules.class));
    }

    @Test
    void testGetById_Success() throws Exception {
        // Given
        when(service.getById(testRuleId)).thenReturn(Optional.of(testRule));

        // When & Then
        mockMvc.perform(get("/rules/{id}", testRuleId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.admissionEligibility", is("符合0-2歲兒童托育補助資格")));

        verify(service, times(1)).getById(testRuleId);
    }

    @Test
    void testGetById_NotFound() throws Exception {
        // Given
        Long nonExistentId = 999L;
        when(service.getById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/rules/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(service, times(1)).getById(nonExistentId);
    }

    @Test
    void testGetAll_Success() throws Exception {
        // Given
        Rules anotherRule = new Rules();
        anotherRule.setId(2L);
        anotherRule.setAdmissionEligibility("另一個入學資格");
        List<Rules> rules = Arrays.asList(testRule, anotherRule);
        when(service.getAll()).thenReturn(rules);

        // When & Then
        mockMvc.perform(get("/rules")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].admissionEligibility", is("符合0-2歲兒童托育補助資格")))
                .andExpect(jsonPath("$[1].admissionEligibility", is("另一個入學資格")));

        verify(service, times(1)).getAll();
    }

    @Test
    void testGetAll_EmptyList() throws Exception {
        // Given
        when(service.getAll()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/rules")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(service, times(1)).getAll();
    }

    @Test
    void testUpdate_Success() throws Exception {
        // Given
        testRule.setAdmissionEligibility("更新後的入學資格");
        testRule.setFeeAndRefundPolicy("更新後的收費政策");
        when(service.update(eq(testRuleId), any(Rules.class))).thenReturn(testRule);

        // When & Then
        mockMvc.perform(put("/rules/{id}", testRuleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRule)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admissionEligibility", is("更新後的入學資格")))
                .andExpect(jsonPath("$.feeAndRefundPolicy", is("更新後的收費政策")));

        verify(service, times(1)).update(eq(testRuleId), any(Rules.class));
    }

    @Test
    void testUpdateWithJdbc_Success() throws Exception {
        // Given
        testRule.setAdmissionEligibility("JDBC更新後的入學資格");
        when(jdbcRepository.updateById(eq(testRuleId), any(Rules.class))).thenReturn(testRule);

        // When & Then
        mockMvc.perform(put("/rules/jdbc/{id}", testRuleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRule)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admissionEligibility", is("JDBC更新後的入學資格")));

        verify(jdbcRepository, times(1)).updateById(eq(testRuleId), any(Rules.class));
    }

    @Test
    void testUpdateWithJdbc_NotFound() throws Exception {
        // Given
        Long nonExistentId = 999L;
        when(jdbcRepository.updateById(eq(nonExistentId), any(Rules.class)))
                .thenThrow(new RuntimeException("Rule not found"));

        // When & Then
        mockMvc.perform(put("/rules/jdbc/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRule)))
                .andExpect(status().isNotFound());

        verify(jdbcRepository, times(1)).updateById(eq(nonExistentId), any(Rules.class));
    }
}

package Group4.Childcare.service;

import Group4.Childcare.Model.Rules;
import Group4.Childcare.Repository.RulesJdbcRepository;
import Group4.Childcare.Service.RulesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RulesService 單元測試
 *
 * 測試範圍：
 * 1. create() - 創建規則
 * 2. getById() - 根據ID查詢規則
 * 3. getAll() - 查詢所有規則
 * 4. update() - 更新規則
 */
@ExtendWith(MockitoExtension.class)
class RulesServiceTest {

    @Mock
    private RulesJdbcRepository repository;

    @InjectMocks
    private RulesService service;

    private Rules testRule;
    private Long testRuleId;

    @BeforeEach
    void setUp() {
        testRuleId = 1L;
        testRule = new Rules();
        testRule.setId(testRuleId);
        testRule.setAdmissionEligibility("符合0-2歲兒童托育補助資格");
        testRule.setServiceContentAndTime("週一至週五 08:00-18:00 提供托育服務");
        testRule.setFeeAndRefundPolicy("月費10000元，退費按比例計算");
    }

    @Test
    void testCreate_Success() {
        // Given
        when(repository.save(any(Rules.class))).thenReturn(testRule);

        // When
        Rules result = service.create(testRule);

        // Then
        assertNotNull(result);
        assertEquals(testRuleId, result.getId());
        assertEquals("符合0-2歲兒童托育補助資格", result.getAdmissionEligibility());
        assertEquals("週一至週五 08:00-18:00 提供托育服務", result.getServiceContentAndTime());
        verify(repository, times(1)).save(testRule);
    }

    @Test
    void testGetById_Success() {
        // Given
        when(repository.findById(testRuleId)).thenReturn(Optional.of(testRule));

        // When
        Optional<Rules> result = service.getById(testRuleId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testRuleId, result.get().getId());
        assertEquals("符合0-2歲兒童托育補助資格", result.get().getAdmissionEligibility());
        verify(repository, times(1)).findById(testRuleId);
    }

    @Test
    void testGetById_NotFound() {
        // Given
        Long nonExistentId = 999L;
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When
        Optional<Rules> result = service.getById(nonExistentId);

        // Then
        assertFalse(result.isPresent());
        verify(repository, times(1)).findById(nonExistentId);
    }

    @Test
    void testGetAll_Success() {
        // Given
        Rules anotherRule = new Rules();
        anotherRule.setId(2L);
        anotherRule.setAdmissionEligibility("符合入學資格的兒童");
        anotherRule.setServiceContentAndTime("提供全日托育");
        List<Rules> ruleList = Arrays.asList(testRule, anotherRule);
        when(repository.findAll()).thenReturn(ruleList);

        // When
        List<Rules> result = service.getAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    void testGetAll_EmptyList() {
        // Given
        when(repository.findAll()).thenReturn(Arrays.asList());

        // When
        List<Rules> result = service.getAll();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findAll();
    }

    @Test
    void testUpdate_Success() {
        // Given
        testRule.setAdmissionEligibility("更新後的入學資格");
        testRule.setServiceContentAndTime("更新後的服務時間");
        when(repository.save(any(Rules.class))).thenReturn(testRule);

        // When
        Rules result = service.update(testRuleId, testRule);

        // Then
        assertNotNull(result);
        assertEquals(testRuleId, result.getId());
        assertEquals("更新後的入學資格", result.getAdmissionEligibility());
        assertEquals("更新後的服務時間", result.getServiceContentAndTime());
        verify(repository, times(1)).save(testRule);
    }

    @Test
    void testUpdate_EnsuresIdIsSet() {
        // Given
        Long newId = 5L;
        Rules ruleWithoutId = new Rules();
        ruleWithoutId.setAdmissionEligibility("新規則的入學資格");
        when(repository.save(any(Rules.class))).thenReturn(ruleWithoutId);

        // When
        Rules result = service.update(newId, ruleWithoutId);

        // Then
        assertNotNull(result);
        // 驗證 update 方法有設置 ID
        verify(repository, times(1)).save(argThat(rule ->
            rule.getId() != null && rule.getId().equals(newId)
        ));
    }

    @Test
    void testCreate_WithAllFields() {
        // Given
        Rules fullRule = new Rules();
        fullRule.setId(2L);
        fullRule.setAdmissionEligibility("符合入學資格的兒童");
        fullRule.setServiceContentAndTime("提供全日托育服務");
        fullRule.setFeeAndRefundPolicy("月費15000元，退費政策依規定辦理");
        when(repository.save(any(Rules.class))).thenReturn(fullRule);

        // When
        Rules result = service.create(fullRule);

        // Then
        assertNotNull(result);
        assertEquals("符合入學資格的兒童", result.getAdmissionEligibility());
        assertEquals("提供全日托育服務", result.getServiceContentAndTime());
        assertEquals("月費15000元，退費政策依規定辦理", result.getFeeAndRefundPolicy());
        verify(repository, times(1)).save(fullRule);
    }

    @Test
    void testCreate_WithPartialFields() {
        // Given
        Rules partialRule = new Rules();
        partialRule.setId(3L);
        partialRule.setAdmissionEligibility("基本入學資格");
        // 只設置部分欄位
        when(repository.save(any(Rules.class))).thenReturn(partialRule);

        // When
        Rules result = service.create(partialRule);

        // Then
        assertNotNull(result);
        assertEquals("基本入學資格", result.getAdmissionEligibility());
        verify(repository, times(1)).save(partialRule);
    }
}


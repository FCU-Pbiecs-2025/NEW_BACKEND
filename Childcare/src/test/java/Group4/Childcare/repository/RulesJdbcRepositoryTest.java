package Group4.Childcare.repository;

import Group4.Childcare.Model.Rules;
import Group4.Childcare.Repository.RulesJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RulesJdbcRepository 單元測試
 * 測試規則管理相關的資料庫操作
 */
@ExtendWith(MockitoExtension.class)
class RulesJdbcRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private RulesJdbcRepository repository;

    private Rules testRule;
    private Rules testRuleWithId;

    @BeforeEach
    void setUp() {
        // 沒有 ID 的規則（用於新增測試）
        testRule = new Rules();
        testRule.setAdmissionEligibility("符合年齡及居住地條件");
        testRule.setServiceContentAndTime("週一至週五 08:00-18:00");
        testRule.setFeeAndRefundPolicy("月費 10000 元，退費依比例計算");

        // 有 ID 的規則（用於更新測試）
        testRuleWithId = new Rules();
        testRuleWithId.setId(1L);
        testRuleWithId.setAdmissionEligibility("符合年齡及居住地條件");
        testRuleWithId.setServiceContentAndTime("週一至週五 08:00-18:00");
        testRuleWithId.setFeeAndRefundPolicy("月費 10000 元，退費依比例計算");
    }

    // ===== 測試 save (新增規則) =====
    @Test
    void testSave_InsertWhenIdIsNull() {
        // Given: ID 為 null 的規則
        when(jdbcTemplate.update(anyString(), any(), any(), any()))
                .thenReturn(1);

        // When
        Rules result = repository.save(testRule);

        // Then: 應該呼叫 insert
        assertNotNull(result);
        verify(jdbcTemplate, times(1)).update(
                contains("INSERT"),
                eq(testRule.getAdmissionEligibility()),
                eq(testRule.getServiceContentAndTime()),
                eq(testRule.getFeeAndRefundPolicy()));
    }

    @Test
    void testSave_InsertWhenIdIsZero() {
        // Given: ID 為 0 的規則
        testRule.setId(0L);
        when(jdbcTemplate.update(anyString(), any(), any(), any()))
                .thenReturn(1);

        // When
        Rules result = repository.save(testRule);

        // Then: 應該呼叫 insert
        assertNotNull(result);
        verify(jdbcTemplate, times(1)).update(
                contains("INSERT"),
                any(), any(), any());
    }

    @Test
    void testSave_UpdateWhenIdExists() {
        // Given: 有 ID 的規則
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any()))
                .thenReturn(1);

        // When
        Rules result = repository.save(testRuleWithId);

        // Then: 應該呼叫 update
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(jdbcTemplate, times(1)).update(
                contains("UPDATE"),
                eq(testRuleWithId.getAdmissionEligibility()),
                eq(testRuleWithId.getServiceContentAndTime()),
                eq(testRuleWithId.getFeeAndRefundPolicy()),
                eq(testRuleWithId.getId()));
    }

    // ===== 測試 updateById =====
    @Test
    void testUpdateById_Success() {
        // Given
        Long ruleId = 1L;
        Rules updatedRule = new Rules();
        updatedRule.setAdmissionEligibility("更新的入學資格");
        updatedRule.setServiceContentAndTime("更新的服務內容");
        updatedRule.setFeeAndRefundPolicy("更新的收費政策");

        when(jdbcTemplate.update(anyString(), any(), any(), any(), any()))
                .thenReturn(1);

        // When
        Rules result = repository.updateById(ruleId, updatedRule);

        // Then
        assertNotNull(result);
        assertEquals(ruleId, result.getId());
        verify(jdbcTemplate, times(1)).update(
                contains("UPDATE"),
                eq(updatedRule.getAdmissionEligibility()),
                eq(updatedRule.getServiceContentAndTime()),
                eq(updatedRule.getFeeAndRefundPolicy()),
                eq(ruleId));
    }

    @Test
    void testUpdateById_ThrowsExceptionWhenNotFound() {
        // Given: 更新不存在的規則
        Long nonExistentId = 999L;
        Rules updatedRule = new Rules();
        updatedRule.setAdmissionEligibility("測試");
        updatedRule.setServiceContentAndTime("測試");
        updatedRule.setFeeAndRefundPolicy("測試");

        when(jdbcTemplate.update(anyString(), any(), any(), any(), any()))
                .thenReturn(0); // 沒有行被更新

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            repository.updateById(nonExistentId, updatedRule);
        });

        assertTrue(exception.getMessage().contains("not found"));
        assertTrue(exception.getMessage().contains(nonExistentId.toString()));
    }

    // ===== 測試 findById =====
    @Test
    void testFindById_Success() {
        // Given
        Long ruleId = 1L;
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(ruleId)))
                .thenReturn(testRuleWithId);

        // When
        Optional<Rules> result = repository.findById(ruleId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(ruleId, result.get().getId());
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), any(RowMapper.class), eq(ruleId));
    }

    @Test
    void testFindById_ReturnsEmptyWhenNotFound() {
        // Given
        Long nonExistentId = 999L;
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(nonExistentId)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // When
        Optional<Rules> result = repository.findById(nonExistentId);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testFindById_ReturnsEmptyOnException() {
        // Given
        Long ruleId = 1L;
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(ruleId)))
                .thenThrow(new RuntimeException("Database error"));

        // When
        Optional<Rules> result = repository.findById(ruleId);

        // Then
        assertFalse(result.isPresent());
    }

    // ===== 測試 findAll =====
    @Test
    void testFindAll_ReturnsAllRules() {
        // Given
        List<Rules> mockRules = Arrays.asList(testRuleWithId, testRuleWithId);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(mockRules);

        // When
        List<Rules> result = repository.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class));
    }

    @Test
    void testFindAll_ReturnsEmptyListWhenNoRules() {
        // Given
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(Collections.emptyList());

        // When
        List<Rules> result = repository.findAll();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ===== 測試 deleteById =====
    @Test
    void testDeleteById_Success() {
        // Given
        Long ruleId = 1L;
        when(jdbcTemplate.update(anyString(), eq(ruleId)))
                .thenReturn(1);

        // When
        repository.deleteById(ruleId);

        // Then
        verify(jdbcTemplate, times(1)).update(contains("DELETE"), eq(ruleId));
    }

    @Test
    void testDeleteById_WhenNotFound() {
        // Given: 刪除不存在的規則
        Long nonExistentId = 999L;
        when(jdbcTemplate.update(anyString(), eq(nonExistentId)))
                .thenReturn(0);

        // When
        repository.deleteById(nonExistentId);

        // Then: 不應該拋出異常，但應該呼叫 update
        verify(jdbcTemplate, times(1)).update(anyString(), eq(nonExistentId));
    }

    // ===== 測試 delete =====
    @Test
    void testDelete_Success() {
        // Given
        when(jdbcTemplate.update(anyString(), eq(testRuleWithId.getId())))
                .thenReturn(1);

        // When
        repository.delete(testRuleWithId);

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), eq(testRuleWithId.getId()));
    }

    // ===== 測試 existsById =====
    @Test
    void testExistsById_ReturnsTrue() {
        // Given
        Long ruleId = 1L;
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(ruleId)))
                .thenReturn(1);

        // When
        boolean result = repository.existsById(ruleId);

        // Then
        assertTrue(result);
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(Integer.class), eq(ruleId));
    }

    @Test
    void testExistsById_ReturnsFalseWhenCountIsZero() {
        // Given
        Long nonExistentId = 999L;
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(nonExistentId)))
                .thenReturn(0);

        // When
        boolean result = repository.existsById(nonExistentId);

        // Then
        assertFalse(result);
    }

    @Test
    void testExistsById_ReturnsFalseWhenCountIsNull() {
        // Given
        Long ruleId = 1L;
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(ruleId)))
                .thenReturn(null);

        // When
        boolean result = repository.existsById(ruleId);

        // Then
        assertFalse(result);
    }

    // ===== 測試 count =====
    @Test
    void testCount_ReturnsCorrectCount() {
        // Given
        Long expectedCount = 50L;
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(expectedCount);

        // When
        long result = repository.count();

        // Then
        assertEquals(expectedCount, result);
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(Long.class));
    }

    @Test
    void testCount_ReturnsZeroWhenNull() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(null);

        // When
        long result = repository.count();

        // Then
        assertEquals(0, result);
    }

    @Test
    void testCount_ReturnsZeroWhenNoRules() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(0L);

        // When
        long result = repository.count();

        // Then
        assertEquals(0, result);
    }
}

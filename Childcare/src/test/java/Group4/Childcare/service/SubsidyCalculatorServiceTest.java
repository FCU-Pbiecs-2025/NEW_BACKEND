package Group4.Childcare.service;

import Group4.Childcare.service.SubsidyCalculatorService.CalculationResult;
import Group4.Childcare.service.SubsidyCalculatorService.SubsidyForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 補助金額試算服務單元測試
 * Unit Tests for Subsidy Calculator Service
 */
@DisplayName("補助金額試算服務測試")
class SubsidyCalculatorServiceTest {

    private SubsidyCalculatorService service;

    @BeforeEach
    void setUp() {
        service = new SubsidyCalculatorService();
    }

    // ============================================================
    // 測試組 1: 表單驗證測試
    // ============================================================

    @Test
    @DisplayName("測試1.1: 空表單應返回驗證錯誤")
    void testEmptyFormValidation() {
        SubsidyForm form = new SubsidyForm();
        CalculationResult result = service.calculateSubsidy(form);

        assertFalse(result.isEligible());
        assertTrue(result.getMessage().contains("請填寫"));
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("測試1.2: 部分欄位填寫應返回缺失欄位錯誤")
    void testPartialFormValidation() {
        SubsidyForm form = new SubsidyForm();
        form.setIsCitizen("yes");
        form.setFetusCount("1");

        CalculationResult result = service.calculateSubsidy(form);

        assertFalse(result.isEligible());
        assertTrue(result.getMessage().contains("請填寫"));
        assertTrue(result.getErrors().size() >= 4);
    }

    // ============================================================
    // 測試組 2: 資格檢查測試
    // ============================================================

    @Test
    @DisplayName("測試2.1: 非新竹縣縣民不符合申請資格")
    void testNonCitizenNotEligible() {
        SubsidyForm form = createCompleteForm("no", "1", "0-2", "no", "normal", "A");
        CalculationResult result = service.calculateSubsidy(form);

        assertFalse(result.isEligible());
        assertEquals("不符合申請資格", result.getMessage());
        assertNull(result.getSubsidyAmount());
    }

    @Test
    @DisplayName("測試2.2: 育嬰留停中不符合申請資格")
    void testParentalLeaveNotEligible() {
        SubsidyForm form = createCompleteForm("yes", "1", "0-2", "yes", "normal", "A");
        CalculationResult result = service.calculateSubsidy(form);

        assertFalse(result.isEligible());
        assertEquals("不符合申請資格", result.getMessage());
    }

    @Test
    @DisplayName("測試2.3: 3-6歲幼兒不符合申請資格")
    void testAge3to6NotEligible() {
        SubsidyForm form = createCompleteForm("yes", "1", "3-6", "no", "normal", "A");
        CalculationResult result = service.calculateSubsidy(form);

        assertFalse(result.isEligible());
        assertEquals("不符合申請資格", result.getMessage());
    }

    // ============================================================
    // 測試組 3: 公共托育機構補助金額計算
    // ============================================================

    @ParameterizedTest(name = "公托 + {0} + {1}胎 = {2}元")
    @CsvSource({
        "normal, 1, 7000",
        "normal, 2, 8000",
        "normal, 3, 9000",
        "midlow, 1, 9000",
        "midlow, 2, 10000",
        "midlow, 3, 11000",
        "low, 1, 11000",
        "low, 2, 12000",
        "low, 3, 13000"
    })
    @DisplayName("測試3: 公共托育機構補助金額計算")
    void testPublicInstitutionSubsidy(String identity, String fetusCount, int expectedAmount) {
        SubsidyForm form = createCompleteForm("yes", fetusCount, "0-2", "no", identity, "A");
        CalculationResult result = service.calculateSubsidy(form);

        assertTrue(result.isEligible());
        assertEquals(expectedAmount, result.getSubsidyAmount());
        assertEquals("試算成功", result.getMessage());
    }

    // ============================================================
    // 測試組 4: 準公共托育機構補助金額計算
    // ============================================================

    @ParameterizedTest(name = "準公托 + {0} + {1}胎 = {2}元")
    @CsvSource({
        "normal, 1, 13000",
        "normal, 2, 14000",
        "normal, 3, 15000",
        "midlow, 1, 15000",
        "midlow, 2, 16000",
        "midlow, 3, 17000",
        "low, 1, 17000",
        "low, 2, 18000",
        "low, 3, 19000"
    })
    @DisplayName("測試4: 準公共托育機構補助金額計算")
    void testQuasiPublicInstitutionSubsidy(String identity, String fetusCount, int expectedAmount) {
        SubsidyForm form = createCompleteForm("yes", fetusCount, "0-2", "no", identity, "B");
        CalculationResult result = service.calculateSubsidy(form);

        assertTrue(result.isEligible());
        assertEquals(expectedAmount, result.getSubsidyAmount());
    }

    // ============================================================
    // 測試組 5: 邊界條件測試
    // ============================================================

    @Test
    @DisplayName("測試5.1: 連續提交測試")
    void testMultipleSubmissions() {
        // 第一次提交空表單
        SubsidyForm emptyForm = new SubsidyForm();
        CalculationResult result1 = service.calculateSubsidy(emptyForm);
        assertFalse(result1.isEligible());

        // 第二次提交完整表單
        SubsidyForm completeForm = createCompleteForm("yes", "1", "0-2", "no", "normal", "A");
        CalculationResult result2 = service.calculateSubsidy(completeForm);
        assertTrue(result2.isEligible());
        assertEquals(7000, result2.getSubsidyAmount());
    }

    @Test
    @DisplayName("測試5.2: 從符合資格變更為不符合資格")
    void testEligibilityChange() {
        // 先創建符合資格的表單
        SubsidyForm form = createCompleteForm("yes", "1", "0-2", "no", "normal", "A");
        CalculationResult result1 = service.calculateSubsidy(form);
        assertTrue(result1.isEligible());

        // 修改為不符合資格
        form.setIsCitizen("no");
        CalculationResult result2 = service.calculateSubsidy(form);
        assertFalse(result2.isEligible());
        assertNull(result2.getSubsidyAmount());
    }

    // ============================================================
    // 測試組 6: 使用者操作流程測試
    // ============================================================

    @Test
    @DisplayName("測試6.1: 完整流程 - 新手媽媽申請公托補助")
    void testNewMotherCompleteFlow() {
        SubsidyForm form = new SubsidyForm();

        // 步驟式填寫
        form.setIsCitizen("yes");
        form.setFetusCount("1");
        form.setChildAge("0-2");
        form.setIsParentalLeave("no");
        form.setIdentity("normal");
        form.setOrg("A");

        CalculationResult result = service.calculateSubsidy(form);

        assertTrue(result.isEligible());
        assertEquals(7000, result.getSubsidyAmount());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("測試6.2: 完整流程 - 低收雙胞胎家庭申請準公托補助")
    void testLowIncomeTwinsCompleteFlow() {
        SubsidyForm form = createCompleteForm("yes", "2", "0-2", "no", "low", "B");
        CalculationResult result = service.calculateSubsidy(form);

        assertTrue(result.isEligible());
        assertEquals(18000, result.getSubsidyAmount());

        // 驗證雙胞胎比單胎多1000元
        SubsidyForm singleForm = createCompleteForm("yes", "1", "0-2", "no", "low", "B");
        CalculationResult singleResult = service.calculateSubsidy(singleForm);
        assertEquals(1000, result.getSubsidyAmount() - singleResult.getSubsidyAmount());
    }

    @Test
    @DisplayName("測試6.3: 錯誤操作流程 - 用戶忘記填寫某些欄位")
    void testIncompleteFormErrorFlow() {
        SubsidyForm form = new SubsidyForm();
        form.setIsCitizen("yes");
        form.setFetusCount("1");

        CalculationResult result = service.calculateSubsidy(form);

        assertFalse(result.isEligible());
        assertTrue(result.getMessage().contains("請填寫"));
        assertFalse(result.getErrors().isEmpty());
    }

    // ============================================================
    // 輔助方法
    // ============================================================

    /**
     * 創建完整的表單
     */
    private SubsidyForm createCompleteForm(String isCitizen, String fetusCount,
                                          String childAge, String isParentalLeave,
                                          String identity, String org) {
        SubsidyForm form = new SubsidyForm();
        form.setIsCitizen(isCitizen);
        form.setFetusCount(fetusCount);
        form.setChildAge(childAge);
        form.setIsParentalLeave(isParentalLeave);
        form.setIdentity(identity);
        form.setOrg(org);
        return form;
    }
}


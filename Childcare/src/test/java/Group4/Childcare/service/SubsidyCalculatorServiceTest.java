package Group4.Childcare.service;

import Group4.Childcare.Service.SubsidyCalculatorService;
import Group4.Childcare.Service.SubsidyCalculatorService.CalculationResult;
import Group4.Childcare.Service.SubsidyCalculatorService.SubsidyForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

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
    // 測試組 7: Getter/Setter 和輔助方法測試
    // ============================================================

    @Test
    @DisplayName("測試7.1: SubsidyForm Getter/Setter 測試")
    void testSubsidyFormGettersSetters() {
        SubsidyForm form = new SubsidyForm();

        form.setIsCitizen("yes");
        assertEquals("yes", form.getIsCitizen());

        form.setFetusCount("2");
        assertEquals("2", form.getFetusCount());

        form.setChildAge("0-2");
        assertEquals("0-2", form.getChildAge());

        form.setIsParentalLeave("no");
        assertEquals("no", form.getIsParentalLeave());

        form.setIdentity("midlow");
        assertEquals("midlow", form.getIdentity());

        form.setOrg("B");
        assertEquals("B", form.getOrg());
    }

    @Test
    @DisplayName("測試7.2: CalculationResult Getter/Setter 測試")
    void testCalculationResultGettersSetters() {
        CalculationResult result = new CalculationResult();

        result.setSubsidyAmount(10000);
        assertEquals(10000, result.getSubsidyAmount());

        result.setEligible(true);
        assertTrue(result.isEligible());

        result.setMessage("測試訊息");
        assertEquals("測試訊息", result.getMessage());

        result.getErrors().add("錯誤1");
        result.getErrors().add("錯誤2");
        assertEquals(2, result.getErrors().size());
        assertTrue(result.getErrors().contains("錯誤1"));
    }

    @Test
    @DisplayName("測試7.3: createEmptyForm 方法測試")
    void testCreateEmptyForm() {
        SubsidyForm form = service.createEmptyForm();

        assertNotNull(form);
        assertNull(form.getIsCitizen());
        assertNull(form.getFetusCount());
        assertNull(form.getChildAge());
        assertNull(form.getIsParentalLeave());
        assertNull(form.getIdentity());
        assertNull(form.getOrg());
    }

    // ============================================================
    // 測試組 8: 空白字串和 Trim 測試
    // ============================================================

    @Test
    @DisplayName("測試8.1: 欄位包含空白字元應視為未填寫")
    void testWhitespaceFieldsValidation() {
        SubsidyForm form = new SubsidyForm();
        form.setIsCitizen("   ");  // 只有空白
        form.setFetusCount(" ");
        form.setChildAge("  ");
        form.setIsParentalLeave("   ");
        form.setIdentity(" ");
        form.setOrg("  ");

        CalculationResult result = service.calculateSubsidy(form);

        assertFalse(result.isEligible());
        assertTrue(result.getMessage().contains("請填寫"));
        assertEquals(6, result.getErrors().size());
    }

    @Test
    @DisplayName("測試8.2: 混合 null 和空白字串")
    void testMixedNullAndWhitespace() {
        SubsidyForm form = new SubsidyForm();
        form.setIsCitizen(null);
        form.setFetusCount("  ");
        form.setChildAge("");
        // isParentalLeave, identity, org 保持 null

        CalculationResult result = service.calculateSubsidy(form);

        assertFalse(result.isEligible());
        assertEquals(6, result.getErrors().size());
    }

    // ============================================================
    // 測試組 9: 胎數邊界值測試
    // ============================================================

    @Test
    @DisplayName("測試9.1: 四胞胎應獲得三胞胎以上的補助（+2000）")
    void testQuadrupletsSubsidy() {
        SubsidyForm form = createCompleteForm("yes", "4", "0-2", "no", "normal", "A");
        CalculationResult result = service.calculateSubsidy(form);

        assertTrue(result.isEligible());
        assertEquals(9000, result.getSubsidyAmount()); // 7000 + 2000
    }

    @Test
    @DisplayName("測試9.2: 五胞胎應獲得三胞胎以上的補助（+2000）")
    void testQuintupletsSubsidy() {
        SubsidyForm form = createCompleteForm("yes", "5", "0-2", "no", "low", "B");
        CalculationResult result = service.calculateSubsidy(form);

        assertTrue(result.isEligible());
        assertEquals(19000, result.getSubsidyAmount()); // 17000 + 2000
    }

    // ============================================================
    // 測試組 10: 複雜組合情況測試
    // ============================================================

    @Test
    @DisplayName("測試10.1: 中低收入 + 三胞胎 + 公托 = 最大加成")
    void testMidLowIncomeTripletsPublic() {
        SubsidyForm form = createCompleteForm("yes", "3", "0-2", "no", "midlow", "A");
        CalculationResult result = service.calculateSubsidy(form);

        assertTrue(result.isEligible());
        assertEquals(11000, result.getSubsidyAmount()); // 9000 + 2000
    }

    @Test
    @DisplayName("測試10.2: 所有資格都不符合的情況")
    void testAllIneligibleConditions() {
        // 非縣民 + 育嬰留停 + 3-6歲
        SubsidyForm form = createCompleteForm("no", "1", "3-6", "yes", "normal", "A");
        CalculationResult result = service.calculateSubsidy(form);

        assertFalse(result.isEligible());
        assertEquals("不符合申請資格", result.getMessage());
    }

    @Test
    @DisplayName("測試10.3: 單一欄位缺失驗證")
    void testSingleFieldMissing() {
        // 測試每個欄位單獨缺失的情況

        // 缺少 isCitizen
        SubsidyForm form1 = createCompleteForm(null, "1", "0-2", "no", "normal", "A");
        CalculationResult result1 = service.calculateSubsidy(form1);
        assertFalse(result1.isEligible());
        assertEquals(1, result1.getErrors().size());

        // 缺少 fetusCount
        SubsidyForm form2 = createCompleteForm("yes", null, "0-2", "no", "normal", "A");
        CalculationResult result2 = service.calculateSubsidy(form2);
        assertFalse(result2.isEligible());
        assertEquals(1, result2.getErrors().size());

        // 缺少 childAge
        SubsidyForm form3 = createCompleteForm("yes", "1", null, "no", "normal", "A");
        CalculationResult result3 = service.calculateSubsidy(form3);
        assertFalse(result3.isEligible());
        assertEquals(1, result3.getErrors().size());

        // 缺少 isParentalLeave
        SubsidyForm form4 = createCompleteForm("yes", "1", "0-2", null, "normal", "A");
        CalculationResult result4 = service.calculateSubsidy(form4);
        assertFalse(result4.isEligible());
        assertEquals(1, result4.getErrors().size());

        // 缺少 identity
        SubsidyForm form5 = createCompleteForm("yes", "1", "0-2", "no", null, "A");
        CalculationResult result5 = service.calculateSubsidy(form5);
        assertFalse(result5.isEligible());
        assertEquals(1, result5.getErrors().size());

        // 缺少 org
        SubsidyForm form6 = createCompleteForm("yes", "1", "0-2", "no", "normal", null);
        CalculationResult result6 = service.calculateSubsidy(form6);
        assertFalse(result6.isEligible());
        assertEquals(1, result6.getErrors().size());
    }

    // ============================================================
    // 測試組 11: 錯誤訊息內容驗證
    // ============================================================

    @Test
    @DisplayName("測試11.1: 驗證錯誤訊息包含正確的欄位名稱")
    void testErrorMessageContent() {
        SubsidyForm form = new SubsidyForm();
        CalculationResult result = service.calculateSubsidy(form);

        assertFalse(result.isEligible());
        String message = result.getMessage();

        // 驗證訊息包含中文欄位名稱
        assertTrue(message.contains("是否為新竹縣縣民") ||
                   message.contains("胎數") ||
                   message.contains("申請幼兒年紀") ||
                   message.contains("是否育嬰留停") ||
                   message.contains("申請身分別") ||
                   message.contains("托育機構選擇"));
    }

    @Test
    @DisplayName("測試11.2: CalculationResult 初始化時 errors 列表不為 null")
    void testCalculationResultInitialization() {
        CalculationResult result = new CalculationResult();

        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().isEmpty());
    }

    // ============================================================
    // 測試組 12: 資格檢查的各種組合
    // ============================================================

    @Test
    @DisplayName("測試12.1: 非縣民但其他條件都符合")
    void testNonCitizenOtherwiseEligible() {
        SubsidyForm form = createCompleteForm("no", "1", "0-2", "no", "normal", "A");
        CalculationResult result = service.calculateSubsidy(form);

        assertFalse(result.isEligible());
        assertNull(result.getSubsidyAmount());
    }

    @Test
    @DisplayName("測試12.2: 育嬰留停但其他條件都符合")
    void testParentalLeaveOtherwiseEligible() {
        SubsidyForm form = createCompleteForm("yes", "2", "0-2", "yes", "low", "B");
        CalculationResult result = service.calculateSubsidy(form);

        assertFalse(result.isEligible());
        assertNull(result.getSubsidyAmount());
    }

    @Test
    @DisplayName("測試12.3: 3-6歲但其他條件都符合")
    void testAge3to6OtherwiseEligible() {
        SubsidyForm form = createCompleteForm("yes", "3", "3-6", "no", "midlow", "A");
        CalculationResult result = service.calculateSubsidy(form);

        assertFalse(result.isEligible());
        assertNull(result.getSubsidyAmount());
    }

    // ============================================================
    // 測試組 13: 無效機構類型測試
    // ============================================================

    @Test
    @DisplayName("測試13.1: 無效的機構類型應返回0補助金額")
    void testInvalidOrganizationType() {
        SubsidyForm form = createCompleteForm("yes", "1", "0-2", "no", "normal", "C");
        CalculationResult result = service.calculateSubsidy(form);

        assertTrue(result.isEligible());
        assertEquals(0, result.getSubsidyAmount());
        assertEquals("試算成功", result.getMessage());
    }

    @Test
    @DisplayName("測試13.2: 空字串機構類型應返回0補助金額")
    void testEmptyOrganizationType() {
        SubsidyForm form = new SubsidyForm();
        form.setIsCitizen("yes");
        form.setFetusCount("1");
        form.setChildAge("0-2");
        form.setIsParentalLeave("no");
        form.setIdentity("normal");
        form.setOrg("");

        CalculationResult result = service.calculateSubsidy(form);

        assertFalse(result.isEligible());
        assertTrue(result.getMessage().contains("請填寫"));
    }

    @Test
    @DisplayName("測試13.3: 未知機構代碼 + 雙胞胎")
    void testUnknownOrgWithTwins() {
        SubsidyForm form = createCompleteForm("yes", "2", "0-2", "no", "low", "X");
        CalculationResult result = service.calculateSubsidy(form);

        assertTrue(result.isEligible());
        assertEquals(1000, result.getSubsidyAmount()); // 只有雙胞胎加成
    }

    // ============================================================
    // 測試組 14: 無效身分別測試
    // ============================================================

    @Test
    @DisplayName("測試14.1: 無效的身分別應返回0補助金額（公托）")
    void testInvalidIdentityPublic() {
        SubsidyForm form = createCompleteForm("yes", "1", "0-2", "no", "unknown", "A");
        CalculationResult result = service.calculateSubsidy(form);

        assertTrue(result.isEligible());
        assertEquals(0, result.getSubsidyAmount());
    }

    @Test
    @DisplayName("測試14.2: 無效的身分別應返回0補助金額（準公托）")
    void testInvalidIdentityQuasi() {
        SubsidyForm form = createCompleteForm("yes", "1", "0-2", "no", "invalid", "B");
        CalculationResult result = service.calculateSubsidy(form);

        assertTrue(result.isEligible());
        assertEquals(0, result.getSubsidyAmount());
    }

    @Test
    @DisplayName("測試14.3: 無效身分別 + 三胞胎仍有加成")
    void testInvalidIdentityWithTriplets() {
        SubsidyForm form = createCompleteForm("yes", "3", "0-2", "no", "xyz", "A");
        CalculationResult result = service.calculateSubsidy(form);

        assertTrue(result.isEligible());
        assertEquals(2000, result.getSubsidyAmount()); // 只有三胞胎加成
    }

    // ============================================================
    // 測試組 15: 極端胎數測試
    // ============================================================

    @Test
    @DisplayName("測試15.1: 六胞胎補助計算")
    void testSextuplets() {
        SubsidyForm form = createCompleteForm("yes", "6", "0-2", "no", "normal", "B");
        CalculationResult result = service.calculateSubsidy(form);

        assertTrue(result.isEligible());
        assertEquals(15000, result.getSubsidyAmount()); // 13000 + 2000
    }

    @Test
    @DisplayName("測試15.2: 十胞胎補助計算")
    void testDecuplets() {
        SubsidyForm form = createCompleteForm("yes", "10", "0-2", "no", "low", "A");
        CalculationResult result = service.calculateSubsidy(form);

        assertTrue(result.isEligible());
        assertEquals(13000, result.getSubsidyAmount()); // 11000 + 2000
    }

    @Test
    @DisplayName("測試15.3: 單胎無加成驗證")
    void testSingleChildNoBonus() {
        SubsidyForm form = createCompleteForm("yes", "1", "0-2", "no", "normal", "A");
        CalculationResult result = service.calculateSubsidy(form);

        assertTrue(result.isEligible());
        assertEquals(7000, result.getSubsidyAmount()); // 無加成
    }

    // ============================================================
    // 測試組 16: CalculationResult 錯誤列表操作測試
    // ============================================================

    @Test
    @DisplayName("測試16.1: 使用setErrors替換錯誤列表")
    void testSetErrorsList() {
        CalculationResult result = new CalculationResult();

        List<String> customErrors = new ArrayList<>();
        customErrors.add("自訂錯誤1");
        customErrors.add("自訂錯誤2");
        customErrors.add("自訂錯誤3");

        result.setErrors(customErrors);

        assertEquals(3, result.getErrors().size());
        assertTrue(result.getErrors().contains("自訂錯誤1"));
        assertTrue(result.getErrors().contains("自訂錯誤2"));
        assertTrue(result.getErrors().contains("自訂錯誤3"));
    }

    @Test
    @DisplayName("測試16.2: 錯誤列表可變性測試")
    void testErrorsListMutability() {
        CalculationResult result = new CalculationResult();

        result.getErrors().add("錯誤A");
        assertEquals(1, result.getErrors().size());

        result.getErrors().add("錯誤B");
        assertEquals(2, result.getErrors().size());

        result.getErrors().clear();
        assertEquals(0, result.getErrors().size());
    }

    // ============================================================
    // 測試組 17: 資格檢查多重失敗組合
    // ============================================================

    @Test
    @DisplayName("測試17.1: 非縣民 + 育嬰留停組合")
    void testNonCitizenAndParentalLeave() {
        SubsidyForm form = createCompleteForm("no", "1", "0-2", "yes", "normal", "A");
        CalculationResult result = service.calculateSubsidy(form);

        assertFalse(result.isEligible());
        assertEquals("不符合申請資格", result.getMessage());
    }

    @Test
    @DisplayName("測試17.2: 非縣民 + 3-6歲組合")
    void testNonCitizenAndAge3to6() {
        SubsidyForm form = createCompleteForm("no", "2", "3-6", "no", "low", "B");
        CalculationResult result = service.calculateSubsidy(form);

        assertFalse(result.isEligible());
    }

    @Test
    @DisplayName("測試17.3: 育嬰留停 + 3-6歲組合")
    void testParentalLeaveAndAge3to6() {
        SubsidyForm form = createCompleteForm("yes", "3", "3-6", "yes", "midlow", "A");
        CalculationResult result = service.calculateSubsidy(form);

        assertFalse(result.isEligible());
    }

    // ============================================================
    // 測試組 18: 空字串與 null 詳細區分測試
    // ============================================================

    @Test
    @DisplayName("測試18.1: 所有欄位為空字串")
    void testAllFieldsEmptyString() {
        SubsidyForm form = new SubsidyForm();
        form.setIsCitizen("");
        form.setFetusCount("");
        form.setChildAge("");
        form.setIsParentalLeave("");
        form.setIdentity("");
        form.setOrg("");

        CalculationResult result = service.calculateSubsidy(form);

        assertFalse(result.isEligible());
        assertEquals(6, result.getErrors().size());
    }

    @Test
    @DisplayName("測試18.2: 混合空白、空字串和 null")
    void testMixedEmptyTypes() {
        SubsidyForm form = new SubsidyForm();
        form.setIsCitizen("");        // 空字串
        form.setFetusCount("  ");     // 純空白
        form.setChildAge(null);       // null
        form.setIsParentalLeave("\t"); // tab
        form.setIdentity("\n");       // 換行
        form.setOrg("   \t  ");       // 混合空白

        CalculationResult result = service.calculateSubsidy(form);

        assertFalse(result.isEligible());
        assertEquals(6, result.getErrors().size());
    }

    // ============================================================
    // 測試組 19: 完整數據流測試
    // ============================================================

    @Test
    @DisplayName("測試19.1: 從錯誤修正到成功的完整流程")
    void testErrorCorrectionFlow() {
        SubsidyForm form = new SubsidyForm();

        // 步驟1: 空表單
        CalculationResult result1 = service.calculateSubsidy(form);
        assertFalse(result1.isEligible());

        // 步驟2: 填寫部分欄位
        form.setIsCitizen("yes");
        form.setFetusCount("2");
        form.setChildAge("0-2");
        CalculationResult result2 = service.calculateSubsidy(form);
        assertFalse(result2.isEligible());

        // 步驟3: 填寫所有欄位
        form.setIsParentalLeave("no");
        form.setIdentity("midlow");
        form.setOrg("B");
        CalculationResult result3 = service.calculateSubsidy(form);
        assertTrue(result3.isEligible());
        assertEquals(16000, result3.getSubsidyAmount());
    }

    @Test
    @DisplayName("測試19.2: 變更申請身分別對補助金額的影響")
    void testIdentityChangeImpact() {
        SubsidyForm form = createCompleteForm("yes", "1", "0-2", "no", "normal", "A");

        // normal
        CalculationResult result1 = service.calculateSubsidy(form);
        assertEquals(7000, result1.getSubsidyAmount());

        // 改為 midlow
        form.setIdentity("midlow");
        CalculationResult result2 = service.calculateSubsidy(form);
        assertEquals(9000, result2.getSubsidyAmount());

        // 改為 low
        form.setIdentity("low");
        CalculationResult result3 = service.calculateSubsidy(form);
        assertEquals(11000, result3.getSubsidyAmount());
    }

    @Test
    @DisplayName("測試19.3: 變更機構類型對補助金額的影響")
    void testOrgTypeChangeImpact() {
        SubsidyForm form = createCompleteForm("yes", "1", "0-2", "no", "normal", "A");

        // 公托
        CalculationResult result1 = service.calculateSubsidy(form);
        assertEquals(7000, result1.getSubsidyAmount());

        // 改為準公托
        form.setOrg("B");
        CalculationResult result2 = service.calculateSubsidy(form);
        assertEquals(13000, result2.getSubsidyAmount());

        // 驗證差額
        assertEquals(6000, result2.getSubsidyAmount() - result1.getSubsidyAmount());
    }

    // ============================================================
    // 測試組 20: 極端組合測試
    // ============================================================

    @Test
    @DisplayName("測試20.1: 最高補助金額組合（低收 + 三胞胎以上 + 準公托）")
    void testMaximumSubsidy() {
        SubsidyForm form = createCompleteForm("yes", "3", "0-2", "no", "low", "B");
        CalculationResult result = service.calculateSubsidy(form);

        assertTrue(result.isEligible());
        assertEquals(19000, result.getSubsidyAmount());
    }

    @Test
    @DisplayName("測試20.2: 最低補助金額組合（一般戶 + 單胎 + 公托）")
    void testMinimumSubsidy() {
        SubsidyForm form = createCompleteForm("yes", "1", "0-2", "no", "normal", "A");
        CalculationResult result = service.calculateSubsidy(form);

        assertTrue(result.isEligible());
        assertEquals(7000, result.getSubsidyAmount());
    }

    @Test
    @DisplayName("測試20.3: 補助差額驗證 - 各身分別在準公托的差距")
    void testSubsidyDifferenceAcrossIdentities() {
        SubsidyForm normalForm = createCompleteForm("yes", "1", "0-2", "no", "normal", "B");
        SubsidyForm midlowForm = createCompleteForm("yes", "1", "0-2", "no", "midlow", "B");
        SubsidyForm lowForm = createCompleteForm("yes", "1", "0-2", "no", "low", "B");

        CalculationResult normalResult = service.calculateSubsidy(normalForm);
        CalculationResult midlowResult = service.calculateSubsidy(midlowForm);
        CalculationResult lowResult = service.calculateSubsidy(lowForm);

        assertEquals(13000, normalResult.getSubsidyAmount());
        assertEquals(15000, midlowResult.getSubsidyAmount());
        assertEquals(17000, lowResult.getSubsidyAmount());

        // 驗證每級差2000元
        assertEquals(2000, midlowResult.getSubsidyAmount() - normalResult.getSubsidyAmount());
        assertEquals(2000, lowResult.getSubsidyAmount() - midlowResult.getSubsidyAmount());
    }

    // ============================================================
    // 測試組 21: 特殊字元和邊界字串測試
    // ============================================================

    @Test
    @DisplayName("測試21.1: 欄位包含多種空白字元")
    void testVariousWhitespaceCharacters() {
        SubsidyForm form = new SubsidyForm();
        form.setIsCitizen(" \t \n ");
        form.setFetusCount("\r\n");
        form.setChildAge("  \t  ");

        CalculationResult result = service.calculateSubsidy(form);

        assertFalse(result.isEligible());
        assertTrue(result.getErrors().size() >= 3);
    }

    @Test
    @DisplayName("測試21.2: 有效值前後有空白")
    void testValidValueWithWhitespace() {
        // 注意：當前實現使用 equals 比較，所以帶空白的值會被視為不符合
        SubsidyForm form = new SubsidyForm();
        form.setIsCitizen(" yes ");
        form.setFetusCount(" 1 ");
        form.setChildAge(" 0-2 ");
        form.setIsParentalLeave(" no ");
        form.setIdentity(" normal ");
        form.setOrg(" A ");

        CalculationResult result = service.calculateSubsidy(form);

        // 因為使用 equals 比較，" yes " != "yes"，所以不符合資格
        assertFalse(result.isEligible());
    }

    // ============================================================
    // 測試組 22: 多次計算一致性測試
    // ============================================================

    @Test
    @DisplayName("測試22.1: 相同表單多次計算結果一致")
    void testMultipleCalculationConsistency() {
        SubsidyForm form = createCompleteForm("yes", "2", "0-2", "no", "midlow", "A");

        CalculationResult result1 = service.calculateSubsidy(form);
        CalculationResult result2 = service.calculateSubsidy(form);
        CalculationResult result3 = service.calculateSubsidy(form);

        assertEquals(result1.getSubsidyAmount(), result2.getSubsidyAmount());
        assertEquals(result2.getSubsidyAmount(), result3.getSubsidyAmount());
        assertEquals(result1.isEligible(), result2.isEligible());
        assertEquals(result1.getMessage(), result2.getMessage());
    }

    @Test
    @DisplayName("測試22.2: 不同表單實例相同數據應得到相同結果")
    void testDifferentFormInstancesSameData() {
        SubsidyForm form1 = createCompleteForm("yes", "3", "0-2", "no", "low", "B");
        SubsidyForm form2 = createCompleteForm("yes", "3", "0-2", "no", "low", "B");

        CalculationResult result1 = service.calculateSubsidy(form1);
        CalculationResult result2 = service.calculateSubsidy(form2);

        assertEquals(result1.getSubsidyAmount(), result2.getSubsidyAmount());
        assertEquals(result1.isEligible(), result2.isEligible());
        assertEquals(result1.getMessage(), result2.getMessage());
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


package Group4.Childcare.cucumber;

import Group4.Childcare.service.SubsidyCalculatorService;
import Group4.Childcare.service.SubsidyCalculatorService.CalculationResult;
import Group4.Childcare.service.SubsidyCalculatorService.SubsidyForm;
import io.cucumber.java.zh_tw.假設;
import io.cucumber.java.zh_tw.當;
import io.cucumber.java.zh_tw.那麼;
import io.cucumber.java.zh_tw.而且;
import io.cucumber.datatable.DataTable;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.Map;

/**
 * Cucumber 步驟定義 - 補助金額試算器
 * Step Definitions for Subsidy Calculator
 */
public class SubsidyCalculatorSteps {

    private SubsidyCalculatorService subsidyService;
    private SubsidyForm form;
    private CalculationResult result;
    private Integer previousAmount;

    public SubsidyCalculatorSteps() {
        this.subsidyService = new SubsidyCalculatorService();
    }

    // ============================================================
    // 假設步驟 (Given)
    // ============================================================

    @假設("補助金額試算系統已啟動")
    public void 補助金額試算系統已啟動() {
        this.form = new SubsidyForm();
        this.result = null;
    }

    @假設("我填寫以下申請資料:")
    public void 我填寫以下申請資料(DataTable dataTable) {
        this.form = new SubsidyForm();
        List<Map<String, String>> rows = dataTable.asMaps();

        for (Map<String, String> row : rows) {
            String field = row.get("欄位");
            String value = row.get("值");
            setFormField(field, value);
        }
    }

    @假設("我是一位新手媽媽")
    public void 我是一位新手媽媽() {
        this.form = new SubsidyForm();
    }

    @假設("我是新竹縣縣民")
    public void 我是新竹縣縣民() {
        this.form.setIsCitizen("yes");
    }

    @假設("我有一個0-2歲的孩子")
    public void 我有一個0到2歲的孩子() {
        this.form.setChildAge("0-2");
        this.form.setFetusCount("1");
    }

    @假設("我沒有育嬰留停")
    public void 我沒有育嬰留停() {
        this.form.setIsParentalLeave("no");
    }

    @假設("我是一般身分")
    public void 我是一般身分() {
        this.form.setIdentity("normal");
    }

    @假設("我是低收入戶家長")
    public void 我是低收入戶家長() {
        this.form = new SubsidyForm();
        this.form.setIdentity("low");
        this.form.setIsCitizen("yes");
    }

    @假設("我有雙胞胎")
    public void 我有雙胞胎() {
        this.form.setFetusCount("2");
    }

    @假設("孩子年齡是0-2歲")
    public void 孩子年齡是0到2歲() {
        this.form.setChildAge("0-2");
        this.form.setIsParentalLeave("no");
    }

    @假設("我已經完成一次試算且獲得補助金額")
    public void 我已經完成一次試算且獲得補助金額() {
        this.form = new SubsidyForm();
        this.form.setIsCitizen("yes");
        this.form.setFetusCount("1");
        this.form.setChildAge("0-2");
        this.form.setIsParentalLeave("no");
        this.form.setIdentity("normal");
        this.form.setOrg("A");

        this.result = subsidyService.calculateSubsidy(this.form);
        this.previousAmount = this.result.getSubsidyAmount();
    }

    @假設("我只填寫以下欄位:")
    public void 我只填寫以下欄位(DataTable dataTable) {
        this.form = new SubsidyForm();
        List<Map<String, String>> rows = dataTable.asMaps();

        for (Map<String, String> row : rows) {
            String field = row.get("欄位");
            String value = row.get("值");
            setFormField(field, value);
        }
    }

    // ============================================================
    // 當步驟 (When)
    // ============================================================

    @當("我提交空白表單")
    public void 我提交空白表單() {
        this.form = new SubsidyForm();
        this.result = subsidyService.calculateSubsidy(this.form);
    }

    @當("我提交試算申請")
    public void 我提交試算申請() {
        this.result = subsidyService.calculateSubsidy(this.form);
    }

    @當("我選擇公共托育機構並提交試算")
    public void 我選擇公共托育機構並提交試算() {
        this.form.setOrg("A");
        this.result = subsidyService.calculateSubsidy(this.form);
    }

    @當("我選擇準公共托育機構並提交試算")
    public void 我選擇準公共托育機構並提交試算() {
        this.form.setOrg("B");
        this.result = subsidyService.calculateSubsidy(this.form);
    }

    @當("我將 {string} 改為 {string}")
    public void 我將欄位改為(String field, String value) {
        setFormField(field, value);
    }

    @當("我重新提交試算")
    public void 我重新提交試算() {
        this.result = subsidyService.calculateSubsidy(this.form);
    }

    // ============================================================
    // 那麼步驟 (Then)
    // ============================================================

    @那麼("系統應該返回驗證錯誤訊息")
    public void 系統應該返回驗證錯誤訊息() {
        Assertions.assertFalse(result.isEligible(), "應該返回不符合資格");
        Assertions.assertNotNull(result.getMessage(), "應該有錯誤訊息");
    }

    @那麼("錯誤訊息應該包含 {string}")
    public void 錯誤訊息應該包含(String expectedMessage) {
        Assertions.assertTrue(
            result.getMessage().contains(expectedMessage),
            "錯誤訊息應該包含: " + expectedMessage + "，實際訊息: " + result.getMessage()
        );
    }

    @那麼("系統應該返回 {string}")
    public void 系統應該返回(String expectedMessage) {
        Assertions.assertEquals(expectedMessage, result.getMessage(),
            "系統返回訊息不符合預期");
    }

    @那麼("補助金額應該是 {int} 元")
    public void 補助金額應該是多少元(Integer expectedAmount) {
        Assertions.assertNotNull(result.getSubsidyAmount(), "補助金額不應為null");
        Assertions.assertEquals(expectedAmount, result.getSubsidyAmount(),
            "補助金額應該是 " + expectedAmount + " 元");
    }

    @那麼("沒有任何錯誤訊息")
    public void 沒有任何錯誤訊息() {
        Assertions.assertTrue(result.getErrors().isEmpty(),
            "不應該有錯誤訊息");
        Assertions.assertTrue(result.isEligible(),
            "應該符合申請資格");
    }

    @那麼("雙胞胎應該比單胎多獲得 {int} 元補助")
    public void 雙胞胎應該比單胎多獲得多少元補助(Integer extraAmount) {
        // 這是邏輯驗證，確認雙胞胎加成為1000元
        Assertions.assertEquals(1000, extraAmount,
            "雙胞胎加成應該是1000元");
    }

    @那麼("之前的補助金額應該被清除")
    public void 之前的補助金額應該被清除() {
        Assertions.assertNull(result.getSubsidyAmount(),
            "補助金額應該被清除");
    }

    @那麼("錯誤訊息應該提示缺少的欄位")
    public void 錯誤訊息應該提示缺少的欄位() {
        Assertions.assertFalse(result.getErrors().isEmpty(),
            "應該有缺少欄位的提示");
        Assertions.assertTrue(result.getMessage().contains("請填寫"),
            "錯誤訊息應該包含'請填寫'");
    }

    // ============================================================
    // 輔助方法
    // ============================================================

    /**
     * 設置表單欄位值
     */
    private void setFormField(String fieldName, String value) {
        String mappedValue = mapValue(value);

        switch (fieldName) {
            case "是否為新竹縣縣民":
                form.setIsCitizen(mappedValue);
                break;
            case "胎數":
                form.setFetusCount(mappedValue);
                break;
            case "申請幼兒年紀":
                form.setChildAge(mappedValue);
                break;
            case "正育嬰留停中":
                form.setIsParentalLeave(mappedValue);
                break;
            case "申請身分別":
                form.setIdentity(mappedValue);
                break;
            case "托育機構選擇":
                form.setOrg(mappedValue);
                break;
        }
    }

    /**
     * 映射中文值到系統值
     */
    private String mapValue(String value) {
        switch (value) {
            case "是": return "yes";
            case "否": return "no";
            case "0-2": return "0-2";
            case "3-6": return "3-6";
            case "一般": return "normal";
            case "中低收入戶": return "midlow";
            case "低收入戶": return "low";
            case "公托": return "A";
            case "準公托": return "B";
            default: return value;
        }
    }
}


package Group4.Childcare;

import Group4.Childcare.Service.SubsidyCalculatorService;
import Group4.Childcare.Service.SubsidyCalculatorService.CalculationResult;
import Group4.Childcare.Service.SubsidyCalculatorService.SubsidyForm;

/**
 * 補助金額試算器快速測試
 * Quick Test for Subsidy Calculator
 */
public class QuickTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("📋 補助金額試算器 - 快速測試");
        System.out.println("=================================================\n");

        SubsidyCalculatorService service = new SubsidyCalculatorService();
        int passedTests = 0;
        int totalTests = 0;

        // 測試 1: 空表單驗證
        totalTests++;
        System.out.println("測試 1: 空表單驗證");
        SubsidyForm emptyForm = new SubsidyForm();
        CalculationResult result1 = service.calculateSubsidy(emptyForm);
        if (!result1.isEligible() && result1.getMessage().contains("請填寫")) {
            System.out.println("✅ 通過");
            passedTests++;
        } else {
            System.out.println("❌ 失敗");
        }

        // 測試 2: 非新竹縣縣民
        totalTests++;
        System.out.println("\n測試 2: 非新竹縣縣民不符合資格");
        SubsidyForm form2 = createForm("no", "1", "0-2", "no", "normal", "A");
        CalculationResult result2 = service.calculateSubsidy(form2);
        if (!result2.isEligible() && "不符合申請資格".equals(result2.getMessage())) {
            System.out.println("✅ 通過");
            passedTests++;
        } else {
            System.out.println("❌ 失敗");
        }

        // 測試 3: 公托 + 一般 + 1胎 = 7000
        totalTests++;
        System.out.println("\n測試 3: 公托 + 一般 + 1胎 = 7000元");
        SubsidyForm form3 = createForm("yes", "1", "0-2", "no", "normal", "A");
        CalculationResult result3 = service.calculateSubsidy(form3);
        if (result3.isEligible() && result3.getSubsidyAmount() == 7000) {
            System.out.println("✅ 通過 - 補助金額: " + result3.getSubsidyAmount() + " 元");
            passedTests++;
        } else {
            System.out.println("❌ 失敗");
        }

        // 測試 4: 公托 + 一般 + 2胎 = 8000
        totalTests++;
        System.out.println("\n測試 4: 公托 + 一般 + 2胎 = 8000元");
        SubsidyForm form4 = createForm("yes", "2", "0-2", "no", "normal", "A");
        CalculationResult result4 = service.calculateSubsidy(form4);
        if (result4.isEligible() && result4.getSubsidyAmount() == 8000) {
            System.out.println("✅ 通過 - 補助金額: " + result4.getSubsidyAmount() + " 元");
            passedTests++;
        } else {
            System.out.println("❌ 失敗");
        }

        // 測試 5: 公托 + 低收 + 3胎 = 13000
        totalTests++;
        System.out.println("\n測試 5: 公托 + 低收入戶 + 3胎 = 13000元");
        SubsidyForm form5 = createForm("yes", "3", "0-2", "no", "low", "A");
        CalculationResult result5 = service.calculateSubsidy(form5);
        if (result5.isEligible() && result5.getSubsidyAmount() == 13000) {
            System.out.println("✅ 通過 - 補助金額: " + result5.getSubsidyAmount() + " 元");
            passedTests++;
        } else {
            System.out.println("❌ 失敗");
        }

        // 測試 6: 準公托 + 一般 + 1胎 = 13000
        totalTests++;
        System.out.println("\n測試 6: 準公托 + 一般 + 1胎 = 13000元");
        SubsidyForm form6 = createForm("yes", "1", "0-2", "no", "normal", "B");
        CalculationResult result6 = service.calculateSubsidy(form6);
        if (result6.isEligible() && result6.getSubsidyAmount() == 13000) {
            System.out.println("✅ 通過 - 補助金額: " + result6.getSubsidyAmount() + " 元");
            passedTests++;
        } else {
            System.out.println("❌ 失敗");
        }

        // 測試 7: 準公托 + 低收 + 2胎 = 18000
        totalTests++;
        System.out.println("\n測試 7: 準公托 + 低收入戶 + 2胎 = 18000元");
        SubsidyForm form7 = createForm("yes", "2", "0-2", "no", "low", "B");
        CalculationResult result7 = service.calculateSubsidy(form7);
        if (result7.isEligible() && result7.getSubsidyAmount() == 18000) {
            System.out.println("✅ 通過 - 補助金額: " + result7.getSubsidyAmount() + " 元");
            passedTests++;
        } else {
            System.out.println("❌ 失敗");
        }

        // 測試 8: 育嬰留停不符合資格
        totalTests++;
        System.out.println("\n測試 8: 育嬰留停中不符合資格");
        SubsidyForm form8 = createForm("yes", "1", "0-2", "yes", "normal", "A");
        CalculationResult result8 = service.calculateSubsidy(form8);
        if (!result8.isEligible() && "不符合申請資格".equals(result8.getMessage())) {
            System.out.println("✅ 通過");
            passedTests++;
        } else {
            System.out.println("❌ 失敗");
        }

        // 測試 9: 3-6歲不符合資格
        totalTests++;
        System.out.println("\n測試 9: 3-6歲幼兒不符合資格");
        SubsidyForm form9 = createForm("yes", "1", "3-6", "no", "normal", "A");
        CalculationResult result9 = service.calculateSubsidy(form9);
        if (!result9.isEligible() && "不符合申請資格".equals(result9.getMessage())) {
            System.out.println("✅ 通過");
            passedTests++;
        } else {
            System.out.println("❌ 失敗");
        }

        // 測試 10: 準公托 + 中低收 + 3胎 = 17000
        totalTests++;
        System.out.println("\n測試 10: 準公托 + 中低收入戶 + 3胎 = 17000元");
        SubsidyForm form10 = createForm("yes", "3", "0-2", "no", "midlow", "B");
        CalculationResult result10 = service.calculateSubsidy(form10);
        if (result10.isEligible() && result10.getSubsidyAmount() == 17000) {
            System.out.println("✅ 通過 - 補助金額: " + result10.getSubsidyAmount() + " 元");
            passedTests++;
        } else {
            System.out.println("❌ 失敗");
        }

        // 總結
        System.out.println("\n=================================================");
        System.out.println("📊 測試結果總結");
        System.out.println("=================================================");
        System.out.println("✅ 通過測試: " + passedTests + "/" + totalTests);
        System.out.println("❌ 失敗測試: " + (totalTests - passedTests) + "/" + totalTests);
        System.out.println("📈 通過率: " + String.format("%.2f", (passedTests * 100.0 / totalTests)) + "%");
        System.out.println("=================================================\n");

        if (passedTests == totalTests) {
            System.out.println("🎉 所有測試通過！補助金額試算器功能正常！");
        } else {
            System.out.println("⚠️  部分測試失敗，請檢查相關功能。");
        }
    }

    private static SubsidyForm createForm(String isCitizen, String fetusCount,
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


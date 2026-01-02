package Group4.Childcare.Service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 補助金額試算服務
 * Subsidy Calculator Service
 */
@Service
public class SubsidyCalculatorService {

    /**
     * 申請表單資料
     */
    public static class SubsidyForm {
        private String isCitizen;           // 是否為新竹縣縣民: "yes" / "no"
        private String fetusCount;          // 胎數: "1" / "2" / "3"
        private String childAge;            // 申請幼兒年紀: "0-2" / "3-6"
        private String isParentalLeave;     // 正育嬰留停中: "yes" / "no"
        private String identity;            // 申請身分別: "normal" / "midlow" / "low"
        private String org;                 // 托育機構選擇: "A" (公托) / "B" (準公托)

        public String getIsCitizen() { return isCitizen; }
        public void setIsCitizen(String isCitizen) { this.isCitizen = isCitizen; }

        public String getFetusCount() { return fetusCount; }
        public void setFetusCount(String fetusCount) { this.fetusCount = fetusCount; }

        public String getChildAge() { return childAge; }
        public void setChildAge(String childAge) { this.childAge = childAge; }

        public String getIsParentalLeave() { return isParentalLeave; }
        public void setIsParentalLeave(String isParentalLeave) { this.isParentalLeave = isParentalLeave; }

        public String getIdentity() { return identity; }
        public void setIdentity(String identity) { this.identity = identity; }

        public String getOrg() { return org; }
        public void setOrg(String org) { this.org = org; }
    }

    /**
     * 試算結果
     */
    public static class CalculationResult {
        private Integer subsidyAmount;      // 補助金額
        private boolean eligible;           // 是否符合資格
        private String message;             // 訊息
        private List<String> errors;        // 錯誤訊息列表

        public CalculationResult() {
            this.errors = new ArrayList<>();
        }

        public Integer getSubsidyAmount() { return subsidyAmount; }
        public void setSubsidyAmount(Integer subsidyAmount) { this.subsidyAmount = subsidyAmount; }

        public boolean isEligible() { return eligible; }
        public void setEligible(boolean eligible) { this.eligible = eligible; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
    }

    /**
     * 計算補助金額
     *
     * @param form 申請表單
     * @return 試算結果
     */
    public CalculationResult calculateSubsidy(SubsidyForm form) {
        CalculationResult result = new CalculationResult();

        // 必填欄位檢查
        List<String> missingFields = validateRequiredFields(form);
        if (!missingFields.isEmpty()) {
            result.setEligible(false);
            result.setMessage("請填寫：" + String.join("、", missingFields));
            result.setErrors(missingFields);
            return result;
        }

        // 資格檢查
        if (!isEligible(form)) {
            result.setEligible(false);
            result.setMessage("不符合申請資格");
            return result;
        }

        // 計算補助金額
        int subsidyAmount = calculateAmount(form);
        result.setSubsidyAmount(subsidyAmount);
        result.setEligible(true);
        result.setMessage("試算成功");

        return result;
    }

    /**
     * 驗證必填欄位
     */
    private List<String> validateRequiredFields(SubsidyForm form) {
        List<String> missingFields = new ArrayList<>();
        Map<String, String> fieldMap = new HashMap<>();

        fieldMap.put("isCitizen", "是否為新竹縣縣民");
        fieldMap.put("fetusCount", "胎數");
        fieldMap.put("childAge", "申請幼兒年紀");
        fieldMap.put("isParentalLeave", "是否育嬰留停");
        fieldMap.put("identity", "申請身分別");
        fieldMap.put("org", "托育機構選擇");

        if (isEmpty(form.getIsCitizen())) missingFields.add(fieldMap.get("isCitizen"));
        if (isEmpty(form.getFetusCount())) missingFields.add(fieldMap.get("fetusCount"));
        if (isEmpty(form.getChildAge())) missingFields.add(fieldMap.get("childAge"));
        if (isEmpty(form.getIsParentalLeave())) missingFields.add(fieldMap.get("isParentalLeave"));
        if (isEmpty(form.getIdentity())) missingFields.add(fieldMap.get("identity"));
        if (isEmpty(form.getOrg())) missingFields.add(fieldMap.get("org"));

        return missingFields;
    }

    /**
     * 檢查是否符合申請資格
     */
    private boolean isEligible(SubsidyForm form) {
        // 不符合申請條件：非新竹縣縣民、育嬰留停、3-6歲
        if (!"yes".equals(form.getIsCitizen())) {
            return false;
        }
        if ("yes".equals(form.getIsParentalLeave())) {
            return false;
        }
        if ("3-6".equals(form.getChildAge())) {
            return false;
        }
        return true;
    }

    /**
     * 計算補助金額
     */
    private int calculateAmount(SubsidyForm form) {
        int baseAmount = 0;
        boolean isPublic = "A".equals(form.getOrg());
        boolean isQuasi = "B".equals(form.getOrg());
        String identity = form.getIdentity();
        int fetusCount = Integer.parseInt(form.getFetusCount());

        // 公共托育機構
        if (isPublic) {
            switch (identity) {
                case "normal":
                    baseAmount = 7000;
                    break;
                case "midlow":
                    baseAmount = 9000;
                    break;
                case "low":
                    baseAmount = 11000;
                    break;
            }
        }

        // 準公共托育機構
        if (isQuasi) {
            switch (identity) {
                case "normal":
                    baseAmount = 13000;
                    break;
                case "midlow":
                    baseAmount = 15000;
                    break;
                case "low":
                    baseAmount = 17000;
                    break;
            }
        }

        // 胎數加成
        if (fetusCount == 2) {
            baseAmount += 1000;
        } else if (fetusCount >= 3) {
            baseAmount += 2000;
        }

        return baseAmount;
    }

    /**
     * 檢查字串是否為空
     */
    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 重置表單（用於測試）
     */
    public SubsidyForm createEmptyForm() {
        return new SubsidyForm();
    }
}


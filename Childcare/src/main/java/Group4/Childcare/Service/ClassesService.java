package Group4.Childcare.Service;

import Group4.Childcare.Model.Classes;
import Group4.Childcare.Repository.ClassesJdbcRepository;
import Group4.Childcare.DTO.ClassSummaryDTO;
import Group4.Childcare.DTO.ClassNameDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClassesService {
    @Autowired
    private ClassesJdbcRepository repository;

    /**
     * 新增一筆班級資料
     * @param entity Classes 實體
     * @return 新增後的 Classes
     */
    public Classes create(Classes entity) {
        return repository.save(entity);
    }

    /**
     * 依班級ID查詢班級資料
     * @param id 班級ID
     * @return 查詢結果 Optional<Classes>
     */
    public Optional<Classes> getById(UUID id) {
        return repository.findById(id);
    }

    /**
     * 查詢所有班級資料
     * @return 班級列表
     */
    public List<Classes> getAll() {
        return repository.findAll();
    }

    /**
     * 取得所有班級與其機構名稱（使用 JDBC LEFT JOIN）
     * @return List<ClassSummaryDTO>
     */
    public List<ClassSummaryDTO> getAllWithInstitutionName() {
        return repository.findAllWithInstitutionName();
    }

    /**
     * 更新班級資料
     * @param id 班級ID
     * @param entity 更新內容
     * @return 更新後的 Classes
     */
    public Classes update(UUID id, Classes entity) {
        // 檢查記錄是否存在
        if (!repository.existsById(id)) {
            throw new RuntimeException("班級記錄不存在，ID: " + id);
        }

        entity.setClassID(id);
        return repository.save(entity);
    }

    /**
     * 刪除班級資料
     * @param id 班級ID
     */
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    /**
     * 依機構ID查詢班級資料
     * @param institutionId 機構ID
     * @return 查詢結果 List<Classes>
     */
    public List<Classes> getByInstitutionId(UUID institutionId) {
        return repository.findByInstitutionId(institutionId);
    }

    // 使用JDBC的offset分頁方法 - 一次取10筆
    public List<Classes> getClassesWithOffsetJdbc(int offset) {
        return repository.findWithOffset(offset, 10);
    }

    // 使用JDBC的offset分頁方法，包含機構名稱 - 可指定每頁筆數
    public List<ClassSummaryDTO> getClassesWithOffsetAndInstitutionNameJdbc(int offset, int size) {
        return repository.findWithOffsetAndInstitutionName(offset, size);
    }

    // 使用JDBC的offset分頁方法，根據機構 ID 過濾（admin 角色使用）
    public List<ClassSummaryDTO> getClassesWithOffsetAndInstitutionNameByInstitutionID(int offset, int size, UUID institutionID) {
        return repository.findWithOffsetAndInstitutionNameByInstitutionID(offset, size, institutionID);
    }

    // 取得總筆數用於分頁計算
    public long getTotalCount() {
        return repository.countTotal();
    }

    // 取得指定機構的班級總數用於分頁計算（admin 角色使用）
    public long getTotalCountByInstitutionID(UUID institutionID) {
        return repository.countByInstitutionID(institutionID);
    }

    /**
     * 依機構名稱模糊搜尋，返回機構及其班級資料
     * @param institutionName 機構名稱關鍵字
     * @return 處理後的機構與班級資料列表
     */
    public List<java.util.Map<String, Object>> searchInstitutionsWithClassesByName(String institutionName) {
        List<java.util.Map<String, Object>> rawResults = repository.findInstitutionsWithClassesByName(institutionName);

        // 將相同機構的班級資料合併
        java.util.Map<String, java.util.Map<String, Object>> institutionMap = new java.util.LinkedHashMap<>();

        for (java.util.Map<String, Object> row : rawResults) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> institution = (java.util.Map<String, Object>) row.get("institution");
            String institutionId = (String) institution.get("institutionID");

            if (!institutionMap.containsKey(institutionId)) {
                java.util.Map<String, Object> institutionWithClasses = new java.util.HashMap<>();
                institutionWithClasses.put("institution", institution);
                institutionWithClasses.put("classes", new java.util.ArrayList<java.util.Map<String, Object>>());
                institutionMap.put(institutionId, institutionWithClasses);
            }

            // 如果有班級資料，加入班級列表
            if (row.get("class") != null) {
                @SuppressWarnings("unchecked")
                java.util.List<java.util.Map<String, Object>> classes =
                    (java.util.List<java.util.Map<String, Object>>) institutionMap.get(institutionId).get("classes");
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> classData = (java.util.Map<String, Object>) row.get("class");
                classes.add(classData);
            }
        }

        return new java.util.ArrayList<>(institutionMap.values());
    }

    /**
     * 依機構名稱模糊搜尋班級，回傳 ClassSummaryDTO 列表
     * @param institutionName 機構名稱關鍵字
     * @return ClassSummaryDTO 列表
     */
    public List<ClassSummaryDTO> searchClassesByInstitutionName(String institutionName) {
        return repository.findClassesByInstitutionName(institutionName);
    }

    /**
     * 根據機構UUID查詢該機構的所有班級名稱
     * @param institutionId 機構UUID
     * @return List<ClassNameDTO> 班級名稱列表
     */
    public List<ClassNameDTO> getClassNamesByInstitutionId(UUID institutionId) {
        return repository.findClassNamesByInstitutionId(institutionId);
    }
}

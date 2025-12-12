package Group4.Childcare.Service;

import Group4.Childcare.Model.Institutions;
import Group4.Childcare.DTO.InstitutionSummaryDTO;
import Group4.Childcare.DTO.InstitutionSimpleDTO;
import Group4.Childcare.DTO.InstitutionOffsetDTO;
import Group4.Childcare.Repository.InstitutionsJdbcRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InstitutionsService {
    @Autowired
    private InstitutionsJdbcRepository repository;

    @Autowired
    private FileService fileService;

    public Institutions create(Institutions entity) {
        return repository.save(entity);
    }

    public Optional<Institutions> getById(UUID id) {
        return repository.findById(id);
    }

    public List<Institutions> getAll() {
        return repository.findAll();
    }

    public Institutions update(UUID id, Institutions entity) {
        entity.setInstitutionID(id);
        return repository.save(entity);
    }

    /**
     * 更新機構資訊並上傳圖片
     * @param id 機構ID
     * @param entity 機構資料
     * @param imageFile 圖片檔案
     * @return 更新後的機構資訊
     * @throws IOException 如果圖片上傳失敗
     */
    public Institutions updateWithImage(UUID id, Institutions entity, MultipartFile imageFile) throws IOException {
        entity.setInstitutionID(id);

        // 如果有上傳新圖片，則儲存並更新 imagePath
        if (imageFile != null && !imageFile.isEmpty()) {
            String imagePath = fileService.saveInstitutionImage(imageFile, id);
            entity.setImagePath(imagePath);
        }

        return repository.save(entity);
    }

    public List<InstitutionSummaryDTO> getSummaryAll() {
        return repository.findSummaryData();
    }

    /**
     * 取得所有機構的 ID 和 name
     * @return List<InstitutionSimpleDTO>
     */
    public List<InstitutionSimpleDTO> getAllSimple() {
        return repository.findAllSimple();
    }

    /**
     * 取得機構分頁資料
     * @param offset 起始項目索引
     * @param size 每頁大小
     * @param institutionID 機構 ID（可選，admin 角色使用）
     * @param search 搜尋關鍵字（可選，搜尋機構名稱、聯絡人、電話）
     * @return InstitutionOffsetDTO
     */
    public InstitutionOffsetDTO getOffset(int offset, int size, UUID institutionID, String search) {
        // 驗證參數
        if (size <= 0) size = 10;
        if (offset < 0) offset = 0;

        // 根據是否有 institutionID 和 search 決定查詢方式
        List<Institutions> content;
        long totalElements;

        if (institutionID != null) {
            // admin 角色：只查詢指定機構的資料
            if (search != null && !search.trim().isEmpty()) {
                content = repository.findByInstitutionIDWithSearchAndPagination(institutionID, search.trim(), offset, size);
                totalElements = repository.countByInstitutionIDWithSearch(institutionID, search.trim());
            } else {
                content = repository.findByInstitutionIDWithPagination(institutionID, offset, size);
                totalElements = repository.countByInstitutionID(institutionID);
            }
        } else {
            // super_admin 角色：查詢所有機構
            if (search != null && !search.trim().isEmpty()) {
                content = repository.findAllWithSearchAndPagination(search.trim(), offset, size);
                totalElements = repository.countAllWithSearch(search.trim());
            } else {
                content = repository.findAllWithPagination(offset, size);
                totalElements = repository.count();
            }
        }

        // 計算總頁數
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = (offset + size) < totalElements;

        // 組建 DTO
        InstitutionOffsetDTO dto = new InstitutionOffsetDTO();
        dto.setOffset(offset);
        dto.setSize(size);
        dto.setTotalPages(totalPages);
        dto.setHasNext(hasNext);
        dto.setContent(content);
        dto.setTotalElements(totalElements);

        return dto;
    }

    /**
     * 取得機構分頁資料（僅搜尋機構名稱）
     * @param offset 起始項目索引
     * @param size 每頁大小
     * @param institutionID 機構 ID（可選，admin 角色使用）
     * @param name 機構名稱搜尋關鍵字（可選，僅搜尋機構名稱）
     * @return InstitutionOffsetDTO
     */
    public InstitutionOffsetDTO getOffsetByName(int offset, int size, UUID institutionID, String name) {
        // 驗證參數
        if (size <= 0) size = 10;
        if (offset < 0) offset = 0;

        // 根據是否有 institutionID 和 name 決定查詢方式
        List<Institutions> content;
        long totalElements;

        if (institutionID != null) {
            // admin 角色：只查詢指定機構的資料
            if (name != null && !name.trim().isEmpty()) {
                content = repository.findByInstitutionIDWithNameSearchAndPagination(institutionID, name.trim(), offset, size);
                totalElements = repository.countByInstitutionIDWithNameSearch(institutionID, name.trim());
            } else {
                content = repository.findByInstitutionIDWithPagination(institutionID, offset, size);
                totalElements = repository.countByInstitutionID(institutionID);
            }
        } else {
            // super_admin 角色：查詢所有機構
            if (name != null && !name.trim().isEmpty()) {
                content = repository.findAllWithNameSearchAndPagination(name.trim(), offset, size);
                totalElements = repository.countAllWithNameSearch(name.trim());
            } else {
                content = repository.findAllWithPagination(offset, size);
                totalElements = repository.count();
            }
        }

        // 計算總頁數
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = (offset + size) < totalElements;

        // 組建 DTO
        InstitutionOffsetDTO dto = new InstitutionOffsetDTO();
        dto.setOffset(offset);
        dto.setSize(size);
        dto.setTotalPages(totalPages);
        dto.setHasNext(hasNext);
        dto.setContent(content);
        dto.setTotalElements(totalElements);

        return dto;
    }
}

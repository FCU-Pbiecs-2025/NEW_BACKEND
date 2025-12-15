package Group4.Childcare.Service;

import Group4.Childcare.Model.Banners;
import Group4.Childcare.Repository.BannersJdbcRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BannersService {
    private final BannersJdbcRepository repository;

    @Autowired
    public BannersService(BannersJdbcRepository repository) {
        this.repository = repository;
    }

    public Banners create(Banners entity) {
        return repository.save(entity);
    }

    public Optional<Banners> getById(Integer id) {
        return repository.findById(id);
    }

    public List<Banners> getAll() {
        return repository.findAll();
    }

    public List<Banners> getBannersWithOffsetJdbc(int offset, int limit) {
        return repository.findWithOffset(offset, limit);
    }

    public long getTotalCount() {
        return repository.count();
    }

    public Banners update(Integer id, Banners entity) {
        entity.setSortOrder(id);
        return repository.save(entity);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }

    // 取得上架且未過期的 banners
    public List<Banners> findActiveBanners() {
        return repository.findActiveBanners();
    }

    /**
     * 定時檢查並自動下架已過期的 banners
     * 每分鐘執行一次
     */
    @Scheduled(cron = "0 * * * * *") // 每分鐘 0 秒執行
    public void autoExpireBanners() {
        int updated = repository.updateExpiredBanners();

    }

    /**
     * 按日期範圍查詢 banners（分頁）
     * @param startDate 開始日期（可為 null）
     * @param endDate 結束日期（可為 null）
     * @param offset 偏移量
     * @param limit 每頁筆數
     * @return 符合條件的 banners 列表
     */
    public List<Banners> getBannersByDateRange(java.sql.Timestamp startDate, java.sql.Timestamp endDate, int offset, int limit) {
        return repository.findByDateRangeWithOffset(startDate, endDate, offset, limit);
    }

    /**
     * 計算符合日期範圍條件的 banners 總數
     * @param startDate 開始日期（可為 null）
     * @param endDate 結束日期（可為 null）
     * @return 符合條件的總筆數
     */
    public long getCountByDateRange(java.sql.Timestamp startDate, java.sql.Timestamp endDate) {
        return repository.countByDateRange(startDate, endDate);
    }
}

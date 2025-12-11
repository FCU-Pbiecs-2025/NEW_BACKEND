package Group4.Childcare.Service;

import Group4.Childcare.Model.Announcements;
import Group4.Childcare.DTO.AnnouncementSummaryDTO;
import Group4.Childcare.Repository.AnnouncementsJdbcRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AnnouncementsService {
    @Autowired
    private AnnouncementsJdbcRepository jdbcRepository;

    public Announcements create(Announcements entity) {
        return jdbcRepository.save(entity);
    }

    public Optional<Announcements> getById(UUID id) {
        return jdbcRepository.findById(id);
    }

    public List<Announcements> getAll() {
        return jdbcRepository.findAll();
    }

    public Announcements update(UUID id, Announcements entity) {
        entity.setAnnouncementID(id);
        return jdbcRepository.save(entity);
    }

    public List<AnnouncementSummaryDTO> getSummaryAll() {
        return jdbcRepository.findSummaryData();
    }

    // 新增：取得後台用的 active announcements summaries（Type=2, Status=1, EndDate >= today）
    public List<AnnouncementSummaryDTO> getAdminActiveBackend() {
        return jdbcRepository.findAdminActiveBackend();
    }

    // 使用JDBC的offset分頁方法 - 一次取8筆
    public List<Announcements> getAnnouncementsWithOffsetJdbc(int offset) {
        return jdbcRepository.findWithOffset(offset, 8);
    }

    // 取得總筆數用於分頁計算
    public long getTotalCount() {
        return jdbcRepository.countTotal();
    }

    public Announcements createAnnouncementJdbc(Announcements entity) {
        if (entity.getAnnouncementID() == null) {
            entity.setAnnouncementID(UUID.randomUUID());
        }
        return jdbcRepository.insertWithAttachment(entity);
    }

    // Update using JDBC repository (will call update when AnnouncementID is present)
    public Announcements updateWithJdbc(UUID id, Announcements entity) {
        entity.setAnnouncementID(id);
        return jdbcRepository.save(entity);
    }

    // Delete announcement by ID
    public boolean delete(UUID id) {
        try {
            // Check if announcement exists before deletion
            if (!jdbcRepository.existsById(id)) {
                return false;
            }
            jdbcRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

package Group4.Childcare.Controller;

import Group4.Childcare.DTO.AnnouncementSummaryDTO;
import Group4.Childcare.Repository.AnnouncementsJdbcRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;

@RestController
@RequestMapping("/adminhome")
public class AdminHomeController {
  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private AnnouncementsJdbcRepository announcementsJdbcRepository;

  // 取得後台公告（回傳 DTO）
  @GetMapping("/announcements")
  public ResponseEntity<List<AnnouncementSummaryDTO>> getAdminAnnouncements() {
    List<AnnouncementSummaryDTO> result = announcementsJdbcRepository.findAdminActiveSummaries();
    return ResponseEntity.ok(result);
  }

  // 取得代辦事項數量 (status="審核中":聲請中, status="撤銷申請審核中":撤銷聲請中)
  // 支援依機構ID篩選：super_admin 不傳 InstitutionID 查全部；admin 傳 InstitutionID 查該機構
  @GetMapping("/todo-counts")
  public ResponseEntity<Map<String, Integer>> getTodoCounts(
          @RequestParam(value = "InstitutionID", required = false) String InstitutionID) {

    Integer count1;
    Integer count5;

    if (InstitutionID != null && !InstitutionID.trim().isEmpty()) {
      // 有指定機構ID：統計該機構的待辦事項
      String sql1 = "SELECT COUNT(*) FROM dbo.application_participants ap " +
                    "LEFT JOIN dbo.applications a ON ap.ApplicationID = a.ApplicationID " +
                    "WHERE ap.Status = ? AND a.InstitutionID = ?  and ap.ParticipantType = 0 ";
      count1 = jdbcTemplate.queryForObject(sql1, Integer.class, "審核中", UUID.fromString(InstitutionID));
      count5 = jdbcTemplate.queryForObject(sql1, Integer.class, "撤銷申請審核中", UUID.fromString(InstitutionID));
    } else {
      // 沒有指定機構ID：統計全部
      count1 = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.application_participants WHERE Status = ? and ParticipantType = 0", Integer.class, "審核中");
      count5 = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.application_participants WHERE Status = ? and ParticipantType = 0", Integer.class, "撤銷申請審核中");
    }

    Map<String, Integer> result = new HashMap<>();
    result.put("pending", count1 != null ? count1 : 0);
    result.put("revoke", count5 != null ? count5 : 0);
    return ResponseEntity.ok(result);
  }
}
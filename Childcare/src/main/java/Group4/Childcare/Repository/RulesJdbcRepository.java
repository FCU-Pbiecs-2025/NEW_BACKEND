package Group4.Childcare.Repository;

import Group4.Childcare.Model.Rules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class RulesJdbcRepository {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private static final String TABLE_NAME = "rules";

  /**
   * RowMapper 用於將資料庫查詢結果映射到 Rules 物件
   */
  private static final RowMapper<Rules> RULES_ROW_MAPPER = new RowMapper<Rules>() {
    @Override
    public Rules mapRow(ResultSet rs, int rowNum) throws SQLException {
      Rules rules = new Rules();
      rules.setId(rs.getLong("id"));
      rules.setAdmissionEligibility(rs.getString("AdmissionEligibility"));
      rules.setServiceContentAndTime(rs.getString("ServiceContentAndTime"));
      rules.setFeeAndRefundPolicy(rs.getString("FeeAndRefundPolicy"));
      return rules;
    }
  };

  /**
   * 儲存規則 - 根據 ID 判斷是新增還是更新
   * @param rules 規則物件
   * @return 儲存後的規則物件
   */
  public Rules save(Rules rules) {
    if (rules.getId() == null || rules.getId() == 0) {
      return insert(rules);
    } else {
      return update(rules);
    }
  }

  /**
   * 新增規則到資料庫 (ID 自動產生)
   * @param rules 要新增的規則
   * @return 新增後的規則物件
   */
  private Rules insert(Rules rules) {
    String sql = "INSERT INTO " + TABLE_NAME +
            " (AdmissionEligibility, ServiceContentAndTime, FeeAndRefundPolicy) " +
            "VALUES (?, ?, ?)";

    jdbcTemplate.update(sql,
            rules.getAdmissionEligibility(),
            rules.getServiceContentAndTime(),
            rules.getFeeAndRefundPolicy()
    );

    // 注意：實際應用中，你可能需要取得自動產生的 ID
    // 這是簡化版本
    return rules;
  }

  /**
   * 更新現有規則
   * @param rules 要更新的規則物件 (必須包含 ID)
   * @return 更新後的規則物件
   */
  private Rules update(Rules rules) {
    String sql = "UPDATE " + TABLE_NAME +
            " SET AdmissionEligibility = ?, ServiceContentAndTime = ?, FeeAndRefundPolicy = ? " +
            "WHERE id = ?";

    jdbcTemplate.update(sql,
            rules.getAdmissionEligibility(),
            rules.getServiceContentAndTime(),
            rules.getFeeAndRefundPolicy(),
            rules.getId()
    );

    return rules;
  }

  /**
   * 使用 JDBC 更新指定 ID 的規則
   * @param id 規則ID
   * @param rules 新的規則內容
   * @return 更新後的規則
   */
  public Rules updateById(Long id, Rules rules) {
    String sql = "UPDATE " + TABLE_NAME +
            " SET AdmissionEligibility = ?, ServiceContentAndTime = ?, FeeAndRefundPolicy = ? " +
            "WHERE id = ?";

    int rowsAffected = jdbcTemplate.update(sql,
            rules.getAdmissionEligibility(),
            rules.getServiceContentAndTime(),
            rules.getFeeAndRefundPolicy(),
            id
    );

    if (rowsAffected > 0) {
      rules.setId(id);
      return rules;
    } else {
      throw new RuntimeException("Rules with id " + id + " not found");
    }
  }

  /**
   * 依據 ID 查詢單一規則
   * @param id 規則 ID
   * @return Optional 包裝的規則物件，如果不存在則為空
   */
  public Optional<Rules> findById(Long id) {
    String sql = "SELECT * FROM " + TABLE_NAME + " WHERE id = ?";
    try {
      Rules rules = jdbcTemplate.queryForObject(sql, RULES_ROW_MAPPER, id);
      return Optional.ofNullable(rules);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * 查詢所有規則
   * @return 規則列表
   */
  public List<Rules> findAll() {
    String sql = "SELECT * FROM " + TABLE_NAME;
    return jdbcTemplate.query(sql, RULES_ROW_MAPPER);
  }

  /**
   * 依據 ID 刪除規則
   * @param id 要刪除的規則 ID
   */
  public void deleteById(Long id) {
    String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
    jdbcTemplate.update(sql, id);
  }

  /**
   * 刪除指定的規則物件
   * @param rules 要刪除的規則物件
   */
  public void delete(Rules rules) {
    deleteById(rules.getId());
  }

  /**
   * 檢查指定 ID 的規則是否存在
   * @param id 規則 ID
   * @return true 如果存在，false 如果不存在
   */
  public boolean existsById(Long id) {
    String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE id = ?";
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
    return count != null && count > 0;
  }

  /**
   * 計算規則總數
   * @return 規則總數
   */
  public long count() {
    String sql = "SELECT COUNT(*) FROM " + TABLE_NAME;
    Long count = jdbcTemplate.queryForObject(sql, Long.class);
    return count != null ? count : 0;
  }
}

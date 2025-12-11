package Group4.Childcare.Repository;

import Group4.Childcare.Model.ChildInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ChildInfoJdbcRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String TABLE_NAME = "child_Info";

    // RowMapper for ChildInfo entity
    private static final RowMapper<ChildInfo> CHILD_INFO_ROW_MAPPER = new RowMapper<ChildInfo>() {
        @Override
        public ChildInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            ChildInfo childInfo = new ChildInfo();
            childInfo.setChildID(UUID.fromString(rs.getString("ChildID")));
            childInfo.setNationalID(rs.getString("NationalID"));
            childInfo.setName(rs.getString("Name"));
            childInfo.setGender(rs.getBoolean("Gender"));

            if (rs.getDate("BirthDate") != null) {
                childInfo.setBirthDate(rs.getDate("BirthDate").toLocalDate());
            }

            if (rs.getString("FamilyInfoID") != null) {
                childInfo.setFamilyInfoID(UUID.fromString(rs.getString("FamilyInfoID")));
            }

            childInfo.setHouseholdAddress(rs.getString("HouseholdAddress"));

            return childInfo;
        }
    };

    // Save method
    public ChildInfo save(ChildInfo childInfo) {
        if (childInfo.getChildID() == null) {
            return insert(childInfo);
        }
      return insert(childInfo);
    }

  public ChildInfo put(ChildInfo childInfo) {
    return update(childInfo);
  }


  // Insert method
    private ChildInfo insert(ChildInfo childInfo) {
        String sql = "INSERT INTO " + TABLE_NAME +
                    " (ChildID, NationalID, Name, Gender, BirthDate, FamilyInfoID, HouseholdAddress) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
            childInfo.getChildID().toString(),
            childInfo.getNationalID(),
            childInfo.getName(),
            childInfo.getGender(),
            childInfo.getBirthDate(),
            childInfo.getFamilyInfoID() != null ? childInfo.getFamilyInfoID().toString() : null,
            childInfo.getHouseholdAddress()
        );

        return childInfo;
    }

    // Update method
    private ChildInfo update(ChildInfo childInfo) {
        String sql = "UPDATE " + TABLE_NAME +
                    " SET NationalID = ?, Name = ?, Gender = ?, BirthDate = ?, FamilyInfoID = ?, HouseholdAddress = ? " +
                    "WHERE ChildID = ?";

        jdbcTemplate.update(sql,
            childInfo.getNationalID(),
            childInfo.getName(),
            childInfo.getGender(),
            childInfo.getBirthDate(),
            childInfo.getFamilyInfoID() != null ? childInfo.getFamilyInfoID().toString() : null,
            childInfo.getHouseholdAddress(),
            childInfo.getChildID().toString()
        );

        return childInfo;
    }

    // Find by ID
    public Optional<ChildInfo> findById(UUID id) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE ChildID = ?";
        try {
            ChildInfo childInfo = jdbcTemplate.queryForObject(sql, CHILD_INFO_ROW_MAPPER, id.toString());
            return Optional.ofNullable(childInfo);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // Find all
    public List<ChildInfo> findAll() {
        String sql = "SELECT * FROM " + TABLE_NAME;
        return jdbcTemplate.query(sql, CHILD_INFO_ROW_MAPPER);
    }

    // Delete by ID
    public void deleteById(UUID id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE ChildID = ?";
        jdbcTemplate.update(sql, id.toString());
    }

    // Delete entity
    public void delete(ChildInfo childInfo) {
        deleteById(childInfo.getChildID());
    }

    // Check if exists by ID
    public boolean existsById(UUID id) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE ChildID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id.toString());
        return count != null && count > 0;
    }

    // Count all
    public long count() {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME;
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    // Find by FamilyInfoID
    public List<ChildInfo> findByFamilyInfoID(UUID familyInfoID) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE FamilyInfoID = ?";
        return jdbcTemplate.query(sql, CHILD_INFO_ROW_MAPPER, familyInfoID.toString());
    }
}

package Group4.Childcare.Repository;

import Group4.Childcare.Model.FamilyInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FamilyInfoJdbcRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String TABLE_NAME = "family_info";

    // RowMapper for FamilyInfo entity
    private static final RowMapper<FamilyInfo> FAMILY_INFO_ROW_MAPPER = new RowMapper<FamilyInfo>() {
        @Override
        public FamilyInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            FamilyInfo familyInfo = new FamilyInfo();
            familyInfo.setFamilyInfoID(UUID.fromString(rs.getString("FamilyInfoID")));
            return familyInfo;
        }
    };

    // Save method
    public FamilyInfo save(FamilyInfo familyInfo) {
        if (familyInfo.getFamilyInfoID() == null) {
            familyInfo.setFamilyInfoID(UUID.randomUUID());
            return insert(familyInfo);
        } else {
            return update(familyInfo);
        }
    }

    // Insert method
    private FamilyInfo insert(FamilyInfo familyInfo) {
        String sql = "INSERT INTO " + TABLE_NAME + " (FamilyInfoID) VALUES (?)";

        jdbcTemplate.update(sql, familyInfo.getFamilyInfoID().toString());

        return familyInfo;
    }

    // Update method
    private FamilyInfo update(FamilyInfo familyInfo) {
        // Since there's only ID field, no actual update needed
        // But keeping the method for consistency
        return familyInfo;
    }

    // Find by ID
    public Optional<FamilyInfo> findById(UUID id) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE FamilyInfoID = ?";
        try {
            FamilyInfo familyInfo = jdbcTemplate.queryForObject(sql, FAMILY_INFO_ROW_MAPPER, id.toString());
            return Optional.ofNullable(familyInfo);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // Find all
    public List<FamilyInfo> findAll() {
        String sql = "SELECT * FROM " + TABLE_NAME;
        return jdbcTemplate.query(sql, FAMILY_INFO_ROW_MAPPER);
    }

    // Delete by ID
    public void deleteById(UUID id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE FamilyInfoID = ?";
        jdbcTemplate.update(sql, id.toString());
    }

    // Delete entity
    public void delete(FamilyInfo familyInfo) {
        deleteById(familyInfo.getFamilyInfoID());
    }

    // Check if exists by ID
    public boolean existsById(UUID id) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE FamilyInfoID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id.toString());
        return count != null && count > 0;
    }

    // Count all
    public long count() {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME;
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }
}

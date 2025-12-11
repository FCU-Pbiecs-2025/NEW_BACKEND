package Group4.Childcare.Repository;

import Group4.Childcare.Model.ParentInfo;
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
public class ParentInfoJdbcRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String TABLE_NAME = "parent_info";

    // RowMapper for ParentInfo entity
    private static final RowMapper<ParentInfo> PARENT_INFO_ROW_MAPPER = new RowMapper<ParentInfo>() {
        @Override
        public ParentInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            ParentInfo parentInfo = new ParentInfo();
            parentInfo.setParentID(UUID.fromString(rs.getString("ParentID")));
            parentInfo.setNationalID(rs.getString("NationalID"));
            parentInfo.setName(rs.getString("Name"));
            parentInfo.setGender(rs.getBoolean("Gender"));
            parentInfo.setRelationship(rs.getString("Relationship"));
            parentInfo.setOccupation(rs.getString("Occupation"));
            parentInfo.setPhoneNumber(rs.getString("PhoneNumber"));
            parentInfo.setHouseholdAddress(rs.getString("HouseholdAddress"));
            parentInfo.setMailingAddress(rs.getString("MailingAddress"));
            parentInfo.setEmail(rs.getString("Email"));

            if (rs.getDate("BirthDate") != null) {
                parentInfo.setBirthDate(rs.getDate("BirthDate").toLocalDate());
            }

            parentInfo.setIsSuspended(rs.getBoolean("IsSuspended"));

            if (rs.getDate("SuspendEnd") != null) {
                parentInfo.setSuspendEnd(rs.getDate("SuspendEnd").toLocalDate());
            }

            if (rs.getString("FamilyInfoID") != null) {
                parentInfo.setFamilyInfoID(UUID.fromString(rs.getString("FamilyInfoID")));
            }

            return parentInfo;
        }
    };

    // Save method
    public ParentInfo save(ParentInfo parentInfo) {
        if (parentInfo.getParentID() == null) {
            parentInfo.setParentID(UUID.randomUUID());
            return insert(parentInfo);
        } else {
            return update(parentInfo);
        }
    }

    // Insert method
    private ParentInfo insert(ParentInfo parentInfo) {
        String sql = "INSERT INTO " + TABLE_NAME +
                    " (ParentID, NationalID, Name, Gender, Relationship, Occupation, PhoneNumber, " +
                    "HouseholdAddress, MailingAddress, Email, BirthDate, IsSuspended, SuspendEnd, FamilyInfoID) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
            parentInfo.getParentID().toString(),
            parentInfo.getNationalID(),
            parentInfo.getName(),
            parentInfo.getGender(),
            parentInfo.getRelationship(),
            parentInfo.getOccupation(),
            parentInfo.getPhoneNumber(),
            parentInfo.getHouseholdAddress(),
            parentInfo.getMailingAddress(),
            parentInfo.getEmail(),
            parentInfo.getBirthDate(),
            parentInfo.getIsSuspended(),
            parentInfo.getSuspendEnd(),
            parentInfo.getFamilyInfoID() != null ? parentInfo.getFamilyInfoID().toString() : null
        );

        return parentInfo;
    }

    // Update method
    private ParentInfo update(ParentInfo parentInfo) {
        String sql = "UPDATE " + TABLE_NAME +
                    " SET NationalID = ?, Name = ?, Gender = ?, Relationship = ?, Occupation = ?, " +
                    "PhoneNumber = ?, HouseholdAddress = ?, MailingAddress = ?, Email = ?, BirthDate = ?, " +
                    "IsSuspended = ?, SuspendEnd = ?, FamilyInfoID = ? WHERE ParentID = ?";

        jdbcTemplate.update(sql,
            parentInfo.getNationalID(),
            parentInfo.getName(),
            parentInfo.getGender(),
            parentInfo.getRelationship(),
            parentInfo.getOccupation(),
            parentInfo.getPhoneNumber(),
            parentInfo.getHouseholdAddress(),
            parentInfo.getMailingAddress(),
            parentInfo.getEmail(),
            parentInfo.getBirthDate(),
            parentInfo.getIsSuspended(),
            parentInfo.getSuspendEnd(),
            parentInfo.getFamilyInfoID() != null ? parentInfo.getFamilyInfoID().toString() : null,
            parentInfo.getParentID().toString()
        );

        return parentInfo;
    }

    // Find by ID
    public Optional<ParentInfo> findById(UUID id) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE ParentID = ?";
        try {
            ParentInfo parentInfo = jdbcTemplate.queryForObject(sql, PARENT_INFO_ROW_MAPPER, id.toString());
            return Optional.ofNullable(parentInfo);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // Find all
    public List<ParentInfo> findAll() {
        String sql = "SELECT * FROM " + TABLE_NAME;
        return jdbcTemplate.query(sql, PARENT_INFO_ROW_MAPPER);
    }

    // Delete by ID
    public void deleteById(UUID id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE ParentID = ?";
        jdbcTemplate.update(sql, id.toString());
    }

    // Delete entity
    public void delete(ParentInfo parentInfo) {
        deleteById(parentInfo.getParentID());
    }

    // Check if exists by ID
    public boolean existsById(UUID id) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE ParentID = ?";
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
    public List<ParentInfo> findByFamilyInfoID(UUID familyInfoID) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE FamilyInfoID = ?";
        return jdbcTemplate.query(sql, PARENT_INFO_ROW_MAPPER, familyInfoID.toString());
    }
}

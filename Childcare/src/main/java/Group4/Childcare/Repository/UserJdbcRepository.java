package Group4.Childcare.Repository;

import Group4.Childcare.Model.Users;
import Group4.Childcare.DTO.UserSummaryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.sql.Date;

@Repository
public class UserJdbcRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FamilyInfoJdbcRepository familyInfoJdbcRepository;

    private static final String TABLE_NAME = "users";

    // RowMapper for Users entity
    private static final RowMapper<Users> USERS_ROW_MAPPER = new RowMapper<Users>() {
        @Override
        public Users mapRow(ResultSet rs, int rowNum) throws SQLException {
            Users user = new Users();
            user.setUserID(UUID.fromString(rs.getString("UserID")));
            user.setAccount(rs.getString("Account"));
            user.setPassword(rs.getString("Password"));
            user.setAccountStatus(rs.getByte("AccountStatus"));
            user.setPermissionType(rs.getByte("PermissionType"));
            user.setName(rs.getString("Name"));
            user.setGender(rs.getBoolean("Gender"));
            user.setPhoneNumber(rs.getString("PhoneNumber"));
            user.setMailingAddress(rs.getString("MailingAddress"));
            user.setEmail(rs.getString("Email"));

            if (rs.getDate("BirthDate") != null) {
                user.setBirthDate(rs.getDate("BirthDate").toLocalDate());
            }

            if (rs.getString("FamilyInfoID") != null) {
                user.setFamilyInfoID(UUID.fromString(rs.getString("FamilyInfoID")));
            }

            if (rs.getString("InstitutionID") != null) {
                user.setInstitutionID(UUID.fromString(rs.getString("InstitutionID")));
            }

            // Map NationalID if present
            String nationalId = rs.getString("NationalID");
            user.setNationalID(nationalId);

            return user;
        }
    };

    // RowMapper for UserSummaryDTO (use fully-qualified to avoid ambiguity)
    private static final RowMapper<Group4.Childcare.DTO.UserSummaryDTO> USER_SUMMARY_ROW_MAPPER = new RowMapper<Group4.Childcare.DTO.UserSummaryDTO>() {
        @Override
        public Group4.Childcare.DTO.UserSummaryDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            Group4.Childcare.DTO.UserSummaryDTO user = new Group4.Childcare.DTO.UserSummaryDTO();
            user.setUserID(UUID.fromString(rs.getString("UserID")));
            user.setAccount(rs.getString("Account"));
            user.setPermissionType(rs.getByte("PermissionType"));
            user.setAccountStatus(rs.getByte("AccountStatus"));
            // 處理可能為 null 的 InstitutionName
            String institutionName = rs.getString("InstitutionName");
            user.setInstitutionName(institutionName);
            return user;
        }
    };

    // Save method (Insert or Update)
    public Users save(Users user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        // 驗證必要欄位 (僅 account 和 password 為必填)
        if (user.getAccount() == null || user.getAccount().trim().isEmpty()) {
            throw new IllegalArgumentException("Account is required");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        // Name 和 NationalID 可為 null，無需驗證

        // If user has no FamilyInfoID, create a new FamilyInfo row and set it
        if (user.getFamilyInfoID() == null) {
            System.out.println("Creating new FamilyInfo for user: " + user.getAccount());
            Group4.Childcare.Model.FamilyInfo newFamily = new Group4.Childcare.Model.FamilyInfo();
            Group4.Childcare.Model.FamilyInfo saved = familyInfoJdbcRepository.save(newFamily);
            user.setFamilyInfoID(saved.getFamilyInfoID());
            System.out.println("FamilyInfo created with ID: " + saved.getFamilyInfoID());
        }

        if (user.getUserID() == null) {
            System.out.println("UserID is null, generating UUID and calling insert...");
            user.setUserID(UUID.randomUUID());
            System.out.println("Generated UserID: " + user.getUserID());
            return insert(user);
        } else {
            System.out.println("UserID already exists: " + user.getUserID() + ", calling update...");
            return update(user);
        }
    }

    // Insert method
    private Users insert(Users user) {
        String sql = "INSERT INTO " + TABLE_NAME +
                    " (UserID, Account, Password, AccountStatus, PermissionType, Name, Gender, " +
                    "PhoneNumber, MailingAddress, Email, BirthDate, FamilyInfoID, InstitutionID, NationalID) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            jdbcTemplate.update(sql,
                user.getUserID().toString(),
                user.getAccount(),
                user.getPassword(),
                user.getAccountStatus() != null ? user.getAccountStatus() : 1,
                user.getPermissionType() != null ? user.getPermissionType() : 1,
                user.getName(),
                user.getGender() != null ? user.getGender() : false,
                user.getPhoneNumber(),
                user.getMailingAddress(),
                user.getEmail(),
                user.getBirthDate() != null ? Date.valueOf(user.getBirthDate()) : null,
                user.getFamilyInfoID() != null ? user.getFamilyInfoID().toString() : null,
                user.getInstitutionID() != null ? user.getInstitutionID().toString() : null,
                user.getNationalID()
            );
            System.out.println("User inserted successfully with ID: " + user.getUserID());
            return user;
        } catch (Exception e) {
            System.err.println("Error inserting user: " + e.getMessage());
            throw new RuntimeException("Failed to insert user", e);
        }
    }

    // Update method
    private Users update(Users user) {
        String sql = "UPDATE " + TABLE_NAME +
                    " SET Account = ?, Password = ?, AccountStatus = ?, PermissionType = ?, Name = ?, " +
                    "Gender = ?, PhoneNumber = ?, MailingAddress = ?, Email = ?, BirthDate = ?, " +
                    "FamilyInfoID = ?, InstitutionID = ?, NationalID = ? WHERE UserID = ?";

        try {
            int rowsUpdated = jdbcTemplate.update(sql,
                user.getAccount(),
                user.getPassword(),
                user.getAccountStatus() != null ? user.getAccountStatus() : 1,
                user.getPermissionType() != null ? user.getPermissionType() : 1,
                user.getName(),
                user.getGender() != null ? user.getGender() : false,
                user.getPhoneNumber(),
                user.getMailingAddress(),
                user.getEmail(),
                user.getBirthDate() != null ? Date.valueOf(user.getBirthDate()) : null,
                user.getFamilyInfoID() != null ? user.getFamilyInfoID().toString() : null,
                user.getInstitutionID() != null ? user.getInstitutionID().toString() : null,
                user.getNationalID(),
                user.getUserID().toString()
            );

            if (rowsUpdated > 0) {
                System.out.println("User updated successfully with ID: " + user.getUserID());
            } else {
                System.err.println("No user found to update with ID: " + user.getUserID());
            }
            return user;
        } catch (Exception e) {
            System.err.println("Error updating user: " + e.getMessage());
            throw new RuntimeException("Failed to update user", e);
        }
    }

    // Find by ID
    public Optional<Users> findById(UUID id) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE UserID = ?";
        try {
            Users user = jdbcTemplate.queryForObject(sql, USERS_ROW_MAPPER, id.toString());
            return Optional.ofNullable(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // Find all
    public List<Users> findAll() {
        String sql = "SELECT * FROM " + TABLE_NAME;
        return jdbcTemplate.query(sql, USERS_ROW_MAPPER);
    }

    // Delete by ID
    public void deleteById(UUID id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE UserID = ?";
        jdbcTemplate.update(sql, id.toString());
    }

    // Delete entity
    public void delete(Users user) {
        deleteById(user.getUserID());
    }

    // Check if exists by ID
    public boolean existsById(UUID id) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE UserID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id.toString());
        return count != null && count > 0;
    }

    // Count all
    public long count() {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME;
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    // Custom method: Find by Account
    public Optional<Users> findByAccount(String account) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE Account = ?";
        try {
            Users user = jdbcTemplate.queryForObject(sql, USERS_ROW_MAPPER, account);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // 取得總筆數用於分頁計算
    public long countTotal() {
        return count();
    }

    // 使用offset分頁查詢，包含機構名稱 - 一次取指定筆數
    public List<Group4.Childcare.DTO.UserSummaryDTO> findWithOffsetAndInstitutionName(int offset, int limit) {
        String sql = "SELECT u.UserID, u.Account, u.PermissionType, u.AccountStatus, i.InstitutionName " +
                     "FROM " + TABLE_NAME + " u LEFT JOIN institutions i ON u.InstitutionID = i.InstitutionID " +
                     "ORDER BY u.UserID OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        return jdbcTemplate.query(sql, USER_SUMMARY_ROW_MAPPER, offset, limit);
    }

    /**
     * 部分更新使用者資料（僅更新姓名、信箱、電話、地址）
     * @param id 使用者ID
     * @param name 姓名（可為 null，表示不更新）
     * @param email 信箱（可為 null，表示不更新）
     * @param phoneNumber 電話（可為 null，表示不更新）
     * @param mailingAddress 地址（可為 null，表示不更新）
     * @return 更新的行數
     */
    public int updateProfile(UUID id, String name, String email, String phoneNumber, String mailingAddress) {
        StringBuilder sql = new StringBuilder("UPDATE " + TABLE_NAME + " SET ");
        List<Object> params = new java.util.ArrayList<>();
        boolean first = true;

        if (name != null) {
            if (!first) sql.append(", ");
            sql.append("Name = ?");
            params.add(name);
            first = false;
        }
        if (email != null) {
            if (!first) sql.append(", ");
            sql.append("Email = ?");
            params.add(email);
            first = false;
        }
        if (phoneNumber != null) {
            if (!first) sql.append(", ");
            sql.append("PhoneNumber = ?");
            params.add(phoneNumber);
            first = false;
        }
        if (mailingAddress != null) {
            if (!first) sql.append(", ");
            sql.append("MailingAddress = ?");
            params.add(mailingAddress);
            first = false;
        }

        // 如果沒有任何欄位需要更新，直接返回
        if (first) {
            System.out.println("No fields to update for user: " + id);
            return 0;
        }

        sql.append(" WHERE UserID = ?");
        params.add(id.toString());

        try {
            int rowsUpdated = jdbcTemplate.update(sql.toString(), params.toArray());
            System.out.println("User profile updated, rows affected: " + rowsUpdated);
            return rowsUpdated;
        } catch (Exception e) {
            System.err.println("Error updating user profile: " + e.getMessage());
            throw new RuntimeException("Failed to update user profile", e);
        }
    }

    /**
     * 根據 Email 查詢使用者
     * @param email 使用者 Email
     * @return 使用者資料
     */
    public Optional<Users> findByEmail(String email) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE Email = ?";
        try {
            Users user = jdbcTemplate.queryForObject(sql, USERS_ROW_MAPPER, email);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 部分更新帳號狀態
     * @param id 使用者ID
     * @param accountStatus 新的帳號狀態
     * @return 更新的行數
     */
    public int updateAccountStatus(UUID id, Integer accountStatus) {
        String sql = "UPDATE " + TABLE_NAME + " SET AccountStatus = ? WHERE UserID = ?";
        try {
            int rowsUpdated = jdbcTemplate.update(sql, accountStatus, id.toString());
            System.out.println("AccountStatus updated for UserID: " + id + ", rows: " + rowsUpdated);
            return rowsUpdated;
        } catch (Exception e) {
            System.err.println("Error updating account status: " + e.getMessage());
            throw new RuntimeException("Failed to update account status", e);
        }
    }
}

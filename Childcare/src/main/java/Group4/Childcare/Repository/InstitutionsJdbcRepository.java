package Group4.Childcare.Repository;

import Group4.Childcare.Model.Institutions;
import Group4.Childcare.DTO.InstitutionSummaryDTO;
import Group4.Childcare.DTO.InstitutionSimpleDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InstitutionsJdbcRepository {

    // 注入 Spring 的 JdbcTemplate 以執行 SQL 操作
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 資料表名稱常數
    private static final String TABLE_NAME = "institutions";

    // 用於將 ResultSet 映射為 Institutions 實體的 RowMapper
    private static final RowMapper<Institutions> INSTITUTIONS_ROW_MAPPER = new RowMapper<Institutions>() {
        @Override
        public Institutions mapRow(ResultSet rs, int rowNum) throws SQLException {
            Institutions institution = new Institutions();
            // 依欄位名稱取得資料並設置到 Institutions 物件
            institution.setInstitutionID(UUID.fromString(rs.getString("InstitutionID")));
            institution.setInstitutionName(rs.getString("InstitutionName"));
            institution.setContactPerson(rs.getString("ContactPerson"));
            institution.setAddress(rs.getString("Address"));
            institution.setPhoneNumber(rs.getString("PhoneNumber"));
            institution.setFax(rs.getString("Fax"));
            institution.setEmail(rs.getString("Email"));
            institution.setRelatedLinks(rs.getString("RelatedLinks"));
            institution.setDescription(rs.getString("Description"));
            institution.setResponsiblePerson(rs.getString("ResponsiblePerson"));
            institution.setImagePath(rs.getString("ImagePath"));
            institution.setCreatedUser(rs.getString("CreatedUser"));

            // 處理 LocalDateTime 型態的欄位
            if (rs.getTimestamp("CreatedTime") != null) {
                institution.setCreatedTime(rs.getTimestamp("CreatedTime").toLocalDateTime());
            }

            institution.setUpdatedUser(rs.getString("UpdatedUser"));

            if (rs.getTimestamp("UpdatedTime") != null) {
                institution.setUpdatedTime(rs.getTimestamp("UpdatedTime").toLocalDateTime());
            }

            institution.setLatitude(rs.getBigDecimal("Latitude"));
            institution.setLongitude(rs.getBigDecimal("Longitude"));
            institution.setInstitutionsType(rs.getBoolean("InstitutionsType"));
            institution.setAccountStatus(rs.getInt("AccountStatus"));

            return institution;
        }
    };

    // 用於將 ResultSet 映射為 InstitutionSummaryDTO 的 RowMapper
    private static final RowMapper<InstitutionSummaryDTO> SUMMARY_ROW_MAPPER = new RowMapper<InstitutionSummaryDTO>() {
        @Override
        public InstitutionSummaryDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            // 只取部分欄位，回傳簡要 DTO
            return new InstitutionSummaryDTO(
                UUID.fromString(rs.getString("InstitutionID")),
                rs.getString("InstitutionName"),
                rs.getString("Address"),
                rs.getString("PhoneNumber")
            );
        }
    };

    /**
     * 儲存機構資料，若 ID 為空則新增，否則更新
     * @param institution 機構物件
     * @return 儲存後的機構物件
     */
    public Institutions save(Institutions institution) {
        if (institution.getInstitutionID() == null) {
            institution.setInstitutionID(UUID.randomUUID());
            return insert(institution);
        } else {
            return update(institution);
        }
    }

    /**
     * 新增機構資料
     * @param institution 機構物件
     * @return 新增後的機構物件
     */
    private Institutions insert(Institutions institution) {
        // 自動設置時間戳
        LocalDateTime now = LocalDateTime.now();
        if (institution.getCreatedTime() == null) {
            institution.setCreatedTime(now);
        }
        if (institution.getUpdatedTime() == null) {
            institution.setUpdatedTime(now);
        }

        String sql = "INSERT INTO " + TABLE_NAME +
                    " (InstitutionID, InstitutionName, ContactPerson, Address, PhoneNumber, Fax, Email, " +
                    "RelatedLinks, Description, ResponsiblePerson, ImagePath, CreatedUser, CreatedTime, " +
                    "UpdatedUser, UpdatedTime, Latitude, Longitude, InstitutionsType, AccountStatus) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // 執行 SQL 新增
        jdbcTemplate.update(sql,
            institution.getInstitutionID().toString(),
            institution.getInstitutionName(),
            institution.getContactPerson(),
            institution.getAddress(),
            institution.getPhoneNumber(),
            institution.getFax(),
            institution.getEmail(),
            institution.getRelatedLinks(),
            institution.getDescription(),
            institution.getResponsiblePerson(),
            institution.getImagePath(),
            institution.getCreatedUser(),
            institution.getCreatedTime(),
            institution.getUpdatedUser(),
            institution.getUpdatedTime(),
            institution.getLatitude(),
            institution.getLongitude(),
            institution.getInstitutionsType(),
            institution.getAccountStatus()
        );

        return institution;
    }

    /**
     * 更新機構資料
     * @param institution 機構物件
     * @return 更新後的機構物件
     */
    private Institutions update(Institutions institution) {
        // 自動設置更新時間
        institution.setUpdatedTime(LocalDateTime.now());

        String sql = "UPDATE " + TABLE_NAME +
                    " SET InstitutionName = ?, ContactPerson = ?, Address = ?, PhoneNumber = ?, " +
                    "Fax = ?, Email = ?, RelatedLinks = ?, Description = ?, ResponsiblePerson = ?, " +
                    "ImagePath = ?, CreatedUser = ?, CreatedTime = ?, UpdatedUser = ?, UpdatedTime = ?, " +
                    "Latitude = ?, Longitude = ?, InstitutionsType = ?, AccountStatus = ? WHERE InstitutionID = ?";

        // 執行 SQL 更新
        jdbcTemplate.update(sql,
            institution.getInstitutionName(),
            institution.getContactPerson(),
            institution.getAddress(),
            institution.getPhoneNumber(),
            institution.getFax(),
            institution.getEmail(),
            institution.getRelatedLinks(),
            institution.getDescription(),
            institution.getResponsiblePerson(),
            institution.getImagePath(),
            institution.getCreatedUser(),
            institution.getCreatedTime(),
            institution.getUpdatedUser(),
            institution.getUpdatedTime(),
            institution.getLatitude(),
            institution.getLongitude(),
            institution.getInstitutionsType(),
            institution.getAccountStatus(),
            institution.getInstitutionID().toString()
        );

        return institution;
    }

    /**
     * 依 ID 查詢機構
     * @param id 機構 ID
     * @return 查詢到的機構 Optional
     */
    public Optional<Institutions> findById(UUID id) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE InstitutionID = ?";
        try {
            Institutions institution = jdbcTemplate.queryForObject(sql, INSTITUTIONS_ROW_MAPPER, id.toString());
            return Optional.ofNullable(institution);
        } catch (Exception e) {
            // 查無資料時回傳空 Optional
            return Optional.empty();
        }
    }

    /**
     * 查詢所有機構
     * @return 機構列表
     */
    public List<Institutions> findAll() {
        String sql = "SELECT * FROM " + TABLE_NAME;
        return jdbcTemplate.query(sql, INSTITUTIONS_ROW_MAPPER);
    }

    /**
     * 查詢所有已啟用的機構 (accountStatus = 1)
     * @return 已啟用的機構列表
     */
    public List<Institutions> findAllActive() {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE AccountStatus = 1";
        return jdbcTemplate.query(sql, INSTITUTIONS_ROW_MAPPER);
    }

    /**
     * 依 ID 刪除機構
     * @param id 機構 ID
     */
    public void deleteById(UUID id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE InstitutionID = ?";
        jdbcTemplate.update(sql, id.toString());
    }

    /**
     * 依實體刪除機構
     * @param institution 機構物件
     */
    public void delete(Institutions institution) {
        deleteById(institution.getInstitutionID());
    }

    /**
     * 檢查指定 ID 是否存在
     * @param id 機構 ID
     * @return 是否存在
     */
    public boolean existsById(UUID id) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE InstitutionID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id.toString());
        return count != null && count > 0;
    }

    /**
     * 計算所有機構數量
     * @return 機構數量
     */
    public long count() {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME;
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    /**
     * 查詢機構簡要資料
     * @return 機構簡要資料列表
     */
    public List<InstitutionSummaryDTO> findSummaryData() {
        String sql = "SELECT InstitutionID, InstitutionName, Address, PhoneNumber FROM " + TABLE_NAME;
        return jdbcTemplate.query(sql, SUMMARY_ROW_MAPPER);
    }

    /**
     * 查詢所有機構的 ID 和名稱（簡化資料）
     * 只返回 AccountStatus = 1（啟用）的機構
     * @return List<InstitutionSimpleDTO>
     */
    public List<InstitutionSimpleDTO> findAllSimple() {
        String sql = "SELECT InstitutionID, InstitutionName FROM " + TABLE_NAME + " WHERE AccountStatus = 1";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            InstitutionSimpleDTO dto = new InstitutionSimpleDTO();
            dto.setInstitutionID(UUID.fromString(rs.getString("InstitutionID")));
            dto.setInstitutionName(rs.getString("InstitutionName"));
            return dto;
        });
    }

    /**
     * 分頁查詢所有機構
     * @param offset 起始位置
     * @param limit 每頁數量
     * @return List<Institutions>
     */
    public List<Institutions> findAllWithPagination(int offset, int limit) {
        String sql = "SELECT * FROM " + TABLE_NAME + " ORDER BY InstitutionID OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        return jdbcTemplate.query(sql, INSTITUTIONS_ROW_MAPPER, offset, limit);
    }

    /**
     * 根據機構 ID 分頁查詢（admin 角色使用）
     * @param institutionID 機構 ID
     * @param offset 起始位置
     * @param limit 每頁數量
     * @return List<Institutions>
     */
    public List<Institutions> findByInstitutionIDWithPagination(UUID institutionID, int offset, int limit) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE InstitutionID = ? ORDER BY InstitutionID OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        return jdbcTemplate.query(sql, INSTITUTIONS_ROW_MAPPER, institutionID.toString(), offset, limit);
    }

    /**
     * 計算指定機構的總數（用於分頁）
     * @param institutionID 機構 ID
     * @return 數量
     */
    public long countByInstitutionID(UUID institutionID) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE InstitutionID = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, institutionID.toString());
        return count != null ? count : 0;
    }

    /**
     * 根據搜尋條件分頁查詢所有機構
     * @param search 搜尋關鍵字
     * @param offset 起始位置
     * @param limit 每頁數量
     * @return List<Institutions>
     */
    public List<Institutions> findAllWithSearchAndPagination(String search, int offset, int limit) {
        String sql = "SELECT * FROM " + TABLE_NAME +
                    " WHERE InstitutionName LIKE ? OR ContactPerson LIKE ? OR PhoneNumber LIKE?" +
                    " ORDER BY InstitutionID OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        String searchPattern = "%" + search + "%";
        return jdbcTemplate.query(sql, INSTITUTIONS_ROW_MAPPER, searchPattern, searchPattern, searchPattern, offset, limit);
    }

    /**
     * 計算搜尋條件下的總數
     * @param search 搜尋關鍵字
     * @return 數量
     */
    public long countAllWithSearch(String search) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME +
                    " WHERE InstitutionName LIKE ? OR ContactPerson LIKE ? OR PhoneNumber LIKE ?";
        String searchPattern = "%" + search + "%";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, searchPattern, searchPattern, searchPattern);
        return count != null ? count : 0;
    }

    /**
     * 根據機構 ID 和搜尋條件分頁查詢（admin 角色使用）
     * @param institutionID 機構 ID
     * @param search 搜尋關鍵字
     * @param offset 起始位置
     * @param limit 每頁數量
     * @return List<Institutions>
     */
    public List<Institutions> findByInstitutionIDWithSearchAndPagination(UUID institutionID, String search, int offset, int limit) {
        String sql = "SELECT * FROM " + TABLE_NAME +
                    " WHERE InstitutionID = ? AND (InstitutionName LIKE ? OR ContactPerson LIKE ? OR PhoneNumber LIKE ?)" +
                    " ORDER BY InstitutionID OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        String searchPattern = "%" + search + "%";
        return jdbcTemplate.query(sql, INSTITUTIONS_ROW_MAPPER, institutionID.toString(), searchPattern, searchPattern, searchPattern, offset, limit);
    }

    /**
     * 計算指定機構和搜尋條件下的總數
     * @param institutionID 機構 ID
     * @param search 搜尋關鍵字
     * @return 數量
     */
    public long countByInstitutionIDWithSearch(UUID institutionID, String search) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME +
                    " WHERE InstitutionID = ? AND (InstitutionName LIKE ? OR ContactPerson LIKE ? OR PhoneNumber LIKE ?)";
        String searchPattern = "%" + search + "%";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, institutionID.toString(), searchPattern, searchPattern, searchPattern);
        return count != null ? count : 0;
    }

    /**
     * 根據機構名稱搜尋條件分頁查詢所有機構（僅搜尋機構名稱）
     * @param name 機構名稱搜尋關鍵字
     * @param offset 起始位置
     * @param limit 每頁數量
     * @return List<Institutions>
     */
    public List<Institutions> findAllWithNameSearchAndPagination(String name, int offset, int limit) {
        String sql = "SELECT * FROM " + TABLE_NAME +
                    " WHERE InstitutionName LIKE ?" +
                    " ORDER BY InstitutionID OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        String searchPattern = "%" + name + "%";
        return jdbcTemplate.query(sql, INSTITUTIONS_ROW_MAPPER, searchPattern, offset, limit);
    }

    /**
     * 計算機構名稱搜尋條件下的總數（僅搜尋機構名稱）
     * @param name 機構名稱搜尋關鍵字
     * @return 數量
     */
    public long countAllWithNameSearch(String name) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME +
                    " WHERE InstitutionName LIKE ?";
        String searchPattern = "%" + name + "%";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, searchPattern);
        return count != null ? count : 0;
    }

    /**
     * 根據機構 ID 和機構名稱搜尋條件分頁查詢（admin 角色使用，僅搜尋機構名稱）
     * @param institutionID 機構 ID
     * @param name 機構名稱搜尋關鍵字
     * @param offset 起始位置
     * @param limit 每頁數量
     * @return List<Institutions>
     */
    public List<Institutions> findByInstitutionIDWithNameSearchAndPagination(UUID institutionID, String name, int offset, int limit) {
        String sql = "SELECT * FROM " + TABLE_NAME +
                    " WHERE InstitutionID = ? AND InstitutionName LIKE ?" +
                    " ORDER BY InstitutionID OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        String searchPattern = "%" + name + "%";
        return jdbcTemplate.query(sql, INSTITUTIONS_ROW_MAPPER, institutionID.toString(), searchPattern, offset, limit);
    }

    /**
     * 計算指定機構和機構名稱搜尋條件下的總數（僅搜尋機構名稱）
     * @param institutionID 機構 ID
     * @param name 機構名稱搜尋關鍵字
     * @return 數量
     */
    public long countByInstitutionIDWithNameSearch(UUID institutionID, String name) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME +
                    " WHERE InstitutionID = ? AND InstitutionName LIKE ?";
        String searchPattern = "%" + name + "%";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, institutionID.toString(), searchPattern);
        return count != null ? count : 0;
    }
}

package Group4.Childcare.Repository;

import Group4.Childcare.Model.Classes;
import Group4.Childcare.DTO.ClassSummaryDTO;
import Group4.Childcare.DTO.ClassNameDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ClassesJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ClassesJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String TABLE_NAME = "classes";

    // RowMapper for Classes entity
    private static final RowMapper<Classes> CLASSES_ROW_MAPPER = (rs, _) -> {
        Classes classes = new Classes();
        classes.setClassID(UUID.fromString(rs.getString("ClassID")));
        classes.setClassName(rs.getString("ClassName"));
        // map TINYINT/INT -> Integer, handling nulls robustly
        Object capObj = rs.getObject("Capacity");
        if (capObj != null) {
            classes.setCapacity(((Number) capObj).intValue());
        } else {
            classes.setCapacity(null);
        }
        Object curObj = rs.getObject("CurrentStudents");
        if (curObj != null) {
            classes.setCurrentStudents(((Number) curObj).intValue());
        } else {
            classes.setCurrentStudents(null);
        }
        Object minAgeObj = rs.getObject("MinAgeDescription");
        if (minAgeObj != null) {
            classes.setMinAgeDescription(((Number) minAgeObj).intValue());
        } else {
            classes.setMinAgeDescription(null);
        }
        Object maxAgeObj = rs.getObject("MaxAgeDescription");
        if (maxAgeObj != null) {
            classes.setMaxAgeDescription(((Number) maxAgeObj).intValue());
        } else {
            classes.setMaxAgeDescription(null);
        }
        classes.setAdditionalInfo(rs.getString("AdditionalInfo"));
        String institutionId = rs.getString("InstitutionID");
        if (institutionId != null) {
            classes.setInstitutionID(UUID.fromString(institutionId));
        }
        return classes;
    };

    // RowMapper for ClassNameDTO (only class name)
    private static final RowMapper<ClassNameDTO> CLASS_NAME_ROW_MAPPER = (rs, _) -> {
        ClassNameDTO dto = new ClassNameDTO();
        String classIdStr = rs.getString("ClassID");
        if (classIdStr != null) {
            dto.setClassID(UUID.fromString(classIdStr));
        }
        dto.setClassName(rs.getString("ClassName"));
        return dto;
    };

    // RowMapper for ClassSummaryDTO (includes institution name via LEFT JOIN)
    private static final RowMapper<ClassSummaryDTO> CLASS_SUMMARY_ROW_MAPPER = (rs, _) -> {
        ClassSummaryDTO dto = new ClassSummaryDTO();
        String classIdStr = rs.getString("ClassID");
        if (classIdStr != null) {
            dto.setClassID(UUID.fromString(classIdStr));
        }
        dto.setClassName(rs.getString("ClassName"));
        Object capObj = rs.getObject("Capacity");
        if (capObj != null) {
            dto.setCapacity(((Number) capObj).intValue());
        } else {
            dto.setCapacity(null);
        }
        Object minAgeObj = rs.getObject("MinAgeDescription");
        if (minAgeObj != null) {
            dto.setMinAgeDescription(String.valueOf(((Number) minAgeObj).intValue()));
        } else {
            dto.setMinAgeDescription(null);
        }
        Object maxAgeObj = rs.getObject("MaxAgeDescription");
        if (maxAgeObj != null) {
            dto.setMaxAgeDescription(String.valueOf(((Number) maxAgeObj).intValue()));
        } else {
            dto.setMaxAgeDescription(null);
        }
        // map CurrentStudents into dto
        Object curStudentsObj = rs.getObject("CurrentStudents");
        if (curStudentsObj != null) {
            dto.setCurrentStudents(((Number) curStudentsObj).intValue());
        } else {
            dto.setCurrentStudents(null);
        }
        dto.setInstitutionName(rs.getString("InstitutionName"));
        String institutionIdStr = rs.getString("InstitutionID");
        if (institutionIdStr != null) {
            dto.setInstitutionID(UUID.fromString(institutionIdStr));
        }
        return dto;
    };

    // Save method
    public Classes save(Classes classes) {
        if (classes.getClassID() == null) {
            classes.setClassID(UUID.randomUUID());
            return insert(classes);
        } else {
            return update(classes);
        }
    }

    // Insert method
    private Classes insert(Classes classes) {
        String sql = "INSERT INTO " + TABLE_NAME +
                    " (ClassID, ClassName, Capacity, CurrentStudents, MinAgeDescription, MaxAgeDescription, AdditionalInfo, InstitutionID) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
            classes.getClassID().toString(),
            classes.getClassName(),
            classes.getCapacity(),
            classes.getCurrentStudents(),
            classes.getMinAgeDescription(),
            classes.getMaxAgeDescription(),
            classes.getAdditionalInfo(),
            classes.getInstitutionID() != null ? classes.getInstitutionID().toString() : null
        );

        return classes;
    }

    // Update method
    private Classes update(Classes classes) {
        String sql = "UPDATE " + TABLE_NAME +
                    " SET ClassName = ?, Capacity = ?, CurrentStudents = ?, MinAgeDescription = ?, " +
                    "MaxAgeDescription = ?, AdditionalInfo = ?, InstitutionID = ? WHERE ClassID = ?";

        jdbcTemplate.update(sql,
            classes.getClassName(),
            classes.getCapacity(),
            classes.getCurrentStudents(),
            classes.getMinAgeDescription(),
            classes.getMaxAgeDescription(),
            classes.getAdditionalInfo(),
            classes.getInstitutionID() != null ? classes.getInstitutionID().toString() : null,
            classes.getClassID().toString()
        );

        return classes;
    }

    // Find by ID
    public Optional<Classes> findById(UUID id) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE ClassID = ?";
        try {
            Classes classes = jdbcTemplate.queryForObject(sql, CLASSES_ROW_MAPPER, id.toString());
            return Optional.ofNullable(classes);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // Find by InstitutionID
    public List<Classes> findByInstitutionId(UUID id) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE InstitutionID = ?";
        return jdbcTemplate.query(sql, CLASSES_ROW_MAPPER, id.toString());
    }

    // Find all
    public List<Classes> findAll() {
        String sql = "SELECT * FROM " + TABLE_NAME;
        return jdbcTemplate.query(sql, CLASSES_ROW_MAPPER);
    }

    // Find all with institution name using LEFT JOIN
    public List<ClassSummaryDTO> findAllWithInstitutionName() {
        String sql = "SELECT c.ClassID, c.ClassName, c.Capacity, c.MinAgeDescription, c.MaxAgeDescription,c.CurrentStudents, i.InstitutionName, i.InstitutionID " +
                     "FROM " + TABLE_NAME + " c LEFT JOIN institutions i ON c.InstitutionID = i.InstitutionID";
        return jdbcTemplate.query(sql, CLASS_SUMMARY_ROW_MAPPER);
    }

    // Delete by ID
    public void deleteById(UUID id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE ClassID = ?";
        jdbcTemplate.update(sql, id.toString());
    }

    // Delete entity
    public void delete(Classes classes) {
        deleteById(classes.getClassID());
    }

    // Check if exists by ID
    public boolean existsById(UUID id) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE ClassID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id.toString());
        return count != null && count > 0;
    }

    // Count all
    public long count() {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME;
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    // 使用offset分頁查詢 - 一次取10筆
    public List<Classes> findWithOffset(int offset, int limit) {
        String sql = "SELECT * FROM " + TABLE_NAME + " ORDER BY ClassID OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        return jdbcTemplate.query(sql, CLASSES_ROW_MAPPER, offset, limit);
    }

    // 取得總筆數用於分頁計算
    public long countTotal() {
        return count();
    }

    // 使用offset分頁查詢，包含機構名稱 - 一次取指定筆數
    public List<ClassSummaryDTO> findWithOffsetAndInstitutionName(int offset, int limit) {
        String sql = "SELECT c.ClassID, c.ClassName, c.Capacity, c.MinAgeDescription, c.MaxAgeDescription,c.CurrentStudents, i.InstitutionName, i.InstitutionID " +
                     "FROM " + TABLE_NAME + " c LEFT JOIN institutions i ON c.InstitutionID = i.InstitutionID " +
                     "ORDER BY c.ClassID OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        return jdbcTemplate.query(sql, CLASS_SUMMARY_ROW_MAPPER, offset, limit);
    }

    /**
     * 使用offset分頁查詢，根據機構 ID 過濾（admin 角色使用）
     * @param offset 分頁偏移量
     * @param limit 每頁筆數
     * @param institutionID 機構 ID
     * @return 班級列表
     */
    public List<ClassSummaryDTO> findWithOffsetAndInstitutionNameByInstitutionID(int offset, int limit, UUID institutionID) {
        String sql = "SELECT c.ClassID, c.ClassName, c.Capacity, c.MinAgeDescription, c.MaxAgeDescription,c.CurrentStudents, i.InstitutionName, i.InstitutionID " +
                     "FROM " + TABLE_NAME + " c LEFT JOIN institutions i ON c.InstitutionID = i.InstitutionID " +
                     "WHERE c.InstitutionID = ? " +
                     "ORDER BY c.ClassID OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        return jdbcTemplate.query(sql, CLASS_SUMMARY_ROW_MAPPER, institutionID.toString(), offset, limit);
    }

    /**
     * 取得指定機構的班級總數（用於分頁計算）
     * @param institutionID 機構 ID
     * @return 班級總數
     */
    public long countByInstitutionID(UUID institutionID) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE InstitutionID = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, institutionID.toString());
        return count != null ? count : 0;
    }

    /**
     * 依機構名稱模糊搜尋，返回機構及其班級資料
     * @param institutionName 機構名稱關鍵字
     * @return List<Map<String, Object>>，包含機構資料和班級列表
     */
    public List<java.util.Map<String, Object>> findInstitutionsWithClassesByName(String institutionName) {
        String sql = """
            SELECT
                i.InstitutionID, i.InstitutionName, i.ContactPerson, i.Address,
                i.PhoneNumber, i.Fax, i.Email, i.RelatedLinks, i.Description,
                i.ResponsiblePerson, i.ImagePath, i.CreatedUser, i.CreatedTime,
                i.UpdatedUser, i.UpdatedTime, i.Latitude, i.Longitude,
                c.ClassID, c.ClassName, c.Capacity, c.CurrentStudents,
                c.MinAgeDescription, c.MaxAgeDescription, c.AdditionalInfo
            FROM institutions i
            LEFT JOIN classes c ON i.InstitutionID = c.InstitutionID
            WHERE i.InstitutionName LIKE ?
            ORDER BY i.InstitutionName, c.ClassName
            """;

        return jdbcTemplate.query(sql, (rs, _) -> {
            java.util.Map<String, Object> result = new java.util.HashMap<>();

            // Institution data
            java.util.Map<String, Object> institution = new java.util.HashMap<>();
            institution.put("institutionID", rs.getString("InstitutionID"));
            institution.put("institutionName", rs.getString("InstitutionName"));
            institution.put("contactPerson", rs.getString("ContactPerson"));
            institution.put("address", rs.getString("Address"));
            institution.put("phoneNumber", rs.getString("PhoneNumber"));
            institution.put("fax", rs.getString("Fax"));
            institution.put("email", rs.getString("Email"));
            institution.put("relatedLinks", rs.getString("RelatedLinks"));
            institution.put("description", rs.getString("Description"));
            institution.put("responsiblePerson", rs.getString("ResponsiblePerson"));
            institution.put("imagePath", rs.getString("ImagePath"));
            institution.put("createdUser", rs.getString("CreatedUser"));
            institution.put("createdTime", rs.getTimestamp("CreatedTime"));
            institution.put("updatedUser", rs.getString("UpdatedUser"));
            institution.put("updatedTime", rs.getTimestamp("UpdatedTime"));
            institution.put("latitude", rs.getBigDecimal("Latitude"));
            institution.put("longitude", rs.getBigDecimal("Longitude"));
            result.put("institution", institution);

            // Class data (may be null if no classes)
            if (rs.getString("ClassID") != null) {
                java.util.Map<String, Object> classData = new java.util.HashMap<>();
                classData.put("classID", rs.getString("ClassID"));
                classData.put("className", rs.getString("ClassName"));
                classData.put("capacity", rs.getObject("Capacity"));
                classData.put("currentStudents", rs.getObject("CurrentStudents"));
                classData.put("minAgeDescription", rs.getObject("MinAgeDescription"));
                classData.put("maxAgeDescription", rs.getObject("MaxAgeDescription"));
                classData.put("additionalInfo", rs.getString("AdditionalInfo"));
                result.put("class", classData);
            }

            return result;
        }, "%" + institutionName + "%");
    }

    /**
     * 依機構名稱模糊搜尋班級，回傳 ClassSummaryDTO 列表
     * @param institutionName 機構名稱關鍵字
     * @return List<ClassSummaryDTO>
     */
    public List<ClassSummaryDTO> findClassesByInstitutionName(String institutionName) {
        String sql = "SELECT c.ClassID, c.ClassName, c.Capacity, c.MinAgeDescription, c.CurrentStudents, i.InstitutionName, i.InstitutionID " +
                     "FROM " + TABLE_NAME + " c LEFT JOIN institutions i ON c.InstitutionID = i.InstitutionID " +
                     "WHERE i.InstitutionName LIKE ? " +
                     "ORDER BY i.InstitutionName, c.ClassName";
        return jdbcTemplate.query(sql, CLASS_SUMMARY_ROW_MAPPER, "%" + institutionName + "%");
    }

    /**
     * 根據機構UUID查詢該機構的所有班級名稱
     * @param institutionId 機構UUID
     * @return List<ClassNameDTO> 班級名稱列表
     */
    public List<ClassNameDTO> findClassNamesByInstitutionId(UUID institutionId) {
        String sql = "SELECT c.ClassID, c.ClassName FROM " + TABLE_NAME + " c " +
                     "WHERE c.InstitutionID = ? " +
                     "ORDER BY c.ClassName";
        return jdbcTemplate.query(sql, CLASS_NAME_ROW_MAPPER, institutionId.toString());
    }
}

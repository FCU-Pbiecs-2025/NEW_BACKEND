package Group4.Childcare.Repository;

import Group4.Childcare.Model.Applications;
import Group4.Childcare.DTO.ApplicationSummaryWithDetailsDTO;
import Group4.Childcare.DTO.ApplicationCaseDTO;
import Group4.Childcare.DTO.ApplicationParticipantDTO;
import Group4.Childcare.DTO.CaseOffsetListDTO;
import Group4.Childcare.DTO.CaseEditUpdateDTO;
import Group4.Childcare.DTO.UserSimpleDTO;
import Group4.Childcare.DTO.UserApplicationDetailsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;

@Repository
public class ApplicationsJdbcRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String TABLE_NAME = "applications";

    private static final RowMapper<Applications> APPLICATIONS_ROW_MAPPER = (rs, rowNum) -> {
        Applications application = new Applications();
        application.setApplicationID(UUID.fromString(rs.getString("ApplicationID")));
        if (rs.getDate("ApplicationDate") != null) {
            application.setApplicationDate(rs.getDate("ApplicationDate").toLocalDate());
        }
        if (rs.getString("InstitutionID") != null) {
            application.setInstitutionID(UUID.fromString(rs.getString("InstitutionID")));
        }
        if (rs.getString("UserID") != null) {
            application.setUserID(UUID.fromString(rs.getString("UserID")));
        }
        application.setIdentityType(rs.getByte("IdentityType"));
        application.setAttachmentPath(rs.getString("AttachmentPath"));
        return application;
    };

    private static final RowMapper<ApplicationSummaryWithDetailsDTO> DETAILS_ROW_MAPPER = (rs, rowNum) -> {
        ApplicationSummaryWithDetailsDTO dto = new ApplicationSummaryWithDetailsDTO();
        dto.setApplicationID(UUID.fromString(rs.getString("ApplicationID")));
        if (rs.getDate("ApplicationDate") != null) {
            dto.setApplicationDate(rs.getDate("ApplicationDate").toLocalDate());
        }
        dto.setName(rs.getString("Name"));
        dto.setInstitutionName(rs.getString("InstitutionName"));
        dto.setInstitutionID(rs.getString("InstitutionID"));
        dto.setNationalID(rs.getString("NationalID"));
        dto.setStatus(rs.getString("Status"));
        try { dto.setParticipantType(rs.getString("ParticipantType")); } catch (Exception ex) { dto.setParticipantType(null); }
        try { dto.setPName(rs.getString("PName")); } catch (Exception ex) { }
        try { Object caseNum = rs.getObject("CaseNumber"); if (caseNum != null) dto.setCaseNumber(((Number) caseNum).longValue()); } catch (Exception ex) { dto.setCaseNumber(null); }
        return dto;
    };

    public Applications save(Applications application) {
        // 如果 ApplicationID 為 null，生成新的 UUID 並執行 INSERT
        if (application.getApplicationID() == null) {
            application.setApplicationID(UUID.randomUUID());
            System.out.println("🆕 [save] ApplicationID is null, generating new UUID and executing INSERT");
            return insert(application);
        }

        // 如果 ApplicationID 不為 null，檢查資料庫中是否已存在
        boolean exists = existsById(application.getApplicationID());
        System.out.println("🔍 [save] ApplicationID = " + application.getApplicationID() + ", exists in DB = " + exists);

        if (exists) {
            System.out.println("📝 [save] Record exists, executing UPDATE");
            return update(application);
        } else {
            System.out.println("🆕 [save] Record not exists, executing INSERT");
            return insert(application);
        }
    }

    private Applications insert(Applications application) {
        String sql = "INSERT INTO " + TABLE_NAME +
                " (ApplicationID, ApplicationDate, CaseNumber, InstitutionID, UserID, IdentityType, AttachmentPath, AttachmentPath1, AttachmentPath2, AttachmentPath3) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        System.out.println("🔵 ApplicationsJdbcRepository.insert() - Executing SQL:");
        System.out.println("  SQL: " + sql);
        System.out.println("  ApplicationID: " + application.getApplicationID());
        System.out.println("  ApplicationDate: " + application.getApplicationDate());
        System.out.println("  CaseNumber: " + application.getCaseNumber());
        System.out.println("  InstitutionID: " + application.getInstitutionID());
        System.out.println("  UserID: " + application.getUserID());
        System.out.println("  IdentityType: " + application.getIdentityType());
        System.out.println("  AttachmentPath: " + application.getAttachmentPath());

        try {
            int rows = jdbcTemplate.update(sql,
                    application.getApplicationID().toString(),
                    application.getApplicationDate(),
                    application.getCaseNumber(),
                    application.getInstitutionID() != null ? application.getInstitutionID().toString() : null,
                    application.getUserID() != null ? application.getUserID().toString() : null,
                    application.getIdentityType(),
                    application.getAttachmentPath(),
                    application.getAttachmentPath1(),
                    application.getAttachmentPath2(),
                    application.getAttachmentPath3()
            );

            System.out.println("✅ INSERT completed! Rows affected: " + rows);

            // 立即驗證資料是否真的存入
            String verifySql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE ApplicationID = ?";
            Integer count = jdbcTemplate.queryForObject(verifySql, Integer.class, application.getApplicationID().toString());
            System.out.println("🔎 VERIFICATION: Record exists in DB = " + (count != null && count > 0) + " (count=" + count + ")");

            if (count == null || count == 0) {
                System.err.println("⚠️ WARNING: INSERT reported success but record NOT FOUND in database!");
            }

        } catch (Exception e) {
            System.err.println("❌ INSERT FAILED with exception: " + e.getMessage());
            e.printStackTrace();
            throw e; // 重新拋出異常
        }

        return application;
    }

    private Applications update(Applications application) {
        // 先從資料庫取得原始資料，避免遺失欄位
        Optional<Applications> originalOpt = findById(application.getApplicationID());
        if (originalOpt.isEmpty()) {
            throw new RuntimeException("Application not found for update: " + application.getApplicationID());
        }
        Applications original = originalOpt.get();

        // 只更新非 null 的欄位，保留原始資料中的其他欄位
        String sql = "UPDATE " + TABLE_NAME + " SET ";
        List<Object> params = new ArrayList<>();
        List<String> setClauses = new ArrayList<>();

        if (application.getApplicationDate() != null) {
            setClauses.add("ApplicationDate = ?");
            params.add(application.getApplicationDate());
        }

        if (application.getCaseNumber() != null) {
            setClauses.add("CaseNumber = ?");
            params.add(application.getCaseNumber());
        }

        if (application.getInstitutionID() != null) {
            setClauses.add("InstitutionID = ?");
            params.add(application.getInstitutionID().toString());
        }

        if (application.getUserID() != null) {
            setClauses.add("UserID = ?");
            params.add(application.getUserID().toString());
        }

        if (application.getIdentityType() != null) {
            setClauses.add("IdentityType = ?");
            params.add(application.getIdentityType());
        }

        if (application.getAttachmentPath() != null) {
            setClauses.add("AttachmentPath = ?");
            params.add(application.getAttachmentPath());
        }

        if (application.getAttachmentPath1() != null) {
            setClauses.add("AttachmentPath1 = ?");
            params.add(application.getAttachmentPath1());
        }

        if (application.getAttachmentPath2() != null) {
            setClauses.add("AttachmentPath2 = ?");
            params.add(application.getAttachmentPath2());
        }

        if (application.getAttachmentPath3() != null) {
            setClauses.add("AttachmentPath3 = ?");
            params.add(application.getAttachmentPath3());
        }

        if (setClauses.isEmpty()) {
            System.out.println("⚠️ No fields to update for ApplicationID: " + application.getApplicationID());
            return original; // 沒有欄位需要更新，返回原始資料
        }

        sql += String.join(", ", setClauses) + " WHERE ApplicationID = ?";
        params.add(application.getApplicationID().toString());

        System.out.println("🔵 ApplicationsJdbcRepository.update() - Executing SQL:");
        System.out.println("  ApplicationID: " + application.getApplicationID());
        System.out.println("  SQL: " + sql);
        System.out.println("  Params: " + params);

        int rows = jdbcTemplate.update(sql, params.toArray());

        System.out.println("✅ UPDATE completed! Rows affected: " + rows);

        // 返回更新後的資料（合併原始資料和更新內容）
        if (application.getApplicationDate() == null) application.setApplicationDate(original.getApplicationDate());
        if (application.getCaseNumber() == null) application.setCaseNumber(original.getCaseNumber());
        if (application.getInstitutionID() == null) application.setInstitutionID(original.getInstitutionID());
        if (application.getUserID() == null) application.setUserID(original.getUserID());
        if (application.getIdentityType() == null) application.setIdentityType(original.getIdentityType());
        if (application.getAttachmentPath() == null) application.setAttachmentPath(original.getAttachmentPath());
        if (application.getAttachmentPath1() == null) application.setAttachmentPath1(original.getAttachmentPath1());
        if (application.getAttachmentPath2() == null) application.setAttachmentPath2(original.getAttachmentPath2());
        if (application.getAttachmentPath3() == null) application.setAttachmentPath3(original.getAttachmentPath3());

        return application;
    }

    public Optional<Applications> findById(UUID id) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE ApplicationID = ?";
        try {
            Applications application = jdbcTemplate.queryForObject(sql, APPLICATIONS_ROW_MAPPER, id.toString());
            return Optional.ofNullable(application);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<Applications> getApplicationById(UUID id) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE ApplicationID = ?";
        List<Applications> result = jdbcTemplate.query(sql, APPLICATIONS_ROW_MAPPER, id.toString());
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.getFirst());
    }

    public Optional<ApplicationSummaryWithDetailsDTO> findApplicationSummaryWithDetailsById(UUID id) {
        String sql = "SELECT a.ApplicationID, a.ApplicationDate, u.Name AS name, i.InstitutionName AS institutionName, ap.Status, ap.ParticipantType, ap.NationalID, ap.Name AS Cname " +
                "FROM applications a " +
                "LEFT JOIN users u ON a.UserID = u.UserID  " +
                "LEFT JOIN application_participants ap ON a.ApplicationID = ap.ApplicationID " +
                "LEFT JOIN institutions i ON a.InstitutionID = i.InstitutionID " +
                "WHERE a.ApplicationID = ?";
        return jdbcTemplate.query(sql, new Object[]{id}, (rs, rowNum) -> {
            ApplicationSummaryWithDetailsDTO dto = new ApplicationSummaryWithDetailsDTO();
            dto.setApplicationID(UUID.fromString(rs.getString("ApplicationID")));
            dto.setApplicationDate(rs.getDate("ApplicationDate").toLocalDate());
            dto.setName(rs.getString("name"));
            dto.setInstitutionName(rs.getString("institutionName"));
            dto.setNationalID(rs.getString("NationalID"));
            dto.setParticipantType(rs.getString("ParticipantType"));
            return dto;
        }).stream().findFirst();
    }

    public Optional<ApplicationCaseDTO> findApplicationCaseById(UUID id, String nationalID, UUID participantID) {
        String sql = "SELECT a.ApplicationID, a.ApplicationDate, i.InstitutionName, " +
                // 加入身分別欄位
                "a.IdentityType, " +
                // 加入案件流水號欄位
                "a.CaseNumber, " +
                // 從 applications 帶出四個附件欄位
                "a.AttachmentPath, a.AttachmentPath1, a.AttachmentPath2, a.AttachmentPath3, " +
                "ap.ParticipantID, ap.ParticipantType, ap.NationalID, ap.Name, ap.Gender, ap.RelationShip, ap.Occupation, " +
                "ap.PhoneNumber, ap.HouseholdAddress, ap.MailingAddress, ap.Email, ap.BirthDate, " +
                "ap.IsSuspended, ap.SuspendEnd, ap.CurrentOrder, ap.Status, ap.Reason, ap.ClassID, ap.ReviewDate " +
                "FROM applications a " +
                "LEFT JOIN institutions i ON a.InstitutionID = i.InstitutionID " +
                "LEFT JOIN application_participants ap ON a.ApplicationID = ap.ApplicationID " +
                "WHERE a.ApplicationID = ? " +
                "ORDER BY ap.CurrentOrder";

        java.util.Map<String, Object> resultMap = new java.util.HashMap<>();

        jdbcTemplate.query(sql, (rs, rowNum) -> {
            if (!resultMap.containsKey("header")) {
                ApplicationCaseDTO dto = new ApplicationCaseDTO();
                if (rs.getString("ApplicationID") != null) {
                    dto.applicationId = UUID.fromString(rs.getString("ApplicationID"));
                }
                if (rs.getDate("ApplicationDate") != null) {
                    dto.applicationDate = rs.getDate("ApplicationDate").toLocalDate();
                }
                dto.institutionName = rs.getString("InstitutionName");
                dto.parents = new java.util.ArrayList<>();
                dto.children = new java.util.ArrayList<>();

                // 映射身分別 IdentityType -> ApplicationCaseDTO.identityType
                try {
                    Object idTypeObj = rs.getObject("IdentityType");
                    if (idTypeObj != null) {
                        dto.identityType = ((Number) idTypeObj).byteValue();
                    }
                } catch (Exception ex) {
                    dto.identityType = null;
                }

                // 映射案件流水號 CaseNumber -> ApplicationCaseDTO.caseNumber
                try {
                    Object caseNumberObj = rs.getObject("CaseNumber");
                    if (caseNumberObj != null) {
                        dto.caseNumber = ((Number) caseNumberObj).longValue();
                    }
                } catch (Exception ex) {
                    dto.caseNumber = null;
                }

                // 映射附件欄位
                try { dto.attachmentPath = rs.getString("AttachmentPath"); } catch (Exception ex) { dto.attachmentPath = null; }
                try { dto.attachmentPath1 = rs.getString("AttachmentPath1"); } catch (Exception ex) { dto.attachmentPath1 = null; }
                try { dto.attachmentPath2 = rs.getString("AttachmentPath2"); } catch (Exception ex) { dto.attachmentPath2 = null; }
                try { dto.attachmentPath3 = rs.getString("AttachmentPath3"); } catch (Exception ex) { dto.attachmentPath3 = null; }

                resultMap.put("header", dto);
            }

            if (rs.getString("NationalID") != null) {
                ApplicationParticipantDTO p = new ApplicationParticipantDTO();
                Object ptObj = null;
                try { ptObj = rs.getObject("ParticipantType"); } catch (Exception ex) { ptObj = null; }
                Boolean isParent = null;
                if (ptObj instanceof Boolean) {
                    try { isParent = rs.getBoolean("ParticipantType"); } catch (Exception ex) { isParent = null; }
                } else if (ptObj != null) {
                    try { int v = rs.getInt("ParticipantType"); isParent = (v == 2); } catch (Exception ex) { isParent = null; }
                }

                // 設置 ParticipantID
                try {
                    String participantIdStr = rs.getString("ParticipantID");
                    if (participantIdStr != null && !participantIdStr.isEmpty()) {
                        p.participantID = UUID.fromString(participantIdStr);
                    }
                } catch (Exception ex) { p.participantID = null; }

                p.participantType = (isParent != null && isParent) ? "家長" : "幼兒";
                p.nationalID = rs.getString("NationalID");
                p.name = rs.getString("Name");
                try { Object gObj = rs.getObject("Gender"); if (gObj != null) { p.gender = rs.getBoolean("Gender") ? "男" : "女"; } else { p.gender = null; } } catch (Exception ex) { p.gender = null; }
                p.relationShip = rs.getString("RelationShip");
                p.occupation = rs.getString("Occupation");
                p.phoneNumber = rs.getString("PhoneNumber");
                p.householdAddress = rs.getString("HouseholdAddress");
                p.mailingAddress = rs.getString("MailingAddress");
                p.email = rs.getString("Email");
                if (rs.getDate("BirthDate") != null) p.birthDate = rs.getDate("BirthDate").toString();
                try { Object susp = rs.getObject("IsSuspended"); if (susp != null) p.isSuspended = rs.getBoolean("IsSuspended"); else p.isSuspended = null; } catch (Exception ex) { p.isSuspended = null; }
                if (rs.getDate("SuspendEnd") != null) p.suspendEnd = rs.getDate("SuspendEnd").toString();
                try { Object co = rs.getObject("CurrentOrder"); if (co != null) p.currentOrder = rs.getInt("CurrentOrder"); else p.currentOrder = null; } catch (Exception ex) { p.currentOrder = null; }
                p.status = rs.getString("Status");
                p.reason = rs.getString("Reason");
                p.classID = rs.getString("ClassID");
                if (rs.getTimestamp("ReviewDate") != null) {
                    p.reviewDate = rs.getTimestamp("ReviewDate").toLocalDateTime();
                }

                ApplicationCaseDTO dto = (ApplicationCaseDTO) resultMap.get("header");

                if (isParent != null && isParent) {
                    // Parents 總是添加，不受過濾限制
                    dto.parents.add(p);
                } else {
                    // Children 只在符合 participantID 過濾或沒有過濾時才添加
                    boolean shouldAdd = (participantID == null || participantID.toString().isEmpty() || participantID.equals(p.participantID));
                    if (shouldAdd) {
                        dto.children.add(p);
                    }
                }
            }

            return null;
        }, id.toString());

        if (!resultMap.containsKey("header")) {
            return Optional.empty();
        }

        ApplicationCaseDTO dto = (ApplicationCaseDTO) resultMap.get("header");
        return Optional.of(dto);
    }

    public void updateApplicationCase(UUID id, ApplicationCaseDTO dto) {
        if (dto != null) {
            java.util.List<ApplicationParticipantDTO> participants = new java.util.ArrayList<>();
            if (dto.parents != null) participants.addAll(dto.parents);
            if (dto.children != null) participants.addAll(dto.children);

            for (ApplicationParticipantDTO p : participants) {
                if (p == null || p.nationalID == null || p.nationalID.isEmpty()) continue;

                Boolean participantType = null;
                if (p.participantType != null) participantType = "家長".equals(p.participantType) || "1".equals(p.participantType);
                Boolean gender = null;
                if (p.gender != null) gender = "男".equals(p.gender);
                java.sql.Date birthDate = null;
                if (p.birthDate != null && !p.birthDate.isEmpty()) {
                    try { birthDate = java.sql.Date.valueOf(java.time.LocalDate.parse(p.birthDate)); } catch (Exception ex) { birthDate = null; }
                }
                java.sql.Date suspendEnd = null;
                if (p.suspendEnd != null && !p.suspendEnd.isEmpty()) {
                    try { suspendEnd = java.sql.Date.valueOf(java.time.LocalDate.parse(p.suspendEnd)); } catch (Exception ex) { suspendEnd = null; }
                }
                java.util.UUID classUUID = null;
                if (p.classID != null && !p.classID.isEmpty()) {
                    try { classUUID = java.util.UUID.fromString(p.classID); } catch (Exception ex) { classUUID = null; }
                }
                java.sql.Timestamp reviewTs = null;
                if (p.reviewDate != null) reviewTs = java.sql.Timestamp.valueOf(p.reviewDate);

                // 檢查是否為幼兒
                boolean isChild = (participantType != null && participantType == false) ||
                                  (p.participantType != null && ("幼兒".equals(p.participantType) || "0".equals(p.participantType)));

                // 如果是幼兒且 CurrentOrder 為 null，自動設置排序號
                if (isChild && p.currentOrder == null) {
                    System.out.println("🔵 [updateApplicationCase] 幼兒 CurrentOrder 為 null，自動設置排序號");
                    System.out.println("  NationalID: " + p.nationalID);
                    System.out.println("  Name: " + p.name);
                    System.out.println("  Status: " + p.status);

                    // 獲取該申請案件的InstitutionID
                    String getInstitutionIdSql = "SELECT InstitutionID FROM applications WHERE ApplicationID = ?";
                    java.util.UUID institutionId = null;
                    try {
                        String institutionIdStr = jdbcTemplate.queryForObject(getInstitutionIdSql, String.class, id.toString());
                        if (institutionIdStr != null) {
                            institutionId = java.util.UUID.fromString(institutionIdStr);
                            System.out.println("  InstitutionID: " + institutionId);
                        }
                    } catch (Exception ex) {
                        System.out.println("  ❌ 無法獲取 InstitutionID: " + ex.getMessage());
                    }

                    if (institutionId != null) {
                        // 查詢同機構的最大CurrentOrder值
                        String getMaxOrderSql =
                            "SELECT MAX(ap.CurrentOrder) FROM application_participants ap " +
                            "INNER JOIN applications a ON ap.ApplicationID = a.ApplicationID " +
                            "WHERE a.InstitutionID = ? " +
                            "AND ap.CurrentOrder IS NOT NULL " +
                            "AND ap.ParticipantType = 0";  // 只檢查幼兒記錄

                        Integer maxOrder = null;
                        try {
                            maxOrder = jdbcTemplate.queryForObject(getMaxOrderSql, Integer.class, institutionId.toString());
                            System.out.println("  查詢到的最大 CurrentOrder: " + maxOrder);
                        } catch (Exception ex) {
                            System.out.println("  查詢最大 CurrentOrder 失敗 (可能沒有記錄): " + ex.getMessage());
                        }

                        // 如果沒有任何CurrentOrder，則設置為1；否則設置為最大值+1
                        if (maxOrder == null) {
                            p.currentOrder = 1;
                            System.out.println("  ✅ 設置 CurrentOrder = 1 (該機構第一個排序號)");
                        } else {
                            p.currentOrder = maxOrder + 1;
                            System.out.println("  ✅ 設置 CurrentOrder = " + p.currentOrder + " (最大值 + 1)");
                        }
                    } else {
                        System.out.println("  ⚠️ InstitutionID 為 null，無法設置 CurrentOrder");
                    }
                } else {
                    if (isChild && p.currentOrder != null) {
                        System.out.println("🔵 [updateApplicationCase] 幼兒已有 CurrentOrder: " + p.currentOrder + " (NationalID: " + p.nationalID + ")");
                    }
                }

                String updateSql = "UPDATE application_participants SET ParticipantType = ?, Name = ?, Gender = ?, RelationShip = ?, Occupation = ?, PhoneNumber = ?, HouseholdAddress = ?, MailingAddress = ?, Email = ?, BirthDate = ?, IsSuspended = ?, SuspendEnd = ?, CurrentOrder = ?, Status = ?, Reason = ?, ClassID = ?, ReviewDate = ? WHERE ApplicationID = ? AND NationalID = ?";
                int updated = 0;
                try {
                    updated = jdbcTemplate.update(updateSql,
                            participantType, p.name, gender, p.relationShip, p.occupation, p.phoneNumber, p.householdAddress,
                            p.mailingAddress, p.email, birthDate, p.isSuspended, suspendEnd, p.currentOrder, p.status, p.reason,
                            classUUID != null ? classUUID.toString() : null, reviewTs, id.toString(), p.nationalID
                    );
                } catch (Exception ex) {
                    updated = 0;
                }

                if (updated == 0) {
                    String insertSql = "INSERT INTO application_participants (ApplicationID, ParticipantType, NationalID, Name, Gender, RelationShip, Occupation, PhoneNumber, HouseholdAddress, MailingAddress, Email, BirthDate, IsSuspended, SuspendEnd, CurrentOrder, Status, Reason, ClassID, ReviewDate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try {
                        jdbcTemplate.update(insertSql,
                                id.toString(), participantType, p.nationalID, p.name, gender, p.relationShip, p.occupation,
                                p.phoneNumber, p.householdAddress, p.mailingAddress, p.email, birthDate, p.isSuspended, suspendEnd,
                                p.currentOrder, p.status, p.reason, classUUID != null ? classUUID.toString() : null, reviewTs
                        );
                    } catch (Exception ex) {
                        // ignore
                    }
                }
            }
        }
    }

    public void deleteById(UUID id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE ApplicationID = ?";
        jdbcTemplate.update(sql, id.toString());
    }

    public void delete(Applications application) {
        deleteById(application.getApplicationID());
    }

    public boolean existsById(UUID id) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE ApplicationID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id.toString());
        return count != null && count > 0;
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME;
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    public List<Applications> findAll() {
        String sql = "SELECT * FROM " + TABLE_NAME;
        return jdbcTemplate.query(sql, APPLICATIONS_ROW_MAPPER);
    }

    public List<Group4.Childcare.DTO.ApplicationSummaryDTO> findSummaryByUserID(UUID userID) {
        String sql = "SELECT a.ApplicationID, a.ApplicationDate, ap.Status, ap.Reason " +
                "FROM applications a " +
                "LEFT JOIN application_participants ap ON a.ApplicationID = ap.ApplicationID " +
                "WHERE a.UserID = ? AND ap.ParticipantType = 0";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Group4.Childcare.DTO.ApplicationSummaryDTO dto = new Group4.Childcare.DTO.ApplicationSummaryDTO();
            dto.setApplicationID(UUID.fromString(rs.getString("ApplicationID")));
            if (rs.getDate("ApplicationDate") != null) {
                dto.setApplicationDate(rs.getDate("ApplicationDate").toLocalDate());
            }
            dto.setStatus(rs.getString("Status"));
            dto.setReason(rs.getString("Reason"));
            return dto;
        }, userID.toString());
    }

    public List<ApplicationSummaryWithDetailsDTO> findSummariesWithOffset(int offset, int limit) {
        // 以 application_participants 為主體，每一列代表一個幼兒參與者
        String sql =
                "SELECT " +
                        "  a.ApplicationID, " +
                        "  a.ApplicationDate, " +
                        "  a.CaseNumber, " +
                        "  u.Name AS Name, " +                    // 申請人名稱（users.Name）\n" +
                        "  i.InstitutionName AS InstitutionName, " +
                        "  ap.Status, " +
                        "  a.InstitutionID, " +
                        "  ap.NationalID AS NationalID, " +       // 幼兒身分證\n" +
                        "  ap.ParticipantType AS ParticipantType, " +
                        "  ap.Name AS PName " +                   // 幼兒姓名\n" +
                        "FROM application_participants ap " +
                        "JOIN applications a ON ap.ApplicationID = a.ApplicationID " +
                        "LEFT JOIN users u ON a.UserID = u.UserID " +
                        "LEFT JOIN institutions i ON a.InstitutionID = i.InstitutionID " +
                        "WHERE ap.ParticipantType = 0 AND ap.Status in ('審核中','需要補件','已退件')  " +         // 只取幼兒\n" +
                        "ORDER BY a.ApplicationDate DESC, ap.CurrentOrder ASC " +
                        "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try {
            return jdbcTemplate.query(sql, DETAILS_ROW_MAPPER, offset, limit);
        } catch (Exception e) {
            throw new RuntimeException("Failed to query application summaries: " + e.getMessage(), e);
        }
    }

    public List<ApplicationSummaryWithDetailsDTO> searchApplications(String institutionID, String institutionName, String caseNumber, String nationalID) {
        StringBuilder sql = new StringBuilder(
                "SELECT a.ApplicationID, a.ApplicationDate, a.CaseNumber, " +
                        "       u.Name AS Name, i.InstitutionName, a.InstitutionID, " +
                        "       ap.Status, ap.ParticipantType, ap.NationalID, ap.Name AS PName " +
                        "FROM applications a " +
                        "LEFT JOIN users u ON a.UserID = u.UserID " +
                        "LEFT JOIN institutions i ON a.InstitutionID = i.InstitutionID " +
                        "LEFT JOIN application_participants ap ON a.ApplicationID = ap.ApplicationID " +
                        "WHERE ap.ParticipantType = 0  and ap.Status in ('審核中','需要補件','已退件') "  // 只取幼兒
        );

        java.util.List<Object> params = new java.util.ArrayList<>();

        // 機構過濾（優先使用 institutionID）
        if (institutionID != null && !institutionID.trim().isEmpty()) {
            sql.append("AND a.InstitutionID = ? ");
            params.add(institutionID.trim());
        } else if (institutionName != null && !institutionName.trim().isEmpty()) {
            sql.append("AND i.InstitutionName = ? ");
            params.add(institutionName.trim());
        }

        // 流水案號過濾
        if (caseNumber != null && !caseNumber.trim().isEmpty()) {
            sql.append("AND a.CaseNumber = ? ");
            try {
                // CaseNumber 為數字型（BIGINT），嘗試轉換
                params.add(Long.parseLong(caseNumber.trim()));
            } catch (Exception ex) {
                // 若後端為字串型，則直接帶字串
                params.add(caseNumber.trim());
            }
        }

        // 幼兒身分證過濾（application_participants.NationalID，僅限 ParticipantType=0）
        if (nationalID != null && !nationalID.trim().isEmpty()) {
            sql.append("AND ap.NationalID = ? ");
            params.add(nationalID.trim());
        }

        sql.append("ORDER BY a.ApplicationDate DESC, a.CaseNumber ASC");

        return jdbcTemplate.query(sql.toString(), params.toArray(), DETAILS_ROW_MAPPER);
    }

    public List<ApplicationSummaryWithDetailsDTO> revokesearchApplications(String institutionID, String institutionName, String caseNumber, String nationalID) {
        StringBuilder sql = new StringBuilder(
                "SELECT c.ApplicationID, c.CancellationDate, c.CaseNumber, c.NationalID " +
                        "       u.Name AS Name, i.InstitutionName, a.InstitutionID, " +
                        "       ap.Status, ap.reason, ap.Name AS PName " +
                        "FROM cancellation c " +
                        "LEFT JOIN users u ON a.UserID = u.UserID " +
                        "LEFT JOIN institutions i ON a.InstitutionID = i.InstitutionID " +
                        "LEFT JOIN application_participants ap ON a.ApplicationID = ap.ApplicationID " +
                        "WHERE ap.ParticipantType = 0  and ap.Status in ('撤銷申請審核中') "  // 只取幼兒
        );

        java.util.List<Object> params = new java.util.ArrayList<>();

        // 機構過濾（優先使用 institutionID）
        if (institutionID != null && !institutionID.trim().isEmpty()) {
            sql.append("AND a.InstitutionID = ? ");
            params.add(institutionID.trim());
        } else if (institutionName != null && !institutionName.trim().isEmpty()) {
            sql.append("AND i.InstitutionName = ? ");
            params.add(institutionName.trim());
        }

        // 流水案號過濾
        if (caseNumber != null && !caseNumber.trim().isEmpty()) {
            sql.append("AND a.CaseNumber = ? ");
            try {
                // CaseNumber 為數字型（BIGINT），嘗試轉換
                params.add(Long.parseLong(caseNumber.trim()));
            } catch (Exception ex) {
                // 若後端為字串型，則直接帶字串
                params.add(caseNumber.trim());
            }
        }

        // 幼兒身分證過濾（application_participants.NationalID，僅限 ParticipantType=0）
        if (nationalID != null && !nationalID.trim().isEmpty()) {
            sql.append("AND c.NationalID = ? ");
            params.add(nationalID.trim());
        }

        sql.append("ORDER BY c.CancellationDate DESC, a.CaseNumber ASC");

        return jdbcTemplate.query(sql.toString(), params.toArray(), DETAILS_ROW_MAPPER);
    }

    public void updateParticipantStatusReason(UUID id, String nationalID, String status, String reason, java.time.LocalDateTime reviewDate) {
        System.out.println("🔵 [updateParticipantStatusReason] ApplicationID: " + id + ", NationalID: " + nationalID + ", Status: " + status);

        // 先查詢該參與者的當前狀態和 CurrentOrder（只查詢幼兒記錄）
        String getCurrentInfoSql = "SELECT Status, CurrentOrder, ParticipantType FROM application_participants WHERE ApplicationID = ? AND NationalID = ? AND ParticipantType = 0";
        String oldStatus = null;
        Integer oldCurrentOrder = null;
        Boolean isChild = null;

        try {
            // 使用 queryForList 並取第一筆，避免多筆記錄錯誤
            java.util.List<java.util.Map<String, Object>> results = jdbcTemplate.queryForList(getCurrentInfoSql, id.toString(), nationalID);

            if (!results.isEmpty()) {
                java.util.Map<String, Object> currentInfo = results.get(0);
                oldStatus = (String) currentInfo.get("Status");
                Object currentOrderObj = currentInfo.get("CurrentOrder");
                if (currentOrderObj != null) {
                    oldCurrentOrder = ((Number) currentOrderObj).intValue();
                }
                Object participantTypeObj = currentInfo.get("ParticipantType");
                if (participantTypeObj != null) {
                    if (participantTypeObj instanceof Boolean) {
                        isChild = !(Boolean) participantTypeObj;
                    } else if (participantTypeObj instanceof Number) {
                        isChild = ((Number) participantTypeObj).intValue() == 0;
                    }
                }

                System.out.println("  查詢當前資料 - 舊狀態: " + oldStatus + ", 舊CurrentOrder: " + oldCurrentOrder + ", isChild: " + isChild);

                if (results.size() > 1) {
                    System.out.println("  ⚠️ 警告: 找到 " + results.size() + " 筆記錄，使用第一筆");
                }
            } else {
                System.out.println("  ⚠️ 警告: 找不到幼兒記錄 (ParticipantType = 0)");
            }
        } catch (Exception ex) {
            System.out.println("  ❌ 無法查詢當前資料: " + ex.getMessage());
            ex.printStackTrace();
        }

        Integer currentOrder = null;

        // 情況1: 如果狀態改為"候補中"，設置新的 CurrentOrder
        if (status != null && "候補中".equals(status)) {
            System.out.println("  狀態改為候補中，開始處理 CurrentOrder");

            if (isChild != null && isChild) {
                // 獲取該申請案件的InstitutionID
                String getInstitutionIdSql = "SELECT InstitutionID FROM applications WHERE ApplicationID = ?";
                java.util.UUID institutionId = null;
                try {
                    String institutionIdStr = jdbcTemplate.queryForObject(getInstitutionIdSql, String.class, id.toString());
                    if (institutionIdStr != null) {
                        institutionId = java.util.UUID.fromString(institutionIdStr);
                        System.out.println("  InstitutionID: " + institutionId);
                    }
                } catch (Exception ex) {
                    System.out.println("  ❌ 無法獲取 InstitutionID: " + ex.getMessage());
                }

                if (institutionId != null) {
                    // 查詢同機構的最大CurrentOrder值
                    String getMaxOrderSql =
                        "SELECT MAX(ap.CurrentOrder) FROM application_participants ap " +
                        "INNER JOIN applications a ON ap.ApplicationID = a.ApplicationID " +
                        "WHERE a.InstitutionID = ? " +
                        "AND ap.CurrentOrder IS NOT NULL " +
                        "AND ap.ParticipantType = 0";

                    Integer maxOrder = null;
                    try {
                        maxOrder = jdbcTemplate.queryForObject(getMaxOrderSql, Integer.class, institutionId.toString());
                        System.out.println("  查詢到的最大 CurrentOrder: " + maxOrder);
                    } catch (Exception ex) {
                        System.out.println("  查詢最大 CurrentOrder 失敗 (可能沒有記錄): " + ex.getMessage());
                    }

                    if (maxOrder == null) {
                        currentOrder = 1;
                        System.out.println("  ✅ 設置 CurrentOrder = 1 (首個候補)");
                    } else {
                        currentOrder = maxOrder + 1;
                        System.out.println("  ✅ 設置 CurrentOrder = " + currentOrder + " (maxOrder + 1)");
                    }
                }
            } else {
                System.out.println("  ⚠️ 非幼兒記錄，不設置 CurrentOrder");
            }
        }
        // 情況2: 如果原本是"候補中"且有 CurrentOrder，現在改為其他狀態（如已錄取），需要遞補後面的 CurrentOrder
        else if (oldStatus != null && "候補中".equals(oldStatus) && oldCurrentOrder != null && isChild != null && isChild) {
            System.out.println("  從候補中變更為其他狀態，需要遞補後面的 CurrentOrder");

            // 獲取該申請案件的InstitutionID
            String getInstitutionIdSql = "SELECT InstitutionID FROM applications WHERE ApplicationID = ?";
            java.util.UUID institutionId = null;
            try {
                String institutionIdStr = jdbcTemplate.queryForObject(getInstitutionIdSql, String.class, id.toString());
                if (institutionIdStr != null) {
                    institutionId = java.util.UUID.fromString(institutionIdStr);
                    System.out.println("  InstitutionID: " + institutionId);
                }
            } catch (Exception ex) {
                System.out.println("  ❌ 無法獲取 InstitutionID: " + ex.getMessage());
            }

            if (institutionId != null) {
                // 將該個案後面所有的 CurrentOrder 減 1
                String updateFollowingOrdersSql =
                    "UPDATE application_participants " +
                    "SET CurrentOrder = CurrentOrder - 1 " +
                    "WHERE ParticipantType = 0 " +
                    "AND CurrentOrder > ? " +
                    "AND ApplicationID IN ( " +
                    "  SELECT ApplicationID FROM applications WHERE InstitutionID = ? " +
                    ")";

                try {
                    int updatedCount = jdbcTemplate.update(updateFollowingOrdersSql, oldCurrentOrder, institutionId.toString());
                    System.out.println("  ✅ 遞補完成：將 CurrentOrder > " + oldCurrentOrder + " 的 " + updatedCount + " 筆記錄減 1");
                } catch (Exception ex) {
                    System.out.println("  ❌ 遞補 CurrentOrder 失敗: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }

            // 將當前個案的 CurrentOrder 設為 null（因為已不在候補狀態）
            currentOrder = null;
            System.out.println("  將當前個案的 CurrentOrder 設為 null");
        }

        // 只更新幼兒記錄（ParticipantType = 0）
        String sql = "UPDATE application_participants SET Status = ?, Reason = ?, ReviewDate = ?, CurrentOrder = ? WHERE ApplicationID = ? AND NationalID = ? AND ParticipantType = 0";
        java.sql.Timestamp ts = null;
        if (reviewDate != null) ts = java.sql.Timestamp.valueOf(reviewDate);
        try {
            int rowsAffected = jdbcTemplate.update(sql, status, reason, ts, currentOrder, id.toString(), nationalID);
            System.out.println("✅ [updateParticipantStatusReason] 更新完成，影響行數: " + rowsAffected + ", 新 CurrentOrder: " + currentOrder);

            if (rowsAffected == 0) {
                System.out.println("  ⚠️ 警告: 沒有更新任何記錄，請檢查 ApplicationID、NationalID 和 ParticipantType");
            } else if (rowsAffected > 1) {
                System.out.println("  ⚠️ 警告: 更新了 " + rowsAffected + " 筆記錄，可能有重複資料");
            }
        } catch (Exception ex) {
            System.out.println("❌ [updateParticipantStatusReason] 更新失敗: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * 查詢案件列表（根據幼兒 ParticipantType=0）
     * @param offset 分頁起始位置
     * @param limit 每頁筆數
     * @param status 審核狀態（可選）
     * @param institutionId 機構ID（可選）
     * @return List<CaseOffsetListDTO>
     */
    public List<CaseOffsetListDTO> findCaseListWithOffset(int offset, int limit, String status, UUID institutionId,
                                                          UUID applicationId, UUID classId, String childNationalId,
                                                          Long caseNumber, String identityType) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
                .append("ap.ParticipantID, ")
                .append("a.CaseNumber, ")
                .append("a.ApplicationDate, ")
                .append("i.InstitutionName, ")
                .append("ap.NationalID, ")
                .append("ap.Name, ")
                .append("ap.BirthDate, ")
                .append("ap.CurrentOrder, ")
                .append("ap.Status, ")
                .append("c.ClassName, ")
                .append("u.NationalID AS ApplicantNationalID, ")
                .append("u.Name AS ApplicantNationalName, ")
                .append("a.IdentityType ")
                .append("FROM applications a ")
                .append("LEFT JOIN institutions i ON a.InstitutionID = i.InstitutionID ")
                .append("LEFT JOIN application_participants ap ON a.ApplicationID = ap.ApplicationID ")
                .append("LEFT JOIN classes c ON ap.ClassID = c.ClassID ")
                .append("LEFT JOIN users u ON a.UserID = u.UserID ")
                .append("WHERE ap.ParticipantType = 0 "); // 0 = 幼兒

        java.util.List<Object> params = new java.util.ArrayList<>();

        if (status != null && !status.isEmpty()) {
            sql.append("AND ap.Status = ? ");
            params.add(status);
        }

        if (institutionId != null) {
            sql.append("AND a.InstitutionID = ? ");
            params.add(institutionId.toString());
        }

        if (applicationId != null) {
            sql.append("AND a.ApplicationID = ? ");
            params.add(applicationId.toString());
        }

        if (classId != null) {
            sql.append("AND ap.ClassID = ? ");
            params.add(classId.toString());
        }

        if (childNationalId != null && !childNationalId.isEmpty()) {
            sql.append("AND ap.NationalID = ? ");
            params.add(childNationalId);
        }

        if (caseNumber != null) {
            sql.append("AND a.CaseNumber = ? ");
            params.add(caseNumber);
        }

        if (identityType != null && !identityType.isEmpty()) {
            sql.append("AND a.IdentityType = ? ");
            params.add(identityType);
        }

        sql.append("ORDER BY a.ApplicationDate DESC ")
                .append("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        // 將分頁參數加入預備語句參數列表
        params.add(offset);
        params.add(limit);

        RowMapper<CaseOffsetListDTO> rowMapper = (rs, rowNum) -> {
            CaseOffsetListDTO dto = new CaseOffsetListDTO();

            // 設置 ParticipantID (application_participants.ParticipantID)
            try {
                Object participantIdObj = rs.getObject("ParticipantID");
                if (participantIdObj instanceof java.util.UUID) {
                    dto.setParticipantID((java.util.UUID) participantIdObj);
                } else if (participantIdObj != null) {
                    dto.setParticipantID(java.util.UUID.fromString(rs.getString("ParticipantID")));
                }
            } catch (Exception e) {
                dto.setParticipantID(null);
            }

            dto.setCaseNumber(rs.getLong("CaseNumber"));
            if (rs.getDate("ApplicationDate") != null) {
                dto.setApplicationDate(rs.getDate("ApplicationDate").toLocalDate());
            }
            dto.setInstitutionName(rs.getString("InstitutionName"));
            dto.setChildNationalId(rs.getString("NationalID"));
            dto.setChildName(rs.getString("Name"));
            if (rs.getDate("BirthDate") != null) {
                dto.setChildBirthDate(rs.getDate("BirthDate").toLocalDate());
            }
            Object orderObj = rs.getObject("CurrentOrder");
            if (orderObj != null) {
                dto.setCurrentOrder(((Number) orderObj).intValue());
            }
            dto.setReviewStatus(rs.getString("Status"));

            // 新增的欄位
            dto.setClassName(rs.getString("ClassName"));
            dto.setApplicantNationalId(rs.getString("ApplicantNationalID"));
            dto.setApplicantNationalName(rs.getString("ApplicantNationalName"));
            Object identityTypeObj = rs.getObject("IdentityType");
            if (identityTypeObj != null) {
                dto.setIdentityType(identityTypeObj.toString());
            }
            dto.setCaseStatus(rs.getString("Status")); // 案件狀態使用 ap.Status

            return dto;
        };

        return jdbcTemplate.query(sql.toString(), params.toArray(), rowMapper);
    }

    /**
     * 查詢案件列表的總筆數
     * @param status 審核狀態（可選）
     * @param institutionId 機構ID（可選）
     * @param applicationId 案件ID（可選）
     * @param classId 班級ID（可選）
     * @param childNationalId 幼兒身分證字號（可選）
     * @param caseNumber 案件流水號（可選）
     * @param identityType 身分別（可選）
     * @return 總筆數
     */
    public long countCaseList(String status, UUID institutionId, UUID applicationId, UUID classId,
                              String childNationalId, Long caseNumber, String identityType) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(DISTINCT a.ApplicationID) ")
                .append("FROM applications a ")
                .append("LEFT JOIN application_participants ap ON a.ApplicationID = ap.ApplicationID ")
                .append("LEFT JOIN classes c ON ap.ClassID = c.ClassID ")
                .append("LEFT JOIN users u ON a.UserID = u.UserID ")
                .append("WHERE ap.ParticipantType = 0 "); // 0 = 幼兒

        java.util.List<Object> params = new java.util.ArrayList<>();

        if (status != null && !status.isEmpty()) {
            sql.append("AND ap.Status = ? ");
            params.add(status);
        }

        if (institutionId != null) {
            sql.append("AND a.InstitutionID = ? ");
            params.add(institutionId.toString());
        }

        if (applicationId != null) {
            sql.append("AND a.ApplicationID = ? ");
            params.add(applicationId.toString());
        }

        if (classId != null) {
            sql.append("AND ap.ClassID = ? ");
            params.add(classId.toString());
        }

        if (childNationalId != null && !childNationalId.isEmpty()) {
            sql.append("AND ap.NationalID = ? ");
            params.add(childNationalId);
        }

        if (caseNumber != null) {
            sql.append("AND a.CaseNumber = ? ");
            params.add(caseNumber);
        }

        if (identityType != null && !identityType.isEmpty()) {
            sql.append("AND a.IdentityType = ? ");
            params.add(identityType);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), params.toArray(), Long.class);
        return count != null ? count : 0;
    }

    /**
     * 根據身分證字號查詢參與者及其完整的案件信息
     * @param nationalID 身分證字號
     * @return CaseEditUpdateDTO 列表
     */
    public List<CaseEditUpdateDTO> findByNationalID(String nationalID) {
        String sql = "SELECT DISTINCT " +
                "a.CaseNumber, " +
                "a.ApplicationDate, " +
                "a.IdentityType, " +
                "a.InstitutionID, " +
                "a.ApplicationID, " +
                "a.UserID, " +
                "i.InstitutionName, " +
                "ap.Status, " +
                "ap.CurrentOrder, " +
                "ap.ReviewDate, " +
                "c.ClassName, " +
                "ap.ParticipantID, " +
                "ap.Name, " +
                "ap.Gender, " +
                "ap.BirthDate, " +
                "ap.MailingAddress, " +
                "ap.Email, " +
                "ap.PhoneNumber, " +
                "ap.NationalID AS ParticipantNationalID, " +
                // 新增：從 users 表帶出申請人資料
                "u.UserID AS ApplicantUserID, " +
                "u.Name AS ApplicantName, " +
                "u.Gender AS ApplicantGender, " +
                "u.BirthDate AS ApplicantBirthDate, " +
                "u.MailingAddress AS ApplicantMailingAddress, " +
                "u.Email AS ApplicantEmail, " +
                "u.PhoneNumber AS ApplicantPhoneNumber, " +
                "u.NationalID AS ApplicantNationalID, " +
                // 新增：從 applications 表帶出四個附件欄位
                "a.AttachmentPath, " +
                "a.AttachmentPath1, " +
                "a.AttachmentPath2, " +
                "a.AttachmentPath3 " +
                "FROM applications a " +
                "LEFT JOIN institutions i ON a.InstitutionID = i.InstitutionID " +
                "LEFT JOIN application_participants ap ON a.ApplicationID = ap.ApplicationID " +
                "LEFT JOIN classes c ON ap.ClassID = c.ClassID " +
                "LEFT JOIN users u ON a.UserID = u.UserID " +
                "WHERE ap.NationalID = ? ";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            CaseEditUpdateDTO dto = new CaseEditUpdateDTO();

            // 來自 applications 表
            try { Object caseNum = rs.getObject("CaseNumber"); if (caseNum != null) dto.setCaseNumber(((Number) caseNum).longValue()); } catch (Exception ex) { }

            if (rs.getDate("ApplicationDate") != null) {
                dto.setApplyDate(rs.getDate("ApplicationDate").toLocalDate());
            }

            try { Object identityType = rs.getObject("IdentityType"); if (identityType != null) dto.setIdentityType(((Number) identityType).intValue()); } catch (Exception ex) { }

            try {
                String institutionIdStr = rs.getString("InstitutionID");
                if (institutionIdStr != null && !institutionIdStr.isEmpty()) {
                    dto.setInstitutionId(java.util.UUID.fromString(institutionIdStr));
                }
            } catch (Exception ex) { }

            try {
                String appIdStr = rs.getString("ApplicationID");
                if (appIdStr != null && !appIdStr.isEmpty()) {
                    dto.setApplicationID(java.util.UUID.fromString(appIdStr));
                }
            } catch (Exception ex) { }

            // 來自 institutions 表
            dto.setInstitutionName(rs.getString("InstitutionName"));

            // 來自 application_participants 表
            try {
                String participantIdStr = rs.getString("ParticipantID");
                if (participantIdStr != null && !participantIdStr.isEmpty()) {
                    dto.setParticipantID(java.util.UUID.fromString(participantIdStr));
                }
            } catch (Exception ex) { }

            try { Object currentOrder = rs.getObject("CurrentOrder"); if (currentOrder != null) dto.setCurrentOrder(((Number) currentOrder).intValue()); } catch (Exception ex) { }

            if (rs.getTimestamp("ReviewDate") != null) {
                dto.setReviewDate(rs.getTimestamp("ReviewDate").toLocalDateTime());
            }

            // 來自 classes 表
            dto.setSelectedClass(rs.getString("ClassName"));

            // 創建並設置申請人信息 (UserSimpleDTO) - 來自 users 表
            UserSimpleDTO userDTO = new UserSimpleDTO();
            try {
                String applicantUserIdStr = rs.getString("ApplicantUserID");
                if (applicantUserIdStr != null && !applicantUserIdStr.isEmpty()) {
                    userDTO.setUserID(applicantUserIdStr);
                }
            } catch (Exception ex) { }

            userDTO.setName(rs.getString("ApplicantName"));

            try {
                Boolean genderVal = rs.getBoolean("ApplicantGender");
                if (!rs.wasNull()) {
                    userDTO.setGender(genderVal ? "M" : "F");
                }
            } catch (Exception ex) { }

            if (rs.getDate("ApplicantBirthDate") != null) {
                userDTO.setBirthDate(rs.getDate("ApplicantBirthDate").toString());
            }

            userDTO.setMailingAddress(rs.getString("ApplicantMailingAddress"));
            userDTO.setEmail(rs.getString("ApplicantEmail"));
            userDTO.setPhoneNumber(rs.getString("ApplicantPhoneNumber"));
            userDTO.setNationalID(rs.getString("ApplicantNationalID"));

            dto.setUser(userDTO);

            // 來自 applications 表：附件欄位
            try { dto.setAttachmentPath(rs.getString("AttachmentPath")); } catch (Exception ex) { /* ignore */ }
            try { dto.setAttachmentPath1(rs.getString("AttachmentPath1")); } catch (Exception ex) { /* ignore */ }
            try { dto.setAttachmentPath2(rs.getString("AttachmentPath2")); } catch (Exception ex) { /* ignore */ }
            try { dto.setAttachmentPath3(rs.getString("AttachmentPath3")); } catch (Exception ex) { /* ignore */ }

            return dto;
        }, nationalID);
    }

    /**
     * 根據 UserID 查詢使用者申請詳細資料
     * 包含 applications、application_participants、cancellation、user 表的聯合查詢
     * @param userID 使用者ID
     * @return 包含申請詳細資料的清單
     */
    public List<UserApplicationDetailsDTO> findUserApplicationDetails(UUID userID) {
        String sql = "SELECT " +
                "a.ApplicationID, " +
                "a.ApplicationDate, " +
                "a.InstitutionID, " +
                "a.CaseNumber, " +
                "i.InstitutionName, " +
                "ap.Name as childname, " +
                "ap.BirthDate, " +
                "ap.Status, " +
                "ap.CurrentOrder, " +
                "ap.NationalID as childNationalID, " +
                "ap.Reason, " +
                "c.CancellationID, " +
                "u.Name as username " +
                "FROM applications a " +
                "LEFT JOIN application_participants ap ON a.ApplicationID = ap.ApplicationID " +
                "LEFT JOIN users u ON a.UserID = u.UserID " +
                "LEFT JOIN cancellation c ON  c.ApplicationID = a.ApplicationID " +
                "LEFT JOIN  institutions i ON  i.InstitutionID = a.InstitutionID " +
                "WHERE a.UserID = ?  and ap.ParticipantType=0" +
                "ORDER BY a.ApplicationDate DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UserApplicationDetailsDTO dto = new UserApplicationDetailsDTO();
            dto.setApplicationID(UUID.fromString(rs.getString("ApplicationID")));
            dto.setApplicationDate(rs.getDate("ApplicationDate").toLocalDate());
            dto.setInstitutionID(UUID.fromString(rs.getString("InstitutionID")));
            dto.setInstitutionName(rs.getString("InstitutionName"));
            dto.setChildname(rs.getString("childname"));
            if (rs.getDate("BirthDate") != null) {
                dto.setBirthDate(rs.getDate("BirthDate").toLocalDate());
            }
            dto.setCaseNumber(rs.getString("CaseNumber"));
            dto.setStatus(rs.getString("Status"));
            dto.setCurrentOrder(rs.getInt("CurrentOrder"));
            dto.setChildNationalID(rs.getString("childNationalID"));
            dto.setReason(rs.getString("Reason"));
            dto.setCancellationID(rs.getString("CancellationID") != null ? UUID.fromString(rs.getString("CancellationID")) : null);
            dto.setUsername(rs.getString("username"));
            return dto;
        }, userID.toString());
    }

    /**
     * 根據 ParticipantID 查詢案件詳情
     * @param participantID 參與者ID（幼兒）
     * @return CaseEditUpdateDTO（包含該幼兒的案件信息）或 Optional.empty()
     */
    public Optional<CaseEditUpdateDTO> findCaseByParticipantId(UUID participantID) {
        String sql = "SELECT DISTINCT " +
                "a.CaseNumber, " +
                "a.ApplicationDate, " +
                "a.IdentityType, " +
                "a.InstitutionID, " +
                "a.ApplicationID, " +
                "i.InstitutionName, " +
                "ap.Status, " +
                "ap.CurrentOrder, " +
                "ap.ReviewDate, " +
                "c.ClassName, " +
                "ap.ParticipantID, " +
                "ap.Name, " +
                "ap.Gender, " +
                "ap.BirthDate, " +
                "ap.MailingAddress, " +
                "ap.Email, " +
                "ap.PhoneNumber, " +
                "ap.NationalID AS ParticipantNationalID, " +
                "a.AttachmentPath, " +
                "a.AttachmentPath1, " +
                "a.AttachmentPath2, " +
                "a.AttachmentPath3, " +
                "u.UserID AS ApplicantUserID, " +
                "u.Name AS ApplicantName, " +
                "u.Gender AS ApplicantGender, " +
                "u.BirthDate AS ApplicantBirthDate, " +
                "u.MailingAddress AS ApplicantMailingAddress, " +
                "u.Email AS ApplicantEmail, " +
                "u.PhoneNumber AS ApplicantPhoneNumber, " +
                "u.NationalID AS ApplicantNationalID " +
                "FROM applications a " +
                "LEFT JOIN institutions i ON a.InstitutionID = i.InstitutionID " +
                "LEFT JOIN application_participants ap ON a.ApplicationID = ap.ApplicationID " +
                "LEFT JOIN classes c ON ap.ClassID = c.ClassID " +
                "LEFT JOIN users u ON a.UserID = u.UserID " +
                "WHERE ap.ParticipantID = ? ";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            CaseEditUpdateDTO dto = new CaseEditUpdateDTO();

            try { Object caseNum = rs.getObject("CaseNumber"); if (caseNum != null) dto.setCaseNumber(((Number) caseNum).longValue()); } catch (Exception ex) { }

            if (rs.getDate("ApplicationDate") != null) {
                dto.setApplyDate(rs.getDate("ApplicationDate").toLocalDate());
            }

            try { Object identityType = rs.getObject("IdentityType"); if (identityType != null) dto.setIdentityType(((Number) identityType).intValue()); } catch (Exception ex) { }

            try {
                String institutionIdStr = rs.getString("InstitutionID");
                if (institutionIdStr != null && !institutionIdStr.isEmpty()) {
                    dto.setInstitutionId(java.util.UUID.fromString(institutionIdStr));
                }
            } catch (Exception ex) { }

            try {
                String appIdStr = rs.getString("ApplicationID");
                if (appIdStr != null && !appIdStr.isEmpty()) {
                    dto.setApplicationID(java.util.UUID.fromString(appIdStr));
                }
            } catch (Exception ex) { }

            // 設置 ParticipantID
            try {
                String participantIdStr = rs.getString("ParticipantID");
                if (participantIdStr != null && !participantIdStr.isEmpty()) {
                    dto.setParticipantID(java.util.UUID.fromString(participantIdStr));
                }
            } catch (Exception ex) { }

            dto.setInstitutionName(rs.getString("InstitutionName"));

            try { Object currentOrder = rs.getObject("CurrentOrder"); if (currentOrder != null) dto.setCurrentOrder(((Number) currentOrder).intValue()); } catch (Exception ex) { }

            if (rs.getTimestamp("ReviewDate") != null) {
                dto.setReviewDate(rs.getTimestamp("ReviewDate").toLocalDateTime());
            }

            dto.setSelectedClass(rs.getString("ClassName"));

            // 創建並設置申請人信息 (UserSimpleDTO) - 來自 users 表
            UserSimpleDTO userDTO = new UserSimpleDTO();
            try {
                String applicantUserIdStr = rs.getString("ApplicantUserID");
                if (applicantUserIdStr != null && !applicantUserIdStr.isEmpty()) {
                    userDTO.setUserID(applicantUserIdStr);
                }
            } catch (Exception ex) { }

            userDTO.setName(rs.getString("ApplicantName"));

            try {
                Boolean genderVal = rs.getBoolean("ApplicantGender");
                if (!rs.wasNull()) {
                    userDTO.setGender(genderVal ? "M" : "F");
                }
            } catch (Exception ex) { }

            if (rs.getDate("ApplicantBirthDate") != null) {
                userDTO.setBirthDate(rs.getDate("ApplicantBirthDate").toString());
            }

            userDTO.setMailingAddress(rs.getString("ApplicantMailingAddress"));
            userDTO.setEmail(rs.getString("ApplicantEmail"));
            userDTO.setPhoneNumber(rs.getString("ApplicantPhoneNumber"));
            userDTO.setNationalID(rs.getString("ApplicantNationalID"));

            dto.setUser(userDTO);

            // 來自 applications 表：附件欄位
            try { dto.setAttachmentPath(rs.getString("AttachmentPath")); } catch (Exception ex) { }
            try { dto.setAttachmentPath1(rs.getString("AttachmentPath1")); } catch (Exception ex) { }
            try { dto.setAttachmentPath2(rs.getString("AttachmentPath2")); } catch (Exception ex) { }
            try { dto.setAttachmentPath3(rs.getString("AttachmentPath3")); } catch (Exception ex) { }

            return dto;
        }, participantID.toString()).stream().findFirst();
    }

    /**
     * 根據 ParticipantID 查詢該幼兒在案件中的詳細信息（包括所有參與者）
     * @param participantID 參與者ID
     * @return ApplicationCaseDTO 包含該案件的所有參與者信息
     */
    public Optional<ApplicationCaseDTO> findApplicationCaseByParticipantId(UUID participantID) {
        String sql = "SELECT a.ApplicationID, a.ApplicationDate, i.InstitutionName, " +
                "a.IdentityType, " +
                "a.AttachmentPath, a.AttachmentPath1, a.AttachmentPath2, a.AttachmentPath3, " +
                "ap.ParticipantID, ap.ParticipantType, ap.NationalID, ap.Name, ap.Gender, ap.RelationShip, ap.Occupation, " +
                "ap.PhoneNumber, ap.HouseholdAddress, ap.MailingAddress, ap.Email, ap.BirthDate, " +
                "ap.IsSuspended, ap.SuspendEnd, ap.CurrentOrder, ap.Status, ap.Reason, ap.ClassID, ap.ReviewDate " +
                "FROM applications a " +
                "LEFT JOIN institutions i ON a.InstitutionID = i.InstitutionID " +
                "LEFT JOIN application_participants ap ON a.ApplicationID = ap.ApplicationID " +
                "WHERE a.ApplicationID = (SELECT ApplicationID FROM application_participants WHERE ParticipantID = ?) " +
                "ORDER BY ap.CurrentOrder";

        java.util.Map<String, Object> resultMap = new java.util.HashMap<>();

        jdbcTemplate.query(sql, (rs, rowNum) -> {
            if (!resultMap.containsKey("header")) {
                ApplicationCaseDTO dto = new ApplicationCaseDTO();
                if (rs.getString("ApplicationID") != null) {
                    dto.applicationId = UUID.fromString(rs.getString("ApplicationID"));
                }
                if (rs.getDate("ApplicationDate") != null) {
                    dto.applicationDate = rs.getDate("ApplicationDate").toLocalDate();
                }
                dto.institutionName = rs.getString("InstitutionName");
                dto.parents = new java.util.ArrayList<>();
                dto.children = new java.util.ArrayList<>();

                try {
                    Object idTypeObj = rs.getObject("IdentityType");
                    if (idTypeObj != null) {
                        dto.identityType = ((Number) idTypeObj).byteValue();
                    }
                } catch (Exception ex) {
                    dto.identityType = null;
                }

                try { dto.attachmentPath = rs.getString("AttachmentPath"); } catch (Exception ex) { dto.attachmentPath = null; }
                try { dto.attachmentPath1 = rs.getString("AttachmentPath1"); } catch (Exception ex) { dto.attachmentPath1 = null; }
                try { dto.attachmentPath2 = rs.getString("AttachmentPath2"); } catch (Exception ex) { dto.attachmentPath2 = null; }
                try { dto.attachmentPath3 = rs.getString("AttachmentPath3"); } catch (Exception ex) { dto.attachmentPath3 = null; }

                resultMap.put("header", dto);
            }

            if (rs.getString("NationalID") != null) {
                ApplicationParticipantDTO p = new ApplicationParticipantDTO();
                Object ptObj = null;
                try { ptObj = rs.getObject("ParticipantType"); } catch (Exception ex) { ptObj = null; }
                Boolean isParent = null;
                if (ptObj instanceof Boolean) {
                    try { isParent = rs.getBoolean("ParticipantType"); } catch (Exception ex) { isParent = null; }
                } else if (ptObj != null) {
                    try { int v = rs.getInt("ParticipantType"); isParent = (v == 2); } catch (Exception ex) { isParent = null; }
                }

                // 設置 ParticipantID
                try {
                    String participantIdStr = rs.getString("ParticipantID");
                    if (participantIdStr != null && !participantIdStr.isEmpty()) {
                        p.participantID = UUID.fromString(participantIdStr);
                    }
                } catch (Exception ex) { p.participantID = null; }

                p.participantType = (isParent != null && isParent) ? "家長" : "幼兒";
                p.nationalID = rs.getString("NationalID");
                p.name = rs.getString("Name");
                try { Object gObj = rs.getObject("Gender"); if (gObj != null) { p.gender = rs.getBoolean("Gender") ? "男" : "女"; } else { p.gender = null; } } catch (Exception ex) { p.gender = null; }
                p.relationShip = rs.getString("RelationShip");
                p.occupation = rs.getString("Occupation");
                p.phoneNumber = rs.getString("PhoneNumber");
                p.householdAddress = rs.getString("HouseholdAddress");
                p.mailingAddress = rs.getString("MailingAddress");
                p.email = rs.getString("Email");
                if (rs.getDate("BirthDate") != null) p.birthDate = rs.getDate("BirthDate").toString();
                try { Object susp = rs.getObject("IsSuspended"); if (susp != null) p.isSuspended = rs.getBoolean("IsSuspended"); else p.isSuspended = null; } catch (Exception ex) { p.isSuspended = null; }
                if (rs.getDate("SuspendEnd") != null) p.suspendEnd = rs.getDate("SuspendEnd").toString();
                try { Object co = rs.getObject("CurrentOrder"); if (co != null) p.currentOrder = rs.getInt("CurrentOrder"); else p.currentOrder = null; } catch (Exception ex) { p.currentOrder = null; }
                p.status = rs.getString("Status");
                p.reason = rs.getString("Reason");
                p.classID = rs.getString("ClassID");
                if (rs.getTimestamp("ReviewDate") != null) {
                    p.reviewDate = rs.getTimestamp("ReviewDate").toLocalDateTime();
                }

                ApplicationCaseDTO dto = (ApplicationCaseDTO) resultMap.get("header");

                if (isParent != null && isParent) {
                    dto.parents.add(p);
                } else {
                    dto.children.add(p);
                }
            }

            return null;
        }, participantID.toString());

        if (!resultMap.containsKey("header")) {
            return Optional.empty();
        }

        ApplicationCaseDTO dto = (ApplicationCaseDTO) resultMap.get("header");
        return Optional.of(dto);
    }

    // 新增：更新 applications 表的四個附件欄位
    public int updateAttachmentPaths(java.util.UUID applicationId, String path0, String path1, String path2, String path3) {
        String sql = "UPDATE " + TABLE_NAME + " SET AttachmentPath = ?, AttachmentPath1 = ?, AttachmentPath2 = ?, AttachmentPath3 = ? WHERE ApplicationID = ?";
        try {
            return jdbcTemplate.update(sql,
                    path0,
                    path1,
                    path2,
                    path3,
                    applicationId != null ? applicationId.toString() : null
            );
        } catch (Exception ex) {
            // 若更新失敗，回傳 0
            return 0;
        }
    }
    public long countCaseNumberWithDateFormat() {
        // 查詢 CaseNumber >= 100000000000 (12位數，代表符合 YYYYMMDD+4位流水號格式)
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE CaseNumber >= 100000000000";
        System.out.println("🔍 [countCaseNumberWithDateFormat] Executing SQL: " + sql);
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        System.out.println("✅ [countCaseNumberWithDateFormat] Count result: " + count);
        return count != null ? count : 0;
    }
}




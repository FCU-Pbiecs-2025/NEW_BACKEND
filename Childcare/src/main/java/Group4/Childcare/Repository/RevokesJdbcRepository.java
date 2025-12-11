package Group4.Childcare.Repository;

import Group4.Childcare.DTO.RevokeApplicationDTO;
import Group4.Childcare.DTO.ApplicationParticipantDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

@Repository
public class RevokesJdbcRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public RevokesJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RevokeApplicationDTO> findRevokedApplications(int page, int size, String institutionID, String caseNumber, String nationalID) {
        StringBuilder sql = new StringBuilder(
                "SELECT c.[CancellationID], a.[ApplicationID], c.[CancellationDate], " +
                        "a.[UserID], u.[Name] AS [UserName], c.[CaseNumber], " +
                        "a.[InstitutionID], i.[InstitutionName], c.[NationalID], " +
                        "c.[AbandonReason] " +
                        "FROM [dbo].[cancellation] c " +
                        "JOIN [dbo].[applications] a ON c.[ApplicationID] = a.[ApplicationID] " +
                        "JOIN [dbo].[application_participants] ap ON c.[ApplicationID] = ap.[ApplicationID] and c.[NationalID]=ap.[NationalID] " +
                        "JOIN [dbo].[users] u ON a.[UserID] = u.[UserID] " +
                        "JOIN [dbo].[institutions] i ON a.[InstitutionID] = i.[InstitutionID] " +
                        "WHERE ap.Status='撤銷申請審核中' ");

        java.util.List<Object> params = new java.util.ArrayList<>();
        if (institutionID != null && !institutionID.isEmpty()) {
            sql.append("AND a.[InstitutionID] = ? ");
            params.add(institutionID);
        }
        if (caseNumber != null && !caseNumber.isEmpty()) {
            sql.append("AND c.[CaseNumber] = ? ");
            params.add(caseNumber);
        }
        if (nationalID != null && !nationalID.isEmpty()) {
            sql.append("AND ap.[NationalID] = ? ");
            params.add(nationalID);
        }


        sql.append("ORDER BY c.[CancellationDate] DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(page * size);
        params.add(size);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            if (rowNum < 0) throw new IllegalStateException("invalid row");
            return new RevokeApplicationDTO(
                UUID.fromString(rs.getString("CancellationID")),
                UUID.fromString(rs.getString("ApplicationID")),
                rs.getDate("CancellationDate") != null ? rs.getDate("CancellationDate").toLocalDate().atStartOfDay() : null,
                UUID.fromString(rs.getString("UserID")),
                rs.getString("UserName"),
                UUID.fromString(rs.getString("InstitutionID")),
                rs.getString("InstitutionName"),
                rs.getString("AbandonReason"),
                rs.getString("NationalID"),
                rs.getString("CaseNumber")
        );
        }, params.toArray());
    }

    // New: total count for pagination
    public long countRevokedApplications(String institutionID, String caseNumber, String nationalID) {
        // Counting cancellations is sufficient as each cancellation represents one revoked application
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM [dbo].[cancellation] c " +
            "JOIN [dbo].[applications] a ON c.[ApplicationID] = a.[ApplicationID] " +
            "JOIN [dbo].[application_participants] ap ON c.[ApplicationID] = ap.[ApplicationID] and c.[NationalID]=ap.[NationalID] " +
            "WHERE ap.Status='撤銷申請審核中' ");

        java.util.List<Object> params = new java.util.ArrayList<>();
        if (institutionID != null && !institutionID.isEmpty()) {
            sql.append("AND a.[InstitutionID] = ? ");
            params.add(institutionID);
        }
        if (caseNumber != null && !caseNumber.isEmpty()) {
            sql.append("AND c.[CaseNumber] = ? ");
            params.add(caseNumber);
        }
        if (nationalID != null && !nationalID.isEmpty()) {
            sql.append("AND ap.[NationalID] = ? ");
            params.add(nationalID);
        }

        Long count = params.isEmpty()
            ? jdbcTemplate.queryForObject(sql.toString(), Long.class)
            : jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    // 分頁搜尋撤銷申請（根據 CaseNumber 和 NationalID）
    public List<RevokeApplicationDTO> searchRevokedApplicationsPaged(String caseNumber, String nationalID, int page, int size, String institutionID) {
        StringBuilder sql = new StringBuilder(
            "SELECT c.[CancellationID], a.[ApplicationID], c.[CancellationDate], " +
            "a.[UserID], u.[Name] AS [UserName], c.[CaseNumber], " +
            "a.[InstitutionID], i.[InstitutionName], c.[NationalID], " +
            "c.[AbandonReason] " +
            "FROM [dbo].[cancellation] c " +
            "JOIN [dbo].[applications] a ON c.[ApplicationID] = a.[ApplicationID] " +
            "JOIN [dbo].[users] u ON a.[UserID] = u.[UserID] " +
            "JOIN [dbo].[institutions] i ON a.[InstitutionID] = i.[InstitutionID] " +
            "JOIN [dbo].[application_participants] ap ON c.[ApplicationID] = ap.[ApplicationID] and c.[NationalID]=ap.[NationalID] " );
        boolean hasWhere = false;
        if (caseNumber != null && !caseNumber.isEmpty()) {
            sql.append("WHERE c.[CaseNumber] LIKE ? ");
            hasWhere = true;
        }
        if (nationalID != null && !nationalID.isEmpty()) {
            sql.append(hasWhere ? "AND " : "WHERE ");
            sql.append("c.[NationalID] LIKE ? ");
            hasWhere = true;
        }
        if (institutionID != null && !institutionID.isEmpty()) {
            sql.append(hasWhere ? "AND " : "WHERE ");
            sql.append("a.[InstitutionID] = ? ");
            hasWhere = true;
        }
        sql.append(hasWhere ? "AND " : "WHERE ");
        sql.append("ap.Status='撤銷申請審核中' ORDER BY c.[CancellationDate] DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        // 準備參數
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (caseNumber != null && !caseNumber.isEmpty()) params.add("%" + caseNumber + "%");
        if (nationalID != null && !nationalID.isEmpty()) params.add("%" + nationalID + "%");
        if (institutionID != null && !institutionID.isEmpty()) params.add(institutionID);
        params.add(page * size);
        params.add(size);
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            if (rowNum < 0) throw new IllegalStateException("invalid row"); // use rowNum to avoid unused-parameter warning
            return new RevokeApplicationDTO(
            UUID.fromString(rs.getString("CancellationID")),
            UUID.fromString(rs.getString("ApplicationID")),
            rs.getDate("CancellationDate") != null ? rs.getDate("CancellationDate").toLocalDate().atStartOfDay() : null,
            UUID.fromString(rs.getString("UserID")),
            rs.getString("UserName"),
            UUID.fromString(rs.getString("InstitutionID")),
            rs.getString("InstitutionName"),
            rs.getString("AbandonReason"),
            rs.getString("NationalID"),
            rs.getString("CaseNumber")
        );
        }, params.toArray());
    }

    // 條件搜尋總數（根據 CaseNumber 與 NationalID）
    public long countSearchRevokedApplications(String caseNumber, String nationalID, String institutionID) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM [dbo].[cancellation] c " +
            "JOIN [dbo].[applications] a ON c.[ApplicationID] = a.[ApplicationID] " +
            "JOIN [dbo].[users] u ON a.[UserID] = u.[UserID] " +
            "JOIN [dbo].[institutions] i ON a.[InstitutionID] = i.[InstitutionID] " +
            "JOIN [dbo].[application_participants] ap ON c.[ApplicationID] = ap.[ApplicationID] and c.[NationalID]=ap.[NationalID] " );
        boolean hasWhere = false;
        if (caseNumber != null && !caseNumber.isEmpty()) {
            sql.append("WHERE c.[CaseNumber] LIKE ? ");
            hasWhere = true;
        }
        if (nationalID != null && !nationalID.isEmpty()) {
            sql.append(hasWhere ? "AND " : "WHERE ");
            sql.append("c.[NationalID] LIKE ? ");
            hasWhere = true;
        }
        if (institutionID != null && !institutionID.isEmpty()) {
            sql.append(hasWhere ? "AND " : "WHERE ");
            sql.append("a.[InstitutionID] = ? ");
            hasWhere = true;
        }
        sql.append(hasWhere ? "AND " : "WHERE ");
        sql.append("ap.Status='撤銷申請審核中' ");

        java.util.List<Object> params = new java.util.ArrayList<>();
        if (caseNumber != null && !caseNumber.isEmpty()) params.add("%" + caseNumber + "%");
        if (nationalID != null && !nationalID.isEmpty()) params.add("%" + nationalID + "%");
        if (institutionID != null && !institutionID.isEmpty()) params.add(institutionID);
        Long count = params.isEmpty()
            ? jdbcTemplate.queryForObject(sql.toString(), Long.class)
            : jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    // 新增：根據 CancellationID 取得撤銷資料（含基本欄位）
    public RevokeApplicationDTO getRevokeByCancellationID(String cancellationID) {
        String sql = "SELECT c.[CancellationID],c.[ApplicationID], c.[CancellationDate], a.[UserID], u.[Name] AS [UserName], a.[InstitutionID], i.[InstitutionName], c.[AbandonReason], c.[NationalID], c.[CaseNumber] FROM [dbo].[cancellation] c JOIN [dbo].[applications] a ON c.[ApplicationID] = a.[ApplicationID] JOIN [dbo].[users] u ON a.[UserID] = u.[UserID] JOIN [dbo].[institutions] i ON a.[InstitutionID] = i.[InstitutionID] WHERE c.[CancellationID] = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{cancellationID}, (rs, rowNum) -> new RevokeApplicationDTO(
                UUID.fromString(rs.getString("CancellationID")),
                UUID.fromString(rs.getString("ApplicationID")),
                rs.getDate("CancellationDate") != null ? rs.getDate("CancellationDate").toLocalDate().atStartOfDay() : null,
                UUID.fromString(rs.getString("UserID")),
                rs.getString("UserName"),
                UUID.fromString(rs.getString("InstitutionID")),
                rs.getString("InstitutionName"),
                rs.getString("AbandonReason"),
                rs.getString("NationalID"),
                rs.getString("CaseNumber")
        ));
    }

    // 新增：透過 cancellation LEFT JOIN application_participants 取得 participantType == 1 的家長資料
    public List<ApplicationParticipantDTO> getParentsByCancellation(String cancellationID) {
        String sql = "SELECT ap.[NationalID], ap.[Name], ap.[Gender], ap.[RelationShip], ap.[Occupation], ap.[PhoneNumber], ap.[HouseholdAddress], ap.[MailingAddress], ap.[Email], ap.[BirthDate], ap.[IsSuspended], ap.[SuspendEnd] FROM [dbo].[cancellation] c LEFT JOIN [dbo].[applications] a ON c.[ApplicationID] = a.[ApplicationID] LEFT JOIN [dbo].[application_participants] ap ON a.[ApplicationID] = ap.[ApplicationID] WHERE c.[CancellationID] = ? AND ap.[ParticipantType] = 1";
        return jdbcTemplate.query(sql, new Object[]{cancellationID}, (rs, rowNum) -> {
            ApplicationParticipantDTO dto = new ApplicationParticipantDTO();
            dto.participantType = "1"; // or map as needed
            dto.nationalID = rs.getString("NationalID");
            dto.name = rs.getString("Name");
            dto.gender = rs.getString("Gender") != null && rs.getString("Gender").equals("1") ? "男" : "女";
            dto.relationShip = rs.getString("RelationShip");
            dto.occupation = rs.getString("Occupation");
            dto.phoneNumber = rs.getString("PhoneNumber");
            dto.householdAddress = rs.getString("HouseholdAddress");
            dto.mailingAddress = rs.getString("MailingAddress");
            dto.email = rs.getString("Email");
            dto.birthDate = rs.getString("BirthDate");
            dto.isSuspended = rs.getString("IsSuspended") != null && (rs.getString("IsSuspended").equals("1") || rs.getString("IsSuspended").equalsIgnoreCase("true"));
            dto.suspendEnd = rs.getString("SuspendEnd");
            return dto;
        });
    }

    // 新增：依據 CancellationID 與 NationalID 查 applications 與 application_participants 裡的資料
    public ApplicationParticipantDTO getApplicationDetailByCancellationAndNationalID(String cancellationID, String nationalID) {
        String sql = "SELECT  ap.[ApplicationID],   ap.[NationalID], ap.[Name], ap.[Gender], ap.[RelationShip], ap.[Occupation], ap.[PhoneNumber], ap.[HouseholdAddress], ap.[MailingAddress], ap.[Email], ap.[BirthDate], ap.[IsSuspended], ap.[SuspendEnd] FROM [dbo].[cancellation] c LEFT JOIN [dbo].[applications] a ON c.[ApplicationID] = a.[ApplicationID] LEFT JOIN [dbo].[application_participants] ap ON a.[ApplicationID] = ap.[ApplicationID] WHERE c.[CancellationID] = ? AND ap.[NationalID] = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{cancellationID, nationalID}, (rs, rowNum) -> {
            ApplicationParticipantDTO dto = new ApplicationParticipantDTO();
            dto.applicationID = UUID.fromString(rs.getString("ApplicationID"));
            dto.participantType = "0"; // Child participant type
            dto.nationalID = rs.getString("NationalID");
            dto.name = rs.getString("Name");
            dto.gender = rs.getString("Gender") != null && rs.getString("Gender").equals("1") ? "男" : "女";
            dto.relationShip = rs.getString("RelationShip");
            dto.occupation = rs.getString("Occupation");
            dto.phoneNumber = rs.getString("PhoneNumber");
            dto.householdAddress = rs.getString("HouseholdAddress");
            dto.mailingAddress = rs.getString("MailingAddress");
            dto.email = rs.getString("Email");
            dto.birthDate = rs.getString("BirthDate");
            dto.isSuspended = rs.getString("IsSuspended") != null && (rs.getString("IsSuspended").equals("1") || rs.getString("IsSuspended").equalsIgnoreCase("true"));
            dto.suspendEnd = rs.getString("SuspendEnd");
            return dto;
        });
    }

    // 新增：更新撤銷聲請的確認日期
    public int updateConfirmDate(String cancellationID, LocalDate confirmDate) {
        String sql = "UPDATE [dbo].[cancellation] SET [ConfirmDate] = ? WHERE [CancellationID] = ?";
        return jdbcTemplate.update(sql, confirmDate, cancellationID.toUpperCase());
    }

    // 新增：插入一筆 cancellation 紀錄，回傳生成的 CancellationID
    @Transactional
    public void insertCancellation(String applicationID, String abandonReason, String nationalID, LocalDate cancellationDate, String caseNumber) {
        // 產生 CancellationID 並寫入 cancellation 表
        String cancellationID = UUID.randomUUID().toString();
        String sql = "INSERT INTO [dbo].[cancellation] ([CancellationID], [ApplicationID], [AbandonReason], [CancellationDate], [NationalID], [CaseNumber]) VALUES (?, ?, ?, ?, ?, ?)";
        int updated = jdbcTemplate.update(sql, cancellationID, applicationID, abandonReason, cancellationDate, nationalID, caseNumber);
        if (updated <= 0) throw new IllegalStateException("Failed to insert cancellation");

        // 更新 application_participants 的 Status 為「撤銷申請審核中」，條件為 ApplicationID 與 NationalID
        String updateStatusSql = "UPDATE [dbo].[application_participants] SET [Status] = ? WHERE [ApplicationID] = ? AND [NationalID] = ?";
        int updateCount = jdbcTemplate.update(updateStatusSql, "撤銷申請審核中", applicationID, nationalID);
        if (updateCount == 0) {
            // 沒有找到對應的 application_participants 行；記錄即可（如需嚴格檢查可改為拋例外）
            System.out.println("No application_participants row matched for ApplicationID=" + applicationID + ", NationalID=" + nationalID);
        }
        // method intentionally returns void
    }

    // 新增：更新 application_participants 的 Status
    public int updateApplicationParticipantStatus(String applicationID, String nationalID, String status) {
        String updateStatusSql = "UPDATE [dbo].[application_participants] SET [Status] = ? WHERE [ApplicationID] = ? AND [NationalID] = ?";
        int updateCount = jdbcTemplate.update(updateStatusSql, status, applicationID, nationalID);
        if (updateCount == 0) {
            System.out.println("No application_participants row matched for ApplicationID=" + applicationID + ", NationalID=" + nationalID);
        }
        return updateCount;
    }
}

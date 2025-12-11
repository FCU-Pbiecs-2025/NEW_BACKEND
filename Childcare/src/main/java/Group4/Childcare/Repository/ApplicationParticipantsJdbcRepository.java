package Group4.Childcare.Repository;

import Group4.Childcare.Model.ApplicationParticipants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ApplicationParticipantsJdbcRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String TABLE_NAME = "application_participants";

    private static final RowMapper<ApplicationParticipants> ROW_MAPPER = (rs, rowNum) -> {
        ApplicationParticipants ap = new ApplicationParticipants();
        String applicationIdStr = rs.getString("ApplicationID");
        if (applicationIdStr != null) {
            ap.setApplicationID(UUID.fromString(applicationIdStr));
        }
        ap.setParticipantType(rs.getBoolean("ParticipantType"));
        ap.setNationalID(rs.getString("NationalID"));
        ap.setName(rs.getString("Name"));
        ap.setGender(rs.getBoolean("Gender"));
        ap.setRelationShip(rs.getString("RelationShip"));
        ap.setOccupation(rs.getString("Occupation"));
        ap.setPhoneNumber(rs.getString("PhoneNumber"));
        ap.setHouseholdAddress(rs.getString("HouseholdAddress"));
        ap.setMailingAddress(rs.getString("MailingAddress"));
        ap.setEmail(rs.getString("Email"));
        Date birthDate = rs.getDate("BirthDate");
        if (birthDate != null) {
            ap.setBirthDate(birthDate.toLocalDate());
        }
        ap.setIsSuspended(rs.getBoolean("IsSuspended"));
        Date suspendEnd = rs.getDate("SuspendEnd");
        if (suspendEnd != null) {
            ap.setSuspendEnd(suspendEnd.toLocalDate());
        }
        ap.setCurrentOrder(rs.getInt("CurrentOrder"));
        ap.setStatus(rs.getString("Status"));
        ap.setReason(rs.getString("Reason"));
        String classIdStr = rs.getString("ClassID");
        if (classIdStr != null) {
            ap.setClassID(UUID.fromString(classIdStr));
        }
        return ap;
    };

    public ApplicationParticipants save(ApplicationParticipants ap) {

        // 如果 ParticipantID 為 null，生成新的 UUID 並執行 INSERT
        if (ap.getParticipantID() == null) {
            ap.setParticipantID(UUID.randomUUID());
            System.out.println("🆕 [save] ParticipantID is null, generating new UUID and executing INSERT");
            return insert(ap);
        }

        // 如果 ParticipantID 不為 null，檢查資料庫中是否已存在
        boolean exists = existsById(ap.getParticipantID());
        System.out.println("🔍 [save] ParticipantID = " + ap.getParticipantID() + ", exists in DB = " + exists);

        if (exists) {
            System.out.println("📝 [save] Record exists, executing UPDATE");
            return update(ap);
        } else {
            System.out.println("🆕 [save] Record not exists, executing INSERT");
            return insert(ap);
        }
    }

    private ApplicationParticipants insert(ApplicationParticipants ap) {
        // Ensure ParticipantID is set
        if (ap.getParticipantID() == null) {
            ap.setParticipantID(UUID.randomUUID());
        }

        String sql = "INSERT INTO " + TABLE_NAME +
                " (ParticipantID, ApplicationID, ParticipantType, NationalID, Name, Gender, RelationShip, Occupation, PhoneNumber, HouseholdAddress, MailingAddress, Email, BirthDate, IsSuspended, SuspendEnd, CurrentOrder, Status, Reason, ClassID, ReviewDate) " +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        System.out.println("🔵 ApplicationParticipantsJdbcRepository.insert() - Executing SQL:");
        System.out.println("  ParticipantID: " + ap.getParticipantID());
        System.out.println("  ApplicationID: " + ap.getApplicationID());
        System.out.println("  ParticipantType: " + ap.getParticipantType());
        System.out.println("  Name: " + ap.getName());
        System.out.println("  NationalID: " + ap.getNationalID());

        int rows = jdbcTemplate.update(sql,
                ap.getParticipantID() != null ? ap.getParticipantID().toString() : null,
                ap.getApplicationID() != null ? ap.getApplicationID().toString() : null,
                ap.getParticipantType(),
                ap.getNationalID(),
                ap.getName(),
                ap.getGender(),
                ap.getRelationShip(),
                ap.getOccupation(),
                ap.getPhoneNumber(),
                ap.getHouseholdAddress(),
                ap.getMailingAddress(),
                ap.getEmail(),
                ap.getBirthDate(),
                ap.getIsSuspended(),
                ap.getSuspendEnd(),
                ap.getCurrentOrder(),
                ap.getStatus(),
                ap.getReason(),
                ap.getClassID() != null ? ap.getClassID().toString() : null,
                ap.getReviewDate()
        );

        System.out.println("✅ INSERT completed! Rows affected: " + rows);
        return ap;
    }

    private ApplicationParticipants update(ApplicationParticipants ap) {
        String sql = "UPDATE " + TABLE_NAME +
                " SET ParticipantType = ?, NationalID = ?, Name = ?, Gender = ?, RelationShip = ?, Occupation = ?, PhoneNumber = ?, HouseholdAddress = ?, MailingAddress = ?, Email = ?, BirthDate = ?, IsSuspended = ?, SuspendEnd = ?, CurrentOrder = ?, Status = ?, Reason = ?, ClassID = ? WHERE ApplicationID = ?";
        jdbcTemplate.update(sql,
                ap.getParticipantType(),
                ap.getNationalID(),
                ap.getName(),
                ap.getGender(),
                ap.getRelationShip(),
                ap.getOccupation(),
                ap.getPhoneNumber(),
                ap.getHouseholdAddress(),
                ap.getMailingAddress(),
                ap.getEmail(),
                ap.getBirthDate(),
                ap.getIsSuspended(),
                ap.getSuspendEnd(),
                ap.getCurrentOrder(),
                ap.getStatus(),
                ap.getReason(),
                ap.getClassID() != null ? ap.getClassID().toString() : null,
                ap.getApplicationID() != null ? ap.getApplicationID().toString() : null
        );
        return ap;
    }

    public Optional<ApplicationParticipants> findById(UUID id) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE ApplicationID = ?";
        try {
            ApplicationParticipants ap = jdbcTemplate.queryForObject(sql, ROW_MAPPER, id.toString());
            return Optional.ofNullable(ap);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<ApplicationParticipants> findAll() {
        String sql = "SELECT * FROM " + TABLE_NAME;
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    public void deleteById(UUID id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE ApplicationID = ?";
        jdbcTemplate.update(sql, id.toString());
    }

    public void delete(ApplicationParticipants ap) {
        deleteById(ap.getApplicationID());
    }

    public boolean existsById(UUID id) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE ParticipantID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id.toString());
        return count != null && count > 0;
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME;
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    public List<ApplicationParticipants> findByApplicationIDAndNationalID(UUID applicationID, String nationalID) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE ApplicationID = ? AND NationalID = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, applicationID.toString(), nationalID);
    }

    /**
     * 計算指定 NationalID 且 ParticipantType = false (幼兒) 的總案件數
     * @param nationalID 幼兒身分證字號
     * @return 該幼兒的總案件數
     */
    public int countApplicationsByChildNationalID(String nationalID) {
        String sql = "SELECT COUNT(DISTINCT ApplicationID) FROM " + TABLE_NAME +
                     " WHERE NationalID = ? AND ParticipantType = 0";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, nationalID);
        return count != null ? count : 0;
    }
}


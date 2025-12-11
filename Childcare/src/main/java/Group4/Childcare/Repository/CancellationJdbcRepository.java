package Group4.Childcare.Repository;

import Group4.Childcare.Model.Cancellation;
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
public class CancellationJdbcRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String TABLE_NAME = "cancellation";

    // RowMapper for Cancellation entity
    private static final RowMapper<Cancellation> CANCELLATION_ROW_MAPPER = new RowMapper<Cancellation>() {
        @Override
        public Cancellation mapRow(ResultSet rs, int rowNum) throws SQLException {
            Cancellation cancellation = new Cancellation();
            cancellation.setCancellationID(UUID.fromString(rs.getString("CancellationID")));

            if (rs.getString("ApplicationID") != null) {
                cancellation.setApplicationID(UUID.fromString(rs.getString("ApplicationID")));
            }

            cancellation.setAbandonReason(rs.getString("AbandonReason"));

            if (rs.getDate("CancellationDate") != null) {
                cancellation.setCancellationDate(rs.getDate("CancellationDate").toLocalDate());
            }

            if (rs.getDate("ConfirmDate") != null) {
                cancellation.setConfirmDate(rs.getDate("ConfirmDate").toLocalDate());
            }

            return cancellation;
        }
    };

    // Save method
    public Cancellation save(Cancellation cancellation) {
        if (cancellation.getCancellationID() == null) {
            cancellation.setCancellationID(UUID.randomUUID());
            return insert(cancellation);
        } else {
            return update(cancellation);
        }
    }

    // Insert method
    private Cancellation insert(Cancellation cancellation) {
        String sql = "INSERT INTO " + TABLE_NAME +
                    " (CancellationID, ApplicationID, AbandonReason, CancellationDate, ConfirmDate) " +
                    "VALUES (?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
            cancellation.getCancellationID().toString(),
            cancellation.getApplicationID() != null ? cancellation.getApplicationID().toString() : null,
            cancellation.getAbandonReason(),
            cancellation.getCancellationDate(),
            cancellation.getConfirmDate()
        );

        return cancellation;
    }

    // Update method
    private Cancellation update(Cancellation cancellation) {
        String sql = "UPDATE " + TABLE_NAME +
                    " SET ApplicationID = ?, AbandonReason = ?, CancellationDate = ?, ConfirmDate = ? " +
                    "WHERE CancellationID = ?";

        jdbcTemplate.update(sql,
            cancellation.getApplicationID() != null ? cancellation.getApplicationID().toString() : null,
            cancellation.getAbandonReason(),
            cancellation.getCancellationDate(),
            cancellation.getConfirmDate(),
            cancellation.getCancellationID().toString()
        );

        return cancellation;
    }

    // Find by ID
    public Optional<Cancellation> findById(UUID id) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE CancellationID = ?";
        try {
            Cancellation cancellation = jdbcTemplate.queryForObject(sql, CANCELLATION_ROW_MAPPER, id.toString());
            return Optional.ofNullable(cancellation);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // Find all
    public List<Cancellation> findAll() {
        String sql = "SELECT * FROM " + TABLE_NAME;
        return jdbcTemplate.query(sql, CANCELLATION_ROW_MAPPER);
    }

    // Delete by ID
    public void deleteById(UUID id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE CancellationID = ?";
        jdbcTemplate.update(sql, id.toString());
    }

    // Delete entity
    public void delete(Cancellation cancellation) {
        deleteById(cancellation.getCancellationID());
    }

    // Check if exists by ID
    public boolean existsById(UUID id) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE CancellationID = ?";
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

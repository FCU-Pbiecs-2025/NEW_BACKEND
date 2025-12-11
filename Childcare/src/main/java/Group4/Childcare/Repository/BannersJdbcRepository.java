package Group4.Childcare.Repository;

import Group4.Childcare.Model.Banners;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
public class BannersJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String TABLE_NAME = "banners";

    // RowMapper for Banners entity
    private static final RowMapper<Banners> BANNERS_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        Banners banner = new Banners();
        banner.setSortOrder(rs.getInt("SortOrder"));

        Timestamp t1 = rs.getTimestamp("StartTime");
        if (t1 != null) {
            banner.setStartTime(t1.toLocalDateTime());
        }

        Timestamp t2 = rs.getTimestamp("EndTime");
        if (t2 != null) {
            banner.setEndTime(t2.toLocalDateTime());
        }

        banner.setImageName(rs.getString("ImageName"));
        banner.setLinkUrl(rs.getString("LinkUrl"));
        try {
            banner.setStatus(rs.getBoolean("Status"));
        } catch (SQLException ex) {
            int v = rs.getInt("Status");
            banner.setStatus(v != 0);
        }

        return banner;
    };

    @Autowired
    public BannersJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Save method (insert or update)
    public Banners save(Banners banner) {
        if (banner.getSortOrder() > 0 && existsById(banner.getSortOrder())) {
            return update(banner);
        } else {
            return insert(banner);
        }
    }

    // Insert method using manual SortOrder
    private Banners insert(Banners banner) {
        if (banner == null) {
            throw new IllegalArgumentException("Banner cannot be null for insert");
        }
        if (banner.getSortOrder() <= 0 || banner.getStartTime() == null || banner.getEndTime() == null || banner.getImageName() == null || banner.getStatus() == null) {
            throw new IllegalArgumentException("SortOrder, StartTime, EndTime, ImageName, Status are required and cannot be null");
        }
        String sql = "INSERT INTO " + TABLE_NAME + " (SortOrder, StartTime, EndTime, ImageName, LinkUrl, Status) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            int rowsAffected = jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setInt(1, banner.getSortOrder());
                ps.setTimestamp(2, Timestamp.valueOf(banner.getStartTime()));
                ps.setTimestamp(3, Timestamp.valueOf(banner.getEndTime()));
                ps.setString(4, banner.getImageName());
                if (banner.getLinkUrl() != null) {
                    ps.setString(5, banner.getLinkUrl());
                } else {
                    ps.setNull(5, java.sql.Types.NVARCHAR);
                }
                ps.setBoolean(6, banner.getStatus());
                return ps;
            });
            if (rowsAffected == 0) {
                throw new RuntimeException("Failed to insert banner");
            }
            return banner;
        } catch (Exception e) {
            throw new RuntimeException("Failed to insert banner: " + e.getMessage(), e);
        }
    }

    // Update method (merge null fields with existing record and avoid setting NOT NULL columns to null)
    private Banners update(Banners banner) {
        if (banner == null || banner.getSortOrder() <= 0) {
            throw new IllegalArgumentException("Banner or SortOrder cannot be null/invalid for update");
        }

        Optional<Banners> existingOpt = findById(banner.getSortOrder());
        if (existingOpt.isEmpty()) {
            throw new RuntimeException("No banner found with SortOrder: " + banner.getSortOrder());
        }
        Banners existing = existingOpt.get();

        // Merge: keep existing values when incoming are null (except LinkUrl which can be null intentionally)
        if (banner.getStartTime() == null) banner.setStartTime(existing.getStartTime());
        if (banner.getEndTime() == null) banner.setEndTime(existing.getEndTime());
        if (banner.getImageName() == null) banner.setImageName(existing.getImageName());
        if (banner.getStatus() == null) banner.setStatus(existing.getStatus());
        // LinkUrl: allow null overwrite => if banner.getLinkUrl() is null, keep as null (explicit clearing)

        String sql = "UPDATE " + TABLE_NAME + " SET StartTime = ?, EndTime = ?, ImageName = ?, LinkUrl = ?, Status = ? WHERE SortOrder = ?";
        try {
            int rowsAffected = jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql);
                if (banner.getStartTime() != null) {
                    ps.setTimestamp(1, Timestamp.valueOf(banner.getStartTime()));
                } else {
                    ps.setNull(1, Types.TIMESTAMP);
                }
                if (banner.getEndTime() != null) {
                    ps.setTimestamp(2, Timestamp.valueOf(banner.getEndTime()));
                } else {
                    ps.setNull(2, Types.TIMESTAMP);
                }
                ps.setString(3, banner.getImageName());
                if (banner.getLinkUrl() != null) {
                    ps.setString(4, banner.getLinkUrl());
                } else {
                    ps.setNull(4, java.sql.Types.NVARCHAR);
                }
                ps.setBoolean(5, banner.getStatus());
                ps.setInt(6, banner.getSortOrder());
                return ps;
            });
            if (rowsAffected == 0) {
                throw new RuntimeException("No banner updated for SortOrder: " + banner.getSortOrder());
            }
            return banner;
        } catch (Exception e) {
            throw new RuntimeException("Failed to update banner with SortOrder: " + banner.getSortOrder() + ": " + e.getMessage(), e);
        }
    }

    // Find by ID
    public Optional<Banners> findById(Integer id) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE SortOrder = ?";
        try {
            Banners banner = jdbcTemplate.queryForObject(sql, BANNERS_ROW_MAPPER, id);
            return Optional.ofNullable(banner);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // Find all
    public List<Banners> findAll() {
        String sql = "SELECT * FROM " + TABLE_NAME + " ORDER BY SortOrder";
        return jdbcTemplate.query(sql, BANNERS_ROW_MAPPER);
    }

    // Paged find
    public List<Banners> findPage(int offset, int limit) {
        String sql = "SELECT * FROM " + TABLE_NAME + " ORDER BY SortOrder OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        return jdbcTemplate.query(sql, BANNERS_ROW_MAPPER, offset, limit);
    }

    // Delete entity
    public void delete(Banners banner) {
        deleteById(banner.getSortOrder());
    }

    // Delete by ID
    public void deleteById(Integer id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE SortOrder = ?";
        jdbcTemplate.update(sql, id);
    }

    // Check if exists by ID
    public boolean existsById(Integer id) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE SortOrder = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    // Count all
    public long count() {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME;
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    // Paginated find with offset and limit
    public List<Banners> findWithOffset(int offset, int limit) {
        String sql = "SELECT * FROM " + TABLE_NAME + " ORDER BY SortOrder OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
        return jdbcTemplate.query(sql, BANNERS_ROW_MAPPER);
    }

    // Find active banners: Status = true, now between StartTime and EndTime
    public List<Banners> findActiveBanners() {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE Status = 0 AND ? >= StartTime AND ? <= EndTime ORDER BY SortOrder";
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return jdbcTemplate.query(sql, BANNERS_ROW_MAPPER, now, now);
    }
}

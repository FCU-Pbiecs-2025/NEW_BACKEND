package Group4.Childcare.repository;

import Group4.Childcare.Model.Banners;
import Group4.Childcare.Repository.BannersJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class BannersJdbcRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private BannersJdbcRepository bannersRepository;

    private int testSortOrder;

    @BeforeEach
    void setUp() {
        testSortOrder = 1;
    }

    // ==================== save (Insert & Update) Tests ====================

    @Test
    void testSave_Insert_Success() throws Exception {
        // Given
        Banners banner = createTestBanner();
        banner.setSortOrder(5);

        // Mock existsById check (for save logic)
        // save checks if sortOrder > 0 (true) && existsById
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(5)))
                .thenReturn(0); // Not exists -> Insert

        // Mock insert update
        when(jdbcTemplate.update(any(PreparedStatementCreator.class))).thenReturn(1);

        // When
        Banners result = bannersRepository.save(banner);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getSortOrder());
        verify(jdbcTemplate).update(any(PreparedStatementCreator.class));
    }

    @Test
    void testSave_Update_Success() {
        // Given
        Banners banner = createTestBanner();
        banner.setSortOrder(5);

        // Mock existsById -> true
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(5)))
                .thenReturn(1);
        // Mock findById for update (it fetches existing to merge)
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(5)))
                .thenReturn(banner); // Return same banner as existing

        // Mock update execution
        when(jdbcTemplate.update(any(PreparedStatementCreator.class))).thenReturn(1);

        // When
        Banners result = bannersRepository.save(banner);

        // Then
        assertNotNull(result);
        verify(jdbcTemplate).update(any(PreparedStatementCreator.class));
    }

    @Test
    void testInsert_MissingRequiredFields_ThrowsException() {
        Banners banner = new Banners();
        banner.setSortOrder(1);
        // Missing start/end time etc.

        assertThrows(IllegalArgumentException.class, () -> bannersRepository.save(banner));
    }

    @Test
    void testInsert_NullBanner_ThrowsException() {
        // Using reflection or modified save logic to test private insert directly is
        // hard,
        // but save(null) throws NPE usually, but here checking repository logic
        try {
            bannersRepository.save(null);
            fail("Should throw NPE or IllegalArgumentException");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    void testUpdate_NotFound_ThrowsException() {
        // Given
        Banners banner = createTestBanner();
        banner.setSortOrder(999);

        // Exists -> true so it goes to update
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(999)))
                .thenReturn(1);

        // But findById inside update returns empty (simulation of race condition or
        // weird state)
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(999)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // When & Then
        assertThrows(RuntimeException.class, () -> bannersRepository.save(banner));
    }

    @Test
    void testUpdate_NoRowsAffected_ThrowsException() {
        // Given
        Banners banner = createTestBanner();
        banner.setSortOrder(5);

        // Exists -> true
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(5))).thenReturn(1);
        // Existing record found
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(5))).thenReturn(banner);

        // Mock update returning 0
        when(jdbcTemplate.update(any(PreparedStatementCreator.class))).thenReturn(0);

        // When & Then
        assertThrows(RuntimeException.class, () -> bannersRepository.save(banner));
    }

    // ==================== findById Tests ====================

    @Test
    void testFindById_Found() {
        Banners banner = createTestBanner();
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(testSortOrder)))
                .thenReturn(banner);

        Optional<Banners> result = bannersRepository.findById(testSortOrder);
        assertTrue(result.isPresent());
        assertEquals(testSortOrder, result.get().getSortOrder());
    }

    @Test
    void testFindById_NotFound() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyInt()))
                .thenThrow(new EmptyResultDataAccessException(1));

        Optional<Banners> result = bannersRepository.findById(999);
        assertFalse(result.isPresent());
    }

    // ==================== findAll & delete Tests ====================

    @Test
    void testFindAll() {
        List<Banners> list = Arrays.asList(createTestBanner(), createTestBanner());
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(list);

        List<Banners> result = bannersRepository.findAll();
        assertEquals(2, result.size());
    }

    @Test
    void testDeleteById() {
        bannersRepository.deleteById(1);
        verify(jdbcTemplate).update(anyString(), eq(1));
    }

    @Test
    void testDelete_Entity() {
        Banners banner = new Banners();
        banner.setSortOrder(10);
        bannersRepository.delete(banner);
        verify(jdbcTemplate).update(anyString(), eq(10));
    }

    // ==================== Custom Queries Tests ====================

    @Test
    void testFindActiveBanners() {
        List<Banners> list = Collections.singletonList(createTestBanner());
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any()))
                .thenReturn(list);

        List<Banners> result = bannersRepository.findActiveBanners();
        assertFalse(result.isEmpty());
    }

    @Test
    void testUpdateExpiredBanners() {
        when(jdbcTemplate.update(anyString(), any(Timestamp.class))).thenReturn(5);

        int updated = bannersRepository.updateExpiredBanners();
        assertEquals(5, updated);
    }

    @Test
    void testCount() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(10L);
        assertEquals(10L, bannersRepository.count());
    }

    @Test
    void testExistsById() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1))).thenReturn(1);
        assertTrue(bannersRepository.existsById(1));

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(2))).thenReturn(0);
        assertFalse(bannersRepository.existsById(2));
    }

    @Test
    void testFindPage() {
        List<Banners> list = Collections.singletonList(createTestBanner());
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt())).thenReturn(list);

        List<Banners> result = bannersRepository.findPage(0, 10);
        assertEquals(1, result.size());
    }

    @Test
    void testFindWithOffset() {
        List<Banners> list = Collections.singletonList(createTestBanner());
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(list);

        List<Banners> result = bannersRepository.findWithOffset(0, 10);
        assertEquals(1, result.size());
    }

    @Test
    void testFindByDateRangeWithOffset() {
        // This method uses a PreparedStatement callback
        List<Banners> list = Collections.singletonList(createTestBanner());

        // It's tricky to mock the functional interface callback for
        // query(PreparedStatementCreator, RowMapper)
        // But here it uses query(PreparedStatementCreator, RowMapper) or similar
        // The repository code uses: jdbcTemplate.query(connection -> ..., rowMapper)

        // We can mock jdbcTemplate.query(PreparedStatementCreator, RowMapper)
        when(jdbcTemplate.query(any(PreparedStatementCreator.class), any(RowMapper.class))).thenReturn(list);

        List<Banners> result = bannersRepository.findByDateRangeWithOffset(
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now().plusDays(1)),
                0, 10);

        assertEquals(1, result.size());
    }

    @Test
    void testCountByDateRange() {
        // Similar to above, uses query(PreparedStatementCreator, ResultSetExtractor)
        when(jdbcTemplate.query(any(PreparedStatementCreator.class),
                any(org.springframework.jdbc.core.ResultSetExtractor.class)))
                .thenReturn(5L);

        long count = bannersRepository.countByDateRange(
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now().plusDays(1)));

        assertEquals(5L, count);
    }

    // ==================== Helper Methods ====================

    private Banners createTestBanner() {
        Banners banner = new Banners();
        banner.setSortOrder(testSortOrder);
        banner.setStartTime(LocalDateTime.now());
        banner.setEndTime(LocalDateTime.now().plusDays(7));
        banner.setImageName("test-banner.jpg");
        banner.setLinkUrl("https://example.com");
        banner.setStatus(true);
        return banner;
    }
}

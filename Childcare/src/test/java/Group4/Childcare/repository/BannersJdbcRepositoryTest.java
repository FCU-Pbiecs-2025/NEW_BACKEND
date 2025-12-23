package Group4.Childcare.repository;

import Group4.Childcare.Model.Banners;
import Group4.Childcare.Repository.BannersJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class BannersJdbcRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private BannersJdbcRepository bannersRepository;

    private int testSortOrder;

    @BeforeEach
    void setUp() {
        testSortOrder = 1;
    }

    // ==================== save Tests ====================

    @Test
    void testSave_NewBanner_Success() {
        // Given
        Banners banner = createTestBanner();
        banner.setSortOrder(5);

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyInt()))
            .thenReturn(0); // not exists

        when(jdbcTemplate.update(any(PreparedStatementCreator.class)))
            .thenReturn(1);

        // When
        Banners result = bannersRepository.save(banner);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getSortOrder());
    }

    @Test
    void testSave_ExistingBanner_Update_Success() {
        // Given
        Banners banner = createTestBanner();
        banner.setSortOrder(testSortOrder);

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testSortOrder)))
            .thenReturn(1); // exists

        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(testSortOrder)))
            .thenReturn(banner);

        when(jdbcTemplate.update(any(PreparedStatementCreator.class)))
            .thenReturn(1);

        // When
        Banners result = bannersRepository.save(banner);

        // Then
        assertNotNull(result);
        assertEquals(testSortOrder, result.getSortOrder());
    }

    // ==================== findById Tests ====================

    @Test
    void testFindById_Success() {
        // Given
        Banners mockBanner = createTestBanner();
        mockBanner.setSortOrder(testSortOrder);

        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(testSortOrder)))
            .thenReturn(mockBanner);

        // When
        Optional<Banners> result = bannersRepository.findById(testSortOrder);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testSortOrder, result.get().getSortOrder());
        assertEquals("test-banner.jpg", result.get().getImageName());
    }

    @Test
    void testFindById_NotFound() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyInt()))
            .thenThrow(new RuntimeException("Not found"));

        // When
        Optional<Banners> result = bannersRepository.findById(testSortOrder);

        // Then
        assertFalse(result.isPresent());
    }

    // ==================== findAll Tests ====================

    @Test
    void testFindAll_Success() {
        // Given
        Banners banner1 = createTestBanner();
        banner1.setSortOrder(1);
        banner1.setImageName("banner1.jpg");

        Banners banner2 = createTestBanner();
        banner2.setSortOrder(2);
        banner2.setImageName("banner2.jpg");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(Arrays.asList(banner1, banner2));

        // When
        List<Banners> result = bannersRepository.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("banner1.jpg", result.get(0).getImageName());
        assertEquals("banner2.jpg", result.get(1).getImageName());
    }

    @Test
    void testFindAll_EmptyResult() {
        // Given
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(Collections.emptyList());

        // When
        List<Banners> result = bannersRepository.findAll();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== findActiveBanners Tests ====================

    @Test
    void testFindActiveBanners_Success() {
        // Given
        Banners banner1 = createTestBanner();
        banner1.setStatus(true);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any()))
            .thenReturn(Collections.singletonList(banner1));

        // When
        List<Banners> result = bannersRepository.findActiveBanners();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getStatus());
    }

    // ==================== count Tests ====================

    @Test
    void testCount_Success() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(10L);

        // When
        long count = bannersRepository.count();

        // Then
        assertEquals(10L, count);
    }

    @Test
    void testCount_Zero() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(0L);

        // When
        long count = bannersRepository.count();

        // Then
        assertEquals(0L, count);
    }

    // ==================== existsById Tests ====================

    @Test
    void testExistsById_True() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testSortOrder)))
            .thenReturn(1);

        // When
        boolean exists = bannersRepository.existsById(testSortOrder);

        // Then
        assertTrue(exists);
    }

    @Test
    void testExistsById_False() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testSortOrder)))
            .thenReturn(0);

        // When
        boolean exists = bannersRepository.existsById(testSortOrder);

        // Then
        assertFalse(exists);
    }

    // ==================== deleteById Tests ====================

    @Test
    void testDeleteById_Success() {
        // Given
        when(jdbcTemplate.update(anyString(), eq(testSortOrder)))
            .thenReturn(1);

        // When
        bannersRepository.deleteById(testSortOrder);

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), eq(testSortOrder));
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


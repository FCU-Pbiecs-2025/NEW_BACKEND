package Group4.Childcare.service;

import Group4.Childcare.Model.Banners;
import Group4.Childcare.Repository.BannersJdbcRepository;
import Group4.Childcare.Service.BannersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * BannersService 單元測試
 *
 * 測試範圍：
 * 1. create() - 創建橫幅
 * 2. getById() - 根據ID查詢橫幅
 * 3. getAll() - 查詢所有橫幅
 * 4. update() - 更新橫幅
 * 5. delete() - 刪除橫幅
 * 6. getBannersWithOffsetJdbc() - 分頁查詢橫幅
 * 7. getTotalCount() - 取得橫幅總數
 * 8. findActiveBanners() - 查詢活動橫幅
 */
@ExtendWith(MockitoExtension.class)
class BannersServiceTest {

    @Mock
    private BannersJdbcRepository repository;

    @InjectMocks
    private BannersService service;

    private Banners testBanner;
    private Integer testBannerId;

    @BeforeEach
    void setUp() {
        testBannerId = 1;
        testBanner = new Banners();
        testBanner.setSortOrder(testBannerId);
        testBanner.setImageName("banner1.jpg");
        testBanner.setLinkUrl("https://example.com");
        testBanner.setStatus(true);
        testBanner.setStartTime(LocalDateTime.now());
        testBanner.setEndTime(LocalDateTime.now().plusDays(30));
    }

    @Test
    void testCreate_Success() {
        // Given
        when(repository.save(any(Banners.class))).thenReturn(testBanner);

        // When
        Banners result = service.create(testBanner);

        // Then
        assertNotNull(result);
        assertEquals(testBannerId, result.getSortOrder());
        assertEquals("banner1.jpg", result.getImageName());
        assertEquals("https://example.com", result.getLinkUrl());
        verify(repository, times(1)).save(testBanner);
    }

    @Test
    void testGetById_Success() {
        // Given
        when(repository.findById(testBannerId)).thenReturn(Optional.of(testBanner));

        // When
        Optional<Banners> result = service.getById(testBannerId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testBannerId, result.get().getSortOrder());
        assertEquals("banner1.jpg", result.get().getImageName());
        verify(repository, times(1)).findById(testBannerId);
    }

    @Test
    void testGetById_NotFound() {
        // Given
        Integer nonExistentId = 999;
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When
        Optional<Banners> result = service.getById(nonExistentId);

        // Then
        assertFalse(result.isPresent());
        verify(repository, times(1)).findById(nonExistentId);
    }

    @Test
    void testGetAll_Success() {
        // Given
        Banners anotherBanner = new Banners();
        anotherBanner.setSortOrder(2);
        anotherBanner.setImageName("banner2.jpg");
        List<Banners> bannerList = Arrays.asList(testBanner, anotherBanner);
        when(repository.findAll()).thenReturn(bannerList);

        // When
        List<Banners> result = service.getAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    void testUpdate_Success() {
        // Given
        testBanner.setImageName("banner_updated.jpg");
        when(repository.save(any(Banners.class))).thenReturn(testBanner);

        // When
        Banners result = service.update(testBannerId, testBanner);

        // Then
        assertNotNull(result);
        assertEquals(testBannerId, result.getSortOrder());
        assertEquals("banner_updated.jpg", result.getImageName());
        verify(repository, times(1)).save(testBanner);
    }

    @Test
    void testDelete_Success() {
        // Given
        doNothing().when(repository).deleteById(testBannerId);

        // When
        service.delete(testBannerId);

        // Then
        verify(repository, times(1)).deleteById(testBannerId);
    }

    @Test
    void testDelete_Exception() {
        // Given
        Integer bannerId = 123;
        doThrow(new RuntimeException("Database error")).when(repository).deleteById(bannerId);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            service.delete(bannerId);
        });
        verify(repository, times(1)).deleteById(bannerId);
    }

    @Test
    void testGetBannersWithOffsetJdbc_Success() {
        // Given
        int offset = 0;
        int limit = 5;
        List<Banners> bannerList = Arrays.asList(testBanner);
        when(repository.findWithOffset(offset, limit)).thenReturn(bannerList);

        // When
        List<Banners> result = service.getBannersWithOffsetJdbc(offset, limit);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testBannerId, result.get(0).getSortOrder());
        verify(repository, times(1)).findWithOffset(offset, limit);
    }

    @Test
    void testGetBannersWithOffsetJdbc_EmptyResult() {
        // Given
        int offset = 100;
        int limit = 5;
        when(repository.findWithOffset(offset, limit)).thenReturn(Arrays.asList());

        // When
        List<Banners> result = service.getBannersWithOffsetJdbc(offset, limit);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findWithOffset(offset, limit);
    }

    @Test
    void testGetTotalCount_Success() {
        // Given
        long expectedCount = 15L;
        when(repository.count()).thenReturn(expectedCount);

        // When
        long result = service.getTotalCount();

        // Then
        assertEquals(expectedCount, result);
        verify(repository, times(1)).count();
    }

    @Test
    void testFindActiveBanners_Success() {
        // Given
        Banners activeBanner1 = new Banners();
        activeBanner1.setSortOrder(1);
        activeBanner1.setImageName("active_banner1.jpg");
        activeBanner1.setStatus(true);
        activeBanner1.setStartTime(LocalDateTime.now().minusDays(1));
        activeBanner1.setEndTime(LocalDateTime.now().plusDays(7));

        Banners activeBanner2 = new Banners();
        activeBanner2.setSortOrder(2);
        activeBanner2.setImageName("active_banner2.jpg");
        activeBanner2.setStatus(true);
        activeBanner2.setStartTime(LocalDateTime.now().minusDays(1));
        activeBanner2.setEndTime(LocalDateTime.now().plusDays(7));

        List<Banners> activeBanners = Arrays.asList(activeBanner1, activeBanner2);
        when(repository.findActiveBanners()).thenReturn(activeBanners);

        // When
        List<Banners> result = service.findActiveBanners();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(banner -> banner.getStatus() == true));
        verify(repository, times(1)).findActiveBanners();
    }

    @Test
    void testFindActiveBanners_EmptyResult() {
        // Given
        when(repository.findActiveBanners()).thenReturn(Arrays.asList());

        // When
        List<Banners> result = service.findActiveBanners();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findActiveBanners();
    }

    @Test
    void testCreate_WithInactiveStatus() {
        // Given
        testBanner.setStatus(false);
        when(repository.save(any(Banners.class))).thenReturn(testBanner);

        // When
        Banners result = service.create(testBanner);

        // Then
        assertNotNull(result);
        assertFalse(result.getStatus());
        verify(repository, times(1)).save(testBanner);
    }

    @Test
    void testUpdate_EnsuresIdIsSet() {
        // Given
        Integer newId = 5;
        Banners bannerWithoutId = new Banners();
        bannerWithoutId.setImageName("new_banner.jpg");
        when(repository.save(any(Banners.class))).thenReturn(bannerWithoutId);

        // When
        Banners result = service.update(newId, bannerWithoutId);

        // Then
        assertNotNull(result);
        // 驗證 update 方法有設置 ID
        verify(repository, times(1)).save(argThat(banner ->
            banner.getSortOrder() == newId
        ));
    }
}


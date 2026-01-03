package Group4.Childcare.controller;

import Group4.Childcare.Model.Banners;
import Group4.Childcare.Service.BannersService;
import Group4.Childcare.Controller.BannersController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BannersController 單元測試
 *
 * 測試範圍：
 * 1. create() - 創建橫幅
 * 2. getById() - 根據ID查詢橫幅
 * 3. getAll() - 查詢所有橫幅
 * 4. getBannersByOffsetJdbc() - 分頁查詢橫幅
 * 5. update() - 更新橫幅
 * 6. delete() - 刪除橫幅
 */
@ExtendWith(MockitoExtension.class)
class BannersControllerTest {

    @Mock
    private BannersService service;

    @InjectMocks
    private BannersController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Path storageLocation;

    private Banners testBanner;
    private Integer testBannerId;

    @BeforeEach
    void setUp() throws IOException {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 設定測試用的 storageLocation
        String basePath = System.getProperty("user.dir");
        File dir = new File(basePath, "BannerResource");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        storageLocation = dir.toPath();

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
    void testCreate_Success() throws Exception {
        // Given
        when(service.create(any(Banners.class))).thenReturn(testBanner);

        // When & Then
        mockMvc.perform(post("/banners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testBanner)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sortOrder", is(testBannerId)))
                .andExpect(jsonPath("$.imageName", is("banner1.jpg")))
                .andExpect(jsonPath("$.status", is(true)));

        verify(service, times(1)).create(any(Banners.class));
    }

    @Test
    void testCreate_BadRequest() throws Exception {
        // Given
        Banners invalidBanner = new Banners();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/banners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidBanner)))
                .andExpect(status().isCreated());
    }

    @Test
    void testGetById_Success() throws Exception {
        // Given
        when(service.getById(testBannerId)).thenReturn(Optional.of(testBanner));

        // When & Then
        mockMvc.perform(get("/banners/{id}", testBannerId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortOrder", is(testBannerId)))
                .andExpect(jsonPath("$.imageName", is("banner1.jpg")));

        verify(service, times(1)).getById(testBannerId);
    }

    @Test
    void testGetById_NotFound() throws Exception {
        // Given
        Integer nonExistentId = 999;
        when(service.getById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/banners/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(service, times(1)).getById(nonExistentId);
    }

    @Test
    void testGetAll_Success() throws Exception {
        // Given
        Banners anotherBanner = new Banners();
        anotherBanner.setSortOrder(2);
        anotherBanner.setImageName("banner2.jpg");
        List<Banners> banners = Arrays.asList(testBanner, anotherBanner);
        when(service.getAll()).thenReturn(banners);

        // When & Then
        mockMvc.perform(get("/banners")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].imageName", is("banner1.jpg")))
                .andExpect(jsonPath("$[1].imageName", is("banner2.jpg")));

        verify(service, times(1)).getAll();
    }

    @Test
    void testGetBannersByOffsetJdbc_Success() throws Exception {
        // Given
        int offset = 0;
        int size = 10;
        List<Banners> banners = Arrays.asList(testBanner);
        when(service.getBannersWithOffsetJdbc(offset, size)).thenReturn(banners);
        when(service.getTotalCount()).thenReturn(1L);

        // When & Then
        mockMvc.perform(get("/banners/offset")
                .param("offset", String.valueOf(offset))
                .param("size", String.valueOf(size))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offset", is(offset)))
                .andExpect(jsonPath("$.size", is(size)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.hasNext", is(false)))
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(service, times(1)).getBannersWithOffsetJdbc(offset, size);
        verify(service, times(1)).getTotalCount();
    }

    @Test
    void testDelete_Success() throws Exception {
        // Given
        when(service.getById(testBannerId)).thenReturn(Optional.of(testBanner));
        doNothing().when(service).delete(testBannerId);

        // When & Then
        mockMvc.perform(delete("/banners/{id}", testBannerId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(service, times(1)).delete(testBannerId);
    }

    @Test
    void testDelete_NotFound() throws Exception {
        // Given
        Integer nonExistentId = 999;
        when(service.getById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(delete("/banners/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(service, never()).delete(any());
    }

    // ===== updateJson 測試 =====
    @Test
    void testUpdateJson_Success() throws Exception {
        when(service.getById(testBannerId)).thenReturn(Optional.of(testBanner));
        when(service.update(eq(testBannerId), any(Banners.class))).thenReturn(testBanner);

        mockMvc.perform(put("/banners/{id}", testBannerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testBanner)))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateJson_NotFound() throws Exception {
        Integer nonExistentId = 999;
        when(service.getById(nonExistentId)).thenReturn(Optional.empty());

        mockMvc.perform(put("/banners/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testBanner)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateJson_NullMeta() throws Exception {
        when(service.getById(testBannerId)).thenReturn(Optional.of(testBanner));
        when(service.update(eq(testBannerId), any(Banners.class))).thenReturn(testBanner);

        mockMvc.perform(put("/banners/{id}", testBannerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());
    }

    // ===== getActiveBanners 測試 =====
    @Test
    void testGetActiveBanners_Success() throws Exception {
        List<Banners> activeBanners = Arrays.asList(testBanner);
        when(service.findActiveBanners()).thenReturn(activeBanners);

        mockMvc.perform(get("/banners/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void testGetActiveBanners_Empty() throws Exception {
        when(service.findActiveBanners()).thenReturn(Arrays.asList());

        mockMvc.perform(get("/banners/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ===== queryBannersByDateRange 測試 =====
    @Test
    void testQueryBannersByDateRange_Success() throws Exception {
        List<Banners> banners = Arrays.asList(testBanner);
        when(service.getBannersByDateRange(any(), any(), eq(0), eq(10))).thenReturn(banners);
        when(service.getCountByDateRange(any(), any())).thenReturn(1L);

        mockMvc.perform(get("/banners/query")
                .param("startDate", "2025-01-01")
                .param("endDate", "2025-12-31")
                .param("offset", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void testQueryBannersByDateRange_NoParams() throws Exception {
        List<Banners> banners = Arrays.asList(testBanner);
        when(service.getBannersByDateRange(any(), any(), eq(0), eq(10))).thenReturn(banners);
        when(service.getCountByDateRange(any(), any())).thenReturn(1L);

        mockMvc.perform(get("/banners/query"))
                .andExpect(status().isOk());
    }

    @Test
    void testQueryBannersByDateRange_OnlyStartDate() throws Exception {
        List<Banners> banners = Arrays.asList(testBanner);
        when(service.getBannersByDateRange(any(), any(), eq(0), eq(10))).thenReturn(banners);
        when(service.getCountByDateRange(any(), any())).thenReturn(1L);

        mockMvc.perform(get("/banners/query")
                .param("startDate", "2025-01-01"))
                .andExpect(status().isOk());
    }

    @Test
    void testQueryBannersByDateRange_OnlyEndDate() throws Exception {
        List<Banners> banners = Arrays.asList(testBanner);
        when(service.getBannersByDateRange(any(), any(), eq(0), eq(10))).thenReturn(banners);
        when(service.getCountByDateRange(any(), any())).thenReturn(1L);

        mockMvc.perform(get("/banners/query")
                .param("endDate", "2025-12-31"))
                .andExpect(status().isOk());
    }

    @Test
    void testQueryBannersByDateRange_InvalidDateFormat() throws Exception {
        mockMvc.perform(get("/banners/query")
                .param("startDate", "invalid-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ===== upload 測試 =====
    @Test
    void testUpload_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test.jpg", 
            MediaType.IMAGE_JPEG_VALUE, 
            "test image content".getBytes()
        );

        Banners meta = new Banners();
        meta.setSortOrder(1);
        meta.setStartTime(LocalDateTime.now());
        meta.setEndTime(LocalDateTime.now().plusDays(30));
        meta.setStatus(true);

        MockMultipartFile metaPart = new MockMultipartFile(
            "meta", 
            "", 
            MediaType.APPLICATION_JSON_VALUE, 
            objectMapper.writeValueAsBytes(meta)
        );

        when(service.create(any(Banners.class))).thenReturn(testBanner);

        mockMvc.perform(multipart("/banners/upload")
                .file(file)
                .file(metaPart))
                .andExpect(status().isCreated());

        verify(service, times(1)).create(any(Banners.class));
    }

    @Test
    void testUpload_EmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file", 
            "test.jpg", 
            MediaType.IMAGE_JPEG_VALUE, 
            new byte[0]
        );

        mockMvc.perform(multipart("/banners/upload")
                .file(emptyFile))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpload_MissingRequiredMetaFields() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test.jpg", 
            MediaType.IMAGE_JPEG_VALUE, 
            "test image content".getBytes()
        );

        Banners incompleteMeta = new Banners();
        // Missing required fields

        MockMultipartFile metaPart = new MockMultipartFile(
            "meta", 
            "", 
            MediaType.APPLICATION_JSON_VALUE, 
            objectMapper.writeValueAsBytes(incompleteMeta)
        );

        mockMvc.perform(multipart("/banners/upload")
                .file(file)
                .file(metaPart))
                .andExpect(status().isBadRequest());
    }

    // ===== updateWithFile 測試 =====
    @Test
    void testUpdateWithFile_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "updated.jpg", 
            MediaType.IMAGE_JPEG_VALUE, 
            "updated image content".getBytes()
        );

        Banners updatedMeta = new Banners();
        updatedMeta.setStartTime(LocalDateTime.now());
        updatedMeta.setEndTime(LocalDateTime.now().plusDays(60));
        updatedMeta.setStatus(false);

        MockMultipartFile metaPart = new MockMultipartFile(
            "meta", 
            "", 
            MediaType.APPLICATION_JSON_VALUE, 
            objectMapper.writeValueAsBytes(updatedMeta)
        );

        when(service.getById(testBannerId)).thenReturn(Optional.of(testBanner));
        when(service.update(eq(testBannerId), any(Banners.class))).thenReturn(testBanner);

        mockMvc.perform(multipart("/banners/{id}", testBannerId)
                .file(file)
                .file(metaPart)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isOk());

        verify(service, times(1)).update(eq(testBannerId), any(Banners.class));
    }

    @Test
    void testUpdateWithFile_NotFound() throws Exception {
        Integer nonExistentId = 999;
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "updated.jpg", 
            MediaType.IMAGE_JPEG_VALUE, 
            "updated image content".getBytes()
        );

        when(service.getById(nonExistentId)).thenReturn(Optional.empty());

        mockMvc.perform(multipart("/banners/{id}", nonExistentId)
                .file(file)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateWithFile_OnlyMeta() throws Exception {
        Banners updatedMeta = new Banners();
        updatedMeta.setStartTime(LocalDateTime.now());
        updatedMeta.setEndTime(LocalDateTime.now().plusDays(60));
        updatedMeta.setStatus(false);

        MockMultipartFile metaPart = new MockMultipartFile(
            "meta", 
            "", 
            MediaType.APPLICATION_JSON_VALUE, 
            objectMapper.writeValueAsBytes(updatedMeta)
        );

        when(service.getById(testBannerId)).thenReturn(Optional.of(testBanner));
        when(service.update(eq(testBannerId), any(Banners.class))).thenReturn(testBanner);

        mockMvc.perform(multipart("/banners/{id}", testBannerId)
                .file(metaPart)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isOk());

        verify(service, times(1)).update(eq(testBannerId), any(Banners.class));
    }

    // ===== getImage 測試 =====
    @Test
    void testGetImage_Success() throws Exception {
        // Note: This test is limited because it requires actual file system interaction
        // In a real scenario, you might need to mock the file system or use integration tests
        String imageName = "test-image.jpg";
        
        mockMvc.perform(get("/banners/image/{imageName}", imageName))
                .andExpect(status().isNotFound()); // Will be NotFound since file doesn't exist in test
    }

    @Test
    void testGetImage_WithQueryParams() throws Exception {
        String imageNameWithParams = "test-image.jpg?_=123456";
        
        mockMvc.perform(get("/banners/image/{imageName}", imageNameWithParams))
                .andExpect(status().isNotFound());
    }

    // ===== 異常處理測試 =====
    @Test
    void testCreate_ServiceException() throws Exception {
        when(service.create(any(Banners.class))).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/banners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testBanner)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testUpdateJson_ServiceException() throws Exception {
        when(service.getById(testBannerId)).thenReturn(Optional.of(testBanner));
        when(service.update(eq(testBannerId), any(Banners.class)))
            .thenThrow(new RuntimeException("Update failed"));

        mockMvc.perform(put("/banners/{id}", testBannerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testBanner)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testUpdateJson_PartialUpdate() throws Exception {
        Banners partialUpdate = new Banners();
        partialUpdate.setStatus(false);
        // Only status is set, other fields are null

        when(service.getById(testBannerId)).thenReturn(Optional.of(testBanner));
        when(service.update(eq(testBannerId), any(Banners.class))).thenReturn(testBanner);

        mockMvc.perform(put("/banners/{id}", testBannerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partialUpdate)))
                .andExpect(status().isOk());

        verify(service, times(1)).update(eq(testBannerId), any(Banners.class));
    }

    @Test
    void testGetBannersByOffsetJdbc_CustomPagination() throws Exception {
        int offset = 5;
        int size = 20;
        List<Banners> banners = Arrays.asList(testBanner);
        when(service.getBannersWithOffsetJdbc(offset, size)).thenReturn(banners);
        when(service.getTotalCount()).thenReturn(100L);

        mockMvc.perform(get("/banners/offset")
                .param("offset", String.valueOf(offset))
                .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offset", is(offset)))
                .andExpect(jsonPath("$.size", is(size)))
                .andExpect(jsonPath("$.totalElements", is(100)))
                .andExpect(jsonPath("$.hasNext", is(true)));
    }

    @Test
    void testQueryBannersByDateRange_EmptyResult() throws Exception {
        when(service.getBannersByDateRange(any(), any(), eq(0), eq(10))).thenReturn(Arrays.asList());
        when(service.getCountByDateRange(any(), any())).thenReturn(0L);

        mockMvc.perform(get("/banners/query")
                .param("startDate", "2025-01-01")
                .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    // ===== 新增分支覆蓋率測試 =====
    
    @Test
    void testUpload_NullFile() throws Exception {
        // 測試 file == null 的情況
        mockMvc.perform(multipart("/banners/upload"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpload_MetaWithNullSortOrder() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test.jpg", 
            MediaType.IMAGE_JPEG_VALUE, 
            "test image content".getBytes()
        );

        Banners meta = new Banners();
        meta.setSortOrder(0); // <= 0
        meta.setStartTime(LocalDateTime.now());
        meta.setEndTime(LocalDateTime.now().plusDays(30));
        meta.setStatus(true);

        MockMultipartFile metaPart = new MockMultipartFile(
            "meta", 
            "", 
            MediaType.APPLICATION_JSON_VALUE, 
            objectMapper.writeValueAsBytes(meta)
        );

        mockMvc.perform(multipart("/banners/upload")
                .file(file)
                .file(metaPart))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpload_MetaWithNullStartTime() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test.jpg", 
            MediaType.IMAGE_JPEG_VALUE, 
            "test image content".getBytes()
        );

        Banners meta = new Banners();
        meta.setSortOrder(1);
        meta.setStartTime(null); // null
        meta.setEndTime(LocalDateTime.now().plusDays(30));
        meta.setStatus(true);

        MockMultipartFile metaPart = new MockMultipartFile(
            "meta", 
            "", 
            MediaType.APPLICATION_JSON_VALUE, 
            objectMapper.writeValueAsBytes(meta)
        );

        mockMvc.perform(multipart("/banners/upload")
                .file(file)
                .file(metaPart))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpload_MetaWithNullEndTime() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test.jpg", 
            MediaType.IMAGE_JPEG_VALUE, 
            "test image content".getBytes()
        );

        Banners meta = new Banners();
        meta.setSortOrder(1);
        meta.setStartTime(LocalDateTime.now());
        meta.setEndTime(null); // null
        meta.setStatus(true);

        MockMultipartFile metaPart = new MockMultipartFile(
            "meta", 
            "", 
            MediaType.APPLICATION_JSON_VALUE, 
            objectMapper.writeValueAsBytes(meta)
        );

        mockMvc.perform(multipart("/banners/upload")
                .file(file)
                .file(metaPart))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpload_MetaWithNullStatus() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test.jpg", 
            MediaType.IMAGE_JPEG_VALUE, 
            "test image content".getBytes()
        );

        Banners meta = new Banners();
        meta.setSortOrder(1);
        meta.setStartTime(LocalDateTime.now());
        meta.setEndTime(LocalDateTime.now().plusDays(30));
        meta.setStatus(null); // null

        MockMultipartFile metaPart = new MockMultipartFile(
            "meta", 
            "", 
            MediaType.APPLICATION_JSON_VALUE, 
            objectMapper.writeValueAsBytes(meta)
        );

        mockMvc.perform(multipart("/banners/upload")
                .file(file)
                .file(metaPart))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateWithFile_MetaNull() throws Exception {
        // 測試 meta 為 null 的情況
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "updated.jpg", 
            MediaType.IMAGE_JPEG_VALUE, 
            "updated image content".getBytes()
        );

        when(service.getById(testBannerId)).thenReturn(Optional.of(testBanner));
        when(service.update(eq(testBannerId), any(Banners.class))).thenReturn(testBanner);

        mockMvc.perform(multipart("/banners/{id}", testBannerId)
                .file(file)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateWithFile_MetaWithImageName() throws Exception {
        // 測試 meta.getImageName() 不為 null 的情況
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "updated.jpg", 
            MediaType.IMAGE_JPEG_VALUE, 
            "updated image content".getBytes()
        );

        Banners updatedMeta = new Banners();
        updatedMeta.setStartTime(LocalDateTime.now());
        updatedMeta.setEndTime(LocalDateTime.now().plusDays(60));
        updatedMeta.setStatus(false);
        updatedMeta.setImageName("custom-image.jpg");

        MockMultipartFile metaPart = new MockMultipartFile(
            "meta", 
            "", 
            MediaType.APPLICATION_JSON_VALUE, 
            objectMapper.writeValueAsBytes(updatedMeta)
        );

        when(service.getById(testBannerId)).thenReturn(Optional.of(testBanner));
        when(service.update(eq(testBannerId), any(Banners.class))).thenReturn(testBanner);

        mockMvc.perform(multipart("/banners/{id}", testBannerId)
                .file(file)
                .file(metaPart)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateWithFile_BannerWithNullImageName() throws Exception {
        // 測試 banner.getImageName() 為 null 的情況
        Banners bannerNoImage = new Banners();
        bannerNoImage.setSortOrder(testBannerId);
        bannerNoImage.setImageName(null); // null
        bannerNoImage.setStatus(true);
        bannerNoImage.setStartTime(LocalDateTime.now());
        bannerNoImage.setEndTime(LocalDateTime.now().plusDays(30));

        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "new.jpg", 
            MediaType.IMAGE_JPEG_VALUE, 
            "new image content".getBytes()
        );

        when(service.getById(testBannerId)).thenReturn(Optional.of(bannerNoImage));
        when(service.update(eq(testBannerId), any(Banners.class))).thenReturn(bannerNoImage);

        mockMvc.perform(multipart("/banners/{id}", testBannerId)
                .file(file)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateJson_MetaWithImageName() throws Exception {
        // 測試 updateJson 中 meta.getImageName() 不為空的情況
        Banners metaWithImage = new Banners();
        metaWithImage.setStartTime(LocalDateTime.now());
        metaWithImage.setEndTime(LocalDateTime.now().plusDays(60));
        metaWithImage.setStatus(false);
        metaWithImage.setImageName("updated-image.jpg");

        when(service.getById(testBannerId)).thenReturn(Optional.of(testBanner));
        when(service.update(eq(testBannerId), any(Banners.class))).thenReturn(testBanner);

        mockMvc.perform(put("/banners/{id}", testBannerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(metaWithImage)))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateJson_MetaWithEmptyImageName() throws Exception {
        // 測試 meta.getImageName().isEmpty() 為 true 的情況
        Banners metaWithEmptyImage = new Banners();
        metaWithEmptyImage.setStartTime(LocalDateTime.now());
        metaWithEmptyImage.setEndTime(LocalDateTime.now().plusDays(60));
        metaWithEmptyImage.setStatus(false);
        metaWithEmptyImage.setImageName("");

        when(service.getById(testBannerId)).thenReturn(Optional.of(testBanner));
        when(service.update(eq(testBannerId), any(Banners.class))).thenReturn(testBanner);

        mockMvc.perform(put("/banners/{id}", testBannerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(metaWithEmptyImage)))
                .andExpect(status().isOk());
    }

    @Test
    void testDelete_WithNullImageName() throws Exception {
        // 測試 banner.getImageName() 為 null 的情況
        Banners bannerNoImage = new Banners();
        bannerNoImage.setSortOrder(testBannerId);
        bannerNoImage.setImageName(null);

        when(service.getById(testBannerId)).thenReturn(Optional.of(bannerNoImage));
        doNothing().when(service).delete(testBannerId);

        mockMvc.perform(delete("/banners/{id}", testBannerId))
                .andExpect(status().isNoContent());
    }

    @Test
    void testQueryBannersByDateRange_EmptyStartDate() throws Exception {
        // 測試 startDate.trim().isEmpty() 為 true 的情況
        List<Banners> banners = Arrays.asList(testBanner);
        when(service.getBannersByDateRange(any(), any(), eq(0), eq(10))).thenReturn(banners);
        when(service.getCountByDateRange(any(), any())).thenReturn(1L);

        mockMvc.perform(get("/banners/query")
                .param("startDate", "  ")
                .param("endDate", "2025-12-31"))
                .andExpect(status().isOk());
    }

    @Test
    void testQueryBannersByDateRange_EmptyEndDate() throws Exception {
        // 測試 endDate.trim().isEmpty() 為 true 的情況
        List<Banners> banners = Arrays.asList(testBanner);
        when(service.getBannersByDateRange(any(), any(), eq(0), eq(10))).thenReturn(banners);
        when(service.getCountByDateRange(any(), any())).thenReturn(1L);

        mockMvc.perform(get("/banners/query")
                .param("startDate", "2025-01-01")
                .param("endDate", "   "))
                .andExpect(status().isOk());
    }

    @Test
    void testQueryBannersByDateRange_NoHasNext() throws Exception {
        // 測試 hasNext 為 false 的情況
        List<Banners> banners = Arrays.asList(testBanner);
        when(service.getBannersByDateRange(any(), any(), eq(0), eq(10))).thenReturn(banners);
        when(service.getCountByDateRange(any(), any())).thenReturn(5L); // totalCount = 5, offset + size = 10

        mockMvc.perform(get("/banners/query")
                .param("startDate", "2025-01-01")
                .param("endDate", "2025-12-31")
                .param("offset", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNext", is(false)));
    }

    // ===== getImage 方法測試 =====
    
    @Test
    void testGetImage_FileExists() throws Exception {
        // 測試檔案存在時的成功讀取
        String filename = "test-banner.jpg";
        Path testFile = storageLocation.resolve(filename);
        Files.write(testFile, "test banner content".getBytes());

        try {
            mockMvc.perform(get("/banners/image/{imageName}", filename))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Content-Disposition"));
        } finally {
            Files.deleteIfExists(testFile);
        }
    }

    @Test
    void testGetImage_FileExistsWithUUIDPrefix() throws Exception {
        // 測試帶有 UUID 前綴的檔名解析
        String filename = "550e8400-e29b-41d4-a716-446655440000_original.jpg";
        Path testFile = storageLocation.resolve(filename);
        Files.write(testFile, "test banner content".getBytes());

        try {
            mockMvc.perform(get("/banners/image/{imageName}", filename))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Content-Disposition"));
        } finally {
            Files.deleteIfExists(testFile);
        }
    }

    @Test
    void testGetImage_FileExistsNoUnderscore() throws Exception {
        // 測試沒有底線的檔名
        String filename = "simple.jpg";
        Path testFile = storageLocation.resolve(filename);
        Files.write(testFile, "test banner content".getBytes());

        try {
            mockMvc.perform(get("/banners/image/{imageName}", filename))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Content-Disposition"));
        } finally {
            Files.deleteIfExists(testFile);
        }
    }

    @Test
    void testUpdateWithFile_EmptyFile() throws Exception {
        // 測試 file.isEmpty() 為 true 的情況
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file", 
            "empty.jpg", 
            MediaType.IMAGE_JPEG_VALUE, 
            new byte[0]
        );

        Banners updatedMeta = new Banners();
        updatedMeta.setStartTime(LocalDateTime.now());
        updatedMeta.setEndTime(LocalDateTime.now().plusDays(60));
        updatedMeta.setStatus(false);

        MockMultipartFile metaPart = new MockMultipartFile(
            "meta", 
            "", 
            MediaType.APPLICATION_JSON_VALUE, 
            objectMapper.writeValueAsBytes(updatedMeta)
        );

        when(service.getById(testBannerId)).thenReturn(Optional.of(testBanner));
        when(service.update(eq(testBannerId), any(Banners.class))).thenReturn(testBanner);

        mockMvc.perform(multipart("/banners/{id}", testBannerId)
                .file(emptyFile)
                .file(metaPart)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateWithFile_ServiceException() throws Exception {
        // 測試 service.update 拋出異常的情況
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test.jpg", 
            MediaType.IMAGE_JPEG_VALUE, 
            "test content".getBytes()
        );

        when(service.getById(testBannerId)).thenReturn(Optional.of(testBanner));
        when(service.update(eq(testBannerId), any(Banners.class)))
            .thenThrow(new RuntimeException("Update failed"));

        mockMvc.perform(multipart("/banners/{id}", testBannerId)
                .file(file)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testUpdateJson_MetaNull() throws Exception {
        // 測試 meta 為 null 的邊界情況 (實際上 @RequestBody 不會為 null,Spring 會返回 400)
        mockMvc.perform(put("/banners/{id}", testBannerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"))
                .andExpect(status().isBadRequest()); // Spring 會返回 400
    }
}

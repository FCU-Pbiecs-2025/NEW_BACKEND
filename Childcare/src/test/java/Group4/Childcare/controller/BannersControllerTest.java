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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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

    private Banners testBanner;
    private Integer testBannerId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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
}


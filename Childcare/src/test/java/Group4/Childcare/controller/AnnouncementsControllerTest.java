package Group4.Childcare.controller;

import Group4.Childcare.Model.Announcements;
import Group4.Childcare.DTO.AnnouncementSummaryDTO;
import Group4.Childcare.Service.AnnouncementsService;
import Group4.Childcare.Controller.AnnouncementsController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import java.time.LocalDate;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AnnouncementsController 單元測試
 *
 * 測試範圍：
 * 1. getById() - 根據ID查詢公告
 * 2. getAll() - 查詢所有公告
 * 3. getAnnouncementsByOffsetJdbc() - 分頁查詢公告
 * 4. update() - 更新公告
 * 5. getSummary() - 查詢公告摘要
 * 6. getAdminActiveBackend() - 查詢後台活動公告
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementsControllerTest {

    @Mock
    private AnnouncementsService service;

    @InjectMocks
    private AnnouncementsController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private Announcements testAnnouncement;
    private UUID testAnnouncementId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        testAnnouncementId = UUID.randomUUID();
        testAnnouncement = new Announcements();
        testAnnouncement.setAnnouncementID(testAnnouncementId);
        testAnnouncement.setTitle("測試公告");
        testAnnouncement.setContent("測試內容");
        testAnnouncement.setType((byte) 1);
        testAnnouncement.setStatus((byte) 1);
        testAnnouncement.setStartDate(LocalDate.now());
        testAnnouncement.setEndDate(LocalDate.now().plusDays(7));
    }

    @Test
    void testGetById_Success() throws Exception {
        // Given
        when(service.getById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));

        // When & Then
        mockMvc.perform(get("/announcements/{id}", testAnnouncementId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.announcementID", is(testAnnouncementId.toString())))
                .andExpect(jsonPath("$.title", is("測試公告")))
                .andExpect(jsonPath("$.content", is("測試內容")));

        verify(service, times(1)).getById(testAnnouncementId);
    }

    @Test
    void testGetById_NotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(service.getById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/announcements/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(service, times(1)).getById(nonExistentId);
    }

    @Test
    void testGetAll_Success() throws Exception {
        // Given
        List<Announcements> announcements = Arrays.asList(testAnnouncement);
        when(service.getAll()).thenReturn(announcements);

        // When & Then
        mockMvc.perform(get("/announcements")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("測試公告")));

        verify(service, times(1)).getAll();
    }

    @Test
    void testGetAll_EmptyList() throws Exception {
        // Given
        when(service.getAll()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/announcements")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(service, times(1)).getAll();
    }

    @Test
    void testGetAnnouncementsByOffsetJdbc_Success() throws Exception {
        // Given
        int offset = 0;
        List<Announcements> announcements = Arrays.asList(testAnnouncement);
        when(service.getAnnouncementsWithOffsetJdbc(offset)).thenReturn(announcements);
        when(service.getTotalCount()).thenReturn(1L);

        // When & Then
        mockMvc.perform(get("/announcements/offset")
                .param("offset", String.valueOf(offset))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offset", is(offset)))
                .andExpect(jsonPath("$.size", is(8)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.hasNext", is(false)))
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(service, times(1)).getAnnouncementsWithOffsetJdbc(offset);
        verify(service, times(1)).getTotalCount();
    }

    @Test
    void testUpdate_Success() throws Exception {
        // Given
        testAnnouncement.setTitle("更新後的標題");
        when(service.update(eq(testAnnouncementId), any(Announcements.class))).thenReturn(testAnnouncement);

        // When & Then
        mockMvc.perform(put("/announcements/{id}", testAnnouncementId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAnnouncement)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("更新後的標題")));

        verify(service, times(1)).update(eq(testAnnouncementId), any(Announcements.class));
    }

    @Test
    void testGetSummary_Success() throws Exception {
        // Given
        AnnouncementSummaryDTO summaryDTO = new AnnouncementSummaryDTO(
                testAnnouncementId,
                "測試公告",
                "測試內容",
                LocalDate.now()
        );
        List<AnnouncementSummaryDTO> summaries = Arrays.asList(summaryDTO);
        when(service.getSummaryAll()).thenReturn(summaries);

        // When & Then
        mockMvc.perform(get("/announcements/summary")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("測試公告")));

        verify(service, times(1)).getSummaryAll();
    }

    @Test
    void testGetAdminActiveBackend_Success() throws Exception {
        // Given
        AnnouncementSummaryDTO summaryDTO = new AnnouncementSummaryDTO(
                testAnnouncementId,
                "活動公告",
                "活動內容",
                LocalDate.now()
        );
        List<AnnouncementSummaryDTO> activeList = Arrays.asList(summaryDTO);
        when(service.getAdminActiveBackend()).thenReturn(activeList);

        // When & Then
        mockMvc.perform(get("/announcements/active/backend")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("活動公告")));

        verify(service, times(1)).getAdminActiveBackend();
    }
}


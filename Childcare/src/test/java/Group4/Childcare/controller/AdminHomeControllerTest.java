package Group4.Childcare.controller;

import Group4.Childcare.Controller.AdminHomeController;
import Group4.Childcare.DTO.AnnouncementSummaryDTO;
import Group4.Childcare.Repository.AnnouncementsJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminHomeController 單元測試
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AdminHomeControllerTest {

        @Mock
        private JdbcTemplate jdbcTemplate;

        @Mock
        private AnnouncementsJdbcRepository announcementsJdbcRepository;

        @InjectMocks
        private AdminHomeController controller;

        private MockMvc mockMvc;
        private UUID testInstitutionId;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
                testInstitutionId = UUID.randomUUID();
        }

        @Test
        void testGetAdminAnnouncements_Success() throws Exception {
                List<AnnouncementSummaryDTO> announcements = new ArrayList<>();
                AnnouncementSummaryDTO dto = new AnnouncementSummaryDTO();
                dto.setTitle("測試公告");
                announcements.add(dto);

                when(announcementsJdbcRepository.findAdminActiveSummaries()).thenReturn(announcements);

                mockMvc.perform(get("/adminhome/announcements")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)));

                verify(announcementsJdbcRepository, times(1)).findAdminActiveSummaries();
        }

        @Test
        void testGetAdminAnnouncements_Empty() throws Exception {
                when(announcementsJdbcRepository.findAdminActiveSummaries()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/adminhome/announcements")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void testGetTodoCounts_WithInstitutionId() throws Exception {
                when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("審核中"),
                                org.mockito.ArgumentMatchers.any(UUID.class)))
                                .thenReturn(5);
                when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("撤銷申請審核中"),
                                org.mockito.ArgumentMatchers.any(UUID.class)))
                                .thenReturn(3);

                mockMvc.perform(get("/adminhome/todo-counts")
                                .param("InstitutionID", testInstitutionId.toString())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.pending", is(5)))
                                .andExpect(jsonPath("$.revoke", is(3)));
        }

        @Test
        void testGetTodoCounts_WithoutInstitutionId() throws Exception {
                when(jdbcTemplate.queryForObject(contains("WHERE Status = ?"), eq(Integer.class), eq("審核中")))
                                .thenReturn(10);
                when(jdbcTemplate.queryForObject(contains("WHERE Status = ?"), eq(Integer.class), eq("撤銷申請審核中")))
                                .thenReturn(2);

                mockMvc.perform(get("/adminhome/todo-counts")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.pending", is(10)))
                                .andExpect(jsonPath("$.revoke", is(2)));
        }

        @Test
        void testGetTodoCounts_EmptyInstitutionId() throws Exception {
                when(jdbcTemplate.queryForObject(contains("WHERE Status = ?"), eq(Integer.class), eq("審核中")))
                                .thenReturn(0);
                when(jdbcTemplate.queryForObject(contains("WHERE Status = ?"), eq(Integer.class), eq("撤銷申請審核中")))
                                .thenReturn(0);

                mockMvc.perform(get("/adminhome/todo-counts")
                                .param("InstitutionID", "  ")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.pending", is(0)))
                                .andExpect(jsonPath("$.revoke", is(0)));
        }

        @Test
        void testGetTodoCounts_NullResults() throws Exception {
                when(jdbcTemplate.queryForObject(contains("WHERE Status = ?"), eq(Integer.class), eq("審核中")))
                                .thenReturn(null);
                when(jdbcTemplate.queryForObject(contains("WHERE Status = ?"), eq(Integer.class), eq("撤銷申請審核中")))
                                .thenReturn(null);

                mockMvc.perform(get("/adminhome/todo-counts")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.pending", is(0)))
                                .andExpect(jsonPath("$.revoke", is(0)));
        }
}

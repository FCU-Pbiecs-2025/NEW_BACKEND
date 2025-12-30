package Group4.Childcare.controller;

import Group4.Childcare.Controller.CancellationController;
import Group4.Childcare.Model.Cancellation;
import Group4.Childcare.Service.CancellationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CancellationController 單元測試
 */
@ExtendWith(MockitoExtension.class)
class CancellationControllerTest {

    @Mock
    private CancellationService service;

    @InjectMocks
    private CancellationController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Cancellation testCancellation;
    private UUID testId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();

        testId = UUID.randomUUID();
        testCancellation = new Cancellation();
        testCancellation.setCancellationID(testId);
    }

    @Test
    void testCreate_Success() throws Exception {
        when(service.create(any(Cancellation.class))).thenReturn(testCancellation);

        mockMvc.perform(post("/cancellation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCancellation)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancellationID", is(testId.toString())));

        verify(service, times(1)).create(any(Cancellation.class));
    }

    @Test
    void testGetById_Success() throws Exception {
        when(service.getById(testId)).thenReturn(Optional.of(testCancellation));

        mockMvc.perform(get("/cancellation/{id}", testId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancellationID", is(testId.toString())));

        verify(service, times(1)).getById(testId);
    }

    @Test
    void testGetById_NotFound() throws Exception {
        when(service.getById(testId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/cancellation/{id}", testId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(service, times(1)).getById(testId);
    }

    @Test
    void testGetAll_Success() throws Exception {
        List<Cancellation> list = Arrays.asList(testCancellation);
        when(service.getAll()).thenReturn(list);

        mockMvc.perform(get("/cancellation")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(service, times(1)).getAll();
    }

    @Test
    void testGetAll_Empty() throws Exception {
        when(service.getAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/cancellation")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testUpdate_Success() throws Exception {
        when(service.update(eq(testId), any(Cancellation.class))).thenReturn(testCancellation);

        mockMvc.perform(put("/cancellation/{id}", testId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCancellation)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancellationID", is(testId.toString())));

        verify(service, times(1)).update(eq(testId), any(Cancellation.class));
    }
}

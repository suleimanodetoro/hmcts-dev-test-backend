package uk.gov.hmcts.reform.dev.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.dev.dto.CreateTaskRequest;
import uk.gov.hmcts.reform.dev.dto.UpdateTaskStatusRequest;
import uk.gov.hmcts.reform.dev.exceptions.TaskNotFoundException;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.models.TaskStatus;
import uk.gov.hmcts.reform.dev.services.TaskService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private transient MockMvc mockMvc;

    @MockitoBean
    private transient TaskService taskService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private Task sampleTask() {
        return Task.builder()
            .id(1L)
            .title("Prepare bundle")
            .description("Prepare hearing bundle")
            .status(TaskStatus.PENDING)
            .dueDateTime(LocalDateTime.of(2026, 6, 1, 9, 0))
            .createdAt(LocalDateTime.of(2026, 5, 1, 10, 0))
            .updatedAt(LocalDateTime.of(2026, 5, 1, 10, 0))
            .build();
    }

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
            .title("Prepare bundle")
            .description("Prepare hearing bundle")
            .status(TaskStatus.PENDING)
            .dueDateTime(LocalDateTime.of(2026, 6, 1, 9, 0))
            .build();
        when(taskService.create(any(CreateTaskRequest.class))).thenReturn(sampleTask());

        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "http://localhost/tasks/1"))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Prepare bundle"))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createReturns400WhenTitleBlank() throws Exception {
        String body = """
            {"title":"   ","status":"PENDING","dueDateTime":"2026-06-01T09:00:00"}""";

        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[?(@.field=='title')]").exists());
    }

    @Test
    void createReturns400WhenStatusInvalid() throws Exception {
        String body = """
            {"title":"x","status":"BOGUS","dueDateTime":"2026-06-01T09:00:00"}""";

        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("status")));
    }

    @Test
    void listReturnsTasks() throws Exception {
        when(taskService.findAll()).thenReturn(List.of(sampleTask()));

        mockMvc.perform(get("/tasks"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getReturnsTaskWhenFound() throws Exception {
        when(taskService.findById(1L)).thenReturn(sampleTask());

        mockMvc.perform(get("/tasks/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getReturns404WhenMissing() throws Exception {
        when(taskService.findById(99L)).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(get("/tasks/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("99")));
    }

    @Test
    void updateStatusReturnsUpdatedTask() throws Exception {
        Task updated = sampleTask();
        updated.setStatus(TaskStatus.IN_PROGRESS);
        when(taskService.updateStatus(eq(1L), eq(TaskStatus.IN_PROGRESS))).thenReturn(updated);

        UpdateTaskStatusRequest req = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);
        mockMvc.perform(patch("/tasks/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void updateStatusReturns400WhenMissingStatus() throws Exception {
        mockMvc.perform(patch("/tasks/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[?(@.field=='status')]").exists());
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/tasks/1"))
            .andExpect(status().isNoContent());
        verify(taskService).delete(1L);
    }
}

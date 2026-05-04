package uk.gov.hmcts.reform.dev;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.dev.dto.CreateTaskRequest;
import uk.gov.hmcts.reform.dev.dto.UpdateTaskStatusRequest;
import uk.gov.hmcts.reform.dev.models.TaskStatus;
import uk.gov.hmcts.reform.dev.repositories.TaskRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskApiIntegrationTest {

    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    private transient TaskRepository taskRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void clearDatabase() {
        taskRepository.deleteAll();
    }

    @Test
    void fullTaskLifecycle() throws Exception {
        CreateTaskRequest create = CreateTaskRequest.builder()
            .title("File hearing bundle")
            .description("Send to court office by 9am")
            .status(TaskStatus.PENDING)
            .dueDateTime(LocalDateTime.of(2026, 6, 1, 9, 0))
            .build();

        MvcResult created = mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(create)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andReturn();

        Long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/tasks/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("File hearing bundle"))
            .andExpect(jsonPath("$.status").value("PENDING"));

        UpdateTaskStatusRequest update = new UpdateTaskStatusRequest(TaskStatus.COMPLETED);
        mockMvc.perform(patch("/tasks/" + id + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("COMPLETED"));

        mockMvc.perform(delete("/tasks/" + id))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/tasks/" + id))
            .andExpect(status().isNotFound());

        assertThat(taskRepository.count()).isZero();
    }

    @Test
    void rejectsInvalidPayload() throws Exception {
        String body = """
            {"title":"","status":"PENDING","dueDateTime":"2026-06-01T09:00:00"}""";

        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[?(@.field=='title')]").exists());
    }

    @Test
    void returns404ForUnknownTask() throws Exception {
        mockMvc.perform(get("/tasks/9999"))
            .andExpect(status().isNotFound());
    }
}

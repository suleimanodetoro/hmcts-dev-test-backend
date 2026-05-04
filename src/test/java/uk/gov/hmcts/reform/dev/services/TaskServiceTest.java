package uk.gov.hmcts.reform.dev.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.reform.dev.dto.CreateTaskRequest;
import uk.gov.hmcts.reform.dev.exceptions.TaskNotFoundException;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.models.TaskStatus;
import uk.gov.hmcts.reform.dev.repositories.TaskRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private CreateTaskRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = CreateTaskRequest.builder()
            .title("  Review case bundle  ")
            .description("Review the bundle for hearing")
            .status(TaskStatus.PENDING)
            .dueDateTime(LocalDateTime.of(2026, 6, 1, 9, 0))
            .build();
    }

    @Test
    void createTrimsTitleAndPersistsTask() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task result = taskService.create(validRequest);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Review case bundle");
        assertThat(saved.getDescription()).isEqualTo("Review the bundle for hearing");
        assertThat(saved.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(saved.getDueDateTime()).isEqualTo(LocalDateTime.of(2026, 6, 1, 9, 0));
        assertThat(result).isSameAs(saved);
    }

    @Test
    void findAllReturnsTasksOrderedByDueDateTime() {
        Task task = Task.builder().id(1L).title("a").status(TaskStatus.PENDING)
            .dueDateTime(LocalDateTime.now()).build();
        when(taskRepository.findAll(any(Sort.class))).thenReturn(List.of(task));

        List<Task> result = taskService.findAll();

        assertThat(result).containsExactly(task);
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(taskRepository).findAll(sortCaptor.capture());
        assertThat(sortCaptor.getValue().getOrderFor("dueDateTime")).isNotNull();
    }

    @Test
    void findByIdReturnsTaskWhenPresent() {
        Task task = Task.builder().id(7L).status(TaskStatus.PENDING).title("t")
            .dueDateTime(LocalDateTime.now()).build();
        when(taskRepository.findById(7L)).thenReturn(Optional.of(task));

        Task result = taskService.findById(7L);

        assertThat(result).isSameAs(task);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(99L))
            .isInstanceOf(TaskNotFoundException.class)
            .hasMessageContaining("99");
    }

    @Test
    void updateStatusReplacesStatusAndSaves() {
        Task task = Task.builder().id(3L).status(TaskStatus.PENDING).title("t")
            .dueDateTime(LocalDateTime.now()).build();
        when(taskRepository.findById(3L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task updated = taskService.updateStatus(3L, TaskStatus.IN_PROGRESS);

        assertThat(updated.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        verify(taskRepository).save(task);
    }

    @Test
    void updateStatusThrowsWhenTaskMissing() {
        when(taskRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateStatus(404L, TaskStatus.COMPLETED))
            .isInstanceOf(TaskNotFoundException.class);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void deleteRemovesWhenPresent() {
        when(taskRepository.existsById(5L)).thenReturn(true);

        taskService.delete(5L);

        verify(taskRepository, times(1)).deleteById(5L);
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(taskRepository.existsById(5L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.delete(5L))
            .isInstanceOf(TaskNotFoundException.class);
        verify(taskRepository, never()).deleteById(any());
    }
}

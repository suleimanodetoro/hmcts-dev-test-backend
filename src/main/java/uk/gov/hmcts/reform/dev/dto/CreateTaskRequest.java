package uk.gov.hmcts.reform.dev.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.dev.models.TaskStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTaskRequest {

    @NotBlank(message = "title must not be blank")
    @Size(max = 200, message = "title must be 200 characters or fewer")
    private String title;

    @Size(max = 2000, message = "description must be 2000 characters or fewer")
    private String description;

    @NotNull(message = "status is required")
    private TaskStatus status;

    @NotNull(message = "dueDateTime is required")
    private LocalDateTime dueDateTime;
}

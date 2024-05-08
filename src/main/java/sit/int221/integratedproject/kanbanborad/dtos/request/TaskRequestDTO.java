package sit.int221.integratedproject.kanbanborad.dtos.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequestDTO {
    @NotNull(message = "Title can not be null")
    @Size(min = 1, max = 100)
    private String title;
    @Size(min = 1, max = 500)
    private String description;
    @Size(min = 1, max = 30)
    private String assignees;
    @NotNull(message = "Status can not be null")
    private String status;
}
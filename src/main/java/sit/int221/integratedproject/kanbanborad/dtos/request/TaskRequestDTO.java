package sit.int221.integratedproject.kanbanborad.dtos.request;

import jakarta.validation.constraints.NotBlank;
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
    @NotBlank
    private String title;
    @Size(min = 1, max = 500)
    @NotBlank
    private String description;
    @Size(min = 1, max = 30)
    @NotBlank
    private String assignees;
    @NotNull(message = "Status can not be null")
    private Integer status;
}
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
public class TaskUpdateRequestDTO {
    @NotNull
    @Size(min = 0, max = 100)
    @NotBlank
    private String title;
    @Size(min = 0, max = 500)
    private String description;
    @Size(min = 0, max = 30)
    private String assignees;
    @NotNull
    private Integer status;
}

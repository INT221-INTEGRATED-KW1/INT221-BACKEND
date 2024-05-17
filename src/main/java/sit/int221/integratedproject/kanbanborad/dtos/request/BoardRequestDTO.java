package sit.int221.integratedproject.kanbanborad.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BoardRequestDTO {
    private Boolean limitMaximumTask;
}

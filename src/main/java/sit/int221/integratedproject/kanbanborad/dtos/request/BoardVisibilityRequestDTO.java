package sit.int221.integratedproject.kanbanborad.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BoardVisibilityRequestDTO {
    @NotNull
    @NotBlank
    private String visibility;
}

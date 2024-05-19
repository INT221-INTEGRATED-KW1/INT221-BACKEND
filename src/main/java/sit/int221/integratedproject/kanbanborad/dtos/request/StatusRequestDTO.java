package sit.int221.integratedproject.kanbanborad.dtos.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StatusRequestDTO {
    @NotNull
    @Size(min = 1, max = 50)
    @Column(unique = true)
    private String name;
    @Size(min = 1, max = 200)
    private String description;
    @Size(min = 1, max = 20)
    private String color;
}

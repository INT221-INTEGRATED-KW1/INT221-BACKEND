package sit.int221.integratedproject.kanbanborad.dtos.response;

import lombok.Data;

@Data
public class StatusResponseDTO {
    private Integer id;
    private String name;
    private String description;
    private Boolean limitMaximumTask;
    private String color;
}

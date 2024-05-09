package sit.int221.integratedproject.kanbanborad.dtos.response;

import lombok.Data;

@Data
public class StatusAddEditResponseDTO {
    private Integer id;
    private String name;
    private String description;
    private String color;
}

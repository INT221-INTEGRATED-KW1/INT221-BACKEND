package sit.int221.integratedproject.kanbanborad.dtos.response;

import lombok.Data;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Status;

@Data
public class TaskAddEditResponseDTO {
    private Integer id;
    private String title;
    private String description;
    private String assignees;
    private Status status;
}

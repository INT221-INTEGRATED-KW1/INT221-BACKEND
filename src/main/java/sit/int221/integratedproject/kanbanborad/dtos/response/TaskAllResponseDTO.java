package sit.int221.integratedproject.kanbanborad.dtos.response;

import lombok.Data;
import sit.int221.integratedproject.kanbanborad.models.Status;

@Data
public class TaskAllResponseDTO {
    private Integer id;
    private String title;
    private String assignees;
    private Status status;
}

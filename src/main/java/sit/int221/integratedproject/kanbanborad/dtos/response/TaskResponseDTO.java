package sit.int221.integratedproject.kanbanborad.dtos.response;

import lombok.Data;
import sit.int221.integratedproject.kanbanborad.entities.Status;

@Data
public class TaskResponseDTO {
    private Integer id;
    private String title;
    private String assignees;
    private Status status;
    public String getStatus() {
        return this.status.getName();
    }
    public String getColor() {
        return this.status.getColor();
    }
}

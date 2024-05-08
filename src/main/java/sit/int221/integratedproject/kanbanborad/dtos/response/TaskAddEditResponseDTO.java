package sit.int221.integratedproject.kanbanborad.dtos.response;

import lombok.Data;
import sit.int221.integratedproject.kanbanborad.entities.Status;

@Data
public class TaskAddEditResponseDTO {
    private Integer id;
    private String title;
    private String description;
    private String assignees;
    private Status status;
    public String getStatus() {
        if (status != null && status.getId() != null) {
            Integer id = status.getId();
            if (id.equals(1)) {
                this.status.setName("NO_STATUS");
            } else if (id.equals(2)) {
                this.status.setName("TO_DO");
            } else if (id.equals(3)) {
                this.status.setName("DOING");
            } else if (id.equals(4)) {
                this.status.setName("DONE");
            }
            return this.status.getName();
        }
        return null;
    }
}

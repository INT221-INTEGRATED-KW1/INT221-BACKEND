package sit.int221.integratedproject.kanbanborad.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import sit.int221.integratedproject.kanbanborad.entities.Task;

import java.util.List;

@Data
public class StatusLimitResponseDTO {
    private Integer id;
    private Boolean limitMaximumTask;
//    private Integer noOfTasks;
//    @JsonInclude(JsonInclude.Include.NON_NULL)
//    private List<Task> tasks;
}

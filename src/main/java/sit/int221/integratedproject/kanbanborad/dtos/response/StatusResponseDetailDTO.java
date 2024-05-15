package sit.int221.integratedproject.kanbanborad.dtos.response;

import lombok.Data;

@Data
public class StatusResponseDetailDTO {
    private Integer id;
    private String name;
    private String description;
    private Boolean limitMaximumTask;
    private String color;
    private Integer noOfTasks;
}

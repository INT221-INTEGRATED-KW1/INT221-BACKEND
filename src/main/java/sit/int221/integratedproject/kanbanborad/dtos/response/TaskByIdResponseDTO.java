package sit.int221.integratedproject.kanbanborad.dtos.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import sit.int221.integratedproject.kanbanborad.models.Status;

import java.sql.Timestamp;

@Data
public class TaskByIdResponseDTO {
    private Integer id;
    private String title;
    private String description;
    private String assignees;
    @Enumerated(EnumType.STRING)
    private Status status;
    @JsonFormat(pattern="dd-MM-yyyy HH:mm:ss")
    private Timestamp createdOn;
    @JsonFormat(pattern="dd-MM-yyyy HH:mm:ss")
    private Timestamp updatedOn;
    public String getTitle() {
        return title.trim();
    }
    public String getDescription() {
        return description.trim();
    }
    public String getAssignees() {
        return assignees.trim();
    }
}

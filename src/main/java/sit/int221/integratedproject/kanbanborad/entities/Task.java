package sit.int221.integratedproject.kanbanborad.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import sit.int221.integratedproject.kanbanborad.models.Status;

import java.sql.Timestamp;

@Entity
@Table(name = "tasks")
@Data
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @NotNull(message = "Title can not be null")
    @NotBlank(message = "Title can not be empty")
    @Column(name = "title" ,length = 100)
    private String title;
    @NotBlank(message = "Description can not be empty")
    @Column(name = "description", length = 500)
    private String description;
    @NotBlank(message = "Description can not be empty")
    @Column(name = "assignees", length = 30)
    private String assignees;
    @NotNull(message = "Status can not be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @NotNull(message = "CreatedOn can not be null")
    @Column(name = "createdOn")
    private Timestamp createdOn;
    @JsonFormat(pattern="dd-MM-yyyy HH:mm:ss")
    @NotNull(message = "UpdatedOn can not be null")
    @Column(name = "updatedOn")
    private Timestamp updatedOn;
}

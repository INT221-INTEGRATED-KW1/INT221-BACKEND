package sit.int221.integratedproject.kanbanborad.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import sit.int221.integratedproject.kanbanborad.models.Status;

import java.sql.Timestamp;

@Entity
@Table(name = "tasks")
@Data
@DynamicInsert
@DynamicUpdate
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotNull(message = "Title can not be null")
    @Size(min = 1, max = 100)
    private String title;
    @Size(min = 1, max = 500)
    private String description;
    @Size(min = 1, max = 30)
    private String assignees;
    @NotNull(message = "Status can not be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private Timestamp createdOn;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private Timestamp updatedOn;
}
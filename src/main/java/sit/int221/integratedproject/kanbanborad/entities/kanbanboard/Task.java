package sit.int221.integratedproject.kanbanborad.entities.kanbanboard;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.sql.Timestamp;

@Entity
@Table(name = "tasks")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotNull
    @Size(min = 0, max = 100)
    @NotBlank
    private String title;
    @Size(min = 0, max = 500)
    private String description;
    @Size(min = 0, max = 30)
    private String assignees;
    @NotNull
    @ManyToOne
    @JoinColumn(name = "status_id")
    private Status status;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Column(insertable = false, updatable = false)
    private Timestamp createdOn;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Column(insertable = false, updatable = false)
    private Timestamp updatedOn;
}
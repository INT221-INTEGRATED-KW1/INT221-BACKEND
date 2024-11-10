package sit.int221.integratedproject.kanbanborad.entities.kanbanboard;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.sql.Timestamp;

@Entity
@Table(name = "attachments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"task_id", "filename"})
})
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotNull
    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;
    @NotBlank
    @Column(name = "filename", length = 255, nullable = false)
    private String filename;
    @NotBlank
    @Column(name = "file_path", length = 255, nullable = false)
    private String filePath;
    @NotNull
    @Column(name = "file_size")
    private Integer fileSize;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Column(name = "created_on", insertable = false, updatable = false, nullable = false)
    private Timestamp createdOn;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Column(name = "updated_on", insertable = false, updatable = false, nullable = false)
    private Timestamp updatedOn;
    public Attachment(String filename, String filePath) {
        this.filename = filename;
        this.filePath = filePath;
    }
}

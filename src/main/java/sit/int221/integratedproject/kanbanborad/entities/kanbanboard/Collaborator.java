package sit.int221.integratedproject.kanbanborad.entities.kanbanboard;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Table(name = "collaborators")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Collaborator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;
    private String oid;
    private String name;
    private String email;
    @Column(name = "access_right")
    private String accessRight;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Column(insertable = false, updatable = false)
    private Timestamp addedOn;
}

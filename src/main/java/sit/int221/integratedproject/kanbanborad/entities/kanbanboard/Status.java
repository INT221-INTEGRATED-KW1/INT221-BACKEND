package sit.int221.integratedproject.kanbanborad.entities.kanbanboard;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "status")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Status {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Integer id;
    @NotNull
    @Column(unique = true)
    @Size(min = 0, max = 50)
    @NotBlank
    private String name;
    @Size(min = 0, max = 200)
    private String description;
    @Size(min = 0, max = 20)
    private String color;
    @JsonIgnore
    @OneToMany(mappedBy = "status")
    private List<Task> tasks = new ArrayList<>();
}

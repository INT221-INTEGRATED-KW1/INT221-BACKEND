package sit.int221.integratedproject.kanbanborad.entities;

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
    @NotNull(message = "name can not be null")
    @Column(unique = true)
    @Size(min = 1, max = 50)
    @NotBlank
    private String name;
    @Size(min = 1, max = 200)
    @NotBlank
    private String description;
    @Size(min = 1, max = 20)
    private String color;
    @ManyToOne
    @JoinColumn(name = "board_id")
    private Board board;
    @JsonIgnore
    @OneToMany(mappedBy = "status")
    private List<Task> tasks = new ArrayList<>();
}

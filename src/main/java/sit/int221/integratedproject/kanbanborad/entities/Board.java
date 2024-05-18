package sit.int221.integratedproject.kanbanborad.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "board")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Integer id;
    private Boolean limitMaximumStatus;
    @JsonIgnore
    @OneToMany(mappedBy = "board")
    private List<Status> statuses = new ArrayList<>();
    @JsonIgnore
    @OneToMany(mappedBy = "board")
    private List<Task> tasks = new ArrayList<>();
}
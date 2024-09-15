package sit.int221.integratedproject.kanbanborad.entities.kanbanboard;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boards")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Board {
    @Id
    @Column(name = "board_id", unique = true)
    private String id;
    @NotNull
    @Size(min = 0, max = 36)
    @NotBlank
    private String oid;
    @Size(min = 0, max = 120)
    @Column(name = "board_name")
    @NotNull
    @NotBlank
    private String name;
    private Boolean limitMaximumStatus;
    @JsonIgnore
    @OneToMany(mappedBy = "board")
    private List<Status> statuses = new ArrayList<>();
    @JsonIgnore
    @OneToMany(mappedBy = "board")
    private List<Task> tasks = new ArrayList<>();
    @PrePersist
    public void generateId() {
        if (this.id == null || this.id.isEmpty()) {
            this.id = NanoIdUtils.randomNanoId(NanoIdUtils.DEFAULT_NUMBER_GENERATOR, NanoIdUtils.DEFAULT_ALPHABET, 10);
        }
    }
}

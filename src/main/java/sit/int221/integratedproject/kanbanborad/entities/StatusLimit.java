package sit.int221.integratedproject.kanbanborad.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "statusLimit")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class StatusLimit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Boolean statusLimit;
}
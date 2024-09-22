package sit.int221.integratedproject.kanbanborad.entities.kanbanboard;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_token")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @NotNull
    @Size(min = 0, max = 255)
    @NotBlank
    private String token;
    @NotNull
    @Size(min = 0, max = 36)
    @NotBlank
    private String oid;
    @NotNull
    @Size(min = 0, max = 36)
    @NotBlank
    private String exp;
}

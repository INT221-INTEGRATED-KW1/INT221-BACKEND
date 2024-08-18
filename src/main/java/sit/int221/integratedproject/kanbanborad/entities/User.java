package sit.int221.integratedproject.kanbanborad.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sit.int221.integratedproject.kanbanborad.enumeration.RoleEnum;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class User {
    @Id
    @NotNull
    @NotBlank
    @Column(unique = true)
    @Size(min = 0, max = 36)
    private String oid;
    @NotNull
    @NotBlank
    @Size(min = 0, max = 100)
    private String name;
    @NotNull
    @NotBlank
    @Size(min = 0, max = 50)
    private String username;
    @NotNull
    @NotBlank
    @Size(min = 0, max = 50)
    private String email;
    @NotNull
    @NotBlank
    @Size(min = 0, max = 100)
    private String password;
    @NotNull
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private RoleEnum role;
}

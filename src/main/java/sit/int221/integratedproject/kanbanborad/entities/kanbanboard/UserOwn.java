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
@Table(name = "users_own")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserOwn {
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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Column(insertable = false, updatable = false)
    private Timestamp createdOn;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Column(insertable = false, updatable = false)
    private Timestamp updatedOn;
}

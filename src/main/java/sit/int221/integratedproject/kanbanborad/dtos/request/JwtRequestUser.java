package sit.int221.integratedproject.kanbanborad.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JwtRequestUser {
    @NotBlank
    @Size(min = 0, max = 50)
    private String userName;
    @Size(min = 0, max = 14)
    private String password;
}

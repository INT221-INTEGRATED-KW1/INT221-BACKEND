package sit.int221.integratedproject.kanbanborad.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private String access_token;
    private String refresh_token;
}

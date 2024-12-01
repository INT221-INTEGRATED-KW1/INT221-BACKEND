package sit.int221.integratedproject.kanbanborad.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MicrosoftTokenPayload {
    private String oid;
    private String name;
    private String preferredUsername;
    private String email;
}

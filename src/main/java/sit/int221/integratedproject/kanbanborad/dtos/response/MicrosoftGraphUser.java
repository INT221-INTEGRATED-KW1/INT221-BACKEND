package sit.int221.integratedproject.kanbanborad.dtos.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MicrosoftGraphUser {
    private String id;
    private String displayName;
    private String mail;
    private String userPrincipalName;
}

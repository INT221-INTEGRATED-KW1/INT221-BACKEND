package sit.int221.integratedproject.kanbanborad.dtos.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollaboratorResponseDTO {
    private String oid;
    private String name;
    private String email;
    private String accessRight;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private Timestamp addedOn;
    private String invitationStatus;
}

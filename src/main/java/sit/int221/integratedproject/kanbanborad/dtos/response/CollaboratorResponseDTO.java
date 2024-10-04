package sit.int221.integratedproject.kanbanborad.dtos.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class CollaboratorResponseDTO {
    private String oid;
    private String name;
    private String email;
    private String accessRight; // READ or WRITE
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private Timestamp addedOn;
}

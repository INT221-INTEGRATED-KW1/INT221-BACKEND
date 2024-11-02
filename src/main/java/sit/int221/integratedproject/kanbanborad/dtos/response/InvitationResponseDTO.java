package sit.int221.integratedproject.kanbanborad.dtos.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sit.int221.integratedproject.kanbanborad.enumeration.InvitationStatus;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvitationResponseDTO {
    private Integer id;
    private String inviterOid;
    private String inviteeOid;
    private String accessRight;
    private String boardId;
    private InvitationStatus status;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private Timestamp createdOn;

    public InvitationResponseDTO(Integer id, String inviterOid, String accessRight, String id1, InvitationStatus status) {
        this.id = id;
        this.inviterOid = inviterOid;
        this.accessRight = accessRight;
        this.boardId = id1;
        this.status = status;
    }
}

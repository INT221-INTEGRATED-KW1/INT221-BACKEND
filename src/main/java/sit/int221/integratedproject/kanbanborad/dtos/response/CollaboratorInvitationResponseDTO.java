package sit.int221.integratedproject.kanbanborad.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sit.int221.integratedproject.kanbanborad.enumeration.CollabStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollaboratorInvitationResponseDTO {
    private String inviterName;
    private String accessRight;
    private String boardName;
    private CollabStatus status;
}

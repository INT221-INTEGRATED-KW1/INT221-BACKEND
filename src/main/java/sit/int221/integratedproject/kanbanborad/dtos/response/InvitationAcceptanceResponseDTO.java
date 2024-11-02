package sit.int221.integratedproject.kanbanborad.dtos.response;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvitationAcceptanceResponseDTO {
    private String collaboratorOid; // The unique ID of the collaborator
    private String accessRight;      // The access right granted
    private String status;           // The current status of the collaborator
}
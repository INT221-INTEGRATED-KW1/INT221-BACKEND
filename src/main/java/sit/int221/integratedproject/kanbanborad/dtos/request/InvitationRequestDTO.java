package sit.int221.integratedproject.kanbanborad.dtos.request;

import lombok.Data;

@Data
public class InvitationRequestDTO {
    private String inviteeOid; // Unique identifier for the invitee
    private String accessRight; // Access rights for the invitee
}
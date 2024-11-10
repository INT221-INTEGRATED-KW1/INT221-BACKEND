package sit.int221.integratedproject.kanbanborad.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollabBoardResponseDTO {
    private String id;
    private String name;
    private OwnerResponseDTO owner;
    private String visibility;
    private String access_right;
    private String invitationStatus;
}

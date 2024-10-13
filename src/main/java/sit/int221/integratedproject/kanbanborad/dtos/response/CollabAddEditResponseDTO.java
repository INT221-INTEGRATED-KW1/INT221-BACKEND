package sit.int221.integratedproject.kanbanborad.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollabAddEditResponseDTO {
    private String boardID;
    private String collaboratorName;
    private String collaboratorEmail;
    private String accessRight;
}

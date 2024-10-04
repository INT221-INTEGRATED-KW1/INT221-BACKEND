package sit.int221.integratedproject.kanbanborad.dtos.request;

import lombok.Data;

@Data
public class CollaboratorRequestDTO {
    private String email;
    private String access_right;
}

package sit.int221.integratedproject.kanbanborad.dtos.response;

import lombok.Data;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Status;

import java.util.List;

@Data
public class TaskResponseWithAttachmentsDTO {
    private Integer id;
    private String title;
    private String description;
    private String assignees;
    private Status status;
    private Integer attachmentCount;
    private List<AttachmentResponseDTO> attachments;
}

package sit.int221.integratedproject.kanbanborad.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentRequestDTO {
    private String filename;
    private long fileSize;
    private byte[] fileContent;
}

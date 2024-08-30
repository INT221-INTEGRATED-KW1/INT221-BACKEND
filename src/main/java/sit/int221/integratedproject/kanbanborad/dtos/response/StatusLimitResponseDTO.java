package sit.int221.integratedproject.kanbanborad.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
public class StatusLimitResponseDTO {
    private String id;
    private Boolean statusLimit;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<StatusResponseDetailDTO> statuses;
}

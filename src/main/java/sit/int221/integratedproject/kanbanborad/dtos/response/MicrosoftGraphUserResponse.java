package sit.int221.integratedproject.kanbanborad.dtos.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class MicrosoftGraphUserResponse {
    private List<MicrosoftGraphUser> value;
}
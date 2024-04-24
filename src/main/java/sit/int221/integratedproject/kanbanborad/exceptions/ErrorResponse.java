package sit.int221.integratedproject.kanbanborad.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private final LocalDateTime timeStamp = LocalDateTime.now();
    private final int status;
    private final String error;
    private final String message;
    private final String path;
}

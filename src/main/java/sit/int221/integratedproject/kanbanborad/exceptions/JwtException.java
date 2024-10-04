package sit.int221.integratedproject.kanbanborad.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import sit.int221.integratedproject.kanbanborad.enumeration.JwtErrorType;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class JwtException extends RuntimeException {
    private final JwtErrorType errorType;

    public JwtException(JwtErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public JwtErrorType getErrorType() {
        return errorType;
    }
}
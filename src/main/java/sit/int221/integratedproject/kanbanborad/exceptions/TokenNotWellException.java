package sit.int221.integratedproject.kanbanborad.exceptions;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenNotWellException extends RuntimeException {
    public TokenNotWellException(String message) {
        super(message);
    }
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
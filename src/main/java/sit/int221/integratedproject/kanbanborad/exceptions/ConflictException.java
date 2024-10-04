package sit.int221.integratedproject.kanbanborad.exceptions;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}

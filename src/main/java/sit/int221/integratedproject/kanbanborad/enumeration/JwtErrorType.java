package sit.int221.integratedproject.kanbanborad.enumeration;

public enum JwtErrorType {
    EXPIRED_TOKEN,
    TAMPERED_TOKEN,
    MALFORMED_TOKEN,
    MISSING_TOKEN,
    UNAUTHORIZED_ACCESS,
    UNKNOWN_ERROR
}
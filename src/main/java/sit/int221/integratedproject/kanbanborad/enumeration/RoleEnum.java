package sit.int221.integratedproject.kanbanborad.enumeration;

public enum RoleEnum {
    LECTURER("LECTURER"),
    STAFF("STAFF"),
    STUDENT("STUDENT");

    private final String value;

    RoleEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}

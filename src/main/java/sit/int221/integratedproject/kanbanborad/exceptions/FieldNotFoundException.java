package sit.int221.integratedproject.kanbanborad.exceptions;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FieldNotFoundException extends RuntimeException {
    private String fieldName;

    public FieldNotFoundException(String fieldName) {
        this.fieldName = fieldName;
    }

}
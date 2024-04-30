package sit.int221.integratedproject.kanbanborad.utils;

import sit.int221.integratedproject.kanbanborad.models.Status;

public class Utils {
    public static String trimString(String input) {
        return input != null ? input.trim() : null;
    }
    public static Status checkAndSetDefaultStatus(Status status) {
        return status == null ? Status.NO_STATUS : status;
    }
}
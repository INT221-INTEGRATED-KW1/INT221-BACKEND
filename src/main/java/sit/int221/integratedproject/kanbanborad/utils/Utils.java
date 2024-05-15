package sit.int221.integratedproject.kanbanborad.utils;

public class Utils {
    public static String NO_STATUS = "No Status";
    public static String DONE = "Done";
    public static int MAX_SIZE = 10;
    public static Boolean DEFAULT_LIMIT = false;
    public static String trimString(String input) {
        return input != null ? input.trim() : null;
    }
    public static String checkAndSetDefaultNull(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        } else {
            return input.trim();
        }
    }
}
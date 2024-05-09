package sit.int221.integratedproject.kanbanborad.utils;

public class Utils {
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
    public static String checkAndSetDefaultStatus(String status) {
        return status == null ? "NO_STATUS" : status;
    }
//    public static String getString(Status status) {
//        if (status == null || status.getId() == null) {
//            // หากไม่ได้รับค่า status หรือได้รับค่า null มา
//            return "NO_STATUS";
//        } else {
//            Integer id = status.getId();
//            if (id.equals(1)) {
//                return "NO_STATUS";
//            } else if (id.equals(2)) {
//                return "TO_DO";
//            } else if (id.equals(3)) {
//                return "DOING";
//            } else if (id.equals(4)) {
//                return "DONE";
//            }
//            return null;
//        }
//    }
}
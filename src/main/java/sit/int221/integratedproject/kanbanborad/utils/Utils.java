package sit.int221.integratedproject.kanbanborad.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import sit.int221.integratedproject.kanbanborad.exceptions.ForbiddenException;
import sit.int221.integratedproject.kanbanborad.services.JwtTokenUtil;

public class Utils {
    public static String NO_STATUS = "No Status";
    public static String DONE = "Done";
    public static int MAX_SIZE = 10;
    public static int STATUS_LIMIT = 1;
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
    public static Claims getClaims(String token, JwtTokenUtil jwtTokenUtil) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new ForbiddenException("Authorization required for private boards.");
        }

        String jwtToken = token.substring(7);
        try {
            return jwtTokenUtil.getAllClaimsFromToken(jwtToken);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to get JWT Token");
        } catch (ExpiredJwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT Token has expired");
        }
    }


}
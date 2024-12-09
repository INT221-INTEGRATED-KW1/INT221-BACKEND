package sit.int221.integratedproject.kanbanborad.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import sit.int221.integratedproject.kanbanborad.exceptions.ForbiddenException;
import sit.int221.integratedproject.kanbanborad.services.JwtTokenUtil;

import java.util.Base64;
import java.util.Map;

public class Utils {
    public static String NO_STATUS = "No Status";
    public static String DONE = "Done";
    public static int MAX_SIZE = 10;
    public static String SIT_DOMAIN = "@ad.sit.kmutt.ac.th";
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

    public static boolean isMicrosoftToken(String token) {
        String issuer = getIssuer(token);
        return issuer != null && issuer.contains("microsoft");
    }

    public static Claims extractClaimsFromMicrosoftToken(String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            Claims claims = Jwts.claims();
            claims.put("oid", claimsSet.getStringClaim("oid"));
            claims.put("name", claimsSet.getStringClaim("name"));
            claims.put("preferred_username", claimsSet.getStringClaim("preferred_username"));

            return claims;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Microsoft Token");
        }
    }

    public static String getIssuer(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> claims = mapper.readValue(payload, Map.class);
            return (String) claims.get("iss");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
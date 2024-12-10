
package sit.int221.integratedproject.kanbanborad.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import sit.int221.integratedproject.kanbanborad.config.AzureConfig;
import sit.int221.integratedproject.kanbanborad.dtos.response.MicrosoftGraphUser;
import sit.int221.integratedproject.kanbanborad.dtos.response.MicrosoftGraphUserResponse;
import sit.int221.integratedproject.kanbanborad.entities.itbkkshared.User;
import sit.int221.integratedproject.kanbanborad.exceptions.ForbiddenException;
import sit.int221.integratedproject.kanbanborad.services.JwtTokenUtil;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

public class Utils {
    public static String NO_STATUS = "No Status";
    public static String DONE = "Done";
    public static int MAX_SIZE = 10;
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

    public static String fetchMicrosoftGraphUser(String accessToken, String encodedEmail) {
        try {
            String graphEndpoint = "https://graph.microsoft.com/v1.0/users?$filter=" + encodedEmail;

            HttpRequest request = HttpRequest.newBuilder(URI.create(graphEndpoint))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.err.println("Failed to fetch user. Status: " + response.statusCode() + ", Body: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Error fetching user from MS Graph API: " + e.getMessage());
        }
        return null;
    }

    public static String getMicrosoftGraphToken(String userToken) {
        try {
            AzureConfig azureConfig = AzureConfig.getInstance();

            if (userToken.startsWith("Bearer ")) {
                userToken = userToken.substring(7);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(azureConfig.getTokenUrl(), azureConfig.getTenantId())))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "client_id=" + azureConfig.getClientId() +
                                    "&scope=https://graph.microsoft.com/.default" +
                                    "&client_secret=" + azureConfig.getClientSecret() +
                                    "&grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer" +
                                    "&assertion=" + userToken +
                                    "&requested_token_use=on_behalf_of"))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> tokenResponse = new ObjectMapper().readValue(response.body(), Map.class);
                return (String) tokenResponse.get("access_token");
            } else {
                System.err.println("Failed to fetch token. Status: " + response.statusCode() + ", Body: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Error fetching Microsoft Graph token: " + e.getMessage());
        }
        return null;
    }

    public static Optional<User> findUserInMicrosoftEntra(String email, String userToken) {
        try {
            String encodedEmail = URLEncoder.encode("mail eq '" + email + "'", StandardCharsets.UTF_8);

            String accessToken =  getMicrosoftGraphToken(userToken);
            if (accessToken == null) {
                throw new RuntimeException("Failed to retrieve Microsoft Graph Token.");
            }

            String responseBody = fetchMicrosoftGraphUser(accessToken, encodedEmail);
            if (responseBody == null) {
                return Optional.empty();
            }

            MicrosoftGraphUserResponse graphResponse = new ObjectMapper().readValue(responseBody, MicrosoftGraphUserResponse.class);
            if (!graphResponse.getValue().isEmpty()) {
                MicrosoftGraphUser graphUser = graphResponse.getValue().get(0);
                User user = new User();
                user.setOid(graphUser.getId());
                user.setName(graphUser.getDisplayName());
                user.setEmail(graphUser.getMail());
                user.setUsername(graphUser.getUserPrincipalName());
                return Optional.of(user);
            }
        } catch (Exception e) {
            System.err.println("Error finding user in Microsoft Entra: " + e.getMessage());
        }
        return Optional.empty();
    }


}

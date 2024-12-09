
package sit.int221.integratedproject.kanbanborad.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import sit.int221.integratedproject.kanbanborad.enumeration.JwtErrorType;
import sit.int221.integratedproject.kanbanborad.exceptions.TokenIsMissingException;
import sit.int221.integratedproject.kanbanborad.exceptions.TokenNotWellException;
import sit.int221.integratedproject.kanbanborad.services.JwtTokenUtil;
import sit.int221.integratedproject.kanbanborad.services.JwtUserDetailsService;
import sit.int221.integratedproject.kanbanborad.services.MicrosoftUserDetailsService;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUserDetailsService jwtUserDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final MicrosoftUserDetailsService microsoftUserDetailsService;

    public JwtAuthFilter(JwtUserDetailsService jwtUserDetailsService, JwtTokenUtil jwtTokenUtil, MicrosoftUserDetailsService microsoftUserDetailsService) {
        this.jwtUserDetailsService = jwtUserDetailsService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.microsoftUserDetailsService = microsoftUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        final String requestTokenHeader = request.getHeader("Authorization");
        String oid = null;
        String jwtToken = null;

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        if (requestURI.equals("/login") || requestURI.equals("/token") || requestURI.equals("/login/microsoft")) {
            chain.doFilter(request, response);
            return;
        }

        if (isPublicEndpoint(requestURI, method)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
                jwtToken = requestTokenHeader.substring(7);

                if (isMicrosoftToken(jwtToken)) {
                    oid = validateMicrosoftToken(jwtToken);
                }
                else if (isLocalJwt(jwtToken)) {
                    oid = jwtTokenUtil.getOidFromToken(jwtToken);
                    validateLocalJwt(jwtToken, oid);
                }
                else {
                    throw new TokenNotWellException("JWT Token not well-formed");
                }

                if (oid != null) {
                    if (isMicrosoftToken(jwtToken)) {
                        UserDetails microsoftUserDetails = microsoftUserDetailsService.loadUserByOid(oid);
                        UsernamePasswordAuthenticationToken microsoftAuthToken =
                                new UsernamePasswordAuthenticationToken(microsoftUserDetails, null, microsoftUserDetails.getAuthorities());
                        microsoftAuthToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(microsoftAuthToken);
                    }
                    else if (isLocalJwt(jwtToken)) {
                        UserDetails userDetails = jwtUserDetailsService.loadUserByOid(oid);
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
            else if (requestTokenHeader == null) {
                throw new TokenIsMissingException("JWT Token is missing");
            }
            else {
                throw new TokenNotWellException("JWT Token not well-formed");
            }

            chain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            handleException(JwtErrorType.EXPIRED_TOKEN, response, "JWT Token has expired", HttpStatus.UNAUTHORIZED, request.getRequestURI());
        } catch (MalformedJwtException | SignatureException e) {
            handleException(JwtErrorType.TAMPERED_TOKEN, response, "JWT Token has been tampered with", HttpStatus.UNAUTHORIZED, request.getRequestURI());
        } catch (TokenNotWellException e) {
            handleException(JwtErrorType.MALFORMED_TOKEN, response, "JWT Token not well-formed", HttpStatus.UNAUTHORIZED, request.getRequestURI());
        } catch (TokenIsMissingException e) {
            handleException(JwtErrorType.MISSING_TOKEN, response, "JWT Token is missing", HttpStatus.UNAUTHORIZED, request.getRequestURI());
        } catch (Exception e) {
            handleException(JwtErrorType.UNKNOWN_ERROR, response, "An unknown error occurred", HttpStatus.INTERNAL_SERVER_ERROR, request.getRequestURI());
        }
    }

    private String getIssuer(String token) {
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
            return null; // เกิดข้อผิดพลาด
        }
    }

    private boolean isLocalJwt(String token) {
        String issuer = getIssuer(token);
        return "https://intproj23.sit.kmutt.ac.th/kw1/".equals(issuer);
    }

    private boolean isMicrosoftToken(String token) {
        String issuer = getIssuer(token);
        return issuer != null && issuer.contains("microsoft");
    }

    private String validateMicrosoftToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getStringClaim("oid");
        } catch (Exception e) {
            throw new TokenNotWellException("Invalid Microsoft Token");
        }
    }

    private void validateLocalJwt(String jwtToken, String oid) {
        if (!jwtTokenUtil.validateToken(jwtToken, oid)) {
            throw new SignatureException("Token validation failed");
        }
    }

    private boolean isPublicEndpoint(String requestURI, String method) {
        return method.equalsIgnoreCase("GET") &&
                (requestURI.matches("/v3/boards/[A-Za-z0-9_]+") ||
                        requestURI.matches("/v3/boards/[A-Za-z0-9_]+/collabs") ||
                        requestURI.matches("/v3/boards/[A-Za-z0-9_]+/collabs/[A-Za-z0-9_]+") ||
                        requestURI.matches("/v3/boards/[A-Za-z0-9_]+/statuses(/\\d+)?") ||
                        requestURI.matches("/v3/boards/[A-Za-z0-9_]+/tasks(/\\d+)?"));
    }

    private void handleException(JwtErrorType type, HttpServletResponse response, String message, HttpStatus status, String instance)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String jsonResponse = String.format("{\"type\": \"%s\", \"status\": %d, \"message\": \"%s\", \"instance\": \"uri=%s\"}",
                type.name() ,status.value(), message, instance);
        response.getWriter().write(jsonResponse);
    }
}


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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import sit.int221.integratedproject.kanbanborad.enumeration.JwtErrorType;
import sit.int221.integratedproject.kanbanborad.exceptions.TokenIsMissingException;
import sit.int221.integratedproject.kanbanborad.exceptions.TokenNotWellException;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.BoardRepository;
import sit.int221.integratedproject.kanbanborad.services.JwtTokenUtil;
import sit.int221.integratedproject.kanbanborad.services.JwtUserDetailsService;

import java.io.IOException;
import java.util.Map;

@Component
public class    JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUserDetailsService jwtUserDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final BoardRepository boardRepository;

    public JwtAuthFilter(JwtUserDetailsService jwtUserDetailsService, JwtTokenUtil jwtTokenUtil, BoardRepository boardRepository) {
        this.jwtUserDetailsService = jwtUserDetailsService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.boardRepository = boardRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        final String requestTokenHeader = request.getHeader("Authorization");
        String oid = null;
        String jwtToken = null;

        String requestURI = request.getRequestURI();
        if (isPublicEndpoint(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
                jwtToken = requestTokenHeader.substring(7);

                if (isMicrosoftToken(jwtToken)) {
                    oid = validateMicrosoftToken(jwtToken); // Validate และดึง OID
                } else if (isLocalJwt(jwtToken)) {
                    oid = jwtTokenUtil.getOidFromToken(jwtToken); // JWT ของระบบ
                    validateLocalJwt(jwtToken, oid);
                } else {
                    throw new TokenNotWellException("JWT Token not well-formed");
                }

                if (oid != null) {
                    UserDetails userDetails = jwtUserDetailsService.loadUserByOid(oid);
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } else if (requestTokenHeader == null) {
                throw new TokenIsMissingException("JWT Token is missing");
            } else {
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

    private boolean isLocalJwt(String token) {
        return token.split("\\.").length == 3;
    }

    private boolean isMicrosoftToken(String token) {
        return token.contains(".");
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

    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.matches("/v3/boards/[A-Za-z0-9]+") ||
                requestURI.matches("/v3/boards/[A-Za-z0-9]+/statuses(/\\d+)?");
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


package sit.int221.integratedproject.kanbanborad.filter;

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
import sit.int221.integratedproject.kanbanborad.dtos.response.AuthenticateUser;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
import sit.int221.integratedproject.kanbanborad.enumeration.JwtErrorType;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.exceptions.TokenIsMissingException;
import sit.int221.integratedproject.kanbanborad.exceptions.TokenNotWellException;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.BoardRepository;
import sit.int221.integratedproject.kanbanborad.services.JwtTokenUtil;
import sit.int221.integratedproject.kanbanborad.services.JwtUserDetailsService;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
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

        // Get request URI
        String requestURI = request.getRequestURI();
        if (requestURI.equals("/login") || requestURI.equals("/token")) {
            chain.doFilter(request, response);
            return;
        }

        String method = request.getMethod();

        boolean isPublicGetEndpoint = method.equalsIgnoreCase("GET") &&
                (requestURI.matches("/v3/boards/[A-Za-z0-9]+") ||
                        requestURI.matches("/v3/boards/[A-Za-z0-9]+/statuses(/\\d+)?") ||
                        requestURI.matches("/v3/boards/[A-Za-z0-9]+/tasks(/\\d+)?"));

        // If the endpoint is public GET, allow access without token
        if (isPublicGetEndpoint) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // Check if the token exists and starts with Bearer
            if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
                jwtToken = requestTokenHeader.substring(7);

                // Check if the token has 3 parts
                String[] tokenParts = jwtToken.split("\\.");
                if (tokenParts.length == 3) {
                    oid = jwtTokenUtil.getOidFromToken(jwtToken);

                    if (oid != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = this.jwtUserDetailsService.loadUserByOid(oid);
                        if (jwtTokenUtil.validateToken(jwtToken, (AuthenticateUser) userDetails)) {
                            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                            usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                        } else {
                            throw new SignatureException("Token validation failed");
                        }
                    }
                } else {
                    throw new TokenNotWellException("JWT Token not well-formed");
                }
            } else if (requestTokenHeader == null) {
                throw new TokenIsMissingException("JWT Token is missing");
            }

            chain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            handleException(JwtErrorType.EXPIRED_TOKEN,response, "JWT Token has expired", HttpStatus.UNAUTHORIZED, request.getRequestURI());
        } catch (MalformedJwtException | SignatureException e) {
            handleException(JwtErrorType.TAMPERED_TOKEN,response, "JWT Token has been tampered with", HttpStatus.UNAUTHORIZED, request.getRequestURI());
        } catch (TokenNotWellException e) {
            handleException(JwtErrorType.MALFORMED_TOKEN, response, "JWT Token not well-formed", HttpStatus.UNAUTHORIZED, request.getRequestURI());
        } catch (TokenIsMissingException e) {
            handleException(JwtErrorType.MISSING_TOKEN, response, "JWT Token is missing", HttpStatus.UNAUTHORIZED, request.getRequestURI());
        }
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

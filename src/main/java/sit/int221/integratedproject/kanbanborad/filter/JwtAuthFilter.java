package sit.int221.integratedproject.kanbanborad.filter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import sit.int221.integratedproject.kanbanborad.dtos.response.AuthenticateUser;
import sit.int221.integratedproject.kanbanborad.exceptions.TokenIsMissingException;
import sit.int221.integratedproject.kanbanborad.exceptions.TokenNotWellException;
import sit.int221.integratedproject.kanbanborad.services.JwtTokenUtil;
import sit.int221.integratedproject.kanbanborad.services.JwtUserDetailsService;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUserDetailsService jwtUserDetailsService;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        final String requestTokenHeader = request.getHeader("Authorization");
        String oid = null;
        String jwtToken = null;

        // Skip JWT processing for certain endpoints
        String requestURI = request.getRequestURI();
        if (requestURI.equals("/v2/users/login")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
                jwtToken = requestTokenHeader.substring(7);

                // Manually check if the JWT token has three parts
                String[] tokenParts = jwtToken.split("\\.");
                if (tokenParts.length == 3) {
                    // If it has three parts, proceed with standard JWT processing
                    oid = jwtTokenUtil.getOidFromToken(jwtToken);

                    if (oid != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = this.jwtUserDetailsService.loadUserByOid(oid);
                        if (jwtTokenUtil.validateToken(jwtToken, (AuthenticateUser) userDetails)) {
                            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                            usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                        } else {
                            System.out.println("Token validation failed");
                        }
                    }
                } else if (tokenParts.length != 3) {
                    throw new TokenNotWellException("JWT Token not well-formed");
                }
            }
            else if (requestTokenHeader == null) {
                throw new TokenIsMissingException("JWT Token is missing");
            }

            chain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            handleException(response, "JWT Token has expired", HttpStatus.UNAUTHORIZED, request.getRequestURI());
        } catch (MalformedJwtException e) {
            handleException(response, "JWT Token has been tampered with", HttpStatus.UNAUTHORIZED, request.getRequestURI());
        }  catch (TokenNotWellException e) {
            handleException(response, "JWT Token not well-formed", HttpStatus.UNAUTHORIZED, request.getRequestURI());
        } catch (TokenIsMissingException e) {
            handleException(response, "JWT Token is missing", HttpStatus.UNAUTHORIZED, request.getRequestURI());
        } catch (Exception e) {
            handleException(response, "An error occurred during JWT processing", HttpStatus.INTERNAL_SERVER_ERROR, request.getRequestURI());
        }
    }

    private void handleException(HttpServletResponse response, String message, HttpStatus status, String instance)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String jsonResponse = String.format("{\"status\": %d, \"message\": \"%s\", \"instance\": \"uri=%s\"}",
                status.value(), message, instance);
        response.getWriter().write(jsonResponse);
    }
}



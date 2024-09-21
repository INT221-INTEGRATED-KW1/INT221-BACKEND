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
        if (requestURI.equals("/login") ||
                requestURI.matches("/v3/boards/[A-Za-z0-9]+/statuses(/\\d+)?") ||
                requestURI.matches("/v3/boards/[A-Za-z0-9]+/tasks(/\\d+)?") ||
                requestURI.matches("/v3/boards/[A-Za-z0-9]+") ||
                requestURI.matches("/v3/boards/[A-Za-z0-9]+/maximum-status")) {
            chain.doFilter(request, response);
            return;
        }

        // Extract the board ID only for board-related endpoints
        String boardId = extractBoardIdFromURI(requestURI);

        // For endpoints that require a board ID (like getting statuses), handle the board visibility check
        if (boardId != null) {
            try {
                Board board = boardRepository.findById(boardId)
                        .orElseThrow(() -> new ItemNotFoundException("Board Id " + boardId + " DOES NOT EXIST !!!"));

                if (board.getVisibility().equalsIgnoreCase("PUBLIC")) {
                    chain.doFilter(request, response);
                    return;
                }
            } catch (ItemNotFoundException e) {
                // Return 404 if the board does not exist
                handleException(response, e.getMessage(), HttpStatus.NOT_FOUND, request.getRequestURI());
                return; // Stop further processing
            }
        } else if (!requestURI.equals("/v3/boards")) {
            // Handle the case where the board ID is missing and it's not a boards endpoint
            handleException(response, "Board ID is missing in the request", HttpStatus.BAD_REQUEST, request.getRequestURI());
            return; // Stop further processing
        }

        try {
            // Process the JWT token
            if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
                jwtToken = requestTokenHeader.substring(7);

                // Manually check if the JWT token has three parts
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
                            System.out.println("Token validation failed");
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
            handleException(response, "JWT Token has expired", HttpStatus.UNAUTHORIZED, request.getRequestURI());
        } catch (MalformedJwtException e) {
            handleException(response, "JWT Token has been tampered with", HttpStatus.UNAUTHORIZED, request.getRequestURI());
        } catch (TokenNotWellException e) {
            handleException(response, "JWT Token not well-formed", HttpStatus.UNAUTHORIZED, request.getRequestURI());
        } catch (TokenIsMissingException e) {
            handleException(response, "JWT Token is missing", HttpStatus.UNAUTHORIZED, request.getRequestURI());
        } catch (SignatureException e) {
            handleException(response, "JWT Token has been tampered with", HttpStatus.UNAUTHORIZED, request.getRequestURI());
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

    private String extractBoardIdFromURI(String uri) {
        // Split the URI by "/"
        String[] parts = uri.split("/");

        // Check if there are enough parts to extract the board ID (4th part in the URI)
        if (parts.length > 3) {
            return parts[3]; // The board ID is the 4th part of the URI (index 3)
        }

        // Return null if there are not enough parts
        return null;
    }
}

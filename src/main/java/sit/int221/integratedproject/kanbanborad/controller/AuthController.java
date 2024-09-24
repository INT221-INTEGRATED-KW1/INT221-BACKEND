package sit.int221.integratedproject.kanbanborad.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import sit.int221.integratedproject.kanbanborad.dtos.request.JwtRequestUser;
import sit.int221.integratedproject.kanbanborad.dtos.response.AuthenticateUser;
import sit.int221.integratedproject.kanbanborad.dtos.response.LoginResponseDTO;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.RefreshToken;
import sit.int221.integratedproject.kanbanborad.exceptions.GeneralException;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.RefreshTokenRepository;
import sit.int221.integratedproject.kanbanborad.services.AuthService;
import sit.int221.integratedproject.kanbanborad.services.JwtTokenUtil;

@RestController
@CrossOrigin(origins = "http://localhost")
public class AuthController {
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthService authService;

    public AuthController(JwtTokenUtil jwtTokenUtil, AuthenticationManager authenticationManager, RefreshTokenRepository refreshTokenRepository, AuthService authService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.authenticationManager = authenticationManager;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody @Valid JwtRequestUser jwtRequestUser) {
        try {
            // Delegate the login logic to the AuthService
            LoginResponseDTO responseDTO = authService.login(jwtRequestUser);
            return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
        } catch (UsernameNotFoundException e) {
            throw new UsernameNotFoundException("Username or Password is incorrect");
        } catch (GeneralException e) {
            throw new GeneralException("There is a problem. Please try again later.");
        }
    }

    @PostMapping("/token")
    public ResponseEntity<Object> refreshAccessToken(
            @RequestHeader(value = "Authorization") String token) {
        Claims claims = null;
        String jwtToken = null;

        if (token != null && token.startsWith("Bearer ")) {
            jwtToken = token.substring(7);
            try {
                // Validate refresh token
                if (!jwtTokenUtil.validateRefreshToken(jwtToken)) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
                }
                claims = jwtTokenUtil.getAllClaimsFromToken(jwtToken);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to get JWT Token");
            } catch (ExpiredJwtException e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT Token has expired");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "JWT Token does not begin with Bearer String");
        }

        return ResponseEntity.ok(authService.refreshAccessToken(claims));
    }

}
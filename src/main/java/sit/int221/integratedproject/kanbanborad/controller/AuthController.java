
package sit.int221.integratedproject.kanbanborad.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import sit.int221.integratedproject.kanbanborad.dtos.request.JwtRequestUser;
import sit.int221.integratedproject.kanbanborad.dtos.request.MicrosoftTokenRequest;
import sit.int221.integratedproject.kanbanborad.dtos.response.LoginResponseDTO;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.UserOwn;
import sit.int221.integratedproject.kanbanborad.exceptions.GeneralException;
import sit.int221.integratedproject.kanbanborad.services.AuthService;
import sit.int221.integratedproject.kanbanborad.services.JwtTokenUtil;

@RestController
@CrossOrigin(origins = "http://localhost")
public class AuthController {
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthService authService;

    public AuthController(JwtTokenUtil jwtTokenUtil,  AuthService authService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody @Valid JwtRequestUser jwtRequestUser) {
        try {
            LoginResponseDTO responseDTO = authService.login(jwtRequestUser);
            return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
        } catch (UsernameNotFoundException e) {
            throw new UsernameNotFoundException("Username or Password is incorrect");
        } catch (GeneralException e) {
            throw new GeneralException("There is a problem. Please try again later.");
        }
    }

    @PostMapping("/login/microsoft")
    public ResponseEntity<Object> loginWithMicrosoft(@RequestBody MicrosoftTokenRequest microsoftTokenRequest) {
        try {
            UserOwn user = authService.loginWithMicrosoft(microsoftTokenRequest);
            return ResponseEntity.status(HttpStatus.OK).body(user);
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

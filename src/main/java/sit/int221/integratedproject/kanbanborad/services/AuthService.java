package sit.int221.integratedproject.kanbanborad.services;

import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import sit.int221.integratedproject.kanbanborad.dtos.request.JwtRequestUser;
import sit.int221.integratedproject.kanbanborad.dtos.response.AuthenticateUser;
import sit.int221.integratedproject.kanbanborad.dtos.response.LoginResponseDTO;
import sit.int221.integratedproject.kanbanborad.exceptions.GeneralException;

import java.time.Instant;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthService(AuthenticationManager authenticationManager, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @Transactional
    public LoginResponseDTO login(JwtRequestUser body) {
        try {
            var authInfo = new UsernamePasswordAuthenticationToken(body.getUsername(), body.getPassword());
            var authentication = authenticationManager.authenticate(authInfo);
            var authenticatedUser = (AuthenticateUser) authentication.getPrincipal();
            var now = Instant.now();
            var accessToken = tokenService.issueAccessToken(authentication, now);

            return new LoginResponseDTO(
                    accessToken
            );
        } catch (AuthenticationException e) {
            throw new UsernameNotFoundException("Username or Password is incorrect");
        } catch (Exception e) {
            throw new GeneralException("There is a problem. Please try again later.");
        }
    }
}

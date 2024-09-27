package sit.int221.integratedproject.kanbanborad.services;

import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import sit.int221.integratedproject.kanbanborad.dtos.request.JwtRequestUser;
import sit.int221.integratedproject.kanbanborad.dtos.response.AuthenticateUser;
import sit.int221.integratedproject.kanbanborad.dtos.response.LoginResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.RefreshTokenResponseDTO;
import sit.int221.integratedproject.kanbanborad.entities.itbkkshared.User;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.RefreshToken;
import sit.int221.integratedproject.kanbanborad.exceptions.GeneralException;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.itbkkshared.UserRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.RefreshTokenRepository;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenUtil jwtTokenUtil,
                       UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public LoginResponseDTO login(JwtRequestUser jwtRequestUser) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(jwtRequestUser.getUserName(), jwtRequestUser.getPassword())
            );

            String accessToken = jwtTokenUtil.generateToken(authentication);
            String refreshToken = jwtTokenUtil.generateRefreshToken(authentication);

            var authenticatedUser = (AuthenticateUser) authentication.getPrincipal();

            RefreshToken refreshTokenEntity = new RefreshToken(null, refreshToken, authenticatedUser.oid(), String.valueOf(System.currentTimeMillis()));

            refreshTokenRepository.save(refreshTokenEntity);

            return new LoginResponseDTO(accessToken, refreshToken);
        } catch (AuthenticationException e) {
            throw new UsernameNotFoundException("Username or Password is incorrect");
        } catch (Exception e) {
            throw new GeneralException("There is a problem. Please try again later.");
        }
    }

    public RefreshTokenResponseDTO refreshAccessToken(Claims claims) {
        String oid = claims.get("oid", String.class);

        User user = userRepository.findByOid(oid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));


        // Create new access token
        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());
        String newAccessToken = jwtTokenUtil.generateToken(authentication);

        return new RefreshTokenResponseDTO(newAccessToken);
    }

}

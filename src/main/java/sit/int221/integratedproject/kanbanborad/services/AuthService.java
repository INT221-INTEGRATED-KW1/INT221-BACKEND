package sit.int221.integratedproject.kanbanborad.services;

import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import sit.int221.integratedproject.kanbanborad.dtos.response.LoginResponseDTO;
import sit.int221.integratedproject.kanbanborad.entities.itbkkshared.User;
import sit.int221.integratedproject.kanbanborad.repositories.itbkkshared.UserRepository;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthService(UserRepository userRepository, JwtTokenUtil jwtTokenUtil) {
        this.userRepository = userRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    public LoginResponseDTO refreshAccessToken(Claims claims) {
        String oid = claims.get("oid", String.class);

        User user = userRepository.findByOid(oid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Create new access token
        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());
        String newAccessToken = jwtTokenUtil.generateToken(authentication);
        String newRefreshToken = jwtTokenUtil.generateRefreshToken(authentication);

        return new LoginResponseDTO(newAccessToken, newRefreshToken);
    }

}

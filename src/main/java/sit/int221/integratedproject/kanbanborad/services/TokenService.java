package sit.int221.integratedproject.kanbanborad.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import sit.int221.integratedproject.kanbanborad.dtos.response.AuthenticateUser;
import sit.int221.integratedproject.kanbanborad.entities.User;
import sit.int221.integratedproject.kanbanborad.repositories.itbkkshared.UserRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class TokenService {
    private final String ISSUER = "https://intproj23.sit.kmutt.ac.th/kw1/";
    private final JwtEncoder jwtEncoder;
    private final long accessTokenExpiredInSeconds;
    private final CustomUserDetailsService customUserDetailService;
    private final UserRepository userRepository;

    public TokenService(JwtEncoder jwtEncoder,
                        @Value("${access-token-expired-in-seconds}") long accessTokenExpiredInSeconds,
                        CustomUserDetailsService customUserDetailService, UserRepository userRepository) {
        this.jwtEncoder = jwtEncoder;
        this.accessTokenExpiredInSeconds = accessTokenExpiredInSeconds;
        this.customUserDetailService = customUserDetailService;
        this.userRepository = userRepository;
    }

    public String issueAccessToken(Authentication auth, Instant issueDate) {
        return generateToken(auth, issueDate, accessTokenExpiredInSeconds);
    }

    public String issueAccessToken(User user, Instant issueDate) {
        AuthenticateUser userDetails = (AuthenticateUser) customUserDetailService.loadUserByUsername(user.getUsername());
        return generateToken(userDetails, issueDate, accessTokenExpiredInSeconds);
    }

    public String generateToken(Authentication auth, Instant issueDate, long expiredInSeconds) {
        var authenticatedUser = (AuthenticateUser) auth.getPrincipal();
        return generateToken(authenticatedUser.oid(), auth.getAuthorities(), issueDate, expiredInSeconds);
    }

    private String generateToken(String oid, Collection<? extends GrantedAuthority> authorities, Instant issueDate, long expiredInSeconds) {
        Instant expire = issueDate.plusSeconds(expiredInSeconds);
        User user = userRepository.findByOid(oid).get();
        HashMap<String, String> claimsMap = new HashMap<>();
        claimsMap.put("iss", ISSUER);
        claimsMap.put("name", user.getName());
        claimsMap.put("oid", user.getOid());
        claimsMap.put("email", user.getEmail());
        claimsMap.put("role", user.getRole().name());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(issueDate)
                .claims(map -> map.putAll(claimsMap))
                .expiresAt(expire)
                .build();

        JwsHeader headers = JwsHeader.with(() -> "RS256")
                .type("JWT")
                .build();

        return encodeClaimToJwt(headers, claims);
    }

    public String generateToken(AuthenticateUser auth, Instant issueDate, long expiredInSeconds) {
        return generateToken(auth.oid(), auth.getAuthorities(), issueDate, expiredInSeconds);
    }

    public String encodeClaimToJwt(JwsHeader headers, JwtClaimsSet claims) {
        return jwtEncoder.encode(JwtEncoderParameters.from(headers,claims)).getTokenValue();
    }

}

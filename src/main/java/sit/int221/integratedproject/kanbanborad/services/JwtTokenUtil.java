package sit.int221.integratedproject.kanbanborad.services;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import sit.int221.integratedproject.kanbanborad.dtos.response.AuthenticateUser;
import sit.int221.integratedproject.kanbanborad.entities.itbkkshared.User;
import sit.int221.integratedproject.kanbanborad.repositories.itbkkshared.UserRepository;

import java.io.Serializable;
import java.net.URL;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


@Component
public class JwtTokenUtil implements Serializable {
    private final String ISSUER = "https://login.microsoftonline.com/79845616-9df0-43e0-8842-e300feb2642a/v2.0";
    private final String JWK_URI = "https://login.microsoftonline.com/79845616-9df0-43e0-8842-e300feb2642a/discovery/v2.0/keys";
    private final String AUDIENCE = "c24bef80-9a21-4c60-95a8-92babebc1a5c";
    @Value("${jwt.secret}")
    private String SECRET_KEY;
    @Value("#{${jwt.max-token-interval-hour}*60*60*1000}")
    private long JWT_TOKEN_VALIDITY;
    @Value("#{${jwt.max-refresh-token-interval-hour}*60*60*1000}")
    private long JWT_REFRESH_TOKEN_VALIDITY;
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
    private final UserRepository userRepository;

    public JwtTokenUtil(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String getOidFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("oid", String.class));
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    public Claims getAllClaimsFromToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(getSignInKey()).parseClaimsJws(token).getBody();
        return claims;
    }

    public Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public String generateToken(Authentication authentication) {
        User user = getUserFromAuthentication(authentication);

        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", ISSUER);
        claims.put("name", user.getName());
        claims.put("oid", user.getOid());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());

        return doGenerateToken(claims);
    }

    public String generateRefreshToken(Authentication authentication) {
        User user = getUserFromAuthentication(authentication);

        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", ISSUER);
        claims.put("oid", user.getOid());

        return doGenerateRefreshToken(claims);
    }

    private User getUserFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof AuthenticateUser) {
            var authenticatedUser = (AuthenticateUser) principal;
            return userRepository.findById(authenticatedUser.oid())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with oid: " + authenticatedUser.oid()));
        } else if (principal instanceof String) {
            return userRepository.findByUsername((String) principal)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + principal));
        } else {
            throw new IllegalArgumentException("Unexpected principal type: " + principal.getClass().getName());
        }
    }

    private String doGenerateRefreshToken(Map<String, Object> claims) {
        return Jwts.builder().setHeaderParam("typ", "JWT")
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_REFRESH_TOKEN_VALIDITY))
                .signWith(signatureAlgorithm, getSignInKey()).compact();
    }

    private String doGenerateToken(Map<String, Object> claims) {
        return Jwts.builder().setHeaderParam("typ", "JWT")
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                .signWith(signatureAlgorithm, getSignInKey()).compact();
    }

    public String generateTokenWithOid(String oid) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("oid", oid);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                .signWith(SignatureAlgorithm.HS256, getSignInKey())
                .compact();
    }

    public Boolean validateToken(String token, AuthenticateUser userDetails) {
        final String oid = getOidFromToken(token);
        return (oid.equals(userDetails.oid()) && !isTokenExpired(token));
    }

    public Boolean validateToken(String token, String oid) {
        final String tokenOid = getOidFromToken(token);
        return (tokenOid.equals(oid) && !isTokenExpired(token));
    }

    public Boolean validateRefreshToken(String token) {
        try {
            final String oid = getOidFromToken(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
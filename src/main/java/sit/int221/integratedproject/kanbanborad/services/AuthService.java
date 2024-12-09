
package sit.int221.integratedproject.kanbanborad.services;

import com.nimbusds.jose.jwk.*;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import sit.int221.integratedproject.kanbanborad.dtos.request.JwtRequestUser;
import sit.int221.integratedproject.kanbanborad.dtos.request.MicrosoftTokenRequest;
import sit.int221.integratedproject.kanbanborad.dtos.response.AuthenticateUser;
import sit.int221.integratedproject.kanbanborad.dtos.response.LoginResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.MicrosoftTokenPayload;
import sit.int221.integratedproject.kanbanborad.dtos.response.RefreshTokenResponseDTO;
import sit.int221.integratedproject.kanbanborad.entities.itbkkshared.User;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.UserOwn;
import sit.int221.integratedproject.kanbanborad.exceptions.GeneralException;
import sit.int221.integratedproject.kanbanborad.repositories.itbkkshared.UserRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.UserOwnRepository;
import sit.int221.integratedproject.kanbanborad.utils.Utils;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;

@Service
public class AuthService {
    @Value("${auth.jwk-uri}")
    private static String AUTH_JWK_URL;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;
    private final UserOwnRepository userOwnRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenUtil jwtTokenUtil,
                       UserRepository userRepository,
                 UserOwnRepository userOwnRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userRepository = userRepository;
        this.userOwnRepository = userOwnRepository;
    }

    @Transactional
    public LoginResponseDTO login(JwtRequestUser jwtRequestUser) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(jwtRequestUser.getUserName(), jwtRequestUser.getPassword())
            );

            String accessToken = jwtTokenUtil.generateToken(authentication);
            String refreshToken = jwtTokenUtil.generateRefreshToken(authentication);

            var authenticatedUser = (AuthenticateUser) authentication.getPrincipal();

            User findUserShare = userRepository.findByOid(authenticatedUser.getOid()).get();
            UserOwn user = new UserOwn();
            user.setOid(findUserShare.getOid());
            user.setName(Utils.trimString(findUserShare.getName()));
            user.setUsername(Utils.trimString(findUserShare.getUsername()));
            user.setEmail(findUserShare.getEmail());
            userOwnRepository.save(user);

            return new LoginResponseDTO(accessToken, refreshToken);
        } catch (AuthenticationException e) {
            throw new UsernameNotFoundException("Username or Password is incorrect");
        } catch (Exception e) {
            throw new GeneralException("There is a problem. Please try again later.");
        }
    }

    @Transactional
    public UserOwn loginWithMicrosoft(MicrosoftTokenRequest microsoftTokenRequest) {
        try {
            MicrosoftTokenPayload payload = validateAndParseToken(microsoftTokenRequest.getToken());

            UserOwn user = userOwnRepository.findByOid(payload.getOid())
                    .orElse(new UserOwn());
            user.setOid(payload.getOid());
            user.setName(Utils.trimString(payload.getName()));
            user.setUsername(Utils.trimString(payload.getPreferredUsername()));
            user.setEmail(payload.getEmail());

            return userOwnRepository.save(user);
        } catch (Exception e) {
            throw new GeneralException("Unable to process Microsoft token.");
        }
    }

    public RefreshTokenResponseDTO refreshAccessToken(Claims claims) {
        String oid = JwtTokenUtil.getOidFromClaims(claims);

        User user = userRepository.findByOid(oid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());
        String newAccessToken = jwtTokenUtil.generateToken(authentication);

        return new RefreshTokenResponseDTO(newAccessToken);
    }

    public MicrosoftTokenPayload validateAndParseToken(String token) {
        try {
            URL jwksURL = new URL(AUTH_JWK_URL);
            JWKSource keySource = new RemoteJWKSet<>(jwksURL);

            SignedJWT signedJWT = SignedJWT.parse(token);

            String keyID = signedJWT.getHeader().getKeyID();
            JWK jwk = (JWK) keySource.get(new JWKSelector(new JWKMatcher.Builder().keyID(keyID).build()), null)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new GeneralException("Unable to find matching JWK"));

            if (!jwk.getKeyType().equals(KeyType.RSA)) {
                throw new GeneralException("Unexpected key type");
            }

            JWSVerifier verifier = new RSASSAVerifier((RSAPublicKey) jwk.toRSAKey().toPublicKey());
            if (!signedJWT.verify(verifier)) {
                throw new GeneralException("Invalid signature");
            }

            MicrosoftTokenPayload payload = new MicrosoftTokenPayload();
            payload.setOid(signedJWT.getJWTClaimsSet().getStringClaim("oid"));
            payload.setName(signedJWT.getJWTClaimsSet().getStringClaim("name"));
            payload.setPreferredUsername(signedJWT.getJWTClaimsSet().getStringClaim("preferred_username"));
            payload.setEmail(signedJWT.getJWTClaimsSet().getStringClaim("preferred_username"));

            return payload;
        } catch (Exception e) {
            throw new GeneralException("Invalid Microsoft token: " + e.getMessage());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}

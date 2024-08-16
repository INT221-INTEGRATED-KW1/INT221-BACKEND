package sit.int221.integratedproject.kanbanborad.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import sit.int221.integratedproject.kanbanborad.dtos.request.JwtRequestUser;
import sit.int221.integratedproject.kanbanborad.dtos.response.LoginResponseDTO;
import sit.int221.integratedproject.kanbanborad.exceptions.GeneralException;
import sit.int221.integratedproject.kanbanborad.services.JwtTokenUtil;
import sit.int221.integratedproject.kanbanborad.services.JwtUserDetailsService;

@RestController
@RequestMapping("/v2/users")
@CrossOrigin(origins = "http://localhost")
public class AuthController {
    @Autowired
    JwtUserDetailsService jwtUserDetailsService;
    @Autowired
    JwtTokenUtil jwtTokenUtil;
    @Autowired
    AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody @Valid JwtRequestUser jwtRequestUser) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(jwtRequestUser.getUsername(), jwtRequestUser.getPassword())
            );
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            //        UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(jwtRequestUser.getUserName());
            String token = jwtTokenUtil.generateToken(userDetails);
            return ResponseEntity.ok(new LoginResponseDTO(token));
        } catch (AuthenticationException e) {
             throw new UsernameNotFoundException("Username or Password is incorrect");
        } catch (Exception e) {
            throw new GeneralException("There is a problem. Please try again later.");
        }
    }

}

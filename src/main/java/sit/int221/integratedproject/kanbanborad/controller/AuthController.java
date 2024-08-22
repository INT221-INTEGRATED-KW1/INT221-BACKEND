package sit.int221.integratedproject.kanbanborad.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sit.int221.integratedproject.kanbanborad.dtos.request.JwtRequestUser;
import sit.int221.integratedproject.kanbanborad.services.AuthService;

@RestController
@RequestMapping("/v2/users")
@CrossOrigin(origins = {"http://ip23kw1.sit.kmutt.ac.th", "http://intproj23.sit.kmutt.ac.th"})
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody @Valid JwtRequestUser jwtRequestUser) {
        return ResponseEntity.ok(authService.login(jwtRequestUser));
    }

}

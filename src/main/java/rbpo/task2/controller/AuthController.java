package rbpo.task2.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import rbpo.task2.dto.Registration;
import rbpo.task2.model.User;
import rbpo.task2.service.RegistrationService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final RegistrationService registration;
    public AuthController(RegistrationService registration) { this.registration = registration; }
    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) { return token; }
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public User register(@Valid @RequestBody Registration request) { return registration.register(request); }
}

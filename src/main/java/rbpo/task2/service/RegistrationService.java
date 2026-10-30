package rbpo.task2.service;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import rbpo.task2.dto.Registration;
import rbpo.task2.model.User;
import rbpo.task2.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class RegistrationService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    public RegistrationService(UserRepository users, PasswordEncoder encoder) { this.users = users; this.encoder = encoder; }
    public User register(Registration request) {
        String password = request.password();
        if (password.getBytes(StandardCharsets.UTF_8).length > 72
                || !password.matches("(?s).*[A-Z].*") || !password.matches("(?s).*[a-z].*")
                || !password.matches("(?s).*[0-9].*") || !password.matches("(?s).*[^a-zA-Z0-9\\s].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пароль: от 10 символов, не более 72 байт, латинские буквы обоих регистров, цифра и спецсимвол");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Учётная запись уже существует");
        }
        User user = new User();
        user.setName(request.name().trim()); user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setRole("USER"); user.setEnabled(true); user.setLocked(false);
        return users.saveAndFlush(user);
    }
}

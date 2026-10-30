package rbpo.task2.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import rbpo.task2.model.User;
import rbpo.task2.service.UserService;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PostFilter;

@RestController //спринг находит этот класс при запуске и запоминает адреса
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;
    private final rbpo.task2.service.RegistrationService registration;

    public UserController(UserService service, rbpo.task2.service.RegistrationService registration) {
        this.service = service;
        this.registration = registration;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @PostFilter("@access.user(filterObject.id)")
    public List<User> getAll() {
        return service.getAll();
    }

    @PreAuthorize("@access.user(#id)")
    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) //201
    public User create(@jakarta.validation.Valid @RequestBody rbpo.task2.dto.Registration request) {
        return registration.register(request);
    }

    @PreAuthorize("@access.user(#id)")
    @PatchMapping("/{id}")
    public User update(@PathVariable Long id, @jakarta.validation.Valid @RequestBody rbpo.task2.dto.ProfileUpdate request) {
        return service.update(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) //204
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
    @PatchMapping("/{id}/state")
    @PreAuthorize("hasRole('ADMIN') and !@access.userId().equals(#id)")
    public User state(@PathVariable Long id, @jakarta.validation.Valid @RequestBody rbpo.task2.dto.AccountState request) {
        return service.state(id, request);
    }
}

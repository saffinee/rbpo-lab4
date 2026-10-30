package rbpo.task2.dto;

import jakarta.validation.constraints.*;

public record Registration(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 10, max = 72) String password) { }

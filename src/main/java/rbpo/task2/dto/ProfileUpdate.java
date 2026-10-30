package rbpo.task2.dto;
import jakarta.validation.constraints.*;
public record ProfileUpdate(@NotBlank @Size(max = 100) String name) { }

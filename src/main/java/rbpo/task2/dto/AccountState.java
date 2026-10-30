package rbpo.task2.dto;
import jakarta.validation.constraints.NotNull;
public record AccountState(@NotNull Boolean enabled, @NotNull Boolean locked) { }

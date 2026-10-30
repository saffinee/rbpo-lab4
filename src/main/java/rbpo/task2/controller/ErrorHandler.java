package rbpo.task2.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;
import java.util.Map;

@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict() { return error(409, "CONFLICT", "Данные конфликтуют с существующими записями или ограничениями базы"); }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<?> invalid() { return error(400, "BAD_REQUEST", "Проверьте поля и параметры запроса"); }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> business(ResponseStatusException ex) {
        return error(ex.getStatusCode().value(), "BUSINESS_ERROR", ex.getReason());
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> forbidden() { return error(403, "FORBIDDEN", "Недостаточно прав"); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected() { return error(500, "INTERNAL_ERROR", "Не удалось обработать запрос"); }
    private ResponseEntity<?> error(int status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("code", code, "message", message));
    }
}

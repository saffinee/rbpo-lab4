package rbpo.task2.security;

import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;

public class SecurityErrors implements AuthenticationEntryPoint, AccessDeniedHandler {
    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        res.setHeader("WWW-Authenticate", "Basic realm=\"parking\", charset=\"UTF-8\"");
        write(res, 401, "UNAUTHORIZED", "Требуются действующие учётные данные");
    }
    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        if (ex instanceof CsrfException) {
            write(res, 403, "CSRF_INVALID", "Получите CSRF-токен и передайте его вместе с cookie сессии");
        } else {
            write(res, 403, "FORBIDDEN", "Недостаточно прав");
        }
    }
    private void write(HttpServletResponse res, int status, String code, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}

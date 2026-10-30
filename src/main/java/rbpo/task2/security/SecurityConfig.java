package rbpo.task2.security;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;
import rbpo.task2.repository.UserRepository;
import java.util.Locale;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService userDetailsService(UserRepository repository) {
        return email -> {
            var user = repository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                    .orElseThrow(() -> new UsernameNotFoundException("Неверные учётные данные"));
            return User.withUsername(user.getEmail()).password(user.getPasswordHash())
                    .roles(user.getRole()).disabled(!user.isEnabled()).accountLocked(user.isLocked()).build();
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var errors = new SecurityErrors();
        return http
                // Basic проверяется заново в каждом запросе, сессия хранит только CSRF-токен.
                .securityContext(c -> c.securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(c -> c.disable())
                .csrf(org.springframework.security.config.Customizer.withDefaults())
                .httpBasic(c -> c.authenticationEntryPoint(errors))
                .exceptionHandling(c -> c.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .authorizeHttpRequests(c -> c
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .build();
    }
}

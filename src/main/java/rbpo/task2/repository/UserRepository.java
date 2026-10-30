package rbpo.task2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rbpo.task2.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    java.util.Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}

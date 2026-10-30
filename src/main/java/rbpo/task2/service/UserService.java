package rbpo.task2.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import rbpo.task2.model.User;
import rbpo.task2.repository.UserRepository;

import java.util.List;

@Service //сервис - тут вся логика, контроллер только принимает запрос и отдаёт ответ
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
    }

    public User update(Long id, rbpo.task2.dto.ProfileUpdate request) {
        User user = getById(id);
        user.setName(request.name().trim());
        return userRepository.save(user);
    }

    public User state(Long id, rbpo.task2.dto.AccountState request) {
        User user = getById(id);
        user.setEnabled(request.enabled()); user.setLocked(request.locked());
        return userRepository.save(user);
    }

    public void delete(Long id) {
        getById(id);
        userRepository.deleteById(id); //машины пользователя база удалит сама (ON DELETE CASCADE)
    }
}

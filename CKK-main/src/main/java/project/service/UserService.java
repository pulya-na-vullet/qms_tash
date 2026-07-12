package project.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.model.User;
import project.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public User createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new DataIntegrityViolationException("Пользователь с таким именем уже существует");
        }
        if (user.getEmail() != null && userRepository.existsByEmail(user.getEmail())) {
            throw new DataIntegrityViolationException("Пользователь с таким email уже существует");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(true);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Проверка уникальности username (если изменился)
        if (!user.getUsername().equals(userDetails.getUsername()) &&
                userRepository.existsByUsername(userDetails.getUsername())) {
            throw new DataIntegrityViolationException("Пользователь с таким именем уже существует");
        }

        // Проверка уникальности email (если изменился)
        if (userDetails.getEmail() != null &&
                !userDetails.getEmail().equals(user.getEmail()) &&
                userRepository.existsByEmail(userDetails.getEmail())) {
            throw new DataIntegrityViolationException("Пользователь с таким email уже существует");
        }

        user.setUsername(userDetails.getUsername());
        user.setFullName(userDetails.getFullName());
        user.setEmail(userDetails.getEmail());
        user.setRoles(userDetails.getRoles());

        // Если пароль не пустой - обновляем его
        if (userDetails.getPassword() != null && !userDetails.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deactivateUser(Long id, String reason) {
        userRepository.findById(id).ifPresent(user -> {
            user.setEnabled(false);
            user.setDeactivationReason(reason);
            userRepository.save(user);
        });
    }

    @Transactional
    public void activateUser(Long id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setEnabled(true);
            user.setDeactivationReason(null);
            userRepository.save(user);
        });
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public long getTotalUsers() {
        return userRepository.count();
    }

    public long getActiveUsers() {
        return userRepository.countByEnabled(true);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Получение пользователя по email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
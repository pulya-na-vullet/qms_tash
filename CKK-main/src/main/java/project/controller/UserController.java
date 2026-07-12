package project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.model.Role;
import project.model.User;
import project.model.dto.*;
import project.service.UserService;

import jakarta.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/users")
public class UserController {

    @Autowired
    private UserService userService;

    // ========== WEB ENDPOINTS (HTML) ==========

    /**
     * Страница управления пользователями
     */
    @GetMapping
    public String userManagementPage(Model model) {
        // Добавляем пустого пользователя для формы
        model.addAttribute("newUser", new User());

        // ДОБАВЛЕНО: список всех пользователей для отображения в таблице
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);

        // Добавляем список ролей
        model.addAttribute("roles", Arrays.asList(Role.values()));

        // Добавляем статистику
        model.addAttribute("totalUsers", userService.getTotalUsers());
        model.addAttribute("activeUsers", userService.getActiveUsers());
        model.addAttribute("adminUsers", getAdminUsersCount());

        return "admin/users";
    }

    /**
     * Создание пользователя через форму (для обратной совместимости)
     */
    @PostMapping
    public String createUser(@ModelAttribute("newUser") @Valid User user,
                             BindingResult result,
                             @RequestParam("role") Role role,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка валидации данных");
            return "redirect:/admin/users";
        }

        try {
            // Устанавливаем роль
            user.setRoles(Set.of(role));
            userService.createUser(user);
            redirectAttributes.addFlashAttribute("successMessage", "Пользователь создан успешно!");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Пользователь с таким именем уже существует");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при создании пользователя: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    /**
     * Деактивация пользователя через форму
     */
    @PostMapping("/{id}/deactivate")
    public String deactivateUser(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            userService.deactivateUser(id, "Деактивирован администратором через веб-интерфейс");
            redirectAttributes.addFlashAttribute("successMessage", "Пользователь деактивирован");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при деактивации пользователя: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Активация пользователя через форму
     */
    @PostMapping("/{id}/activate")
    public String activateUser(@PathVariable Long id,
                               RedirectAttributes redirectAttributes) {
        try {
            userService.activateUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Пользователь активирован");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при активации пользователя: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ========== API ENDPOINTS (JSON) ==========

    /**
     * Получить всех пользователей (API)
     */
    @ResponseBody
    @GetMapping("/api")
    public ApiResponse<List<UserDTO>> getAllUsersAPI() {
        try {
            List<User> users = userService.getAllUsers();
            List<UserDTO> userDTOs = users.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ApiResponse.success(userDTOs, "Пользователи получены успешно");
        } catch (Exception e) {
            return ApiResponse.error("Ошибка при получении пользователей: " + e.getMessage());
        }
    }

    /**
     * Получить пользователя по ID (API)
     */
    @ResponseBody
    @GetMapping("/api/{id}")
    public ApiResponse<UserDTO> getUserByIdAPI(@PathVariable Long id) {
        try {
            User user = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            return ApiResponse.success(convertToDTO(user), "Пользователь получен успешно");
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Создать пользователя (API)
     */
    @ResponseBody
    @PostMapping("/api")
    public ApiResponse<UserDTO> createUserAPI(@RequestBody CreateUserRequest request) {
        try {
            // Валидация
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                throw new RuntimeException("Имя пользователя обязательно");
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                throw new RuntimeException("Пароль обязателен");
            }
            if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
                throw new RuntimeException("Полное имя обязательно");
            }
            if (request.getRoles() == null || request.getRoles().isEmpty()) {
                throw new RuntimeException("Необходимо указать хотя бы одну роль");
            }

            User user = User.builder()
                    .username(request.getUsername().trim())
                    .password(request.getPassword())
                    .fullName(request.getFullName().trim())
                    .email(request.getEmail() != null ? request.getEmail().trim() : null)
                    .roles(new HashSet<>(request.getRoles()))
                    .build();

            User createdUser = userService.createUser(user);

            return ApiResponse.success(convertToDTO(createdUser), "Пользователь создан успешно");
        } catch (DataIntegrityViolationException e) {
            return ApiResponse.error("Пользователь с таким именем или email уже существует");
        } catch (Exception e) {
            return ApiResponse.error("Ошибка при создании пользователя: " + e.getMessage());
        }
    }

    /**
     * Обновить пользователя (API)
     */
    @ResponseBody
    @PutMapping("/api/{id}")
    public ApiResponse<UserDTO> updateUserAPI(@PathVariable Long id,
                                              @RequestBody UpdateUserRequest request) {
        try {
            // Валидация
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                throw new RuntimeException("Имя пользователя обязательно");
            }
            if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
                throw new RuntimeException("Полное имя обязательно");
            }
            if (request.getRoles() == null || request.getRoles().isEmpty()) {
                throw new RuntimeException("Необходимо указать хотя бы одну роль");
            }

            // Создаем объект пользователя с обновленными данными
            User userDetails = User.builder()
                    .username(request.getUsername().trim())
                    .password(request.getPassword()) // может быть null (не менять пароль)
                    .fullName(request.getFullName().trim())
                    .email(request.getEmail() != null ? request.getEmail().trim() : null)
                    .roles(new HashSet<>(request.getRoles()))
                    .build();

            User updatedUser = userService.updateUser(id, userDetails);

            return ApiResponse.success(convertToDTO(updatedUser), "Пользователь обновлен успешно");
        } catch (DataIntegrityViolationException e) {
            return ApiResponse.error("Пользователь с таким именем или email уже существует");
        } catch (Exception e) {
            return ApiResponse.error("Ошибка при обновлении пользователя: " + e.getMessage());
        }
    }

    /**
     * Деактивировать пользователя (API)
     */
    @ResponseBody
    @PostMapping("/api/{id}/deactivate")
    public ApiResponse<Void> deactivateUserAPI(@PathVariable Long id,
                                               @RequestBody(required = false) DeactivationRequest request) {
        try {
            String reason = (request != null && request.getReason() != null) ?
                    request.getReason() : "Деактивирован через API";

            userService.deactivateUser(id, reason);

            return ApiResponse.success(null, "Пользователь деактивирован");
        } catch (Exception e) {
            return ApiResponse.error("Ошибка при деактивации пользователя: " + e.getMessage());
        }
    }

    /**
     * Активировать пользователя (API)
     */
    @ResponseBody
    @PostMapping("/api/{id}/activate")
    public ApiResponse<Void> activateUserAPI(@PathVariable Long id) {
        try {
            userService.activateUser(id);

            return ApiResponse.success(null, "Пользователь активирован");
        } catch (Exception e) {
            return ApiResponse.error("Ошибка при активации пользователя: " + e.getMessage());
        }
    }

    /**
     * Удалить пользователя (API)
     */
    @ResponseBody
    @DeleteMapping("/api/{id}")
    public ApiResponse<Void> deleteUserAPI(@PathVariable Long id) {
        try {
            userService.deleteUser(id);

            return ApiResponse.success(null, "Пользователь удален");
        } catch (Exception e) {
            return ApiResponse.error("Ошибка при удалении пользователя: " + e.getMessage());
        }
    }

    /**
     * Поиск пользователей (API)
     */
    @ResponseBody
    @GetMapping("/api/search")
    public ApiResponse<List<UserDTO>> searchUsersAPI(@RequestParam String query) {
        try {
            List<User> users = userService.getAllUsers().stream()
                    .filter(user -> user.getUsername().toLowerCase().contains(query.toLowerCase()) ||
                            (user.getFullName() != null && user.getFullName().toLowerCase().contains(query.toLowerCase())) ||
                            (user.getEmail() != null && user.getEmail().toLowerCase().contains(query.toLowerCase())))
                    .collect(Collectors.toList());

            List<UserDTO> userDTOs = users.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ApiResponse.success(userDTOs, "Поиск выполнен успешно");
        } catch (Exception e) {
            return ApiResponse.error("Ошибка при поиске пользователей: " + e.getMessage());
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Конвертация User в UserDTO
     */
    private UserDTO convertToDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRoles(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getDeactivationReason()
        );
    }

    /**
     * Получение количества администраторов
     */
    private long getAdminUsersCount() {
        return userService.getAllUsers().stream()
                .filter(user -> user.getRoles().contains(Role.ADMIN))
                .count();
    }

    // ========== DTO для стандартизированных ответов ==========

    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public ApiResponse(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public static <T> ApiResponse<T> success(T data, String message) {
            return new ApiResponse<>(true, message, data);
        }

        public static <T> ApiResponse<T> success(T data) {
            return new ApiResponse<>(true, "Успешно", data);
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null);
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
    }
}
package eu.bb.app.backend.controller;

import eu.bb.app.backend.entity.User;
import eu.bb.app.backend.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "API для управления пользователями (родителями/хозяевами)")
public class UsersController {
    private static final Logger log = LoggerFactory.getLogger(UsersController.class);
    private final UserRepository repo;
    
    public UsersController(UserRepository repo) { this.repo = repo; }
    
    @GetMapping
    @Operation(summary = "Получить всех пользователей", description = "Возвращает список всех зарегистрированных пользователей")
    @ApiResponse(responseCode = "200", description = "Список пользователей успешно получен")
    public List<User> all() {
        log.debug("Getting all users");
        List<User> users = repo.findAll();
        log.info("Retrieved {} users", users.size());
        return users;
    }
    
    @PostMapping
    @Operation(summary = "Создать пользователя", description = "Создает нового пользователя в системе")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пользователь успешно создан"),
        @ApiResponse(responseCode = "400", description = "Некорректные данные пользователя")
    })
    public User create(@RequestBody User u) {
        log.info("Creating new user: {}", u.getEmail());
        // Убеждаемся, что ID установлен в null для нового пользователя
        // Это предотвращает использование merge вместо persist
        if (u.getId() != null && u.getId() == 0) {
            u.setId(null);
        }
        User saved = repo.save(u);
        log.info("User created successfully with ID: {}", saved.getId());
        return saved;
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по ID", description = "Возвращает информацию о пользователе по его идентификатору")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пользователь найден"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public User get(@Parameter(description = "ID пользователя", required = true) @PathVariable Long id) {
        log.debug("Getting user with ID: {}", id);
        User user = repo.findById(id).orElseThrow(() -> {
            log.warn("User not found with ID: {}", id);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        });
        log.debug("User found: {}", user.getEmail());
        return user;
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Обновить пользователя", description = "Обновляет информацию о существующем пользователе")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пользователь успешно обновлен"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public User update(@Parameter(description = "ID пользователя", required = true) @PathVariable Long id, @RequestBody User u) {
        log.info("Updating user with ID: {}", id);
        u.setId(id);
        User updated = repo.save(u);
        log.info("User updated successfully: {}", updated.getEmail());
        return updated;
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить пользователя", description = "Удаляет пользователя из системы")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пользователь успешно удален"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public void delete(@Parameter(description = "ID пользователя", required = true) @PathVariable Long id) {
        log.info("Deleting user with ID: {}", id);
        repo.deleteById(id);
        log.info("User deleted successfully: {}", id);
    }
}

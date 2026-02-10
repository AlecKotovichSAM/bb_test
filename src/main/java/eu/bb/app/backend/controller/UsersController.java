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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.Base64;

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
    @Operation(summary = "Обновить пользователя", description = "Обновляет информацию о существующем пользователе. Можно обновить аватар через поле avatar (base64 строка)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пользователь успешно обновлен"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public User update(@Parameter(description = "ID пользователя", required = true) @PathVariable Long id, @RequestBody User u) {
        log.info("Updating user with ID: {}", id);
        User existing = repo.findById(id).orElseThrow(() -> {
            log.warn("User not found with ID: {}", id);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        });
        
        // Сохраняем существующий аватар, если новый не указан
        if (u.getAvatar() == null) {
            u.setAvatar(existing.getAvatar());
        }
        
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
    
    @PutMapping("/{id}/avatar")
    @Operation(summary = "Обновить аватар пользователя", description = "Обновляет аватар пользователя. Принимает base64 строку в формате data:image/jpeg;base64,... или data:image/png;base64,...")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Аватар успешно обновлен"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public Map<String, String> updateAvatar(
            @Parameter(description = "ID пользователя", required = true) @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        log.info("Updating avatar for user ID: {}", id);
        User user = repo.findById(id).orElseThrow(() -> {
            log.warn("User not found with ID: {}", id);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        });
        
        String avatar = request.get("avatar");
        if (avatar != null && !avatar.isEmpty()) {
            // Валидация формата base64 строки (опционально)
            if (!avatar.startsWith("data:image/")) {
                log.warn("Invalid avatar format for user ID: {}. Expected format: data:image/jpeg;base64,... or data:image/png;base64,...", id);
            }
            user.setAvatar(avatar);
            repo.save(user);
            log.info("Avatar updated successfully for user ID: {}", id);
        } else {
            // Если avatar пустой или null - удаляем аватар
            user.setAvatar(null);
            repo.save(user);
            log.info("Avatar removed for user ID: {}", id);
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("avatar", user.getAvatar());
        return response;
    }
    
    @GetMapping("/{id}/avatar")
    @Operation(summary = "Получить аватар пользователя", description = "Возвращает аватар пользователя как бинарные данные изображения. Можно использовать напрямую в теге <img src=\"/api/users/{id}/avatar\">")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Аватар найден"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден или аватар не установлен")
    })
    public ResponseEntity<byte[]> getAvatar(
            @Parameter(description = "ID пользователя", required = true) @PathVariable Long id) {
        log.debug("Getting avatar for user ID: {}", id);
        User user = repo.findById(id).orElseThrow(() -> {
            log.warn("User not found with ID: {}", id);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        });
        
        if (user.getAvatar() == null || user.getAvatar().isEmpty()) {
            log.debug("Avatar not set for user ID: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Avatar not found");
        }
        
        String avatarDataUri = user.getAvatar();
        
        // Определяем Content-Type и извлекаем base64 данные из data URI
        MediaType contentType = MediaType.IMAGE_JPEG; // по умолчанию
        byte[] imageBytes = null;
        
        if (avatarDataUri.startsWith("data:image/")) {
            // Извлекаем тип изображения из data URI
            int semicolonIndex = avatarDataUri.indexOf(';');
            if (semicolonIndex > 0) {
                String mimeType = avatarDataUri.substring(5, semicolonIndex); // "data:image/jpeg" -> "image/jpeg"
                try {
                    contentType = MediaType.parseMediaType(mimeType);
                } catch (Exception e) {
                    log.warn("Could not parse MIME type from avatar: {}", mimeType);
                    contentType = MediaType.IMAGE_JPEG; // fallback
                }
            }
            
            // Извлекаем base64 данные (после "base64,")
            int base64Index = avatarDataUri.indexOf("base64,");
            if (base64Index > 0) {
                String base64Data = avatarDataUri.substring(base64Index + 7); // "base64," имеет 7 символов
                try {
                    imageBytes = Base64.getDecoder().decode(base64Data);
                } catch (IllegalArgumentException e) {
                    log.error("Failed to decode base64 avatar data for user ID: {}", id, e);
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid avatar data");
                }
            } else {
                log.error("Invalid avatar data URI format for user ID: {}", id);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid avatar format");
            }
        } else {
            // Если формат не data URI, пытаемся декодировать как чистый base64
            try {
                imageBytes = Base64.getDecoder().decode(avatarDataUri);
            } catch (IllegalArgumentException e) {
                log.error("Failed to decode avatar as base64 for user ID: {}", id, e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid avatar data");
            }
        }
        
        log.debug("Avatar retrieved successfully for user ID: {}, Content-Type: {}, size: {} bytes", 
                id, contentType, imageBytes != null ? imageBytes.length : 0);
        return ResponseEntity.ok()
                .contentType(contentType)
                .body(imageBytes);
    }
}

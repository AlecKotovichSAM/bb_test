package eu.bb.app.backend.controller;

import eu.bb.app.backend.entity.Child;
import eu.bb.app.backend.entity.ChildAvatar;
import eu.bb.app.backend.repository.ChildRepository;
import eu.bb.app.backend.repository.ChildAvatarRepository;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

@RestController
@Tag(name = "Children", description = "API для управления детьми пользователей")
public class ChildrenController {
    private static final Logger log = LoggerFactory.getLogger(ChildrenController.class);
    private final ChildRepository repo;
    private final ChildAvatarRepository avatarRepository;
    
    public ChildrenController(ChildRepository repo, ChildAvatarRepository avatarRepository) { 
        this.repo = repo; 
        this.avatarRepository = avatarRepository;
    }
    
    @PostMapping("/api/users/{userId}/children")
    @Operation(summary = "Добавить ребенка пользователю", description = "Создает нового ребенка для указанного пользователя. Можно указать аватар в поле 'avatar' в теле запроса (формат: data:image/jpeg;base64,... или data:image/png;base64,...).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ребенок успешно добавлен"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public Child create(
            @Parameter(description = "ID пользователя (родителя)", required = true) @PathVariable Long userId, 
            @RequestBody Map<String, Object> request) {
        log.info("Creating new child for user ID: {}", userId);
        
        // Используем ObjectMapper для правильной десериализации Child из Map
        // Игнорируем неизвестные свойства (например, "avatar", которого нет в Child entity)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // Извлекаем avatar до конвертации, так как его нет в Child entity
        Object avatarObj = request.get("avatar");
        String avatar = null;
        if (avatarObj != null && !avatarObj.toString().isEmpty() && !"null".equals(avatarObj.toString())) {
            avatar = avatarObj.toString();
        }
        
        // Удаляем avatar из request перед конвертацией в Child
        Map<String, Object> childData = new HashMap<>(request);
        childData.remove("avatar");
        
        Child c = objectMapper.convertValue(childData, Child.class);
        
        // Убеждаемся, что ID установлен в null для нового ребенка
        if (c.getId() != null && c.getId() == 0) {
            c.setId(null);
        }
        
        c.setUserId(userId);
        Child saved = repo.save(c);
        log.info("Child created successfully with ID: {}", saved.getId());
        
        // Если в запросе указан аватар, сохраняем его
        if (avatar != null && !avatar.isEmpty()) {
            ChildAvatar childAvatar = new ChildAvatar();
            childAvatar.setChildId(saved.getId());
            childAvatar.setAvatar(avatar);
            avatarRepository.save(childAvatar);
            log.info("Avatar saved for child ID: {}", saved.getId());
        }
        
        return saved;
    }
    
    @GetMapping("/api/users/{userId}/children")
    @Operation(summary = "Получить детей пользователя", description = "Возвращает список всех детей указанного пользователя")
    @ApiResponse(responseCode = "200", description = "Список детей успешно получен")
    public List<Child> list(@Parameter(description = "ID пользователя", required = true) @PathVariable Long userId) { 
        return repo.findByUserId(userId); 
    }
    
    @GetMapping("/api/children/{id}")
    @Operation(summary = "Получить ребенка по ID", description = "Возвращает информацию о ребенке по его идентификатору")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ребенок найден"),
        @ApiResponse(responseCode = "404", description = "Ребенок не найден")
    })
    public Child get(@Parameter(description = "ID ребенка", required = true) @PathVariable Long id) { 
        return repo.findById(id).orElseThrow(); 
    }
    
    @PutMapping("/api/children/{id}")
    @Operation(summary = "Обновить информацию о ребенке", description = "Обновляет данные существующего ребенка")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Информация о ребенке успешно обновлена"),
        @ApiResponse(responseCode = "404", description = "Ребенок не найден")
    })
    public Child update(@Parameter(description = "ID ребенка", required = true) @PathVariable Long id, @RequestBody Child c) { 
        c.setId(id); 
        return repo.save(c); 
    }
    
    @DeleteMapping("/api/children/{id}")
    @Operation(summary = "Удалить ребенка", description = "Удаляет ребенка из системы")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ребенок успешно удален"),
        @ApiResponse(responseCode = "404", description = "Ребенок не найден")
    })
    public void delete(@Parameter(description = "ID ребенка", required = true) @PathVariable Long id) { 
        repo.deleteById(id); 
    }
    
    @PutMapping("/api/children/{id}/avatar")
    @Operation(summary = "Обновить аватар ребенка", description = "Обновляет аватар ребенка. Принимает base64 строку в формате data:image/jpeg;base64,... или data:image/png;base64,...")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Аватар успешно обновлен"),
        @ApiResponse(responseCode = "404", description = "Ребенок не найден")
    })
    public Map<String, String> updateAvatar(
            @Parameter(description = "ID ребенка", required = true) @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        log.info("Updating avatar for child ID: {}", id);
        Child child = repo.findById(id).orElseThrow(() -> {
            log.warn("Child not found with ID: {}", id);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found");
        });
        
        String avatar = request.get("avatar");
        if (avatar != null && !avatar.isEmpty()) {
            // Валидация формата base64 строки (опционально)
            if (!avatar.startsWith("data:image/")) {
                log.warn("Invalid avatar format for child ID: {}. Expected format: data:image/jpeg;base64,... or data:image/png;base64,...", id);
            }
            
            // Находим существующий аватар или создаем новый
            ChildAvatar childAvatar = avatarRepository.findByChildId(id)
                    .orElse(new ChildAvatar());
            childAvatar.setChildId(id);
            childAvatar.setAvatar(avatar);
            avatarRepository.save(childAvatar);
            log.info("Avatar updated successfully for child ID: {}", id);
        } else {
            // Если avatar пустой или null - удаляем аватар
            avatarRepository.deleteByChildId(id);
            log.info("Avatar removed for child ID: {}", id);
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("avatar", avatar != null ? avatar : "");
        return response;
    }
    
    @GetMapping("/api/children/{id}/avatar")
    @Operation(summary = "Получить аватар ребенка", description = "Возвращает аватар ребенка как бинарные данные изображения. Можно использовать напрямую в теге <img src=\"/api/children/{id}/avatar\">")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Аватар найден"),
        @ApiResponse(responseCode = "404", description = "Ребенок не найден или аватар не установлен")
    })
    public ResponseEntity<byte[]> getAvatar(
            @Parameter(description = "ID ребенка", required = true) @PathVariable Long id) {
        log.debug("Getting avatar for child ID: {}", id);
        Child child = repo.findById(id).orElseThrow(() -> {
            log.warn("Child not found with ID: {}", id);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found");
        });
        
        ChildAvatar childAvatar = avatarRepository.findByChildId(id)
                .orElseThrow(() -> {
                    log.debug("Avatar not set for child ID: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Avatar not found");
                });
        
        String avatarDataUri = childAvatar.getAvatar();
        
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
                    log.error("Failed to decode base64 avatar data for child ID: {}", id, e);
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid avatar data");
                }
            } else {
                log.error("Invalid avatar data URI format for child ID: {}", id);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid avatar format");
            }
        } else {
            // Если формат не data URI, пытаемся декодировать как чистый base64
            try {
                imageBytes = Base64.getDecoder().decode(avatarDataUri);
            } catch (IllegalArgumentException e) {
                log.error("Failed to decode avatar as base64 for child ID: {}", id, e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid avatar data");
            }
        }
        
        log.debug("Avatar retrieved successfully for child ID: {}, Content-Type: {}, size: {} bytes", 
                id, contentType, imageBytes != null ? imageBytes.length : 0);
        return ResponseEntity.ok()
                .contentType(contentType)
                .body(imageBytes);
    }
}

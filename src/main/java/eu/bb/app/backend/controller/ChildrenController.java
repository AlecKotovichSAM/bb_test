package eu.bb.app.backend.controller;

import eu.bb.app.backend.entity.Child;
import eu.bb.app.backend.repository.ChildRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@Tag(name = "Children", description = "API для управления детьми пользователей")
public class ChildrenController {
    private final ChildRepository repo;
    public ChildrenController(ChildRepository repo) { this.repo = repo; }
    
    @PostMapping("/api/users/{userId}/children")
    @Operation(summary = "Добавить ребенка пользователю", description = "Создает нового ребенка для указанного пользователя")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ребенок успешно добавлен"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public Child create(
            @Parameter(description = "ID пользователя (родителя)", required = true) @PathVariable Long userId, 
            @RequestBody Child c) {
        // Убеждаемся, что ID установлен в null для нового ребенка
        if (c.getId() != null && c.getId() == 0) {
            c.setId(null);
        }
        c.setUserId(userId); 
        return repo.save(c); 
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
}

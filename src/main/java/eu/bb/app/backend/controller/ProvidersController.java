package eu.bb.app.backend.controller;

import eu.bb.app.backend.entity.Provider;
import eu.bb.app.backend.repository.ProviderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/providers")
@Tag(name = "Providers", description = "API для управления провайдерами локаций (места проведения событий)")
public class ProvidersController {
    private final ProviderRepository repo;
    public ProvidersController(ProviderRepository repo) { this.repo = repo; }
    
    @GetMapping
    @Operation(summary = "Получить всех провайдеров", description = "Возвращает список всех провайдеров локаций (клубы, кафе, парки развлечений и т.д.)")
    @ApiResponse(responseCode = "200", description = "Список провайдеров успешно получен")
    public List<Provider> all() { return repo.findAll(); }
    
    @PostMapping
    @Operation(summary = "Создать провайдера", description = "Добавляет нового провайдера локации в систему")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Провайдер успешно создан"),
        @ApiResponse(responseCode = "400", description = "Некорректные данные провайдера")
    })
    public Provider create(@RequestBody Provider p) {
        // Убеждаемся, что ID установлен в null для нового провайдера
        if (p.getId() != null && p.getId() == 0) {
            p.setId(null);
        }
        return repo.save(p);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Получить провайдера по ID", description = "Возвращает информацию о провайдере по его идентификатору")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Провайдер найден"),
        @ApiResponse(responseCode = "404", description = "Провайдер не найден")
    })
    public Provider get(@Parameter(description = "ID провайдера", required = true) @PathVariable Long id) { 
        return repo.findById(id).orElseThrow(); 
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Обновить провайдера", description = "Обновляет информацию о существующем провайдере")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Провайдер успешно обновлен"),
        @ApiResponse(responseCode = "404", description = "Провайдер не найден")
    })
    public Provider update(
            @Parameter(description = "ID провайдера", required = true) @PathVariable Long id, 
            @RequestBody Provider p) { 
        p.setId(id); 
        return repo.save(p); 
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить провайдера", description = "Удаляет провайдера из системы")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Провайдер успешно удален"),
        @ApiResponse(responseCode = "404", description = "Провайдер не найден")
    })
    public void delete(@Parameter(description = "ID провайдера", required = true) @PathVariable Long id) { 
        repo.deleteById(id); 
    }
}

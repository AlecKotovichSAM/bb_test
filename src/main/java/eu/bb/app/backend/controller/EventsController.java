package eu.bb.app.backend.controller;

import eu.bb.app.backend.entity.Event;
import eu.bb.app.backend.repository.EventRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Events", description = "API для управления событиями (днями рождения)")
public class EventsController {
    private static final Logger log = LoggerFactory.getLogger(EventsController.class);
    private final EventRepository repo;
    
    public EventsController(EventRepository repo) { this.repo = repo; }
    
    @PostMapping("/events")
    @Operation(summary = "Создать событие", description = "Создает новое событие (день рождения) для ребенка")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Событие успешно создано"),
        @ApiResponse(responseCode = "400", description = "Некорректные данные события")
    })
    public Event create(@RequestBody Event e) {
        log.info("Creating new event for child ID: {}, host ID: {}", e.getChildId(), e.getHostId());
        // Убеждаемся, что ID установлен в null для нового события
        if (e.getId() != null && e.getId() == 0) {
            e.setId(null);
        }
        Event saved = repo.save(e);
        log.info("Event created successfully with ID: {}, status: {}", saved.getId(), saved.getStatus());
        return saved;
    }
    
    @GetMapping("/events")
    @Operation(summary = "Получить все события", description = "Возвращает список всех событий в системе")
    @ApiResponse(responseCode = "200", description = "Список событий успешно получен")
    public List<Event> all() {
        log.debug("Getting all events");
        List<Event> events = repo.findAll();
        log.info("Retrieved {} events", events.size());
        return events;
    }
    
    @GetMapping("/users/{userId}/events")
    @Operation(summary = "Получить события пользователя", description = "Возвращает все события, где указанный пользователь является хозяином")
    @ApiResponse(responseCode = "200", description = "Список событий пользователя успешно получен")
    public List<Event> byUser(@Parameter(description = "ID пользователя (хозяина)", required = true) @PathVariable Long userId) {
        log.debug("Getting events for user ID: {}", userId);
        List<Event> events = repo.findByHostId(userId);
        log.info("Retrieved {} events for user ID: {}", events.size(), userId);
        return events;
    }
    
    @GetMapping("/events/{id}")
    @Operation(summary = "Получить событие по ID", description = "Возвращает информацию о событии по его идентификатору")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Событие найдено"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено")
    })
    public Event get(@Parameter(description = "ID события", required = true) @PathVariable Long id) {
        log.debug("Getting event with ID: {}", id);
        Event event = repo.findById(id).orElseThrow(() -> {
            log.warn("Event not found with ID: {}", id);
            return new RuntimeException("Event not found");
        });
        log.debug("Event found: ID={}, status={}", event.getId(), event.getStatus());
        return event;
    }
    
    @PutMapping("/events/{id}")
    @Operation(summary = "Обновить событие", description = "Обновляет информацию о существующем событии")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Событие успешно обновлено"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено")
    })
    public Event update(@Parameter(description = "ID события", required = true) @PathVariable Long id, @RequestBody Event e) {
        log.info("Updating event with ID: {}", id);
        e.setId(id);
        Event updated = repo.save(e);
        log.info("Event updated successfully: ID={}, status={}", updated.getId(), updated.getStatus());
        return updated;
    }
    
    @DeleteMapping("/events/{id}")
    @Operation(summary = "Удалить событие", description = "Удаляет событие из системы")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Событие успешно удалено"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено")
    })
    public void delete(@Parameter(description = "ID события", required = true) @PathVariable Long id) {
        log.info("Deleting event with ID: {}", id);
        repo.deleteById(id);
        log.info("Event deleted successfully: {}", id);
    }
    
    @PostMapping("/events/{id}/cancel")
    @Operation(summary = "Отменить событие", description = "Отменяет событие, устанавливая статус 'Cancelled'")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Событие успешно отменено"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено")
    })
    public Event cancel(@Parameter(description = "ID события", required = true) @PathVariable Long id) {
        log.info("Cancelling event with ID: {}", id);
        Event e = repo.findById(id).orElseThrow(() -> {
            log.warn("Event not found for cancellation: {}", id);
            return new RuntimeException("Event not found");
        });
        e.setStatus("Cancelled");
        Event cancelled = repo.save(e);
        log.info("Event cancelled successfully: ID={}", cancelled.getId());
        return cancelled;
    }
}

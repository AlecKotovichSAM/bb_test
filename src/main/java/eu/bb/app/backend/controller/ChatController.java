package eu.bb.app.backend.controller;

import eu.bb.app.backend.entity.ChatMessage;
import eu.bb.app.backend.repository.ChatMessageRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Chat", description = "API для обмена сообщениями в чате события")
public class ChatController {
    private final ChatMessageRepository repo;
    public ChatController(ChatMessageRepository repo) { this.repo = repo; }
    
    @GetMapping("/events/{eventId}/chat")
    @Operation(summary = "Получить сообщения чата события", description = "Возвращает все сообщения чата для указанного события, отсортированные по времени создания")
    @ApiResponse(responseCode = "200", description = "Список сообщений успешно получен")
    public List<ChatMessage> list(@Parameter(description = "ID события", required = true) @PathVariable Long eventId) { 
        return repo.findByEventIdOrderByCreatedAtAsc(eventId); 
    }
    
    @PostMapping("/events/{eventId}/chat")
    @Operation(summary = "Отправить сообщение в чат", description = "Добавляет новое сообщение в чат события")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Сообщение успешно отправлено"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено")
    })
    public ChatMessage post(
            @Parameter(description = "ID события", required = true) @PathVariable Long eventId, 
            @RequestBody ChatMessage m) {
        // Убеждаемся, что ID установлен в null для нового сообщения
        if (m.getId() != null && m.getId() == 0) {
            m.setId(null);
        }
        m.setEventId(eventId); 
        return repo.save(m); 
    }
}

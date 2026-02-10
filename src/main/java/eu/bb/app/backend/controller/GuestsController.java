package eu.bb.app.backend.controller;

import eu.bb.app.backend.entity.*;
import eu.bb.app.backend.repository.*;
import eu.bb.app.backend.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Guests", description = "API для управления гостями событий и приглашениями")
public class GuestsController {
    private static final Logger log = LoggerFactory.getLogger(GuestsController.class);
    private final EventGuestRepository guests;
    private final GuestRepository guestRepository;
    private final GuestTokenRepository tokens;
    private final TokenService tokenService;
    
    public GuestsController(EventGuestRepository guests, GuestRepository guestRepository, GuestTokenRepository tokens, TokenService tokenService) {
        this.guests = guests; 
        this.guestRepository = guestRepository;
        this.tokens = tokens; 
        this.tokenService = tokenService;
    }
    
    @PostMapping("/events/{eventId}/guests")
    @Operation(summary = "Добавить гостя к событию", description = "Добавляет нового гостя к указанному событию. Принимает guestId (для переиспользования существующего гостя) или guestName (для создания нового)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Гость успешно добавлен"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено"),
        @ApiResponse(responseCode = "400", description = "Некорректные данные гостя")
    })
    public EventGuest add(
            @Parameter(description = "ID события", required = true) @PathVariable Long eventId, 
            @RequestBody EventGuest g) {
        Guest guestEntity;
        
        // Если указан guestId - переиспользуем существующего гостя
        if (g.getGuestId() != null) {
            guestEntity = guestRepository.findById(g.getGuestId())
                    .orElseThrow(() -> {
                        log.warn("Guest not found with ID: {}", g.getGuestId());
                        return new RuntimeException("Guest not found with ID: " + g.getGuestId());
                    });
            log.info("Reusing guest ID: {}, name: {} for event ID: {}", 
                    guestEntity.getId(), guestEntity.getGuestName(), eventId);
        } else {
            // Создаем нового гостя или находим существующего по имени и userId
            String guestName = g.getGuestName();
            if (guestName == null || guestName.isEmpty()) {
                throw new RuntimeException("Guest name is required when guestId is not provided");
            }
            
            guestEntity = guestRepository.findByGuestNameAndUserId(guestName, g.getUserId())
                    .orElse(null);
            
            if (guestEntity == null) {
                // Создаем нового гостя в таблице guests
                guestEntity = new Guest();
                guestEntity.setGuestName(guestName);
                guestEntity.setUserId(g.getUserId());
                guestEntity = guestRepository.save(guestEntity);
                log.info("Created new guest ID: {}, name: {} for event ID: {}", 
                        guestEntity.getId(), guestEntity.getGuestName(), eventId);
            } else {
                log.info("Found existing guest ID: {}, name: {} for event ID: {}", 
                        guestEntity.getId(), guestEntity.getGuestName(), eventId);
            }
        }
        
        // Создаем EventGuest для текущего события
        EventGuest eventGuest = new EventGuest();
        eventGuest.setEventId(eventId);
        eventGuest.setGuest(guestEntity); // Устанавливаем связь - Hibernate использует guest.getId() для колонки guest_id
        eventGuest.setRsvpStatus("open");
        EventGuest saved = guests.save(eventGuest);
        log.info("EventGuest added successfully with ID: {} for event ID: {}", saved.getId(), eventId);
        return saved;
    }
    
    @GetMapping("/events/{eventId}/guests")
    @Operation(summary = "Получить список гостей события", description = "Возвращает всех гостей указанного события")
    @ApiResponse(responseCode = "200", description = "Список гостей успешно получен")
    public List<EventGuest> list(@Parameter(description = "ID события", required = true) @PathVariable Long eventId) {
        log.debug("Getting guests for event ID: {}", eventId);
        List<EventGuest> guestList = guests.findByEventId(eventId);
        // Загружаем связанных гостей из таблицы guests
        for (EventGuest eventGuest : guestList) {
            if (eventGuest.getGuest() == null && eventGuest.getGuestId() != null) {
                Guest guest = guestRepository.findById(eventGuest.getGuestId()).orElse(null);
                eventGuest.setGuest(guest);
            }
        }
        log.info("Retrieved {} guests for event ID: {}", guestList.size(), eventId);
        return guestList;
    }
    
    @PutMapping("/events/{eventId}/guests/{guestId}")
    @Operation(summary = "Обновить информацию о госте", description = "Обновляет данные гостя события")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Информация о госте успешно обновлена"),
        @ApiResponse(responseCode = "404", description = "Гость или событие не найдены")
    })
    public EventGuest update(
            @Parameter(description = "ID события", required = true) @PathVariable Long eventId, 
            @Parameter(description = "ID гостя", required = true) @PathVariable Long guestId, 
            @RequestBody EventGuest g) {
        log.info("Updating guest ID: {} for event ID: {}", guestId, eventId);
        g.setId(guestId); 
        g.setEventId(eventId); 
        EventGuest updated = guests.save(g);
        log.info("Guest updated successfully: ID={}, status={}", updated.getId(), updated.getRsvpStatus());
        return updated;
    }
    
    @DeleteMapping("/events/{eventId}/guests/{guestId}")
    @Operation(summary = "Удалить гостя", description = "Удаляет гостя из события")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Гость успешно удален"),
        @ApiResponse(responseCode = "404", description = "Гость не найден")
    })
    public void delete(
            @Parameter(description = "ID события", required = true) @PathVariable Long eventId, 
            @Parameter(description = "ID гостя", required = true) @PathVariable Long guestId) {
        log.info("Deleting guest ID: {} from event ID: {}", guestId, eventId);
        guests.deleteById(guestId);
        log.info("Guest deleted successfully: {}", guestId);
    }
    
    @PostMapping("/events/{eventId}/guests/{guestId}/token")
    @Operation(summary = "Создать токен приглашения", description = "Генерирует уникальный токен для приглашения гостя. Токен действителен 30 дней")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Токен успешно создан"),
        @ApiResponse(responseCode = "404", description = "Гость не найден")
    })
    public GuestToken token(
            @Parameter(description = "ID события", required = true) @PathVariable Long eventId, 
            @Parameter(description = "ID гостя", required = true) @PathVariable Long guestId) {
        log.info("Creating invitation token for guest ID: {} in event ID: {}", guestId, eventId);
        GuestToken t = new GuestToken(); 
        t.setGuestId(guestId); 
        t.setToken(tokenService.newToken()); 
        t.setValidUntil(LocalDateTime.now().plusDays(30)); 
        GuestToken saved = tokens.save(t);
        log.info("Token created successfully for guest ID: {}, valid until: {}", guestId, saved.getValidUntil());
        return saved;
    }
    
    @GetMapping("/invite/{token}")
    @Operation(summary = "Открыть приглашение по токену", description = "Возвращает информацию о госте по токену приглашения")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Приглашение найдено"),
        @ApiResponse(responseCode = "404", description = "Токен не найден или истек")
    })
    public EventGuest openInvite(@Parameter(description = "Токен приглашения", required = true) @PathVariable String token) {
        log.debug("Opening invite with token: {}", token);
        GuestToken t = tokens.findByToken(token).orElseThrow(() -> {
            log.warn("Token not found: {}", token);
            return new RuntimeException("Token not found");
        });
        EventGuest guest = guests.findById(t.getGuestId()).orElseThrow(() -> {
            log.warn("Guest not found for token: {}", token);
            return new RuntimeException("Guest not found");
        });
        // Загружаем связанного гостя из таблицы guests
        if (guest.getGuest() == null && guest.getGuestId() != null) {
            Guest guestEntity = guestRepository.findById(guest.getGuestId()).orElse(null);
            guest.setGuest(guestEntity);
        }
        log.info("Invite opened successfully for guest: {}", guest.getGuestName());
        return guest;
    }
    
    @PostMapping("/invite/{token}/rsvp")
    @Operation(summary = "Ответить на приглашение (RSVP)", description = "Позволяет гостю ответить на приглашение: accepted, declined или open")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ответ на приглашение успешно сохранен"),
        @ApiResponse(responseCode = "404", description = "Токен не найден"),
        @ApiResponse(responseCode = "400", description = "Некорректный статус RSVP")
    })
    public EventGuest rsvp(
            @Parameter(description = "Токен приглашения", required = true) @PathVariable String token, 
            @Parameter(description = "Статус ответа: accepted, declined или open", required = true) @RequestParam String status) {
        log.info("Processing RSVP with token: {}, status: {}", token, status);
        GuestToken t = tokens.findByToken(token).orElseThrow(() -> {
            log.warn("Token not found for RSVP: {}", token);
            return new RuntimeException("Token not found");
        });
        EventGuest g = guests.findById(t.getGuestId()).orElseThrow(() -> {
            log.warn("Guest not found for RSVP token: {}", token);
            return new RuntimeException("Guest not found");
        });
        // Загружаем связанного гостя из таблицы guests
        if (g.getGuest() == null && g.getGuestId() != null) {
            Guest guestEntity = guestRepository.findById(g.getGuestId()).orElse(null);
            g.setGuest(guestEntity);
        }
        g.setRsvpStatus(status); 
        EventGuest updated = guests.save(g);
        log.info("RSVP updated successfully: guest={}, status={}", updated.getGuestName(), updated.getRsvpStatus());
        return updated;
    }
}

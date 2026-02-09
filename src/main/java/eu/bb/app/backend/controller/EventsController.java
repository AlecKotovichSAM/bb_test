package eu.bb.app.backend.controller;

import eu.bb.app.backend.dto.CreateEventRequest;
import eu.bb.app.backend.dto.EventResponse;
import eu.bb.app.backend.entity.Event;
import eu.bb.app.backend.entity.EventGuest;
import eu.bb.app.backend.entity.GuestChild;
import eu.bb.app.backend.entity.Gift;
import eu.bb.app.backend.repository.EventRepository;
import eu.bb.app.backend.repository.EventGuestRepository;
import eu.bb.app.backend.repository.GuestChildRepository;
import eu.bb.app.backend.repository.ChatMessageRepository;
import eu.bb.app.backend.repository.GiftRepository;
import eu.bb.app.backend.repository.GuestTokenRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Tag(name = "Events", description = "API для управления событиями (днями рождения)")
public class EventsController {
    private static final Logger log = LoggerFactory.getLogger(EventsController.class);
    private final EventRepository repo;
    private final EventGuestRepository guestRepository;
    private final GuestChildRepository childRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final GiftRepository giftRepository;
    private final GuestTokenRepository guestTokenRepository;
    
    public EventsController(EventRepository repo, EventGuestRepository guestRepository, GuestChildRepository childRepository,
                          ChatMessageRepository chatMessageRepository, GiftRepository giftRepository, GuestTokenRepository guestTokenRepository) {
        this.repo = repo;
        this.guestRepository = guestRepository;
        this.childRepository = childRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.giftRepository = giftRepository;
        this.guestTokenRepository = guestTokenRepository;
    }
    
    @PostMapping("/events")
    @Operation(
        summary = "Создать событие", 
        description = "Создает новое событие (день рождения) для ребенка. " +
                     "Опционально можно указать гостей и их детей. " +
                     "Можно создавать новых гостей (указать guestName) или переиспользовать гостей из предыдущих событий (указать guestId). " +
                     "При переиспользовании гостя (guestId) автоматически копируются его данные (имя, userId) и дети, если они не указаны явно. " +
                     "Если статус не указан, автоматически устанавливается 'Draft'."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Событие успешно создано. Возвращает событие со списком созданных гостей и их детей"),
        @ApiResponse(responseCode = "400", description = "Некорректные данные события (например, не указано имя нового гостя или не найден гость для переиспользования)")
    })
    @Transactional
    public EventResponse create(@RequestBody CreateEventRequest request) {
        log.info("Creating new event for child ID: {}, host ID: {}", request.getChildId(), request.getHostId());
        
        // Создаем событие
        Event event = new Event();
        if (request.getHostId() != null && request.getHostId() == 0) {
            event.setHostId(null);
        } else {
            event.setHostId(request.getHostId());
        }
        event.setChildId(request.getChildId());
        event.setDatetime(request.getDatetime());
        event.setLocationType(request.getLocationType());
        event.setLocation(request.getLocation());
        // Устанавливаем статус 'Draft' по умолчанию, если не указан
        event.setStatus(request.getStatus() != null && !request.getStatus().isEmpty() 
                ? request.getStatus() 
                : "Draft");
        event.setComment(request.getComment());
        
        Event savedEvent = repo.save(event);
        log.info("Event created successfully with ID: {}, status: {}", savedEvent.getId(), savedEvent.getStatus());
        
        // Создаем гостей и их детей, если они указаны
        List<EventResponse.GuestWithChildrenResponse> guestsResponse = new ArrayList<>();
        
        if (request.getGuests() != null && !request.getGuests().isEmpty()) {
            for (CreateEventRequest.GuestWithChildren guestRequest : request.getGuests()) {
                EventGuest guest = new EventGuest();
                guest.setEventId(savedEvent.getId());
                guest.setRsvpStatus("open");
                
                // Если указан guestId - переиспользуем гостя из предыдущего события
                if (guestRequest.getGuestId() != null) {
                    EventGuest existingGuest = guestRepository.findById(guestRequest.getGuestId())
                            .orElseThrow(() -> {
                                log.warn("Guest not found with ID: {}", guestRequest.getGuestId());
                                return new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                                        "Guest not found with ID: " + guestRequest.getGuestId());
                            });
                    
                    // Копируем данные из существующего гостя
                    // guestName может быть переопределен в запросе, иначе используем существующее имя
                    guest.setGuestName(guestRequest.getGuestName() != null && !guestRequest.getGuestName().isEmpty()
                            ? guestRequest.getGuestName()
                            : existingGuest.getGuestName());
                    guest.setUserId(existingGuest.getUserId());
                    
                    log.debug("Reusing guest from previous event: original ID={}, name={}", 
                            existingGuest.getId(), guest.getGuestName());
                } else {
                    // Создаем нового гостя
                    if (guestRequest.getGuestName() == null || guestRequest.getGuestName().isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                                "Guest name is required when creating new guest");
                    }
                    guest.setGuestName(guestRequest.getGuestName());
                    guest.setUserId(guestRequest.getUserId()); // может быть null для незарегистрированных
                    log.debug("Creating new guest: name={}", guest.getGuestName());
                }
                
                EventGuest savedGuest = guestRepository.save(guest);
                log.debug("Guest saved: ID={}, name={}, userId={}", 
                        savedGuest.getId(), savedGuest.getGuestName(), savedGuest.getUserId());
                
                // Создаем детей гостя
                List<GuestChild> children = new ArrayList<>();
                
                // Если переиспользуем гостя и не указаны дети в запросе - копируем детей из предыдущего события
                if (guestRequest.getGuestId() != null && 
                    (guestRequest.getChildren() == null || guestRequest.getChildren().isEmpty())) {
                    List<GuestChild> existingChildren = childRepository.findByGuestId(guestRequest.getGuestId());
                    for (GuestChild existingChild : existingChildren) {
                        GuestChild child = new GuestChild();
                        child.setGuestId(savedGuest.getId());
                        child.setFirstName(existingChild.getFirstName());
                        GuestChild savedChild = childRepository.save(child);
                        children.add(savedChild);
                        log.debug("Copied guest child: name={}", savedChild.getFirstName());
                    }
                } else if (guestRequest.getChildren() != null && !guestRequest.getChildren().isEmpty()) {
                    // Создаем новых детей или используем указанных в запросе
                    for (String childName : guestRequest.getChildren()) {
                        GuestChild child = new GuestChild();
                        child.setGuestId(savedGuest.getId());
                        child.setFirstName(childName);
                        GuestChild savedChild = childRepository.save(child);
                        children.add(savedChild);
                        log.debug("Guest child created: ID={}, name={}", savedChild.getId(), savedChild.getFirstName());
                    }
                }
                
                guestsResponse.add(new EventResponse.GuestWithChildrenResponse(savedGuest, children));
            }
            log.info("Created {} guests with their children for event ID: {}", guestsResponse.size(), savedEvent.getId());
        }
        
        return new EventResponse(savedEvent, guestsResponse);
    }
    
    @GetMapping("/events")
    @Operation(summary = "Получить все события", description = "Возвращает список всех событий в системе с гостями и их детьми")
    @ApiResponse(responseCode = "200", description = "Список событий успешно получен")
    public List<EventResponse> all() {
        log.debug("Getting all events");
        List<Event> events = repo.findAll();
        log.info("Retrieved {} events", events.size());
        return events.stream()
                .map(this::buildEventResponse)
                .collect(Collectors.toList());
    }
    
    @GetMapping("/users/{userId}/events")
    @Operation(summary = "Получить события пользователя", description = "Возвращает все события, где указанный пользователь является хозяином, с гостями и их детьми")
    @ApiResponse(responseCode = "200", description = "Список событий пользователя успешно получен")
    public List<EventResponse> byUser(@Parameter(description = "ID пользователя (хозяина)", required = true) @PathVariable Long userId) {
        log.debug("Getting events for user ID: {}", userId);
        List<Event> events = repo.findByHostId(userId);
        log.info("Retrieved {} events for user ID: {}", events.size(), userId);
        return events.stream()
                .map(this::buildEventResponse)
                .collect(Collectors.toList());
    }
    
    @GetMapping("/users/{userId}/guests")
    @Operation(
        summary = "Получить всех гостей из всех событий пользователя", 
        description = "Возвращает всех гостей со всеми их детьми из всех событий, где указанный пользователь является хозяином. " +
                     "Полезно для получения полного списка всех гостей пользователя из всех его событий в одном запросе."
    )
    @ApiResponse(responseCode = "200", description = "Список гостей успешно получен. Каждый элемент содержит гостя и список его детей")
    public List<EventResponse.GuestWithChildrenResponse> getAllGuestsFromUserEvents(
            @Parameter(description = "ID пользователя (хозяина)", required = true) @PathVariable Long userId) {
        log.debug("Getting all guests from events for user ID: {}", userId);
        
        // Получаем все события пользователя
        List<Event> events = repo.findByHostId(userId);
        log.debug("Found {} events for user ID: {}", events.size(), userId);
        
        // Собираем всех гостей из всех событий с их детьми
        List<EventResponse.GuestWithChildrenResponse> allGuests = new ArrayList<>();
        for (Event event : events) {
            List<EventGuest> guests = guestRepository.findByEventId(event.getId());
            for (EventGuest guest : guests) {
                List<GuestChild> children = childRepository.findByGuestId(guest.getId());
                allGuests.add(new EventResponse.GuestWithChildrenResponse(guest, children));
            }
        }
        
        log.info("Retrieved {} guests from {} events for user ID: {}", allGuests.size(), events.size(), userId);
        return allGuests;
    }
    
    @GetMapping("/events/{id}")
    @Operation(summary = "Получить событие по ID", description = "Возвращает информацию о событии по его идентификатору с гостями и их детьми")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Событие найдено"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено")
    })
    public EventResponse get(@Parameter(description = "ID события", required = true) @PathVariable Long id) {
        log.debug("Getting event with ID: {}", id);
        Event event = repo.findById(id).orElseThrow(() -> {
            log.warn("Event not found with ID: {}", id);
            return new RuntimeException("Event not found");
        });
        log.debug("Event found: ID={}, status={}", event.getId(), event.getStatus());
        return buildEventResponse(event);
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
    @Operation(summary = "Удалить событие", description = "Удаляет событие из системы вместе со всеми связанными данными (гости, дети гостей, токены, подарки, сообщения чата)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Событие успешно удалено"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено")
    })
    @Transactional
    public void delete(@Parameter(description = "ID события", required = true) @PathVariable Long id) {
        log.info("Deleting event with ID: {}", id);
        
        // Проверяем, что событие существует
        if (!repo.existsById(id)) {
            log.warn("Event not found for deletion: {}", id);
            throw new RuntimeException("Event not found");
        }
        
        // Удаляем связанные данные в правильном порядке (зависимые таблицы сначала)
        List<EventGuest> guests = guestRepository.findByEventId(id);
        List<Long> guestIds = guests.stream().map(EventGuest::getId).collect(Collectors.toList());
        
        // Удаляем детей гостей и токены гостей
        for (EventGuest guest : guests) {
            childRepository.deleteAll(childRepository.findByGuestId(guest.getId()));
            guestTokenRepository.deleteAll(guestTokenRepository.findByGuestId(guest.getId()));
        }
        
        // Обнуляем ссылки на гостей в подарках перед удалением гостей
        List<Gift> gifts = giftRepository.findByEventId(id);
        for (Gift gift : gifts) {
            if (gift.getReservedByGuest() != null && guestIds.contains(gift.getReservedByGuest())) {
                gift.setReservedByGuest(null);
                giftRepository.save(gift);
            }
        }
        // Принудительно сохраняем изменения перед удалением гостей
        giftRepository.flush();
        
        // Удаляем сообщения чата
        chatMessageRepository.deleteAll(chatMessageRepository.findByEventIdOrderByCreatedAtAsc(id));
        
        // Удаляем подарки
        giftRepository.deleteAll(gifts);
        
        // Удаляем гостей (после удаления подарков, чтобы не было ссылок)
        guestRepository.deleteAll(guests);
        
        // Удаляем само событие
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
    
    /**
     * Вспомогательный метод для построения EventResponse из Event с загрузкой гостей и их детей
     */
    private EventResponse buildEventResponse(Event event) {
        List<EventGuest> guests = guestRepository.findByEventId(event.getId());
        List<EventResponse.GuestWithChildrenResponse> guestsResponse = guests.stream()
                .map(guest -> {
                    List<GuestChild> children = childRepository.findByGuestId(guest.getId());
                    return new EventResponse.GuestWithChildrenResponse(guest, children);
                })
                .collect(Collectors.toList());
        return new EventResponse(event, guestsResponse);
    }
}

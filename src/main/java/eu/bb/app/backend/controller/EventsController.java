package eu.bb.app.backend.controller;

import eu.bb.app.backend.dto.CreateEventRequest;
import eu.bb.app.backend.dto.EventResponse;
import eu.bb.app.backend.entity.Event;
import eu.bb.app.backend.entity.EventGuest;
import eu.bb.app.backend.entity.GuestChild;
import eu.bb.app.backend.entity.Gift;
import eu.bb.app.backend.repository.EventRepository;
import eu.bb.app.backend.repository.EventGuestRepository;
import eu.bb.app.backend.repository.GuestRepository;
import eu.bb.app.backend.repository.GuestChildRepository;
import eu.bb.app.backend.repository.ChatMessageRepository;
import eu.bb.app.backend.repository.GiftRepository;
import eu.bb.app.backend.repository.GuestTokenRepository;
import eu.bb.app.backend.entity.Guest;
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
    private final EventGuestRepository eventGuestRepository;
    private final GuestRepository guestRepository;
    private final GuestChildRepository childRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final GiftRepository giftRepository;
    private final GuestTokenRepository guestTokenRepository;
    
    public EventsController(EventRepository repo, EventGuestRepository eventGuestRepository, GuestRepository guestRepository, 
                          GuestChildRepository childRepository, ChatMessageRepository chatMessageRepository, 
                          GiftRepository giftRepository, GuestTokenRepository guestTokenRepository) {
        this.repo = repo;
        this.eventGuestRepository = eventGuestRepository;
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
                Guest guestEntity;
                Long existingEventGuestId = null; // ID существующего EventGuest для копирования детей
                
                // Обрабатываем guestId == 0 так же, как hostId == 0 (устанавливаем в null)
                final Long guestIdToReuse = (guestRequest.getGuestId() != null && guestRequest.getGuestId() == 0) 
                        ? null 
                        : guestRequest.getGuestId();
                
                if (guestIdToReuse != null) {
                    // Переиспользуем существующего гостя из таблицы guests
                    // guestIdToReuse - это ID из таблицы guests
                    guestEntity = guestRepository.findById(guestIdToReuse)
                            .orElseThrow(() -> {
                                log.warn("Guest not found with ID: {}", guestIdToReuse);
                                return new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                                        "Guest not found with ID: " + guestIdToReuse);
                            });
                    
                    // Если указано новое имя в запросе - обновляем гостя
                    if (guestRequest.getGuestName() != null && !guestRequest.getGuestName().isEmpty() 
                            && !guestRequest.getGuestName().equals(guestEntity.getGuestName())) {
                        guestEntity.setGuestName(guestRequest.getGuestName());
                        guestEntity = guestRepository.save(guestEntity);
                    }
                    
                    // Находим существующий EventGuest для копирования детей
                    // Ищем первый EventGuest с этим guestId из событий того же пользователя (hostId)
                    // Это гарантирует, что мы найдем правильный EventGuest с детьми, а не старый из seed данных
                    log.info("Looking for existing EventGuest with guestId={} from events of hostId={} to copy children", 
                            guestIdToReuse, savedEvent.getHostId());
                    Optional<EventGuest> existingEventGuest = savedEvent.getHostId() != null
                            ? eventGuestRepository.findFirstByGuestIdAndHostId(guestIdToReuse, savedEvent.getHostId())
                            : eventGuestRepository.findFirstByGuestId(guestIdToReuse);
                    
                    if (existingEventGuest.isPresent()) {
                        existingEventGuestId = existingEventGuest.get().getId();
                        log.info("Found existing EventGuest ID={} for guestId={} from hostId={}, will copy children", 
                                existingEventGuestId, guestIdToReuse, savedEvent.getHostId());
                    } else {
                        log.warn("No existing EventGuest found for guestId={} from hostId={}, children will not be copied. " +
                                "This may happen if this is the first time this guest is used by this host.", 
                                guestIdToReuse, savedEvent.getHostId());
                    }
                    
                    log.debug("Reusing guest from guests table: ID={}, name={}", 
                            guestEntity.getId(), guestEntity.getGuestName());
                } else {
                    // Создаем нового гостя или находим существующего по имени и userId
                    if (guestRequest.getGuestName() == null || guestRequest.getGuestName().isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                                "Guest name is required when creating new guest");
                    }
                    
                    // Ищем существующего гостя по имени и userId
                    guestEntity = guestRepository.findByGuestNameAndUserId(
                            guestRequest.getGuestName(), 
                            guestRequest.getUserId())
                            .orElse(null);
                    
                    if (guestEntity == null) {
                        // Создаем нового гостя в таблице guests
                        guestEntity = new Guest();
                        guestEntity.setGuestName(guestRequest.getGuestName());
                        guestEntity.setUserId(guestRequest.getUserId());
                        guestEntity = guestRepository.save(guestEntity);
                        log.debug("Created new guest in guests table: ID={}, name={}", 
                                guestEntity.getId(), guestEntity.getGuestName());
                    } else {
                        log.debug("Found existing guest in guests table: ID={}, name={}", 
                                guestEntity.getId(), guestEntity.getGuestName());
                    }
                }
                
                // Создаем EventGuest для текущего события
                EventGuest eventGuest = new EventGuest();
                eventGuest.setEventId(savedEvent.getId());
                eventGuest.setGuest(guestEntity); // Устанавливаем связь - Hibernate использует guest.getId() для колонки guest_id
                eventGuest.setRsvpStatus("open");
                EventGuest savedEventGuest = eventGuestRepository.save(eventGuest);
                log.debug("EventGuest created: ID={}, eventId={}, guestId={}", 
                        savedEventGuest.getId(), savedEventGuest.getEventId(), savedEventGuest.getGuestId());
                
                // Создаем детей гостя
                List<GuestChild> children = new ArrayList<>();
                
                // Если переиспользуем гостя и не указаны дети в запросе - копируем детей из предыдущего EventGuest
                if (existingEventGuestId != null && 
                    (guestRequest.getChildren() == null || guestRequest.getChildren().isEmpty())) {
                    log.info("Copying children from existing EventGuest ID={} to new EventGuest ID={}", 
                            existingEventGuestId, savedEventGuest.getId());
                    List<GuestChild> existingChildren = childRepository.findByGuestId(existingEventGuestId);
                    log.info("Found {} existing children for EventGuest ID={} to copy", 
                            existingChildren.size(), existingEventGuestId);
                    for (GuestChild existingChild : existingChildren) {
                        GuestChild child = new GuestChild();
                        child.setGuestId(savedEventGuest.getId()); // ссылается на EventGuest.id
                        child.setFirstName(existingChild.getFirstName());
                        GuestChild savedChild = childRepository.save(child);
                        children.add(savedChild);
                        log.info("Copied guest child: name={}, new child ID={}", savedChild.getFirstName(), savedChild.getId());
                    }
                    log.info("Total {} children copied for EventGuest ID={}", children.size(), savedEventGuest.getId());
                } else if (guestRequest.getChildren() != null && !guestRequest.getChildren().isEmpty()) {
                    // Создаем новых детей или используем указанных в запросе
                    if (existingEventGuestId != null) {
                        log.info("Children specified in request, skipping auto-copy. existingEventGuestId={}, children in request={}", 
                                existingEventGuestId, guestRequest.getChildren());
                    }
                    for (String childName : guestRequest.getChildren()) {
                        GuestChild child = new GuestChild();
                        child.setGuestId(savedEventGuest.getId()); // ссылается на EventGuest.id
                        child.setFirstName(childName);
                        GuestChild savedChild = childRepository.save(child);
                        children.add(savedChild);
                        log.debug("Guest child created: ID={}, name={}", savedChild.getId(), savedChild.getFirstName());
                    }
                }
                
                guestsResponse.add(new EventResponse.GuestWithChildrenResponse(savedEventGuest, children));
            }
            log.info("Created {} guests with their children for event ID: {}", guestsResponse.size(), savedEvent.getId());
        }
        
        // Используем buildEventResponse для гарантии правильной загрузки всех данных
        return buildEventResponse(savedEvent);
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
        
        // Собираем уникальных гостей из всех событий с их детьми
        // Используем Map для дедупликации по guestId из таблицы guests
        Map<Long, EventResponse.GuestWithChildrenResponse> uniqueGuestsMap = new HashMap<>();
        for (Event event : events) {
            List<EventGuest> eventGuests = eventGuestRepository.findByEventId(event.getId());
            for (EventGuest eventGuest : eventGuests) {
                Long guestId = eventGuest.getGuestId();
                
                // Если гость уже есть в Map, пропускаем (уже добавлен из другого события)
                if (uniqueGuestsMap.containsKey(guestId)) {
                    continue;
                }
                
                // Загружаем Guest из таблицы guests
                if (eventGuest.getGuest() == null && eventGuest.getGuestId() != null) {
                    Guest guest = guestRepository.findById(eventGuest.getGuestId())
                            .orElse(null);
                    eventGuest.setGuest(guest);
                }
                
                List<GuestChild> children = childRepository.findByGuestId(eventGuest.getId());
                uniqueGuestsMap.put(guestId, new EventResponse.GuestWithChildrenResponse(eventGuest, children));
            }
        }
        
        List<EventResponse.GuestWithChildrenResponse> allGuests = new ArrayList<>(uniqueGuestsMap.values());
        log.info("Retrieved {} unique guests from {} events for user ID: {}", allGuests.size(), events.size(), userId);
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
        List<EventGuest> eventGuests = eventGuestRepository.findByEventId(id);
        List<Long> eventGuestIds = eventGuests.stream().map(EventGuest::getId).collect(Collectors.toList());
        
        // Удаляем детей гостей и токены гостей
        for (EventGuest eventGuest : eventGuests) {
            childRepository.deleteAll(childRepository.findByGuestId(eventGuest.getId()));
            guestTokenRepository.deleteAll(guestTokenRepository.findByGuestId(eventGuest.getId()));
        }
        
        // Обнуляем ссылки на гостей в подарках перед удалением гостей
        List<Gift> gifts = giftRepository.findByEventId(id);
        for (Gift gift : gifts) {
            if (gift.getReservedByGuest() != null && eventGuestIds.contains(gift.getReservedByGuest())) {
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
        
        // Удаляем EventGuest записи (после удаления подарков, чтобы не было ссылок)
        // НЕ удаляем записи из таблицы guests - они могут использоваться в других событиях
        eventGuestRepository.deleteAll(eventGuests);
        
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
        List<EventGuest> eventGuests = eventGuestRepository.findByEventId(event.getId());
        List<EventResponse.GuestWithChildrenResponse> guestsResponse = eventGuests.stream()
                .map(eventGuest -> {
                    // Загружаем Guest из таблицы guests
                    if (eventGuest.getGuest() == null && eventGuest.getGuestId() != null) {
                        Guest guest = guestRepository.findById(eventGuest.getGuestId())
                                .orElse(null);
                        eventGuest.setGuest(guest);
                    }
                    List<GuestChild> children = childRepository.findByGuestId(eventGuest.getId());
                    return new EventResponse.GuestWithChildrenResponse(eventGuest, children);
                })
                .collect(Collectors.toList());
        return new EventResponse(event, guestsResponse);
    }
}

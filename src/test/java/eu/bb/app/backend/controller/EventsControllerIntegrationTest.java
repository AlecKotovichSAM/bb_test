package eu.bb.app.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.bb.app.backend.entity.Event;
import eu.bb.app.backend.entity.User;
import eu.bb.app.backend.entity.Child;
import eu.bb.app.backend.entity.EventGuest;
import eu.bb.app.backend.entity.GuestChild;
import eu.bb.app.backend.entity.ChatMessage;
import eu.bb.app.backend.entity.Gift;
import eu.bb.app.backend.entity.GuestToken;
import eu.bb.app.backend.repository.ChatMessageRepository;
import eu.bb.app.backend.repository.EventGuestRepository;
import eu.bb.app.backend.repository.EventRepository;
import eu.bb.app.backend.repository.GiftRepository;
import eu.bb.app.backend.repository.GuestTokenRepository;
import eu.bb.app.backend.repository.GuestChildRepository;
import eu.bb.app.backend.repository.UserRepository;
import eu.bb.app.backend.repository.ChildRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EventsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChildRepository childRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private GiftRepository giftRepository;

    @Autowired
    private GuestTokenRepository guestTokenRepository;

    @Autowired
    private EventGuestRepository eventGuestRepository;

    @Autowired
    private GuestChildRepository guestChildRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private User testUser;
    private Child testChild;

    @BeforeEach
    void setUp() {
        // Delete in order: dependent tables first, then parent tables
        chatMessageRepository.deleteAll();
        giftRepository.deleteAll();
        guestTokenRepository.deleteAll();
        guestChildRepository.deleteAll();
        eventGuestRepository.deleteAll();
        eventRepository.deleteAll();
        childRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user and child
        testUser = new User();
        testUser.setFirstName("Host");
        testUser.setEmail("host@example.com");
        testUser = userRepository.save(testUser);

        testChild = new Child();
        testChild.setUserId(testUser.getId());
        testChild.setFirstName("Child");
        testChild = childRepository.save(testChild);
    }

    @Test
    void testCreateEvent_WithIdZero_ShouldFixAndCreate() throws Exception {
        // Given - event with id=0
        Event event = new Event();
        event.setId(0L); // Should be fixed to null
        event.setHostId(testUser.getId());
        event.setChildId(testChild.getId());
        event.setDatetime(LocalDateTime.now().plusDays(30));
        event.setLocationType("manual");
        event.setLocation("Test Location");
        event.setStatus("Planned");

        // When & Then - новый формат ответа EventResponse
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.event.id").exists())
                .andExpect(jsonPath("$.event.id").isNumber())
                .andExpect(jsonPath("$.event.status").value("Planned"))
                .andExpect(jsonPath("$.event.location").value("Test Location"))
                .andExpect(jsonPath("$.guests").isArray());

        // Verify in database
        assertThat(eventRepository.findAll()).hasSize(1);
        Event saved = eventRepository.findAll().get(0);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isNotEqualTo(0L);
    }

    @Test
    void testCreateEvent_WithoutId() throws Exception {
        // Given
        Event event = new Event();
        event.setHostId(testUser.getId());
        event.setChildId(testChild.getId());
        event.setDatetime(LocalDateTime.now().plusDays(30));
        event.setStatus("Draft");

        // When & Then - новый формат ответа EventResponse
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.event.id").exists())
                .andExpect(jsonPath("$.event.status").value("Draft"))
                .andExpect(jsonPath("$.guests").isArray());
    }

    @Test
    void testGetAllEvents() throws Exception {
        // Given
        Event event1 = createTestEvent("Event1", "Planned");
        Event event2 = createTestEvent("Event2", "Draft");
        eventRepository.save(event1);
        eventRepository.save(event2);

        // When & Then - новый формат ответа List<EventResponse>
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].event.id").exists())
                .andExpect(jsonPath("$[0].event.status").value("Planned"))
                .andExpect(jsonPath("$[0].guests").isArray())
                .andExpect(jsonPath("$[1].event.id").exists())
                .andExpect(jsonPath("$[1].event.status").value("Draft"))
                .andExpect(jsonPath("$[1].guests").isArray());
    }

    @Test
    void testGetEventsByUser() throws Exception {
        // Given
        Event event1 = createTestEvent("Event1", "Planned");
        eventRepository.save(event1);

        // When & Then - новый формат ответа List<EventResponse>
        mockMvc.perform(get("/api/users/{userId}/events", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].event.id").exists())
                .andExpect(jsonPath("$[0].event.hostId").value(testUser.getId()))
                .andExpect(jsonPath("$[0].event.status").value("Planned"))
                .andExpect(jsonPath("$[0].guests").isArray());
    }

    @Test
    void testGetAllGuestsFromUserEvents() throws Exception {
        // Given - создаем несколько событий с гостями и детьми
        Event event1 = createTestEvent("Event1", "Planned");
        Event savedEvent1 = eventRepository.save(event1);
        
        Event event2 = createTestEvent("Event2", "Draft");
        Event savedEvent2 = eventRepository.save(event2);
        
        // Гости для первого события
        EventGuest guest1 = new EventGuest();
        guest1.setEventId(savedEvent1.getId());
        guest1.setGuestName("Guest1");
        guest1.setRsvpStatus("open");
        EventGuest savedGuest1 = eventGuestRepository.save(guest1);
        
        GuestChild child1 = new GuestChild();
        child1.setGuestId(savedGuest1.getId());
        child1.setFirstName("Child1");
        guestChildRepository.save(child1);
        
        EventGuest guest2 = new EventGuest();
        guest2.setEventId(savedEvent1.getId());
        guest2.setGuestName("Guest2");
        guest2.setRsvpStatus("accepted");
        eventGuestRepository.save(guest2);
        
        // Гости для второго события
        EventGuest guest3 = new EventGuest();
        guest3.setEventId(savedEvent2.getId());
        guest3.setGuestName("Guest3");
        guest3.setRsvpStatus("open");
        EventGuest savedGuest3 = eventGuestRepository.save(guest3);
        
        GuestChild child3a = new GuestChild();
        child3a.setGuestId(savedGuest3.getId());
        child3a.setFirstName("Child3a");
        guestChildRepository.save(child3a);
        
        GuestChild child3b = new GuestChild();
        child3b.setGuestId(savedGuest3.getId());
        child3b.setFirstName("Child3b");
        guestChildRepository.save(child3b);

        // When & Then - получаем всех гостей из всех событий пользователя
        mockMvc.perform(get("/api/users/{userId}/guests", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3)) // 3 гостя из 2 событий
                .andExpect(jsonPath("$[0].guest.guestName").exists())
                .andExpect(jsonPath("$[0].children").isArray())
                .andExpect(jsonPath("$[1].guest.guestName").exists())
                .andExpect(jsonPath("$[1].children").isArray())
                .andExpect(jsonPath("$[2].guest.guestName").exists())
                .andExpect(jsonPath("$[2].children").isArray())
                .andExpect(jsonPath("$[2].children.length()").value(2)); // Guest3 имеет 2 детей
    }

    @Test
    void testCancelEvent() throws Exception {
        // Given
        Event event = createTestEvent("ToCancel", "Planned");
        Event saved = eventRepository.save(event);
        Long eventId = saved.getId();

        // When & Then
        mockMvc.perform(post("/api/events/{id}/cancel", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Cancelled"));

        // Verify in database
        Event cancelled = eventRepository.findById(eventId).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo("Cancelled");
    }

    @Test
    void testGetEventById() throws Exception {
        // Given - создаем событие с гостями и детьми
        Event event = createTestEvent("TestEvent", "Planned");
        Event savedEvent = eventRepository.save(event);
        
        EventGuest guest1 = new EventGuest();
        guest1.setEventId(savedEvent.getId());
        guest1.setGuestName("Sophie");
        guest1.setRsvpStatus("open");
        EventGuest savedGuest1 = eventGuestRepository.save(guest1);
        
        GuestChild child1 = new GuestChild();
        child1.setGuestId(savedGuest1.getId());
        child1.setFirstName("Max");
        guestChildRepository.save(child1);
        
        GuestChild child2 = new GuestChild();
        child2.setGuestId(savedGuest1.getId());
        child2.setFirstName("Emma");
        guestChildRepository.save(child2);

        // When & Then - новый формат ответа EventResponse
        String response = mockMvc.perform(get("/api/events/{id}", savedEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.event.id").value(savedEvent.getId()))
                .andExpect(jsonPath("$.event.status").value("Planned"))
                .andExpect(jsonPath("$.guests").isArray())
                .andExpect(jsonPath("$.guests.length()").value(1))
                .andExpect(jsonPath("$.guests[0].guest.guestName").value("Sophie"))
                .andExpect(jsonPath("$.guests[0].children").isArray())
                .andExpect(jsonPath("$.guests[0].children.length()").value(2))
                .andExpect(jsonPath("$.guests[0].children[0].firstName").exists())
                .andExpect(jsonPath("$.guests[0].children[1].firstName").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // Проверяем, что оба имени присутствуют
        assertThat(response).contains("Max").contains("Emma");
    }

    @Test
    void testCreateEvent_ReuseGuestFromPreviousEvent() throws Exception {
        // Given - создаем первое событие с гостем и детьми
        Event event1 = createTestEvent("Event1", "Planned");
        Event savedEvent1 = eventRepository.save(event1);
        
        EventGuest guest1 = new EventGuest();
        guest1.setEventId(savedEvent1.getId());
        guest1.setGuestName("Sophie");
        guest1.setRsvpStatus("open");
        EventGuest savedGuest1 = eventGuestRepository.save(guest1);
        
        GuestChild child1 = new GuestChild();
        child1.setGuestId(savedGuest1.getId());
        child1.setFirstName("Max");
        guestChildRepository.save(child1);
        
        GuestChild child2 = new GuestChild();
        child2.setGuestId(savedGuest1.getId());
        child2.setFirstName("Emma");
        guestChildRepository.save(child2);
        
        // When - создаем второе событие, переиспользуя гостя из первого события
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("hostId", testUser.getId());
        request.put("childId", testChild.getId());
        request.put("datetime", LocalDateTime.now().plusDays(60).toString());
        request.put("locationType", "manual");
        request.put("location", "New Location");
        request.put("status", "Planned");
        
        List<Map<String, Object>> guests = new java.util.ArrayList<>();
        Map<String, Object> reusedGuest = new java.util.HashMap<>();
        reusedGuest.put("guestId", savedGuest1.getId()); // переиспользуем гостя
        // children не указываем - должны скопироваться автоматически
        guests.add(reusedGuest);
        
        Map<String, Object> newGuest = new java.util.HashMap<>();
        newGuest.put("guestName", "Ben");
        newGuest.put("children", java.util.Arrays.asList("Tom"));
        guests.add(newGuest);
        
        request.put("guests", guests);
        
        // Then - проверяем, что гость переиспользован с детьми
        String response = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.event.id").exists())
                .andExpect(jsonPath("$.guests").isArray())
                .andExpect(jsonPath("$.guests.length()").value(2))
                .andExpect(jsonPath("$.guests[0].guest.guestName").value("Sophie"))
                .andExpect(jsonPath("$.guests[0].children.length()").value(2))
                .andExpect(jsonPath("$.guests[0].children[0].firstName").exists())
                .andExpect(jsonPath("$.guests[0].children[1].firstName").exists())
                .andExpect(jsonPath("$.guests[1].guest.guestName").value("Ben"))
                .andExpect(jsonPath("$.guests[1].children.length()").value(1))
                .andExpect(jsonPath("$.guests[1].children[0].firstName").value("Tom"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // Проверяем содержимое ответа через AssertJ для проверки имен детей
        assertThat(response).contains("Max").contains("Emma");
        
        // Verify - проверяем, что созданы новые записи для нового события
        List<Event> allEvents = eventRepository.findAll();
        assertThat(allEvents).hasSize(2);
        
        Event event2 = allEvents.stream()
                .filter(e -> !e.getId().equals(savedEvent1.getId()))
                .findFirst().orElseThrow();
        
        List<EventGuest> guestsEvent2 = eventGuestRepository.findByEventId(event2.getId());
        assertThat(guestsEvent2).hasSize(2);
        
        // Проверяем, что у переиспользованного гостя скопировались дети
        EventGuest reusedGuestEntity = guestsEvent2.stream()
                .filter(g -> "Sophie".equals(g.getGuestName()))
                .findFirst().orElseThrow();
        List<GuestChild> reusedChildren = guestChildRepository.findByGuestId(reusedGuestEntity.getId());
        assertThat(reusedChildren).hasSize(2);
        assertThat(reusedChildren.stream().map(GuestChild::getFirstName))
                .containsExactlyInAnyOrder("Max", "Emma");
    }

    @Test
    void testCreateEvent_ReuseGuestWithOverrideName() throws Exception {
        // Given - создаем первое событие с гостем
        Event event1 = createTestEvent("Event1", "Planned");
        Event savedEvent1 = eventRepository.save(event1);
        
        EventGuest guest1 = new EventGuest();
        guest1.setEventId(savedEvent1.getId());
        guest1.setGuestName("Sophie");
        guest1.setRsvpStatus("open");
        EventGuest savedGuest1 = eventGuestRepository.save(guest1);
        
        // When - переиспользуем гостя, но переопределяем имя
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("hostId", testUser.getId());
        request.put("childId", testChild.getId());
        request.put("datetime", LocalDateTime.now().plusDays(60).toString());
        request.put("status", "Planned");
        
        List<Map<String, Object>> guests = new java.util.ArrayList<>();
        Map<String, Object> reusedGuest = new java.util.HashMap<>();
        reusedGuest.put("guestId", savedGuest1.getId());
        reusedGuest.put("guestName", "Sophie Updated"); // переопределяем имя
        guests.add(reusedGuest);
        
        request.put("guests", guests);
        
        // Then - проверяем, что имя переопределено
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guests[0].guest.guestName").value("Sophie Updated"));
    }

    @Test
    void testCreateEvent_ReuseGuestWithOverrideChildren() throws Exception {
        // Given - создаем первое событие с гостем и детьми
        Event event1 = createTestEvent("Event1", "Planned");
        Event savedEvent1 = eventRepository.save(event1);
        
        EventGuest guest1 = new EventGuest();
        guest1.setEventId(savedEvent1.getId());
        guest1.setGuestName("Sophie");
        guest1.setRsvpStatus("open");
        EventGuest savedGuest1 = eventGuestRepository.save(guest1);
        
        GuestChild child1 = new GuestChild();
        child1.setGuestId(savedGuest1.getId());
        child1.setFirstName("Max");
        guestChildRepository.save(child1);
        
        // When - переиспользуем гостя, но переопределяем детей
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("hostId", testUser.getId());
        request.put("childId", testChild.getId());
        request.put("datetime", LocalDateTime.now().plusDays(60).toString());
        request.put("status", "Planned");
        
        List<Map<String, Object>> guests = new java.util.ArrayList<>();
        Map<String, Object> reusedGuest = new java.util.HashMap<>();
        reusedGuest.put("guestId", savedGuest1.getId());
        reusedGuest.put("children", java.util.Arrays.asList("NewChild1", "NewChild2")); // переопределяем детей
        guests.add(reusedGuest);
        
        request.put("guests", guests);
        
        // Then - проверяем, что дети переопределены
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guests[0].guest.guestName").value("Sophie"))
                .andExpect(jsonPath("$.guests[0].children.length()").value(2))
                .andExpect(jsonPath("$.guests[0].children[0].firstName").value("NewChild1"))
                .andExpect(jsonPath("$.guests[0].children[1].firstName").value("NewChild2"));
    }

    @Test
    void testCreateEvent_ReuseGuestWithUserId() throws Exception {
        // Given - создаем пользователя и первое событие с зарегистрированным гостем
        User registeredUser = new User();
        registeredUser.setFirstName("Registered");
        registeredUser.setEmail("registered@example.com");
        User savedRegisteredUser = userRepository.save(registeredUser);
        
        Event event1 = createTestEvent("Event1", "Planned");
        Event savedEvent1 = eventRepository.save(event1);
        
        EventGuest guest1 = new EventGuest();
        guest1.setEventId(savedEvent1.getId());
        guest1.setGuestName("Sophie");
        guest1.setUserId(savedRegisteredUser.getId());
        guest1.setRsvpStatus("open");
        EventGuest savedGuest1 = eventGuestRepository.save(guest1);
        
        // When - переиспользуем гостя с userId
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("hostId", testUser.getId());
        request.put("childId", testChild.getId());
        request.put("datetime", LocalDateTime.now().plusDays(60).toString());
        request.put("status", "Planned");
        
        List<Map<String, Object>> guests = new java.util.ArrayList<>();
        Map<String, Object> reusedGuest = new java.util.HashMap<>();
        reusedGuest.put("guestId", savedGuest1.getId());
        guests.add(reusedGuest);
        
        request.put("guests", guests);
        
        // Then - проверяем, что userId скопирован
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guests[0].guest.guestName").value("Sophie"))
                .andExpect(jsonPath("$.guests[0].guest.userId").value(savedRegisteredUser.getId()));
    }

    @Test
    void testCreateEvent_ReuseNonExistentGuest_ShouldFail() throws Exception {
        // Given - несуществующий ID гостя
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("hostId", testUser.getId());
        request.put("childId", testChild.getId());
        request.put("datetime", LocalDateTime.now().plusDays(60).toString());
        request.put("status", "Planned");
        
        List<Map<String, Object>> guests = new java.util.ArrayList<>();
        Map<String, Object> reusedGuest = new java.util.HashMap<>();
        reusedGuest.put("guestId", 99999L); // несуществующий ID
        guests.add(reusedGuest);
        
        request.put("guests", guests);
        
        // When & Then - должна быть ошибка 400
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // ResponseStatusException с HttpStatus.BAD_REQUEST
    }

    @Test
    void testCreateEvent_NewGuestWithoutName_ShouldFail() throws Exception {
        // Given - новый гость без имени
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("hostId", testUser.getId());
        request.put("childId", testChild.getId());
        request.put("datetime", LocalDateTime.now().plusDays(60).toString());
        request.put("status", "Planned");
        
        List<Map<String, Object>> guests = new java.util.ArrayList<>();
        Map<String, Object> newGuest = new java.util.HashMap<>();
        // guestName не указан
        newGuest.put("children", java.util.Arrays.asList("Tom"));
        guests.add(newGuest);
        
        request.put("guests", guests);
        
        // When & Then - должна быть ошибка 400
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // ResponseStatusException с HttpStatus.BAD_REQUEST
    }

    @Test
    void testDeleteEvent() throws Exception {
        // Given
        Event event = createTestEvent("ToDelete", "Planned");
        Event saved = eventRepository.save(event);
        Long eventId = saved.getId();

        // When & Then
        mockMvc.perform(delete("/api/events/{id}", eventId))
                .andExpect(status().isOk());

        // Verify deleted
        assertThat(eventRepository.findById(eventId)).isEmpty();
    }

    @Test
    void testDeleteEvent_WithAllRelatedData_ShouldCascadeDelete() throws Exception {
        // Given - создаем событие со всеми связанными данными
        Event event = createTestEvent("EventToDelete", "Planned");
        Event savedEvent = eventRepository.save(event);
        Long eventId = savedEvent.getId();

        // Создаем гостей
        EventGuest guest1 = new EventGuest();
        guest1.setEventId(eventId);
        guest1.setGuestName("Guest1");
        guest1.setRsvpStatus("open");
        EventGuest savedGuest1 = eventGuestRepository.save(guest1);

        EventGuest guest2 = new EventGuest();
        guest2.setEventId(eventId);
        guest2.setGuestName("Guest2");
        guest2.setRsvpStatus("accepted");
        EventGuest savedGuest2 = eventGuestRepository.save(guest2);

        // Создаем детей гостей
        GuestChild child1 = new GuestChild();
        child1.setGuestId(savedGuest1.getId());
        child1.setFirstName("Child1");
        guestChildRepository.save(child1);

        GuestChild child2 = new GuestChild();
        child2.setGuestId(savedGuest1.getId());
        child2.setFirstName("Child2");
        guestChildRepository.save(child2);

        // Создаем токены гостей
        GuestToken token1 = new GuestToken();
        token1.setGuestId(savedGuest1.getId());
        token1.setToken("token-1");
        token1.setValidUntil(LocalDateTime.now().plusDays(30));
        guestTokenRepository.save(token1);

        GuestToken token2 = new GuestToken();
        token2.setGuestId(savedGuest2.getId());
        token2.setToken("token-2");
        token2.setValidUntil(LocalDateTime.now().plusDays(30));
        guestTokenRepository.save(token2);

        // Создаем подарки
        Gift gift1 = new Gift();
        gift1.setEventId(eventId);
        gift1.setTitle("Gift1");
        gift1.setDescription("Description1");
        gift1.setPrice(new BigDecimal("29.99"));
        gift1.setStatus("open");
        giftRepository.save(gift1);

        Gift gift2 = new Gift();
        gift2.setEventId(eventId);
        gift2.setTitle("Gift2");
        gift2.setDescription("Description2");
        gift2.setPrice(new BigDecimal("49.99"));
        gift2.setStatus("reserved");
        gift2.setReservedByGuest(savedGuest2.getId());
        giftRepository.save(gift2);

        // Создаем сообщения чата
        ChatMessage message1 = new ChatMessage();
        message1.setEventId(eventId);
        message1.setUserId(testUser.getId());
        message1.setMessage("Message 1");
        message1.setCreatedAt(LocalDateTime.now());
        chatMessageRepository.save(message1);

        ChatMessage message2 = new ChatMessage();
        message2.setEventId(eventId);
        message2.setUserId(testUser.getId());
        message2.setMessage("Message 2");
        message2.setCreatedAt(LocalDateTime.now());
        chatMessageRepository.save(message2);

        // Проверяем, что все данные созданы
        assertThat(eventGuestRepository.findByEventId(eventId)).hasSize(2);
        assertThat(guestChildRepository.findByGuestId(savedGuest1.getId())).hasSize(2);
        assertThat(guestTokenRepository.findByGuestId(savedGuest1.getId())).hasSize(1);
        assertThat(guestTokenRepository.findByGuestId(savedGuest2.getId())).hasSize(1);
        assertThat(giftRepository.findByEventId(eventId)).hasSize(2);
        assertThat(chatMessageRepository.findByEventIdOrderByCreatedAtAsc(eventId)).hasSize(2);

        // When - удаляем событие
        mockMvc.perform(delete("/api/events/{id}", eventId))
                .andExpect(status().isOk());

        // Then - проверяем, что все связанные данные удалены
        assertThat(eventRepository.findById(eventId)).isEmpty();
        assertThat(eventGuestRepository.findByEventId(eventId)).isEmpty();
        assertThat(guestChildRepository.findByGuestId(savedGuest1.getId())).isEmpty();
        assertThat(guestTokenRepository.findByGuestId(savedGuest1.getId())).isEmpty();
        assertThat(guestTokenRepository.findByGuestId(savedGuest2.getId())).isEmpty();
        assertThat(giftRepository.findByEventId(eventId)).isEmpty();
        assertThat(chatMessageRepository.findByEventIdOrderByCreatedAtAsc(eventId)).isEmpty();
    }

    @Test
    void testCreateEvent_WithGuestsAndChildren() throws Exception {
        // Given - создаем событие с гостями и их детьми
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("hostId", testUser.getId());
        request.put("childId", testChild.getId());
        request.put("datetime", LocalDateTime.now().plusDays(30).toString());
        request.put("locationType", "manual");
        request.put("location", "Test Location");
        request.put("status", "Planned");
        
        List<Map<String, Object>> guests = new java.util.ArrayList<>();
        Map<String, Object> guest1 = new java.util.HashMap<>();
        guest1.put("guestName", "Sophie");
        guest1.put("children", java.util.Arrays.asList("Max", "Emma"));
        guests.add(guest1);
        
        Map<String, Object> guest2 = new java.util.HashMap<>();
        guest2.put("guestName", "Ben");
        guest2.put("children", java.util.Arrays.asList("Tom"));
        guests.add(guest2);
        
        Map<String, Object> guest3 = new java.util.HashMap<>();
        guest3.put("guestName", "Clara");
        // без детей
        guests.add(guest3);
        
        request.put("guests", guests);

        // When & Then
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.event.id").exists())
                .andExpect(jsonPath("$.event.status").value("Planned"))
                .andExpect(jsonPath("$.guests").isArray())
                .andExpect(jsonPath("$.guests.length()").value(3))
                .andExpect(jsonPath("$.guests[0].guest.guestName").value("Sophie"))
                .andExpect(jsonPath("$.guests[0].children").isArray())
                .andExpect(jsonPath("$.guests[0].children.length()").value(2))
                .andExpect(jsonPath("$.guests[0].children[0].firstName").value("Max"))
                .andExpect(jsonPath("$.guests[0].children[1].firstName").value("Emma"))
                .andExpect(jsonPath("$.guests[1].guest.guestName").value("Ben"))
                .andExpect(jsonPath("$.guests[1].children.length()").value(1))
                .andExpect(jsonPath("$.guests[1].children[0].firstName").value("Tom"))
                .andExpect(jsonPath("$.guests[2].guest.guestName").value("Clara"))
                .andExpect(jsonPath("$.guests[2].children").isEmpty());

        // Verify in database
        assertThat(eventRepository.findAll()).hasSize(1);
        Event saved = eventRepository.findAll().get(0);
        assertThat(saved.getId()).isNotNull();
        
        List<EventGuest> savedGuests = eventGuestRepository.findByEventId(saved.getId());
        assertThat(savedGuests).hasSize(3);
        
        EventGuest guestSophie = savedGuests.stream()
                .filter(g -> "Sophie".equals(g.getGuestName()))
                .findFirst().orElseThrow();
        List<GuestChild> sophieChildren = guestChildRepository.findByGuestId(guestSophie.getId());
        assertThat(sophieChildren).hasSize(2);
        assertThat(sophieChildren.stream().map(GuestChild::getFirstName))
                .containsExactlyInAnyOrder("Max", "Emma");
    }

    @Test
    void testCreateEvent_WithoutGuests() throws Exception {
        // Given - создаем событие без гостей (обратная совместимость)
        Event event = new Event();
        event.setHostId(testUser.getId());
        event.setChildId(testChild.getId());
        event.setDatetime(LocalDateTime.now().plusDays(30));
        event.setStatus("Draft");

        // When & Then
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.event.id").exists())
                .andExpect(jsonPath("$.event.status").value("Draft"))
                .andExpect(jsonPath("$.guests").isArray())
                .andExpect(jsonPath("$.guests.length()").value(0));
    }

    @Test
    void testCreateEvent_WithoutStatus_ShouldSetDraft() throws Exception {
        // Given - создаем событие без указания статуса
        Event event = new Event();
        event.setHostId(testUser.getId());
        event.setChildId(testChild.getId());
        event.setDatetime(LocalDateTime.now().plusDays(30));
        // статус не устанавливаем

        // When & Then - должен автоматически установиться статус 'Draft'
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.event.id").exists())
                .andExpect(jsonPath("$.event.status").value("Draft"))
                .andExpect(jsonPath("$.guests").isArray());

        // Verify in database
        Event saved = eventRepository.findAll().get(0);
        assertThat(saved.getStatus()).isEqualTo("Draft");
    }

    private Event createTestEvent(String comment, String status) {
        Event event = new Event();
        event.setHostId(testUser.getId());
        event.setChildId(testChild.getId());
        event.setDatetime(LocalDateTime.now().plusDays(30));
        event.setStatus(status);
        event.setComment(comment);
        return event;
    }
}

package eu.bb.app.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.bb.app.backend.entity.Event;
import eu.bb.app.backend.entity.User;
import eu.bb.app.backend.entity.Child;
import eu.bb.app.backend.repository.ChatMessageRepository;
import eu.bb.app.backend.repository.EventGuestRepository;
import eu.bb.app.backend.repository.EventRepository;
import eu.bb.app.backend.repository.GiftRepository;
import eu.bb.app.backend.repository.GuestTokenRepository;
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

import java.time.LocalDateTime;

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

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private User testUser;
    private Child testChild;

    @BeforeEach
    void setUp() {
        // Delete in order: dependent tables first, then parent tables
        chatMessageRepository.deleteAll();
        giftRepository.deleteAll();
        guestTokenRepository.deleteAll();
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

        // When & Then
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("Planned"))
                .andExpect(jsonPath("$.location").value("Test Location"));

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

        // When & Then
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("Draft"));
    }

    @Test
    void testGetAllEvents() throws Exception {
        // Given
        Event event1 = createTestEvent("Event1", "Planned");
        Event event2 = createTestEvent("Event2", "Draft");
        eventRepository.save(event1);
        eventRepository.save(event2);

        // When & Then
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testGetEventsByUser() throws Exception {
        // Given
        Event event1 = createTestEvent("Event1", "Planned");
        eventRepository.save(event1);

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/events", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].hostId").value(testUser.getId()));
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

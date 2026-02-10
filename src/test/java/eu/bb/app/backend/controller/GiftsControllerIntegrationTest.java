package eu.bb.app.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import eu.bb.app.backend.entity.Child;
import eu.bb.app.backend.entity.Event;
import eu.bb.app.backend.entity.EventGuest;
import eu.bb.app.backend.entity.Guest;
import eu.bb.app.backend.entity.Gift;
import eu.bb.app.backend.entity.GuestToken;
import eu.bb.app.backend.entity.User;
import eu.bb.app.backend.repository.ChatMessageRepository;
import eu.bb.app.backend.repository.ChildRepository;
import eu.bb.app.backend.repository.EventGuestRepository;
import eu.bb.app.backend.repository.EventRepository;
import eu.bb.app.backend.repository.GiftRepository;
import eu.bb.app.backend.repository.GiftCategoryRepository;
import eu.bb.app.backend.repository.GuestRepository;
import eu.bb.app.backend.repository.GuestTokenRepository;
import eu.bb.app.backend.repository.UserRepository;
import eu.bb.app.backend.entity.GiftCategory;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GiftsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GiftRepository giftRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChildRepository childRepository;

    @Autowired
    private EventGuestRepository guestRepository;

    @Autowired
    private GuestRepository guestEntityRepository;

    @Autowired
    private GuestTokenRepository tokenRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private GiftCategoryRepository categoryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private User testUser;
    private Child testChild;
    private Event testEvent;
    private EventGuest testGuest;
    private GuestToken testToken;

    @BeforeEach
    void setUp() {
        // Delete in order: dependent tables first, then parent tables
        chatMessageRepository.deleteAll();
        giftRepository.deleteAll();
        tokenRepository.deleteAll();
        guestRepository.deleteAll();
        guestEntityRepository.deleteAll(); // удаляем guests перед events
        eventRepository.deleteAll();
        childRepository.deleteAll();
        userRepository.deleteAll();
        // Категории не удаляем - они создаются через миграции и должны оставаться

        // Create test data
        testUser = new User();
        testUser.setFirstName("Host");
        testUser.setEmail("host@example.com");
        testUser = userRepository.save(testUser);

        testChild = new Child();
        testChild.setUserId(testUser.getId());
        testChild.setFirstName("Child");
        testChild = childRepository.save(testChild);

        testEvent = new Event();
        testEvent.setHostId(testUser.getId());
        testEvent.setChildId(testChild.getId());
        testEvent.setDatetime(LocalDateTime.now().plusDays(30));
        testEvent.setStatus("Planned");
        testEvent = eventRepository.save(testEvent);

        Guest guestEntity = new Guest();
        guestEntity.setGuestName("Test Guest");
        guestEntity = guestEntityRepository.save(guestEntity);
        
        testGuest = new EventGuest();
        testGuest.setEventId(testEvent.getId());
        testGuest.setGuest(guestEntity);
        testGuest.setRsvpStatus("open");
        testGuest = guestRepository.save(testGuest);

        testToken = new GuestToken();
        testToken.setGuestId(testGuest.getId());
        testToken.setToken("test-token-123");
        testToken.setValidUntil(LocalDateTime.now().plusDays(30));
        testToken = tokenRepository.save(testToken);
    }

    @Test
    void testCreateGift_WithIdZero_ShouldFixAndCreate() throws Exception {
        // Given - gift with id=0
        Gift gift = new Gift();
        gift.setId(0L); // Should be fixed to null
        gift.setTitle("Test Gift");
        gift.setDescription("Test Description");
        gift.setPrice(new BigDecimal("29.99"));
        gift.setStatus("open");

        // When & Then
        mockMvc.perform(post("/api/events/{eventId}/gifts", testEvent.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gift)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Test Gift"))
                .andExpect(jsonPath("$.status").value("open"))
                .andExpect(jsonPath("$.eventId").value(testEvent.getId()));

        // Verify in database
        assertThat(giftRepository.findAll()).hasSize(1);
        Gift saved = giftRepository.findAll().get(0);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isNotEqualTo(0L);
        assertThat(saved.getStatus()).isEqualTo("open");
    }

    @Test
    void testCreateGift_WithoutId() throws Exception {
        // Given
        Gift gift = new Gift();
        gift.setTitle("New Gift");
        gift.setPrice(new BigDecimal("19.99"));

        // When & Then
        mockMvc.perform(post("/api/events/{eventId}/gifts", testEvent.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gift)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("open"));
    }

    @Test
    void testReserveGift() throws Exception {
        // Given
        Gift gift = new Gift();
        gift.setEventId(testEvent.getId());
        gift.setTitle("Reservable Gift");
        gift.setStatus("open");
        Gift savedGift = giftRepository.save(gift);

        // When & Then
        mockMvc.perform(post("/api/invite/{token}/gifts/{giftId}/reserve", 
                        testToken.getToken(), savedGift.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("reserved"))
                .andExpect(jsonPath("$.reservedByGuest").value(testGuest.getId()));

        // Verify in database
        Gift reserved = giftRepository.findById(savedGift.getId()).orElseThrow();
        assertThat(reserved.getStatus()).isEqualTo("reserved");
        assertThat(reserved.getReservedByGuest()).isEqualTo(testGuest.getId());
        // Проверяем, что категории загружены (могут быть пустыми)
        assertThat(reserved.getCategories()).isNotNull();
    }

    @Test
    void testCancelReservation() throws Exception {
        // Given
        Gift gift = new Gift();
        gift.setEventId(testEvent.getId());
        gift.setTitle("Reserved Gift");
        gift.setStatus("reserved");
        gift.setReservedByGuest(testGuest.getId());
        Gift savedGift = giftRepository.save(gift);

        // When & Then
        mockMvc.perform(post("/api/invite/{token}/gifts/{giftId}/cancel",
                        testToken.getToken(), savedGift.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("open"))
                .andExpect(jsonPath("$.reservedByGuest").isEmpty());

        // Verify in database
        Gift cancelled = giftRepository.findById(savedGift.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo("open");
        assertThat(cancelled.getReservedByGuest()).isNull();
        // Проверяем, что категории загружены (могут быть пустыми)
        assertThat(cancelled.getCategories()).isNotNull();
    }

    @Test
    void testGetGiftsForEvent() throws Exception {
        // Given
        Gift gift1 = createTestGift("Gift 1");
        Gift gift2 = createTestGift("Gift 2");
        giftRepository.save(gift1);
        giftRepository.save(gift2);

        // When & Then
        mockMvc.perform(get("/api/events/{eventId}/gifts", testEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].categories").exists())
                .andExpect(jsonPath("$[1].categories").exists());
    }
    
    @Test
    void testCreateGift_WithCategories() throws Exception {
        // Given - получаем категорию из БД
        GiftCategory legoCategory = categoryRepository.findByName("Lego")
                .orElseThrow(() -> new RuntimeException("Lego category not found"));
        
        Gift gift = new Gift();
        gift.setTitle("Test Gift with Category");
        gift.setDescription("Test Description");
        gift.setPrice(new BigDecimal("29.99"));
        gift.setStatus("open");
        
        Set<GiftCategory> categories = new HashSet<>();
        categories.add(legoCategory);
        gift.setCategories(categories);

        // When & Then
        mockMvc.perform(post("/api/events/{eventId}/gifts", testEvent.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gift)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Test Gift with Category"))
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.categories.length()").value(1))
                .andExpect(jsonPath("$.categories[0].name").value("Lego"));

        // Verify in database - используем метод с загрузкой категорий
        Gift saved = giftRepository.findByIdWithCategories(
                giftRepository.findAll().stream()
                        .filter(g -> "Test Gift with Category".equals(g.getTitle()))
                        .findFirst()
                        .orElseThrow()
                        .getId()
        ).orElseThrow();
        assertThat(saved.getCategories()).hasSize(1);
        assertThat(saved.getCategories().iterator().next().getName()).isEqualTo("Lego");
    }
    
    @Test
    void testUpdateGift_WithCategories() throws Exception {
        // Given
        Gift gift = createTestGift("Gift to Update");
        Gift savedGift = giftRepository.save(gift);
        
        GiftCategory sportCategory = categoryRepository.findByName("Sport")
                .orElseThrow(() -> new RuntimeException("Sport category not found"));
        
        // When - обновляем подарок с категориями
        Gift updateGift = new Gift();
        updateGift.setTitle("Updated Gift");
        updateGift.setPrice(new BigDecimal("39.99"));
        Set<GiftCategory> categories = new HashSet<>();
        categories.add(sportCategory);
        updateGift.setCategories(categories);

        mockMvc.perform(put("/api/gifts/{id}", savedGift.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateGift)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Gift"))
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.categories.length()").value(1))
                .andExpect(jsonPath("$.categories[0].name").value("Sport"));

        // Verify in database - используем метод с загрузкой категорий
        Gift updated = giftRepository.findByIdWithCategories(savedGift.getId()).orElseThrow();
        assertThat(updated.getCategories()).hasSize(1);
        assertThat(updated.getCategories().iterator().next().getName()).isEqualTo("Sport");
    }

    @Test
    void testDeleteGift() throws Exception {
        // Given
        Gift gift = createTestGift("ToDelete");
        Gift saved = giftRepository.save(gift);
        Long giftId = saved.getId();

        // When & Then
        mockMvc.perform(delete("/api/gifts/{id}", giftId))
                .andExpect(status().isOk());

        // Verify deleted
        assertThat(giftRepository.findById(giftId)).isEmpty();
    }

    private Gift createTestGift(String title) {
        Gift gift = new Gift();
        gift.setEventId(testEvent.getId());
        gift.setTitle(title);
        gift.setPrice(new BigDecimal("25.00"));
        gift.setStatus("open");
        return gift;
    }
}

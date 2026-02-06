package eu.bb.app.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import eu.bb.app.backend.entity.User;
import eu.bb.app.backend.repository.ChatMessageRepository;
import eu.bb.app.backend.repository.ChildRepository;
import eu.bb.app.backend.repository.EventGuestRepository;
import eu.bb.app.backend.repository.EventRepository;
import eu.bb.app.backend.repository.GiftRepository;
import eu.bb.app.backend.repository.GuestTokenRepository;
import eu.bb.app.backend.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UsersControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private GiftRepository giftRepository;

    @Autowired
    private GuestTokenRepository guestTokenRepository;

    @Autowired
    private EventGuestRepository eventGuestRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ChildRepository childRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

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
    }

    @Test
    void testCreateUser_WithoutId() throws Exception {
        // Given
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setAddress("123 Main St");

        // When & Then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));

        // Verify in database
        assertThat(userRepository.findAll()).hasSize(1);
        User savedUser = userRepository.findAll().get(0);
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getFirstName()).isEqualTo("John");
    }

    @Test
    void testCreateUser_WithIdZero_ShouldFixAndCreate() throws Exception {
        // Given - user with id=0 (the bug scenario)
        User user = new User();
        user.setId(0L); // This should be fixed to null
        user.setFirstName("Jane");
        user.setLastName("Smith");
        user.setEmail("jane.smith@example.com");
        user.setAddress("456 Oak Ave");

        // When & Then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.email").value("jane.smith@example.com"));

        // Verify in database - should have auto-generated ID, not 0
        assertThat(userRepository.findAll()).hasSize(1);
        User savedUser = userRepository.findAll().get(0);
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getId()).isNotEqualTo(0L);
        assertThat(savedUser.getFirstName()).isEqualTo("Jane");
    }

    @Test
    void testGetAllUsers() throws Exception {
        // Given
        User user1 = createTestUser("User1", "user1@example.com");
        User user2 = createTestUser("User2", "user2@example.com");
        userRepository.save(user1);
        userRepository.save(user2);

        // When & Then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].firstName").exists())
                .andExpect(jsonPath("$[1].firstName").exists());
    }

    @Test
    void testGetUserById() throws Exception {
        // Given
        User user = createTestUser("Test", "test@example.com");
        User saved = userRepository.save(user);
        Long userId = saved.getId();

        // When & Then
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void testGetUserById_NotFound() throws Exception {
        // When & Then - ResponseStatusException with 404 should be thrown
        mockMvc.perform(get("/api/users/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateUser() throws Exception {
        // Given
        User user = createTestUser("Original", "original@example.com");
        User saved = userRepository.save(user);
        Long userId = saved.getId();

        User updatedUser = new User();
        updatedUser.setFirstName("Updated");
        updatedUser.setLastName("Name");
        updatedUser.setEmail("updated@example.com");
        updatedUser.setAddress("New Address");

        // When & Then
        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));

        // Verify in database
        User updated = userRepository.findById(userId).orElseThrow();
        assertThat(updated.getFirstName()).isEqualTo("Updated");
        assertThat(updated.getEmail()).isEqualTo("updated@example.com");
    }

    @Test
    void testDeleteUser() throws Exception {
        // Given
        User user = createTestUser("ToDelete", "delete@example.com");
        User saved = userRepository.save(user);
        Long userId = saved.getId();

        // When & Then
        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isOk());

        // Verify deleted
        assertThat(userRepository.findById(userId)).isEmpty();
        assertThat(userRepository.findAll()).isEmpty();
    }

    @Test
    void testCreateMultipleUsers_WithIdZero() throws Exception {
        // Given - multiple users with id=0
        User user1 = new User();
        user1.setId(0L);
        user1.setFirstName("User1");
        user1.setEmail("user1@example.com");

        User user2 = new User();
        user2.setId(0L);
        user2.setFirstName("User2");
        user2.setEmail("user2@example.com");

        // When
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2)))
                .andExpect(status().isOk());

        // Then - both should be created with different IDs
        assertThat(userRepository.findAll()).hasSize(2);
        assertThat(userRepository.findAll())
                .extracting(User::getId)
                .doesNotContain(0L)
                .doesNotHaveDuplicates();
    }

    private User createTestUser(String firstName, String email) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName("Last");
        user.setEmail(email);
        user.setAddress("Address");
        return user;
    }
}

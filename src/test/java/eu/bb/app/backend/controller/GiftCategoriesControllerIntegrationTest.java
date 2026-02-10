package eu.bb.app.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import eu.bb.app.backend.repository.GiftCategoryRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GiftCategoriesControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GiftCategoryRepository categoryRepository;

    @Test
    void testGetAllCategories() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(5)) // 5 категорий из миграции
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].id").exists());
        
        // Verify categories are sorted by name
        var categories = categoryRepository.findAllByOrderByNameAsc();
        assertThat(categories).hasSize(5);
        assertThat(categories.get(0).getName()).isEqualTo("Basteln");
        assertThat(categories.get(1).getName()).isEqualTo("Bücher");
        assertThat(categories.get(2).getName()).isEqualTo("Lego");
        assertThat(categories.get(3).getName()).isEqualTo("Outdoor");
        assertThat(categories.get(4).getName()).isEqualTo("Sport");
    }
    
    @Test
    void testGetAllCategories_ContainsExpectedCategories() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.name == 'Lego')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Sport')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Outdoor')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Bücher')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Basteln')]").exists());
    }
}

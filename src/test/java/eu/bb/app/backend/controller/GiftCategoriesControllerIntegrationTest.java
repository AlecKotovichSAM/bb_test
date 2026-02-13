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

import eu.bb.app.backend.entity.GiftCategory;
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
    void testGetAllCategories_ReturnsOnlyNonHidden() throws Exception {
        // Создаем видимую категорию
        GiftCategory visibleCategory = new GiftCategory();
        visibleCategory.setName("Test Category Visible");
        visibleCategory.setHidden(false);
        categoryRepository.save(visibleCategory);
        
        // Создаем скрытую категорию
        GiftCategory hiddenCategory = new GiftCategory();
        hiddenCategory.setName("Test Category Hidden");
        hiddenCategory.setHidden(true);
        categoryRepository.save(hiddenCategory);
        
        // When & Then - API должен возвращать только видимые категории
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1)) // Только одна видимая категория
                .andExpect(jsonPath("$[0].name").value("Test Category Visible"))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].hidden").doesNotExist()); // Поле hidden не должно возвращаться
        
        // Verify categories are sorted by name and only non-hidden
        var categories = categoryRepository.findByHiddenFalseOrderByNameAsc();
        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getName()).isEqualTo("Test Category Visible");
        assertThat(categories.get(0).getHidden()).isFalse();
    }
    
    @Test
    void testGetAllCategories_ExcludesHiddenCategories() throws Exception {
        // Создаем несколько видимых категорий
        GiftCategory cat1 = new GiftCategory();
        cat1.setName("Category A");
        cat1.setHidden(false);
        categoryRepository.save(cat1);
        
        GiftCategory cat2 = new GiftCategory();
        cat2.setName("Category B");
        cat2.setHidden(false);
        categoryRepository.save(cat2);
        
        // Создаем скрытую категорию
        GiftCategory hiddenCat = new GiftCategory();
        hiddenCat.setName("Hidden Category");
        hiddenCat.setHidden(true);
        categoryRepository.save(hiddenCat);
        
        // When & Then - API должен возвращать только видимые категории, отсортированные по имени
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Category A"))
                .andExpect(jsonPath("$[1].name").value("Category B"))
                .andExpect(jsonPath("$[?(@.name == 'Hidden Category')]").doesNotExist()); // Скрытая категория не должна быть в ответе
    }
    
    @Test
    void testGetAllCategories_EmptyWhenAllHidden() throws Exception {
        // Все существующие категории из миграции имеют hidden = true
        // When & Then - API должен возвращать пустой массив
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}

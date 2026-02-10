package eu.bb.app.backend.controller;

import eu.bb.app.backend.entity.GiftCategory;
import eu.bb.app.backend.repository.GiftCategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Gift Categories", description = "API для управления категориями подарков")
public class GiftCategoriesController {
    private static final Logger log = LoggerFactory.getLogger(GiftCategoriesController.class);
    private final GiftCategoryRepository categoryRepository;
    
    public GiftCategoriesController(GiftCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }
    
    @GetMapping("/categories")
    @Operation(summary = "Получить список всех категорий", description = "Возвращает все категории подарков, отсортированные по имени")
    @ApiResponse(responseCode = "200", description = "Список категорий успешно получен")
    public List<GiftCategory> list() {
        log.debug("Getting all gift categories");
        List<GiftCategory> categories = categoryRepository.findAllByOrderByNameAsc();
        log.info("Retrieved {} gift categories", categories.size());
        return categories;
    }
}

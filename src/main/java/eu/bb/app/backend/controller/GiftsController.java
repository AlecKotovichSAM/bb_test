package eu.bb.app.backend.controller;

import eu.bb.app.backend.entity.Gift;
import eu.bb.app.backend.entity.GuestToken;
import eu.bb.app.backend.repository.GiftRepository;
import eu.bb.app.backend.repository.GuestTokenRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Gifts", description = "API для управления подарками к событиям")
public class GiftsController {
    private static final Logger log = LoggerFactory.getLogger(GiftsController.class);
    private final GiftRepository gifts;
    private final GuestTokenRepository tokens;
    
    public GiftsController(GiftRepository gifts, GuestTokenRepository tokens) { this.gifts = gifts; this.tokens = tokens; }

    @PostMapping("/events/{eventId}/gifts")
    @Operation(summary = "Добавить подарок к событию", description = "Создает новый подарок для указанного события. Статус автоматически устанавливается в 'open'")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Подарок успешно добавлен"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено")
    })
    public Gift create(
            @Parameter(description = "ID события", required = true) @PathVariable Long eventId, 
            @RequestBody Gift g) {
        log.info("Creating gift '{}' for event ID: {}", g.getTitle(), eventId);
        // Убеждаемся, что ID установлен в null для нового подарка
        if (g.getId() != null && g.getId() == 0) {
            g.setId(null);
        }
        g.setEventId(eventId); 
        g.setStatus("open"); 
        Gift saved = gifts.save(g);
        log.info("Gift created successfully: ID={}, title={}, price={}", saved.getId(), saved.getTitle(), saved.getPrice());
        return saved;
    }
    
    @GetMapping("/events/{eventId}/gifts")
    @Operation(summary = "Получить список подарков события", description = "Возвращает все подарки, связанные с указанным событием")
    @ApiResponse(responseCode = "200", description = "Список подарков успешно получен")
    public List<Gift> list(@Parameter(description = "ID события", required = true) @PathVariable Long eventId) {
        log.debug("Getting gifts for event ID: {}", eventId);
        List<Gift> giftList = gifts.findByEventId(eventId);
        log.info("Retrieved {} gifts for event ID: {}", giftList.size(), eventId);
        return giftList;
    }
    
    @PutMapping("/gifts/{id}")
    @Operation(summary = "Обновить подарок", description = "Обновляет информацию о существующем подарке")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Подарок успешно обновлен"),
        @ApiResponse(responseCode = "404", description = "Подарок не найден")
    })
    public Gift update(
            @Parameter(description = "ID подарка", required = true) @PathVariable Long id, 
            @RequestBody Gift g) {
        log.info("Updating gift ID: {}", id);
        Gift existing = gifts.findById(id).orElseThrow(() -> {
            log.warn("Gift not found for update: {}", id);
            return new RuntimeException("Gift not found");
        });
        // Сохраняем eventId из существующего подарка
        g.setId(id);
        g.setEventId(existing.getEventId());
        // Сохраняем статус и reservedByGuest, если они не переданы
        if (g.getStatus() == null) {
            g.setStatus(existing.getStatus());
        }
        if (g.getReservedByGuest() == null) {
            g.setReservedByGuest(existing.getReservedByGuest());
        }
        Gift updated = gifts.save(g);
        log.info("Gift updated successfully: ID={}, title={}", updated.getId(), updated.getTitle());
        return updated;
    }
    
    @DeleteMapping("/gifts/{id}")
    @Operation(summary = "Удалить подарок", description = "Удаляет подарок из события")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Подарок успешно удален"),
        @ApiResponse(responseCode = "404", description = "Подарок не найден")
    })
    public void delete(@Parameter(description = "ID подарка", required = true) @PathVariable Long id) {
        log.info("Deleting gift ID: {}", id);
        gifts.deleteById(id);
        log.info("Gift deleted successfully: {}", id);
    }

    @PostMapping("/invite/{token}/gifts/{giftId}/reserve")
    @Operation(summary = "Зарезервировать подарок", description = "Позволяет гостю зарезервировать подарок по токену приглашения. Статус подарка меняется на 'reserved'")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Подарок успешно зарезервирован"),
        @ApiResponse(responseCode = "404", description = "Токен или подарок не найдены"),
        @ApiResponse(responseCode = "400", description = "Подарок уже зарезервирован")
    })
    public Gift reserve(
            @Parameter(description = "Токен приглашения гостя", required = true) @PathVariable String token, 
            @Parameter(description = "ID подарка", required = true) @PathVariable Long giftId) {
        log.info("Reserving gift ID: {} with token: {}", giftId, token);
        GuestToken t = tokens.findByToken(token).orElseThrow(() -> {
            log.warn("Token not found for gift reservation: {}", token);
            return new RuntimeException("Token not found");
        });
        Gift g = gifts.findById(giftId).orElseThrow(() -> {
            log.warn("Gift not found for reservation: {}", giftId);
            return new RuntimeException("Gift not found");
        });
        if ("reserved".equals(g.getStatus())) {
            log.warn("Gift already reserved: {}", giftId);
        }
        g.setStatus("reserved"); 
        g.setReservedByGuest(t.getGuestId()); 
        Gift reserved = gifts.save(g);
        log.info("Gift reserved successfully: ID={}, title={}, reserved by guest={}", reserved.getId(), reserved.getTitle(), reserved.getReservedByGuest());
        return reserved;
    }

    @PostMapping("/invite/{token}/gifts/{giftId}/cancel")
    @Operation(summary = "Отменить резервацию подарка", description = "Отменяет резервацию подарка, если она была сделана тем же гостем. Статус меняется на 'open'")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Резервация успешно отменена"),
        @ApiResponse(responseCode = "404", description = "Токен или подарок не найдены"),
        @ApiResponse(responseCode = "403", description = "Подарок зарезервирован другим гостем")
    })
    public Gift cancel(
            @Parameter(description = "Токен приглашения гостя", required = true) @PathVariable String token, 
            @Parameter(description = "ID подарка", required = true) @PathVariable Long giftId) {
        log.info("Cancelling gift reservation: gift ID={}, token={}", giftId, token);
        GuestToken t = tokens.findByToken(token).orElseThrow(() -> {
            log.warn("Token not found for gift cancellation: {}", token);
            return new RuntimeException("Token not found");
        });
        Gift g = gifts.findById(giftId).orElseThrow(() -> {
            log.warn("Gift not found for cancellation: {}", giftId);
            return new RuntimeException("Gift not found");
        });
        if (t.getGuestId().equals(g.getReservedByGuest())) {
            g.setStatus("open"); 
            g.setReservedByGuest(null);
            log.info("Gift reservation cancelled successfully: ID={}", g.getId());
        } else {
            log.warn("Gift reserved by different guest. Current guest: {}, token guest: {}", g.getReservedByGuest(), t.getGuestId());
        }
        return gifts.save(g);
    }
}

package eu.bb.app.backend.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.*;
import java.util.*;

class UC04_CreateEventE2ETest extends BaseE2ETest {
    @Test
    void create_event_with_user_child_location_and_status() {
        Map<String,Object> user = new HashMap<>();
        user.put("firstName", "Anna");
        user.put("lastName", "Muster");
        user.put("email", "anna@example.com");
        user.put("address", "Berlin");
        ResponseEntity<Map> ru = POST("/api/users", user, Map.class);
        assertThat(ru.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long userId = ((Number)ru.getBody().get("id")).longValue();

        Map<String,Object> child = new HashMap<>();
        child.put("firstName", "Levi");
        child.put("birthday", "2018-06-10");
        child.put("gender", "male");
        child.put("avatar", null);
        ResponseEntity<Map> rc = POST("/api/users/"+userId+"/children", child, Map.class);
        assertThat(rc.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long childId = ((Number)rc.getBody().get("id")).longValue();

        Map<String,Object> ev = new HashMap<>();
        ev.put("hostId", userId);
        ev.put("childId", childId);
        ev.put("datetime", "2026-03-15T15:00:00");
        ev.put("locationType", "manual");
        ev.put("location", "Berlin, Familiencafe");
        ev.put("status", "Draft");
        ResponseEntity<Map> re = POST("/api/events", ev, Map.class);
        assertThat(re.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String,Object> eventResponse = (Map<String,Object>) re.getBody();
        Map<String,Object> event = (Map<String,Object>) eventResponse.get("event");
        assertThat(event.get("id")).isNotNull();
        assertThat((String)event.get("status")).isEqualTo("Draft");
        assertThat(eventResponse.get("guests")).isNotNull();
    }

    @Test
    void create_event_with_guests_and_children() {
        Map<String,Object> user = new HashMap<>();
        user.put("firstName", "Anna");
        user.put("lastName", "Muster");
        user.put("email", "anna@example.com");
        user.put("address", "Berlin");
        ResponseEntity<Map> ru = POST("/api/users", user, Map.class);
        assertThat(ru.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long userId = ((Number)ru.getBody().get("id")).longValue();

        Map<String,Object> child = new HashMap<>();
        child.put("firstName", "Levi");
        child.put("birthday", "2018-06-10");
        child.put("gender", "male");
        child.put("avatar", null);
        ResponseEntity<Map> rc = POST("/api/users/"+userId+"/children", child, Map.class);
        assertThat(rc.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long childId = ((Number)rc.getBody().get("id")).longValue();

        // Создаем событие с гостями и их детьми
        Map<String,Object> ev = new HashMap<>();
        ev.put("hostId", userId);
        ev.put("childId", childId);
        ev.put("datetime", "2026-03-15T15:00:00");
        ev.put("locationType", "manual");
        ev.put("location", "Berlin, Familiencafe");
        ev.put("status", "Planned");
        
        List<Map<String,Object>> guests = new ArrayList<>();
        Map<String,Object> guest1 = new HashMap<>();
        guest1.put("guestName", "Sophie");
        guest1.put("children", Arrays.asList("Max", "Emma"));
        guests.add(guest1);
        
        Map<String,Object> guest2 = new HashMap<>();
        guest2.put("guestName", "Ben");
        guest2.put("children", Arrays.asList("Tom"));
        guests.add(guest2);
        
        Map<String,Object> guest3 = new HashMap<>();
        guest3.put("guestName", "Clara");
        // без детей
        guests.add(guest3);
        
        ev.put("guests", guests);
        
        ResponseEntity<Map> re = POST("/api/events", ev, Map.class);
        assertThat(re.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String,Object> eventResponse = (Map<String,Object>) re.getBody();
        Map<String,Object> event = (Map<String,Object>) eventResponse.get("event");
        assertThat(event.get("id")).isNotNull();
        assertThat((String)event.get("status")).isEqualTo("Planned");
        
        List<Map<String,Object>> guestsResponse = (List<Map<String,Object>>) eventResponse.get("guests");
        assertThat(guestsResponse).hasSize(3);
        
        // Проверяем первого гостя с двумя детьми
        Map<String,Object> guest1Response = guestsResponse.get(0);
        Map<String,Object> guest1Data = (Map<String,Object>) guest1Response.get("guest");
        assertThat(guest1Data.get("guestName")).isEqualTo("Sophie");
        List<Map<String,Object>> children1 = (List<Map<String,Object>>) guest1Response.get("children");
        assertThat(children1).hasSize(2);
        assertThat(children1.get(0).get("firstName")).isIn("Max", "Emma");
        assertThat(children1.get(1).get("firstName")).isIn("Max", "Emma");
        
        // Проверяем второго гостя с одним ребенком
        Map<String,Object> guest2Response = guestsResponse.get(1);
        Map<String,Object> guest2Data = (Map<String,Object>) guest2Response.get("guest");
        assertThat(guest2Data.get("guestName")).isEqualTo("Ben");
        List<Map<String,Object>> children2 = (List<Map<String,Object>>) guest2Response.get("children");
        assertThat(children2).hasSize(1);
        assertThat(children2.get(0).get("firstName")).isEqualTo("Tom");
        
        // Проверяем третьего гостя без детей
        Map<String,Object> guest3Response = guestsResponse.get(2);
        Map<String,Object> guest3Data = (Map<String,Object>) guest3Response.get("guest");
        assertThat(guest3Data.get("guestName")).isEqualTo("Clara");
        List<Map<String,Object>> children3 = (List<Map<String,Object>>) guest3Response.get("children");
        assertThat(children3).isEmpty();
    }

    @Test
    void create_event_reuse_guest_should_not_duplicate() {
        // Given - создаем пользователя и ребенка
        Map<String,Object> user = new HashMap<>();
        user.put("firstName", "Test");
        user.put("lastName", "User");
        user.put("email", "test@example.com");
        user.put("address", "Test Address");
        ResponseEntity<Map> ru = POST("/api/users", user, Map.class);
        assertThat(ru.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long userId = ((Number)ru.getBody().get("id")).longValue();

        Map<String,Object> child = new HashMap<>();
        child.put("firstName", "TestChild");
        child.put("birthday", "2018-06-10");
        child.put("gender", "male");
        child.put("avatar", null);
        ResponseEntity<Map> rc = POST("/api/users/"+userId+"/children", child, Map.class);
        assertThat(rc.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long childId = ((Number)rc.getBody().get("id")).longValue();

        // When - создаем первый ивент с новым гостем "Юлька" и ребенком "Ксюша"
        Map<String,Object> event1 = new HashMap<>();
        event1.put("hostId", userId);
        event1.put("childId", childId);
        event1.put("datetime", "2026-02-09T19:51:05");
        event1.put("locationType", "pizzeria");
        event1.put("location", "PIZZA TEMPO NEMIGA");
        event1.put("comment", "ДР Лёвы!");
        
        List<Map<String,Object>> guests1 = new ArrayList<>();
        Map<String,Object> guest1 = new HashMap<>();
        guest1.put("guestName", "Юлька");
        guest1.put("children", Arrays.asList("Ксюша"));
        guests1.add(guest1);
        event1.put("guests", guests1);
        
        ResponseEntity<Map> re1 = POST("/api/events", event1, Map.class);
        assertThat(re1.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String,Object> event1Response = (Map<String,Object>) re1.getBody();
        List<Map<String,Object>> guests1Response = (List<Map<String,Object>>) event1Response.get("guests");
        assertThat(guests1Response).hasSize(1);
        
        // Сохраняем guestId из первого ивента
        Map<String,Object> guest1Data = (Map<String,Object>) guests1Response.get(0).get("guest");
        // Используем guestId (ID из таблицы guests), а не id (ID EventGuest)
        Long guestId = ((Number)guest1Data.get("guestId")).longValue();
        assertThat(guest1Data.get("guestName")).isEqualTo("Юлька");
        
        // Проверяем через GET /api/users/{userId}/guests с фильтрацией по guestId, что дети найдены
        ResponseEntity<List> guestsListResponseBefore = GET("/api/users/"+userId+"/guests", List.class);
        assertThat(guestsListResponseBefore.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String,Object>> allGuestsBefore = guestsListResponseBefore.getBody();
        
        // Фильтруем по guestId и проверяем, что гость найден с детьми
        Map<String,Object> guestWithChildren = allGuestsBefore.stream()
                .filter(g -> {
                    Map<String,Object> guest = (Map<String,Object>) g.get("guest");
                    return guest != null && guestId.equals(((Number) guest.get("guestId")).longValue());
                })
                .findFirst()
                .orElse(null);
        
        assertThat(guestWithChildren).isNotNull()
                .as("Гость с guestId=%d должен быть найден через API /api/users/{userId}/guests", guestId);
        
        Map<String,Object> foundGuest = (Map<String,Object>) guestWithChildren.get("guest");
        assertThat(foundGuest.get("guestName")).isEqualTo("Юлька");
        
        // Проверяем, что у гостя есть дети
        List<Map<String,Object>> children = (List<Map<String,Object>>) guestWithChildren.get("children");
        assertThat(children).isNotNull().as("У гостя должны быть дети");
        assertThat(children.size()).isGreaterThan(0)
                .as("У гостя должен быть хотя бы один ребенок. Найдено детей: %d", children.size());
        
        // When - создаем второй ивент с тем же гостем, используя guestId
        Map<String,Object> event2 = new HashMap<>();
        event2.put("hostId", userId);
        event2.put("childId", childId);
        event2.put("datetime", "2026-02-09T19:54:56.941Z");
        event2.put("locationType", "ресторан-2");
        event2.put("location", "Литвины-2");
        event2.put("comment", "ДР-2");
        
        List<Map<String,Object>> guests2 = new ArrayList<>();
        Map<String,Object> reusedGuest = new HashMap<>();
        reusedGuest.put("guestId", guestId); // переиспользуем гостя из первого ивента
        guests2.add(reusedGuest);
        event2.put("guests", guests2);
        
        ResponseEntity<Map> re2 = POST("/api/events", event2, Map.class);
        assertThat(re2.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String,Object> event2Response = (Map<String,Object>) re2.getBody();
        List<Map<String,Object>> guests2Response = (List<Map<String,Object>>) event2Response.get("guests");
        assertThat(guests2Response).hasSize(1);
        
        // Then - проверяем через GET /api/users/{userId}/guests, что гость не дублируется
        // Должен быть только один гость с именем "Юлька", а не два
        ResponseEntity<List> guestsListResponse = GET("/api/users/"+userId+"/guests", List.class);
        assertThat(guestsListResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String,Object>> allGuests = guestsListResponse.getBody();
        
        // Фильтруем гостей с именем "Юлька"
        long yulkaCount = allGuests.stream()
                .map(g -> (Map<String,Object>) g.get("guest"))
                .map(g -> (String) g.get("guestName"))
                .filter(name -> "Юлька".equals(name))
                .count();
        
        // БАГ: если баг есть, то будет 2 гостя с именем "Юлька" вместо 1
        // Ожидаем, что должен быть только 1 гость с именем "Юлька"
        assertThat(yulkaCount)
                .as("Гость 'Юлька' не должен дублироваться при переиспользовании через guestId. " +
                    "Ожидается 1 гость, но найдено %d", yulkaCount)
                .isEqualTo(1);
    }

    @Test
    void create_event_reuse_guest_with_auto_copy_children() {
        // Given - создаем пользователя и ребенка
        Map<String,Object> user = new HashMap<>();
        user.put("firstName", "Test");
        user.put("lastName", "User");
        user.put("email", "test@example.com");
        user.put("address", "Test Address");
        ResponseEntity<Map> ru = POST("/api/users", user, Map.class);
        Long userId = ((Number)ru.getBody().get("id")).longValue();

        Map<String,Object> child = new HashMap<>();
        child.put("firstName", "TestChild");
        child.put("birthday", "2018-06-10");
        child.put("gender", "male");
        ResponseEntity<Map> rc = POST("/api/users/"+userId+"/children", child, Map.class);
        Long childId = ((Number)rc.getBody().get("id")).longValue();

        // When - создаем первое событие с гостем и двумя детьми
        Map<String,Object> event1 = new HashMap<>();
        event1.put("hostId", userId);
        event1.put("childId", childId);
        event1.put("datetime", "2026-03-15T15:00:00");
        event1.put("status", "Planned");
        
        List<Map<String,Object>> guests1 = new ArrayList<>();
        Map<String,Object> guest1 = new HashMap<>();
        guest1.put("guestName", "Sophie");
        guest1.put("children", Arrays.asList("Max", "Emma"));
        guests1.add(guest1);
        event1.put("guests", guests1);
        
        ResponseEntity<Map> re1 = POST("/api/events", event1, Map.class);
        assertThat(re1.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String,Object> event1Response = (Map<String,Object>) re1.getBody();
        List<Map<String,Object>> guests1Response = (List<Map<String,Object>>) event1Response.get("guests");
        assertThat(guests1Response).hasSize(1);
        
        Map<String,Object> guest1Response = guests1Response.get(0);
        Map<String,Object> guest1Data = (Map<String,Object>) guest1Response.get("guest");
        Long guestId = ((Number)guest1Data.get("guestId")).longValue();
        
        // Проверяем, что дети были добавлены к гостю в первом событии
        List<Map<String,Object>> children1 = (List<Map<String,Object>>) guest1Response.get("children");
        assertThat(children1).hasSize(2).as("Дети должны быть добавлены к гостю в первом событии");
        assertThat(children1.get(0).get("firstName")).isIn("Max", "Emma");
        assertThat(children1.get(1).get("firstName")).isIn("Max", "Emma");
        
        // Проверяем через GET /api/users/{userId}/guests с фильтрацией по guestId, что дети найдены
        ResponseEntity<List> guestsListResponseBefore = GET("/api/users/"+userId+"/guests", List.class);
        assertThat(guestsListResponseBefore.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String,Object>> allGuestsBefore = guestsListResponseBefore.getBody();
        
        // Фильтруем по guestId и проверяем, что гость найден с детьми
        Map<String,Object> guestWithChildren = allGuestsBefore.stream()
                .filter(g -> {
                    Map<String,Object> guest = (Map<String,Object>) g.get("guest");
                    return guest != null && guestId.equals(((Number) guest.get("guestId")).longValue());
                })
                .findFirst()
                .orElse(null);
        
        assertThat(guestWithChildren).isNotNull()
                .as("Гость с guestId=%d должен быть найден через API /api/users/{userId}/guests", guestId);
        
        Map<String,Object> foundGuest = (Map<String,Object>) guestWithChildren.get("guest");
        assertThat(foundGuest.get("guestName")).isEqualTo("Sophie");
        
        // Проверяем, что у гостя есть дети
        List<Map<String,Object>> childrenBefore = (List<Map<String,Object>>) guestWithChildren.get("children");
        assertThat(childrenBefore).isNotNull().as("У гостя должны быть дети");
        assertThat(childrenBefore.size()).isEqualTo(2)
                .as("У гостя должно быть 2 ребенка. Найдено детей: %d", childrenBefore.size());
        assertThat(childrenBefore.get(0).get("firstName")).isIn("Max", "Emma");
        assertThat(childrenBefore.get(1).get("firstName")).isIn("Max", "Emma");
        
        // When - создаем второе событие, переиспользуя гостя БЕЗ указания детей
        Map<String,Object> event2 = new HashMap<>();
        event2.put("hostId", userId);
        event2.put("childId", childId);
        event2.put("datetime", "2026-04-20T15:00:00");
        event2.put("status", "Planned");
        
        List<Map<String,Object>> guests2 = new ArrayList<>();
        Map<String,Object> reusedGuest = new HashMap<>();
        reusedGuest.put("guestId", guestId); // переиспользуем гостя, дети не указаны
        guests2.add(reusedGuest);
        event2.put("guests", guests2);
        
        ResponseEntity<Map> re2 = POST("/api/events", event2, Map.class);
        assertThat(re2.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String,Object> event2Response = (Map<String,Object>) re2.getBody();
        List<Map<String,Object>> guests2Response = (List<Map<String,Object>>) event2Response.get("guests");
        
        // Then - проверяем, что дети автоматически скопировались
        assertThat(guests2Response).hasSize(1);
        Map<String,Object> reusedGuestResponse = guests2Response.get(0);
        Map<String,Object> reusedGuestData = (Map<String,Object>) reusedGuestResponse.get("guest");
        assertThat(reusedGuestData.get("guestName")).isEqualTo("Sophie");
        assertThat(((Number)reusedGuestData.get("guestId")).longValue()).isEqualTo(guestId);
        
        List<Map<String,Object>> children2 = (List<Map<String,Object>>) reusedGuestResponse.get("children");
        assertThat(children2).hasSize(2);
        assertThat(children2.get(0).get("firstName")).isIn("Max", "Emma");
        assertThat(children2.get(1).get("firstName")).isIn("Max", "Emma");
    }

    @Test
    void create_event_reuse_guest_with_override_children() {
        // Given - создаем пользователя и ребенка
        Map<String,Object> user = new HashMap<>();
        user.put("firstName", "Test");
        user.put("lastName", "User");
        user.put("email", "test@example.com");
        user.put("address", "Test Address");
        ResponseEntity<Map> ru = POST("/api/users", user, Map.class);
        Long userId = ((Number)ru.getBody().get("id")).longValue();

        Map<String,Object> child = new HashMap<>();
        child.put("firstName", "TestChild");
        child.put("birthday", "2018-06-10");
        child.put("gender", "male");
        ResponseEntity<Map> rc = POST("/api/users/"+userId+"/children", child, Map.class);
        Long childId = ((Number)rc.getBody().get("id")).longValue();

        // When - создаем первое событие с гостем и детьми
        Map<String,Object> event1 = new HashMap<>();
        event1.put("hostId", userId);
        event1.put("childId", childId);
        event1.put("datetime", "2026-03-15T15:00:00");
        event1.put("status", "Planned");
        
        List<Map<String,Object>> guests1 = new ArrayList<>();
        Map<String,Object> guest1 = new HashMap<>();
        guest1.put("guestName", "Sophie");
        guest1.put("children", Arrays.asList("Max", "Emma"));
        guests1.add(guest1);
        event1.put("guests", guests1);
        
        ResponseEntity<Map> re1 = POST("/api/events", event1, Map.class);
        Map<String,Object> event1Response = (Map<String,Object>) re1.getBody();
        List<Map<String,Object>> guests1Response = (List<Map<String,Object>>) event1Response.get("guests");
        Map<String,Object> guest1Data = (Map<String,Object>) guests1Response.get(0).get("guest");
        Long guestId = ((Number)guest1Data.get("guestId")).longValue();
        
        // When - создаем второе событие, переиспользуя гостя С переопределением детей
        Map<String,Object> event2 = new HashMap<>();
        event2.put("hostId", userId);
        event2.put("childId", childId);
        event2.put("datetime", "2026-04-20T15:00:00");
        event2.put("status", "Planned");
        
        List<Map<String,Object>> guests2 = new ArrayList<>();
        Map<String,Object> reusedGuest = new HashMap<>();
        reusedGuest.put("guestId", guestId);
        reusedGuest.put("children", Arrays.asList("NewChild1", "NewChild2")); // переопределяем детей
        guests2.add(reusedGuest);
        event2.put("guests", guests2);
        
        ResponseEntity<Map> re2 = POST("/api/events", event2, Map.class);
        assertThat(re2.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String,Object> event2Response = (Map<String,Object>) re2.getBody();
        List<Map<String,Object>> guests2Response = (List<Map<String,Object>>) event2Response.get("guests");
        
        // Then - проверяем, что дети переопределены
        assertThat(guests2Response).hasSize(1);
        Map<String,Object> reusedGuestResponse = guests2Response.get(0);
        Map<String,Object> reusedGuestData = (Map<String,Object>) reusedGuestResponse.get("guest");
        assertThat(reusedGuestData.get("guestName")).isEqualTo("Sophie");
        
        List<Map<String,Object>> children2 = (List<Map<String,Object>>) reusedGuestResponse.get("children");
        assertThat(children2).hasSize(2);
        assertThat(children2.get(0).get("firstName")).isEqualTo("NewChild1");
        assertThat(children2.get(1).get("firstName")).isEqualTo("NewChild2");
    }

    @Test
    void create_event_reuse_guest_with_override_name() {
        // Given - создаем пользователя и ребенка
        Map<String,Object> user = new HashMap<>();
        user.put("firstName", "Test");
        user.put("lastName", "User");
        user.put("email", "test@example.com");
        user.put("address", "Test Address");
        ResponseEntity<Map> ru = POST("/api/users", user, Map.class);
        Long userId = ((Number)ru.getBody().get("id")).longValue();

        Map<String,Object> child = new HashMap<>();
        child.put("firstName", "TestChild");
        child.put("birthday", "2018-06-10");
        child.put("gender", "male");
        ResponseEntity<Map> rc = POST("/api/users/"+userId+"/children", child, Map.class);
        Long childId = ((Number)rc.getBody().get("id")).longValue();

        // When - создаем первое событие с гостем
        Map<String,Object> event1 = new HashMap<>();
        event1.put("hostId", userId);
        event1.put("childId", childId);
        event1.put("datetime", "2026-03-15T15:00:00");
        event1.put("status", "Planned");
        
        List<Map<String,Object>> guests1 = new ArrayList<>();
        Map<String,Object> guest1 = new HashMap<>();
        guest1.put("guestName", "Sophie");
        guests1.add(guest1);
        event1.put("guests", guests1);
        
        ResponseEntity<Map> re1 = POST("/api/events", event1, Map.class);
        Map<String,Object> event1Response = (Map<String,Object>) re1.getBody();
        List<Map<String,Object>> guests1Response = (List<Map<String,Object>>) event1Response.get("guests");
        Map<String,Object> guest1Data = (Map<String,Object>) guests1Response.get(0).get("guest");
        Long guestId = ((Number)guest1Data.get("guestId")).longValue();
        
        // When - создаем второе событие, переиспользуя гостя С обновлением имени
        Map<String,Object> event2 = new HashMap<>();
        event2.put("hostId", userId);
        event2.put("childId", childId);
        event2.put("datetime", "2026-04-20T15:00:00");
        event2.put("status", "Planned");
        
        List<Map<String,Object>> guests2 = new ArrayList<>();
        Map<String,Object> reusedGuest = new HashMap<>();
        reusedGuest.put("guestId", guestId);
        reusedGuest.put("guestName", "Sophie Updated"); // обновляем имя
        guests2.add(reusedGuest);
        event2.put("guests", guests2);
        
        ResponseEntity<Map> re2 = POST("/api/events", event2, Map.class);
        assertThat(re2.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String,Object> event2Response = (Map<String,Object>) re2.getBody();
        List<Map<String,Object>> guests2Response = (List<Map<String,Object>>) event2Response.get("guests");
        
        // Then - проверяем, что имя обновлено
        assertThat(guests2Response).hasSize(1);
        Map<String,Object> reusedGuestData = (Map<String,Object>) guests2Response.get(0).get("guest");
        assertThat(reusedGuestData.get("guestName")).isEqualTo("Sophie Updated");
        assertThat(((Number)reusedGuestData.get("guestId")).longValue()).isEqualTo(guestId); // guestId остается тем же
    }

    @Test
    void create_event_reuse_guest_without_children_should_not_copy() {
        // Given - создаем пользователя и ребенка
        Map<String,Object> user = new HashMap<>();
        user.put("firstName", "Test");
        user.put("lastName", "User");
        user.put("email", "test@example.com");
        user.put("address", "Test Address");
        ResponseEntity<Map> ru = POST("/api/users", user, Map.class);
        Long userId = ((Number)ru.getBody().get("id")).longValue();

        Map<String,Object> child = new HashMap<>();
        child.put("firstName", "TestChild");
        child.put("birthday", "2018-06-10");
        child.put("gender", "male");
        ResponseEntity<Map> rc = POST("/api/users/"+userId+"/children", child, Map.class);
        Long childId = ((Number)rc.getBody().get("id")).longValue();

        // When - создаем первое событие с гостем БЕЗ детей
        Map<String,Object> event1 = new HashMap<>();
        event1.put("hostId", userId);
        event1.put("childId", childId);
        event1.put("datetime", "2026-03-15T15:00:00");
        event1.put("status", "Planned");
        
        List<Map<String,Object>> guests1 = new ArrayList<>();
        Map<String,Object> guest1 = new HashMap<>();
        guest1.put("guestName", "Sophie");
        // дети не указаны
        guests1.add(guest1);
        event1.put("guests", guests1);
        
        ResponseEntity<Map> re1 = POST("/api/events", event1, Map.class);
        Map<String,Object> event1Response = (Map<String,Object>) re1.getBody();
        List<Map<String,Object>> guests1Response = (List<Map<String,Object>>) event1Response.get("guests");
        Map<String,Object> guest1Data = (Map<String,Object>) guests1Response.get(0).get("guest");
        Long guestId = ((Number)guest1Data.get("guestId")).longValue();
        
        // When - создаем второе событие, переиспользуя гостя БЕЗ указания детей
        Map<String,Object> event2 = new HashMap<>();
        event2.put("hostId", userId);
        event2.put("childId", childId);
        event2.put("datetime", "2026-04-20T15:00:00");
        event2.put("status", "Planned");
        
        List<Map<String,Object>> guests2 = new ArrayList<>();
        Map<String,Object> reusedGuest = new HashMap<>();
        reusedGuest.put("guestId", guestId);
        // дети не указаны
        guests2.add(reusedGuest);
        event2.put("guests", guests2);
        
        ResponseEntity<Map> re2 = POST("/api/events", event2, Map.class);
        assertThat(re2.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String,Object> event2Response = (Map<String,Object>) re2.getBody();
        List<Map<String,Object>> guests2Response = (List<Map<String,Object>>) event2Response.get("guests");
        
        // Then - проверяем, что детей нет (их не было в первом событии)
        assertThat(guests2Response).hasSize(1);
        Map<String,Object> reusedGuestResponse = guests2Response.get(0);
        Map<String,Object> reusedGuestData = (Map<String,Object>) reusedGuestResponse.get("guest");
        assertThat(reusedGuestData.get("guestName")).isEqualTo("Sophie");
        
        List<Map<String,Object>> children2 = (List<Map<String,Object>>) reusedGuestResponse.get("children");
        assertThat(children2).isEmpty();
    }

    @Test
    void create_event_reuse_nonexistent_guest_should_fail() {
        // Given - создаем пользователя и ребенка
        Map<String,Object> user = new HashMap<>();
        user.put("firstName", "Test");
        user.put("lastName", "User");
        user.put("email", "test@example.com");
        user.put("address", "Test Address");
        ResponseEntity<Map> ru = POST("/api/users", user, Map.class);
        Long userId = ((Number)ru.getBody().get("id")).longValue();

        Map<String,Object> child = new HashMap<>();
        child.put("firstName", "TestChild");
        child.put("birthday", "2018-06-10");
        child.put("gender", "male");
        ResponseEntity<Map> rc = POST("/api/users/"+userId+"/children", child, Map.class);
        Long childId = ((Number)rc.getBody().get("id")).longValue();

        // When - пытаемся создать событие с несуществующим guestId
        Map<String,Object> event = new HashMap<>();
        event.put("hostId", userId);
        event.put("childId", childId);
        event.put("datetime", "2026-03-15T15:00:00");
        event.put("status", "Planned");
        
        List<Map<String,Object>> guests = new ArrayList<>();
        Map<String,Object> reusedGuest = new HashMap<>();
        reusedGuest.put("guestId", 99999L); // несуществующий ID
        guests.add(reusedGuest);
        event.put("guests", guests);
        
        // Then - должна быть ошибка 400
        ResponseEntity<Map> re = POST("/api/events", event, Map.class);
        assertThat(re.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_event_new_guest_without_name_should_fail() {
        // Given - создаем пользователя и ребенка
        Map<String,Object> user = new HashMap<>();
        user.put("firstName", "Test");
        user.put("lastName", "User");
        user.put("email", "test@example.com");
        user.put("address", "Test Address");
        ResponseEntity<Map> ru = POST("/api/users", user, Map.class);
        Long userId = ((Number)ru.getBody().get("id")).longValue();

        Map<String,Object> child = new HashMap<>();
        child.put("firstName", "TestChild");
        child.put("birthday", "2018-06-10");
        child.put("gender", "male");
        ResponseEntity<Map> rc = POST("/api/users/"+userId+"/children", child, Map.class);
        Long childId = ((Number)rc.getBody().get("id")).longValue();

        // When - пытаемся создать событие с новым гостем без имени
        Map<String,Object> event = new HashMap<>();
        event.put("hostId", userId);
        event.put("childId", childId);
        event.put("datetime", "2026-03-15T15:00:00");
        event.put("status", "Planned");
        
        List<Map<String,Object>> guests = new ArrayList<>();
        Map<String,Object> newGuest = new HashMap<>();
        // guestName не указан
        newGuest.put("children", Arrays.asList("Tom"));
        guests.add(newGuest);
        event.put("guests", guests);
        
        // Then - должна быть ошибка 400
        ResponseEntity<Map> re = POST("/api/events", event, Map.class);
        assertThat(re.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_event_response_contains_guestId() {
        // Given - создаем пользователя и ребенка
        Map<String,Object> user = new HashMap<>();
        user.put("firstName", "Test");
        user.put("lastName", "User");
        user.put("email", "test@example.com");
        user.put("address", "Test Address");
        ResponseEntity<Map> ru = POST("/api/users", user, Map.class);
        Long userId = ((Number)ru.getBody().get("id")).longValue();

        Map<String,Object> child = new HashMap<>();
        child.put("firstName", "TestChild");
        child.put("birthday", "2018-06-10");
        child.put("gender", "male");
        ResponseEntity<Map> rc = POST("/api/users/"+userId+"/children", child, Map.class);
        Long childId = ((Number)rc.getBody().get("id")).longValue();

        // When - создаем событие с гостем
        Map<String,Object> event = new HashMap<>();
        event.put("hostId", userId);
        event.put("childId", childId);
        event.put("datetime", "2026-03-15T15:00:00");
        event.put("status", "Planned");
        
        List<Map<String,Object>> guests = new ArrayList<>();
        Map<String,Object> guest = new HashMap<>();
        guest.put("guestName", "Sophie");
        guest.put("children", Arrays.asList("Max"));
        guests.add(guest);
        event.put("guests", guests);
        
        ResponseEntity<Map> re = POST("/api/events", event, Map.class);
        assertThat(re.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String,Object> eventResponse = (Map<String,Object>) re.getBody();
        List<Map<String,Object>> guestsResponse = (List<Map<String,Object>>) eventResponse.get("guests");
        
        // Then - проверяем, что в ответе есть guestId
        assertThat(guestsResponse).hasSize(1);
        Map<String,Object> guestData = (Map<String,Object>) guestsResponse.get(0).get("guest");
        assertThat(guestData.get("id")).isNotNull(); // ID EventGuest
        assertThat(guestData.get("guestId")).isNotNull(); // ID Guest из таблицы guests
        assertThat(guestData.get("guestName")).isEqualTo("Sophie");
        
        // Проверяем, что guestId можно использовать для переиспользования
        Long guestId = ((Number)guestData.get("guestId")).longValue();
        assertThat(guestId).isNotNull();
        assertThat(guestId).isGreaterThan(0);
    }

    @Test
    void create_event_reuse_guest_creates_new_eventguest() {
        // Given - создаем пользователя и ребенка
        Map<String,Object> user = new HashMap<>();
        user.put("firstName", "Test");
        user.put("lastName", "User");
        user.put("email", "test@example.com");
        user.put("address", "Test Address");
        ResponseEntity<Map> ru = POST("/api/users", user, Map.class);
        Long userId = ((Number)ru.getBody().get("id")).longValue();

        Map<String,Object> child = new HashMap<>();
        child.put("firstName", "TestChild");
        child.put("birthday", "2018-06-10");
        child.put("gender", "male");
        ResponseEntity<Map> rc = POST("/api/users/"+userId+"/children", child, Map.class);
        Long childId = ((Number)rc.getBody().get("id")).longValue();

        // When - создаем первое событие с гостем
        Map<String,Object> event1 = new HashMap<>();
        event1.put("hostId", userId);
        event1.put("childId", childId);
        event1.put("datetime", "2026-03-15T15:00:00");
        event1.put("status", "Planned");
        
        List<Map<String,Object>> guests1 = new ArrayList<>();
        Map<String,Object> guest1 = new HashMap<>();
        guest1.put("guestName", "Sophie");
        guests1.add(guest1);
        event1.put("guests", guests1);
        
        ResponseEntity<Map> re1 = POST("/api/events", event1, Map.class);
        Map<String,Object> event1Response = (Map<String,Object>) re1.getBody();
        Map<String,Object> event1Data = (Map<String,Object>) event1Response.get("event");
        Long event1Id = ((Number)event1Data.get("id")).longValue();
        
        List<Map<String,Object>> guests1Response = (List<Map<String,Object>>) event1Response.get("guests");
        Map<String,Object> guest1Data = (Map<String,Object>) guests1Response.get(0).get("guest");
        Long guestId = ((Number)guest1Data.get("guestId")).longValue();
        Long eventGuest1Id = ((Number)guest1Data.get("id")).longValue();
        
        // When - создаем второе событие, переиспользуя гостя
        Map<String,Object> event2 = new HashMap<>();
        event2.put("hostId", userId);
        event2.put("childId", childId);
        event2.put("datetime", "2026-04-20T15:00:00");
        event2.put("status", "Planned");
        
        List<Map<String,Object>> guests2 = new ArrayList<>();
        Map<String,Object> reusedGuest = new HashMap<>();
        reusedGuest.put("guestId", guestId);
        guests2.add(reusedGuest);
        event2.put("guests", guests2);
        
        ResponseEntity<Map> re2 = POST("/api/events", event2, Map.class);
        Map<String,Object> event2Response = (Map<String,Object>) re2.getBody();
        Map<String,Object> event2Data = (Map<String,Object>) event2Response.get("event");
        Long event2Id = ((Number)event2Data.get("id")).longValue();
        
        List<Map<String,Object>> guests2Response = (List<Map<String,Object>>) event2Response.get("guests");
        Map<String,Object> guest2Data = (Map<String,Object>) guests2Response.get(0).get("guest");
        Long eventGuest2Id = ((Number)guest2Data.get("id")).longValue();
        
        // Then - проверяем, что создан новый EventGuest, но используется тот же Guest
        assertThat(eventGuest2Id).isNotEqualTo(eventGuest1Id); // разные EventGuest ID
        assertThat(((Number)guest2Data.get("guestId")).longValue()).isEqualTo(guestId); // тот же Guest ID
        
        // Проверяем через GET, что оба события имеют гостей
        ResponseEntity<Map> getEvent1 = GET("/api/events/"+event1Id, Map.class);
        assertThat(getEvent1.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String,Object> getEvent1Response = (Map<String,Object>) getEvent1.getBody();
        List<Map<String,Object>> getGuests1 = (List<Map<String,Object>>) getEvent1Response.get("guests");
        assertThat(getGuests1).hasSize(1);
        
        ResponseEntity<Map> getEvent2 = GET("/api/events/"+event2Id, Map.class);
        assertThat(getEvent2.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String,Object> getEvent2Response = (Map<String,Object>) getEvent2.getBody();
        List<Map<String,Object>> getGuests2 = (List<Map<String,Object>>) getEvent2Response.get("guests");
        assertThat(getGuests2).hasSize(1);
        
        // Проверяем, что guestId одинаковый в обоих событиях
        Map<String,Object> getGuest1Data = (Map<String,Object>) getGuests1.get(0).get("guest");
        Map<String,Object> getGuest2Data = (Map<String,Object>) getGuests2.get(0).get("guest");
        assertThat(getGuest1Data.get("guestId")).isEqualTo(getGuest2Data.get("guestId"));
    }
}

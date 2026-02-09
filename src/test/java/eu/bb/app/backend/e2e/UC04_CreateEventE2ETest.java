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
}

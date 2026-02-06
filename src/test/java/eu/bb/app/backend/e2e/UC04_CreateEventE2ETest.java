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
        assertThat(re.getBody().get("id")).isNotNull();
        assertThat((String)re.getBody().get("status")).isEqualTo("Draft");
    }
}

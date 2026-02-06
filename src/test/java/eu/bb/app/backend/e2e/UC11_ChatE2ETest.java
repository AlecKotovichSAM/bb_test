package eu.bb.app.backend.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.*;
import java.util.*;

class UC11_ChatE2ETest extends BaseE2ETest {
    @Test
    void post_and_list_chat_messages() {
        Map<String,Object> user = new HashMap<>();
        user.put("firstName", "Anna");
        user.put("lastName", "Muster");
        user.put("email", "anna@example.com");
        user.put("address", "Berlin");
        Long uid = ((Number) POST("/api/users", user, Map.class).getBody().get("id")).longValue();
        
        Map<String,Object> child = new HashMap<>();
        child.put("firstName", "Levi");
        child.put("birthday", "2018-06-10");
        child.put("gender", "male");
        child.put("avatar", null);
        Long cid = ((Number) POST("/api/users/"+uid+"/children", child, Map.class).getBody().get("id")).longValue();
        
        Map<String,Object> ev = new HashMap<>();
        ev.put("hostId", uid);
        ev.put("childId", cid);
        ev.put("datetime", "2026-03-15T15:00:00");
        ev.put("locationType", "manual");
        ev.put("location", "Berlin");
        ev.put("status", "Planned");
        Long eventId = ((Number) POST("/api/events", ev, Map.class).getBody().get("id")).longValue();

        Map<String,Object> m = new HashMap<>();
        m.put("userId", uid);
        m.put("message","Hallo zusammen!");
        ResponseEntity<Map> r1 = POST("/api/events/"+eventId+"/chat", m, Map.class);
        assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<List> rl = GET("/api/events/"+eventId+"/chat", List.class);
        assertThat(rl.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rl.getBody()).isNotEmpty();
    }
}

package eu.bb.app.backend.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.*;
import java.util.*;

class UC08_SendInvitationsE2ETest extends BaseE2ETest {
    @Test
    void add_guest_and_generate_token() {
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
        ev.put("status", "Draft");
        Long eventId = ((Number) POST("/api/events", ev, Map.class).getBody().get("id")).longValue();

        Map<String,Object> guest = new HashMap<>();
        guest.put("guestName", "Sophie");
        ResponseEntity<Map> rg = POST("/api/events/"+eventId+"/guests", guest, Map.class);
        assertThat(rg.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long guestId = ((Number) rg.getBody().get("id")).longValue();

        ResponseEntity<Map> rt = POST("/api/events/"+eventId+"/guests/"+guestId+"/token", new HashMap<>(), Map.class);
        assertThat(rt.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rt.getBody().get("token")).isNotNull();

        String token = (String) rt.getBody().get("token");
        ResponseEntity<Map> ro = GET("/api/invite/"+token, Map.class);
        assertThat(ro.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number)ro.getBody().get("id")).longValue()).isEqualTo(guestId);
    }
}

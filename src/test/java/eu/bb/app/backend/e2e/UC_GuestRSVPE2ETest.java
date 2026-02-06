package eu.bb.app.backend.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.*;
import java.util.*;

class UC_GuestRSVPE2ETest extends BaseE2ETest {
    @Test
    void rsvp_via_token() {
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
        guest.put("guestName", "Laura");
        Long guestId = ((Number) POST("/api/events/"+eventId+"/guests", guest, Map.class).getBody().get("id")).longValue();
        String token = (String) POST("/api/events/"+eventId+"/guests/"+guestId+"/token", new HashMap<>(), Map.class).getBody().get("token");

        ResponseEntity<Map> rv = POST("/api/invite/"+token+"/rsvp?status=accepted", new HashMap<>(), Map.class);
        assertThat(rv.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((String)rv.getBody().get("rsvpStatus")).isEqualTo("accepted");
    }
}

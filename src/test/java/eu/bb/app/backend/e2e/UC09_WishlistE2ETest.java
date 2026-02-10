package eu.bb.app.backend.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.*;
import java.util.*;

class UC09_WishlistE2ETest extends BaseE2ETest {
    @Test
    void manage_gifts() {
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
        Map<String,Object> eventResponse = (Map<String,Object>) POST("/api/events", ev, Map.class).getBody();
        Map<String,Object> event = (Map<String,Object>) eventResponse.get("event");
        Long eventId = ((Number) event.get("id")).longValue();

        Map<String,Object> g = new HashMap<>();
        g.put("title","LEGO Set");
        g.put("description","Car");
        g.put("price",59.99);
        ResponseEntity<Map> rg = POST("/api/events/"+eventId+"/gifts", g, Map.class);
        assertThat(rg.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long giftId = ((Number) rg.getBody().get("id")).longValue();
        
        // Проверяем, что подарок имеет поле categories
        Map<String,Object> giftResponse = (Map<String,Object>) rg.getBody();
        assertThat(giftResponse.get("categories")).isNotNull();

        ResponseEntity<List> list = GET("/api/events/"+eventId+"/gifts", List.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody()).hasSize(1);
        
        // Проверяем, что в списке подарков есть категории
        Map<String,Object> giftFromList = (Map<String,Object>) list.getBody().get(0);
        assertThat(giftFromList.get("categories")).isNotNull();

        Map<String,Object> upd = new HashMap<>();
        upd.put("id", giftId);
        upd.put("title","LEGO Set X");
        upd.put("description","Car");
        upd.put("price",59.99);
        ResponseEntity<Map> ru = PUT("/api/gifts/"+giftId, upd, Map.class);
        assertThat(ru.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}

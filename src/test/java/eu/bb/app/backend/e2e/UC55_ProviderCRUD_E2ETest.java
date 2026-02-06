package eu.bb.app.backend.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.*;
import java.util.*;

class UC55_ProviderCRUD_E2ETest extends BaseE2ETest {
    @Test
    void provider_crud() {
        Map<String,Object> p = new HashMap<>();
        p.put("companyName","Klettermax Indoor");
        p.put("website","https://klettermax.de");
        p.put("email","info@klettermax.de");
        p.put("phone","030-123456");
        p.put("address","Berlin");
        ResponseEntity<Map> rp = POST("/api/providers", p, Map.class);
        assertThat(rp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long id = ((Number)rp.getBody().get("id")).longValue();

        ResponseEntity<List> all = GET("/api/providers", List.class);
        assertThat(all.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(all.getBody()).isNotEmpty();

        DELETE("/api/providers/"+id);
    }
}

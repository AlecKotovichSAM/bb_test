package eu.bb.app.backend.e2e;

import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseE2ETest {
    @LocalServerPort protected int port;
    protected final TestRestTemplate http = new TestRestTemplate();

    protected String url(String p) {
        if (!p.startsWith("/")) {
			p = "/" + p;
		}
        return "http://localhost:"+port + p;
    }

    protected <T> ResponseEntity<T> GET(String path, Class<T> type) {
        return http.getForEntity(url(path), type);
    }

    protected <T> ResponseEntity<T> POST(String path, Object body, Class<T> type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return http.postForEntity(url(path), entity, type);
    }

    protected <T> ResponseEntity<T> PUT(String path, Object body, Class<T> type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return http.exchange(url(path), HttpMethod.PUT, entity, type);
    }

    protected void DELETE(String path) {
        http.delete(url(path));
    }
}

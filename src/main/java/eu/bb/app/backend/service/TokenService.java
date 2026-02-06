package eu.bb.app.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class TokenService {
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    public String newToken() {
        String token = UUID.randomUUID().toString();
        log.debug("Generated new token: {}", token);
        return token;
    }
}

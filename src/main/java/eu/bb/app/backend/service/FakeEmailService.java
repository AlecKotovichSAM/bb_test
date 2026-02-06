package eu.bb.app.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FakeEmailService {
    private static final Logger log = LoggerFactory.getLogger(FakeEmailService.class);

    public void send(String to, String subject, String body) {
        log.info("FAKE EMAIL -> TO: {} SUBJECT: {} BODY: {}", to, subject, body);
    }
}

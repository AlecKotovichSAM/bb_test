package eu.bb.app.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {
    private static final Logger log = LoggerFactory.getLogger(BackendApplication.class);

    public static void main(String[] args) {
        log.info("Starting BB Backend Application...");
        SpringApplication.run(BackendApplication.class, args);
        log.info("BB Backend Application started successfully");
    }
}

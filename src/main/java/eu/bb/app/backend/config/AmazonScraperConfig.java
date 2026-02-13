package eu.bb.app.backend.config;

import eu.bb.app.backend.service.AmazonScraper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация для AmazonScraper bean.
 */
@Configuration
public class AmazonScraperConfig {
    
    @Bean
    public AmazonScraper amazonScraper() {
        return new AmazonScraper();
    }
}

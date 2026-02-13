package eu.bb.app.backend.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Тесты для AmazonPageParser.
 * 
 * ВАЖНО: Эти тесты требуют реального доступа к интернету, так как они загружают реальные страницы Amazon через Selenium.
 */
class AmazonPageParserTest {
    
    private final AmazonPageParser parser = new AmazonPageParser();
    
    @Test
    void testParseExample1() throws IOException {
        String url = "https://www.amazon.de/-/en/720%C2%B0Dgree-uberBottle-University-Lightweight-Shockproof/dp/B07ZKLYVHQ?th=1";
        AmazonPageParser.ParseResult result = parser.parse(url);
        
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals("B07ZKLYVHQ", result.getAsin());
        assertNotNull(result.getTitle());
        assertNotNull(result.getImageUrl());
        // Категория может быть null или не null в зависимости от структуры title
        // Проверяем только что метод getCategory() работает
        assertNotNull(result.getCategory()); // или может быть null, но проверим что метод существует
    }
    
    @Test
    void testParseExample2() throws IOException {
        String url = "https://www.amazon.de/PUMA-Anzarun-Unisex-Trainers-White/dp/B07S9STQ7Z/ref=lp_2145731031_1_1?pf_rd_p=b7b797ed-078a-4a5b-91a4-303c74775994&pf_rd_r=Z5QYTS341Z3Q06GAV5XC&th=1&psc=1";
        AmazonPageParser.ParseResult result = parser.parse(url);
        
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals("B07S9STQ7Z", result.getAsin());
        assertNotNull(result.getTitle());
        assertNotNull(result.getImageUrl());
        // Категория может быть null или не null в зависимости от структуры title
        // Проверяем только что метод getCategory() работает
        assertNotNull(result.getCategory()); // или может быть null, но проверим что метод существует
    }
    
    @Test
    void testParseNullUrl() throws IOException {
        AmazonPageParser.ParseResult result = parser.parse(null);
        assertNotNull(result);
        assertFalse(result.isValid());
    }
    
    @Test
    void testParseEmptyUrl() throws IOException {
        AmazonPageParser.ParseResult result = parser.parse("");
        assertNotNull(result);
        assertFalse(result.isValid());
    }
    
    @Test
    void testParseResultToString() {
        AmazonPageParser.ParseResult result = new AmazonPageParser.ParseResult(
                "B07S9STQ7Z", 
                "PUMA Sneaker", 
                new BigDecimal("29.99"), 
                "€",
                "https://example.com/image.jpg",
                "Description",
                "In stock",
                "Sports & Outdoors",
                true);
        
        String str = result.toString();
        assertNotNull(str);
        assertTrue(str.contains("B07S9STQ7Z"));
        assertTrue(str.contains("PUMA"));
        assertTrue(str.contains("Sports & Outdoors"));
    }
    
    @Test
    void testParseResultGetters() {
        AmazonPageParser.ParseResult result = new AmazonPageParser.ParseResult(
                "B07S9STQ7Z", 
                "Test Product", 
                new BigDecimal("19.99"), 
                "€",
                "https://example.com/image.jpg",
                "Test description",
                "Available",
                "Beauty",
                true);
        
        assertEquals("B07S9STQ7Z", result.getAsin());
        assertEquals("Test Product", result.getTitle());
        assertEquals(new BigDecimal("19.99"), result.getPrice());
        assertEquals("€", result.getCurrency());
        assertEquals("https://example.com/image.jpg", result.getImageUrl());
        assertEquals("Test description", result.getDescription());
        assertEquals("Available", result.getAvailability());
        assertEquals("Beauty", result.getCategory());
        assertTrue(result.isValid());
    }
}

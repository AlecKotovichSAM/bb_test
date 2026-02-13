package eu.bb.app.backend.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Парсер для извлечения ASIN из Amazon URL.
 */
public class AmazonUrlParser {
    
    // Паттерны для поиска ASIN в URL
    private static final Pattern ASIN_PATTERN_DP = Pattern.compile("/dp/([A-Z0-9]{10})");
    private static final Pattern ASIN_PATTERN_PRODUCT = Pattern.compile("/gp/product/([A-Z0-9]{10})");
    private static final Pattern ASIN_PATTERN_PRODUCT_SLASH = Pattern.compile("/product/([A-Z0-9]{10})");
    
    /**
     * Извлекает ASIN из Amazon URL.
     * Поддерживает форматы: /dp/ASIN, /gp/product/ASIN, /product/ASIN
     * 
     * @param url Amazon URL
     * @return ASIN или null, если не найден
     */
    public static String extractAsin(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        // Попытка найти ASIN в формате /dp/ASIN
        Matcher matcher = ASIN_PATTERN_DP.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Попытка найти ASIN в формате /gp/product/ASIN
        matcher = ASIN_PATTERN_PRODUCT.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Попытка найти ASIN в формате /product/ASIN
        matcher = ASIN_PATTERN_PRODUCT_SLASH.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
}

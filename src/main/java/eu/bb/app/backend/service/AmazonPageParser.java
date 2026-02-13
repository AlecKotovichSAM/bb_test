package eu.bb.app.backend.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Парсер для извлечения информации о продукте со страницы Amazon.
 * Использует Selenium WebDriver для загрузки страницы (как в AmazonScraper) и парсит HTML через Jsoup.
 * WebDriver инициализируется при создании бина для оптимизации производительности.
 */
@Service
public class AmazonPageParser {
    
    private static final Logger log = LoggerFactory.getLogger(AmazonPageParser.class);
    
    // Селекторы для цены — Amazon часто меняет блоки
    private static final List<String> PRICE_SELECTORS = Arrays.asList(
            "#corePrice_feature_div .a-price .a-offscreen",
            "#tp_price_block_total_price_ww .a-price .a-offscreen",
            "#priceblock_ourprice",
            "#priceblock_dealprice",
            "#price_inside_buybox"
    );
    
    private volatile ChromeOptions chromeOptions;
    private volatile WebDriver driver;
    private volatile boolean initialized = false;
    
    @PostConstruct
    public void init() {
        initializeWebDriver();
    }
    
    /**
     * Инициализирует WebDriverManager, ChromeOptions и WebDriver.
     * Вызывается один раз при первом использовании или через @PostConstruct.
     */
    private synchronized void initializeWebDriver() {
        if (initialized) {
            return;
        }
        
        log.info("Initializing AmazonPageParser - setting up WebDriverManager");
        // Инициализируем WebDriverManager один раз при создании бина
        WebDriverManager.chromedriver().setup();
        
        // Создаем ChromeOptions один раз с оптимизациями для скорости
        chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless=new");
        chromeOptions.addArguments("--disable-blink-features=AutomationControlled");
        chromeOptions.addArguments("--window-size=1920,1080");
        chromeOptions.addArguments("--lang=en-US");
        // Оптимизации для ускорения загрузки
        chromeOptions.addArguments("--disable-extensions");
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--disable-gpu");
        // НЕ отключаем изображения и JavaScript - они нужны для корректной работы
        
        // Создаем WebDriver один раз для переиспользования
        driver = new ChromeDriver(chromeOptions);
        
        initialized = true;
        log.info("AmazonPageParser initialized successfully with reusable WebDriver");
    }
    
    @PreDestroy
    public void cleanup() {
        if (driver != null) {
            log.info("Closing WebDriver");
            driver.quit();
            driver = null;
        }
    }
    
    /**
     * Результат парсинга страницы Amazon.
     */
    public static class ParseResult {
        private final String asin;
        private final String title;
        private final BigDecimal price;
        private final String currency;
        private final String imageUrl;
        private final String description;
        private final String availability;
        private final String category;
        private final boolean isValid;
        
        public ParseResult(String asin, String title, BigDecimal price, String currency, 
                          String imageUrl, String description, String availability, String category, boolean isValid) {
            this.asin = asin;
            this.title = title;
            this.price = price;
            this.currency = currency;
            this.imageUrl = imageUrl;
            this.description = description;
            this.availability = availability;
            this.category = category;
            this.isValid = isValid;
        }
        
        public String getAsin() {
            return asin;
        }
        
        public String getTitle() {
            return title;
        }
        
        public BigDecimal getPrice() {
            return price;
        }
        
        public String getCurrency() {
            return currency;
        }
        
        public String getImageUrl() {
            return imageUrl;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getAvailability() {
            return availability;
        }
        
        public String getCategory() {
            return category;
        }
        
        public boolean isValid() {
            return isValid;
        }
        
        @Override
        public String toString() {
            return String.format("ParseResult{asin='%s', title='%s', price=%s %s, imageUrl='%s', category='%s', isValid=%s}",
                    asin, title, price, currency, imageUrl, category, isValid);
        }
    }
    
    /**
     * Парсит страницу Amazon по URL и извлекает информацию о продукте.
     * 
     * @param url URL страницы Amazon
     * @return ParseResult с извлеченными данными
     * @throws IOException если не удалось загрузить страницу
     */
    public ParseResult parse(String url) throws IOException {
        if (url == null || url.trim().isEmpty()) {
            return new ParseResult(null, null, null, null, null, null, null, null, false);
        }
        
        log.debug("Parsing Amazon page: {}", url);
        
        long startTime = System.currentTimeMillis();
        
        // Извлекаем ASIN из URL
        long time1 = System.currentTimeMillis();
        String asin = AmazonUrlParser.extractAsin(url);
        log.debug("Extracted ASIN in {} ms", System.currentTimeMillis() - time1);
        
        // Загружаем HTML страницу через Selenium
        long time2 = System.currentTimeMillis();
        String html = loadPageWithSelenium(url);
        long seleniumTime = System.currentTimeMillis() - time2;
        log.info("Selenium page load took {} ms", seleniumTime);
        
        long time3 = System.currentTimeMillis();
        Document doc = Jsoup.parse(html);
        log.debug("Jsoup parsing took {} ms", System.currentTimeMillis() - time3);
        
        // Извлекаем название продукта
        long time4 = System.currentTimeMillis();
        String title = extractTitle(doc);
        log.debug("Title extraction took {} ms", System.currentTimeMillis() - time4);
        
        // Извлекаем цену
        long time5 = System.currentTimeMillis();
        String priceText = extractPriceText(doc);
        BigDecimal price = parsePrice(priceText);
        String currency = extractCurrency(doc, priceText);
        log.debug("Price extraction took {} ms, priceText={}, price={}, currency={}", 
                System.currentTimeMillis() - time5, priceText, price, currency);
        
        // Извлекаем изображение
        long time6 = System.currentTimeMillis();
        String imageUrl = extractImageUrl(doc);
        log.debug("Image extraction took {} ms", System.currentTimeMillis() - time6);
        
        // Извлекаем описание
        long time7 = System.currentTimeMillis();
        String description = extractDescription(doc);
        log.debug("Description extraction took {} ms", System.currentTimeMillis() - time7);
        
        // Извлекаем доступность
        long time8 = System.currentTimeMillis();
        String availability = extractAvailability(doc);
        log.debug("Availability extraction took {} ms", System.currentTimeMillis() - time8);
        
        // Извлекаем категорию
        long time9 = System.currentTimeMillis();
        String category = extractCategory(doc);
        log.debug("Category extraction took {} ms, category={}", System.currentTimeMillis() - time9, category);
        
        // Результат валиден, если найдено хотя бы название или ASIN
        boolean isValid = (asin != null && !asin.isEmpty()) || (title != null && !title.isEmpty());
        
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Total parsing time: {} ms (Selenium: {} ms, Parsing: {} ms)", 
                totalTime, seleniumTime, totalTime - seleniumTime);
        log.debug("Parsed result: {}", new ParseResult(asin, title, price, currency, imageUrl, description, availability, category, isValid));
        
        return new ParseResult(asin, title, price, currency, imageUrl, description, availability, category, isValid);
    }
    
    /**
     * Загружает страницу через Selenium WebDriver.
     * Использует переиспользуемый WebDriver для оптимизации производительности.
     */
    private String loadPageWithSelenium(String url) {
        long startTime = System.currentTimeMillis();
        
        // Инициализируем WebDriver, если еще не инициализирован (для тестов без Spring контекста)
        if (!initialized) {
            long initStart = System.currentTimeMillis();
            initializeWebDriver();
            log.debug("WebDriver initialization took {} ms", System.currentTimeMillis() - initStart);
        }
        
        try {
            long getStart = System.currentTimeMillis();
            driver.get(url);
            log.debug("driver.get() took {} ms", System.currentTimeMillis() - getStart);
            
            // Используем WebDriverWait вместо фиксированных задержек
            // Уменьшаем время ожидания до 3 секунд для ускорения
            long waitStart = System.currentTimeMillis();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
            try {
                // Ждем появления productTitle или других ключевых элементов
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("productTitle")));
                log.debug("WebDriverWait for productTitle took {} ms", System.currentTimeMillis() - waitStart);
            } catch (Exception e) {
                // Если не дождались, делаем минимальную задержку
                log.debug("WebDriverWait timeout, using minimal fallback sleep");
                Thread.sleep(500);
            }
            
            // Проверяем, загрузился ли контент
            long pageSourceStart = System.currentTimeMillis();
            String page = driver.getPageSource();
            log.debug("getPageSource() took {} ms, page size: {} bytes", 
                    System.currentTimeMillis() - pageSourceStart, page.length());
            
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("Selenium page load total time: {} ms", totalTime);
            
            return page;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to load page", e);
        }
        // НЕ закрываем driver - он переиспользуется
    }
    
    /**
     * Извлекает название продукта.
     */
    private String extractTitle(Document doc) {
        Element titleElement = doc.selectFirst("#productTitle");
        if (titleElement != null) {
            String title = titleElement.text();
            if (title != null && !title.trim().isEmpty()) {
                return title.trim();
            }
        }
        return null;
    }
    
    /**
     * Извлекает текст цены.
     * Ищет формат €XX.YY в тексте страницы.
     */
    private String extractPriceText(Document doc) {
        // Сначала пробуем селекторы из AmazonScraper
        for (String selector : PRICE_SELECTORS) {
            Element element = doc.selectFirst(selector);
            if (element != null) {
                String text = element.text();
                if (text != null && !text.trim().isEmpty()) {
                    // Проверяем, содержит ли текст формат €XX.YY
                    if (text.matches(".*€\\s*\\d+[.,]\\d+.*")) {
                        log.debug("Found price with selector '{}': {}", selector, text);
                        return text.trim();
                    }
                }
            }
        }
        
        // Ищем формат €XX.YY во всем тексте страницы
        String pageText = doc.text();
        if (pageText != null && !pageText.isEmpty()) {
            // Паттерн для поиска цены в формате €XX.YY или € XX.YY
            Pattern pricePattern = Pattern.compile("€\\s*\\d+[.,]\\d+");
            Matcher matcher = pricePattern.matcher(pageText);
            if (matcher.find()) {
                String priceText = matcher.group();
                log.debug("Found price in page text: {}", priceText);
                return priceText.trim();
            }
        }
        
        // Ищем в HTML элементах, содержащих цену
        Elements priceElements = doc.select("[class*='price'], [id*='price'], .a-price, span.a-price");
        for (Element el : priceElements) {
            String text = el.text();
            if (text != null && text.matches(".*€\\s*\\d+[.,]\\d+.*")) {
                log.debug("Found price in price element: {}", text);
                return text.trim();
            }
        }
        
        log.warn("Could not extract price from page");
        return null;
    }
    
    /**
     * Парсит строку с ценой в BigDecimal.
     * Ожидает формат €XX.YY или EUR XX.YY
     */
    private BigDecimal parsePrice(String priceText) {
        if (priceText == null || priceText.trim().isEmpty()) {
            return null;
        }
        
        log.debug("Parsing price text: {}", priceText);
        
        // Ищем паттерн €XX.YY или EUR XX.YY
        Pattern pricePattern = Pattern.compile("(?:€|EUR)\\s*(\\d+[.,]\\d+)");
        Matcher matcher = pricePattern.matcher(priceText);
        if (matcher.find()) {
            String priceValue = matcher.group(1);
            // Заменяем запятую на точку для парсинга
            priceValue = priceValue.replace(',', '.');
            
            try {
                BigDecimal result = new BigDecimal(priceValue);
                log.debug("Parsed price: {} from text: {}", result, priceText);
                return result;
            } catch (NumberFormatException e) {
                log.warn("Failed to parse price value: {} from text: {}", priceValue, priceText, e);
            }
        }
        
        // Fallback: удаляем все символы кроме цифр, точки и запятой
        String cleaned = priceText.replaceAll("[^0-9.,]", "").trim();
        
        if (cleaned.isEmpty()) {
            log.warn("Price text contains no digits: {}", priceText);
            return null;
        }
        
        // Заменяем запятую на точку для парсинга
        cleaned = cleaned.replace(',', '.');
        
        // Удаляем все точки кроме последней (для тысяч)
        if (cleaned.contains(".")) {
            int dotCount = cleaned.length() - cleaned.replace(".", "").length();
            if (dotCount > 1) {
                // Множественные точки - это разделители тысяч, оставляем только последнюю
                int lastDotIndex = cleaned.lastIndexOf('.');
                cleaned = cleaned.substring(0, lastDotIndex).replace(".", "") + cleaned.substring(lastDotIndex);
            }
        }
        
        try {
            BigDecimal result = new BigDecimal(cleaned);
            log.debug("Parsed price (fallback): {} from text: {}", result, priceText);
            return result;
        } catch (NumberFormatException e) {
            log.warn("Failed to parse price: {} (cleaned: {})", priceText, cleaned, e);
            return null;
        }
    }
    
    /**
     * Извлекает валюту.
     * Ищет либо € либо EUR в тексте страницы или цене.
     */
    private String extractCurrency(Document doc, String priceText) {
        // Сначала проверяем текст цены
        if (priceText != null && !priceText.isEmpty()) {
            if (priceText.contains("€")) {
                log.debug("Found currency € in price text");
                return "€";
            } else if (priceText.contains("EUR")) {
                log.debug("Found currency EUR in price text");
                return "€";
            }
        }
        
        // Ищем € или EUR в тексте страницы
        String pageText = doc.text();
        if (pageText != null && !pageText.isEmpty()) {
            if (pageText.contains("€")) {
                log.debug("Found currency € in page text");
                return "€";
            } else if (pageText.contains("EUR")) {
                log.debug("Found currency EUR in page text");
                return "€";
            }
        }
        
        // Пытаемся найти валюту в селекторах цены
        String[] currencySelectors = {
            "span.a-price-symbol",
            "span.a-price .a-price-symbol",
            ".a-price-symbol",
            "#corePriceDisplay_feature_div .a-price-symbol"
        };
        
        for (String selector : currencySelectors) {
            Element priceElement = doc.selectFirst(selector);
            if (priceElement != null) {
                String symbol = priceElement.text();
                if (symbol != null && !symbol.trim().isEmpty()) {
                    String trimmed = symbol.trim();
                    if (trimmed.contains("€") || trimmed.contains("EUR")) {
                        log.debug("Found currency with selector '{}': {}", selector, trimmed);
                        return "€";
                    }
                }
            }
        }
        
        // Определяем по домену (fallback)
        String url = doc.baseUri();
        if (url != null && !url.isEmpty()) {
            if (url.contains("amazon.de")) {
                log.debug("Determined currency from domain: €");
                return "€";
            } else if (url.contains("amazon.co.uk") || url.contains("amazon.uk")) {
                log.debug("Determined currency from domain: £");
                return "£";
            } else if (url.contains("amazon.com")) {
                log.debug("Determined currency from domain: $");
                return "$";
            }
        }
        
        log.warn("Could not extract currency from page");
        return null;
    }
    
    /**
     * Извлекает URL изображения продукта.
     */
    private String extractImageUrl(Document doc) {
        // Сначала пытаемся обычный src
        Element imageElement = doc.selectFirst("#landingImage, #imgTagWrapperId img");
        if (imageElement != null) {
            String imageUrl = imageElement.attr("src");
            if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.contains("placeholder")) {
                return imageUrl;
            }
        }
        
        // Пытаемся извлечь из data-a-dynamic-image
        Element landingImage = doc.selectFirst("#landingImage");
        if (landingImage != null) {
            String dynamicAttr = landingImage.attr("data-a-dynamic-image");
            if (dynamicAttr != null && !dynamicAttr.isEmpty()) {
                String url = extractFirstUrlFromDynamicImage(dynamicAttr);
                if (url != null && !url.isEmpty()) {
                    return url;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Извлекает первый URL из data-a-dynamic-image JSON.
     */
    private String extractFirstUrlFromDynamicImage(String dynamicAttr) {
        if (dynamicAttr == null || dynamicAttr.trim().isEmpty()) {
            return null;
        }
        
        Pattern p = Pattern.compile("\"(https?://[^\"]+?)\"\\s*:\\s*\\[\\d+\\s*,\\s*\\d+\\]");
        Matcher m = p.matcher(dynamicAttr);
        if (m.find()) {
            return m.group(1);
        }
        
        // Fallback: более простой URL
        Pattern p2 = Pattern.compile("https?://[^\\s,}]+");
        Matcher m2 = p2.matcher(dynamicAttr);
        return m2.find() ? m2.group() : null;
    }
    
    /**
     * Извлекает описание продукта.
     */
    private String extractDescription(Document doc) {
        // Краткое описание (буллеты)
        Elements bullets = doc.select("#feature-bullets ul li span");
        String shortDescription = String.join("\n", bullets.eachText());
        
        // Подробное описание
        String longDescription = doc.select("#productDescription").text();
        
        if (longDescription != null && !longDescription.trim().isEmpty()) {
            return shortDescription + "\n\n" + longDescription;
        }
        
        return shortDescription.isEmpty() ? null : shortDescription;
    }
    
    /**
     * Извлекает информацию о доступности продукта.
     */
    private String extractAvailability(Document doc) {
        Element availabilityElement = doc.selectFirst("#availability span, #availability");
        if (availabilityElement != null) {
            String availability = availabilityElement.text();
            if (availability != null && !availability.trim().isEmpty()) {
                return availability.trim();
            }
        }
        return null;
    }
    
    /**
     * Извлекает категорию продукта из тега title.
     * Формат: Amazon.de: Beauty</title> - извлекает "Beauty"
     */
    private String extractCategory(Document doc) {
        Element titleElement = doc.selectFirst("title");
        if (titleElement == null) {
            return null;
        }
        
        String titleText = titleElement.text();
        if (titleText == null || titleText.trim().isEmpty()) {
            return null;
        }
        
        // Паттерн для поиска категории после двоеточия: Amazon.de: Beauty или Amazon.com: Electronics
        // Ищем формат "Amazon.*: Category" или просто ": Category"
        Pattern categoryPattern = Pattern.compile("Amazon[^:]*:\\s*([^:-]+?)(?:\\s*[-:]\\s*Amazon.*)?$");
        Matcher matcher = categoryPattern.matcher(titleText);
        
        if (matcher.find()) {
            String category = matcher.group(1).trim();
            
            // Убираем суффиксы типа " - Amazon.de" или " : Amazon.de"
            category = category.replaceAll("\\s*-\\s*Amazon.*$", "");
            category = category.replaceAll("\\s*:\\s*Amazon.*$", "");
            category = category.trim();
            
            if (!category.isEmpty() && !category.equalsIgnoreCase("Amazon")) {
                log.debug("Extracted category from title '{}': {}", titleText, category);
                return category;
            }
        }
        
        // Альтернативный паттерн: ищем текст после последнего двоеточия перед " - " или " : "
        Pattern altPattern = Pattern.compile(".*?:\\s*([^:-]+?)(?:\\s*[-:]\\s*Amazon.*)?$");
        Matcher altMatcher = altPattern.matcher(titleText);
        if (altMatcher.find()) {
            String category = altMatcher.group(1).trim();
            if (!category.isEmpty() && !category.equalsIgnoreCase("Amazon")) {
                log.debug("Extracted category (alternative) from title '{}': {}", titleText, category);
                return category;
            }
        }
        
        log.debug("Could not extract category from title: {}", titleText);
        return null;
    }
}

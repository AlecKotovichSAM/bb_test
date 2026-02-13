package eu.bb.app.backend.service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import io.github.bonigarcia.wdm.WebDriverManager;

public class AmazonScraper {


	    // Селекторы для цены — Amazon часто меняет блоки
	    private static final List<String> PRICE_SELECTORS = Arrays.asList(
	            "#corePrice_feature_div .a-price .a-offscreen",   // современный блок цены
	            "#tp_price_block_total_price_ww .a-price .a-offscreen", // total price (некоторые товары)
	            "#priceblock_ourprice",                           // старый блок
	            "#priceblock_dealprice",                          // старая цена по акции
	            "#price_inside_buybox"                            // иногда встречается
	    );

	    public static void main(String[] args) throws Exception {

	        // ⚠️ Больше не указываем System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
	        WebDriverManager.chromedriver().setup();

	        ChromeOptions opts = new ChromeOptions();
	        opts.addArguments("--headless=new");
	        opts.addArguments("--disable-blink-features=AutomationControlled");
	        opts.addArguments("--window-size=1920,1080");

	        // Важно: добавим язык интерфейса, чтобы цена и текст были предсказуемыми
	        opts.addArguments("--lang=en-US");

	        WebDriver driver = new ChromeDriver(opts);

	        String url = "https://www.amazon.de/-/en/720%C2%B0Dgree-uberBottle-University-Lightweight-Shockproof/dp/B07ZKLYVHQ?th=1";

	        String html;
	        try {
	            driver.get(url);

	            // Небольшая задержка на первичную отрисовку
	            Thread.sleep(2500);

	            // Можно сделать пару простых «пинков», чтобы динамика успела догрузиться
	            // (без явных «явных ожиданий» — чтобы код остался простым)
	            String page = driver.getPageSource();
	            if (!page.contains("productTitle")) {
	                Thread.sleep(1500);
	                page = driver.getPageSource();
	            }

	            html = page;
	        } finally {
	            driver.quit();
	        }

	        Document doc = Jsoup.parse(html);

	        // Заголовок
	        String title = doc.select("#productTitle").text();

	        // Цена — пробуем список селекторов по очереди
	        String price = extractFirstTextBySelectors(doc, PRICE_SELECTORS);

	        // Картинка — сначала обычный src, затем парсим data-a-dynamic-image
	        String image = doc.select("#landingImage, #imgTagWrapperId img").attr("src");
	        if (image == null || image.isBlank()) {
	            String dyn = doc.select("#landingImage").attr("data-a-dynamic-image");
	            image = extractFirstUrlFromDynamicImage(dyn);
	        }

	        // Краткое описание (буллеты)
	        Elements bullets = doc.select("#feature-bullets ul li span");
	        String shortDescription = String.join("\n", bullets.eachText());

	        // Подробное описание (если есть отдельный блок)
	        String longDescription = doc.select("#productDescription").text();

	        System.out.println("Title: " + (title.isBlank() ? "<not found>" : title));
	        System.out.println("Price: " + (isBlank(price) ? "<not found>" : price));
	        System.out.println("Image: " + (isBlank(image) ? "<not found>" : image));
	        System.out.println("Short Description:\n" + (isBlank(shortDescription) ? "<not found>" : shortDescription));
	        if (!isBlank(longDescription)) {
	            System.out.println("\nLong Description:\n" + longDescription);
	        }
	    }

	    private static String extractFirstTextBySelectors(Document doc, List<String> selectors) {
	        for (String sel : selectors) {
	            Element el = doc.selectFirst(sel);
	            if (el != null) {
	                String text = el.text();
	                if (!isBlank(text)) {
						return text;
					}
	            }
	        }
	        return null;
	    }

	    // В data-a-dynamic-image Amazon кладёт JSON-объект вида:
	    // {"https://images-na.ssl-images-amazon.com/images/I/....jpg":[500,500], "...": [..]}
	    // Возьмём первую ссылку через простой регекс, чтобы не тянуть JSON-парсер
	    private static String extractFirstUrlFromDynamicImage(String dynamicAttr) {
	        if (isBlank(dynamicAttr)) {
				return null;
			}
	        Pattern p = Pattern.compile("\"(https?://[^\"]+?)\"\\s*:\\s*\\[\\d+\\s*,\\s*\\d+\\]");
	        Matcher m = p.matcher(dynamicAttr);
	        if (m.find()) {
	            return m.group(1);
	        }
	        // fallback: более простой URL
	        Pattern p2 = Pattern.compile("https?://[^\\s,}]+");
	        Matcher m2 = p2.matcher(dynamicAttr);
	        return m2.find() ? m2.group() : null;
	    }

	    private static boolean isBlank(String s) {
	        return s == null || s.trim().isEmpty();
	    }
	    
	}


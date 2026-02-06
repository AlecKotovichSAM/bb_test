# AGENTS.md - Руководство для AI агентов

## AutoConfigureMockMvc

### Важно: Правильный импорт

В этом проекте используется **специфичный импорт** для `AutoConfigureMockMvc`:

```java
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
```

**НЕ использовать:**
```java
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;  // НЕПРАВИЛЬНО
```

### Контекст

Проект использует Spring Boot 4.0.2, где структура пакетов для тестирования изменилась. 
Правильный пакет для `AutoConfigureMockMvc` в Spring Boot 4.x:
- `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`

### Использование в тестах

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MyControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    
    // тесты...
}
```

### Зависимости

Для работы `AutoConfigureMockMvc` требуется:
- `spring-boot-starter-test` (включает MockMvc)
- `spring-boot-starter-webmvc-test` (специфичная зависимость для Spring Boot 4.x)

---

## Другие важные заметки

### ObjectMapper в тестах

В интеграционных тестах `ObjectMapper` создается вручную с поддержкой Java 8 time types:

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
```

Причина: `ObjectMapper` не всегда доступен как bean в тестовом контексте, поэтому создается напрямую. 
Модуль `JavaTimeModule` необходим для сериализации/десериализации `LocalDateTime` и других Java 8 time types.

### Тестовая конфигурация

- Профиль: `@ActiveProfiles("test")`
- База данных: in-memory H2 (`jdbc:h2:mem:testdb`)
- Flyway: включен (миграции выполняются перед тестами)
- Hibernate DDL: `validate` (схема создается через Flyway)

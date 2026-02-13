# BB Backend API

REST API для управления событиями, гостями, подарками и общением. API предоставляет полный функционал для организации и управления событиями с поддержкой приглашений, вишлистов и чата.

## ⚠️ Notice: Прототип

**Важно:** Данный проект является **прототипом** и использует упрощенные решения для быстрой разработки и тестирования API.

### Упрощения и особенности прототипа:

- **Аутентификация:** Не требуется. Все endpoints доступны без токенов или ключей API
- **Архитектура:** Намеренно **не используется сервисный слой** - контроллеры напрямую вызывают репозитории. Это упрощает код для прототипа, но в production версии рекомендуется добавить сервисный слой для бизнес-логики
- **Язык/Фреймворк:** Используется **Java/Spring Boot** вместо Node.js
- **База данных:** Используется **H2 Database** (встроенная) вместо PostgreSQL
- **Кэширование:** Redis не используется
- **Хранилище файлов:** Используется **локальная файловая система** приложения вместо S3 или облачных хранилищ
- **Email:** Реализованы **фейковые email-ы** без интеграции с SMTP серверами
- **SSO:** Google/Apple Single Sign-On не реализован
- **Карты:** Интеграция с Maps API отсутствует

### Что реализовано:

✅ **Полный REST API** для всех use case-ов  
✅ **Схема базы данных** в формате **Flyway миграций**  
✅ **Swagger/OpenAPI документация**  
✅ **E2E тесты** для всех сценариев  
✅ **Postman коллекции** для тестирования

### Цель прототипа:

Прототип создан для демонстрации API и схемы данных. В production версии потребуется:
- Реальная аутентификация и авторизация
- Сервисный слой для бизнес-логики (сейчас контроллеры напрямую вызывают репозитории)
- Production-ready база данных (PostgreSQL)
- Интеграция с внешними сервисами (SMTP, S3, Maps API)
- Кэширование и оптимизация производительности

## Содержание

- [Технологический стек](#технологический-стек)
- [Быстрый старт](#быстрый-старт)
- [API Документация](#api-документация)
- [Основные сущности](#основные-сущности)
- [API Endpoints](#api-endpoints)
- [E2E Тесты и сценарии использования](#e2e-тесты-и-сценарии-использования)
- [Postman коллекции](#postman-коллекции)
- [Детали реализации](#детали-реализации)
- [Интеграция](#интеграция)
- [Docker](#docker)
- [Разработка](#разработка)

## Технологический стек

- **Java 25** - язык программирования
- **Spring Boot 4.0.2** - фреймворк приложения
- **Spring Data JPA** - ORM и работа с базой данных
- **H2 Database** - встроенная база данных (файловая и in-memory для тестов)
- **Flyway** - миграции базы данных
- **Springdoc OpenAPI 3** - документация API (Swagger UI)
- **SLF4J/Logback** - логирование
- **Maven** - система сборки
- **JUnit 5** - тестирование

## Быстрый старт

### Локальный запуск

1. **Требования:**
   - JDK 25
   - Maven 3.6+

2. **Сборка и запуск:**
   ```bash
   mvn clean package
   java -jar target/backend-0.0.1-SNAPSHOT.jar
   ```

3. **Доступ к приложению:**
   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui
   - API Docs (JSON): http://localhost:8080/v3/api-docs
   - H2 Console: http://localhost:8080/h2

### Docker

**Вариант 1: Скачать готовый образ с GitHub (рекомендуется)**

```bash
# Скачать и запустить образ из GitHub Container Registry
docker run -p 8080:8080 ghcr.io/<username>/<repo-name>/bb-backend:latest
```

Замените `<username>` и `<repo-name>` на ваши значения. Образ автоматически скачается, если его нет локально.

**Вариант 2: Собрать образ локально**

```bash
# Сборка образа
docker build -t bb-backend .

# Запуск контейнера
docker run -p 8080:8080 bb-backend
```

## API Документация

### Swagger UI

После запуска приложения интерактивная документация API доступна по адресу:
```
http://localhost:8080/swagger-ui
```

**Что можно делать в Swagger UI:**

1. **Просмотр всех endpoints** - все API endpoints организованы по категориям (Users, Events, Gifts, Chat и т.д.)

2. **Изучение параметров** - для каждого endpoint показаны:
   - HTTP метод (GET, POST, PUT, DELETE)
   - Описание операции
   - Параметры запроса (path, query, body)
   - Формат ответа и примеры
   - Коды ответов (200, 404, 400 и т.д.)

3. **Тестирование API** - можно выполнять запросы прямо из браузера:
   - Нажмите на endpoint для раскрытия деталей
   - Нажмите кнопку **"Try it out"**
   - Заполните параметры запроса
   - Нажмите **"Execute"** для отправки запроса
   - Просмотрите ответ сервера

4. **Примеры запросов и ответов** - для каждого endpoint показаны примеры JSON для запросов и ответов

5. **Схемы данных** - внизу страницы можно посмотреть схемы всех сущностей (User, Event, Gift и т.д.) с описанием полей

**Пример использования:**
1. Откройте http://localhost:8080/swagger-ui в браузере
2. Найдите раздел **"Users"**
3. Раскройте `POST /api/users` - создать пользователя
4. Нажмите **"Try it out"**
5. В поле **Request body** введите:
   ```json
   {
     "firstName": "Anna",
     "lastName": "Muster",
     "email": "anna@example.com",
     "address": "Berlin"
   }
   ```
6. Нажмите **"Execute"**
7. Просмотрите ответ сервера с созданным пользователем

### OpenAPI спецификация

JSON спецификация OpenAPI 3.0 доступна по адресу:
```
http://localhost:8080/v3/api-docs
```

Эта спецификация может быть использована для:
- Импорта в Postman, Insomnia и другие API клиенты
- Генерации клиентского кода
- Интеграции с другими инструментами документации
- Автоматического тестирования API

**Пример использования спецификации:**
```bash
# Скачать спецификацию
curl http://localhost:8080/v3/api-docs > api-spec.json

# Импортировать в Postman
# File → Import → Upload Files → выбрать api-spec.json
```

## Основные сущности

### User (Пользователь)
Родитель/хозяин события. Содержит базовую информацию о пользователе.

**Поля:**
- `id` - уникальный идентификатор (автогенерируется)
- `firstName` - имя
- `lastName` - фамилия
- `email` - email адрес
- `address` - адрес

**Важно:** Аватар пользователя хранится в отдельной таблице `user_avatars`. Для работы с аватаром используйте endpoints `/api/users/{id}/avatar`.

### Child (Ребенок)
Ребенок пользователя, для которого организуется событие.

**Поля:**
- `id` - уникальный идентификатор
- `userId` - ID родителя
- `firstName` - имя ребенка
- `birthday` - дата рождения (формат: `YYYY-MM-DD`)
- `gender` - пол (`male`, `female`)

**Важно:** Аватар ребенка хранится в отдельной таблице `child_avatars`. Для работы с аватаром используйте endpoints `/api/children/{id}/avatar`.

### Event (Событие)
Событие (день рождения) для ребенка.

**Поля:**
- `id` - уникальный идентификатор
- `hostId` - ID хозяина события (пользователя)
- `childId` - ID ребенка
- `datetime` - дата и время события (формат: `YYYY-MM-DDTHH:mm:ss`)
- `locationType` - тип локации (`manual`, `provider`)
- `location` - адрес/название места
- `status` - статус события (`Draft`, `Planned`, `Cancelled`)

### Guest (Гость)
Нормализованная таблица гостей. Один гость может участвовать в нескольких событиях.

**Поля:**
- `id` (guestId) - уникальный идентификатор гостя из таблицы `guests`
- `guestName` - имя гостя
- `userId` - ID зарегистрированного пользователя (если гость зарегистрирован)

### EventGuest (Гость события)
Связь между событием и гостем. Один гость может участвовать в нескольких событиях.

**Поля:**
- `id` - уникальный идентификатор записи EventGuest (связь события и гостя)
- `eventId` - ID события
- `guestId` - ID гостя из таблицы `guests` (используется для переиспользования)
- `guestName` - имя гостя (из связанной таблицы `guests`)
- `rsvpStatus` - статус ответа (`open`, `accepted`, `declined`)

**Важно:** 
- `id` - это ID записи EventGuest (связь события и гостя)
- `guestId` - это ID гостя из таблицы `guests` (используйте для переиспользования между событиями)

### Gift (Подарок)
Подарок в вишлисте события.

**Поля:**
- `id` - уникальный идентификатор
- `eventId` - ID события
- `title` - название подарка
- `description` - описание
- `url` - ссылка на подарок
- `image` - ссылка на изображение подарка
- `price` - цена (BigDecimal, формат: `10.2`)
- `status` - статус (`open`, `reserved`)
- `reservedByGuest` - ID гостя, зарезервировавшего подарок (может быть `null`)
- `categories` - массив категорий подарка (связь many-to-many с `GiftCategory`)

### GiftCategory (Категория подарка)
Категория для классификации подарков.

**Поля:**
- `id` - уникальный идентификатор
- `name` - название категории (уникальное, например: "Lego", "Sport", "Outdoor", "Bücher", "Basteln")

### GuestToken (Токен приглашения)
Уникальный токен для доступа гостя к приглашению.

**Поля:**
- `id` - уникальный идентификатор
- `guestId` - ID гостя
- `token` - уникальный токен (строка)
- `validUntil` - срок действия (30 дней с момента создания)

### ChatMessage (Сообщение чата)
Сообщение в чате события.

**Поля:**
- `id` - уникальный идентификатор
- `eventId` - ID события
- `userId` - ID пользователя, отправившего сообщение
- `message` - текст сообщения
- `createdAt` - время создания (автоматически)

## API Endpoints

### Users API (`/api/users`)

#### Получить всех пользователей
```http
GET /api/users
```

**Ответ:**
```json
[
  {
    "id": 1,
    "firstName": "Anna",
    "lastName": "Muster",
    "email": "anna@example.com",
    "address": "Berlin"
  }
]
```

#### Создать пользователя
```http
POST /api/users
Content-Type: application/json

{
  "firstName": "Anna",
  "lastName": "Muster",
  "email": "anna@example.com",
  "address": "Berlin"
}
```

**Важно:** При создании не передавайте поле `id` или передавайте `id: 0` - оно будет автоматически установлено в `null`.

#### Получить пользователя по ID
```http
GET /api/users/{id}
```

#### Обновить пользователя
```http
PUT /api/users/{id}
Content-Type: application/json

{
  "firstName": "Anna",
  "lastName": "Muster",
  "email": "anna.new@example.com",
  "address": "Berlin, Germany"
}
```

#### Удалить пользователя
```http
DELETE /api/users/{id}
```

#### Обновить аватар пользователя
```http
PUT /api/users/{id}/avatar
Content-Type: application/json

{
  "avatar": "data:image/jpeg;base64,/9j/4AAQSkZJRg..."
}
```

**Формат аватара:** data URI string (`data:image/jpeg;base64,...` или `data:image/png;base64,...`)

**Удаление аватара:** передайте пустую строку `"avatar": ""`

#### Получить аватар пользователя
```http
GET /api/users/{id}/avatar
```

Возвращает бинарные данные изображения с правильным Content-Type. Можно использовать напрямую в теге `<img src="/api/users/{id}/avatar">`.

### Children API (`/api/users/{userId}/children`)

#### Добавить ребенка пользователю
```http
POST /api/users/{userId}/children
Content-Type: application/json

{
  "firstName": "Levi",
  "birthday": "2018-06-10",
  "gender": "male",
  "avatar": "data:image/jpeg;base64,..."  // опционально
}
```

**Аватар:** Можно указать аватар при создании ребенка в поле `avatar` (формат: `data:image/jpeg;base64,...` или `data:image/png;base64,...`). Если не указан, ребенок создается без аватара.

#### Получить детей пользователя
```http
GET /api/users/{userId}/children
```

#### Получить ребенка по ID
```http
GET /api/children/{id}
```

#### Обновить ребенка
```http
PUT /api/children/{id}
Content-Type: application/json

{
  "firstName": "Levi",
  "birthday": "2018-06-10",
  "gender": "male"
}
```

**Важно:** Аватар не может быть обновлен через этот endpoint. Используйте `PUT /api/children/{id}/avatar` для обновления аватара.

#### Удалить ребенка
```http
DELETE /api/children/{id}
```

#### Обновить аватар ребенка
```http
PUT /api/children/{id}/avatar
Content-Type: application/json

{
  "avatar": "data:image/jpeg;base64,/9j/4AAQSkZJRg..."
}
```

**Формат аватара:** data URI string (`data:image/jpeg;base64,...` или `data:image/png;base64,...`)

**Удаление аватара:** передайте пустую строку `"avatar": ""`

#### Получить аватар ребенка
```http
GET /api/children/{id}/avatar
```

Возвращает бинарные данные изображения с правильным Content-Type. Можно использовать напрямую в теге `<img src="/api/children/{id}/avatar">`.

### Events API (`/api/events`)

#### Создать событие
```http
POST /api/events
Content-Type: application/json

{
  "hostId": 1,
  "childId": 1,
  "datetime": "2026-03-15T15:00:00",
  "locationType": "manual",
  "location": "Berlin, Familiencafe",
  "status": "Draft",
  "guests": [
    {
      "guestName": "Sophie",
      "children": ["Max", "Emma"]
    }
  ]
}
```

**Статусы события:**
- `Draft` - черновик (устанавливается по умолчанию, если не указан)
- `Planned` - запланировано
- `Cancelled` - отменено

**Создание события с гостями:**

Можно указать гостей при создании события. Поддерживаются два способа:

1. **Создание нового гостя:**
```json
{
  "hostId": 1,
  "childId": 1,
  "datetime": "2026-03-15T15:00:00",
  "guests": [
    {
      "guestName": "Sophie",
      "userId": 2,
      "children": ["Max", "Emma"]
    }
  ]
}
```

2. **Переиспользование существующего гостя:**
```json
{
  "hostId": 1,
  "childId": 1,
  "datetime": "2026-04-20T15:00:00",
  "guests": [
    {
      "guestId": 5
      // дети не указаны - будут автоматически скопированы из предыдущего события
    }
  ]
}
```

**Параметры в guests[]:**
- `guestId` (опционально) - ID гостя из таблицы `guests` для переиспользования
- `guestName` (обязательно, если нет `guestId`) - имя нового гостя или обновление имени существующего
- `userId` (опционально) - ID зарегистрированного пользователя
- `children` (опционально) - список имен детей

**Автокопирование детей:**
- Если `guestId` указан, а `children` не указаны или пустой массив: автоматически копируются дети из предыдущего `EventGuest` того же пользователя (`hostId`)
- Если `children` указаны: используются указанные дети (автокопирование не выполняется)
- Если предыдущий `EventGuest` не найден: дети не копируются (первое использование гостя данным пользователем)

#### Получить все события
```http
GET /api/events
```

#### Получить события пользователя
```http
GET /api/users/{userId}/events
```

#### Получить всех гостей пользователя
```http
GET /api/users/{userId}/guests
```

Возвращает уникальных гостей из всех событий пользователя с их детьми. Гости дедуплицируются по `guestId` - один гость появляется один раз, даже если участвует в нескольких событиях.

**Ответ:**
```json
[
  {
    "guest": {
      "id": 123,
      "guestId": 5,
      "guestName": "Sophie",
      "userId": 2
    },
    "children": [
      { "id": 1, "firstName": "Max" },
      { "id": 2, "firstName": "Emma" }
    ]
  }
]
```

#### Получить событие по ID
```http
GET /api/events/{id}
```

#### Обновить событие
```http
PUT /api/events/{id}
Content-Type: application/json

{
  "hostId": 1,
  "childId": 1,
  "datetime": "2026-03-15T16:00:00",
  "locationType": "manual",
  "location": "Berlin, New Location",
  "status": "Planned"
}
```

#### Отменить событие
```http
POST /api/events/{id}/cancel
```

Устанавливает статус события в `Cancelled`.

#### Удалить событие
```http
DELETE /api/events/{id}
```

### Guests API (`/api/events/{eventId}/guests`)

#### Добавить гостя к событию
```http
POST /api/events/{eventId}/guests
Content-Type: application/json
```

Поддерживает два формата:

**Создание нового гостя:**
```json
{
  "guestName": "Sophie",
  "userId": 2
}
```

**Переиспользование существующего гостя:**
```json
{
  "guestId": 5
}
```

#### Получить список гостей события
```http
GET /api/events/{eventId}/guests
```

#### Обновить информацию о госте
```http
PUT /api/events/{eventId}/guests/{guestId}
Content-Type: application/json

{
  "guestName": "Sophie",
  "rsvpStatus": "accepted"
}
```

#### Удалить гостя
```http
DELETE /api/events/{eventId}/guests/{guestId}
```

#### Создать токен приглашения для гостя
```http
POST /api/events/{eventId}/guests/{guestId}/token
```

**Ответ:**
```json
{
  "id": 1,
  "guestId": 1,
  "token": "abc123def456...",
  "validUntil": "2026-03-15T15:00:00"
}
```

Токен действителен 30 дней с момента создания.

### Invitations API (`/api/invite/{token}`)

#### Открыть приглашение по токену
```http
GET /api/invite/{token}
```

Возвращает информацию о госте по токену приглашения.

#### Ответить на приглашение (RSVP)
```http
POST /api/invite/{token}/rsvp?status=accepted
```

**Параметры:**
- `status` - статус ответа: `accepted`, `declined` или `open`

**Примеры:**
```bash
# Принять приглашение
POST /api/invite/{token}/rsvp?status=accepted

# Отклонить приглашение
POST /api/invite/{token}/rsvp?status=declined

# Сбросить ответ
POST /api/invite/{token}/rsvp?status=open
```

### Gifts API (`/api/events/{eventId}/gifts`)

#### Добавить подарок к событию
```http
POST /api/events/{eventId}/gifts
Content-Type: application/json

{
  "title": "LEGO Set",
  "description": "Car",
  "url": "https://example.com/lego-set",
  "image": "https://example.com/lego-image.jpg",
  "price": 59.99,
  "categories": [
    { "id": 1 },           // по ID существующей категории
    { "name": "Lego" }     // или по имени (создастся, если не существует)
  ]
}
```

**Категории:** Можно указать категории подарка через поле `categories` (массив объектов с полем `id` или `name`). Если категория с указанным именем не существует, она будет создана автоматически.

Статус автоматически устанавливается в `open`.

#### Получить список подарков события
```http
GET /api/events/{eventId}/gifts
```

#### Обновить подарок
```http
PUT /api/gifts/{id}
Content-Type: application/json

{
  "title": "LEGO Set X",
  "description": "Car",
  "price": 59.99,
  "categories": [
    { "id": 1 },
    { "name": "Sport" }
  ]
}
```

**Важно:** 
- При обновлении не нужно передавать `eventId`, `status` и `reservedByGuest` - они сохраняются автоматически из существующего подарка
- Если поле `categories` не указано, существующие категории сохраняются
- Если указано `categories`, категории обновляются согласно переданному списку

#### Зарезервировать подарок (для гостя по токену)
```http
POST /api/invite/{token}/gifts/{giftId}/reserve
```

Изменяет статус подарка на `reserved` и устанавливает `reservedByGuest`.

#### Отменить резервацию подарка
```http
POST /api/invite/{token}/gifts/{giftId}/cancel
```

Отменяет резервацию, если она была сделана тем же гостем. Статус меняется на `open`, `reservedByGuest` устанавливается в `null`.

#### Удалить подарок
```http
DELETE /api/gifts/{id}
```

### Gift Categories API (`/api/categories`)

#### Получить список всех категорий
```http
GET /api/categories
```

**Ответ:**
```json
[
  {
    "id": 1,
    "name": "Basteln"
  },
  {
    "id": 2,
    "name": "Bücher"
  },
  {
    "id": 3,
    "name": "Lego"
  },
  {
    "id": 4,
    "name": "Outdoor"
  },
  {
    "id": 5,
    "name": "Sport"
  }
]
```

Категории возвращаются отсортированными по имени в алфавитном порядке.

### Chat API (`/api/events/{eventId}/chat`)

#### Получить сообщения чата события
```http
GET /api/events/{eventId}/chat
```

Сообщения возвращаются отсортированными по времени создания (от старых к новым).

#### Отправить сообщение в чат
```http
POST /api/events/{eventId}/chat
Content-Type: application/json

{
  "userId": 1,
  "message": "Hallo zusammen!"
}
```

## E2E Тесты и сценарии использования

Проект содержит набор End-to-End тестов, которые демонстрируют основные сценарии использования API. Все тесты находятся в пакете `eu.bb.app.backend.e2e`.

### UC04: Создание события

**Тесты:** 
- `UC04_CreateEventE2ETest.create_event_with_user_child_location_and_status()` - базовое создание события
- `UC04_CreateEventE2ETest.create_event_reuse_guest_should_not_duplicate()` - переиспользование гостя без дублирования
- `UC04_CreateEventE2ETest.create_event_reuse_guest_with_auto_copy_children()` - автокопирование детей при переиспользовании
- `UC04_CreateEventE2ETest.create_event_reuse_guest_with_override_children()` - переопределение детей при переиспользовании

**Сценарии:**
1. Создание пользователя (родителя)
2. Добавление ребенка пользователю
3. Создание события для ребенка с указанием локации и статуса
4. Переиспользование гостя между событиями с автокопированием детей
5. Переиспользование гостя с переопределением детей

**Пример использования:**
```java
// 1. Создать пользователя
POST /api/users
{
  "firstName": "Anna",
  "lastName": "Muster",
  "email": "anna@example.com",
  "address": "Berlin"
}

// 2. Добавить ребенка
POST /api/users/{userId}/children
{
  "firstName": "Levi",
  "birthday": "2018-06-10",
  "gender": "male"
}

// 3. Создать событие с гостем
POST /api/events
{
  "hostId": {userId},
  "childId": {childId},
  "datetime": "2026-03-15T15:00:00",
  "locationType": "manual",
  "location": "Berlin, Familiencafe",
  "status": "Draft",
  "guests": [
    {
      "guestName": "Sophie",
      "children": ["Max", "Emma"]
    }
  ]
}

// 4. Переиспользовать гостя во втором событии (дети скопируются автоматически)
POST /api/events
{
  "hostId": {userId},
  "childId": {childId},
  "datetime": "2026-04-20T15:00:00",
  "guests": [
    {
      "guestId": {guestId}  // из ответа первого события
    }
  ]
}
```

### UC05: Отмена события

**Тест:** `UC05_CancelEventE2ETest.cancel_event()`

**Сценарий:**
1. Создание пользователя, ребенка и события
2. Отмена события через специальный endpoint

**Пример использования:**
```http
POST /api/events/{eventId}/cancel
```

Статус события автоматически меняется на `Cancelled`.

### UC08: Отправка приглашений

**Тест:** `UC08_SendInvitationsE2ETest.add_guest_and_generate_token()`

**Сценарий:**
1. Создание события
2. Добавление гостя к событию
3. Генерация токена приглашения для гостя
4. Открытие приглашения по токену

**Пример использования:**
```http
# 1. Добавить гостя
POST /api/events/{eventId}/guests
{
  "guestName": "Sophie"
}

# 2. Создать токен
POST /api/events/{eventId}/guests/{guestId}/token

# 3. Открыть приглашение
GET /api/invite/{token}
```

### UC09: Управление вишлистом

**Тест:** `UC09_WishlistE2ETest.manage_gifts()`

**Сценарий:**
1. Создание события
2. Добавление подарка в вишлист
3. Получение списка подарков
4. Обновление информации о подарке

**Пример использования:**
```http
# 1. Добавить подарок
POST /api/events/{eventId}/gifts
{
  "title": "LEGO Set",
  "description": "Car",
  "price": 59.99
}

# 2. Получить список подарков
GET /api/events/{eventId}/gifts

# 3. Обновить подарок
PUT /api/gifts/{giftId}
{
  "title": "LEGO Set X",
  "description": "Car",
  "price": 59.99
}
```

### UC11: Чат события

**Тест:** `UC11_ChatE2ETest.post_and_list_chat_messages()`

**Сценарий:**
1. Создание события
2. Отправка сообщения в чат
3. Получение списка сообщений

**Пример использования:**
```http
# 1. Отправить сообщение
POST /api/events/{eventId}/chat
{
  "userId": 1,
  "message": "Hallo zusammen!"
}

# 2. Получить сообщения
GET /api/events/{eventId}/chat
```

### UC_GuestRSVP: Ответ гостя на приглашение

**Тест:** `UC_GuestRSVPE2ETest.rsvp_via_token()`

**Сценарий:**
1. Создание события и гостя
2. Генерация токена приглашения
3. Ответ гостя на приглашение через токен

**Пример использования:**
```http
# Ответить на приглашение
POST /api/invite/{token}/rsvp?status=accepted
```

**Возможные статусы:**
- `accepted` - принято
- `declined` - отклонено
- `open` - без ответа

### UC55: CRUD операции с провайдерами

**Тест:** `UC55_ProviderCRUD_E2ETest`

**Сценарий:**
Полный цикл CRUD операций для провайдеров (поставщиков услуг для событий).

## Postman коллекции

Проект включает готовые Postman коллекции для тестирования API:

### Файлы

- `postman/bb_e2e_collection.json` - коллекция запросов, покрывающая все E2E сценарии
- `postman/bb_local_env.json` - переменные окружения для локального запуска

### Импорт в Postman

1. Откройте Postman
2. Нажмите **Import**
3. Выберите файл `postman/bb_e2e_collection.json`
4. Импортируйте переменные окружения из `postman/bb_local_env.json`

### Использование

1. Выберите окружение `bb_local_env`
2. Убедитесь, что переменная `baseUrl` установлена в `http://localhost:8080`
3. Запустите коллекцию или отдельные запросы

### Структура коллекции

Коллекция организована по сценариям использования:
- **Env** - базовые проверки (health check)
- **UC04 Create Event** - создание события
- **UC05 Cancel Event** - отмена события
- **UC08 Send Invitations** - отправка приглашений
- **UC09 Wishlist** - управление вишлистом
- **UC11 Chat** - чат события
- **UC55 Provider CRUD** - операции с провайдерами

## Детали реализации

### Обработка ID при создании сущностей

При создании новых сущностей через POST запросы, если в теле запроса передается `id: 0`, оно автоматически устанавливается в `null`. Это предотвращает проблемы с оптимистичной блокировкой (OptimisticLockingFailureException) и гарантирует создание новой записи вместо обновления существующей.

**Реализация:**
```java
if (u.getId() != null && u.getId() == 0) {
    u.setId(null);
}
```

### Обновление подарков

При обновлении подарка через `PUT /api/gifts/{id}` система автоматически сохраняет следующие поля из существующего подарка, если они не переданы в запросе:
- `eventId` - всегда сохраняется из существующего подарка
- `status` - сохраняется, если не передан
- `reservedByGuest` - сохраняется, если не передан

Это гарантирует целостность данных и предотвращает случайное изменение связей.

### Токены приглашений

- Токены генерируются автоматически при создании через `TokenService`
- Срок действия: 30 дней с момента создания
- Токены используются для:
  - Открытия приглашения (`GET /api/invite/{token}`)
  - Ответа на приглашение (`POST /api/invite/{token}/rsvp`)
  - Резервации подарков (`POST /api/invite/{token}/gifts/{giftId}/reserve`)

### База данных

- **H2 Database** - используется как для разработки, так и для тестов
- **Flyway** - управление миграциями схемы БД
- Миграции находятся в `src/main/resources/db/migration/`
- Файл БД по умолчанию: `./data/bb.mv.db`

### Логирование

- Используется **SLF4J** с **Logback**
- Конфигурация: `src/main/resources/logback-spring.xml`
- Уровни логирования:
  - `DEBUG` - для пакета `eu.bb.app.backend`
  - `INFO` - для Spring Web
  - `WARN` - для Hibernate

### Тестирование

#### Интеграционные тесты
- Используют in-memory H2 базу данных
- Профиль: `test` (файл `application-test.properties`)
- Автоматическая очистка данных перед каждым тестом через `@BeforeEach`
- Используют `MockMvc` для тестирования контроллеров

#### E2E тесты
- Используют реальный HTTP сервер (`RANDOM_PORT`)
- Используют `TestRestTemplate` для HTTP запросов
- Профиль: `test`
- Демонстрируют полные сценарии использования API

## Интеграция

### Аутентификация

**Текущая версия API не требует аутентификации.** Все endpoints доступны без токенов или ключей API.

### Формат данных

- **Content-Type:** `application/json` для всех POST/PUT запросов
- **Даты:** формат ISO 8601 (`YYYY-MM-DD` для дат, `YYYY-MM-DDTHH:mm:ss` для дат со временем)
- **Цены:** формат `BigDecimal` с двумя знаками после запятой (например, `59.99`)

### Коды ответов

- `200 OK` - успешный запрос
- `400 Bad Request` - некорректные данные запроса
- `404 Not Found` - ресурс не найден
- `500 Internal Server Error` - внутренняя ошибка сервера

### Обработка ошибок

При ошибках API возвращает стандартные HTTP коды статуса. Для некоторых операций (например, получение несуществующего пользователя) возвращается `404 Not Found` с сообщением об ошибке.

### Rate Limiting

В текущей версии rate limiting не реализован.

### CORS

По умолчанию CORS не настроен. Для работы с фронтендом из другого домена необходимо настроить CORS в конфигурации Spring.

## Docker

### Сборка образа

```bash
docker build -t bb-backend .
```

### Запуск контейнера

```bash
docker run -p 8080:8080 bb-backend
```

### Переменные окружения

Для настройки приложения через переменные окружения можно использовать стандартные Spring Boot properties:

```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:h2:file:/app/data/bb \
  -e SERVER_PORT=8080 \
  bb-backend
```

### Монтирование данных

Для сохранения данных БД между перезапусками:

```bash
docker run -p 8080:8080 \
  -v $(pwd)/data:/app/data \
  bb-backend
```

### GitHub Container Registry

При коммите в репозиторий автоматически собирается Docker образ и публикуется в GitHub Container Registry (ghcr.io). Вы можете скачать готовый образ с GitHub и запустить его без необходимости собирать локально.

**Скачать и запустить образ из GitHub:**

1. **Скачать образ:**
   ```bash
   docker pull ghcr.io/<username>/<repo-name>/bb-backend:latest
   ```
   
   Замените `<username>` и `<repo-name>` на ваши значения (например, `ghcr.io/myuser/bb_test/bb-backend:latest`).

2. **Запустить контейнер:**
   ```bash
   docker run -p 8080:8080 ghcr.io/<username>/<repo-name>/bb-backend:latest
   ```

3. **Или скачать и запустить одной командой:**
   ```bash
   docker run -p 8080:8080 ghcr.io/<username>/<repo-name>/bb-backend:latest
   ```
   Docker автоматически скачает образ, если его нет локально.

**Где найти адрес образа:**
- Перейдите в репозиторий на GitHub
- Откройте раздел **Packages** (справа от кода)
- Найдите пакет `bb-backend`
- Скопируйте команду `docker pull` из инструкций пакета

**Пример полного использования:**
```bash
# Скачать образ
docker pull ghcr.io/myuser/bb_test/bb-backend:latest

# Запустить с сохранением данных
docker run -d -p 8080:8080 \
  -v $(pwd)/data:/app/data \
  --name bb-backend \
  ghcr.io/myuser/bb_test/bb-backend:latest

# Просмотр логов
docker logs bb-backend

# Остановка
docker stop bb-backend
```

## Разработка

### Структура проекта

```
src/
├── main/
│   ├── java/eu/bb/app/backend/
│   │   ├── controller/     # REST контроллеры
│   │   ├── entity/         # JPA сущности
│   │   ├── repository/     # Spring Data репозитории
│   │   ├── service/        # Бизнес-логика
│   │   └── config/          # Конфигурация (OpenAPI)
│   └── resources/
│       ├── db/migration/   # Flyway миграции
│       ├── application.properties
│       └── logback-spring.xml
└── test/
    ├── java/eu/bb/app/backend/
    │   ├── controller/      # Интеграционные тесты
    │   └── e2e/             # E2E тесты
    └── resources/
        └── application-test.properties
```

### Запуск тестов

```bash
# Все тесты
mvn test

# Только интеграционные тесты
mvn test -Dtest=*IntegrationTest

# Только E2E тесты
mvn test -Dtest=*E2ETest
```

### Миграции базы данных

Миграции Flyway выполняются автоматически при запуске приложения. Файлы миграций:
- `V1__init.sql` - создание схемы БД
- `V2__seed.sql` - начальные данные
- `V3__add_guest_children.sql` - добавление таблицы детей гостей
- `V4__create_guests_table.sql` - нормализация таблицы гостей (создание таблицы `guests` и связь с `event_guests`)
- `V5__create_gift_categories.sql` - создание таблицы категорий подарков и связи many-to-many
- `V6__add_user_avatar.sql` - добавление поля avatar в таблицу users
- `V7__add_user_avatars.sql` - заполнение аватаров для начальных пользователей
- `V8__refactor_user_avatar_to_separate_table.sql` - рефакторинг: вынос аватаров пользователей в отдельную таблицу `user_avatars`
- `V9__refactor_child_avatar_to_separate_table.sql` - рефакторинг: вынос аватаров детей в отдельную таблицу `child_avatars`

### Локальная разработка

1. Запустите приложение локально
2. Используйте H2 Console для просмотра данных: http://localhost:8080/h2
3. Используйте Swagger UI для тестирования API: http://localhost:8080/swagger-ui

### Конфигурация

Основные настройки в `application.properties`:
- Порт сервера: `server.port=8080`
- База данных: H2 файловая (`./data/bb`)
- Flyway: включен
- Swagger UI: `/swagger-ui`
- H2 Console: `/h2`

## Поддержка

Для вопросов и предложений создавайте issues в репозитории проекта.

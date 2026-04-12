# Event Service REST API

Базовый путь: через API Gateway `http://localhost:8080` или напрямую через Eureka.

## Модели данных

### LocationDto

| Поле  | Тип    | Описание |
|-------|--------|----------|
| `lat` | Double | Широта   |
| `lon` | Double | Долгота  |

### CategoryResponseDto

| Поле   | Тип    | Описание                           |
|--------|--------|------------------------------------|
| `id`   | Long   | ID категории                       |
| `name` | String | Название категории (1-50 символов) |

### UserShortDto

| Поле   | Тип    | Описание         |
|--------|--------|------------------|
| `id`   | Long   | ID пользователя  |
| `name` | String | Имя пользователя |

### EventShortDto

| Поле                | Тип                 | Описание                              |
|---------------------|---------------------|---------------------------------------|
| `id`                | Long                | ID события                            |
| `title`             | String              | Заголовок                             |
| `annotation`        | String              | Краткое описание                      |
| `category`          | CategoryResponseDto | Категория                             |
| `eventDate`         | String              | Дата проведения (yyyy-MM-dd HH:mm:ss) |
| `paid`              | Boolean             | Платное/бесплатное                    |
| `confirmedRequests` | Long                | Количество подтверждённых заявок      |
| `initiator`         | UserShortDto        | Инициатор                             |
| `rating`            | Double              | Рейтинг                               |

### EventFullDto

| Поле                | Тип                 | Описание                                 |
|---------------------|---------------------|------------------------------------------|
| `id`                | Long                | ID события                               |
| `title`             | String              | Заголовок                                |
| `annotation`        | String              | Краткое описание                         |
| `description`       | String              | Полное описание                          |
| `category`          | CategoryResponseDto | Категория                                |
| `eventDate`         | String              | Дата проведения (yyyy-MM-dd HH:mm:ss)    |
| `location`          | LocationDto         | Местоположение                           |
| `paid`              | Boolean             | Платное/бесплатное                       |
| `participantLimit`  | Integer             | Лимит участников                         |
| `requestModeration` | Boolean             | Модерация заявок                         |
| `confirmedRequests` | Long                | Количество подтверждённых заявок         |
| `createdOn`         | String              | Дата создания (yyyy-MM-dd HH:mm:ss)      |
| `initiator`         | UserShortDto        | Инициатор                                |
| `state`             | String              | Состояние (PENDING, PUBLISHED, CANCELED) |
| `publishedOn`       | String              | Дата публикации (yyyy-MM-dd HH:mm:ss)    |

### EventWithCommentsDto

Расширяет `EventFullDto`, добавляя поле:

| Поле       | Тип              | Описание            |
|------------|------------------|---------------------|
| `comments` | List<CommentDto> | Список комментариев |

### NewEventDto

| Поле                | Тип         | Ограничения      | Описание                              |
|---------------------|-------------|------------------|---------------------------------------|
| `title`             | String      | 3-120 символов   | Заголовок                             |
| `annotation`        | String      | 20-2000 символов | Краткое описание                      |
| `description`       | String      | 20-7000 символов | Полное описание                       |
| `category`          | Long        | ≥0               | ID категории                          |
| `eventDate`         | String      | Future           | Дата проведения (yyyy-MM-dd HH:mm:ss) |
| `location`          | LocationDto | обязательное     | Местоположение                        |
| `paid`              | Boolean     | -                | Платное (по умолчанию false)          |
| `participantLimit`  | Integer     | ≥0               | Лимит участников (по умолчанию 0)     |
| `requestModeration` | Boolean     | -                | Модерация заявок (по умолчанию true)  |

### UpdateEventUserRequest

| Поле                | Тип         | Ограничения                       | Описание              |
|---------------------|-------------|-----------------------------------|-----------------------|
| `title`             | String      | 3-120 символов                    | Заголовок             |
| `annotation`        | String      | 20-2000 символов                  | Краткое описание      |
| `description`       | String      | 20-7000 символов                  | Полное описание       |
| `category`          | Long        | ≥0                                | ID категории          |
| `eventDate`         | String      | Future                            | Дата проведения       |
| `location`          | LocationDto | -                                 | Местоположение        |
| `paid`              | Boolean     | -                                 | Платное               |
| `participantLimit`  | Integer     | ≥0                                | Лимит участников      |
| `requestModeration` | Boolean     | -                                 | Модерация заявок      |
| `stateAction`       | Enum        | `SEND_TO_REVIEW`, `CANCEL_REVIEW` | Действие над событием |

### UpdateEventAdminRequest

| Поле                | Тип         | Ограничения                     | Описание              |
|---------------------|-------------|---------------------------------|-----------------------|
| `title`             | String      | 3-120 символов                  | Заголовок             |
| `annotation`        | String      | 20-2000 символов                | Краткое описание      |
| `description`       | String      | 20-7000 символов                | Полное описание       |
| `category`          | Long        | ≥0                              | ID категории          |
| `eventDate`         | String      | Future                          | Дата проведения       |
| `location`          | LocationDto | -                               | Местоположение        |
| `paid`              | Boolean     | -                               | Платное               |
| `participantLimit`  | Integer     | ≥0                              | Лимит участников      |
| `requestModeration` | Boolean     | -                               | Модерация заявок      |
| `stateAction`       | Enum        | `PUBLISH_EVENT`, `REJECT_EVENT` | Действие над событием |

## Публичные эндпоинты

### 1. Получение списка событий

**GET** `/events`

**Query Parameters:**

| Параметр        | Тип        | Описание                                   |
|-----------------|------------|--------------------------------------------|
| `text`          | String     | Поиск по тексту в аннотации и описании     |
| `categories`    | List<Long> | Фильтр по категориям                       |
| `paid`          | Boolean    | Фильтр по платности                        |
| `rangeStart`    | String     | Начало диапазона дат (yyyy-MM-dd HH:mm:ss) |
| `rangeEnd`      | String     | Конец диапазона дат                        |
| `onlyAvailable` | Boolean    | Только события с доступными местами        |
| `sort`          | String     | Сортировка (EVENT_DATE, VIEWS)             |
| `from`          | Integer    | Начальный индекс (по умолчанию 0)          |
| `size`          | Integer    | Количество элементов (по умолчанию 10)     |

**Response:** `200 OK` → `Collection<EventShortDto>`

```json
[
  {
    "id": 1,
    "title": "Рок-концерт",
    "annotation": "Выступление известной группы",
    "category": {
      "id": 1,
      "name": "Концерты"
    },
    "eventDate": "2025-06-15 19:00:00",
    "paid": true,
    "confirmedRequests": 45,
    "initiator": {
      "id": 10,
      "name": "Иван Петров"
    },
    "rating": 4.5
  }
]
```

### 2. Получение события по ID

**GET** `/events/{eventId}`

**Headers:**

| Заголовок   | Описание                              |
|-------------|---------------------------------------|
| `X-User-Id` | ID пользователя (для учёта просмотра) |

**Path Parameters:**

| Параметр  | Описание   |
|-----------|------------|
| `eventId` | ID события |

**Response:** `200 OK` → `EventFullDto`

```json
{
  "id": 1,
  "title": "Рок-концерт",
  "annotation": "Выступление известной группы",
  "description": "Полное описание концерта...",
  "category": {
    "id": 1,
    "name": "Концерты"
  },
  "eventDate": "2025-06-15 19:00:00",
  "location": {
    "lat": 55.7558,
    "lon": 37.6173
  },
  "paid": true,
  "participantLimit": 100,
  "requestModeration": true,
  "confirmedRequests": 45,
  "createdOn": "2025-01-10 12:00:00",
  "initiator": {
    "id": 10,
    "name": "Иван Петров"
  },
  "state": "PUBLISHED",
  "publishedOn": "2025-01-15 10:00:00"
}
```

**Ошибки:**

- `404 Not Found` — событие не найдено

### 3. Получение события с комментариями

**GET** `/events/comments/{eventId}`

**Headers:**

| Заголовок   | Описание        |
|-------------|-----------------|
| `X-User-Id` | ID пользователя |

**Path Parameters:**

| Параметр  | Описание   |
|-----------|------------|
| `eventId` | ID события |

**Response:** `200 OK` → `EventWithCommentsDto`

```json
{
  "id": 1,
  "title": "Рок-концерт",
  "annotation": "Выступление известной группы",
  "description": "Полное описание концерта...",
  "category": {
    "id": 1,
    "name": "Концерты"
  },
  "eventDate": "2025-06-15 19:00:00",
  "location": {
    "lat": 55.7558,
    "lon": 37.6173
  },
  "paid": true,
  "participantLimit": 100,
  "requestModeration": true,
  "confirmedRequests": 45,
  "createdOn": "2025-01-10 12:00:00",
  "initiator": {
    "id": 10,
    "name": "Иван Петров"
  },
  "state": "PUBLISHED",
  "publishedOn": "2025-01-15 10:00:00",
  "comments": [
    {
      "id": 1,
      "text": "Отличное событие!",
      "authorName": "Мария",
      "created": "2025-01-20 15:30:00"
    },
    {
      "id": 2,
      "text": "Буду обязательно",
      "authorName": "Алексей",
      "created": "2025-01-21 10:15:00"
    }
  ]
}
```

### 4. Оценка события (лайк)

**PUT** `/events/{eventId}/like`

**Headers:**

| Заголовок   | Описание        |
|-------------|-----------------|
| `X-User-Id` | ID пользователя |

**Path Parameters:**

| Параметр  | Описание   |
|-----------|------------|
| `eventId` | ID события |

**Response:** `200 OK` (без тела)

### 5. Получение рекомендаций

**GET** `/events/recommendations`

**Headers:**

| Заголовок   | Описание        |
|-------------|-----------------|
| `X-User-Id` | ID пользователя |

**Response:** `200 OK` → `List<EventFullDto>`

```json
[
  {
    "id": 5,
    "title": "Джазовый вечер",
    "annotation": "Вечер джазовой музыки",
    "description": "Концерт джазового квартета...",
    "category": {
      "id": 1,
      "name": "Концерты"
    },
    "eventDate": "2025-07-20 20:00:00",
    "location": {
      "lat": 55.7558,
      "lon": 37.6173
    },
    "paid": true,
    "participantLimit": 50,
    "requestModeration": true,
    "confirmedRequests": 12,
    "createdOn": "2025-02-01 14:00:00",
    "initiator": {
      "id": 20,
      "name": "Анна Кузнецова"
    },
    "state": "PUBLISHED",
    "publishedOn": "2025-02-05 09:00:00"
  }
]
```

---

## Приватные эндпоинты (пользовательские)

Базовый путь: `/users/{userId}/events`

### 6. Получение событий пользователя

**GET** `/users/{userId}/events`

**Path Parameters:**

| Параметр | Описание        |
|----------|-----------------|
| `userId` | ID пользователя |

**Query Parameters:**

| Параметр | Описание             | По умолчанию |
|----------|----------------------|--------------|
| `from`   | Начальный индекс     | 0            |
| `size`   | Количество элементов | 10           |

**Response:** `200 OK` → `Collection<EventShortDto>`

```json
[
  {
    "id": 1,
    "title": "Рок-концерт",
    "annotation": "Выступление известной группы",
    "category": {
      "id": 1,
      "name": "Концерты"
    },
    "eventDate": "2025-06-15 19:00:00",
    "paid": true,
    "confirmedRequests": 45,
    "initiator": {
      "id": 10,
      "name": "Иван Петров"
    },
    "rating": 4.5
  }
]
```

### 7. Создание события

**POST** `/users/{userId}/events`

**Path Parameters:**

| Параметр | Описание        |
|----------|-----------------|
| `userId` | ID пользователя |

**Request Body:** NewEventDto

```json
{
  "title": "Новое событие",
  "annotation": "Краткое описание события",
  "description": "Полное описание события с деталями",
  "category": 1,
  "eventDate": "2025-08-15 18:00:00",
  "location": {
    "lat": 55.7558,
    "lon": 37.6173
  },
  "paid": true,
  "participantLimit": 100,
  "requestModeration": true
}
```

**Response:** `201 Created` → `EventFullDto`

### 8. Получение события пользователя

**GET** `/users/{userId}/events/{eventId}`

**Path Parameters:**

| Параметр  | Описание        |
|-----------|-----------------|
| `userId`  | ID пользователя |
| `eventId` | ID события      |

**Response:** `200 OK` → `EventFullDto`

### 9. Обновление события пользователем

**PATCH** `/users/{userId}/events/{eventId}`

**Path Parameters:**

| Параметр  | Описание        |
|-----------|-----------------|
| `userId`  | ID пользователя |
| `eventId` | ID события      |

**Request Body:** UpdateEventUserRequest

```json
{
  "title": "Обновлённый заголовок",
  "annotation": "Обновлённая аннотация",
  "description": "Обновлённое описание",
  "category": 2,
  "eventDate": "2025-09-20 20:00:00",
  "location": {
    "lat": 55.7558,
    "lon": 37.6173
  },
  "paid": false,
  "participantLimit": 200,
  "requestModeration": false,
  "stateAction": "SEND_TO_REVIEW"
}
```

**Response:** `200 OK` → `EventFullDto`

---

## Административные эндпоинты

Базовый путь: `/admin/events`

### 10. Получение событий (админ)

**GET** `/admin/events`

**Query Parameters:**

| Параметр     | Тип          | Описание                                            |
|--------------|--------------|-----------------------------------------------------|
| `users`      | List<Long>   | Фильтр по инициаторам                               |
| `states`     | List<String> | Фильтр по состояниям (PENDING, PUBLISHED, CANCELED) |
| `categories` | List<Long>   | Фильтр по категориям                                |
| `rangeStart` | String       | Начало диапазона дат                                |
| `rangeEnd`   | String       | Конец диапазона дат                                 |
| `from`       | Integer      | Начальный индекс (по умолчанию 0)                   |
| `size`       | Integer      | Количество элементов (по умолчанию 10)              |

**Response:** `200 OK` → `Collection<EventFullDto>`

### 11. Обновление события (админ)

**PATCH** `/admin/events/{eventId}`

**Path Parameters:**

| Параметр  | Описание   |
|-----------|------------|
| `eventId` | ID события |

**Request Body:** UpdateEventAdminRequest

```json
{
  "title": "Обновлённый заголовок",
  "annotation": "Обновлённая аннотация",
  "description": "Обновлённое описание",
  "category": 2,
  "eventDate": "2025-09-20 20:00:00",
  "location": {
    "lat": 55.7558,
    "lon": 37.6173
  },
  "paid": false,
  "participantLimit": 200,
  "requestModeration": false,
  "stateAction": "PUBLISH_EVENT"
}
```

**Response:** `200 OK` → `EventFullDto`

---

## Feign эндпоинты (межсервисное взаимодействие)

Базовый путь: `/events/feign`

| Метод | Endpoint                                | Описание                                    |
|-------|-----------------------------------------|---------------------------------------------|
| GET   | `/events/feign/{eventId}`               | Получение события по ID                     |
| GET   | `/events/feign/user/{userId}/{eventId}` | Получение события пользователя              |
| PATCH | `/events/feign/{eventId}/requests`      | Обновление количества подтверждённых заявок |

---

## Categories (Категории)

## Модели данных

**CategoryResponseDto**

| Поле   | Тип    | Описание                           |
|--------|--------|------------------------------------|
| `id`   | Long   | ID категории                       |
| `name` | String | Название категории (1-50 символов) |

**CategoryRequestDto**

| Поле   | Тип    | Ограничения     | Описание                      |
|--------|--------|-----------------|-------------------------------|
| `id`   | Long   | -               | ID категории (при обновлении) |
| `name` | String | 1-50, not blank | Название категории            |

---

## Административные эндпоинты (`/admin/categories`)

| Метод  | Эндпоинт                    | Описание             |
|--------|-----------------------------|----------------------|
| POST   | `/admin/categories`         | Создание категории   |
| PATCH  | `/admin/categories/{catId}` | Обновление категории |
| DELETE | `/admin/categories/{catId}` | Удаление категории   |

### 12. Создать категорию (админ)

**POST** `/admin/categories`

**Request Body:** `CategoryRequestDto`

```json
{
  "name": "Спектакли"
}
```

**Response:** `201 Created` → `CategoryResponseDto`

```json
{
  "id": 3,
  "name": "Спектакли"
}
```

### 13. Обновить категорию (админ)

**PATCH** `/admin/categories/{catId}`

**Request Body:** `CategoryRequestDto`

```json
{
  "name": "Театральные постановки"
}
```

**Response:** `200 OK` → `CategoryResponseDto`

```json
{
  "id": 3,
  "name": "Театральные постановки"
}
```

### 14. Удалить категорию (админ)

**DELETE** `/admin/categories/{catId}`

**Response:** `204 No Content`

---

## Публичные эндпоинты (`/categories`)

| Метод | Эндпоинт              | Описание                                  |
|-------|-----------------------|-------------------------------------------|
| GET   | `/categories`         | Получение списка категорий (с пагинацией) |
| GET   | `/categories/{catId}` | Получение категории по ID                 |

### 15. Получить список категорий (публичный)

**GET** `/categories`

**Query Parameters:**

| Параметр | Тип     | По умолчанию | Описание             |
|----------|---------|--------------|----------------------|
| `from`   | Integer | 0            | Начальный индекс     |
| `size`   | Integer | 10           | Количество элементов |

**Response:** `200 OK` → `Collection<CategoryResponseDto>`

```json
[
  {
    "id": 1,
    "name": "Концерты"
  },
  {
    "id": 2,
    "name": "Выставки"
  }
]
```

### 16. Получить категорию по ID (публичный)

**GET** `/categories/{catId}`

**Response:** `200 OK` → `CategoryResponseDto`

```json
{
  "id": 1,
  "name": "Концерты"
}
```

**Ошибки:**

- `404 Not Found` — категория не найдена

---

## Compilations (Подборки событий)

## Модели данных

**CompilationDto** (ответ)

| Поле     | Тип                  | Описание                  |
|----------|----------------------|---------------------------|
| `id`     | Long                 | ID подборки               |
| `title`  | String               | Заголовок (1-50 символов) |
| `pinned` | Boolean              | Закреплена ли на главной  |
| `events` | Set\<EventShortDto\> | Список событий в подборке |

**NewCompilationDto** (запрос на создание)

| Поле     | Тип         | Ограничения     | По умолчанию | Описание              |
|----------|-------------|-----------------|--------------|-----------------------|
| `title`  | String      | 1-50, not blank | -            | Заголовок             |
| `pinned` | Boolean     | -               | false        | Закрепить на главной  |
| `events` | Set\<Long\> | -               | пустой Set   | ID событий в подборке |

**UpdateCompilationRequest** (запрос на обновление)

| Поле     | Тип         | Ограничения | Описание              |
|----------|-------------|-------------|-----------------------|
| `title`  | String      | 1-50        | Заголовок             |
| `pinned` | Boolean     | -           | Закрепить на главной  |
| `events` | Set\<Long\> | -           | ID событий в подборке |

---

## Административные эндпоинты (`/admin/compilations`)

| Метод  | Эндпоинт                       | Описание            |
|--------|--------------------------------|---------------------|
| POST   | `/admin/compilations`          | Создание подборки   |
| DELETE | `/admin/compilations/{compId}` | Удаление подборки   |
| PATCH  | `/admin/compilations/{compId}` | Обновление подборки |

### 17. Создать подборку (админ)

**POST** `/admin/compilations`

**Request Body:** `NewCompilationDto`

```json
{
  "title": "Лучшие события лета",
  "pinned": true,
  "events": [
    1,
    2,
    3
  ]
}
```

**Response:** `201 Created` → `CompilationDto`

```json
{
  "id": 1,
  "title": "Лучшие события лета",
  "pinned": true,
  "events": [
    {
      "id": 1,
      "title": "Рок-концерт",
      "annotation": "Выступление известной группы",
      "category": {
        "id": 1,
        "name": "Концерты"
      },
      "eventDate": "2025-06-15 19:00:00",
      "paid": true,
      "confirmedRequests": 45,
      "initiator": {
        "id": 10,
        "name": "Иван Петров"
      },
      "rating": 4.5
    }
  ]
}
```

### 18. Удалить подборку (админ)

**DELETE** `/admin/compilations/{compId}`

**Response:** `204 No Content`

### 19. Обновить подборку (админ)

**PATCH** `/admin/compilations/{compId}`

**Request Body:** `UpdateCompilationRequest`

```json
{
  "title": "Лучшие события года",
  "pinned": false,
  "events": [
    1,
    2,
    3,
    4
  ]
}
```

**Response:** `200 OK` → `CompilationDto`

---

## Публичные эндпоинты (`/compilations`)

| Метод | Эндпоинт                 | Описание                                                      |
|-------|--------------------------|---------------------------------------------------------------|
| GET   | `/compilations`          | Получение списка подборок (с пагинацией и фильтром по pinned) |
| GET   | `/compilations/{compId}` | Получение подборки по ID                                      |

### 20. Получить список подборок (публичный)

**GET** `/compilations`

**Query Parameters:**

| Параметр | Тип     | По умолчанию | Описание               |
|----------|---------|--------------|------------------------|
| `pinned` | Boolean | -            | Фильтр по закрепленным |
| `from`   | Integer | 0            | Начальный индекс       |
| `size`   | Integer | 10           | Количество элементов   |

**Response:** `200 OK` → `Collection<CompilationDto>`

```json
[
  {
    "id": 1,
    "title": "Лучшие события лета",
    "pinned": true,
    "events": [
      ...
    ]
  },
  {
    "id": 2,
    "title": "Концерты в парке",
    "pinned": false,
    "events": [
      ...
    ]
  }
]
```

### 21. Получить подборку по ID (публичный)

**GET** `/compilations/{compId}`

**Response:** `200 OK` → `CompilationDto`

```json
{
  "id": 1,
  "title": "Лучшие события лета",
  "pinned": true,
  "events": [
    ...
  ]
}
```

**Ошибки:**

- `404 Not Found` — подборка не найдена
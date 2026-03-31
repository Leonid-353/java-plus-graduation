# Request Service REST API

Базовый путь: через API Gateway `http://localhost:8080` или напрямую через Eureka.

## Модели данных

### Request (сущность)

| Поле          | Тип           | Ограничения   | Описание                                               |
|---------------|---------------|---------------|--------------------------------------------------------|
| `id`          | Long          | автоинкремент | Уникальный идентификатор                               |
| `eventId`     | Long          | not null      | ID события                                             |
| `requesterId` | Long          | not null      | ID пользователя, подавшего заявку                      |
| `status`      | RequestStatus | not null      | Статус заявки (PENDING, CONFIRMED, REJECTED, CANCELED) |
| `created`     | LocalDateTime | not null      | Дата и время создания                                  |

---

### ParticipationRequestDto

| Поле        | Тип           | Описание                                  |
|-------------|---------------|-------------------------------------------|
| `id`        | Long          | ID заявки                                 |
| `event`     | Long          | ID события                                |
| `requester` | Long          | ID пользователя                           |
| `status`    | RequestStatus | Статус заявки                             |
| `created`   | String        | Дата создания (yyyy-MM-dd'T'HH:mm:ss.SSS) |

### EventRequestStatusUpdateRequest

| Поле         | Тип          | Ограничения | Описание                              |
|--------------|--------------|-------------|---------------------------------------|
| `requestIds` | List\<Long\> | not null    | Список ID заявок для обновления       |
| `status`     | Status       | not null    | Новый статус (CONFIRMED или REJECTED) |

### EventRequestStatusUpdateResult

| Поле                | Тип                             | Описание              |
|---------------------|---------------------------------|-----------------------|
| `confirmedRequests` | List\<ParticipationRequestDto\> | Подтвержденные заявки |
| `rejectedRequests`  | List\<ParticipationRequestDto\> | Отклоненные заявки    |

---

### Enums

**RequestStatus** (статус заявки)

| Значение    | Описание               |
|-------------|------------------------|
| `PENDING`   | Ожидает рассмотрения   |
| `CONFIRMED` | Подтверждена           |
| `REJECTED`  | Отклонена              |
| `CANCELED`  | Отменена пользователем |

**Status** (для массового обновления)

| Значение    | Описание    |
|-------------|-------------|
| `CONFIRMED` | Подтвердить |
| `REJECTED`  | Отклонить   |

---

## Приватные эндпоинты (пользовательские)

Базовый путь: `/users/{userId}/requests`

### 1. Получить заявки пользователя

**GET** `/users/{userId}/requests`

**Path Parameters:**

| Параметр | Тип  | Описание        |
|----------|------|-----------------|
| `userId` | Long | ID пользователя |

**Response:** `200 OK` → `List<ParticipationRequestDto>`

```json
[
  {
    "id": 1,
    "event": 101,
    "requester": 10,
    "status": "CONFIRMED",
    "created": "2025-03-31T10:15:30.000"
  },
  {
    "id": 2,
    "event": 102,
    "requester": 10,
    "status": "PENDING",
    "created": "2025-03-31T11:20:00.000"
  }
]
```

### 2. Создать заявку на участие

**POST** `/users/{userId}/requests`

**Path Parameters:**

| Параметр | Тип  | Описание        |
|----------|------|-----------------|
| `userId` | Long | ID пользователя |

**Query Parameters:**

| Параметр  | Тип  | Описание   |
|-----------|------|------------|
| `eventId` | Long | ID события |

**Response:** `201 Created` → `ParticipationRequestDto`

```json
{
  "id": 3,
  "event": 103,
  "requester": 10,
  "status": "PENDING",
  "created": "2025-03-31T14:25:00.000"
}
```

**Ошибки:**

- `409 Conflict` — превышен лимит участников, повторная заявка или событие недоступно
- `404 Not Found` — событие не найдено

### 3. Отменить заявку

**PATCH** `/users/{userId}/requests/{requestId}/cancel`

**Path Parameters:**

| Параметр    | Тип  | Описание        |
|-------------|------|-----------------|
| `userId`    | Long | ID пользователя |
| `requestId` | Long | ID заявки       |

**Response:** `200 OK` → `ParticipationRequestDto`

```json
{
  "id": 3,
  "event": 103,
  "requester": 10,
  "status": "CANCELED",
  "created": "2025-03-31T14:25:00.000"
}
```

---

## Приватные эндпоинты (для инициаторов событий)

Базовый путь: `/users/{userId}/events/{eventId}/requests`

### 4. Получить список заявок на событие

**GET** `/users/{userId}/events/{eventId}/requests`

**Path Parameters:**

| Параметр  | Тип  | Описание                     |
|-----------|------|------------------------------|
| `userId`  | Long | ID пользователя (инициатора) |
| `eventId` | Long | ID события                   |

**Response:** `200 OK` → `List<ParticipationRequestDto>`

```json
[
  {
    "id": 1,
    "event": 101,
    "requester": 20,
    "status": "PENDING",
    "created": "2025-03-31T10:15:30.000"
  },
  {
    "id": 2,
    "event": 101,
    "requester": 30,
    "status": "PENDING",
    "created": "2025-03-31T11:20:00.000"
  }
]
```

### 5. Обновить статус заявок (подтвердить/отклонить)

**PATCH** `/users/{userId}/events/{eventId}/requests`

**Path Parameters:**

| Параметр  | Тип  | Описание                     |
|-----------|------|------------------------------|
| `userId`  | Long | ID пользователя (инициатора) |
| `eventId` | Long | ID события                   |

**Request Body:** `EventRequestStatusUpdateRequest`

```json
{
  "requestIds": [
    1,
    2
  ],
  "status": "CONFIRMED"
}
```

**Response:** `200 OK` → `EventRequestStatusUpdateResult`

```json
{
  "confirmedRequests": [
    {
      "id": 1,
      "event": 101,
      "requester": 20,
      "status": "CONFIRMED",
      "created": "2025-03-31T10:15:30.000"
    }
  ],
  "rejectedRequests": [
    {
      "id": 2,
      "event": 101,
      "requester": 30,
      "status": "REJECTED",
      "created": "2025-03-31T11:20:00.000"
    }
  ]
}
```

**Ошибки:**

- `409 Conflict` — превышен лимит участников при подтверждении
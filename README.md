# marketplace-mock

Мок маркетплейса для разработки AI-ассистента в отрыве от настоящего маркетплейса,
которого пока не существует. Выполняет две роли:

1. **SSO identity provider** — проверяет логин/пароль и в ответ выпускает подписанный
   HMAC-SHA256 JWT с `userId` в поле `sub`. Этот токен ассистент принимает в заголовке
   `Authorization: Bearer <token>` и доверяет ему `userId`, не принимая его больше напрямую
   от клиента (см. `Market_AI_assistent_backend`).
2. **Витрина маркетплейса** — страница с формой логина и встроенным через `<iframe>`
   ассистентом, которому токен передаётся как `?sso_token=...` в URL.

## Тестовые пользователи

Пользователи хранятся в файловой базе H2 (`./data/marketplace-mock.mv.db`, в Docker — на
именованном volume `marketplace-mock-data`, см. `docker-compose.yml`), пароли — как
BCrypt-хэши. При первом старте с пустой базой `DemoUserSeeder` заводит три тестовых аккаунта:

| Логин | Пароль | userId |
|---|---|---|
| alice | alice123 | 5 |
| boris | boris123 | 6 |
| victoria | victoria123 | 7 |

## Таблицы

Обе таблицы — в той же файловой базе H2, схема создаётся/обновляется автоматически
(`spring.jpa.hibernate.ddl-auto: update`), сидятся один раз при первом запуске на пустой базе.

### `mock_users`

| Колонка | Тип | Описание |
|---|---|---|
| `id` | bigint, PK | тот самый `userId`, который попадает в `sub` выпущенного JWT |
| `name` | varchar | отображаемое имя ("Алиса") |
| `username` | varchar, unique | логин для `/api/auth/login` |
| `password_hash` | varchar | BCrypt-хэш пароля |

### `orders`

| Колонка | Тип | Описание |
|---|---|---|
| `id` | bigint, PK, autoincrement | id заказа |
| `user_id` | bigint | владелец заказа — `id` из `mock_users` |
| `order_composition` | varchar | состав заказа |
| `status` | varchar | статус (`created`, `delivered`, ...) |
| `delivery_date` | timestamp | дата доставки |

## Запуск

```bash
mvn spring-boot:run
```

Сервис поднимается на `:8081`. Переменные окружения:

- `SSO_SHARED_SECRET` — секрет для подписи JWT. **Должен совпадать** со значением
  `SSO_SHARED_SECRET` в `.env` `Market_AI_assistent_backend`. Минимум 32 байта (требование
  HMAC-SHA256).
- `ASSISTANT_FRONTEND_URL` — URL фронтенда ассистента, который получит токен в `?sso_token=`
  (по умолчанию `http://localhost:5173`).

## Эндпоинты

- `GET /` — страница маркетплейса.
- `POST /api/auth/login` — `{"username": "...", "password": "..."}` → `{"token": "..."}` или `401`.
- `GET /api/orders` — требует `Authorization: Bearer <token>`; возвращает заказы владельца
  токена: `[{"id", "orderComposition", "status", "deliveryDate"}, ...]`. Без токена/с неверным
  токеном — `401`.
- `GET /api/orders/{id}` — тот же токен; возвращает заказ, только если `id` принадлежит
  владельцу токена, иначе `404` (чужой заказ или несуществующий id — не различаются).

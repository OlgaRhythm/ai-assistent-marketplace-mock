# Как посмотреть логи и метрики

Короткая памятка. Полное описание стека мониторинга — в [MONITORING.md](./MONITORING.md).

## Логи

### Быстрый способ — `docker logs`

Без Grafana, прямо из терминала:

```bash
docker logs market-ai-assistant-backend --tail 50 -f   # backend
docker logs ai-assistant --tail 50 -f                   # ml
docker logs marketplace-mock --tail 50 -f                # маркетплейс (в Loki не попадает, см. ниже)
```

`-f` — следить за новыми строками в реальном времени, `--tail 50` — последние 50 строк.

### Основной способ — Grafana Explore

1. Открыть **http://localhost:3001**, логин/пароль — `GRAFANA_ADMIN_USER`/`GRAFANA_ADMIN_PASSWORD` из `docker-compose.yml` (по умолчанию `admin` / `local-dev-grafana-password-change-me`).
2. В левом меню — иконка компаса **Explore**.
3. В выпадающем списке источника данных (сверху слева) выбрать **Loki**.
4. Ввести запрос (LogQL) и запустить (`Shift+Enter`).

### ⚠️ Если результат пустой / "No logs volume available"

Почти всегда это не ошибка, а диапазон времени. Проверьте пикер времени (справа сверху) —
если там, например, "Last 15 minutes", а искомое событие было раньше, результат будет пуст.
Поставьте диапазон шире (**Last 6 hours** / **Last 24 hours**) и повторите запрос.

### Готовые запросы

```logql
{compose_service="backend"}                              # все логи backend
{compose_service="ml"}                                    # все логи ML-сервиса
{compose_service="backend"} | json | level="error"         # только ошибки backend
{compose_service="backend"} |= "escalated to operator"     # эскалации оператору
{compose_service="backend"} |= "sso token rejected"         # неуспешные авторизации
{compose_service="backend"} | json | service="frontend"     # ошибки фронтенда (физически лежат в контейнере backend)
```

Поиск одного запроса по `trace_id` (сквозная трассировка через backend → ml):

```logql
{compose_service=~"backend|ml"} | json | trace_id="<ВАШ_TRACE_ID>"
```

`trace_id` виден в каждой строке лога и в заголовке ответа `X-Trace-Id`.

### Чтобы не вводить запросы каждый раз

В Explore справа есть иконка **часов (Query history)** — там сохраняется история всех
запущенных запросов. У нужного запроса нажмите значок ⭐ — он переедет во вкладку
**Starred** и всегда будет под рукой (хранится в Grafana, привязано к вашему логину).

## Метрики

### Главный способ — готовый дашборд

**http://localhost:3001** → папка **Assistant** → дашборд **AI Assistant — overview**.
Все метрики ниже уже выведены на панели — ничего вручную писать не нужно, достаточно
открыть дашборд и выбрать диапазон времени сверху справа.

| Панель на дашборде | Метрика-источник | Что показывает |
|---|---|---|
| % закрыто без оператора (цель > 80%) | `support_responses_total` | Доля обращений, закрытых без привлечения оператора |
| Передано оператору, шт | `support_responses_total{type="OPERATOR"}` | Абсолютное число эскалаций за период |
| % передано оператору (цель < 20%) | `support_responses_total` | То же, в процентах |
| Uptime backend / Uptime ml (цель > 99.5%) | `up{job="backend"}` / `up{job="ml"}` | Доступность сервисов — 1, когда Prometheus успешно опросил `/metrics` |
| Неудачных авторизаций за период | `auth_failures_total` | Отклонённые SSO-токены (сырое число) |
| Неудачные авторизации, rate/сек | `auth_failures_total` | То же, как скорость — на этой метрике висит алерт **Auth failure spike** |
| Время ответа `/support`, avg/p95 | лог `totalMs` (Loki) | Латентность основного эндпоинта — не Prometheus-метрика, а агрегация по логам |
| % ответов с confidence > 0.85 (цель 85%) | `classification_confidence` | Доля классификаций, в которых модель уверена (confidence > 0.85) |
| Confidence классификатора, p50/p95 | `classification_confidence` | Перцентили уверенности модели во времени — падение сигнализирует о деградации |
| Эскалации оператору по причине | `escalations_total{reason}` | Разбивка эскалаций: `message_limit_reached` / `human_agent_intent` |
| HTTP ошибки backend (5xx), rate/сек | `http_requests_total{status}` | Скорость серверных ошибок backend |
| HTTP запросы backend по статусу | `http_requests_total{status}` | Весь трафик backend в разбивке по HTTP-статусам |

### Какие метрики есть "под капотом"

Если нужно собрать свою панель или запрос — вот что реально отдают сервисы в `/metrics`.

**Backend** (`http://backend:5005/metrics`, job `backend` в Prometheus):

| Метрика | Тип | Лейблы | Назначение |
|---|---|---|---|
| `support_responses_total` | counter | `type` (ANSWER / DETAILSREQUEST / ORDERINFOREQUEST / OPERATOR) | Чем закрылось каждое обращение |
| `escalations_total` | counter | `reason` | Эскалации оператору с причиной |
| `classification_confidence` | histogram | — | Confidence классификатора на каждый запрос |
| `auth_failures_total` | counter | — | Отклонённые SSO-токены |
| `http_requests_total` | counter | `route`, `method`, `status` | Все HTTP-запросы backend |
| `process_*`, `nodejs_*` | counter/gauge | — | Дефолтные метрики Node.js-процесса (CPU, память, event loop lag) из `prom-client` |

**ML-сервис** (`http://ml:8000/metrics`, job `ml`):

| Метрика | Тип | Назначение |
|---|---|---|
| `http_request_duration_seconds` | histogram | Латентность по эндпоинтам (`/assistant/process`, `/classify`, `/generate-answer`) |
| `http_requests_total` | counter | Количество запросов по эндпоинту и статусу |
| `http_request_size_bytes` / `http_response_size_bytes` | histogram | Размеры запросов/ответов |

**marketplace-mock** отдаёт свои метрики на `/actuator/prometheus`, но Prometheus их **не собирает** —
осознанное решение (в реальной системе маркетплейс — внешний сервис без доступа к его телеметрии).

### Быстрый способ — curl / Prometheus UI

Без Grafana, напрямую:

```bash
curl http://localhost:5005/metrics   # backend
curl http://localhost:8000/metrics   # ml
```

Или через веб-интерфейс Prometheus — **http://localhost:9090** → вкладка **Graph**,
вписать PromQL и нажать **Execute**. Там же **Status → Targets** — видно, какие
сервисы сейчас доступны для скрейпа.

### Если нужен произвольный запрос в Explore

Источник данных — **Prometheus** (не Loki). Примеры:

```promql
sum(increase(support_responses_total{type!="OPERATOR"}[$__range])) / sum(increase(support_responses_total[$__range])) * 100
sum by (reason) (increase(escalations_total[$__range]))
rate(auth_failures_total[5m])
avg_over_time(up{job="backend"}[$__range]) * 100
```

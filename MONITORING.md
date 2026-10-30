# Логирование и мониторинг

Стек: **Grafana + Loki + Prometheus**, поднимается вместе со всем остальным через
`docker compose up -d --build` из этой папки.

## Как открыть

- Grafana: `http://localhost:3001` (логин/пароль — `GRAFANA_ADMIN_USER`/`GRAFANA_ADMIN_PASSWORD`
  из `docker-compose.yml`, по умолчанию `admin`/`local-dev-grafana-password-change-me`).
  - **Explore → Loki** — поиск логов всех сервисов, фильтр `{compose_service="backend"}`
    и т.п., можно искать по `trace_id` для сквозной трассировки одного запроса.
  - **Alerting → Alert rules** — правило `Auth failure spike`.
- Prometheus: `http://localhost:9090` (Status → Targets — видно, что скрейпится).
- Логи, которые видит Promtail, собираются напрямую из `docker logs` каждого контейнера —
  отдельно ничего настраивать не нужно, сервисы просто пишут в stdout как и раньше.

## Схема

```
backend, ml            --/metrics-->  Prometheus
   |
   |-- stdout (docker logs) --> Promtail --> Loki --> Grafana (Explore)
                                                           |
                                                 Alerting --> webhook-receiver
```

`marketplace-mock` **сознательно не входит** в этот пайплайн — ни логи (Promtail явно
дропает его контейнер в `monitoring/promtail/promtail-config.yml`), ни метрики (нет job'а в
`monitoring/prometheus/prometheus.yml`). Логика: в реальной системе маркетплейс — внешний
сервис, к внутренней телеметрии которого у команды ассистента не было бы доступа; мок сделан
так же, хотя у него и своё `/actuator/prometheus`, и собственные JSON-логи в stdout — просто
никто их отсюда не собирает.

`frontend` отдельно логов не шлёт сам — ошибки браузера (`window.onerror`,
`unhandledrejection`, пойманные `ApiError`) пробрасываются в `POST /logs/frontend` на
бэкенде и попадают в общий пайплайн как `service: "frontend"` (хотя физически лог лежит в
контейнере `backend` — искать по полю `service`, не по `compose_service`, если нужны именно
ошибки фронтенда).

## Формат лога

Одна JSON-строка на событие, во всех сервисах есть:

```json
{
  "timestamp": "...",
  "level": "info|warn|error",
  "service": "backend|ml|marketplace-mock|frontend",
  "trace_id": "...",
  "app_version": "...",
  "message": "...",
  "action": "...",
  "authz_result": "allowed|denied",
  "user_id": 7
}
```

`action`/`authz_result`/`user_id` присутствуют там, где они осмысленны (точки
аутентификации и доступа к заказам) — не на каждой строке. Пароли, токены,
`Authorization`-заголовки никогда не логируются (проверено `grep` по Loki на реальных
значениях пароля и JWT — ноль совпадений).

## Алерт на подозрительную активность

Правило `Auth failure spike` (`monitoring/grafana/provisioning/alerting/alerting.yml`) следит
за `rate(auth_failures_total[1m])` — счётчиком отклонённых SSO-токенов на бэкенде (401 от
`requireSsoAuth`). При превышении порога алерт летит на `webhook-receiver` — локальный сервис,
который просто логирует всё полученное тело в stdout (значит, видно и в `docker compose logs
webhook-receiver`, и в Loki).

Проверено вручную: устойчивый всплеск запросов с невалидным токеном на `/orders` → Grafana
перевела правило в `firing` → `webhook-receiver` получил и залогировал полный payload алерта.

## Соответствие пунктам чек-листа (раздел 2.8)

| Пункт чек-листа | Чем закрыт |
|---|---|
| В журналы не попадают пароли, токены и секреты | JSON-поля фиксированные, пароль/JWT никогда не передаются в `log()`/логгер; проверено `grep` по Loki |
| Время, ID запроса, действие, версия приложения, результат проверки прав | `timestamp`, `trace_id`, `action`, `app_version`, `authz_result` в каждом сервисе |
| Алерты на подозрительные события + тестовая проверка | `Auth failure spike` в Grafana, проверено реальным всплеском 401 → `firing` → webhook доставлен |
| Формат логов пригоден для выгрузки в SIEM (например, CEF) | JSON с фиксированными полями прямо мапится на поля CEF (`service`→Device Product, `action`→Signature ID, `level`→Severity, остальное → Extension); полноценный CEF-экспортёр не делали, пункт в чек-листе условный |

## Удалённый `log-viewer`

Самописный просмотрщик логов (`docker logs`/файлы, без агрегации и алертов) удалён — его
функциональность полностью покрывается Grafana Explore + Alerting.

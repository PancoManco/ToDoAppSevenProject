# frontend

Минимальный React-фронт для ToDoAppSevenProject. Без роутера, без стейт-менеджера,
без UI-библиотек — только React + Vite и один CSS-файл.

## Запуск

```bash
cd frontend
npm install
npm run dev
```

Откроется на `http://localhost:5173` — ровно тот адрес, что прописан в
`FRONTEND_ORIGIN` бэкенда, поэтому CORS сработает без правок.

Бэкенд при этом должен быть поднят: как минимум **gateway (8080), auth, task**
и инфраструктура (postgres, redis). Письма ловим в Mailpit — `localhost:8025`.

## Что делает

```
нет токена ──> AuthScreen ──регистрация──> ввод кода ──┐
                    │                                  │ верификация выдаёт токены
                    └────────── вход ──────────────────┤
                                                       ▼
есть токен ──────────────────────────────────> TasksScreen
                                        (список / создать / отметить / удалить)
```

Весь «роутинг» — одна переменная в `App.jsx`: есть access-токен → задачи, нет → вход.

## Файлы

| Файл | Зачем |
|---|---|
| `src/api.js` | **Единственное место, где фронт знает про бэкенд.** Пути, тело запросов, токен. |
| `src/App.jsx` | Показывает экран входа или экран задач. |
| `src/components/AuthScreen.jsx` | Вход, регистрация, ввод кода — три режима одного экрана. |
| `src/components/TasksScreen.jsx` | Список задач и действия над ними. |
| `src/styles.css` | Вся вёрстка. |

## Сверь с реальным API (важно)

Фронт написан по контракту, который мы обсуждали. Если что-то не сойдётся —
правь **только `src/api.js`**, остальной код трогать не нужно.

| Что | Ожидается |
|---|---|
| Регистрация | `POST /api/v1/auth/register` `{name, email, password}` |
| Верификация | `POST /api/v1/auth/verify-email` `{email, code}` → в ответе пара токенов |
| Повторный код | `POST /api/v1/auth/resend-verification-code` `{email}` |
| Вход | `POST /api/v1/auth/login` `{email, password}` → access-токен в теле |
| Выход | `POST /api/v1/auth/logout` |
| Список задач | `GET /api/v1/tasks` → массив |
| Создать | `POST /api/v1/tasks` `{title, description}` |
| Отметить | `PATCH /api/v1/tasks/{id}/complete?completed=true|false` |
| Удалить | `DELETE /api/v1/tasks/{id}` |

Поле с access-токеном достаётся «на все случаи» (`accessToken` / `access_token` /
`token`) — см. `extractAccessToken` в `api.js`.

Refresh-токен трогать не нужно: он в httpOnly-cookie, браузер шлёт его сам
(во всех запросах стоит `credentials: 'include'`).

## Отладка

* **CORS-ошибка в консоли** → фронт не на 5173 или `FRONTEND_ORIGIN` на бэкенде другой.
* **«Бэкенд недоступен»** → gateway не поднят на 8080.
* **401 сразу после входа** → бэкенд вернул токен под другим именем, чем ждёт `extractAccessToken`.
* **Задачи не приходят (403)** → gateway не маршрутизирует `/api/v1/tasks/**`.

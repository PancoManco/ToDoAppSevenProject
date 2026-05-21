# Task Planner Frontend

Нормальная основа frontend-приложения для планировщика задач.

## Что уже есть

- Login
- Register
- Email verification screen
- Resend verification code
- Forgot password placeholder
- OAuth success page
- Protected app layout
- Protected profile page
- Dashboard
- Tasks board пока на локальном состоянии
- JWT access token в localStorage
- Refresh token через HttpOnly cookie

## Backend endpoints, которые ожидает frontend

```txt
POST /api/v1/auth/register
POST /api/v1/auth/verify-email
POST /api/v1/auth/resend-verification-code
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/me
```

Register expected body:

```json
{
  "name": "Panco",
  "email": "panco@example.com",
  "password": "123456"
}
```

Verify email expected body:

```json
{
  "email": "panco@example.com",
  "code": "123456"
}
```

Login expected body:

```json
{
  "email": "panco@example.com",
  "password": "123456"
}
```

Auth response expected body:

```json
{
  "accessToken": "..."
}
```

## Установка

```bash
cd task-planner-frontend
npm install
npm run dev
```

Открыть:

```txt
http://localhost:5173
```

## Настройка backend URL

Создай `.env` из примера:

```bash
cp .env.example .env
```

Для локального backend:

```txt
VITE_API_BASE_URL=http://localhost:8080
```

## Важно для backend CORS

Backend должен разрешать origin:

```txt
http://localhost:5173
```

И frontend делает запросы с:

```js
credentials: "include"
```

Поэтому refresh cookie будет работать.

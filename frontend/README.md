# Task Planner Frontend — Reset Password Link

Frontend для планировщика задач с auth flow:

- Login
- Register
- Email verification
- Resend verification code
- Forgot password через ссылку
- Reset password по `token` из URL
- OAuth success page
- Protected app layout
- Dashboard
- Tasks page
- Profile page

## Запуск

```bash
cd task-planner-frontend
npm install
npm run dev
```

Открыть:

```txt
http://localhost:5173
```

## Backend URL

Скопируй `.env.example` в `.env`:

```bash
cp .env.example .env
```

```env
VITE_API_BASE_URL=http://localhost:8080
```

## Ожидаемые backend endpoints

```txt
POST /api/v1/auth/register
POST /api/v1/auth/verify-email
POST /api/v1/auth/resend-verification-code
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
POST /api/v1/auth/forgot-password
POST /api/v1/auth/reset-password
GET  /api/me
```

## Reset password flow

### 1. Запрос ссылки

Frontend page:

```txt
/forgot-password
```

Request:

```http
POST /api/v1/auth/forgot-password
```

Body:

```json
{
  "email": "user@example.com"
}
```

Expected response:

```json
{
  "message": "If this email exists, password reset link has been sent"
}
```

### 2. Ссылка в письме

Backend должен отправить ссылку такого вида:

```txt
http://localhost:5173/reset-password?token=RAW_RESET_TOKEN
```

### 3. Смена пароля

Frontend page:

```txt
/reset-password?token=RAW_RESET_TOKEN
```

Request:

```http
POST /api/v1/auth/reset-password
```

Body:

```json
{
  "token": "RAW_RESET_TOKEN",
  "newPassword": "newPassword123"
}
```

Expected response:

```json
{
  "message": "Password has been reset"
}
```

После успеха frontend автоматически перекинет на `/login`.

## Важно для backend CORS

Backend должен разрешать origin:

```txt
http://localhost:5173
```

Frontend делает запросы с:

```js
credentials: "include"
```

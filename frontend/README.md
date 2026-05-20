# Auth Test Frontend v3

Тестовый frontend для email/password JWT auth + OAuth2 login.

## Страницы

- `/` — login/register + OAuth buttons
- `/profile` — защищённая страница
- `/oauth-success` — страница, куда backend редиректит после OAuth success

## Запуск

```bash
cd auth-test-frontend-v3
npm install
npm run dev
```

Открыть:

```txt
http://localhost:5173
```

Backend ожидается здесь:

```txt
http://localhost:8080
```

## OAuth flow

1. Frontend отправляет браузер на:
   - `http://localhost:8080/oauth2/authorization/google`
   - `http://localhost:8080/oauth2/authorization/github`

2. Backend после успешного OAuth:
   - создаёт/находит пользователя
   - создаёт JWT token pair
   - кладёт refresh token в HttpOnly cookie
   - редиректит на `http://localhost:5173/oauth-success`

3. Frontend на `/oauth-success` вызывает:
   - `POST http://localhost:8080/api/v1/auth/refresh`

4. Backend возвращает access token.

5. Frontend открывает `/profile` и вызывает:
   - `GET http://localhost:8080/api/me`

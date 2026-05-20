import React, { useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

const API_BASE = "http://localhost:8080";
const FRONTEND_BASE = "http://localhost:5173";

function App() {
  const [page, setPage] = useState(() => {
    if (window.location.pathname === "/oauth-success") return "oauth-success";
    if (window.location.pathname === "/profile") return "profile";
    return "auth";
  });

  const [mode, setMode] = useState("login");

  const [loginEmail, setLoginEmail] = useState("test@example.com");
  const [loginPassword, setLoginPassword] = useState("123456");

  const [registerName, setRegisterName] = useState("Test User");
  const [registerEmail, setRegisterEmail] = useState("test@example.com");
  const [registerPassword, setRegisterPassword] = useState("123456");

  const [accessToken, setAccessToken] = useState(
    localStorage.getItem("accessToken") || ""
  );

  const [currentUser, setCurrentUser] = useState(null);
  const [status, setStatus] = useState("Готово к тестированию.");
  const [responseData, setResponseData] = useState(null);
  const [loading, setLoading] = useState(false);

  const isAuthenticated = useMemo(() => Boolean(accessToken), [accessToken]);

  useEffect(() => {
    if (page === "oauth-success") {
      handleOAuthSuccess();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  useEffect(() => {
    if (page === "profile" && accessToken && !currentUser) {
      handleMe();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, accessToken]);

  function navigate(nextPage) {
    setPage(nextPage);

    const path = nextPage === "profile" ? "/profile" : "/";
    window.history.pushState({}, "", path);
  }

  function saveAccessToken(token) {
    setAccessToken(token);
    localStorage.setItem("accessToken", token);
  }

  function clearAccessToken() {
    setAccessToken("");
    localStorage.removeItem("accessToken");
    setCurrentUser(null);
  }

  async function api(path, options = {}, overrideAccessToken = accessToken) {
    const headers = {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(overrideAccessToken ? { Authorization: `Bearer ${overrideAccessToken}` } : {}),
      ...(options.headers || {})
    };

    const response = await fetch(`${API_BASE}${path}`, {
      ...options,
      credentials: "include",
      headers
    });

    const text = await response.text();

    let body = null;
    try {
      body = text ? JSON.parse(text) : null;
    } catch {
      body = text;
    }

    if (!response.ok) {
      const error = new Error("Request failed");
      error.status = response.status;
      error.body = body;
      throw error;
    }

    return body;
  }

  async function run(label, callback) {
    setLoading(true);
    setStatus(label);
    setResponseData(null);

    try {
      const data = await callback();
      setResponseData(data ?? { ok: true });
      setStatus("Успешно.");
      return data;
    } catch (error) {
      const payload = {
        status: error.status || "NETWORK_ERROR",
        body: error.body || error.message
      };

      setResponseData(payload);
      setStatus("Ошибка.");
      return null;
    } finally {
      setLoading(false);
    }
  }

  async function handleLogin(event) {
    event?.preventDefault();

    const data = await run("Выполняю вход...", () =>
      api("/api/v1/auth/login", {
        method: "POST",
        body: JSON.stringify({
          email: loginEmail.trim(),
          password: loginPassword
        })
      })
    );

    if (data?.accessToken) {
      saveAccessToken(data.accessToken);
      navigate("profile");
      await loadMeWithToken(data.accessToken);
    }
  }

  async function handleRegister(event) {
    event?.preventDefault();

    const data = await run("Создаю аккаунт...", () =>
      api("/api/v1/auth/register", {
        method: "POST",
        body: JSON.stringify({
          name: registerName.trim(),
          email: registerEmail.trim(),
          password: registerPassword
        })
      })
    );

    if (data?.accessToken) {
      saveAccessToken(data.accessToken);
      setLoginEmail(registerEmail);
      setLoginPassword(registerPassword);
      navigate("profile");
      await loadMeWithToken(data.accessToken);
    }
  }

  function startOAuth(provider) {
    window.location.href = `${API_BASE}/oauth2/authorization/${provider}`;
  }

  async function handleOAuthSuccess() {
    const data = await run("OAuth успешен. Получаю access token через refresh cookie...", () =>
      api("/api/v1/auth/refresh", {
        method: "POST"
      }, "")
    );

    if (data?.accessToken) {
      saveAccessToken(data.accessToken);
      navigate("profile");
      await loadMeWithToken(data.accessToken);
    }
  }

  async function loadMeWithToken(token) {
    const data = await run("Загружаю защищённый профиль...", () =>
      api("/api/me", {
        method: "GET"
      }, token)
    );

    if (data) {
      setCurrentUser(data);
    }
  }

  async function handleMe() {
    const data = await run("Запрашиваю /api/me...", () =>
      api("/api/me", {
        method: "GET"
      })
    );

    if (data) {
      setCurrentUser(data);
    }
  }

  async function handleRefresh() {
    const data = await run("Обновляю access token через refresh cookie...", () =>
      api("/api/v1/auth/refresh", {
        method: "POST"
      }, "")
    );

    if (data?.accessToken) {
      saveAccessToken(data.accessToken);
      await loadMeWithToken(data.accessToken);
    }
  }

  async function handleLogout() {
    await run("Выполняю logout...", () =>
      api("/api/v1/auth/logout", {
        method: "POST"
      }, "")
    );

    clearAccessToken();
    navigate("auth");
  }

  function handleClearLocalToken() {
    clearAccessToken();
    setStatus("Локальный access token удалён. Refresh cookie не тронута.");
    setResponseData({
      message:
        "Теперь можно нажать Refresh. Если refresh cookie существует, backend выдаст новый access token."
    });
  }

  function renderAuthPage() {
    return (
      <div className="grid">
        <section className="card">
          <div className="tabs">
            <button
              className={mode === "login" ? "tab active" : "tab"}
              onClick={() => setMode("login")}
              type="button"
            >
              Вход
            </button>
            <button
              className={mode === "register" ? "tab active" : "tab"}
              onClick={() => setMode("register")}
              type="button"
            >
              Регистрация
            </button>
          </div>

          {mode === "login" ? (
            <form onSubmit={handleLogin} className="form">
              <label>
                Email
                <input
                  value={loginEmail}
                  onChange={(event) => setLoginEmail(event.target.value)}
                  type="email"
                  placeholder="user@example.com"
                  autoComplete="email"
                  required
                />
              </label>

              <label>
                Пароль
                <input
                  value={loginPassword}
                  onChange={(event) => setLoginPassword(event.target.value)}
                  type="password"
                  placeholder="Минимум 6 символов"
                  autoComplete="current-password"
                  required
                />
              </label>

              <button className="primary" disabled={loading} type="submit">
                Войти
              </button>
            </form>
          ) : (
            <form onSubmit={handleRegister} className="form">
              <label>
                Имя
                <input
                  value={registerName}
                  onChange={(event) => setRegisterName(event.target.value)}
                  type="text"
                  placeholder="Ivan Ivanov"
                  autoComplete="name"
                  required
                />
              </label>

              <label>
                Email
                <input
                  value={registerEmail}
                  onChange={(event) => setRegisterEmail(event.target.value)}
                  type="email"
                  placeholder="user@example.com"
                  autoComplete="email"
                  required
                />
              </label>

              <label>
                Пароль
                <input
                  value={registerPassword}
                  onChange={(event) => setRegisterPassword(event.target.value)}
                  type="password"
                  placeholder="От 6 до 20 символов"
                  minLength={6}
                  maxLength={20}
                  autoComplete="new-password"
                  required
                />
              </label>

              <button className="primary" disabled={loading} type="submit">
                Зарегистрироваться и войти
              </button>
            </form>
          )}

          <div className="divider">
            <span>или войти через OAuth</span>
          </div>

          <div className="oauthButtons">
            <button onClick={() => startOAuth("google")} type="button">
              Continue with Google
            </button>
            <button onClick={() => startOAuth("github")} type="button">
              Continue with GitHub
            </button>
          </div>
        </section>

        {renderToolsCard()}
      </div>
    );
  }

  function renderProfilePage() {
    if (!isAuthenticated) {
      return (
        <section className="card protected">
          <h2>Защищённая страница</h2>
          <p className="warning">
            Access token отсутствует. Сначала войди через email/password или OAuth.
          </p>
          <button onClick={() => navigate("auth")} type="button" className="primary">
            На страницу входа
          </button>
        </section>
      );
    }

    return (
      <div className="grid">
        <section className="card protected">
          <p className="eyebrow">Protected page</p>
          <h2>Ты вошёл в защищённую страницу</h2>

          {currentUser ? (
            <div className="profilePanel">
              <div className="avatar">
                {(currentUser.email || "?").slice(0, 1).toUpperCase()}
              </div>

              <div>
                <p className="profileEmail">{currentUser.email}</p>
                <p className="profileMeta">User ID: {currentUser.userId}</p>
                <p className="profileMeta">
                  Roles: {currentUser.roles?.join(", ") || "—"}
                </p>
              </div>
            </div>
          ) : (
            <p>Профиль ещё не загружен. Нажми GET /api/me.</p>
          )}

          <div className="actions">
            <button onClick={handleMe} disabled={loading} type="button">
              Перезагрузить профиль
            </button>
            <button onClick={handleLogout} disabled={loading} type="button">
              Logout
            </button>
          </div>
        </section>

        {renderToolsCard()}
      </div>
    );
  }

  function renderToolsCard() {
    return (
      <section className="card">
        <h2>Проверка авторизации</h2>

        <div className="actions">
          <button onClick={handleMe} disabled={loading} type="button">
            GET /api/me
          </button>
          <button onClick={handleRefresh} disabled={loading} type="button">
            Refresh
          </button>
          <button onClick={handleLogout} disabled={loading} type="button">
            Logout
          </button>
          <button
            onClick={handleClearLocalToken}
            disabled={loading}
            className="secondary"
            type="button"
          >
            Очистить access локально
          </button>
        </div>

        <div className="infoBox">
          <strong>Backend:</strong> <code>{API_BASE}</code>
          <br />
          <strong>Frontend:</strong> <code>{FRONTEND_BASE}</code>
          <br />
          <strong>OAuth success:</strong> <code>{FRONTEND_BASE}/oauth-success</code>
        </div>
      </section>
    );
  }

  return (
    <main className="page">
      <section className="shell">
        <div className="hero">
          <div>
            <p className="eyebrow">Spring Boot JWT + OAuth2</p>
            <h1>Тестовый фронтенд авторизации</h1>
            <p className="description">
              Email/password flow возвращает access token сразу. OAuth flow ставит refresh cookie
              на backend, затем frontend вызывает refresh и попадает на protected page.
            </p>
          </div>

          <div className="topActions">
            <button onClick={() => navigate("auth")} type="button">
              Auth
            </button>
            <button onClick={() => navigate("profile")} type="button">
              Protected Profile
            </button>
            <div className={isAuthenticated ? "badge success" : "badge"}>
              {isAuthenticated ? "Есть access token" : "Не авторизован"}
            </div>
          </div>
        </div>

        {page === "profile" ? renderProfilePage() : renderAuthPage()}

        <section className="card full">
          <div className="statusRow">
            <h2>Состояние</h2>
            <span>{status}</span>
          </div>

          <h3>Access token</h3>
          <textarea
            readOnly
            value={accessToken}
            placeholder="После login/register/refresh access token появится здесь"
          />

          <h3>Ответ backend</h3>
          <pre>{JSON.stringify(responseData, null, 2) || "Пока нет ответа."}</pre>
        </section>
      </section>
    </main>
  );
}

createRoot(document.getElementById("root")).render(<App />);

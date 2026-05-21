import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../shared/auth/AuthContext.jsx";
import { FormError } from "../../shared/ui/FormError.jsx";
import { API_BASE_URL } from "../../shared/api/config.js";

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  const [form, setForm] = useState({
    email: "",
    password: ""
  });

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const from = location.state?.from?.pathname || "/app";

  function updateField(event) {
    setForm((current) => ({
      ...current,
      [event.target.name]: event.target.value
    }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      await login({
        email: form.email.trim(),
        password: form.password
      });

      navigate(from, { replace: true });
    } catch (err) {
      setError(err.message || "Не удалось войти");
    } finally {
      setLoading(false);
    }
  }

  function startOAuth(provider) {
    window.location.href = `${API_BASE_URL}/oauth2/authorization/${provider}`;
  }

  return (
    <div className="auth-card">
      <div className="auth-card-header">
        <p className="eyebrow">Добро пожаловать</p>
        <h2>Вход в аккаунт</h2>
        <p>Войди, чтобы продолжить работу с задачами.</p>
      </div>

      <form className="form" onSubmit={handleSubmit}>
        <label>
          Email
          <input
            name="email"
            value={form.email}
            onChange={updateField}
            type="email"
            autoComplete="email"
            placeholder="you@example.com"
            required
          />
        </label>

        <label>
          Пароль
          <input
            name="password"
            value={form.password}
            onChange={updateField}
            type="password"
            autoComplete="current-password"
            placeholder="••••••••"
            required
          />
        </label>

        <div className="form-row">
          <Link to="/forgot-password">Забыли пароль?</Link>
        </div>

        <FormError error={error} />

        <button className="primary-button" disabled={loading} type="submit">
          {loading ? "Входим..." : "Войти"}
        </button>
      </form>

      <div className="divider">
        <span>или</span>
      </div>

      <div className="oauth-grid">
        <button onClick={() => startOAuth("google")} type="button">
          G
          Google
        </button>
        <button onClick={() => startOAuth("github")} type="button">
          <span aria-hidden="true">GH</span>
          GitHub
        </button>
      </div>

      <p className="auth-footer">
        Нет аккаунта? <Link to="/register">Создать аккаунт</Link>
      </p>
    </div>
  );
}

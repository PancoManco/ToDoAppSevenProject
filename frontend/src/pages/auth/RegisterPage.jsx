import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { authApi } from "../../shared/api/authApi.js";
import { FormError } from "../../shared/ui/FormError.jsx";

export function RegisterPage() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    name: "",
    email: "",
    password: ""
  });

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

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
      await authApi.register({
        name: form.name.trim(),
        email: form.email.trim(),
        password: form.password
      });

      navigate(`/verify-email?email=${encodeURIComponent(form.email.trim())}`, {
        replace: true
      });
    } catch (err) {
      setError(err.message || "Не удалось зарегистрироваться");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-card">
      <div className="auth-card-header">
        <p className="eyebrow">Новый аккаунт</p>
        <h2>Регистрация</h2>
        <p>Создай аккаунт. После этого мы попросим подтвердить почту кодом.</p>
      </div>

      <form className="form" onSubmit={handleSubmit}>
        <label>
          Имя
          <input
            name="name"
            value={form.name}
            onChange={updateField}
            type="text"
            autoComplete="name"
            placeholder="Panco Manco"
            minLength={2}
            maxLength={100}
            required
          />
        </label>

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
            autoComplete="new-password"
            placeholder="От 6 до 20 символов"
            minLength={6}
            maxLength={20}
            required
          />
        </label>

        <FormError error={error} />

        <button className="primary-button" disabled={loading} type="submit">
          {loading ? "Создаём аккаунт..." : "Создать аккаунт"}
        </button>
      </form>

      <p className="auth-footer">
        Уже есть аккаунт? <Link to="/login">Войти</Link>
      </p>
    </div>
  );
}

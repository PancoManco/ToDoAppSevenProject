import { useState } from "react";
import { Link } from "react-router-dom";
import { KeyRound } from "lucide-react";
import { authApi } from "../../shared/api/authApi.js";
import { FormError } from "../../shared/ui/FormError.jsx";

export function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setMessage("");
    setLoading(true);

    try {
      const data = await authApi.requestPasswordReset({
        email: email.trim()
      });

      setSent(true);
      setMessage(
        data?.message ||
          "Если такой email существует, мы отправили ссылку для сброса пароля."
      );
    } catch (err) {
      setError(err.message || "Не удалось отправить ссылку");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-card">
      <div className="auth-card-header center">
        <div className="big-icon">
          <KeyRound size={30} />
        </div>
        <p className="eyebrow">Reset password</p>
        <h2>Забыли пароль?</h2>
        <p>
          Введи email. Если аккаунт существует, мы отправим ссылку для смены
          пароля.
        </p>
      </div>

      <form className="form" onSubmit={handleSubmit}>
        <label>
          Email
          <input
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            type="email"
            autoComplete="email"
            placeholder="you@example.com"
            required
          />
        </label>

        {sent && (
          <div className="form-success">
            {message}
            <br />
            Проверь почту и открой ссылку вида <code>/reset-password?token=...</code>
          </div>
        )}

        <FormError error={error} />

        <button className="primary-button" disabled={loading} type="submit">
          {loading ? "Отправляем..." : "Отправить ссылку"}
        </button>
      </form>

      <p className="auth-footer">
        Вспомнили пароль? <Link to="/login">Вернуться ко входу</Link>
      </p>
    </div>
  );
}

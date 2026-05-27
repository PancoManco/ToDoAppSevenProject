import { useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { LockKeyhole } from "lucide-react";
import { authApi } from "../../shared/api/authApi.js";
import { FormError } from "../../shared/ui/FormError.jsx";

export function ResetPasswordPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const token = useMemo(() => searchParams.get("token") || "", [searchParams]);

  const [newPassword, setNewPassword] = useState("");
  const [repeatPassword, setRepeatPassword] = useState("");
  const [success, setSuccess] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setSuccess("");

    if (!token) {
      setError("Reset token отсутствует в ссылке.");
      return;
    }

    if (newPassword !== repeatPassword) {
      setError("Пароли не совпадают.");
      return;
    }

    setLoading(true);

    try {
      const data = await authApi.resetPassword({
        token,
        newPassword
      });

      setSuccess(data?.message || "Пароль успешно изменён.");
      setNewPassword("");
      setRepeatPassword("");

      setTimeout(() => {
        navigate("/login", { replace: true });
      }, 1400);
    } catch (err) {
      setError(err.message || "Не удалось сменить пароль");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-card">
      <div className="auth-card-header center">
        <div className="big-icon">
          <LockKeyhole size={30} />
        </div>
        <p className="eyebrow">New password</p>
        <h2>Создай новый пароль</h2>
        <p>
          Эта страница открывается из письма. Token берётся из ссылки и
          отправляется на backend вместе с новым паролем.
        </p>
      </div>

      {!token && (
        <div className="form-error">
          В ссылке нет token. Запроси новую ссылку восстановления пароля.
        </div>
      )}

      <form className="form" onSubmit={handleSubmit}>
        <label>
          Новый пароль
          <input
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
            type="password"
            autoComplete="new-password"
            placeholder="От 6 до 20 символов"
            minLength={6}
            maxLength={20}
            required
          />
        </label>

        <label>
          Повтори пароль
          <input
            value={repeatPassword}
            onChange={(event) => setRepeatPassword(event.target.value)}
            type="password"
            autoComplete="new-password"
            placeholder="Повтори новый пароль"
            minLength={6}
            maxLength={20}
            required
          />
        </label>

        {success && <div className="form-success">{success}</div>}
        <FormError error={error} />

        <button className="primary-button" disabled={loading || !token} type="submit">
          {loading ? "Сохраняем..." : "Сменить пароль"}
        </button>
      </form>

      <p className="auth-footer">
        Нужна новая ссылка? <Link to="/forgot-password">Запросить снова</Link>
      </p>
    </div>
  );
}

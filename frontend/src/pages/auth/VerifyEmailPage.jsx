import { useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { MailCheck } from "lucide-react";
import { useAuth } from "../../shared/auth/AuthContext.jsx";
import { authApi } from "../../shared/api/authApi.js";
import { FormError } from "../../shared/ui/FormError.jsx";

export function VerifyEmailPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { verifyEmail } = useAuth();

  const initialEmail = useMemo(() => searchParams.get("email") || "", [searchParams]);

  const [email, setEmail] = useState(initialEmail);
  const [code, setCode] = useState("");
  const [info, setInfo] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setInfo("");
    setLoading(true);

    try {
      await verifyEmail({
        email: email.trim(),
        code: code.trim()
      });

      navigate("/app", { replace: true });
    } catch (err) {
      setError(err.message || "Неверный код подтверждения");
    } finally {
      setLoading(false);
    }
  }

  async function handleResend() {
    setError("");
    setInfo("");
    setLoading(true);

    try {
      await authApi.resendVerificationCode({
        email: email.trim()
      });

      setInfo("Новый код отправлен на почту.");
    } catch (err) {
      setError(err.message || "Не удалось отправить код повторно");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-card">
      <div className="auth-card-header center">
        <div className="big-icon">
          <MailCheck size={30} />
        </div>
        <p className="eyebrow">Подтверждение</p>
        <h2>Проверь почту</h2>
        <p>Введи 6-значный код, который мы отправили на email.</p>
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

        <label>
          Код
          <input
            value={code}
            onChange={(event) =>
              setCode(event.target.value.replace(/\D/g, "").slice(0, 6))
            }
            inputMode="numeric"
            pattern="\d{6}"
            placeholder="123456"
            required
          />
        </label>

        {info && <div className="form-success">{info}</div>}
        <FormError error={error} />

        <button className="primary-button" disabled={loading} type="submit">
          {loading ? "Проверяем..." : "Подтвердить email"}
        </button>

        <button
          className="ghost-button"
          disabled={loading || !email}
          onClick={handleResend}
          type="button"
        >
          Отправить код ещё раз
        </button>
      </form>

      <p className="auth-footer">
        Уже подтвердили? <Link to="/login">Войти</Link>
      </p>
    </div>
  );
}

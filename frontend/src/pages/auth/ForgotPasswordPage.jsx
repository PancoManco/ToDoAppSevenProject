import { useState } from "react";
import { Link } from "react-router-dom";
import { KeyRound } from "lucide-react";

export function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [submitted, setSubmitted] = useState(false);

  function handleSubmit(event) {
    event.preventDefault();
    setSubmitted(true);
  }

  return (
    <div className="auth-card">
      <div className="auth-card-header center">
        <div className="big-icon">
          <KeyRound size={30} />
        </div>
        <p className="eyebrow">Скоро</p>
        <h2>Восстановление пароля</h2>
        <p>
          Страница уже подготовлена. Позже сюда подключим backend endpoint для
          отправки кода или ссылки сброса пароля.
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

        {submitted && (
          <div className="form-success">
            Пока backend не подключён. Позже здесь будет отправка инструкции на почту.
          </div>
        )}

        <button className="primary-button" type="submit">
          Отправить инструкцию
        </button>
      </form>

      <p className="auth-footer">
        Вспомнили пароль? <Link to="/login">Вернуться ко входу</Link>
      </p>
    </div>
  );
}

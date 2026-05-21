import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../shared/auth/AuthContext.jsx";

export function OAuthSuccessPage() {
  const navigate = useNavigate();
  const { refreshSession } = useAuth();
  const [error, setError] = useState("");

  useEffect(() => {
    async function finishOAuth() {
      try {
        await refreshSession();
        navigate("/app", { replace: true });
      } catch (err) {
        setError(err.message || "OAuth login failed");
      }
    }

    finishOAuth();
  }, [navigate, refreshSession]);

  return (
    <main className="oauth-screen">
      <div className="auth-card compact">
        <p className="eyebrow">OAuth</p>
        <h2>{error ? "Ошибка входа" : "Завершаем вход..."}</h2>
        <p>
          {error ||
            "Получаем access token через refresh cookie и открываем приложение."}
        </p>
      </div>
    </main>
  );
}

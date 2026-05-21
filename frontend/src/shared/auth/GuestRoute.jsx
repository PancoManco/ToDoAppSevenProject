import { Navigate } from "react-router-dom";
import { useAuth } from "./AuthContext.jsx";

export function GuestRoute({ children }) {
  const { isAuthenticated, bootstrapping } = useAuth();

  if (bootstrapping) {
    return <div className="boot-screen">Проверяем сессию...</div>;
  }

  if (isAuthenticated) {
    return <Navigate to="/app" replace />;
  }

  return children;
}

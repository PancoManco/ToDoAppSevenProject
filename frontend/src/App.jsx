import { Navigate, Route, Routes } from "react-router-dom";
import { AppLayout } from "./widgets/layout/AppLayout.jsx";
import { AuthLayout } from "./widgets/layout/AuthLayout.jsx";
import { ProtectedRoute } from "./shared/auth/ProtectedRoute.jsx";
import { GuestRoute } from "./shared/auth/GuestRoute.jsx";
import { LoginPage } from "./pages/auth/LoginPage.jsx";
import { RegisterPage } from "./pages/auth/RegisterPage.jsx";
import { VerifyEmailPage } from "./pages/auth/VerifyEmailPage.jsx";
import { ForgotPasswordPage } from "./pages/auth/ForgotPasswordPage.jsx";
import { DashboardPage } from "./pages/app/DashboardPage.jsx";
import { TasksPage } from "./pages/app/TasksPage.jsx";
import { ProfilePage } from "./pages/app/ProfilePage.jsx";
import { OAuthSuccessPage } from "./pages/auth/OAuthSuccessPage.jsx";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/app" replace />} />

      <Route
        path="/login"
        element={
          <GuestRoute>
            <AuthLayout>
              <LoginPage />
            </AuthLayout>
          </GuestRoute>
        }
      />

      <Route
        path="/register"
        element={
          <GuestRoute>
            <AuthLayout>
              <RegisterPage />
            </AuthLayout>
          </GuestRoute>
        }
      />

      <Route
        path="/verify-email"
        element={
          <AuthLayout>
            <VerifyEmailPage />
          </AuthLayout>
        }
      />

      <Route
        path="/forgot-password"
        element={
          <GuestRoute>
            <AuthLayout>
              <ForgotPasswordPage />
            </AuthLayout>
          </GuestRoute>
        }
      />

      <Route path="/oauth-success" element={<OAuthSuccessPage />} />

      <Route
        path="/app"
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="tasks" element={<TasksPage />} />
        <Route path="profile" element={<ProfilePage />} />
      </Route>

      <Route path="*" element={<Navigate to="/app" replace />} />
    </Routes>
  );
}

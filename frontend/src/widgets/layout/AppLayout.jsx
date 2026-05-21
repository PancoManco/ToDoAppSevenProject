import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { CalendarCheck2, LayoutDashboard, LogOut, UserRound } from "lucide-react";
import { Logo } from "../../shared/ui/Logo.jsx";
import { useAuth } from "../../shared/auth/AuthContext.jsx";

export function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate("/login", { replace: true });
  }

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <Logo />

        <nav className="sidebar-nav">
          <NavLink to="/app" end>
            <LayoutDashboard size={18} />
            Обзор
          </NavLink>

          <NavLink to="/app/tasks">
            <CalendarCheck2 size={18} />
            Задачи
          </NavLink>

          <NavLink to="/app/profile">
            <UserRound size={18} />
            Профиль
          </NavLink>
        </nav>

        <button className="sidebar-logout" onClick={handleLogout} type="button">
          <LogOut size={18} />
          Выйти
        </button>
      </aside>

      <section className="app-content">
        <header className="topbar">
          <div>
            <span>Аккаунт</span>
            <strong>{user?.email || "Пользователь"}</strong>
          </div>
        </header>

        <Outlet />
      </section>
    </main>
  );
}

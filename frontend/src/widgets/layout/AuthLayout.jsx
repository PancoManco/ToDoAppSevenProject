import { Logo } from "../../shared/ui/Logo.jsx";

export function AuthLayout({ children }) {
  return (
    <main className="auth-shell">
      <section className="auth-brand">
        <Logo />
        <div>
          <p className="eyebrow">Task Planner</p>
          <h1>Планируй день спокойно, а задачи — красиво.</h1>
          <p>
            Основа приложения для личных задач, проектов, дедлайнов и будущих
            командных сценариев.
          </p>
        </div>

        <div className="brand-card">
          <span>Сегодня</span>
          <strong>5 задач</strong>
          <p>2 важных, 1 просроченная, 2 на вечер</p>
        </div>
      </section>

      <section className="auth-panel">{children}</section>
    </main>
  );
}

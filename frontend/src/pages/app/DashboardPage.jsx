import { CalendarDays, CheckCircle2, Clock3, Flame } from "lucide-react";
import { useAuth } from "../../shared/auth/AuthContext.jsx";

const todayTasks = [
  { title: "Спроектировать email verification flow", time: "10:00", priority: "high" },
  { title: "Добавить страницу задач", time: "13:30", priority: "medium" },
  { title: "Проверить refresh token rotation", time: "16:00", priority: "medium" },
  { title: "Подготовить план на завтра", time: "19:00", priority: "low" }
];

export function DashboardPage() {
  const { user } = useAuth();

  return (
    <div className="page-stack">
      <section className="dashboard-hero">
        <div>
          <p className="eyebrow">Dashboard</p>
          <h1>Привет, {user?.email || "пользователь"}.</h1>
          <p>Вот будущий центр управления задачами, дедлайнами и планами.</p>
        </div>
      </section>

      <section className="stats-grid">
        <StatCard icon={<CheckCircle2 />} label="Выполнено" value="12" />
        <StatCard icon={<Clock3 />} label="В работе" value="5" />
        <StatCard icon={<Flame />} label="Важно" value="3" />
        <StatCard icon={<CalendarDays />} label="Сегодня" value="4" />
      </section>

      <section className="content-card">
        <div className="section-header">
          <div>
            <p className="eyebrow">Today</p>
            <h2>План на сегодня</h2>
          </div>
        </div>

        <div className="task-list">
          {todayTasks.map((task) => (
            <div className="task-row" key={task.title}>
              <div>
                <strong>{task.title}</strong>
                <span>{task.time}</span>
              </div>
              <span className={`priority ${task.priority}`}>
                {task.priority}
              </span>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

function StatCard({ icon, label, value }) {
  return (
    <div className="stat-card">
      <span>{icon}</span>
      <div>
        <strong>{value}</strong>
        <p>{label}</p>
      </div>
    </div>
  );
}

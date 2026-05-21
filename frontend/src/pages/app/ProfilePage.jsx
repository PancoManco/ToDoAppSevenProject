import { ShieldCheck } from "lucide-react";
import { useAuth } from "../../shared/auth/AuthContext.jsx";

export function ProfilePage() {
  const { user } = useAuth();

  return (
    <div className="page-stack">
      <section className="content-card profile-card">
        <div className="profile-avatar">
          {(user?.email || "?").slice(0, 1).toUpperCase()}
        </div>

        <div>
          <p className="eyebrow">Protected profile</p>
          <h1>{user?.email}</h1>
          <p>
            Эта страница доступна только с валидным access token. Данные ниже
            приходят из защищённого endpoint <code>/api/me</code>.
          </p>
        </div>
      </section>

      <section className="content-card">
        <div className="secure-row">
          <ShieldCheck size={24} />
          <div>
            <strong>User ID</strong>
            <span>{user?.userId}</span>
          </div>
        </div>

        <div className="secure-row">
          <ShieldCheck size={24} />
          <div>
            <strong>Email</strong>
            <span>{user?.email}</span>
          </div>
        </div>

        <div className="secure-row">
          <ShieldCheck size={24} />
          <div>
            <strong>Roles</strong>
            <span>{user?.roles?.join(", ") || "—"}</span>
          </div>
        </div>
      </section>
    </div>
  );
}

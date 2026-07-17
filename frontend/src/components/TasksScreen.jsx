import { useEffect, useState } from 'react';
import { auth, tasks as tasksApi, token } from '../api.js';
import TaskModal from './TaskModal.jsx';

export default function TasksScreen({ onLogout }) {
  const [items, setItems] = useState([]);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(null);

  const email = token.claims().email || '';

  useEffect(() => {
    (async () => {
      try {
        setItems(await tasksApi.list());
      } catch (e) {
        if (e.status === 401) return handleLogout();
        setError(e.message);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const replace = (task) =>
      setItems((prev) => prev.map((t) => (t.id === task.id ? task : t)));

  const drop = (id) => setItems((prev) => prev.filter((t) => t.id !== id));

  function fail(e) {
    if (e.status === 401) {
      handleLogout();
      return;
    }
    setError(e.message);
  }

  async function handleCreate(e) {
    e.preventDefault();
    if (!title.trim()) return;
    setError('');
    try {
      const created = await tasksApi.create(title.trim(), description.trim());
      setItems((prev) => [created, ...prev]);
      setTitle('');
      setDescription('');
    } catch (err) {
      fail(err);
    }
  }

  async function handleToggle(task) {
    setError('');
    const optimistic = { ...task, completed: !task.completed };
    replace(optimistic);
    try {
      const saved = await tasksApi.setCompleted(task.id, optimistic.completed);
      replace(saved ?? optimistic);
    } catch (err) {
      replace(task);
      fail(err);
    }
  }

  async function handleSave(id, newTitle, newDescription) {
    setError('');
    try {
      const saved = await tasksApi.update(id, newTitle, newDescription);
      replace(saved ?? { ...items.find((t) => t.id === id), title: newTitle, description: newDescription });
    } catch (err) {
      if (err.status === 401) handleLogout();
      throw err;
    }
  }

  async function handleDelete(id) {
    setError('');
    try {
      await tasksApi.remove(id);
      drop(id);
    } catch (err) {
      if (err.status === 401) handleLogout();
      throw err;
    }
  }

  async function handleQuickDelete(id) {
    setError('');
    try {
      await tasksApi.remove(id);
      drop(id);
    } catch (err) {
      if (err.status === 401) handleLogout();
      fail(err);
    }
  }

  async function handleLogout() {
    await auth.logout();
    onLogout();
  }

  const active = items.filter((t) => !t.completed);
  const done = items.filter((t) => t.completed);

  return (
      <main className="tasks">
        <header className="tasks__head">
          <div>
            <p className="eyebrow">task tracker</p>
            <h1 className="tasks__title">Мои задачи</h1>
          </div>

          <div className="user">
            {email && <span className="user__email">{email}</span>}
            <button className="btn btn--ghost" onClick={handleLogout}>
              Выйти
            </button>
          </div>
        </header>

        <form className="composer" onSubmit={handleCreate}>
          <input
              className="input"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Что нужно сделать?"
              maxLength={200}
          />
          <input
              className="input"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Заметка (необязательно)"
              maxLength={500}
          />
          <button className="btn btn--primary" type="submit" disabled={!title.trim()}>
            Добавить
          </button>
        </form>

        {error && <p className="alert alert--error">{error}</p>}

        {loading ? (
            <p className="hint">Загружаем…</p>
        ) : (
            <div className="board">
              <Column
                  label="В работе"
                  count={active.length}
                  tasks={active}
                  emptyText="Ничего не висит. Можно выдохнуть."
                  onToggle={handleToggle}
                  onOpen={setEditing}
                  onDelete={handleQuickDelete}
              />
              <Column
                  label="Сделано"
                  count={done.length}
                  tasks={done}
                  emptyText="Пока пусто. Отметь первую задачу."
                  onToggle={handleToggle}
                  onOpen={setEditing}
                  onDelete={handleQuickDelete}
              />
            </div>
        )}

        {editing && (
            <TaskModal
                task={editing}
                onSave={handleSave}
                onDelete={handleDelete}
                onToggle={async (id, completed) => {
                  setItems(prev => prev.map(t => t.id === id ? { ...t, completed } : t));
                  setEditing(prev => prev && prev.id === id ? { ...prev, completed } : prev);

                  try {
                    await tasksApi.setCompleted(id, completed);
                  } catch (e) {
                    setItems(prev => prev.map(t => t.id === id ? { ...t, completed: !completed } : t));
                    setEditing(prev => prev && prev.id === id ? { ...prev, completed: !completed } : prev);
                    fail(e);
                  }
                }}
                onClose={() => setEditing(null)}
            />
        )}
      </main>
  );
}

function Column({ label, count, tasks, emptyText, onToggle, onOpen, onDelete }) {
  return (
      <section className="column">
        <header className="column__head">
          <h2 className="column__label">{label}</h2>
          <span className="column__count">{count}</span>
        </header>

        {tasks.length === 0 ? (
            <p className="hint hint--empty">{emptyText}</p>
        ) : (
            <ul className="list">
              {tasks.map((task) => (
                  <li key={task.id} className={`row ${task.completed ? 'row--done' : ''}`}>
                    <button
                        className="check"
                        onClick={() => onToggle(task)}
                        aria-label={task.completed ? 'Вернуть в работу' : 'Отметить сделанной'}
                    >
                      {task.completed && (
                          <svg viewBox="0 0 20 20" aria-hidden="true">
                            <path
                                d="M4 10.5l4 4 8-9"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2.4"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                            />
                          </svg>
                      )}
                    </button>

                    <button className="row__body" onClick={() => onOpen(task)}>
                      <span className="row__title">{task.title}</span>
                      {task.description && <span className="row__note">{task.description}</span>}
                    </button>

                    <button
                        type="button"
                        className="row__delete"
                        onClick={(e) => {
                          e.stopPropagation();
                          onDelete(task.id);
                        }}
                        aria-label="Удалить задачу"
                        title="Удалить"
                    >
                      ×
                    </button>
                  </li>
              ))}
            </ul>
        )}
      </section>
  );
}
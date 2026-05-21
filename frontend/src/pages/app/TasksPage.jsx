import { Plus } from "lucide-react";
import { useState } from "react";

const initialTasks = [
  {
    id: 1,
    title: "Подключить email verification на backend",
    status: "todo",
    priority: "high"
  },
  {
    id: 2,
    title: "Сделать страницу reset password",
    status: "todo",
    priority: "medium"
  },
  {
    id: 3,
    title: "Проверить OAuth Google flow",
    status: "progress",
    priority: "medium"
  },
  {
    id: 4,
    title: "Сверстать dashboard",
    status: "done",
    priority: "low"
  }
];

const columns = [
  { id: "todo", title: "Нужно сделать" },
  { id: "progress", title: "В процессе" },
  { id: "done", title: "Готово" }
];

export function TasksPage() {
  const [tasks, setTasks] = useState(initialTasks);
  const [title, setTitle] = useState("");

  function addTask(event) {
    event.preventDefault();

    const trimmed = title.trim();
    if (!trimmed) return;

    setTasks((current) => [
      {
        id: Date.now(),
        title: trimmed,
        status: "todo",
        priority: "medium"
      },
      ...current
    ]);

    setTitle("");
  }

  function moveTask(id, status) {
    setTasks((current) =>
      current.map((task) => (task.id === id ? { ...task, status } : task))
    );
  }

  return (
    <div className="page-stack">
      <section className="section-header">
        <div>
          <p className="eyebrow">Planner</p>
          <h1>Задачи</h1>
          <p>Пока задачи локальные. Позже подключим CRUD endpoints backend.</p>
        </div>
      </section>

      <form className="quick-add" onSubmit={addTask}>
        <input
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="Новая задача..."
        />
        <button type="submit">
          <Plus size={18} />
          Добавить
        </button>
      </form>

      <section className="board">
        {columns.map((column) => (
          <div className="board-column" key={column.id}>
            <h2>{column.title}</h2>

            <div className="board-items">
              {tasks
                .filter((task) => task.status === column.id)
                .map((task) => (
                  <article className="task-card" key={task.id}>
                    <span className={`priority ${task.priority}`}>
                      {task.priority}
                    </span>
                    <h3>{task.title}</h3>

                    <div className="task-actions">
                      {columns.map((nextColumn) => (
                        <button
                          key={nextColumn.id}
                          onClick={() => moveTask(task.id, nextColumn.id)}
                          type="button"
                          disabled={task.status === nextColumn.id}
                        >
                          {nextColumn.title}
                        </button>
                      ))}
                    </div>
                  </article>
                ))}
            </div>
          </div>
        ))}
      </section>
    </div>
  );
}

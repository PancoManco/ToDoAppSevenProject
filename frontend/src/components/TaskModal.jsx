import { useEffect, useState } from 'react';

/**
 * Модалка редактирования задачи.
 * Кнопки «Сохранить» нет: правки уходят на бэкенд сами, через 700 мс после
 * того, как ты перестал печатать (PUT /tasks/{id}).
 *
 * Esc и клик по фону закрывают окно.
 */
export default function TaskModal({ task, onSave, onDelete, onToggle, onClose }) {
    const [title, setTitle] = useState(task.title || '');
    const [description, setDescription] = useState(task.description || '');
    const [error, setError] = useState('');
    const [deleting, setDeleting] = useState(false);
    const [toggling, setToggling] = useState(false);

    // Esc закрывает
    useEffect(() => {
        const onKey = (e) => {
            if (e.key === 'Escape') onClose();
        };
        window.addEventListener('keydown', onKey);
        return () => window.removeEventListener('keydown', onKey);
    }, [onClose]);

    // Автосохранение: ждём паузу в наборе и отправляем PUT
    useEffect(() => {
        if (title === task.title && description === task.description) return;
        if (!title.trim()) return;

        setError('');

        const timer = setTimeout(async () => {
            try {
                await onSave(task.id, title.trim(), description.trim());
            } catch (e) {
                setError(e.message);
            }
        }, 700);

        return () => clearTimeout(timer);
    }, [title, description]);

    async function handleDelete() {
        setDeleting(true);
        try {
            await onDelete(task.id);
            onClose();
        } catch (e) {
            setError(e.message);
            setDeleting(false);
        }
    }

    async function handleToggle(e) {
        // Защита: если onToggle не передан — ничего не делаем
        if (!onToggle) return;
        e.preventDefault();
        setToggling(true);
        setError('');
        try {
            await onToggle(task.id, !task.completed);
        } catch (err) {
            setError(err.message);
        } finally {
            setToggling(false);
        }
    }

    return (
        <div className="overlay" onMouseDown={onClose}>
            <div
                className="modal"
                role="dialog"
                aria-modal="true"
                aria-label="Редактирование задачи"
                onMouseDown={(e) => e.stopPropagation()}
            >
                <div
                    className="modal__status"
                    onClick={handleToggle}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                            e.preventDefault();
                            handleToggle();
                        }
                    }}
                >
                    <input
                        type="checkbox"
                        id={`task-done-${task.id}`}
                        checked={task.completed}
                        onChange={handleToggle}
                        onClick={(e) => e.stopPropagation()}
                        className="modal__checkbox"
                        disabled={toggling}
                    />
                    <label
                        htmlFor={`task-done-${task.id}`}
                        className="modal__status-label"
                        onClick={(e) => e.preventDefault()}
                    >
                        {task.completed ? 'Задача выполнена ✓' : 'Отметить как выполненную'}
                    </label>
                </div>

                <label className="field">
                    <span className="field__label">Заголовок</span>
                    <input
                        className="input"
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        maxLength={200}
                        autoFocus
                    />
                </label>

                <label className="field">
                    <span className="field__label">Описание</span>
                    <textarea
                        className="input textarea"
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        rows={5}
                        maxLength={500}
                        placeholder="Детали, ссылки, что угодно"
                    />
                </label>

                {error && <p className="alert alert--error">{error}</p>}

                <div className="modal__actions">
                    <button type="button" className="btn btn--ghost" onClick={onClose}>
                        Закрыть
                    </button>
                    <button
                        type="button"
                        className="btn btn--danger"
                        onClick={handleDelete}
                        disabled={deleting}
                    >
                        {deleting ? 'Удаляем…' : 'Удалить задачу'}
                    </button>
                </div>
            </div>
        </div>
    );
}
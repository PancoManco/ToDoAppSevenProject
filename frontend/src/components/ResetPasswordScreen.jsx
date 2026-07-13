import { useState } from 'react';
import { auth } from '../api.js';

/**
 * Открывается, когда в адресе есть ?token=... — то есть человек пришёл
 * по ссылке из письма о сбросе пароля.
 */
export default function ResetPasswordScreen({ resetToken, onDone }) {
    const [password, setPassword] = useState('');
    const [repeat, setRepeat] = useState('');
    const [error, setError] = useState('');
    const [done, setDone] = useState(false);
    const [busy, setBusy] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault();
        if (password !== repeat) {
            setError('Пароли не совпадают');
            return;
        }
        setBusy(true);
        setError('');
        try {
            await auth.resetPassword(resetToken, password);
            setDone(true);
        } catch (err) {
            setError(err.message);
        } finally {
            setBusy(false);
        }
    }

    if (done) {
        return (
            <main className="auth">
                <header className="auth__head">
                    <p className="eyebrow">task tracker</p>
                    <h1 className="auth__title">Пароль обновлён</h1>
                    <p className="auth__sub">Теперь можно войти с новым паролем.</p>
                </header>
                <button className="btn btn--primary btn--wide" onClick={onDone}>
                    Войти
                </button>
            </main>
        );
    }

    return (
        <main className="auth">
            <header className="auth__head">
                <p className="eyebrow">task tracker</p>
                <h1 className="auth__title">Новый пароль</h1>
                <p className="auth__sub">Придумай пароль, который не забудешь.</p>
            </header>

            <form className="card" onSubmit={handleSubmit}>
                <label className="field">
                    <span className="field__label">Новый пароль</span>
                    <input
                        className="input"
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        autoComplete="new-password"
                        autoFocus
                        required
                    />
                </label>

                <label className="field">
                    <span className="field__label">Ещё раз</span>
                    <input
                        className="input"
                        type="password"
                        value={repeat}
                        onChange={(e) => setRepeat(e.target.value)}
                        autoComplete="new-password"
                        required
                    />
                </label>

                {error && <p className="alert alert--error">{error}</p>}

                <button className="btn btn--primary" type="submit" disabled={busy}>
                    {busy ? 'Сохраняем…' : 'Сохранить пароль'}
                </button>

                <div className="auth__switch">
                    <button type="button" className="link" onClick={onDone}>
                        Отмена
                    </button>
                </div>
            </form>
        </main>
    );
}
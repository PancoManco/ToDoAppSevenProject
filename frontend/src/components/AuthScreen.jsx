import { useState } from 'react';
import { auth } from '../api.js';
// import { auth, GOOGLE_LOGIN_URL } from '../api.js';

export default function AuthScreen({ onAuthenticated }) {
  const [mode, setMode] = useState('login');

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [code, setCode] = useState('');

  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);

  function switchMode(next) {
    setMode(next);
    setError('');
    setNotice('');
    setCode('');
  }

  async function run(action) {
    setBusy(true);
    setError('');
    try {
      await action();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  const handleLogin = () =>
    run(async () => {
      await auth.login(email.trim(), password);
      onAuthenticated();
    });

  const handleSignup = () =>
    run(async () => {
      await auth.register(name.trim(), email.trim(), password);
      setNotice(`Код отправлен на ${email.trim()}`);
      setMode('verify');
    });

  const handleVerify = () =>
    run(async () => {
      const accessToken = await auth.verifyEmail(email.trim(), code.trim());
      if (accessToken) {
        onAuthenticated();
      } else {
        setNotice('Почта подтверждена. Теперь войди.');
        setMode('login');
      }
    });

  const handleResend = () =>
    run(async () => {
      await auth.resendCode(email.trim());
      setNotice('Новый код отправлен');
    });

  const handleForgot = () =>
    run(async () => {
      await auth.forgotPassword(email.trim());
      setNotice('Если такая почта есть, мы отправили ссылку для сброса. Загляни в Mailpit.');
    });

  const titles = {
    login: 'С возвращением',
    signup: 'Заведи список',
    verify: 'Подтверди почту',
    forgot: 'Сброс пароля',
  };

  const subs = {
    login: 'Войди, чтобы увидеть свои задачи.',
    signup: 'Пара полей — и можно записывать дела.',
    forgot: 'Пришлём на почту ссылку, по которой можно задать новый пароль.',
  };

  return (
    <main className="auth">
      <header className="auth__head">
        <p className="eyebrow">task tracker</p>
        <h1 className="auth__title">{titles[mode]}</h1>
        <p className="auth__sub">
          {mode === 'verify' ? (
            <>
              Мы отправили шестизначный код на <b>{email}</b>.
            </>
          ) : (
            subs[mode]
          )}
        </p>
      </header>

      <form
        className="card"
        onSubmit={(e) => {
          e.preventDefault();
          if (mode === 'login') handleLogin();
          if (mode === 'signup') handleSignup();
          if (mode === 'verify') handleVerify();
          if (mode === 'forgot') handleForgot();
        }}
      >
        {mode === 'signup' && (
          <label className="field">
            <span className="field__label">Имя</span>
            <input
              className="input"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Иван"
              autoComplete="name"
              required
            />
          </label>
        )}

        {mode !== 'verify' && (
          <label className="field">
            <span className="field__label">Почта</span>
            <input
              className="input"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="ivan@example.com"
              autoComplete="email"
              required
            />
          </label>
        )}

        {(mode === 'login' || mode === 'signup') && (
          <label className="field">
            <span className="field__label">
              Пароль
              {mode === 'login' && (
                <button
                  type="button"
                  className="link link--inline"
                  onClick={() => switchMode('forgot')}
                >
                  Забыли пароль?
                </button>
              )}
            </span>
            <input
              className="input"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              required
            />
          </label>
        )}

        {mode === 'verify' && (
          <label className="field">
            <span className="field__label">Код из письма</span>
            <input
              className="input input--code"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="000000"
              inputMode="numeric"
              autoComplete="one-time-code"
              required
            />
          </label>
        )}

        {error && <p className="alert alert--error">{error}</p>}
        {notice && !error && <p className="alert alert--ok">{notice}</p>}

        <button className="btn btn--primary" type="submit" disabled={busy}>
          {busy && 'Секунду…'}
          {!busy && mode === 'login' && 'Войти'}
          {!busy && mode === 'signup' && 'Зарегистрироваться'}
          {!busy && mode === 'verify' && 'Подтвердить'}
          {!busy && mode === 'forgot' && 'Прислать ссылку'}
        </button>

        {/*{(mode === 'login' || mode === 'signup') && (*/}
        {/*  <>*/}
        {/*    <div className="divider">*/}
        {/*      <span>или</span>*/}
        {/*    </div>*/}

        {/*    /!* Обычная ссылка, а не fetch: Google-вход — это редирект браузера *!/*/}
        {/*    <a className="btn btn--google" href={GOOGLE_LOGIN_URL}>*/}
        {/*      <GoogleMark />*/}
        {/*      Продолжить с Google*/}
        {/*    </a>*/}
        {/*  </>*/}
        {/*)}*/}

        <div className="auth__switch">
          {mode === 'login' && (
            <button type="button" className="link" onClick={() => switchMode('signup')}>
              Ещё нет аккаунта? Зарегистрироваться
            </button>
          )}
          {mode === 'signup' && (
            <button type="button" className="link" onClick={() => switchMode('login')}>
              Уже есть аккаунт? Войти
            </button>
          )}
          {mode === 'verify' && (
            <>
              <button type="button" className="link" onClick={handleResend} disabled={busy}>
                Отправить код заново
              </button>
              <button type="button" className="link" onClick={() => switchMode('login')}>
                Назад ко входу
              </button>
            </>
          )}
          {mode === 'forgot' && (
            <button type="button" className="link" onClick={() => switchMode('login')}>
              Вспомнил — вернуться ко входу
            </button>
          )}
        </div>
      </form>
    </main>
  );
}

// function GoogleMark() {
//   return (
//     <svg viewBox="0 0 18 18" aria-hidden="true" className="google-mark">
//       <path
//         fill="#4285F4"
//         d="M17.64 9.2c0-.64-.06-1.25-.16-1.84H9v3.48h4.84a4.14 4.14 0 0 1-1.8 2.72v2.26h2.92c1.7-1.57 2.68-3.88 2.68-6.62z"
//       />
//       <path
//         fill="#34A853"
//         d="M9 18c2.43 0 4.47-.8 5.96-2.18l-2.92-2.26c-.8.54-1.84.86-3.04.86-2.34 0-4.32-1.58-5.03-3.7H.96v2.33A9 9 0 0 0 9 18z"
//       />
//       <path
//         fill="#FBBC05"
//         d="M3.97 10.72a5.4 5.4 0 0 1 0-3.44V4.95H.96a9 9 0 0 0 0 8.1l3.01-2.33z"
//       />
//       <path
//         fill="#EA4335"
//         d="M9 3.58c1.32 0 2.5.45 3.44 1.35l2.58-2.58C13.46.9 11.43 0 9 0A9 9 0 0 0 .96 4.95l3.01 2.33C4.68 5.16 6.66 3.58 9 3.58z"
//       />
//     </svg>
//   );
// }

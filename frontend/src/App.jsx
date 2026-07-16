import { useEffect, useState } from 'react';
import { auth, token } from './api.js';
import AuthScreen from './components/AuthScreen.jsx';
import ResetPasswordScreen from './components/ResetPasswordScreen.jsx';
import TasksScreen from './components/TasksScreen.jsx';

export default function App() {
  const [loggedIn, setLoggedIn] = useState(Boolean(token.get()));
  const [resetToken, setResetToken] = useState(null);
  const [exchanging, setExchanging] = useState(false);

  useEffect(() => {
    const url = new URL(window.location.href);


    const t = url.searchParams.get('token');
    if (t) {
      setResetToken(t);
      return;
    }


    if (url.pathname.startsWith('/oauth-success')) {
      setExchanging(true);
      auth
        .refresh()
        .then((accessToken) => setLoggedIn(Boolean(accessToken)))
        .catch(() => setLoggedIn(false))
        .finally(() => {
          setExchanging(false);
          window.history.replaceState({}, '', '/'); // убираем /oauth-success из адреса
        });
    }
  }, []);

  if (exchanging) {
    return (
      <div className="app">
        <p className="hint">Завершаем вход через Google…</p>
      </div>
    );
  }

  return (
    <div className="app">
      {resetToken ? (
        <ResetPasswordScreen
          resetToken={resetToken}
          onDone={() => {
            setResetToken(null);
            window.history.replaceState({}, '', '/');
          }}
        />
      ) : loggedIn ? (
        <TasksScreen onLogout={() => setLoggedIn(false)} />
      ) : (
        <AuthScreen onAuthenticated={() => setLoggedIn(true)} />
      )}
    </div>
  );
}

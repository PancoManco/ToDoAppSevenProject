import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { authApi } from "../api/authApi.js";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [accessToken, setAccessToken] = useState(() => localStorage.getItem("accessToken"));
  const [user, setUser] = useState(null);
  const [bootstrapping, setBootstrapping] = useState(true);

  const saveAccessToken = useCallback((token) => {
    localStorage.setItem("accessToken", token);
    setAccessToken(token);
  }, []);

  const clearSession = useCallback(() => {
    localStorage.removeItem("accessToken");
    setAccessToken(null);
    setUser(null);
  }, []);

  const loadUser = useCallback(async () => {
    const profile = await authApi.me();
    setUser(profile);
    return profile;
  }, []);

  const login = useCallback(
    async ({ email, password }) => {
      const data = await authApi.login({ email, password });
      saveAccessToken(data.accessToken);
      await loadUser();
      return data;
    },
    [loadUser, saveAccessToken]
  );

  const verifyEmail = useCallback(
    async ({ email, code }) => {
      const data = await authApi.verifyEmail({ email, code });
      saveAccessToken(data.accessToken);
      await loadUser();
      return data;
    },
    [loadUser, saveAccessToken]
  );

  const refreshSession = useCallback(async () => {
    const data = await authApi.refresh();
    saveAccessToken(data.accessToken);
    await loadUser();
    return data;
  }, [loadUser, saveAccessToken]);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      clearSession();
    }
  }, [clearSession]);

  useEffect(() => {
    let cancelled = false;

    async function bootstrap() {
      try {
        const existingToken = localStorage.getItem("accessToken");

        if (existingToken) {
          await loadUser();
          return;
        }

        await refreshSession();
      } catch {
        if (!cancelled) {
          clearSession();
        }
      } finally {
        if (!cancelled) {
          setBootstrapping(false);
        }
      }
    }

    bootstrap();

    return () => {
      cancelled = true;
    };
  }, [clearSession, loadUser, refreshSession]);

  const value = useMemo(
    () => ({
      accessToken,
      user,
      isAuthenticated: Boolean(accessToken),
      bootstrapping,
      login,
      verifyEmail,
      refreshSession,
      logout,
      clearSession,
      loadUser
    }),
    [
      accessToken,
      user,
      bootstrapping,
      login,
      verifyEmail,
      refreshSession,
      logout,
      clearSession,
      loadUser
    ]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }

  return context;
}

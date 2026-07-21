
const normalizeUrl = (value) => {
  if (!value) return '';
  return value.replace(/\/$/, '');
};

const BASE_URL = import.meta.env.VITE_API_URL || '';
const API = `${BASE_URL}/api/v1`;

//const AUTH_URL = normalizeUrl(import.meta.env.VITE_AUTH_URL);
//export const GOOGLE_LOGIN_URL = `${AUTH_URL}/oauth2/authorization/google`;

const TOKEN_KEY = 'accessToken';

export const token = {
  get: () => localStorage.getItem(TOKEN_KEY),
  save: (value) => localStorage.setItem(TOKEN_KEY, value),
  clear: () => localStorage.removeItem(TOKEN_KEY),

  claims: () => {
    const raw = localStorage.getItem(TOKEN_KEY);
    if (!raw) return {};
    try {
      const payload = raw.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const json = decodeURIComponent(
        atob(payload)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      return JSON.parse(json);
    } catch {
      return {};
    }
  },
};

function extractAccessToken(body) {
  if (!body) return null;
  return (
    body.accessToken ||
    body.access_token ||
    body.token ||
    (body.tokenPair && body.tokenPair.accessToken) ||
    null
  );
}

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

async function request(path, { method = 'GET', body } = {}) {
  const headers = { 'Content-Type': 'application/json' };

  const accessToken = token.get();
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;

  let response;
  try {
    response = await fetch(`${API}${path}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
      credentials: 'include', // refresh-токен в httpOnly cookie
    });
  }  catch {
    throw new ApiError('Бэкенд недоступен. Проверь, что приложение запущено и /api проксируется на gateway.', 0);
  }

  if (response.status === 204) return null;

  const text = await response.text();
  const data = text ? safeJson(text) : null;

  if (!response.ok) {
    throw new ApiError(errorMessage(data, response.status, method, path), response.status);
  }
  return data;
}

function safeJson(text) {
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

function errorMessage(data, status, method, path) {
  if (data) {
    if (typeof data.message === 'string') return data.message;
    if (typeof data.error === 'string') return data.error;
    if (typeof data.detail === 'string') return data.detail;
  }
  if (status === 401) return 'Неверный email или пароль';
  if (status === 429) return 'Слишком много попыток. Подожди немного';
  if (status === 404 || status === 405) {
    return `Бэкенд не знает такой эндпоинт: ${method} ${path}. Поправь api.js`;
  }
  return `Ошибка ${status}`;
}

export const auth = {
  register: (name, email, password) =>
    request('/auth/register', { method: 'POST', body: { name, email, password } }),

  verifyEmail: async (email, code) => {
    const data = await request('/auth/verify-email', {
      method: 'POST',
      body: { email, code },
    });
    const accessToken = extractAccessToken(data);
    if (accessToken) token.save(accessToken);
    return accessToken;
  },

  login: async (email, password) => {
    const data = await request('/auth/login', { method: 'POST', body: { email, password } });
    const accessToken = extractAccessToken(data);
    if (!accessToken) throw new ApiError('Бэкенд не вернул access-токен — проверь api.js', 500);
    token.save(accessToken);
    return accessToken;
  },

  resendCode: (email) =>
    request('/auth/resend-verification-code', { method: 'POST', body: { email } }),


  forgotPassword: (email) =>
    request('/auth/forgot-password', { method: 'POST', body: { email } }),


  resetPassword: (resetToken, newPassword) =>
    request('/auth/reset-password', {
      method: 'POST',
      body: { token: resetToken, newPassword },
    }),

  refresh: async () => {
    const data = await request('/auth/refresh', { method: 'POST' });
    const accessToken = extractAccessToken(data);
    if (accessToken) token.save(accessToken);
    return accessToken;
  },

  logout: async () => {
    try {
      await request('/auth/logout', { method: 'POST' });
    } finally {
      token.clear();
    }
  },
};

export const tasks = {
  list: async () => {
    const data = await request('/tasks');
    if (Array.isArray(data)) return data;
    if (data && Array.isArray(data.content)) return data.content;
    return [];
  },

  create: (title, description) =>
    request('/tasks', { method: 'POST', body: { title, description } }),

  update: (id, title, description) =>
    request(`/tasks/${id}`, { method: 'PUT', body: { title, description } }),

  setCompleted: (id, completed) =>
    request(`/tasks/${id}/${completed ? 'complete' : 'incomplete'}`, { method: 'PATCH' }),

  remove: (id) => request(`/tasks/${id}`, { method: 'DELETE' }),
};

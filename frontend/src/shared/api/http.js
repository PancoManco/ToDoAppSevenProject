import { API_BASE_URL } from "./config.js";

export class ApiError extends Error {
  constructor(message, status, body) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

export async function http(path, options = {}) {
  const token = localStorage.getItem("accessToken");

  const headers = {
    ...(options.body ? { "Content-Type": "application/json" } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers || {})
  };

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    credentials: "include",
    headers
  });

  const text = await response.text();

  let body = null;
  try {
    body = text ? JSON.parse(text) : null;
  } catch {
    body = text;
  }

  if (!response.ok) {
    const message =
      body?.error ||
      body?.message ||
      body?.errors?.code ||
      body?.errors?.email ||
      body?.errors?.password ||
      body?.errors?.name ||
      `Request failed with status ${response.status}`;

    throw new ApiError(message, response.status, body);
  }

  return body;
}

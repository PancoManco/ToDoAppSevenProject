import { http } from "./http.js";

export const authApi = {
  register(payload) {
    return http("/api/v1/auth/register", {
      method: "POST",
      body: JSON.stringify(payload)
    });
  },

  verifyEmail(payload) {
    return http("/api/v1/auth/verify-email", {
      method: "POST",
      body: JSON.stringify(payload)
    });
  },

  resendVerificationCode(payload) {
    return http("/api/v1/auth/resend-verification-code", {
      method: "POST",
      body: JSON.stringify(payload)
    });
  },

  login(payload) {
    return http("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify(payload)
    });
  },

  refresh() {
    return http("/api/v1/auth/refresh", {
      method: "POST"
    });
  },

  logout() {
    return http("/api/v1/auth/logout", {
      method: "POST"
    });
  },

  me() {
    return http("/api/me", {
      method: "GET"
    });
  }

  // Later:
  // requestPasswordReset(payload) {
  //   return http("/api/v1/auth/forgot-password", {
  //     method: "POST",
  //     body: JSON.stringify(payload)
  //   });
  // },
  //
  // resetPassword(payload) {
  //   return http("/api/v1/auth/reset-password", {
  //     method: "POST",
  //     body: JSON.stringify(payload)
  //   });
  // }
};

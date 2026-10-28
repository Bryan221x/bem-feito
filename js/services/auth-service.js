import { apiRequest, getCsrfToken, clearCsrfToken } from "./api-client.js";
export { getCsrfToken };
export function getCurrentUser() { return apiRequest("/api/auth/me", { notifyAuth: false }); }
export function getMyInstitutions() { return apiRequest("/api/me/institutions"); }
export async function login(email, password) {
  const form = new URLSearchParams({ email: email.trim().toLowerCase(), password });
  try {
    const result = await apiRequest("/api/auth/login", { method: "POST", body: form, notifyAuth: false });
    clearCsrfToken();
    await getCsrfToken();
    return result;
  } finally { form.delete("password"); }
}
export async function logout() {
  await apiRequest("/api/auth/logout", { method: "POST" });
  clearCsrfToken();
}

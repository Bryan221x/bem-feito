import { apiRequest } from "./api-client.js";

const paths = { REGISTRATION: "registration-requests", ACCESS: "access-requests" };
function path(type) { if (!paths[type]) throw new Error("Tipo de solicitação inválido."); return paths[type]; }
export const getCategories = () => apiRequest("/api/institution-categories");
export const getStates = () => apiRequest("/api/states");
export const getCities = (uf) => apiRequest(`/api/cities?state=${encodeURIComponent(uf)}`);
export const submitRegistration = (body) => apiRequest("/api/registration-requests", { method: "POST", body });
export const searchInstitutions = (q) => apiRequest(`/api/institutions/access-search?q=${encodeURIComponent(q)}`);
export const submitAccess = (body) => {
  if (!Number.isSafeInteger(body.institutionId) || body.institutionId <= 0) return Promise.reject(new Error("Selecione uma instituição real."));
  return apiRequest("/api/access-requests", { method: "POST", body });
};
export const listRequests = (type, status = "", page = 0) => apiRequest(`/api/admin/${path(type)}?page=${page}&size=20${status ? `&status=${encodeURIComponent(status)}` : ""}`);
export const getRequest = (type, id) => apiRequest(`/api/admin/${path(type)}/${encodeURIComponent(id)}`);
export const decideRequest = (type, id, action, notes, membershipRole) => {
  if (!["approve", "reject", "needs-changes", "cancel"].includes(action)) throw new Error("Decisão inválida.");
  if (action !== "approve" && !notes?.trim()) throw new Error("Informe o motivo da decisão.");
  if (type === "ACCESS" && action === "approve" && !["EDITOR", "MANAGER", "OWNER"].includes(membershipRole)) throw new Error("Selecione a permissão do sistema.");
  return apiRequest(`/api/admin/${path(type)}/${encodeURIComponent(id)}/${action}`, { method: "POST", body: { notes: notes?.trim() || null, membershipRole: type === "ACCESS" && action === "approve" ? membershipRole : null } });
};
export const reissueInvitation = (id) => apiRequest(`/api/admin/users/${encodeURIComponent(id)}/invitation`, { method: "POST" });
export const getRequestDashboard = () => apiRequest("/api/admin/requests/summary");
export const inspectInvitation = (token) => apiRequest("/api/account-invitations/inspect", { method: "POST", body: { token } });
export const acceptInvitation = (token, password, confirmPassword) => apiRequest("/api/account-invitations/accept", { method: "POST", body: { token, password, confirmPassword } });

// One in-flight mutation per form/dialog; failed requests are never retried automatically.
const busy = new WeakSet();
export async function withSubmission(owner, operation) {
  if (busy.has(owner)) return;
  busy.add(owner);
  const buttons = [...owner.querySelectorAll('button[type="submit"], [data-request-decision], [data-reissue-invitation]')];
  buttons.forEach((b) => { b.disabled = true; }); owner.setAttribute("aria-busy", "true");
  try { return await operation(); }
  finally { busy.delete(owner); buttons.forEach((b) => { b.disabled = false; }); owner.removeAttribute("aria-busy"); }
}
export function showBackendFields(form, fields, aliases = {}) {
  Object.entries(fields || {}).forEach(([key, value]) => {
    const name = aliases[key] || key;
    const input = form.elements.namedItem(name);
    input?.setAttribute("aria-invalid", "true");
    // Compare attributes without interpolating untrusted field names into selectors.
    const error = [...form.querySelectorAll("[data-error-for]")].find((e) => e.dataset.errorFor === name);
    if (error) error.textContent = value;
  });
}

import { apiRequest } from "./api-client.js";

export const getInstitution = (id) => apiRequest(`/api/management/institutions/${encodeURIComponent(id)}`);
export const patchInstitution = (id, body) => apiRequest(`/api/institutions/${encodeURIComponent(id)}`, { method: "PATCH", body });
export const getMembers = (id) => apiRequest(`/api/institutions/${encodeURIComponent(id)}/members`);
export const patchMember = (institutionId, id, body) => apiRequest(`/api/institutions/${encodeURIComponent(institutionId)}/members/${encodeURIComponent(id)}`, { method: "PATCH", body });
export const saveContact = (kind, ownerId, id, body) => apiRequest(`/api/${kind}/${encodeURIComponent(ownerId)}/contacts${id ? `/${encodeURIComponent(id)}` : ""}`, { method: id ? "PATCH" : "POST", body });
export const deleteContact = (kind, ownerId, id) => apiRequest(`/api/${kind}/${encodeURIComponent(ownerId)}/contacts/${encodeURIComponent(id)}`, { method: "DELETE" });
export const patchHistoryImage = (owner, id, body) => apiRequest(`/api/institutions/${encodeURIComponent(owner)}/history-images/${encodeURIComponent(id)}`, { method: "PATCH", body });
export const deleteHistoryImage = (owner, id) => apiRequest(`/api/institutions/${encodeURIComponent(owner)}/history-images/${encodeURIComponent(id)}`, { method: "DELETE" });
export function uploadHistoryImage(owner, file, metadata) {
  const body = new FormData(); body.append("file", file);
  body.append("altText", metadata.alt.trim()); body.append("caption", metadata.caption || ""); body.append("displayOrder", String(metadata.order));
  return apiRequest(`/api/institutions/${encodeURIComponent(owner)}/history-images/upload`, { method: "POST", body });
}

export function changedFields(original, next) {
  return Object.fromEntries(Object.entries(next).filter(([key, value]) => (original[key] ?? null) !== value));
}
export function contactPayload(contact) {
  return { type: contact.type, value: contact.type === "EMAIL" ? contact.value.trim().toLowerCase() : contact.value.trim().replace(/[^+0-9]/g, ""),
    label: contact.label?.trim() || null, isPrimary: !!contact.isPrimary, isPublic: !!contact.isPublic, displayOrder: contact.displayOrder ?? 0 };
}

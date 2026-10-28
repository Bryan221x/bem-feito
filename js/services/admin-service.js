import { apiRequest } from "./api-client.js";
export const getAdminSummary = () => apiRequest("/api/admin/summary");
export const getAdminInstitutions = (page = 0, q = "", status = "") => apiRequest(`/api/admin/institutions?${new URLSearchParams({ page, size: 20, q, ...(status ? { status } : {}) })}`);

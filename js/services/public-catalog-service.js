import { apiRequest } from "./api-client.js";
export const getAvailableCities = () => apiRequest("/api/cities/available");
export const getPublicInstitutions = (cityId, page = 0) => apiRequest(`/api/institutions?${new URLSearchParams({ page, size: 20, ...(cityId ? { cityId } : {}) })}`);
export const getPublicInstitution = (id) => apiRequest(`/api/institutions/${encodeURIComponent(id)}`);
export const getMapUnits = (cityId, page = 0) => apiRequest(`/api/units/map?${new URLSearchParams({ page, size: 100, ...(cityId ? { cityId } : {}) })}`);

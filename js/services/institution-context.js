import { getUnit } from "./unit-service.js";
import { getCurrentUser, getMyInstitutions } from "./auth-service.js";
import { getAdminInstitutions } from "./admin-service.js";
import { getInstitution } from "./institution-service.js";

const key = user => `bemfeito:institution:${user.id}`;
export function rememberInstitution(user, id) {
  if (!user?.id) return;
  try { if (id) localStorage.setItem(key(user), String(id)); else localStorage.removeItem(key(user)); } catch { /* Preferência opcional; autorização sempre vem da API. */ }
}
export function chooseInstitution(user, institutions, explicit = null) {
  let preferred;
  try { preferred = localStorage.getItem(key(user)); } catch {}
  const find = id => institutions.find(item => String(item.institutionId ?? item.id) === String(id));
  const selected = find(explicit) || find(preferred) || institutions.find(item => item.status === "ACTIVE") || institutions[0] || null;
  rememberInstitution(user, selected?.institutionId ?? selected?.id);
  return selected;
}
export async function resolveInstitutionContext({ user, explicit = new URLSearchParams(location.search).get("institutionId") } = {}) {
  user ||= await getCurrentUser();
  const unitId = new URLSearchParams(location.search).get("unitId");
  if (!explicit && unitId) explicit = String((await getUnit(unitId)).institutionId);
  let institutions;
  if (user.platformRole === "PLATFORM_ADMIN") {
    // Administração pode abrir qualquer instituição; a API valida a existência.
    if (explicit) {
      try { const institution = await getInstitution(explicit); institutions = [institution]; }
      catch (error) { if (error.status !== 404) throw error; }
    }
    if (!institutions) {
      let preferred;
      try { preferred = localStorage.getItem(key(user)); } catch {}
      if (preferred) {
        try { institutions = [await getInstitution(preferred)]; } catch (error) { if (error.status !== 404) throw error; }
      }
    }
    if (!institutions) {
      const active = await getAdminInstitutions(0, "", "ACTIVE");
      institutions = active.content.length ? active.content : (await getAdminInstitutions()).content;
    }
  } else institutions = await getMyInstitutions();
  const selected = chooseInstitution(user, institutions, explicit);
  return { user, institutions, selected, id: selected ? String(selected.institutionId ?? selected.id) : null };
}
export function applyInstitutionContext(id) {
  if (!id) return;
  const url = new URL(location.href); url.searchParams.set("institutionId", id);
  window.history.replaceState(null, "", url.pathname + url.search + url.hash);
  document.querySelectorAll('a[href]').forEach(link => {
    const target = new URL(link.href, location.href);
    if (target.origin === url.origin && /\/(my-institution|unit-form)\.html$/.test(target.pathname)) {
      target.searchParams.set("institutionId", id); link.href = target.href;
    }
  });
}

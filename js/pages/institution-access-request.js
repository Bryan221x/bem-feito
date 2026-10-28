import { getCurrentUser } from "../services/auth-service.js";
import { searchInstitutions, submitAccess, withSubmission, showBackendFields } from "../services/request-service.js";

export function initInstitutionAccessRequestPage() {
  const page = document.querySelector("[data-institution-access-request-page]");
  const form = page?.querySelector("[data-institution-access-form]"); if (!page || !form) return;
  let selected = null, sequence = 0, timer, identityReady = false;
  const query = form.elements.namedItem("institutionQuery"), results = page.querySelector("[data-search-results]");
  const feedback = page.querySelector("[data-search-feedback]"), selection = page.querySelector("[data-selected-institution]");
  const registration = page.querySelector("[data-search-register]");
  const submit = form.querySelector('button[type="submit"]'); submit.disabled = true;
  getCurrentUser().then((user) => {
    form.elements.namedItem("responsibleName").value = user.name; form.elements.namedItem("email").value = user.email;
    form.elements.namedItem("responsibleName").readOnly = true; form.elements.namedItem("email").readOnly = true;
    identityReady = true; submit.disabled = false;
  }).catch((error) => {
    if (error.status === 401) { identityReady = true; submit.disabled = false; }
    else setMessage(page, "Não foi possível verificar sua sessão. Recarregue a página antes de enviar.", true);
  });
  form.querySelector("[data-phone]")?.addEventListener("input", (e) => {
    const d = e.target.value.replace(/\D/g, "").slice(0, 11); e.target.value = d.replace(/^(\d{2})(\d)/, "($1) $2").replace(/(\d{5})(\d)/, "$1-$2");
  });
  query.addEventListener("input", () => {
    clearTimeout(timer); const current = ++sequence; selected = null; selection.hidden = true; results.replaceChildren(); registration.hidden = true;
    if (query.value.trim().length < 2) { feedback.textContent = "Digite pelo menos 2 caracteres para buscar."; return; }
    feedback.textContent = "Buscando instituições…";
    timer = setTimeout(async () => {
      try {
        const institutions = await searchInstitutions(query.value.trim()); if (current !== sequence) return;
        feedback.textContent = institutions.length ? "Selecione uma instituição:" : "Nenhuma instituição encontrada."; registration.hidden = !!institutions.length;
        results.replaceChildren(...institutions.map((institution) => {
          const item = document.createElement("li"), button = document.createElement("button"); button.type = "button"; button.className = "button button--outline";
          button.textContent = `${institution.displayName} — ${institution.category}${institution.locations.length ? ` — ${institution.locations.map((l) => `${l.city}/${l.state}`).join(", ")}` : ""}`;
          button.addEventListener("click", () => { selected = institution; selection.textContent = `Selecionada: ${institution.displayName}`; selection.hidden = false; results.replaceChildren(); feedback.textContent = "Instituição selecionada."; });
          item.append(button); return item;
        }));
      } catch (error) { if (current === sequence) { feedback.textContent = error.message || "Busca indisponível. Tente novamente."; registration.hidden = false; } }
    }, 350);
  });
  form.addEventListener("input", (e) => { e.target.removeAttribute("aria-invalid"); });
  form.addEventListener("submit", (event) => {
    event.preventDefault(); if (!identityReady) return;
    if (!selected) { setMessage(page, "Pesquise e selecione uma instituição real.", true); query.focus(); return; }
    if (!form.reportValidity()) return;
    const data = new FormData(form);
    const body = { institutionId: selected.id, requesterName: data.get("responsibleName").trim(), requesterEmail: data.get("email").trim(), requesterPhone: data.get("phone").trim(), requesterRole: data.get("role").trim(), justification: data.get("justification").trim() || null };
    return withSubmission(form, async () => {
      try { const result = await submitAccess(body); setMessage(page, `Solicitação #${result.id} enviada. Aguarde a análise da equipe Bem-Feito.`); selected = null; selection.hidden = true; query.value = ""; results.replaceChildren(); feedback.textContent = "Digite pelo menos 2 caracteres para buscar."; }
      catch (error) { showBackendFields(form, error.fields, { institutionId: "institutionQuery", requesterName: "responsibleName", requesterEmail: "email", requesterPhone: "phone", requesterRole: "role" }); setMessage(page, error.message || "Não foi possível enviar.", true); }
    });
  });
}
function setMessage(page, text, error = false) { const message = page.querySelector("[data-access-message]"); message.textContent = text; message.dataset.state = error ? "error" : "neutral"; message.hidden = false; }

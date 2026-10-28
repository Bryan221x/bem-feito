import { getCategories, getStates, getCities, submitRegistration, withSubmission, showBackendFields } from "../services/request-service.js";
let draftSequence = 0;

export function initRegistrationRequestPage() {
  const page = document.querySelector("[data-registration-request-page]");
  const form = page?.querySelector("[data-registration-request-form]");
  if (!page || !form) return;

  const state = { publicContacts: [] };
  bindMask(form.querySelector("[data-cnpj]"), formatCnpj);
  bindMask(form.querySelector("[data-phone]"), formatPhone);
  bindMask(form.querySelector("[data-postal]"), formatPostalCode);
  loadReferences(page, form);
  form.querySelector("[data-reference-retry]")?.addEventListener("click", () => loadReferences(page, form));
  form.elements.namedItem("state")?.addEventListener("change", () => loadCities(page, form));
  form.querySelector("[data-description]")?.addEventListener("input", () => updateCounter(form));
  form.addEventListener("input", (event) => clearFieldError(form, event.target.name));
  form.querySelector("[data-add-public-contact]")?.addEventListener("click", () => addPublicContact(page, state));
  form.querySelector("[data-public-contact-list]")?.addEventListener("click", (event) => removePublicContact(event, page, state));
  form.addEventListener("submit", (event) => submitRequest(event, page, state));
  updateCounter(form);
  renderPublicContacts(page, state);
}

function submitRequest(event, page, state) {
  event.preventDefault();
  const form = event.currentTarget;
  const errors = validateForm(form);
  if (!state.publicContacts.length) errors.push({ field: "publicContacts", message: "Adicione ao menos um contato público." });

  if (errors.length) {
    errors.forEach(({ field, message }) => {
      if (field === "publicContacts") {
        const error = form.querySelector("[data-public-contact-error]");
        if (error) error.textContent = message;
      } else showFieldError(form, field, message);
    });
    const firstControl = form.elements.namedItem(errors[0].field);
    (firstControl || form.querySelector("[data-public-contact-value]"))?.focus();
    setFormMessage(page, "Revise os campos destacados.", true);
    return;
  }

  const data = new FormData(form);
  const request = {
    categoryId: Number(data.get("category")), displayName: clean(data.get("displayName")), legalName: clean(data.get("legalName")),
    cnpj: clean(data.get("document")), objective: clean(data.get("objective")), description: clean(data.get("description")),
    history: clean(data.get("history")) || null, website: clean(data.get("website")) || null,
    responsible: { name: clean(data.get("responsibleName")), email: clean(data.get("responsibleEmail")), phone: clean(data.get("responsiblePhone")), role: clean(data.get("responsibleRole")) },
    publicContacts: state.publicContacts.map(({ type, value, label, isPrimary }) => ({ type, value, label, isPrimary })),
    mainUnit: { name: clean(data.get("unitName")), document: null, description: null, cityId: Number(data.get("city")), postalCode: clean(data.get("postalCode")),
      street: clean(data.get("street")), number: clean(data.get("number")), complement: clean(data.get("complement")) || null, neighborhood: clean(data.get("neighborhood")), contacts: [] },
  };
  return withSubmission(form, async () => {
    try {
      const result = await submitRegistration(request);
      form.reset(); state.publicContacts = []; renderPublicContacts(page, state); updateCounter(form); resetCities(form);
      setFormMessage(page, `Solicitação #${result.id} enviada com sucesso. Ela será analisada pela equipe Bem-Feito.`);
    } catch (error) {
      showBackendFields(form, error.fields, { cnpj: "document", categoryId: "category", "responsible.name": "responsibleName", "responsible.email": "responsibleEmail", "responsible.phone": "responsiblePhone", "responsible.role": "responsibleRole", "mainUnit.cityId": "city", "mainUnit.name": "unitName", ...Object.fromEntries(["postalCode", "street", "number", "complement", "neighborhood"].map((k) => [`mainUnit.${k}`, k])) });
      setFormMessage(page, error.message || "Não foi possível enviar. Tente novamente.", true);
    }
  });
}

function addPublicContact(page, state) {
  const type = page.querySelector("[data-public-contact-type]");
  const value = page.querySelector("[data-public-contact-value]");
  const label = page.querySelector("[data-public-contact-label]");
  const error = page.querySelector("[data-public-contact-error]");
  if (!value?.value.trim()) {
    if (error) error.textContent = "Informe o contato antes de adicionar.";
    value?.focus();
    return;
  }
  const normalized = type.value === "EMAIL" ? value.value.trim().toLowerCase() : value.value.replace(/[^+0-9]/g, "");
  if (state.publicContacts.some((c) => c.type === type.value && c.value === normalized)) { if (error) error.textContent = "Contato já adicionado."; return; }
  if (type.value === "EMAIL" ? !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalized) : !/^\+?[0-9]{8,15}$/.test(normalized)) { if (error) error.textContent = "Informe um contato válido."; return; }
  state.publicContacts.push({
    id: createDraftId(),
    type: type.value,
    value: normalized,
    label: label.value.trim() || null,
    isPrimary: state.publicContacts.length === 0,
    isPublic: true,
  });
  value.value = "";
  label.value = "";
  if (error) error.textContent = "";
  renderPublicContacts(page, state);
}

function removePublicContact(event, page, state) {
  const button = event.target.closest("[data-remove-public-contact]");
  if (!button) return;
  state.publicContacts = state.publicContacts.filter((contact) => contact.id !== button.dataset.contactId);
  if (state.publicContacts.length && !state.publicContacts.some((contact) => contact.isPrimary)) state.publicContacts[0].isPrimary = true;
  renderPublicContacts(page, state);
}

function renderPublicContacts(page, state) {
  const list = page.querySelector("[data-public-contact-list]");
  const empty = page.querySelector("[data-public-contact-empty]");
  if (!list || !empty) return;
  list.replaceChildren(...state.publicContacts.map((contact) => {
    const item = document.createElement("li");
    const text = document.createElement("span");
    text.textContent = `${getTypeLabel(contact.type)}${contact.label ? ` — ${contact.label}` : ""}: ${contact.value}`;
    const button = document.createElement("button");
    button.type = "button";
    button.className = "registration-contact-list__remove";
    button.dataset.removePublicContact = "";
    button.dataset.contactId = contact.id;
    button.textContent = "Remover";
    button.setAttribute("aria-label", `Remover ${getTypeLabel(contact.type)}`);
    item.append(text, button);
    return item;
  }));
  empty.hidden = state.publicContacts.length > 0;
}

function validateForm(form) {
  const required = ["displayName", "legalName", "document", "category", "objective", "description", "responsibleName", "responsibleRole", "responsibleEmail", "responsiblePhone", "unitName", "postalCode", "street", "number", "neighborhood", "city", "state"];
  return required.flatMap((field) => {
    const input = form.elements.namedItem(field);
    return input?.value.trim() && input.checkValidity() ? [] : [{ field, message: input?.type === "email" ? "Informe um e-mail válido." : "Campo obrigatório." }];
  });
}

function bindMask(input, formatter) { input?.addEventListener("input", () => { input.value = formatter(input.value); }); }
function formatCnpj(value) { const d = value.replace(/\D/g, "").slice(0, 14); return d.replace(/^(\d{2})(\d)/, "$1.$2").replace(/^(\d{2})\.(\d{3})(\d)/, "$1.$2.$3").replace(/\.(\d{3})(\d)/, ".$1/$2").replace(/(\d{4})(\d)/, "$1-$2"); }
function formatPhone(value) { const d = value.replace(/\D/g, "").slice(0, 11); return d.replace(/^(\d{2})(\d)/, "($1) $2").replace(/(\d{5})(\d)/, "$1-$2"); }
function formatPostalCode(value) { const d = value.replace(/\D/g, "").slice(0, 8); return d.length > 5 ? `${d.slice(0, 5)}-${d.slice(5)}` : d; }
function showFieldError(form, field, message) { form.elements.namedItem(field)?.setAttribute("aria-invalid", "true"); const error = form.querySelector(`[data-error-for="${field}"]`); if (error) error.textContent = message; }
function clearFieldError(form, field) { if (!field) return; form.elements.namedItem(field)?.removeAttribute("aria-invalid"); const error = form.querySelector(`[data-error-for="${field}"]`); if (error) error.textContent = ""; }
function updateCounter(form) { const input = form.querySelector("[data-description]"); const counter = form.querySelector("[data-description-count]"); if (input && counter) counter.textContent = `${input.value.length}/1000`; }
function setFormMessage(page, message, isError = false) { const element = page.querySelector("[data-registration-form-message]"); if (!element) return; element.textContent = message; element.dataset.state = isError ? "error" : "neutral"; element.hidden = false; }
function getTypeLabel(type) { return { EMAIL: "E-mail", PHONE: "Telefone", WHATSAPP: "WhatsApp" }[type] || "Contato"; }
function clean(value) { return String(value || "").trim(); }
function createDraftId() { return `draft-public-contact-${++draftSequence}`; }

function option(value, label) { const o = document.createElement("option"); o.value = value; o.textContent = label; return o; }
function resetCities(form) { const select = form.elements.namedItem("city"); select.replaceChildren(option("", "Selecione primeiro o estado")); select.disabled = true; }
async function loadReferences(page, form) {
  const feedback = page.querySelector("[data-reference-message]"); const retry = page.querySelector("[data-reference-retry]");
  const category = form.elements.namedItem("category"), state = form.elements.namedItem("state");
  category.disabled = state.disabled = true; resetCities(form); feedback.textContent = "Carregando categorias e estados…"; retry.hidden = true;
  try {
    const [categories, states] = await Promise.all([getCategories(), getStates()]);
    category.replaceChildren(option("", categories.length ? "Selecione a categoria" : "Nenhuma categoria disponível"), ...categories.map((c) => option(c.id, c.name)));
    state.replaceChildren(option("", states.length ? "Selecione o estado" : "Nenhum estado disponível"), ...states.map((s) => option(s.uf, `${s.name} (${s.uf})`)));
    category.disabled = !categories.length; state.disabled = !states.length;
    feedback.textContent = categories.length && states.length ? "" : "Opções indisponíveis. Tente novamente."; retry.hidden = !!(categories.length && states.length);
  } catch { feedback.textContent = "Não foi possível carregar categorias e estados."; retry.hidden = false; }
}
let citySequence = 0;
async function loadCities(page, form) {
  const sequence = ++citySequence; const uf = form.elements.namedItem("state").value; resetCities(form); if (!uf) return;
  const city = form.elements.namedItem("city"), feedback = page.querySelector("[data-reference-message]"); feedback.textContent = "Carregando municípios…";
  try {
    const cities = await getCities(uf); if (sequence !== citySequence) return;
    city.replaceChildren(option("", cities.length ? "Selecione a cidade" : "Nenhuma cidade disponível"), ...cities.map((c) => option(c.id, c.name)));
    city.disabled = !cities.length; feedback.textContent = cities.length ? "" : "Nenhuma cidade disponível para esta UF.";
  } catch { if (sequence === citySequence) feedback.textContent = "Não foi possível carregar municípios. Selecione o estado novamente."; }
}

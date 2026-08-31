const pageStates = new WeakMap();
const GENERAL_FIELDS = [
  "displayName",
  "legalName",
  "document",
  "category",
  "objective",
  "description",
  "website",
];

export function initMyInstitutionPage() {
  const page = document.querySelector("[data-my-institution-page]");
  if (!page) return;

  const state = {
    institutions: [],
    selectedInstitutionId: null,
    requestedInstitutionId: getQueryIdentifier("institutionId"),
    contacts: [],
    contactChanges: [],
  };

  pageStates.set(page, state);
  bindPageActions(page, state);
  renderMyInstitutionPage(page, [], state.requestedInstitutionId);
}

/* Ponto de entrada para a futura camada de dados da conta autenticada. */
export function renderMyInstitutionPage(page, institutions, selectedInstitutionId = null) {
  const state = pageStates.get(page);
  if (!state || !Array.isArray(institutions)) return;

  state.institutions = institutions.filter(hasRealInstitutionIdentity);
  const requestedId =
    normalizeIdentifier(selectedInstitutionId) || state.requestedInstitutionId;
  const selected = state.institutions.find((item) => String(item.id) === requestedId) || null;
  state.selectedInstitutionId = selected ? String(selected.id) : null;
  state.contacts = selected && Array.isArray(selected.contacts) ? selected.contacts.map(normalizeContact).filter(Boolean) : [];
  state.contactChanges = [];

  renderSelector(page, state);
  renderInstitution(page, state, selected);
}

function bindPageActions(page, state) {
  page.querySelector("[data-institution-selector]")?.addEventListener("change", (event) => {
    const institutionId = normalizeIdentifier(event.target.value);
    state.requestedInstitutionId = institutionId;
    renderMyInstitutionPage(page, state.institutions, institutionId);
    if (!institutionId) return;
    page.dispatchEvent(new CustomEvent("bemfeito:institution-context-selected", {
      bubbles: true,
      detail: { institutionId },
    }));
  });

  page.querySelector("[data-edit-general]")?.addEventListener("click", () => openGeneralForm(page, state));
  page.querySelector("[data-edit-reminder]")?.addEventListener("click", () => openGeneralForm(page, state));
  page.querySelector("[data-cancel-general]")?.addEventListener("click", () => closeGeneralForm(page));
  page.querySelector("[data-general-form]")?.addEventListener("submit", (event) => prepareGeneralChanges(event, page, state));
  page.querySelector("[data-add-contact]")?.addEventListener("click", () => openContactForm(page));
  page.querySelector("[data-cancel-contact]")?.addEventListener("click", () => closeContactForm(page));
  page.querySelector("[data-contact-form]")?.addEventListener("submit", (event) => prepareContact(event, page, state));
  page.querySelector("[data-contact-list]")?.addEventListener("click", (event) => handleContactAction(event, page, state));
}

function renderSelector(page, state) {
  const selector = page.querySelector("[data-institution-selector]");
  if (!selector) return;

  selector.replaceChildren();
  if (!state.institutions.length) {
    selector.add(new Option("Nenhuma instituição disponível", ""));
    selector.disabled = true;
    return;
  }

  selector.add(new Option("Selecione uma instituição", ""));
  state.institutions.forEach((institution) => {
    const option = new Option(getDisplayName(institution), String(institution.id));
    option.selected = String(institution.id) === state.selectedInstitutionId;
    selector.add(option);
  });
  selector.disabled = false;
}

function renderInstitution(page, state, institution) {
  const enabled = Boolean(institution);
  page.querySelectorAll("[data-requires-institution]").forEach((control) => {
    if (control instanceof HTMLButtonElement) control.disabled = !enabled;
    control.setAttribute("aria-disabled", String(!enabled));
    if (control instanceof HTMLAnchorElement) control.tabIndex = enabled ? 0 : -1;
  });

  renderPublicLink(page, institution);
  if (!institution) {
    renderEmptyInstitution(page);
    return;
  }

  setText(page, "[data-institution-name]", getDisplayName(institution));
  renderStatus(page, institution);
  setText(page, "[data-institution-document]", displayValue(institution.document));
  setText(page, "[data-institution-location]", getMainLocation(institution));
  setText(page, "[data-institution-created-at]", formatDate(institution.createdAt));
  setText(page, "[data-institution-updated-at]", formatDate(institution.updatedAt));
  page.querySelector("[data-institution-empty]")?.setAttribute("hidden", "");

  GENERAL_FIELDS.forEach((field) => {
    const selector = `[data-general-${toDataName(field)}]`;
    setText(page, selector, displayValue(institution[field] ?? (field === "displayName" ? institution.name : null)));
  });

  renderContacts(page, state.contacts);
  renderUnits(page, institution);
  setText(page, "[data-summary-contacts]", String(state.contacts.length));
  setText(page, "[data-summary-units]", String(Array.isArray(institution.units) ? institution.units.length : 0));
  setText(page, "[data-summary-needs]", String(countActiveNeeds(institution.units)));
  closeGeneralForm(page);
  closeContactForm(page);
  hideFeedback(page);
}

function renderEmptyInstitution(page) {
  setText(page, "[data-institution-name]", "Nenhuma instituição selecionada");
  setText(page, "[data-institution-status]", "Status indisponível");
  const status = page.querySelector("[data-institution-status]");
  status?.removeAttribute("data-status");
  ["document", "location", "created-at", "updated-at"].forEach((key) => setText(page, `[data-institution-${key}]`, "Não disponível"));
  GENERAL_FIELDS.forEach((field) => setText(page, `[data-general-${toDataName(field)}]`, "Não informado"));
  page.querySelector("[data-institution-empty]")?.removeAttribute("hidden");
  renderContacts(page, []);
  renderUnits(page, null);
  ["contacts", "units", "needs"].forEach((key) => setText(page, `[data-summary-${key}]`, "—"));
  closeGeneralForm(page);
  closeContactForm(page);
}

function renderPublicLink(page, institution) {
  const link = page.querySelector("[data-public-page-link]");
  if (!link) return;
  const publicUrl = institution?.publicUrl || (hasRealInstitutionIdentity(institution) ? `./institution.html?institutionId=${encodeURIComponent(institution.id)}` : null);
  if (!publicUrl) {
    link.removeAttribute("href");
    link.setAttribute("aria-disabled", "true");
    link.tabIndex = -1;
    return;
  }
  link.href = publicUrl;
  link.setAttribute("aria-disabled", "false");
  link.tabIndex = 0;
}

function openGeneralForm(page, state) {
  const institution = getSelectedInstitution(state);
  const form = page.querySelector("[data-general-form]");
  if (!institution || !form) return;
  GENERAL_FIELDS.forEach((field) => {
    const control = form.elements.namedItem(field);
    if (control) control.value = institution[field] ?? (field === "displayName" ? institution.name || "" : "");
  });
  page.querySelector("[data-general-view]")?.setAttribute("hidden", "");
  form.hidden = false;
  form.elements.namedItem("displayName")?.focus();
}

function closeGeneralForm(page) {
  page.querySelector("[data-general-view]")?.removeAttribute("hidden");
  const form = page.querySelector("[data-general-form]");
  if (form) form.hidden = true;
}

function prepareGeneralChanges(event, page, state) {
  event.preventDefault();
  const form = event.currentTarget;
  const institution = getSelectedInstitution(state);
  if (!institution || !form.reportValidity()) return;
  const changes = {};
  GENERAL_FIELDS.forEach((field) => {
    const next = form.elements.namedItem(field)?.value.trim() || "";
    const current = String(institution[field] ?? (field === "displayName" ? institution.name || "" : "")).trim();
    if (next !== current) changes[field] = next || null;
  });
  if (!Object.keys(changes).length) {
    showFeedback(page, "Nenhuma alteração foi informada.");
    return;
  }
  page.dispatchEvent(new CustomEvent("bemfeito:institution-partial-update-requested", {
    bubbles: true,
    detail: { institutionId: institution.id, changes },
  }));
  showFeedback(page, "As alterações parciais foram preparadas. A gravação dependerá da integração com o sistema.");
  closeGeneralForm(page);
}

function openContactForm(page, contact = null) {
  const form = page.querySelector("[data-contact-form]");
  if (!form) return;
  form.reset();
  form.elements.contactId.value = contact?.id || "";
  form.elements.type.value = contact?.type || "EMAIL";
  form.elements.label.value = contact?.label || "";
  form.elements.value.value = contact?.value || "";
  form.elements.isPrimary.checked = Boolean(contact?.isPrimary);
  form.elements.isPublic.checked = Boolean(contact?.isPublic);
  setText(page, "[data-contact-form-title]", contact ? "Editar contato" : "Adicionar contato");
  form.hidden = false;
  form.elements.type.focus();
}

function closeContactForm(page) {
  const form = page.querySelector("[data-contact-form]");
  if (!form) return;
  form.hidden = true;
  form.reset();
}

function prepareContact(event, page, state) {
  event.preventDefault();
  const form = event.currentTarget;
  const institution = getSelectedInstitution(state);
  if (!institution || !form.reportValidity()) return;
  const contactId = normalizeIdentifier(form.elements.contactId.value);
  const contact = {
    id: contactId || createDraftId(),
    type: form.elements.type.value,
    value: form.elements.value.value.trim(),
    label: form.elements.label.value.trim() || null,
    isPrimary: form.elements.isPrimary.checked,
    isPublic: form.elements.isPublic.checked,
  };
  const action = contactId ? "update" : "create";
  state.contacts = action === "update" ? state.contacts.map((item) => String(item.id) === contactId ? contact : item) : [...state.contacts, contact];
  state.contactChanges.push({ action, contact });
  renderContacts(page, state.contacts);
  closeContactForm(page);
  dispatchContactChanges(page, institution, state);
}

function handleContactAction(event, page, state) {
  const button = event.target.closest("[data-contact-action]");
  if (!button) return;
  const contactId = button.dataset.contactId;
  const contact = state.contacts.find((item) => String(item.id) === contactId);
  if (!contact) return;
  if (button.dataset.contactAction === "edit") {
    openContactForm(page, contact);
    return;
  }
  state.contacts = state.contacts.filter((item) => String(item.id) !== contactId);
  state.contactChanges.push({ action: "remove", contactId });
  renderContacts(page, state.contacts);
  dispatchContactChanges(page, getSelectedInstitution(state), state);
}

function dispatchContactChanges(page, institution, state) {
  page.dispatchEvent(new CustomEvent("bemfeito:institution-contacts-change-requested", {
    bubbles: true,
    detail: { institutionId: institution.id, contactChanges: [...state.contactChanges] },
  }));
  showFeedback(page, "A alteração de contatos foi preparada. A gravação dependerá da integração com o sistema.");
}

function renderContacts(page, contacts) {
  const list = page.querySelector("[data-contact-list]");
  const empty = page.querySelector("[data-contacts-empty]");
  if (!list || !empty) return;
  list.replaceChildren(...contacts.map(createContactItem));
  empty.hidden = contacts.length > 0;
}

function createContactItem(contact) {
  const item = document.createElement("article");
  item.className = "my-institution-contact";
  const icon = document.createElement("span");
  icon.className = "my-institution-contact__icon";
  icon.setAttribute("aria-hidden", "true");
  icon.innerHTML = '<svg viewBox="0 0 24 24"><path d="M4 4h16v16H4z"></path><path d="m4 6 8 6 8-6"></path></svg>';
  const content = document.createElement("div");
  content.className = "my-institution-contact__content";
  const title = document.createElement("strong");
  title.textContent = contact.label || getContactTypeLabel(contact.type);
  const value = document.createElement("span");
  value.textContent = contact.value;
  const meta = document.createElement("small");
  meta.className = "my-institution-contact__meta";
  meta.textContent = [contact.isPrimary ? "Principal" : null, contact.isPublic ? "Público" : "Privado"].filter(Boolean).join(" • ");
  content.append(title, value, meta);
  const actions = document.createElement("div");
  actions.className = "my-institution-contact__actions";
  actions.append(createActionButton("edit", contact.id, "Editar contato"), createActionButton("remove", contact.id, "Remover contato"));
  item.append(icon, content, actions);
  return item;
}

function renderUnits(page, institution) {
  const list = page.querySelector("[data-unit-list]");
  const empty = page.querySelector("[data-units-empty]");
  const addLink = page.querySelector("[data-add-unit]");
  if (!list || !empty || !addLink) return;
  const units = Array.isArray(institution?.units) ? institution.units.filter((unit) => normalizeIdentifier(unit?.id)) : [];
  list.replaceChildren(...units.map((unit) => createUnitItem(institution.id, unit)));
  empty.hidden = units.length > 0;
  if (institution) addLink.href = buildUnitUrl(institution.id);
  else addLink.removeAttribute("href");
}

function createUnitItem(institutionId, unit) {
  const item = document.createElement("article");
  item.className = "my-institution-unit";
  const content = document.createElement("div");
  content.className = "my-institution-unit__content";
  const name = document.createElement("strong");
  name.textContent = unit.name || "Unidade sem nome";
  const location = document.createElement("span");
  location.textContent = getUnitLocation(unit);
  content.append(name, location);
  const edit = document.createElement("a");
  edit.className = "button button--outline my-institution-unit__edit";
  edit.textContent = "Editar";
  edit.href = buildUnitUrl(institutionId, unit.id);
  item.append(content, edit);
  return item;
}

function createActionButton(action, contactId, label) {
  const button = document.createElement("button");
  button.type = "button";
  button.className = `my-institution-contact__action${action === "remove" ? " my-institution-contact__action--danger" : ""}`;
  button.dataset.contactAction = action;
  button.dataset.contactId = String(contactId);
  button.setAttribute("aria-label", label);
  button.innerHTML = action === "edit" ? '<svg viewBox="0 0 24 24"><path d="M12 20h9"></path><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z"></path></svg>' : '<svg viewBox="0 0 24 24"><path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6"></path></svg>';
  return button;
}

function renderStatus(page, institution) {
  const element = page.querySelector("[data-institution-status]");
  if (!element) return;
  const status = String(institution.status || "unknown").toLowerCase();
  const labels = { active: "Ativa", inactive: "Inativa", pending: "Em análise" };
  element.dataset.status = status;
  element.textContent = institution.statusLabel || labels[status] || "Status não informado";
}

function hasRealInstitutionIdentity(institution) {
  return Boolean(normalizeIdentifier(institution?.id) && getDisplayName(institution));
}

function normalizeContact(contact) {
  if (!contact?.value) return null;
  return {
    id: normalizeIdentifier(contact.id) || createDraftId(),
    type: String(contact.type || "EMAIL").toUpperCase(),
    value: String(contact.value),
    label: contact.label ? String(contact.label) : null,
    isPrimary: Boolean(contact.isPrimary),
    isPublic: Boolean(contact.isPublic),
  };
}

function getSelectedInstitution(state) {
  return state.institutions.find((item) => String(item.id) === state.selectedInstitutionId) || null;
}

function getDisplayName(institution) {
  return String(institution?.displayName || institution?.name || "").trim();
}

function getMainLocation(institution) {
  const unit = Array.isArray(institution.units) ? institution.units[0] : null;
  const city = institution.city || unit?.address?.city || unit?.city;
  const state = institution.state || unit?.address?.state || unit?.state;
  return [city, state].filter(Boolean).join(" - ") || "Não disponível";
}

function getUnitLocation(unit) {
  const address = unit.address || {};
  return [address.city || unit.city, address.state || unit.state].filter(Boolean).join(" - ") || "Localização não informada";
}

function countActiveNeeds(units) {
  if (!Array.isArray(units)) return 0;
  return units.reduce((total, unit) => total + (Array.isArray(unit.needs) ? unit.needs.filter((need) => String(need?.status || "ACTIVE").toUpperCase() === "ACTIVE").length : 0), 0);
}

function buildUnitUrl(institutionId, unitId = null) {
  const params = new URLSearchParams({ institutionId: String(institutionId) });
  if (unitId) params.set("unitId", String(unitId));
  return `./unit-form.html?${params.toString()}`;
}

function getQueryIdentifier(key) {
  return normalizeIdentifier(new URLSearchParams(window.location.search).get(key));
}

function normalizeIdentifier(value) {
  if (value === null || value === undefined) return null;
  return String(value).trim() || null;
}

function formatDate(value) {
  if (!value) return "Não disponível";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Não disponível" : new Intl.DateTimeFormat("pt-BR").format(date);
}

function displayValue(value) {
  return value === null || value === undefined || String(value).trim() === "" ? "Não informado" : String(value);
}

function getContactTypeLabel(type) {
  return { EMAIL: "E-mail", PHONE: "Telefone", WHATSAPP: "WhatsApp" }[String(type).toUpperCase()] || "Contato";
}

function toDataName(value) {
  return value.replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`);
}

function createDraftId() {
  return `draft-contact-${typeof crypto !== "undefined" && crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`}`;
}

function setText(page, selector, value) {
  const element = page.querySelector(selector);
  if (element) element.textContent = value;
}

function showFeedback(page, message) {
  const feedback = page.querySelector("[data-my-institution-feedback]");
  if (!feedback) return;
  feedback.textContent = message;
  feedback.hidden = false;
}

function hideFeedback(page) {
  const feedback = page.querySelector("[data-my-institution-feedback]");
  if (!feedback) return;
  feedback.hidden = true;
  feedback.textContent = "";
}

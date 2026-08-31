const unitFormStates = new WeakMap();
const UNIT_FIELDS = ["name", "document", "description", "postalCode", "address", "number", "complement", "neighborhood", "city", "state", "serviceAreasObservations"];
const ADDRESS_FIELDS = new Set(["postalCode", "address", "number", "complement", "neighborhood", "city", "state"]);

export function initUnitFormPage() {
  const page = document.querySelector("[data-unit-form-page]");
  if (!page) return;
  const params = new URLSearchParams(window.location.search);
  const unitId = normalizeIdentifier(params.get("unitId"));
  const state = {
    currentStep: 1,
    mode: unitId ? "edit" : "create",
    unitId,
    institutionId: null,
    requestedInstitutionId: normalizeIdentifier(params.get("institutionId")),
    institutions: [], contacts: [], contactChanges: [], needs: [], needChanges: [],
    dirtyFields: new Set(), serviceAreasChanged: false,
  };
  unitFormStates.set(page, state);
  configureMode(page, state);
  bindInstitutionSelector(page, state);
  bindSteps(page, state);
  bindFields(page, state);
  bindPostalControls(page);
  bindContactEditor(page, state);
  bindNeedEditor(page, state);
  bindSubmit(page, state);
  updateObservationCounter(page);
  renderContacts(page, state.contacts);
  renderNeeds(page, state.needs);
  updateStep(page, state);
  updateSubmission(page, state);
}

/* Recebe futuramente as instituições permitidas e, no modo de edição, a unidade real. */
export function renderUnitFormContext(page, { institutions = [], selectedInstitutionId = null, unit = null } = {}) {
  const state = unitFormStates.get(page);
  if (!state || !Array.isArray(institutions)) return;
  state.institutions = institutions.filter((institution) => normalizeIdentifier(institution?.id) && getInstitutionName(institution));
  renderInstitutionOptions(page, state, selectedInstitutionId);
  if (unit?.id) {
    state.mode = "edit";
    state.unitId = String(unit.id);
    const ownerId = normalizeIdentifier(unit.institutionId);
    if (ownerId && state.institutions.some((institution) => String(institution.id) === ownerId)) state.institutionId = ownerId;
    const selector = page.querySelector("[data-unit-institution-selector]");
    if (selector) selector.value = state.institutionId || "";
    configureMode(page, state);
    fillUnit(page, state, unit);
  }
  updateSubmission(page, state);
}

export function showUnitFormLoading(page) { if (page) setFeedback(page, "Carregando os dados da unidade..."); }
export function showUnitFormError(page) { if (page) setFeedback(page, "Não foi possível carregar os dados da unidade."); }

function configureMode(page, state) {
  const editing = state.mode === "edit";
  const title = editing ? "Editar unidade" : "Adicionar unidade";
  document.title = `${title} | Bem-Feito`;
  setText(page, "[data-unit-page-title]", title);
  setText(page, "[data-unit-breadcrumb-current]", title);
  setText(page, "[data-unit-page-description]", editing ? "Altere apenas os campos desejados. Os demais dados serão preservados." : "Preencha os dados disponíveis para cadastrar uma unidade.");
  setText(page, "[data-submit-unit]", editing ? "Preparar alterações" : "Preparar cadastro");
  const name = page.querySelector('[name="name"]');
  if (name) name.required = !editing;
}

function renderInstitutionOptions(page, state, selectedInstitutionId) {
  const selector = page.querySelector("[data-unit-institution-selector]");
  if (!selector) return;
  selector.replaceChildren(new Option(state.institutions.length ? "Selecione uma instituição" : "Nenhuma instituição disponível", ""));
  state.institutions.forEach((institution) => selector.add(new Option(getInstitutionName(institution), String(institution.id))));
  const wanted = normalizeIdentifier(selectedInstitutionId) || state.requestedInstitutionId;
  const selected = state.institutions.find((institution) => String(institution.id) === wanted) || null;
  state.institutionId = selected ? String(selected.id) : null;
  selector.value = state.institutionId || "";
  selector.disabled = state.institutions.length === 0;
  updateSubmission(page, state);
}

function bindInstitutionSelector(page, state) {
  page.querySelector("[data-unit-institution-selector]")?.addEventListener("change", (event) => {
    state.institutionId = normalizeIdentifier(event.target.value);
    updateSubmission(page, state);
  });
}

function bindSteps(page, state) {
  page.querySelector("[data-previous-step]")?.addEventListener("click", () => {
    if (state.currentStep === 1) { window.location.href = buildReturnUrl(state.institutionId); return; }
    state.currentStep -= 1; updateStep(page, state, true);
  });
  page.querySelector("[data-next-step]")?.addEventListener("click", () => {
    if (!validateStep(page, state)) return;
    state.currentStep = Math.min(3, state.currentStep + 1); updateStep(page, state, true);
  });
}

function updateStep(page, state, focusStep = false) {
  page.querySelectorAll("[data-unit-step]").forEach((step) => { step.hidden = Number(step.dataset.unitStep) !== state.currentStep; });
  page.querySelectorAll("[data-step-indicator]").forEach((indicator) => {
    const number = Number(indicator.dataset.stepIndicator);
    indicator.classList.toggle("is-active", number === state.currentStep);
    indicator.classList.toggle("is-complete", number < state.currentStep);
    if (number === state.currentStep) indicator.setAttribute("aria-current", "step"); else indicator.removeAttribute("aria-current");
    const badge = indicator.querySelector(".unit-stepper__number"); if (badge) badge.textContent = number < state.currentStep ? "✓" : String(number);
  });
  const next = page.querySelector("[data-next-step]");
  const submit = page.querySelector("[data-submit-unit]");
  if (next) next.hidden = state.currentStep === 3;
  if (submit) submit.hidden = state.currentStep !== 3;
  if (focusStep) {
    page
      .querySelector(`[data-unit-step="${state.currentStep}"]`)
      ?.focus({ preventScroll: true });
  }
}

function validateStep(page, state) {
  if (state.currentStep !== 1 || state.mode === "edit") return true;
  const name = page.querySelector('[name="name"]');
  if (name?.value.trim()) return true;
  showFieldError(page, "name", "Informe o nome da unidade."); name?.focus(); return false;
}

function bindFields(page, state) {
  const form = page.querySelector("[data-unit-form]");
  UNIT_FIELDS.forEach((field) => form?.elements.namedItem(field)?.addEventListener("input", () => {
    state.dirtyFields.add(field); clearFieldError(page, field);
    if (field === "serviceAreasObservations") updateObservationCounter(page);
  }));
  page.querySelectorAll('[name="serviceAreas"]').forEach((checkbox) => checkbox.addEventListener("change", () => { state.serviceAreasChanged = true; }));
  const documentInput = page.querySelector("[data-unit-document]");
  documentInput?.addEventListener("input", () => { documentInput.value = formatCnpj(documentInput.value); });
}

function bindPostalControls(page) {
  const postal = page.querySelector("[data-postal-code]");
  postal?.addEventListener("input", () => { const digits = postal.value.replace(/\D/g, "").slice(0, 8); postal.value = digits.length > 5 ? `${digits.slice(0, 5)}-${digits.slice(5)}` : digits; });
  const state = page.querySelector("[data-state-input]");
  state?.addEventListener("input", () => { state.value = state.value.replace(/[^a-z]/gi, "").toUpperCase(); });
  page.querySelector("[data-search-postal-code]")?.addEventListener("click", () => setFeedback(page, "A busca de CEP será disponibilizada após a integração com um serviço de endereços."));
}

function bindContactEditor(page, state) {
  const form = page.querySelector("[data-unit-contact-form]");
  page.querySelector("[data-add-unit-contact]")?.addEventListener("click", () => openContactEditor(page));
  page.querySelector("[data-cancel-unit-contact]")?.addEventListener("click", () => closeContactEditor(page));
  page.querySelector("[data-unit-contact-list]")?.addEventListener("click", (event) => {
    const button = event.target.closest("[data-unit-contact-action]"); if (!button) return;
    const contact = state.contacts.find((item) => String(item.id) === button.dataset.contactId); if (!contact) return;
    if (button.dataset.unitContactAction === "edit") openContactEditor(page, contact); else removeContact(page, state, contact.id);
  });
  form?.addEventListener("submit", (event) => {
    event.preventDefault(); if (!form.reportValidity()) return;
    const id = normalizeIdentifier(form.elements.contactId.value);
    const contact = { id: id || createDraftId("contact"), type: form.elements.contactType.value, label: form.elements.contactLabel.value.trim() || null, value: form.elements.contactValue.value.trim(), isPrimary: form.elements.contactIsPrimary.checked, isPublic: form.elements.contactIsPublic.checked };
    const action = id ? "update" : "create";
    state.contacts = action === "update" ? state.contacts.map((item) => String(item.id) === id ? contact : item) : [...state.contacts, contact];
    state.contactChanges.push({ action, contact }); renderContacts(page, state.contacts); closeContactEditor(page);
  });
}

function openContactEditor(page, contact = null) {
  const editor = page.querySelector("[data-unit-contact-editor]"); const form = page.querySelector("[data-unit-contact-form]"); if (!editor || !form) return;
  form.reset(); form.elements.contactId.value = contact?.id || ""; form.elements.contactType.value = contact?.type || "PHONE"; form.elements.contactLabel.value = contact?.label || ""; form.elements.contactValue.value = contact?.value || ""; form.elements.contactIsPrimary.checked = Boolean(contact?.isPrimary); form.elements.contactIsPublic.checked = Boolean(contact?.isPublic);
  setText(page, "[data-unit-contact-editor-title]", contact ? "Editar contato" : "Adicionar contato"); editor.hidden = false; form.elements.contactType.focus();
}
function closeContactEditor(page) { const editor = page.querySelector("[data-unit-contact-editor]"); if (editor) editor.hidden = true; page.querySelector("[data-unit-contact-form]")?.reset(); }
function removeContact(page, state, id) { state.contacts = state.contacts.filter((item) => String(item.id) !== String(id)); state.contactChanges.push({ action: "remove", contactId: id }); renderContacts(page, state.contacts); }
function renderContacts(page, contacts) { const list = page.querySelector("[data-unit-contact-list]"); const empty = page.querySelector("[data-unit-contacts-empty]"); if (!list || !empty) return; list.replaceChildren(...contacts.map(createContactItem)); empty.hidden = contacts.length > 0; }
function createContactItem(contact) { const item = document.createElement("article"); item.className = "unit-contact-item"; const content = document.createElement("div"); content.className = "unit-contact-item__content"; const title = document.createElement("strong"); title.textContent = contact.label || getContactTypeLabel(contact.type); const value = document.createElement("span"); value.textContent = contact.value; const meta = document.createElement("small"); meta.textContent = [contact.isPrimary ? "Principal" : null, contact.isPublic ? "Público" : "Privado"].filter(Boolean).join(" • "); content.append(title, value, meta); const actions = document.createElement("div"); actions.className = "unit-contact-item__actions"; actions.append(createCollectionAction("edit", contact.id, "Editar contato", "unitContactAction"), createCollectionAction("remove", contact.id, "Remover contato", "unitContactAction")); item.append(content, actions); return item; }

function bindNeedEditor(page, state) {
  page.querySelector("[data-add-unit-need]")?.addEventListener("click", () => openNeedEditor(page));
  page.querySelector("[data-cancel-unit-need]")?.addEventListener("click", () => closeNeedEditor(page));
  page.querySelector("[data-save-unit-need]")?.addEventListener("click", () => saveNeed(page, state));
  page.querySelector("[data-unit-need-list]")?.addEventListener("click", (event) => {
    const button = event.target.closest("[data-need-action]"); if (!button) return;
    const need = state.needs.find((item) => String(item.id) === button.dataset.needId); if (!need) return;
    if (button.dataset.needAction === "edit") openNeedEditor(page, need); else removeNeed(page, state, need.id);
  });
}
function openNeedEditor(page, need = null) { const editor = page.querySelector("[data-unit-need-editor]"); if (!editor) return; editor.querySelector('[name="needId"]').value = need?.id || ""; editor.querySelector('[name="needName"]').value = need?.name || ""; editor.querySelector('[name="needDescription"]').value = need?.description || ""; editor.querySelector('[name="needPriority"]').value = need?.priority || "MEDIUM"; editor.querySelector('[name="needStatus"]').value = need?.status || "ACTIVE"; setText(page, "[data-unit-need-editor-title]", need ? "Editar necessidade" : "Adicionar necessidade"); editor.hidden = false; editor.querySelector('[name="needName"]')?.focus(); }
function closeNeedEditor(page) { const editor = page.querySelector("[data-unit-need-editor]"); if (!editor) return; editor.hidden = true; editor.querySelectorAll("input, textarea").forEach((control) => { control.value = ""; }); }
function saveNeed(page, state) { const editor = page.querySelector("[data-unit-need-editor]"); const name = editor?.querySelector('[name="needName"]'); if (!editor || !name?.value.trim()) { name?.focus(); return; } const id = normalizeIdentifier(editor.querySelector('[name="needId"]').value); const need = { id: id || createDraftId("need"), name: name.value.trim(), description: editor.querySelector('[name="needDescription"]').value.trim() || null, priority: editor.querySelector('[name="needPriority"]').value, status: editor.querySelector('[name="needStatus"]').value }; const action = id ? "update" : "create"; state.needs = action === "update" ? state.needs.map((item) => String(item.id) === id ? need : item) : [...state.needs, need]; state.needChanges.push({ action, need }); renderNeeds(page, state.needs); closeNeedEditor(page); }
function removeNeed(page, state, id) { state.needs = state.needs.filter((item) => String(item.id) !== String(id)); state.needChanges.push({ action: "remove", needId: id }); renderNeeds(page, state.needs); }
function renderNeeds(page, needs) { const list = page.querySelector("[data-unit-need-list]"); const empty = page.querySelector("[data-unit-needs-empty]"); if (!list || !empty) return; list.replaceChildren(...needs.map(createNeedItem)); empty.hidden = needs.length > 0; }
function createNeedItem(need) { const item = document.createElement("article"); item.className = "unit-need-item"; const content = document.createElement("div"); const name = document.createElement("strong"); name.textContent = need.name; const meta = document.createElement("span"); meta.textContent = `${getPriorityLabel(need.priority)} • ${need.status === "FULFILLED" ? "Atendida" : "Ativa"}`; content.append(name, meta); if (need.description) { const description = document.createElement("p"); description.textContent = need.description; content.append(description); } const actions = document.createElement("div"); actions.className = "unit-contact-item__actions"; actions.append(createCollectionAction("edit", need.id, "Editar necessidade", "needAction"), createCollectionAction("remove", need.id, "Remover necessidade", "needAction")); item.append(content, actions); return item; }

function bindSubmit(page, state) { page.querySelector("[data-unit-form]")?.addEventListener("submit", (event) => { event.preventDefault(); if (!state.institutionId) { setFeedback(page, "Selecione uma instituição válida."); return; } if (!validateStep(page, state)) { state.currentStep = 1; updateStep(page, state, true); return; } const changes = collectChanges(page, state); if (state.mode === "edit" && !Object.keys(changes).length) { setFeedback(page, "Nenhuma alteração foi informada."); return; } page.dispatchEvent(new CustomEvent("bemfeito:unit-save-requested", { bubbles: true, detail: { mode: state.mode, institutionId: state.institutionId, unitId: state.unitId, changes } })); setFeedback(page, state.mode === "edit" ? "As alterações parciais foram preparadas. A gravação dependerá da integração com o sistema." : "O cadastro da unidade foi preparado. A gravação dependerá da integração com o sistema."); }); }

function collectChanges(page, state) {
  const form = page.querySelector("[data-unit-form]"); const creating = state.mode === "create"; const changes = {}; const address = {};
  UNIT_FIELDS.forEach((field) => { if (!creating && !state.dirtyFields.has(field)) return; const value = form.elements.namedItem(field)?.value.trim() || ""; if (ADDRESS_FIELDS.has(field)) { if (value || !creating) address[field === "address" ? "street" : field] = value || null; } else if (value || !creating) changes[field] = value || null; });
  if (Object.keys(address).length) changes.address = address;
  if (creating || state.serviceAreasChanged) changes.serviceAreas = [...page.querySelectorAll('[name="serviceAreas"]:checked')].map((item) => item.value);
  if (creating) { changes.contacts = state.contacts.map(stripDraftId); changes.needs = state.needs.map(stripDraftId); }
  else { if (state.contactChanges.length) changes.contactChanges = state.contactChanges; if (state.needChanges.length) changes.needChanges = state.needChanges; }
  return changes;
}

function fillUnit(page, state, unit) {
  const address = unit.address || {}; const values = { name: unit.name, document: unit.document, description: unit.description, postalCode: address.postalCode || unit.postalCode, address: address.street || unit.addressLine, number: address.number || unit.number, complement: address.complement || unit.complement, neighborhood: address.neighborhood || unit.neighborhood, city: address.city || unit.city, state: address.state || unit.state, serviceAreasObservations: unit.serviceAreasObservations };
  Object.entries(values).forEach(([field, value]) => { const control = page.querySelector(`[name="${field}"]`); if (control) control.value = value || ""; });
  page.querySelectorAll('[name="serviceAreas"]').forEach((checkbox) => { checkbox.checked = Array.isArray(unit.serviceAreas) && unit.serviceAreas.includes(checkbox.value); });
  state.contacts = Array.isArray(unit.contacts) ? unit.contacts.filter((item) => item?.value).map((item) => ({ ...item, id: normalizeIdentifier(item.id) || createDraftId("contact"), type: String(item.type || "PHONE").toUpperCase() })) : [];
  state.needs = Array.isArray(unit.needs) ? unit.needs.filter((item) => item?.name).map((item) => ({ ...item, id: normalizeIdentifier(item.id) || createDraftId("need"), priority: String(item.priority || "MEDIUM").toUpperCase(), status: String(item.status || "ACTIVE").toUpperCase() })) : [];
  state.contactChanges = []; state.needChanges = []; state.dirtyFields.clear(); state.serviceAreasChanged = false; renderContacts(page, state.contacts); renderNeeds(page, state.needs); updateObservationCounter(page);
}

function updateSubmission(page, state) { const button = page.querySelector("[data-submit-unit]"); if (button) button.disabled = !state.institutionId; }
function updateObservationCounter(page) { const input = page.querySelector("[data-needs-observations]"); const counter = page.querySelector("[data-observations-counter]"); if (input && counter) counter.textContent = `${input.value.length}/300`; }
function createCollectionAction(action, id, label, datasetName) { const button = document.createElement("button"); button.type = "button"; button.className = `unit-contact-item__action${action === "remove" ? " unit-contact-item__action--danger" : ""}`; button.dataset[datasetName] = action; button.dataset[datasetName === "needAction" ? "needId" : "contactId"] = String(id); button.setAttribute("aria-label", label); button.textContent = action === "edit" ? "Editar" : "Remover"; return button; }
function stripDraftId(item) { const { id, ...data } = item; return String(id).startsWith("draft-") ? data : item; }
function showFieldError(page, field, message) { page.querySelector(`[name="${field}"]`)?.setAttribute("aria-invalid", "true"); const error = page.querySelector(`[data-field-error="${field}"]`); if (error) error.textContent = message; }
function clearFieldError(page, field) { page.querySelector(`[name="${field}"]`)?.removeAttribute("aria-invalid"); const error = page.querySelector(`[data-field-error="${field}"]`); if (error) error.textContent = ""; }
function buildReturnUrl(institutionId) { return `./my-institution.html${institutionId ? `?institutionId=${encodeURIComponent(institutionId)}` : ""}#units`; }
function getInstitutionName(institution) { return String(institution?.displayName || institution?.name || "").trim(); }
function getContactTypeLabel(type) { return { EMAIL: "E-mail", PHONE: "Telefone", WHATSAPP: "WhatsApp" }[String(type).toUpperCase()] || "Contato"; }
function getPriorityLabel(priority) { return { LOW: "Baixa", MEDIUM: "Média", HIGH: "Alta" }[priority] || "Prioridade não informada"; }
function normalizeIdentifier(value) { if (value === null || value === undefined) return null; return String(value).trim() || null; }
function createDraftId(kind) { return `draft-${kind}-${typeof crypto !== "undefined" && crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`}`; }
function formatCnpj(value) { const d = value.replace(/\D/g, "").slice(0, 14); return d.replace(/^(\d{2})(\d)/, "$1.$2").replace(/^(\d{2})\.(\d{3})(\d)/, "$1.$2.$3").replace(/\.(\d{3})(\d)/, ".$1/$2").replace(/(\d{4})(\d)/, "$1-$2"); }
function setText(page, selector, text) { const element = page.querySelector(selector); if (element) element.textContent = text; }
function setFeedback(page, message) { const element = page.querySelector("[data-unit-form-feedback]"); if (!element) return; element.textContent = message; element.hidden = false; }

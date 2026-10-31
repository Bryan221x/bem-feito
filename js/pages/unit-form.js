import { getUnit, createUnit, patchUnit, getServiceAreas, getNeedCategories, saveServiceAreas, saveOpeningHours, saveNeed as persistNeed, deleteNeed } from "../services/unit-service.js";
import { getInstitution, saveContact, deleteContact, contactPayload, changedFields } from "../services/institution-service.js";
import { getStates, getCities } from "../services/request-service.js";
import { lookupPostalCode } from "../services/api-client.js";
import { getCurrentUser, getMyInstitutions } from "../services/auth-service.js";
import { saving, element, errorMessage, preserveContext } from "../components/api-ui.js";
const unitFormStates = new WeakMap();
const UNIT_FIELDS = ["name", "document", "description", "postalCode", "address", "number", "complement", "neighborhood", "cityId", "latitude", "longitude", "status"];
const ADDRESS_FIELDS = new Set(["postalCode", "address", "number", "complement", "neighborhood", "cityId"]);

// Mesma matriz de InstitutionAuthorizationService; o backend continua autorizando cada escrita.
export function editableUnitFields({ admin = false, permissions = [] } = {}) {
  return UNIT_FIELDS.filter((field) => admin || permissions.includes(
    field === "description" ? "EDIT_UNIT_DESCRIPTION" : field === "document" ? "EDIT_LEGAL_DATA" :
      field === "status" ? "CHANGE_UNIT_STATUS" : "EDIT_UNIT"));
}

export function applyUnitPermissions(page, access) {
  const editable = new Set(editableUnitFields(access));
  for (const field of [...UNIT_FIELDS, "state"]) {
    const control = page.querySelector(`[name="${field}"]`);
    if (control) control.disabled = !editable.has(field === "state" ? "cityId" : field);
  }
  const postalButton = page.querySelector("[data-search-postal-code]");
  if (postalButton) postalButton.disabled = !editable.has("cityId");
}

export function unitPayload(form, original, access) {
  const body = {};
  for (const field of editableUnitFields(access)) {
    const raw = form.elements.namedItem(field)?.value.trim() || "";
    const key = field === "address" ? "street" : field;
    body[key] = ["cityId", "latitude", "longitude"].includes(key) ? raw === "" ? null : Number(raw) :
      ["document", "postalCode"].includes(key) ? raw.replace(/\D/g, "") || null : raw || null;
  }
  if (original) return changedFields({ ...original, ...original.address }, body);
  if (!access.admin && !access.permissions?.includes("CREATE_UNIT")) throw new Error("Você não tem permissão para criar unidades.");
  // O POST exige status; MANAGER só pode criar unidade ACTIVE sem documento próprio.
  if (!access.admin && !access.permissions?.includes("CHANGE_UNIT_STATUS")) body.status = "ACTIVE";
  if (!access.admin && !access.permissions?.includes("EDIT_LEGAL_DATA")) body.document = null;
  return body;
}

export async function initUnitFormPage() {
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
    access: {}, authorized: false,
  };
  unitFormStates.set(page, state);
  configureMode(page, state);
  bindInstitutionSelector(page, state);
  bindSteps(page, state);
  bindFields(page, state);
  bindPostalControls(page, state);
  bindContactEditor(page, state);
  bindNeedEditor(page, state);
  bindSubmit(page, state);
  updateObservationCounter(page);
  renderContacts(page, state.contacts);
  renderNeeds(page, state.needs);
  updateStep(page, state);
  updateSubmission(page, state);
  showUnitFormLoading(page);
  applyUnitPermissions(page, state.access);
  try {
    if (!state.requestedInstitutionId) throw new Error("Selecione uma instituição para continuar.");
    const user = await getCurrentUser();
    const membership = user.platformRole === "PLATFORM_ADMIN" ? null :
      (await getMyInstitutions()).find((m) => String(m.institutionId) === state.requestedInstitutionId);
    state.access = { admin: user.platformRole === "PLATFORM_ADMIN", permissions: membership?.permissions || [] };
    if (!state.access.admin && (!membership || (!state.unitId && !state.access.permissions.includes("CREATE_UNIT")))) {
      throw new Error("Você não tem permissão para acessar este formulário.");
    }
    const [institution, unit, states, areas, categories] = await Promise.all([getInstitution(state.requestedInstitutionId), state.unitId ? getUnit(state.unitId) : null, getStates(), getServiceAreas(), getNeedCategories()]);
    if (unit && String(unit.institutionId) !== String(institution.id)) throw new Error("A unidade não pertence à instituição selecionada.");
    state.original = unit; state.hours = unit?.openingHours || [];
    const uf = page.querySelector('[name="state"]'); uf.replaceChildren(new Option("Selecione", ""), ...states.map((item) => new Option(item.name, item.uf)));
    if (unit) { uf.value = unit.address.state; await loadUnitCities(page, state, unit.address.cityId); }
    const areaOptions = page.querySelector("[data-service-area-options]");
    const allAreas = [...areas, ...(unit?.serviceAreas || []).filter((a) => !areas.some((b) => b.id === a.id))];
    areaOptions.replaceChildren(...allAreas.map((area) => { const label = element("label", null, "unit-needs__option"), input = element("input"), text = element("span", area.name, "unit-needs__content"); input.type = "checkbox"; input.name = "serviceAreas"; input.value = area.id; input.addEventListener("change", () => { state.serviceAreasChanged = true; }); label.append(input, text); return label; }));
    state.needCategories = categories;
    page.querySelector('[name="needCategoryId"]').replaceChildren(new Option("Selecione", ""), ...categories.map((item) => new Option(item.name, item.id)));
    state.authorized = true;
    renderUnitFormContext(page, { institutions: [institution], selectedInstitutionId: institution.id, unit });
    applyUnitPermissions(page, state.access);
    page.querySelector("[data-unit-institution-selector]").disabled = true;
    preserveContext(institution.id); renderHoursEditor(page, state);
    setFeedback(page, (unit ? "Dados carregados. Contatos, necessidades, áreas e horários podem ser salvos separadamente." : "Preencha os dados obrigatórios. Contatos e necessidades serão gravados após criar a unidade.") + " Campos sem permissão de edição ficam desabilitados.");
  } catch (error) { state.authorized = false; state.access = {}; state.institutionId = null; applyUnitPermissions(page, state.access); updateSubmission(page, state); setFeedback(page, errorMessage(error)); }
}


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
  setText(page, "[data-submit-unit]", editing ? "Salvar alterações" : "Cadastrar unidade");
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

function bindPostalControls(page, state) {
  const postal = page.querySelector("[data-postal-code]");
  postal?.addEventListener("input", () => { const digits = postal.value.replace(/\D/g, "").slice(0, 8); postal.value = digits.length > 5 ? `${digits.slice(0, 5)}-${digits.slice(5)}` : digits; });
  page.querySelector('[name="state"]').addEventListener("change", () => { void loadUnitCities(page, state).catch((error) => setFeedback(page, errorMessage(error))); });
  page.querySelector("[data-search-postal-code]").addEventListener("click", async (event) => {
    const button = event.currentTarget, requested = postal.value; button.disabled = true;
    try {
      const data = await lookupPostalCode(requested); if (postal.value !== requested) return;
      const uf = page.querySelector('[name="state"]');
      if (![...uf.options].some((o) => o.value === data.uf)) throw new Error("Estado não encontrado. Preencha manualmente.");
      uf.value = data.uf;
      const cities = await loadUnitCities(page, state);
      if (postal.value !== requested || uf.value !== data.uf || !cities.length) return;
      const city = cities.find((item) => normalizeCity(item.name) === normalizeCity(data.localidade));
      page.querySelector('[name="cityId"]').value = city ? String(city.id) : ""; state.dirtyFields.add("cityId");
      for (const [field, value] of [["address", data.logradouro], ["neighborhood", data.bairro]]) if (value) { page.querySelector(`[name="${field}"]`).value = value; state.dirtyFields.add(field); }
      setFeedback(page, city ? "Endereço localizado. Confira e corrija os dados antes de salvar." : "Cidade não localizada na base. Selecione manualmente.");
    } catch (error) { setFeedback(page, errorMessage(error)); } finally { button.disabled = false; }
  });
  page.querySelector("[data-save-areas]").addEventListener("click", () => {
    if (!state.unitId) { setFeedback(page, "As áreas serão gravadas ao cadastrar a unidade."); return; }
    const ids = [...page.querySelectorAll('[name="serviceAreas"]:checked')].map((i) => Number(i.value));
    void saving(page, (message) => setFeedback(page, message), async () => { await saveServiceAreas(state.unitId, ids); state.serviceAreasChanged = false; });
  });
}
async function loadUnitCities(page, state, selected = null) {
  const sequence = state.citySequence = (state.citySequence || 0) + 1;
  const uf = page.querySelector('[name="state"]').value, city = page.querySelector('[name="cityId"]'); city.disabled = true; city.replaceChildren(new Option("Carregando…", ""));
  state.dirtyFields.add("cityId");
  try { const cities = uf ? await getCities(uf) : []; if (sequence !== state.citySequence) return []; city.replaceChildren(new Option("Selecione", ""), ...cities.map((item) => new Option(item.name, item.id))); city.value = selected ? String(selected) : ""; city.disabled = !cities.length; return cities; }
  catch (error) { if (sequence === state.citySequence) city.replaceChildren(new Option("Selecione o estado novamente", "")); throw error; }
}
function normalizeCity(value) { return String(value || "").normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase(); }

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
    if (!state.unitId) { state.contacts = id ? state.contacts.map((item) => String(item.id) === id ? contact : item) : [...state.contacts, contact]; renderContacts(page, state.contacts); closeContactEditor(page); return; }
    void saving(page, (message) => setFeedback(page, message), async () => {
      const result = await saveContact("units", state.unitId, id?.startsWith("draft-") ? null : id, contactPayload(contact));
      state.contacts = id ? state.contacts.map((item) => String(item.id) === id ? result : item) : [...state.contacts, result]; renderContacts(page, state.contacts); closeContactEditor(page);
    });
  });
}

function openContactEditor(page, contact = null) {
  const editor = page.querySelector("[data-unit-contact-editor]"); const form = page.querySelector("[data-unit-contact-form]"); if (!editor || !form) return;
  form.reset(); form.elements.contactId.value = contact?.id || ""; form.elements.contactType.value = contact?.type || "PHONE"; form.elements.contactLabel.value = contact?.label || ""; form.elements.contactValue.value = contact?.value || ""; form.elements.contactIsPrimary.checked = Boolean(contact?.isPrimary); form.elements.contactIsPublic.checked = Boolean(contact?.isPublic);
  setText(page, "[data-unit-contact-editor-title]", contact ? "Editar contato" : "Adicionar contato"); editor.hidden = false; form.elements.contactType.focus();
}
function closeContactEditor(page) { const editor = page.querySelector("[data-unit-contact-editor]"); if (editor) editor.hidden = true; page.querySelector("[data-unit-contact-form]")?.reset(); }
function removeContact(page, state, id) {
  const remove = () => { state.contacts = state.contacts.filter((item) => String(item.id) !== String(id)); renderContacts(page, state.contacts); };
  if (!state.unitId || String(id).startsWith("draft-")) { remove(); return; }
  void saving(page, (message) => setFeedback(page, message), async () => { await deleteContact("units", state.unitId, id); remove(); });
}
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
function openNeedEditor(page, need = null) { const editor = page.querySelector("[data-unit-need-editor]"); if (!editor) return; editor.querySelector('[name="needId"]').value = need?.id || ""; editor.querySelector('[name="needName"]').value = need?.title || need?.name || ""; editor.querySelector('[name="needDescription"]').value = need?.description || ""; editor.querySelector('[name="needPriority"]').value = need?.priority || "MEDIUM"; editor.querySelector('[name="needStatus"]').value = need?.status || "ACTIVE"; setText(page, "[data-unit-need-editor-title]", need ? "Editar necessidade" : "Adicionar necessidade"); for (const [field, value] of [["needCategoryId", need?.categoryId], ["needQuantity", need?.quantity], ["needUnitLabel", need?.unitLabel]]) editor.querySelector(`[name="${field}"]`).value = value ?? "";
  const category = editor.querySelector('[name="needCategoryId"]'); if (need?.categoryId && ![...category.options].some((o) => o.value === String(need.categoryId))) { category.add(new Option(need.category, need.categoryId)); category.value = need.categoryId; }
  editor.hidden = false; editor.querySelector('[name="needName"]')?.focus(); }
function closeNeedEditor(page) { const editor = page.querySelector("[data-unit-need-editor]"); if (!editor) return; editor.hidden = true; editor.querySelectorAll("input, textarea").forEach((control) => { control.value = ""; }); }
function saveNeed(page, state) {
  const editor = page.querySelector("[data-unit-need-editor]");
  const value = (name) => editor.querySelector(`[name="${name}"]`).value.trim();
  if (!value("needName") || !value("needCategoryId")) { setFeedback(page, "Informe título e categoria da necessidade."); return; }
  const id = value("needId");
  const body = { categoryId: Number(value("needCategoryId")), title: value("needName"), description: value("needDescription") || null, quantity: value("needQuantity") === "" ? null : Number(value("needQuantity")), unitLabel: value("needUnitLabel") || null, priority: value("needPriority"), status: value("needStatus") };
  if (!state.unitId) { const draft = { ...body, name: body.title, id: id || createDraftId("need") }; state.needs = id ? state.needs.map((n) => String(n.id) === id ? draft : n) : [...state.needs, draft]; renderNeeds(page, state.needs); closeNeedEditor(page); return; }
  void saving(page, (message) => setFeedback(page, message), async () => {
    const result = await persistNeed(state.unitId, id?.startsWith("draft-") ? null : id, body);
    state.needs = id ? state.needs.map((n) => String(n.id) === id ? result : n) : [...state.needs, result]; renderNeeds(page, state.needs); closeNeedEditor(page);
  });
}
export function removeNeed(page, state, id) {
  const remove = () => { state.needs = state.needs.filter((n) => String(n.id) !== String(id)); renderNeeds(page, state.needs); };
  if (!state.unitId || String(id).startsWith("draft-")) { remove(); return; }
  const need = state.needs.find((item) => String(item.id) === String(id));
  if (need?.status !== "INACTIVE") {
    setFeedback(page, "Para remover, edite a necessidade, selecione Inativa e salve. Depois clique em Remover.");
    return;
  }
  return saving(page, (message) => setFeedback(page, message), async () => {
    try { await deleteNeed(state.unitId, id); }
    catch (error) {
      if (error.status === 409) throw new Error("A necessidade não pôde ser removida. Recarregue a página e confirme que ela está Inativa antes de tentar novamente.");
      throw error;
    }
    remove();
  });
}

function renderNeeds(page, needs) { const list = page.querySelector("[data-unit-need-list]"); const empty = page.querySelector("[data-unit-needs-empty]"); if (!list || !empty) return; list.replaceChildren(...needs.map(createNeedItem)); empty.hidden = needs.length > 0; }
function createNeedItem(need) { const item = document.createElement("article"); item.className = "unit-need-item"; const content = document.createElement("div"); const name = document.createElement("strong"); name.textContent = need.title || need.name; const meta = document.createElement("span"); meta.textContent = `${getPriorityLabel(need.priority)} • ${need.status === "FULFILLED" ? "Atendida" : need.status === "INACTIVE" ? "Inativa" : "Ativa"}`; content.append(name, meta); if (need.description) { const description = document.createElement("p"); description.textContent = need.description; content.append(description); } const actions = document.createElement("div"); actions.className = "unit-contact-item__actions"; actions.append(createCollectionAction("edit", need.id, "Editar necessidade", "needAction"), createCollectionAction("remove", need.id, "Remover necessidade", "needAction")); item.append(content, actions); return item; }

function bindSubmit(page, state) {
  page.querySelector("[data-unit-form]").addEventListener("submit", (event) => {
    event.preventDefault(); if (!state.institutionId || !state.authorized) return;
    const next = unitPayload(event.currentTarget, state.original, state.access);
    for (const key of ["name", "cityId", "postalCode", "street", "number", "neighborhood"]) if ((!state.original || Object.hasOwn(next, key)) && !next[key]) { setFeedback(page, "Informe nome, cidade, CEP e endereço completo da unidade."); state.currentStep = key === "name" ? 1 : 2; updateStep(page, state); return; }
    return saving(page, (message) => setFeedback(page, message), async () => {
      if (!state.unitId) {
        const created = await createUnit(state.institutionId, next); state.unitId = String(created.id); state.mode = "edit"; state.original = created;
        const url = new URL(location.href); url.searchParams.set("unitId", state.unitId); history.replaceState(null, "", url); configureMode(page, state);
      } else if (Object.keys(next).length) { state.original = await patchUnit(state.unitId, next); }
      if (state.serviceAreasChanged || !state.areasSaved) { await saveServiceAreas(state.unitId, [...page.querySelectorAll('[name="serviceAreas"]:checked')].map((i) => Number(i.value))); state.serviceAreasChanged = false; state.areasSaved = true; }
      for (let i = 0; i < state.contacts.length; i++) if (String(state.contacts[i].id).startsWith("draft-")) state.contacts[i] = await saveContact("units", state.unitId, null, contactPayload(state.contacts[i]));
      for (let i = 0; i < state.needs.length; i++) if (String(state.needs[i].id).startsWith("draft-")) { const { id, name, title, category, ...need } = state.needs[i]; state.needs[i] = await persistNeed(state.unitId, null, { ...need, title: title || name }); }
      for (const row of page.querySelectorAll("[data-hours-day]")) if (row.dataset.dirty === "true") { await saveOpeningHours(state.unitId, row.dataset.hoursDay, hoursPayload(row)); row.dataset.dirty = "false"; }
      renderContacts(page, state.contacts); renderNeeds(page, state.needs); state.dirtyFields.clear();
    });
  });
}

function fillUnit(page, state, unit) {
  const address = unit.address || {}; const values = { name: unit.name, document: unit.document, description: unit.description, postalCode: address.postalCode || unit.postalCode, address: address.street || unit.addressLine, number: address.number || unit.number, complement: address.complement || unit.complement, neighborhood: address.neighborhood || unit.neighborhood, cityId: address.cityId, state: address.state, latitude: unit.latitude, longitude: unit.longitude, status: unit.status };
  Object.entries(values).forEach(([field, value]) => { const control = page.querySelector(`[name="${field}"]`); if (control) control.value = value ?? ""; });
  page.querySelectorAll('[name="serviceAreas"]').forEach((checkbox) => { checkbox.checked = Array.isArray(unit.serviceAreas) && unit.serviceAreas.some((area) => String(area.id) === checkbox.value); });
  state.contacts = Array.isArray(unit.contacts) ? unit.contacts.filter((item) => item?.value).map((item) => ({ ...item, id: normalizeIdentifier(item.id) || createDraftId("contact"), type: String(item.type || "PHONE").toUpperCase() })) : [];
  state.needs = Array.isArray(unit.needs) ? unit.needs.filter((item) => item?.title || item?.name).map((item) => ({ ...item, id: normalizeIdentifier(item.id) || createDraftId("need"), priority: String(item.priority || "MEDIUM").toUpperCase(), status: String(item.status || "ACTIVE").toUpperCase() })) : [];
  state.areasSaved = true; state.contactChanges = []; state.needChanges = []; state.dirtyFields.clear(); state.serviceAreasChanged = false; renderContacts(page, state.contacts); renderNeeds(page, state.needs); updateObservationCounter(page);
}

function updateSubmission(page, state) { const button = page.querySelector("[data-submit-unit]"); if (button) button.disabled = !state.institutionId || !state.authorized; }
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

function renderHoursEditor(page, state) {
  const days = ["Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira", "Sexta-feira", "Sábado", "Domingo"];
  page.querySelector("[data-opening-hours]").replaceChildren(...days.map((day, index) => {
    const h = state.hours.find((item) => Number(item.dayOfWeek) === index + 1), row = element("fieldset", null, "api-hours-row"); row.dataset.hoursDay = index + 1;
    row.append(element("legend", day));
    for (const [key, title, type, value] of [["closed", "Fechado", "checkbox", h?.closed], ["opensAt", "Abre", "time", h?.opensAt?.slice(0,5)], ["closesAt", "Fecha", "time", h?.closesAt?.slice(0,5)], ["note", "Observação", "text", h?.note]]) {
      const label = element("label", title), input = element("input"); input.type = type; input.dataset.hoursField = key; if (type === "checkbox") input.checked = !!value; else input.value = value || ""; if (key === "note") input.maxLength = 150;
      input.addEventListener("input", () => { row.dataset.dirty = "true"; }); label.append(input); row.append(label);
    }
    if (!h) row.append(element("small", "Horário ainda não informado."));
    const button = element("button", "Salvar dia", "button button--outline"); button.type = "button";
    button.addEventListener("click", () => {
      if (!state.unitId) { row.dataset.dirty = "true"; setFeedback(page, "Este horário será salvo ao cadastrar a unidade."); return; }
      void saving(page, (message) => setFeedback(page, message), async () => { await saveOpeningHours(state.unitId, index + 1, hoursPayload(row)); row.dataset.dirty = "false"; });
    }); row.append(button); return row;
  }));
}
export function hoursPayload(row) {
  const get = (key) => row.querySelector(`[data-hours-field="${key}"]`);
  const closed = get("closed").checked, opensAt = get("opensAt").value, closesAt = get("closesAt").value;
  if (!closed && (!opensAt || !closesAt || opensAt >= closesAt)) throw new Error("Informe abertura e fechamento válidos; o fechamento deve ser posterior à abertura.");
  return { closed, opensAt: closed ? null : opensAt, closesAt: closed ? null : closesAt, note: get("note").value.trim() || null };
}

const requestStates = new WeakMap();

export function initRegistrationRequestPage() {
  const page = document.querySelector("[data-registration-request-page]");
  const form = page?.querySelector("[data-registration-request-form]");
  if (!page || !form) return;

  const state = { publicContacts: [] };
  requestStates.set(page, state);
  bindMask(form.querySelector("[data-cnpj]"), formatCnpj);
  bindMask(form.querySelector("[data-phone]"), formatPhone);
  bindMask(form.querySelector("[data-postal]"), formatPostalCode);
  form.querySelector("[data-state]")?.addEventListener("input", (event) => {
    event.target.value = event.target.value.replace(/[^a-z]/gi, "").toUpperCase();
  });
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
    institution: {
      displayName: clean(data.get("displayName")),
      legalName: clean(data.get("legalName")),
      document: clean(data.get("document")),
      category: clean(data.get("category")),
      objective: clean(data.get("objective")),
      description: clean(data.get("description")),
      website: clean(data.get("website")) || null,
    },
    responsible: {
      name: clean(data.get("responsibleName")),
      role: clean(data.get("responsibleRole")) || null,
      accessEmail: clean(data.get("responsibleEmail")),
      phone: clean(data.get("responsiblePhone")),
    },
    publicContacts: state.publicContacts.map(({ id, ...contact }) => contact),
    mainUnit: {
      name: clean(data.get("unitName")),
      address: {
        postalCode: clean(data.get("postalCode")),
        street: clean(data.get("street")),
        number: clean(data.get("number")),
        complement: clean(data.get("complement")) || null,
        neighborhood: clean(data.get("neighborhood")),
        city: clean(data.get("city")),
        state: clean(data.get("state")),
      },
    },
  };

  /* A futura integração fará o POST deste contrato sem misturar credenciais e contatos públicos. */
  page.dispatchEvent(new CustomEvent("bemfeito:registration-requested", { bubbles: true, detail: request }));
  setFormMessage(page, "O envio da solicitação será disponibilizado após a integração com o sistema.");
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
  state.publicContacts.push({
    id: createDraftId(),
    type: type.value,
    value: value.value.trim(),
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
  const required = ["displayName", "legalName", "document", "category", "objective", "description", "responsibleName", "responsibleEmail", "responsiblePhone", "unitName", "postalCode", "street", "number", "neighborhood", "city", "state"];
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
function createDraftId() { return `draft-public-contact-${typeof crypto !== "undefined" && crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`}`; }

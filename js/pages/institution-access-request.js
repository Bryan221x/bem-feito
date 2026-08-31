export function initInstitutionAccessRequestPage() {
  const page = document.querySelector("[data-institution-access-request-page]");
  const form = page?.querySelector("[data-institution-access-form]");
  if (!page || !form) return;

  bindMask(form.querySelector("[data-phone]"), formatPhone);
  form.addEventListener("input", (event) => clearError(form, event.target.name));
  form.addEventListener("submit", (event) => {
    event.preventDefault();
    const errors = validate(form);
    if (errors.length) {
      errors.forEach(({ field, message }) => showError(form, field, message));
      form.elements.namedItem(errors[0].field)?.focus();
      setMessage(page, "Revise os campos destacados.", true);
      return;
    }

    const data = new FormData(form);
    const request = {
      institution: { query: data.get("institutionQuery").trim() },
      responsible: { name: data.get("responsibleName").trim(), email: data.get("email").trim(), phone: data.get("phone").trim(), role: data.get("role").trim() || null },
      justification: data.get("justification").trim() || null,
    };
    page.dispatchEvent(new CustomEvent("bemfeito:institution-access-requested", { bubbles: true, detail: request }));
    setMessage(page, "O envio da solicitação de acesso será disponibilizado após a integração com o sistema.");
  });
}

function validate(form) {
  const errors = [];
  [["institutionQuery", "Informe o nome ou CNPJ da instituição."], ["responsibleName", "Informe o nome do responsável."], ["email", "Informe um e-mail válido."], ["phone", "Informe o telefone ou WhatsApp."]].forEach(([field, message]) => {
    const input = form.elements.namedItem(field);
    if (!input?.value.trim() || !input.checkValidity()) errors.push({ field, message });
  });
  return errors;
}

function bindMask(input, formatter) { input?.addEventListener("input", () => { input.value = formatter(input.value); }); }
function formatPhone(value) { const d = value.replace(/\D/g, "").slice(0, 11); return d.replace(/^(\d{2})(\d)/, "($1) $2").replace(/(\d{5})(\d)/, "$1-$2"); }
function showError(form, field, message) { const input = form.elements.namedItem(field); const error = form.querySelector(`[data-error-for="${field}"]`); input?.setAttribute("aria-invalid", "true"); if (error) error.textContent = message; }
function clearError(form, field) { if (!field) return; form.elements.namedItem(field)?.removeAttribute("aria-invalid"); const error = form.querySelector(`[data-error-for="${field}"]`); if (error) error.textContent = ""; }
function setMessage(page, message, isError = false) { const element = page.querySelector("[data-access-message]"); if (!element) return; element.textContent = message; element.dataset.state = isError ? "error" : "neutral"; element.hidden = false; }

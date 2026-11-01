let sequence = 0;

export function validationText(message) {
  const text = String(message || "Confira o valor informado.");
  if (/must not be (blank|null|empty)/i.test(text)) return "Preencha este campo.";
  if (/well-formed email|valid email/i.test(text)) return "Informe um e-mail válido.";
  if (/size must be between/i.test(text)) return "Confira o tamanho do texto informado.";
  if (/must (match|be greater|be less|be a)/i.test(text)) return "Confira o formato e os limites deste campo.";
  return text;
}

export function clearFieldError(input) {
  if (!input) return;
  input.removeAttribute("aria-invalid");
  const id = input.dataset?.errorId;
  if (id) { const message = document.getElementById(id); if (message) message.textContent = ""; }
  for (const described of (input.getAttribute("aria-describedby") || "").split(/\s+/)) {
    const message = document.getElementById(described);
    if (message?.matches(".field-error, [data-error-for], [data-field-error], .institution-login__field-error")) message.textContent = "";
  }
}

export function fieldError(input, text) {
  if (!input || input.disabled) return;
  input.setAttribute("aria-invalid", "true");
  let message = input.dataset?.errorId && document.getElementById(input.dataset.errorId);
  if (!message) {
    message = (input.getAttribute("aria-describedby") || "").split(/\s+/).map(id => document.getElementById(id))
      .find(node => node?.matches(".field-error, [data-error-for], [data-field-error], .institution-login__field-error"));
  }
  if (!message) {
    message = document.createElement("small"); message.id = `field-error-${++sequence}`;
    message.className = "field-error"; message.setAttribute("aria-live", "polite");
    input.dataset.errorId = message.id;
    input.setAttribute("aria-describedby", [input.getAttribute("aria-describedby"), message.id].filter(Boolean).join(" "));
    input.after(message);
  }
  message.textContent = validationText(text);
}

function validationMessage(input) {
  const validity = input.validity;
  if (validity.valueMissing) return "Preencha este campo.";
  if (validity.typeMismatch) return input.type === "email" ? "Informe um e-mail válido." : "Informe um endereço válido.";
  if (validity.tooLong) return `Use no máximo ${input.maxLength} caracteres.`;
  if (validity.tooShort) return `Use pelo menos ${input.minLength} caracteres.`;
  if (validity.rangeUnderflow || validity.rangeOverflow || validity.stepMismatch || validity.badInput) return "Informe um número dentro dos limites indicados.";
  return "Confira o formato deste campo.";
}

export function focusInvalid(input) {
  if (!input) return;
  input.dispatchEvent(new CustomEvent("bemfeito:invalid-field", { bubbles: true }));
  input.focus();
}

export function validateFields(root) {
  let first = null;
  for (const input of root.querySelectorAll("input, select, textarea")) {
    if (!input.willValidate) continue;
    if (root.tagName === "FORM" && input.form !== root) continue;
    // Steps are revealed on invalid-field; closed independent editors are not submitted.
    const hidden = input.closest("[hidden]");
    if (hidden && !hidden.matches("[data-unit-step]")) continue;
    clearFieldError(input);
    if (!input.validity.valid) { fieldError(input, validationMessage(input)); first ||= input; }
  }
  focusInvalid(first);
  return !first;
}

export function showFieldErrors(root, fields = {}, aliases = {}) {
  const controls = [...root.querySelectorAll("input, select, textarea")];
  let first = null;
  for (const [key, message] of Object.entries(fields)) {
    const input = controls.find((node) => node.name === (aliases[key] || key)) || controls.find((node) => node.name === key);
    if (input && !input.disabled) { fieldError(input, message); first ||= input; }
  }
  focusInvalid(first);
}

export function initFormFeedback() {
  document.querySelectorAll("form").forEach((form) => {
    form.noValidate = true;
    form.addEventListener("submit", (event) => {
      if (!validateFields(form)) { event.preventDefault(); event.stopImmediatePropagation(); }
    }, true);
  });
  const clearCorrected = (event) => {
    const input = event.target;
    if (input.validity?.valid) clearFieldError(input);
  };
  document.addEventListener("input", clearCorrected);
  document.addEventListener("change", clearCorrected);
}

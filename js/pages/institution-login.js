export function initInstitutionLoginPage() {
  const page = document.querySelector(".institution-login-page");

  if (!page) return;

  const form = page.querySelector("[data-institution-login-form]");

  const emailInput = page.querySelector("[data-institution-email]");

  const passwordInput = page.querySelector("[data-institution-password]");

  const passwordToggle = page.querySelector("[data-password-toggle]");

  const message = page.querySelector("[data-institution-login-message]");

  if (!form || !emailInput || !passwordInput) {
    return;
  }

  initPasswordToggle(passwordInput, passwordToggle);

  initFieldValidation(emailInput, passwordInput, message);

  initLoginForm(form, emailInput, passwordInput, message);
}

/* =========================================================
   Exibição da senha
   ========================================================= */

function initPasswordToggle(passwordInput, toggle) {
  if (!toggle) return;

  const slash = toggle.querySelector("[data-password-slash]");

  toggle.addEventListener("click", () => {
    const isVisible = passwordInput.type === "text";

    passwordInput.type = isVisible ? "password" : "text";

    /*
     * Senha oculta: olho com risco.
     * Senha visível: olho sem risco.
     */
    if (slash) {
      slash.classList.toggle("is-hidden", !isVisible);
    }

    toggle.setAttribute("aria-pressed", String(!isVisible));

    toggle.setAttribute(
      "aria-label",
      isVisible ? "Mostrar senha" : "Ocultar senha",
    );
  });
}
/* =========================================================
   Validação durante o preenchimento
   ========================================================= */

function initFieldValidation(emailInput, passwordInput, message) {
  emailInput.addEventListener("input", () => {
    clearFieldError(emailInput);
    hideLoginMessage(message);
  });

  passwordInput.addEventListener("input", () => {
    clearFieldError(passwordInput);
    hideLoginMessage(message);
  });

  emailInput.addEventListener("blur", () => {
    if (emailInput.value.trim()) {
      validateEmail(emailInput);
    }
  });
}

/* =========================================================
   Formulário
   ========================================================= */

function initLoginForm(form, emailInput, passwordInput, message) {
  form.addEventListener("submit", (event) => {
    event.preventDefault();

    hideLoginMessage(message);

    const emailIsValid = validateEmail(emailInput);

    const passwordIsValid = validatePassword(passwordInput);

    if (!emailIsValid) {
      emailInput.focus();
      return;
    }

    if (!passwordIsValid) {
      passwordInput.focus();
      return;
    }

    /*
     * O Front-End valida somente o preenchimento.
     *
     * A autenticação das credenciais será realizada
     * pela API quando o backend estiver disponível.
     */
    showLoginMessage(
      message,
      "O acesso da instituição será disponibilizado após a integração com o sistema de autenticação.",
    );
  });
}

/* =========================================================
   Validações
   ========================================================= */

function validateEmail(input) {
  clearFieldError(input);

  const value = input.value.trim();

  if (!value) {
    showFieldError(input, "Informe o e-mail.");

    return false;
  }

  if (!input.validity.valid) {
    showFieldError(input, "Informe um e-mail válido.");

    return false;
  }

  return true;
}

function validatePassword(input) {
  clearFieldError(input);

  if (!input.value) {
    showFieldError(input, "Informe a senha.");

    return false;
  }

  return true;
}

/* =========================================================
   Estados dos campos
   ========================================================= */

function showFieldError(input, message) {
  input.setAttribute("aria-invalid", "true");

  const field = input.closest(".institution-login__field");

  const error = field?.querySelector(".institution-login__field-error");

  if (error) {
    error.textContent = message;
  }
}

function clearFieldError(input) {
  input.removeAttribute("aria-invalid");

  const field = input.closest(".institution-login__field");

  const error = field?.querySelector(".institution-login__field-error");

  if (error) {
    error.textContent = "";
  }
}

/* =========================================================
   Mensagem geral
   ========================================================= */

function showLoginMessage(messageElement, text) {
  if (!messageElement) return;

  messageElement.textContent = text;
  messageElement.hidden = false;
}

function hideLoginMessage(messageElement) {
  if (!messageElement) return;

  messageElement.hidden = true;
  messageElement.textContent = "";
}

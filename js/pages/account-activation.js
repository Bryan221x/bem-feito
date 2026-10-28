import { inspectInvitation, acceptInvitation, withSubmission } from "../services/request-service.js";

export function takeInvitationToken(location, history) {
  const token = new URLSearchParams(location.hash.slice(1)).get("token") || "";
  history.replaceState(null, "", location.pathname + location.search);
  return /^[A-Za-z0-9_-]{43}$/.test(token) ? token : "";
}
export async function initAccountActivationPage() {
  const page = document.querySelector("[data-account-activation-page]"); if (!page) return;
  let token = takeInvitationToken(window.location, window.history);
  const form = page.querySelector("[data-activation-form]"), message = page.querySelector("[data-activation-message]");
  const login = page.querySelector("[data-activation-login]");
  function state(value, text) { page.dataset.state = value; message.textContent = text; form.hidden = !["ready", "error", "submitting"].includes(value); }
  window.addEventListener("pagehide", () => { token = ""; form.reset(); form.hidden = true; });
  window.addEventListener("pageshow", (e) => { if (e.persisted) state("invalid", "Reabra o link do convite para continuar."); });
  if (!token) { state("invalid", "Convite inválido. Solicite uma nova emissão à equipe Bem-Feito."); return; }
  state("loading", "Verificando convite…");
  try {
    const data = await inspectInvitation(token);
    const states = { INVALID: ["invalid", "Convite inválido. Solicite uma nova emissão."], EXPIRED: ["expired", "O convite expirou. Solicite uma nova emissão."], ALREADY_USED: ["already-used", "Este convite já foi utilizado ou substituído. Entre na conta ou solicite uma nova emissão."] };
    if (data.state !== "READY") { const [value, text] = states[data.state] || states.INVALID; token = ""; state(value, text); login.hidden = false; return; }
    state("ready", `Olá, ${data.name}. Defina sua senha para ${data.maskedEmail}.`);
  } catch { state("invalid", "Não foi possível verificar o convite. Reabra o link e tente novamente."); token = ""; return; }
  form.addEventListener("submit", (event) => {
    event.preventDefault(); if (!token || !form.reportValidity()) return;
    const password = form.elements.namedItem("password"), confirm = form.elements.namedItem("confirmPassword");
    if (password.value.length < 12 || new TextEncoder().encode(password.value).length > 72) { state("error", "Use pelo menos 12 caracteres e no máximo 72 bytes UTF-8."); return; }
    if (password.value !== confirm.value) { state("error", "As senhas não conferem."); return; }
    return withSubmission(form, async () => {
      state("submitting", "Ativando conta…");
      try { await acceptInvitation(token, password.value, confirm.value); token = ""; state("success", "Conta ativada. Você já pode entrar com seu e-mail e senha."); login.hidden = false; }
      catch (error) { state("error", error.message || "Não foi possível ativar. Tente novamente."); }
      finally { form.reset(); }
    });
  });
}
if (typeof document !== "undefined") initAccountActivationPage();

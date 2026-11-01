import { apiRequest } from "../services/api-client.js";
import { initFormFeedback, fieldError, focusInvalid } from "../components/form-feedback.js";
import { withSubmission } from "../services/request-service.js";
import { errorMessage } from "../components/api-ui.js";
const page = document.querySelector("[data-password-recovery]");
if (page) {
  initFormFeedback();
  const request = page.querySelector("[data-recovery-request]"), reset = page.querySelector("[data-recovery-reset]"), message = page.querySelector("[data-recovery-message]");
  let token = "";
  const notify = (text, state) => { message.textContent = text; message.dataset.state = state; };
  const consumeLink = () => {
    if (!location.hash) return;
    token = new URLSearchParams(location.hash.slice(1)).get("token") || "";
    history.replaceState(null, "", location.pathname + location.search);
    reset.reset(); reset.hidden = true;
    request.hidden = true;
    if (/^[A-Za-z0-9_-]{43}$/.test(token)) { reset.hidden = false; notify("Defina sua nova senha. O link será validado ao confirmar.", "ready"); }
    else { token = ""; notify("Link inválido. Solicite outro link de recuperação.", "error"); }
  };
  consumeLink();
  window.addEventListener("hashchange", consumeLink);
  request.addEventListener("submit", event => {
    event.preventDefault();
    void withSubmission(request, async () => {
      notify("Solicitando recuperação…", "pending");
      try { const result = await apiRequest("/api/password-recovery/request", { method:"POST", body:{ email:request.elements.email.value.trim() } }); notify(result.message, "success"); }
      catch (error) { notify(errorMessage(error), "error"); }
    });
  });
  reset.addEventListener("submit", event => {
    event.preventDefault(); if (!token) return;
    const password = reset.elements.password, confirm = reset.elements.confirmPassword;
    if (new TextEncoder().encode(password.value).length > 72) { fieldError(password,"Use no máximo 72 bytes UTF-8."); focusInvalid(password); return; }
    if (password.value !== confirm.value) { fieldError(confirm,"As senhas não conferem."); focusInvalid(confirm); return; }
    void withSubmission(reset, async () => {
      notify("Redefinindo senha…", "pending");
      try { const result = await apiRequest("/api/password-recovery/reset", { method:"POST", body:{ token, password:password.value, confirmPassword:confirm.value } }); token=""; reset.hidden=true; notify(result.message,"success"); }
      catch (error) { notify(errorMessage(error),"error"); }
      finally { reset.reset(); }
    });
  });
  window.addEventListener("pagehide", () => { token=""; reset.reset(); reset.hidden=true; });
  window.addEventListener("pageshow", event => { if(event.persisted) { reset.hidden=true;notify("Reabra o link recebido ou solicite outro para continuar.","pending"); } });
}

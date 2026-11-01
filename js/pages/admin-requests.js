import { formatContact, formatPhone } from "../services/contact-format.js";
import { listRequests, getRequest, decideRequest, reissueInvitation, withSubmission } from "../services/request-service.js";
import { errorMessage } from "../components/api-ui.js";

const labels = { PENDING: "Pendente", APPROVED: "Aprovada", REJECTED: "Rejeitada", NEEDS_CHANGES: "Ajustes solicitados", CANCELLED: "Cancelada" };
export function initAdminRequestsPage() {
  const page = document.querySelector("[data-admin-requests-page]"); if (!page) return;
  const dialog = page.querySelector("[data-request-dialog]"), details = page.querySelector("[data-request-details]");
  const feedback = page.querySelector("[data-request-dialog-feedback]"), type = page.querySelector("[data-request-type]"), status = page.querySelector("[data-request-status]");
  const prev = page.querySelector("[data-request-prev]"), next = page.querySelector("[data-request-next]");
  const notes = page.querySelector("[data-request-notes]"), role = page.querySelector("[data-request-role]");
  const reissue = page.querySelector("[data-reissue-invitation]"), manual = page.querySelector("[data-invitation-result]"), url = page.querySelector("[data-invitation-url]");
  const notice = page.querySelector("[data-request-notice]"), refresh = page.querySelector("[data-refresh-request]");
  function notify(message, state = "success") { if (notice) { notice.hidden = false; notice.textContent = message; notice.dataset.state = state; } }
  let pageNumber = 0, active = null, sequence = 0, detailSequence = 0, deciding = false, leaving = false;
  const params = new URLSearchParams(location.search); if (params.get("type") === "ACCESS") type.value = "ACCESS";
  function clearInvitation() { url.value = ""; manual.hidden = true; }
  function showInvitation(invitation) {
    clearInvitation(); if (leaving || !invitation) return;
    const mode = invitation.deliveryMode;
    if (invitation.activationUrl) { url.value = invitation.activationUrl; manual.hidden = false; }
    if (mode === "EMAIL_SENT") notify(invitation.purpose === "APPROVAL" ? "Acesso aprovado. Notificação aceita pelo servidor de e-mail." : "Convite aceito pelo servidor de e-mail. A entrega na caixa postal depende do provedor.");
    else if (mode === "FAILED") notify("Aprovação preservada. O e-mail não foi enviado. Use o convite manual, quando disponível, ou reemita o convite.", "error");
    else if (mode === "NOT_CONFIGURED") notify("Aprovação preservada. Serviço de e-mail não configurado." + (invitation.activationUrl ? " Convite manual disponível abaixo." : " A conta já ativa pode entrar normalmente."), "pending");
    else if (mode === "MANUAL") notify("Decisão concluída. Convite manual disponível abaixo; compartilhe o link por um canal privado.");
    else notify("Decisão concluída. Envio ainda não confirmado; consulte novamente ou reemita o convite.", "pending");
  }
  async function load() {
    const current = ++sequence; showState(page, "loading"); prev.disabled = next.disabled = true;
    try {
      const data = await listRequests(type.value, status.value, pageNumber); if (current !== sequence) return;
      if (pageNumber > 0 && !data.content.length) { pageNumber--; return load(); }
      renderAdminRequests(page, data.content); prev.disabled = pageNumber === 0; next.disabled = data.last;
      page.querySelector("[data-request-pagination]").textContent = `Página ${pageNumber + 1} de ${Math.max(1, data.totalPages)} · ${data.totalElements} solicitações`;
    } catch { if (current === sequence) showState(page, "error"); }
  }
  async function open(id) {
    if (refresh) refresh.hidden = true;
    const current = ++detailSequence; active = null; clearInvitation(); details.replaceChildren(); notes.value = ""; feedback.textContent = "Carregando detalhes…";
    reissue.hidden = true; page.querySelectorAll("[data-request-decision]").forEach((b) => { b.disabled = true; });
    if (!dialog.open) dialog.showModal();
    try {
      const data = await getRequest(type.value, id); if (current !== detailSequence || !dialog.open) return;
      active = { ...data, type: type.value }; renderDetails(details, data); feedback.textContent = "";
      page.querySelectorAll("[data-request-decision]").forEach((b) => { b.disabled = data.summary.status !== "PENDING"; });
      page.querySelector("[data-request-role-control]").hidden = active.type !== "ACCESS" || data.summary.status !== "PENDING";
      role.value = "EDITOR"; reissue.hidden = data.requesterUserStatus !== "INVITED" || data.summary.status !== "APPROVED";
    } catch (error) { feedback.textContent = error.message || "Não foi possível carregar os detalhes."; }
  }
  page.querySelector("[data-admin-requests-list]").addEventListener("click", (e) => { const button = e.target.closest("[data-request-details]"); if (button) open(button.dataset.requestId); });
  [type, status].forEach((control) => control.addEventListener("change", () => { pageNumber = 0; load(); }));
  prev.addEventListener("click", () => { if (pageNumber) { pageNumber--; load(); } }); next.addEventListener("click", () => { pageNumber++; load(); });
  page.querySelector("[data-request-reload]").addEventListener("click", load);
  page.querySelector("[data-close-request-dialog]").addEventListener("click", () => { if (!deciding) dialog.close(); });
  dialog.addEventListener("cancel", (e) => { if (deciding) e.preventDefault(); });
  dialog.addEventListener("click", (e) => { if (e.target === dialog && !deciding) dialog.close(); });
  dialog.addEventListener("close", () => { detailSequence++; active = null; });
  refresh?.addEventListener("click", () => { if (active && !deciding) void open(active.summary.id); });
  page.querySelectorAll("[data-request-decision]").forEach((button) => button.addEventListener("click", () => {
    if (deciding || !active || active.summary.status !== "PENDING") return;
    const action = button.dataset.requestDecision;
    if (action !== "approve" && !notes.value.trim()) { feedback.textContent = "Informe o motivo da decisão."; notes.focus(); return; }
    const grant = active.type === "ACCESS" && action === "approve" ? ` Conceder ${role.value} à conta solicitante?` : "";
    if (!window.confirm(`Confirmar esta decisão para ${active.summary.institutionName}?${grant}`)) return;
    return withSubmission(dialog, async () => {
      deciding = true; clearInvitation();
      const originalLabel = button.textContent; button.textContent = "Processando…"; feedback.textContent = "Registrando decisão…";
      try {
        const result = await decideRequest(active.type, active.summary.id, action, notes.value, role.value);
        active.summary.status = result.status;
        dialog.close(); notify(`Solicitação ${labels[result.status].toLowerCase()} com sucesso.`); showInvitation(result.invitation);
        notice?.focus(); await load();
      } catch (error) {
        feedback.textContent = errorMessage(error);
        if (error.status === 409) { feedback.textContent += " Atualize os detalhes antes de decidir novamente."; if (refresh) refresh.hidden = false; active.summary.status = "CONFLICT"; }
      }
      finally { deciding = false; button.textContent = originalLabel; }
    }).finally(() => { page.querySelectorAll("[data-request-decision]").forEach((b) => { b.disabled = active?.summary.status !== "PENDING"; }); });
  }));
  reissue.addEventListener("click", () => withSubmission(dialog, async () => {
    if (!active?.requesterUserId) return; deciding = true;
    try { const result = await reissueInvitation(active.requesterUserId); feedback.textContent = "Novo convite emitido; os anteriores foram invalidados."; showInvitation(result); }
    catch (error) { feedback.textContent = error.message || "Não foi possível reemitir o convite."; }
    finally { deciding = false; }
  }).finally(() => page.querySelectorAll("[data-request-decision]").forEach((b) => { b.disabled = active?.summary.status !== "PENDING"; })));
  page.querySelector("[data-copy-invitation]").addEventListener("click", async () => {
    if (!url.value) return;
    try { await navigator.clipboard.writeText(url.value); notify("Link copiado. Compartilhe por um canal privado."); }
    catch { url.focus(); url.select(); notify("Selecione e copie o link manualmente.", "pending"); }
  });
  window.addEventListener("pagehide", () => { leaving = true; clearInvitation(); });
  window.addEventListener("pageshow", () => { leaving = false; });
  load().then(() => { if (params.get("requestId")) open(params.get("requestId")); });
}
export function renderAdminRequests(page, requests) {
  const list = page.querySelector("[data-admin-requests-list]"); list.replaceChildren(...requests.map((r) => {
    const row = document.createElement("tr");
    [r.institutionName, r.responsibleName, r.type === "ACCESS" ? "Acesso" : "Cadastro", formatDate(r.createdAt), labels[r.status]].forEach((value, index) => { const td = document.createElement("td"); td.dataset.label = ["Instituição", "Responsável", "Tipo", "Data", "Status"][index]; if (index === 4) td.dataset.status = r.status.toLowerCase(); td.textContent = value; row.append(td); });
    const td = document.createElement("td"), button = document.createElement("button"); td.dataset.label = "Ação"; button.type = "button"; button.className = "admin-list-table__action"; button.dataset.requestDetails = ""; button.dataset.requestId = r.id; button.textContent = "Ver detalhes"; td.append(button); row.append(td); return row;
  })); showState(page, requests.length ? "table" : "empty");
}
function renderDetails(container, data) {
  const rows = [["Tipo", data.summary.type === "ACCESS" ? "Acesso" : "Cadastro"], ["Instituição", data.summary.institutionName], ["Data", formatDate(data.summary.createdAt)], ["Status", labels[data.summary.status]]];
  const r = data.registration, a = data.access;
  if (r) rows.push(["Razão social", r.legalName], ["CNPJ", r.cnpj], ["Categoria", data.categoryName || `#${r.categoryId}`], ["Objetivo", r.objective], ["Descrição", r.description], ["História", r.history], ["Site", r.website], ["Responsável", r.responsible.name], ["Função", r.responsible.role], ["E-mail de acesso", r.responsible.email], ["Telefone", formatPhone(r.responsible.phone)], ["Contatos públicos", contacts(r.publicContacts)]);
  data.units.forEach((u) => { const d = u.data; rows.push(["Unidade", d.name], ["Documento da unidade", d.document], ["Descrição da unidade", d.description], ["Endereço", `${d.street}, ${d.number}${d.complement ? ` — ${d.complement}` : ""}, ${d.neighborhood}, ${u.city}/${u.state}, CEP ${d.postalCode}`], ["Contatos da unidade", contacts(d.contacts)]); });
  if (a) rows.push(["Solicitante", a.requesterName], ["E-mail", a.requesterEmail], ["Telefone", formatPhone(a.requesterPhone)], ["Função declarada", a.requesterRole], ["Justificativa", a.justification]);
  rows.push(["Conta vinculada", data.requesterUserId ? `#${data.requesterUserId} (${data.requesterUserStatus})` : "Sem conta vinculada"], ["Revisor", data.reviewedByUserId ? `#${data.reviewedByUserId}` : "—"], ["Revisado em", formatDate(data.reviewedAt)], ["Notas da análise", data.reviewNotes]);
  container.replaceChildren(...rows.map(([label, value]) => { const wrapper = document.createElement("div"), dt = document.createElement("dt"), dd = document.createElement("dd"); dt.textContent = label; dd.textContent = value || "—"; wrapper.append(dt, dd); return wrapper; }));
}
function contacts(values) { return values.map((c) => `${c.type}${c.label ? ` (${c.label})` : ""}: ${formatContact(c)}${c.isPrimary ? " — principal" : ""}`).join("\n"); }
function formatDate(value) { return value ? new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" }).format(new Date(value)) : "—"; }
export function showAdminRequestsLoading(page) { showState(page, "loading"); }
export function showAdminRequestsError(page) { showState(page, "error"); }
function showState(page, active) { ["loading", "error", "empty", "table"].forEach((state) => { page.querySelector(`[data-admin-requests-${state}]`).hidden = state !== active; }); }

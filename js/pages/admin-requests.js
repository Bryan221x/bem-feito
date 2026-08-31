const requestStates = new WeakMap();

export function initAdminRequestsPage() {
  const page = document.querySelector("[data-admin-requests-page]");
  if (!page) return;
  requestStates.set(page, { requests: [], activeRequest: null });
  page.querySelector("[data-admin-requests-list]")?.addEventListener("click", (event) => {
    const button = event.target.closest("[data-request-details]"); if (button) openRequestDialog(page, button.dataset.requestId);
  });
  page.querySelector("[data-close-request-dialog]")?.addEventListener("click", () => page.querySelector("[data-request-dialog]")?.close());
  page.querySelector("[data-request-dialog]")?.addEventListener("click", (event) => { if (event.target === event.currentTarget) event.currentTarget.close(); });
  page.querySelectorAll("[data-request-decision]").forEach((button) => button.addEventListener("click", () => dispatchDecision(page, button.dataset.requestDecision)));
  renderAdminRequests(page, []);
}

export function renderAdminRequests(page, requests) {
  const state = requestStates.get(page); if (!state || !Array.isArray(requests)) return;
  state.requests = requests.filter((item) => item?.id && (item.institution?.displayName || item.institutionName));
  const list = page.querySelector("[data-admin-requests-list]"); if (list) list.replaceChildren(...state.requests.map(createRow));
  showState(page, state.requests.length ? "table" : "empty");
  const requestedId = new URLSearchParams(window.location.search).get("requestId"); if (requestedId && state.requests.some((item) => String(item.id) === requestedId)) openRequestDialog(page, requestedId);
}

export function showAdminRequestsLoading(page) { showState(page, "loading"); }
export function showAdminRequestsError(page) { showState(page, "error"); }

function createRow(request) {
  const row = document.createElement("tr");
  row.append(cell(request.institution?.displayName || request.institutionName, "Instituição"), cell(request.responsible?.name || request.responsibleName || "—", "Responsável"), cell(formatDate(request.createdAt), "Data"), cell(request.statusLabel || statusLabel(request.status), "Status"));
  const actionCell = cell("", "Ação"); const button = document.createElement("button"); button.type = "button"; button.className = "admin-list-table__action"; button.dataset.requestDetails = ""; button.dataset.requestId = request.id; button.textContent = "Ver detalhes"; actionCell.append(button); row.append(actionCell); return row;
}

function openRequestDialog(page, requestId) {
  const state = requestStates.get(page); const request = state?.requests.find((item) => String(item.id) === String(requestId)); const dialog = page.querySelector("[data-request-dialog]"); const details = page.querySelector("[data-request-details]"); if (!request || !dialog || !details) return;
  state.activeRequest = request;
  const rows = [
    ["Nome de exibição", request.institution?.displayName || request.institutionName],
    ["Razão social", request.institution?.legalName],
    ["CNPJ", request.institution?.document || request.document],
    ["Categoria", request.institution?.category],
    ["Objetivo", request.institution?.objective],
    ["Descrição", request.institution?.description],
    ["Responsável", request.responsible?.name || request.responsibleName],
    ["Cargo/Função", request.responsible?.role],
    ["E-mail de acesso", request.responsible?.accessEmail || request.email],
    ["Telefone", request.responsible?.phone || request.phone],
    ["Contatos públicos", formatContacts(request.publicContacts)],
    ["Unidade principal", formatMainUnit(request.mainUnit)],
    ["Data", formatDate(request.createdAt)],
    ["Status", request.statusLabel || statusLabel(request.status)],
  ];
  details.replaceChildren(...rows.map(([label, value]) => { const wrapper = document.createElement("div"); const dt = document.createElement("dt"); dt.textContent = label; const dd = document.createElement("dd"); dd.textContent = value || "Não informado"; wrapper.append(dt, dd); return wrapper; }));
  const feedback = page.querySelector("[data-request-dialog-feedback]"); if (feedback) feedback.textContent = "";
  dialog.showModal();
}

function dispatchDecision(page, decision) {
  const state = requestStates.get(page); if (!state?.activeRequest) return;
  page.dispatchEvent(new CustomEvent("bemfeito:admin-request-decision-requested", { bubbles: true, detail: { requestId: state.activeRequest.id, decision } }));
  const feedback = page.querySelector("[data-request-dialog-feedback]"); if (feedback) feedback.textContent = `A decisão de ${decision === "APPROVE" ? "aprovação" : "rejeição"} foi preparada. Nenhum status foi alterado sem a API.`;
}

function cell(value, label) { const td = document.createElement("td"); td.dataset.label = label; td.textContent = value || "—"; return td; }
function statusLabel(value) { return { PENDING: "Pendente", APPROVED: "Aprovada", REJECTED: "Rejeitada" }[String(value || "PENDING").toUpperCase()] || "Não informado"; }
function formatContacts(contacts) { return Array.isArray(contacts) && contacts.length ? contacts.filter((contact) => contact?.value).map((contact) => `${contact.label || contact.type || "Contato"}: ${contact.value}`).join("\n") : null; }
function formatMainUnit(unit) { if (!unit) return null; const address = unit.address || {}; const location = [address.street, address.number, address.neighborhood, [address.city, address.state].filter(Boolean).join(" - ")].filter(Boolean).join(", "); return [unit.name, location].filter(Boolean).join(" — ") || null; }
function formatDate(value) { if (!value) return "—"; const date = new Date(value); return Number.isNaN(date.getTime()) ? "—" : new Intl.DateTimeFormat("pt-BR").format(date); }
function showState(page, active) { ["loading", "error", "empty", "table"].forEach((state) => { const element = page.querySelector(`[data-admin-requests-${state}]`); if (element) element.hidden = state !== active; }); }

const institutionStates = new WeakMap();

export function initAdminInstitutionsPage() {
  const page = document.querySelector("[data-admin-institutions-page]");
  if (!page) return;
  institutionStates.set(page, []);
  page.querySelector("[data-admin-institutions-list]")?.addEventListener("click", (event) => {
    const button = event.target.closest("[data-institution-details]");
    if (!button) return;
    page.dispatchEvent(new CustomEvent("bemfeito:admin-institution-details-requested", { bubbles: true, detail: { institutionId: button.dataset.institutionId } }));
  });
  renderAdminInstitutions(page, []);
}

export function renderAdminInstitutions(page, institutions) {
  if (!page || !Array.isArray(institutions)) return;
  const valid = institutions.filter((item) => item?.id && (item.displayName || item.name));
  institutionStates.set(page, valid);
  const list = page.querySelector("[data-admin-institutions-list]");
  if (list) list.replaceChildren(...valid.map(createRow));
  showState(page, valid.length ? "table" : "empty");
}

export function showAdminInstitutionsLoading(page) { showState(page, "loading"); }
export function showAdminInstitutionsError(page) { showState(page, "error"); }

function createRow(institution) {
  const row = document.createElement("tr");
  const values = [institution.displayName || institution.name, institution.document || "—", [institution.city, institution.state].filter(Boolean).join(" - ") || "—", numberOrDash(institution.unitCount), numberOrDash(institution.responsibleCount)];
  const labels = ["Instituição", "CNPJ", "Cidade", "Unidades", "Responsáveis"];
  values.forEach((value, index) => row.append(cell(value, labels[index])));
  const statusCell = cell("", "Status"); const status = document.createElement("span"); status.className = "admin-list-table__status"; status.textContent = institution.statusLabel || statusLabel(institution.status); statusCell.append(status);
  row.append(statusCell, cell(formatDate(institution.createdAt), "Cadastro"));
  const actionCell = cell("", "Ação"); const action = document.createElement("button"); action.type = "button"; action.className = "admin-list-table__action"; action.dataset.institutionDetails = ""; action.dataset.institutionId = institution.id; action.textContent = "Ver detalhes"; actionCell.append(action); row.append(actionCell);
  return row;
}
function cell(value, label) { const td = document.createElement("td"); td.dataset.label = label; td.textContent = value; return td; }
function numberOrDash(value) { return Number.isFinite(Number(value)) ? String(value) : "—"; }
function statusLabel(value) { return { ACTIVE: "Ativa", INACTIVE: "Inativa", PENDING: "Em análise" }[String(value || "").toUpperCase()] || "Não informado"; }
function formatDate(value) { if (!value) return "—"; const date = new Date(value); return Number.isNaN(date.getTime()) ? "—" : new Intl.DateTimeFormat("pt-BR").format(date); }
function showState(page, active) { ["loading", "error", "empty", "table"].forEach((state) => { const element = page.querySelector(`[data-admin-institutions-${state}]`); if (element) element.hidden = state !== active; }); }

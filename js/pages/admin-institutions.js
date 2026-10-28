import { getAdminInstitutions } from "../services/admin-service.js";
import { getInstitution, patchInstitution, getMembers, patchMember } from "../services/institution-service.js";
import { element, createPagination, errorMessage, saving } from "../components/api-ui.js";
const institutionStates = new WeakMap();

export function initAdminInstitutionsPage() {
  const page = document.querySelector("[data-admin-institutions-page]");
  if (!page) return;
  const list = page.querySelector("[data-admin-institutions-list]");
  const search = element("input"), label = element("label", "Buscar instituição ou CNPJ"), filter = element("select"), filterLabel = element("label", "Status");
  search.type = "search"; search.maxLength = 150; label.append(search); filterLabel.append(filter);
  filter.append(new Option("Todos", ""), new Option("Ativa", "ACTIVE"), new Option("Inativa", "INACTIVE"), new Option("Suspensa", "SUSPENDED"));
  const toolbar = element("form", null, "api-pagination"), submit = element("button", "Buscar", "button button--outline"); submit.type = "submit"; toolbar.append(label, filterLabel, submit); page.querySelector("[data-admin-institutions-table]").before(toolbar);
  const pagination = createPagination(page.querySelector("[data-admin-institutions-table]"), load); let sequence = 0;
  async function load(number = 0) {
    const current = ++sequence; showAdminInstitutionsLoading(page); pagination.busy();
    try { const data = await getAdminInstitutions(number, search.value, filter.value); if (current !== sequence) return; renderAdminInstitutions(page, data.content); pagination.update(data); }
    catch { if (current === sequence) { showAdminInstitutionsError(page); pagination.failed(() => load(number)); } }
  }
  toolbar.addEventListener("submit", (event) => { event.preventDefault(); void load(); }); filter.addEventListener("change", () => { void load(); });
  list.addEventListener("click", (event) => { const button = event.target.closest("[data-institution-details]"); if (button) void openDetail(page, button.dataset.institutionId, load); });
  void load();
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
  const values = [institution.displayName || institution.name, institution.cnpj || "—", institution.location || "—", numberOrDash(institution.unitCount), numberOrDash(institution.responsibleCount)];
  const labels = ["Instituição", "CNPJ", "Cidade", "Unidades", "Responsáveis"];
  values.forEach((value, index) => row.append(cell(value, labels[index])));
  const statusCell = cell("", "Status"); const status = document.createElement("span"); status.className = "admin-list-table__status"; status.textContent = institution.statusLabel || statusLabel(institution.status); statusCell.append(status);
  row.append(statusCell, cell(formatDate(institution.createdAt), "Cadastro"));
  const actionCell = cell("", "Ação"); const action = document.createElement("button"); action.type = "button"; action.className = "admin-list-table__action"; action.dataset.institutionDetails = ""; action.dataset.institutionId = institution.id; action.textContent = "Ver detalhes"; actionCell.append(action); row.append(actionCell);
  return row;
}
function cell(value, label) { const td = document.createElement("td"); td.dataset.label = label; td.textContent = value; return td; }
function numberOrDash(value) { return Number.isFinite(Number(value)) ? String(value) : "—"; }
function statusLabel(value) { return { ACTIVE: "Ativa", INACTIVE: "Inativa", PENDING: "Em análise", SUSPENDED: "Suspensa" }[String(value || "").toUpperCase()] || "Não informado"; }
function formatDate(value) { if (!value) return "—"; const date = new Date(value); return Number.isNaN(date.getTime()) ? "—" : new Intl.DateTimeFormat("pt-BR").format(date); }
function showState(page, active) { ["loading", "error", "empty", "table"].forEach((state) => { const element = page.querySelector(`[data-admin-institutions-${state}]`); if (element) element.hidden = state !== active; }); }

async function openDetail(page, id, reload) {
  const dialog = element("dialog", null, "api-detail-dialog"), title = element("h2", "Detalhes da instituição"), close = element("button", "Fechar", "button button--outline"), feedback = element("p", "Carregando…"), content = element("div");
  title.id = "admin-institution-dialog-title"; dialog.setAttribute("aria-labelledby", title.id); feedback.setAttribute("role", "status"); close.type = "button"; close.addEventListener("click", () => dialog.close());
  dialog.append(title, close, feedback, content); page.append(dialog); dialog.addEventListener("close", () => dialog.remove(), { once: true }); dialog.showModal();
  try {
    const [institution, members] = await Promise.all([getInstitution(id), getMembers(id)]); if (!dialog.open) return; feedback.textContent = "";
    const data = element("dl");
    for (const [label, value] of [["Nome", institution.displayName], ["Razão social", institution.legalName], ["CNPJ", institution.cnpj], ["Categoria", institution.category], ["Status", statusLabel(institution.status)]]) data.append(element("dt", label), element("dd", value));
    const edit = element("a", "Gerenciar instituição e unidades", "button button--outline"); edit.href = `./my-institution.html?institutionId=${encodeURIComponent(id)}`;
    const status = element("select"), statusLabelNode = element("label", "Status global"), save = element("button", "Salvar status", "button button--primary");
    status.append(...["ACTIVE", "INACTIVE", "SUSPENDED"].map((value) => new Option(statusLabel(value), value))); status.value = institution.status; statusLabelNode.append(status); save.type = "button";
    save.addEventListener("click", () => {
      const next = status.value;
      if (next !== "ACTIVE" && !window.confirm(`Confirmar ${statusLabel(next)} para ${institution.displayName}? A instituição deixará de aparecer publicamente.`)) return;
      void saving(content, (message) => { feedback.textContent = message; }, async () => { await patchInstitution(id, { status: next }); await reload(); });
    }); content.append(data, edit, statusLabelNode, save, element("h3", "Unidades"));
    for (const unit of institution.units) { const link = element("a", `${unit.name} — ${unit.address.city}/${unit.address.state} — ${unit.status}`); link.href = `./unit-form.html?institutionId=${id}&unitId=${unit.id}`; content.append(link); }
    if (!institution.units.length) content.append(element("p", "Nenhuma unidade cadastrada."));
    content.append(element("h3", "Responsáveis e vínculos"));
    if (!members.length) content.append(element("p", "Nenhum vínculo cadastrado."));
    for (const member of members) {
      const section = element("section"), role = element("select"), state = element("select"), roleLabel = element("label", "Permissão"), stateLabel = element("label", "Estado do vínculo"), button = element("button", "Salvar vínculo", "button button--outline");
      role.append(...["OWNER", "MANAGER", "EDITOR"].map((v) => new Option(v, v))); state.append(...["ACTIVE", "PENDING", "REVOKED"].map((v) => new Option(v, v))); role.value = member.role; state.value = member.status; roleLabel.append(role); stateLabel.append(state); button.type = "button";
      button.addEventListener("click", () => { const body = { role: role.value, status: state.value }; void saving(section, (message) => { feedback.textContent = message; }, async () => { await patchMember(id, member.id, body); }); });
      section.append(element("p", `${member.user.name} — ${member.user.email} — conta ${member.user.status}`), roleLabel, stateLabel, button); content.append(section);
    }
  } catch (error) { feedback.textContent = errorMessage(error); }
}

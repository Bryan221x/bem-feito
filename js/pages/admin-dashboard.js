export function initAdminDashboardPage() {
  const page = document.querySelector("[data-admin-dashboard-page]");
  if (!page) return;
  renderAdminDashboard(page, { metrics: null, requests: [], activities: [], institutionsByCity: [] });
}

/* Contrato de entrada para dados agregados da futura API administrativa. */
export function renderAdminDashboard(page, data = {}) {
  if (!page) return;
  renderMetrics(page, data.metrics);
  renderRequests(page, data.requests);
  renderActivities(page, data.activities);
  renderCityDistribution(page, data.institutionsByCity);
}

export function showAdminDashboardLoading(page) { setFeedback(page, "Carregando dados administrativos..."); }
export function showAdminDashboardError(page) { setFeedback(page, "Não foi possível carregar os dados administrativos.", "error"); }

function renderMetrics(page, metrics) {
  ["pendingRequests", "institutions", "managers", "units"].forEach((key) => {
    const element = page.querySelector(`[data-admin-metric="${key}"]`);
    if (!element) return;
    const value = metrics?.[key];
    const available = Number.isFinite(Number(value));
    element.textContent = available ? new Intl.NumberFormat("pt-BR").format(Number(value)) : "—";
    element.setAttribute("aria-label", available ? `${value}` : "Dado não disponível");
  });
}

function renderRequests(page, requests) {
  const list = page.querySelector("[data-admin-request-list]");
  const table = page.querySelector("[data-admin-requests-table]");
  const empty = page.querySelector("[data-admin-requests-empty]");
  const rows = Array.isArray(requests) ? requests.filter((item) => item?.id && (item.institution?.displayName || item.institutionName)) : [];
  if (!list || !table || !empty) return;
  list.replaceChildren(...rows.slice(0, 5).map(createRequestRow));
  table.hidden = rows.length === 0;
  empty.hidden = rows.length > 0;
}

function createRequestRow(request) {
  const row = document.createElement("tr");
  const name = cell(request.institution?.displayName || request.institutionName);
  const date = cell(formatDate(request.createdAt));
  const statusCell = document.createElement("td");
  const status = document.createElement("span");
  status.className = "admin-dashboard-table__status";
  status.dataset.status = String(request.status || "pending").toLowerCase();
  status.textContent = request.statusLabel || "Pendente";
  statusCell.append(status);
  const actionCell = document.createElement("td");
  const link = document.createElement("a");
  link.className = "admin-dashboard-table__action";
  link.href = `./admin-requests.html?requestId=${encodeURIComponent(request.id)}`;
  link.textContent = "Ver detalhes";
  actionCell.append(link);
  row.append(name, date, statusCell, actionCell);
  return row;
}

function renderActivities(page, activities) {
  const list = page.querySelector("[data-admin-activity-list]");
  const empty = page.querySelector("[data-admin-activities-empty]");
  const items = Array.isArray(activities) ? activities.filter((item) => item?.description) : [];
  if (!list || !empty) return;
  list.replaceChildren(...items.slice(0, 6).map((activity) => {
    const item = document.createElement("li");
    const text = document.createElement("span"); text.textContent = activity.description;
    const date = document.createElement("time"); date.dateTime = activity.createdAt || ""; date.textContent = formatDateTime(activity.createdAt);
    item.append(text, date); return item;
  }));
  list.hidden = items.length === 0;
  empty.hidden = items.length > 0;
}

function renderCityDistribution(page, values) {
  const list = page.querySelector("[data-admin-city-chart]");
  const empty = page.querySelector("[data-admin-city-empty]");
  const items = Array.isArray(values) ? values.filter((item) => item?.city && Number.isFinite(Number(item.count))) : [];
  if (!list || !empty) return;
  const max = Math.max(...items.map((item) => Number(item.count)), 1);
  list.replaceChildren(...items.map((item) => {
    const row = document.createElement("li");
    row.className = "admin-bar-list__item";
    const label = document.createElement("span"); label.className = "admin-bar-list__label"; label.textContent = item.state ? `${item.city} - ${item.state}` : item.city;
    const track = document.createElement("span"); track.className = "admin-bar-list__track";
    const bar = document.createElement("span"); bar.className = "admin-bar-list__bar"; bar.style.width = `${(Number(item.count) / max) * 100}%`;
    const value = document.createElement("strong"); value.className = "admin-bar-list__value"; value.textContent = String(item.count);
    track.append(bar); row.append(label, track, value); return row;
  }));
  list.hidden = items.length === 0;
  empty.hidden = items.length > 0;
}

function cell(value) { const element = document.createElement("td"); element.textContent = value || "—"; return element; }
function formatDate(value) { if (!value) return "—"; const date = new Date(value); return Number.isNaN(date.getTime()) ? "—" : new Intl.DateTimeFormat("pt-BR").format(date); }
function formatDateTime(value) { if (!value) return "Data não informada"; const date = new Date(value); return Number.isNaN(date.getTime()) ? "Data não informada" : new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" }).format(date); }
function setFeedback(page, message, state = "neutral") { const feedback = page.querySelector("[data-admin-dashboard-feedback]"); if (!feedback) return; feedback.textContent = message; feedback.dataset.state = state; feedback.hidden = false; }

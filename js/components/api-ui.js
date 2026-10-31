export function element(tag, text, className) {
  const node = document.createElement(tag); if (text != null) node.textContent = text; if (className) node.className = className; return node;
}
export function createPagination(after, load) {
  const nav = element("nav", null, "api-pagination"); nav.setAttribute("aria-label", "Paginação");
  const previous = element("button", "Anterior", "button button--outline"), next = element("button", "Próxima", "button button--outline"), label = element("span");
  previous.type = next.type = "button"; label.setAttribute("aria-live", "polite");
  let page = 0; previous.addEventListener("click", () => load(page - 1)); next.addEventListener("click", () => load(page + 1));
  nav.append(previous, label, next); after.after(nav);
  return { busy() { previous.disabled = next.disabled = true; }, update(data) { page = data.page; previous.disabled = page === 0; next.disabled = page + 1 >= data.totalPages; label.textContent = `Página ${page + 1} de ${Math.max(1, data.totalPages)} · ${data.totalElements} registros`; },
    failed(retry) { label.replaceChildren(); const button = element("button", "Tentar novamente", "button button--outline"); button.type = "button"; button.addEventListener("click", retry); label.append(button); } };
}
export function errorMessage(error) {
  const messages = { 401: "Sua sessão expirou. Entre novamente.", 403: "Você não tem permissão ou sua sessão de segurança expirou.", 404: "O registro não está disponível.", 409: "Conflito nos dados. Confira o registro antes de tentar novamente." };
  return [messages[error.status] || error.message || "Não foi possível conectar ao serviço. Tente novamente.",
    error.status === 409 ? error.message : null, ...Object.values(error.fields || {})].filter(Boolean).join(" ");
}
export async function saving(root, feedback, operation) {
  if (root.dataset.saving === "true") return;
  root.dataset.saving = "true"; root.setAttribute("aria-busy", "true");
  const controls = [...root.querySelectorAll("button, input, select, textarea")].map((node) => [node, node.disabled]);
  controls.forEach(([node]) => { node.disabled = true; }); feedback("Salvando…");
  try { await operation(); feedback("Alterações salvas."); return true; }
  catch (error) { feedback(errorMessage(error)); return false; }
  finally { controls.forEach(([node, disabled]) => { node.disabled = disabled; }); delete root.dataset.saving; root.removeAttribute("aria-busy"); }
}
export function preserveContext(id) {
  document.querySelectorAll('a[href]').forEach((link) => {
    const url = new URL(link.href, location.href);
    if (url.origin === location.origin && /\/(my-institution|unit-form)\.html$/.test(url.pathname)) { url.searchParams.set("institutionId", id); link.href = url.href; }
  });
}

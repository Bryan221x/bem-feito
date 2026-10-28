import test, { beforeEach } from "node:test";
import assert from "node:assert/strict";
import { randomBytes } from "node:crypto";

class Element {
  constructor() { this.value = ""; this.hidden = false; this.disabled = false; this.dataset = {}; this.children = []; this.listeners = {}; this.selectors = {}; this.attributes = {}; this.textContent = ""; }
  querySelector(s) { return this.selectors[s] || null; }
  querySelectorAll(s) { return this.selectors[s] || []; }
  addEventListener(type, listener) { (this.listeners[type] ||= []).push(listener); }
  async fire(type, target = this) { for (const fn of this.listeners[type] || []) await fn({ target, currentTarget: this, preventDefault() {} }); }
  append(...nodes) { this.children.push(...nodes); }
  replaceChildren(...nodes) { this.children = nodes; }
  setAttribute(k, v) { this.attributes[k] = v; }
  removeAttribute(k) { delete this.attributes[k]; }
  focus() { this.focused = true; }
  checkValidity() { return !!this.value.trim(); }
  reportValidity() { return true; }
  reset() { this.resets = (this.resets || 0) + 1; }
}
let currentPage = null, pageSelector = "";
globalThis.location = { protocol: "http:", hostname: "127.0.0.1", pathname: "/pages/account-activation.html", search: "", hash: "" };
globalThis.document = { querySelector: (s) => s === pageSelector ? currentPage : null, createElement: () => new Element(), dispatchEvent() {} };
globalThis.window = { location, history: { replaceState() {} }, addEventListener() {} };
globalThis.localStorage = globalThis.sessionStorage = new Proxy({}, { get() { throw new Error("No browser storage allowed"); } });
const RealFormData = globalThis.FormData;
globalThis.FormData = class { constructor(form) { this.form = form; } get(key) { return this.form.fields[key]?.value || ""; } };
const api = await import("../../js/services/api-client.js");
const service = await import("../../js/services/request-service.js");
const registration = await import("../../js/pages/registration-request.js");
const access = await import("../../js/pages/institution-access-request.js");
const adminRequests = await import("../../js/pages/admin-requests.js");
const activation = await import("../../js/pages/account-activation.js");
const response = (body, status = 200) => new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
const tick = () => new Promise((resolve) => setTimeout(resolve, 0));
let calls;
beforeEach(() => { api.clearCsrfToken(); calls = []; currentPage = null; pageSelector = ""; location.hash = "";
  globalThis.fetch = async (url, options) => { calls.push({ url, options }); return response(url.endsWith("/csrf") ? { token: "csrf-test", headerName: "X-CSRF-TOKEN" } : {}); };
});
function route(handler) { globalThis.fetch = async (url, options) => { calls.push({ url, options }); if (url.endsWith("/csrf")) return response({ token: "csrf-test", headerName: "X-CSRF-TOKEN" }); return handler(url, options); }; }
function form(fields) { const f = new Element(); f.fields = Object.fromEntries(Object.entries(fields).map(([k, v]) => { const e = new Element(); e.value = v; e.name = k; return [k, e]; })); f.elements = { namedItem: (k) => f.fields[k] || null }; f.selectors['button[type="submit"], [data-request-decision], [data-reissue-invitation]'] = [new Element()]; f.selectors['button[type="submit"]'] = f.selectors['button[type="submit"], [data-request-decision], [data-reissue-invitation]'][0]; return f; }
function registrationPage() {
  const page = new Element(); const f = form({ displayName: "Instituição", legalName: "Razão", document: "04252011000110", category: "1", objective: "Objetivo", description: "Descrição", responsibleName: "Pessoa", responsibleRole: "Diretoria", responsibleEmail: "person@example.invalid", responsiblePhone: "31999990000", unitName: "Unidade", postalCode: "35595000", street: "Rua", number: "1", complement: "", neighborhood: "Centro", city: "2", state: "MG", website: "", history: "" });
  page.selectors["[data-registration-request-form]"] = f;
  for (const name of ["reference-message", "reference-retry", "registration-form-message", "public-contact-type", "public-contact-value", "public-contact-label", "public-contact-error", "public-contact-list", "public-contact-empty"]) page.selectors[`[data-${name}]`] = new Element();
  f.selectors["[data-add-public-contact]"] = new Element();
  page.selectors["[data-public-contact-type]"].value = "EMAIL";
  page.selectors["[data-public-contact-value]"].value = "PUBLIC@EXAMPLE.INVALID";
  currentPage = page; pageSelector = "[data-registration-request-page]";
  return { page, f };
}

test("category/state/city endpoints preserve real IDs and URL-encode UF", async () => {
  route((url) => response(url.includes("cities") ? [{ id: 25, name: "Cidade", state: { uf: "MG" } }] : [{ id: 2, name: "Categoria", uf: "MG" }]));
  assert.equal((await service.getCategories())[0].id, 2); await service.getStates(); await service.getCities("MG");
  assert.ok(calls.some((c) => c.url.endsWith("/api/cities?state=MG")));
});
test("registration page loads selectors and sends the actual API contract", async () => {
  route((url) => response(url.includes("institution-categories") ? [{ id: 1, name: "Educação" }] : url.endsWith("/states") ? [{ id: 1, name: "Minas Gerais", uf: "MG" }] : url.includes("cities?") ? [{ id: 2, name: "Luz" }] : { id: 71, status: "PENDING" }));
  const { page, f } = registrationPage(); registration.initRegistrationRequestPage(); await tick();
  assert.equal(f.fields.category.children[1].value, 1); assert.equal(f.fields.state.children[1].value, "MG");
  await f.fields.state.fire("change"); assert.equal(f.fields.city.children[1].value, 2);
  await f.selectors["[data-add-public-contact]"].fire("click"); await f.fire("submit");
  const request = calls.find((c) => c.url.endsWith("/api/registration-requests")); const body = JSON.parse(request.options.body);
  assert.equal(body.categoryId, 1); assert.equal(body.mainUnit.cityId, 2); assert.equal(body.responsible.role, "Diretoria"); assert.equal(body.publicContacts[0].value, "public@example.invalid");
  assert.equal(body.institution, undefined); assert.match(page.selectors["[data-registration-form-message]"].textContent, /#71/); assert.equal(f.resets, 1);
});
test("registration handles empty taxonomy and reference failure", async () => {
  route(() => response([])); const { page, f } = registrationPage(); registration.initRegistrationRequestPage(); await tick();
  assert.equal(f.fields.category.disabled, true); assert.match(page.selectors["[data-reference-message]"].textContent, /indisponíveis/);
  route(() => Promise.reject(new TypeError("Network"))); await page.selectors["[data-reference-retry]"].fire("click");
  // Retry is bound inside the form in the production DOM; reinitialize verifies failure state directly.
  registration.initRegistrationRequestPage(); await tick(); assert.match(page.selectors["[data-reference-message]"].textContent, /Não foi possível/);
});
test("registration backend errors do not announce success or clear the form", async () => {
  route((url) => url.endsWith("/registration-requests") ? response({ message: "CNPJ já cadastrado", fields: { cnpj: "Duplicado" } }, 409) : response([{ id: 1, name: "Opção", uf: "MG" }]));
  const { page, f } = registrationPage(); registration.initRegistrationRequestPage(); await tick(); await f.selectors["[data-add-public-contact]"].fire("click"); await f.fire("submit");
  assert.match(page.selectors["[data-registration-form-message]"].textContent, /CNPJ/); assert.equal(f.resets, undefined); assert.equal(f.fields.document.attributes["aria-invalid"], "true");
});
test("access service requires selection and sends a real institutionId", async () => {
  await assert.rejects(service.submitAccess({ institutionId: null }), /Selecione/); assert.equal(calls.length, 0);
  await service.submitAccess({ institutionId: 5, requesterRole: "Diretoria" }); const body = JSON.parse(calls.at(-1).options.body); assert.equal(body.institutionId, 5); assert.equal(body.requesterUserId, undefined);
});
test("access page debounces search and requires selecting a result", async () => {
  route((url) => url.endsWith("/auth/me") ? response({ message: "Anônimo" }, 401) : url.includes("access-search") ? response([{ id: 9, displayName: "Instituição real", category: "Educação", locations: [] }]) : response({ id: 82 }));
  const page = new Element(), f = form({ institutionQuery: "In", responsibleName: "Pessoa", email: "person@example.invalid", phone: "31999990000", role: "Diretoria", justification: "Vínculo" });
  page.selectors["[data-institution-access-form]"] = f;
  for (const name of ["search-results", "search-feedback", "selected-institution", "search-register", "access-message"]) page.selectors[`[data-${name}]`] = new Element();
  currentPage = page; pageSelector = "[data-institution-access-request-page]"; access.initInstitutionAccessRequestPage(); await tick();
  await f.fire("submit"); assert.match(page.selectors["[data-access-message]"].textContent, /selecione/);
  await f.fields.institutionQuery.fire("input"); await f.fields.institutionQuery.fire("input"); await new Promise((resolve) => setTimeout(resolve, 380));
  assert.equal(calls.filter((c) => c.url.includes("access-search")).length, 1);
  await page.selectors["[data-search-results]"].children[0].children[0].fire("click"); await f.fire("submit");
  assert.equal(JSON.parse(calls.find((c) => c.url.endsWith("/api/access-requests")).options.body).institutionId, 9);
  assert.match(page.selectors["[data-access-message]"].textContent, /#82/);
});
test("admin decisions require explicit membership role and notes", async () => {
  assert.throws(() => service.decideRequest("ACCESS", 1, "approve", "", undefined), /permissão/);
  assert.throws(() => service.decideRequest("REGISTRATION", 1, "reject", ""), /motivo/);
  await service.decideRequest("ACCESS", 1, "approve", "Conferido", "MANAGER"); const last = calls.at(-1);
  assert.equal(last.options.headers.get("X-CSRF-TOKEN"), "csrf-test"); assert.equal(JSON.parse(last.options.body).membershipRole, "MANAGER");
});
test("manual invitation is returned only from the administrative mutation", async () => {
  const secret = randomBytes(32).toString("base64url"), url = `http://127.0.0.1:5500/pages/account-activation.html#token=${secret}`;
  route(() => response({ deliveryMode: "MANUAL", activationUrl: url })); const result = await service.reissueInvitation(7);
  assert.equal(result.activationUrl, url); assert.ok(calls.at(-1).url.endsWith("/api/admin/users/7/invitation")); assert.equal(calls.at(-1).options.method, "POST");
});
test("duplicate submission is suppressed and controls recover after failure", async () => {
  const owner = form({}); let resolve, runs = 0;
  const first = service.withSubmission(owner, () => { runs++; return new Promise((r) => { resolve = r; }); });
  await service.withSubmission(owner, () => { runs++; }); assert.equal(runs, 1); assert.equal(owner.attributes["aria-busy"], "true"); resolve(); await first;
  await assert.rejects(service.withSubmission(owner, async () => { throw new Error("Rejected"); })); assert.equal(owner.attributes["aria-busy"], undefined);
  assert.equal(owner.selectors['button[type="submit"]'].disabled, false);
});
test("activation immediately removes the fragment and rejects malformed tokens", () => {
  const raw = randomBytes(32).toString("base64url"); const replaced = []; const loc = { hash: `#token=${raw}`, pathname: "/pages/account-activation.html", search: "" };
  assert.equal(activation.takeInvitationToken(loc, { replaceState: (...args) => replaced.push(args) }), raw); assert.equal(replaced[0][2], loc.pathname);
  assert.equal(activation.takeInvitationToken({ ...loc, hash: "#token=bad" }, { replaceState() {} }), "");
});
test("activation uses POST bodies, CSRF and clears passwords without automatic login", async () => {
  const raw = randomBytes(32).toString("base64url"); location.hash = `#token=${raw}`;
  route((url) => response(url.endsWith("/inspect") ? { state: "READY", name: "Pessoa", maskedEmail: "p***@example.invalid" } : { state: "SUCCESS" }));
  const page = new Element(), f = form({ password: "a".repeat(15), confirmPassword: "a".repeat(15) });
  page.selectors["[data-activation-form]"] = f; page.selectors["[data-activation-message]"] = new Element(); page.selectors["[data-activation-login]"] = new Element();
  currentPage = page; pageSelector = "[data-account-activation-page]"; await activation.initAccountActivationPage(); assert.equal(page.dataset.state, "ready");
  await f.fire("submit"); assert.equal(page.dataset.state, "success"); assert.equal(f.resets, 1); assert.equal(page.selectors["[data-activation-login]"].hidden, false);
  assert.ok(calls.every((c) => !c.url.includes(raw))); assert.ok(!calls.some((c) => c.url.endsWith("/auth/login")));
  const accept = calls.find((c) => c.url.endsWith("/accept")); assert.equal(JSON.parse(accept.options.body).token, raw); assert.equal(accept.options.headers.get("X-CSRF-TOKEN"), "csrf-test");
});
test("activation validates UTF-8 byte limits before calling accept", async () => {
  location.hash = `#token=${randomBytes(32).toString("base64url")}`; route(() => response({ state: "READY", name: "Pessoa", maskedEmail: "p***@example.invalid" }));
  const page = new Element(), f = form({ password: "ã".repeat(40), confirmPassword: "ã".repeat(40) });
  page.selectors["[data-activation-form]"] = f; page.selectors["[data-activation-message]"] = new Element(); page.selectors["[data-activation-login]"] = new Element();
  currentPage = page; pageSelector = "[data-account-activation-page]"; await activation.initAccountActivationPage(); await f.fire("submit");
  assert.equal(page.dataset.state, "error"); assert.ok(!calls.some((c) => c.url.endsWith("/accept")));
});
test("activation shows expired state without displaying the password form", async () => {
  location.hash = `#token=${randomBytes(32).toString("base64url")}`; route(() => response({ state: "EXPIRED" }));
  const page = new Element(), f = form({}); page.selectors["[data-activation-form]"] = f; page.selectors["[data-activation-message]"] = new Element(); page.selectors["[data-activation-login]"] = new Element();
  currentPage = page; pageSelector = "[data-account-activation-page]"; await activation.initAccountActivationPage(); assert.equal(page.dataset.state, "expired"); assert.equal(f.hidden, true);
});

test("admin dialog sends a real decision, updates status, and clears its manual URL on close", async () => {
  const raw = randomBytes(32).toString("base64url"), activationUrl = `http://127.0.0.1:5500/pages/account-activation.html#token=${raw}`;
  let requestStatus = "PENDING";
  const summary = () => ({ id: 4, type: "REGISTRATION", institutionName: "Instituição", responsibleName: "Pessoa", createdAt: "2026-10-27T12:00:00", status: requestStatus });
  const detail = () => ({ summary: summary(), registration: null, access: null, units: [], requesterUserId: requestStatus === "APPROVED" ? 8 : null, requesterUserStatus: requestStatus === "APPROVED" ? "INVITED" : null });
  route((url) => {
    if (url.endsWith("/4/approve")) { requestStatus = "APPROVED"; return response({ id: 4, status: requestStatus, userId: 8, invitation: { userId: 8, deliveryMode: "MANUAL", activationUrl } }); }
    if (url.endsWith("/4")) return response(detail());
    return response({ content: [summary()], last: true, totalPages: 1, totalElements: 1 });
  });
  const page = new Element();
  for (const name of ["request-dialog", "request-details", "request-dialog-feedback", "request-type", "request-status", "request-prev", "request-next", "request-notes", "request-role", "reissue-invitation", "invitation-result", "invitation-url", "request-pagination", "admin-requests-list", "request-role-control", "request-reload", "close-request-dialog", "copy-invitation", "admin-requests-loading", "admin-requests-error", "admin-requests-empty", "admin-requests-table"]) page.selectors[`[data-${name}]`] = new Element();
  const dialog = page.selectors["[data-request-dialog]"]; dialog.showModal = () => { dialog.open = true; }; dialog.close = () => { dialog.open = false; return dialog.fire("close"); };
  const approve = new Element(); approve.dataset.requestDecision = "approve"; page.selectors["[data-request-decision]"] = [approve];
  dialog.selectors['button[type="submit"], [data-request-decision], [data-reissue-invitation]'] = [approve, page.selectors["[data-reissue-invitation]"]];
  page.selectors["[data-request-type]"].value = "REGISTRATION"; page.selectors["[data-request-role]"].value = "EDITOR";
  location.search = ""; currentPage = page; pageSelector = "[data-admin-requests-page]"; window.confirm = () => true;
  adminRequests.initAdminRequestsPage(); await tick();
  await page.selectors["[data-admin-requests-list]"].fire("click", { closest: () => ({ dataset: { requestId: 4 } }) }); await tick();
  assert.equal(dialog.open, true); assert.equal(approve.disabled, false); page.selectors["[data-request-notes]"].value = "Conferido";
  await approve.fire("click"); assert.equal(requestStatus, "APPROVED"); assert.equal(approve.disabled, true);
  assert.equal(page.selectors["[data-invitation-url]"].value, activationUrl); assert.equal(page.selectors["[data-invitation-result]"].hidden, false);
  assert.match(page.selectors["[data-request-dialog-feedback]"].textContent, /Decisão registrada/);
  await dialog.close(); assert.equal(page.selectors["[data-invitation-url]"].value, ""); assert.equal(page.selectors["[data-invitation-result]"].hidden, true);
});

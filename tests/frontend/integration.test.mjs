import test, { beforeEach } from "node:test";
import assert from "node:assert/strict";
class Element {
  constructor(tag = "div") { this.tagName = tag; this.dataset = {}; this.children = []; this.attributes = {}; this.listeners = {}; this.selectors = {}; this.disabled = false; this.hidden = false; this.value = ""; this.textContent = ""; this.classList = { toggle() {}, add() {}, remove() {} }; }
  querySelector(key) { return this.selectors[key] || null; }
  querySelectorAll(key) { return this.selectors[key] || []; }
  append(...nodes) { this.children.push(...nodes); }
  appendChild(node) { this.append(node); }
  replaceChildren(...nodes) { this.children = nodes; }
  before(node) { this.beforeNode = node; }
  after(node) { this.afterNode = node; }
  setAttribute(key, value) { this.attributes[key] = value; }
  removeAttribute(key) { delete this.attributes[key]; }
  addEventListener(type, fn) { (this.listeners[type] ||= []).push(fn); }
  async fire(type) { for (const fn of this.listeners[type] || []) await fn({ target: this, currentTarget: this, preventDefault() {} }); }
  dispatchEvent() {}
}
let currentPage, currentSelector, calls;
globalThis.location = { protocol: "http:", port: "5500", hostname: "127.0.0.1", href: "http://127.0.0.1:5500/pages/institution.html?id=12", search: "?id=12" };
globalThis.window = { location };
globalThis.document = { querySelector: (s) => s === currentSelector ? currentPage : null, createElement: (tag) => new Element(tag), createDocumentFragment: () => new Element(), dispatchEvent() {} };
const api = await import("../../js/services/api-client.js");
const institutions = await import("../../js/services/institution-service.js");
const units = await import("../../js/services/unit-service.js");
const catalog = await import("../../js/services/public-catalog-service.js");
const admin = await import("../../js/services/admin-service.js");
const detail = await import("../../js/components/institution-details.js");
const maps = await import("../../js/services/map-service.js");
const cities = await import("../../js/pages/cities.js");
const institutionPage = await import("../../js/pages/institution.js");
const ui = await import("../../js/components/api-ui.js");
const response = (body, status = 200) => new Response(JSON.stringify(body), { status });
function route(fn) { globalThis.fetch = async (url, options) => { calls.push({ url, options }); return url.endsWith("/csrf") ? response({ token: "isolated-test-csrf", headerName: "X-CSRF-TOKEN" }) : fn(url, options); }; }
beforeEach(() => { calls = []; api.clearCsrfToken(); currentPage = null; currentSelector = ""; route(() => response({})); });
function nodes(root, names) { for (const name of names) root.selectors[`[data-${name}]`] = new Element(); return root; }
function textTree(node) { return [node.textContent, ...node.children.map(textTree)].join(" "); }
test("public catalog uses available cities, real cityId and bounded pagination", async () => {
  await catalog.getAvailableCities(); await catalog.getPublicInstitutions(72, 3); await catalog.getMapUnits(72, 1);
  assert.ok(calls[0].url.endsWith("/cities/available")); assert.match(calls[1].url, /cityId=72/); assert.match(calls[1].url, /page=3&size=20/); assert.match(calls[2].url, /units\/map\?page=1&size=100/);
});
test("partial PATCH retains omission versus explicit null", async () => {
  const changes = institutions.changedFields({ history: "before", displayName: "Name" }, { history: null, displayName: "Name" });
  assert.deepEqual(changes, { history: null }); await institutions.patchInstitution(7, changes);
  assert.deepEqual(JSON.parse(calls.at(-1).options.body), { history: null }); assert.equal(calls.at(-1).options.method, "PATCH");
});
test("institution and unit contacts use separate CRUD endpoints", async () => {
  const body = institutions.contactPayload({ type: "PHONE", value: "+55 (31) 99999-0000", isPublic: false });
  assert.equal(body.value, "+5531999990000"); assert.equal(body.isPublic, false);
  await institutions.saveContact("institutions", 7, null, body); await institutions.saveContact("units", 9, 2, body); await institutions.deleteContact("units", 9, 2);
  assert.deepEqual(calls.filter((c) => !c.url.endsWith("/csrf")).map((c) => c.options.method), ["POST", "PATCH", "DELETE"]);
});
test("unit create and edit keep owner and unit context", async () => {
  await units.createUnit(7, { cityId: 72, name: "Unidade" }); await units.patchUnit(8, { description: null });
  assert.match(calls[1].url, /institutions\/7\/units$/); assert.match(calls[2].url, /units\/8$/); assert.deepEqual(JSON.parse(calls[2].options.body), { description: null });
});
test("needs are persisted independently including fulfilled and inactive statuses", async () => {
  await units.saveNeed(8, null, { title: "Item", categoryId: 2, status: "ACTIVE" }); await units.saveNeed(8, 4, { status: "FULFILLED" }); await units.saveNeed(8, 4, { status: "INACTIVE" }); await units.deleteNeed(8, 4);
  assert.ok(calls.filter((c) => !c.url.endsWith("/csrf")).every((c) => c.url.includes("/units/8/needs")));
});
test("service areas and opening hours use dedicated contracts", async () => {
  await units.saveServiceAreas(8, [2, 5]); await units.saveOpeningHours(8, 7, { closed: true, opensAt: null, closesAt: null, note: "Feriado" });
  assert.deepEqual(JSON.parse(calls[1].options.body), { serviceAreaIds: [2, 5] }); assert.match(calls[2].url, /opening-hours\/7$/);
});
test("multipart upload preserves browser boundary and CSRF", async () => {
  await institutions.uploadHistoryImage(7, new Blob(["image"], { type: "image/png" }), { alt: "Descrição", caption: "Legenda", order: 2 });
  const request = calls.at(-1); assert.ok(request.options.body instanceof FormData); assert.equal(request.options.headers.has("Content-Type"), false); assert.equal(request.options.headers.get("X-CSRF-TOKEN"), "isolated-test-csrf"); assert.equal(request.options.credentials, "include"); assert.equal(request.options.body.get("altText"), "Descrição");
});
test("managed images use API hostname and unsafe URLs are refused", () => {
  assert.equal(api.apiAssetUrl("/api/history-media/7/image.png"), "http://127.0.0.1:8080/api/history-media/7/image.png");
  assert.equal(api.apiAssetUrl("javascript:alert(1)"), null); assert.equal(api.apiAssetUrl("https://user:pass@example.invalid/photo"), null);
});
test("admin summary and lists use bounded server queries", async () => {
  await admin.getAdminSummary(); await admin.getAdminInstitutions(2, "A & B", "SUSPENDED");
  assert.match(calls[0].url, /admin\/summary$/); assert.match(calls[1].url, /page=2&size=20&q=A\+%26\+B&status=SUSPENDED/);
});
test("absent, empty and out of range coordinates never become fake markers", () => {
  for (const value of [null, undefined, "", " ", "bad", 91]) assert.equal(maps.hasCoordinates({ latitude: value, longitude: 0 }), false);
  assert.equal(maps.hasCoordinates({ latitude: 0, longitude: 0 }), true); assert.equal(maps.hasCoordinates({ latitude: -20, longitude: -45 }), true);
});
test("contact links reject unsafe values and provide type appropriate links", () => {
  assert.equal(detail.contactHref({ type: "EMAIL", value: "a@example.invalid" }), "mailto:a%40example.invalid");
  assert.equal(detail.contactHref({ type: "PHONE", value: "+5531999990000" }), "tel:+5531999990000");
  assert.equal(detail.contactHref({ type: "WHATSAPP", value: "+5531999990000" }), "https://wa.me/5531999990000");
  assert.equal(detail.contactHref({ type: "PHONE", value: "javascript:alert(1)" }), null);
});
test("opening hours distinguish open, closed, notes and missing days", () => {
  const rows = detail.renderOpeningHours([{ dayOfWeek: 1, closed: false, opensAt: "09:00:00", closesAt: "12:00:00", note: "Agendar" }, { dayOfWeek: 2, closed: true }]);
  assert.equal(rows.length, 7); assert.match(rows[0].textContent, /09:00 às 12:00 — Agendar/); assert.match(rows[1].textContent, /Fechado/); assert.match(rows[2].textContent, /Não informado/);
});
test("multiple units keep their needs and private contacts separate", () => {
  const page = new Element(), details = nodes(new Element(), ["institution-units", "institution-units-empty", "institution-name", "institution-contact-section", "institution-general-contacts"]); page.selectors["[data-institution-details]"] = details;
  detail.renderInstitutionDetails(page, { id: 7, displayName: "Name", contacts: [{ type: "EMAIL", value: "private@example.invalid", isPublic: false }], units: [{ id: 1, name: "A", needs: [{ name: "Only A", status: "ACTIVE", category: "Categoria", quantity: 4, unitLabel: "kg" }, { name: "Hidden", status: "FULFILLED" }] }, { id: 2, name: "B", needs: [{ name: "Only B", status: "ACTIVE" }] }] });
  const cards = details.selectors["[data-institution-units]"].children;
  assert.equal(cards.length, 2); assert.match(textTree(cards[0]), /Only A/); assert.doesNotMatch(textTree(cards[0]), /Only B|Hidden/); assert.match(textTree(cards[0]), /4 kg/); assert.match(textTree(cards[1]), /Only B/); assert.equal(details.selectors["[data-institution-general-contacts]"].children.length, 0);
});
test("cities render real IDs and search real names", async () => {
  const page = nodes(new Element(), ["city-list", "city-empty", "city-empty-message", "city-map"]); page.selectors["#city-search-input"] = new Element(); page.selectors[".city-search"] = new Element(); currentPage = page; currentSelector = ".city-selection";
  route((url) => response(url.includes("cities/available") ? [{ id: 72, name: "São Paulo", state: { uf: "SP" } }] : { content: [], totalPages: 0 }));
  await cities.initCitiesPage(); assert.equal(page.selectors["[data-city-list]"].children[0].href, "./institutions.html?cityId=72");
  page.selectors["#city-search-input"].value = "sao"; await page.selectors["#city-search-input"].fire("input"); assert.equal(page.selectors["[data-city-empty]"].hidden, true);
  page.selectors["#city-search-input"].value = "inexistente"; await page.selectors["#city-search-input"].fire("input"); assert.equal(page.selectors["[data-city-empty]"].hidden, false);
});
test("institution page distinguishes 404 from service failure", async () => {
  for (const status of [404, 500]) {
    const page = nodes(new Element(), ["institution-details", "institution-back", "institution-not-found", "institution-error", "institution-loading"]);
    page.selectors["[data-institution-required], [data-institution-loading], [data-institution-not-found], [data-institution-error]"] = Object.values(page.selectors).filter((n) => n instanceof Element);
    currentPage = page; currentSelector = ".institution-page"; route(() => response({ message: "Unavailable" }, status));
    await institutionPage.initInstitutionPage(); assert.equal(page.selectors[status === 404 ? "[data-institution-not-found]" : "[data-institution-error]"].hidden, false);
  }
});
test("ViaCEP is optional, omits session credentials and rejects missing CEP", async () => {
  route(() => response({ erro: true })); await assert.rejects(api.lookupPostalCode("123"), /oito/); await assert.rejects(api.lookupPostalCode("00000000"), /não encontrado/);
  assert.equal(calls.at(-1).options.credentials, "omit"); assert.equal(calls.at(-1).options.referrerPolicy, "no-referrer");
});
test("failed saves preserve form data and always unlock controls", async () => {
  const page = new Element(), input = new Element(); input.value = "unsaved"; page.selectors["button, input, select, textarea"] = [input]; const messages = [];
  await ui.saving(page, (message) => messages.push(message), async () => { throw new api.ApiError(409, { message: "Conflict" }); });
  assert.equal(input.value, "unsaved"); assert.equal(input.disabled, false); assert.equal(page.dataset.saving, undefined); assert.match(messages.at(-1), /Conflito/);
});

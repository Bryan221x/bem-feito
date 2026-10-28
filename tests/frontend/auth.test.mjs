import test, { beforeEach } from "node:test";
import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";

const events = [];
const redirects = [];
let pending = true;
let buttons = [];
let replacedBody = null;
globalThis.location = { protocol: "http:", hostname: "127.0.0.1", pathname: "/pages/institution-selection.html", search: "", replace: (url) => redirects.push(url) };
globalThis.document = {
  querySelector: () => null,
  querySelectorAll: () => buttons,
  dispatchEvent: (event) => events.push(event),
  addEventListener: () => {},
  createElement: () => ({ setAttribute() {}, textContent: "" }),
  body: { replaceChildren: (...nodes) => { replacedBody = nodes; } },
  documentElement: { dataset: { authRequired: "authenticated" }, classList: { remove: () => { pending = false; }, add: () => { pending = true; } } },
};
globalThis.localStorage = new Proxy({}, { get() { throw new Error("Authentication must not use localStorage"); } });
globalThis.sessionStorage = new Proxy({}, { get() { throw new Error("Authentication must not use sessionStorage"); } });
const api = await import("../../js/services/api-client.js");
const auth = await import("../../js/services/auth-service.js");
const guard = await import("../../js/services/auth-guard.js");
const csrf = () => ({ token: randomUUID(), headerName: "X-CSRF-TOKEN", parameterName: "_csrf" });
const response = (body, status = 200) => new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
beforeEach(() => {
  api.clearCsrfToken(); events.length = 0; redirects.length = 0; buttons = []; pending = true; replacedBody = null;
  document.documentElement.dataset.authRequired = "authenticated";
  location.pathname = "/pages/institution-selection.html"; location.search = "";
});

test("mutable requests include credentials and the server-provided CSRF header", async () => {
  const token = csrf(); const calls = [];
  globalThis.fetch = async (url, options) => {
    calls.push({ url, options }); return response(url.endsWith("/csrf") ? token : { id: 1 });
  };
  await api.apiRequest("/api/institutions/1", { method: "PATCH", body: { history: "Updated" } });
  assert.equal(calls.length, 2);
  assert.equal(calls[1].options.credentials, "include");
  assert.equal(calls[1].options.headers.get(token.headerName), token.token);
  assert.deepEqual(JSON.parse(calls[1].options.body), { history: "Updated" });
});
test("login uses form processing, normalizes email and refreshes CSRF after session fixation protection", async () => {
  const calls = []; let csrfCalls = 0;
  globalThis.fetch = async (url, options) => {
    if (url.endsWith("/csrf")) { csrfCalls++; return response(csrf()); }
    calls.push({ url, email: options.body.get("email"), password: options.body.get("password"), body: options.body });
    return response({ authenticated: true, user: { platformRole: "USER" } });
  };
  const secret = randomUUID(); await auth.login("  ACCOUNT@EXAMPLE.INVALID ", secret);
  assert.equal(calls[0].email, "account@example.invalid");
  assert.equal(calls[0].password, secret);
  assert.equal(calls[0].body.has("password"), false);
  assert.equal(csrfCalls, 2);
});
test("401 and 403 are distinct errors and do not automatically retry writes", async () => {
  for (const status of [401, 403]) {
    api.clearCsrfToken(); let mutations = 0;
    globalThis.fetch = async (url) => {
      if (url.endsWith("/csrf")) return response(csrf());
      mutations++; return response({ message: "Denied", fields: {} }, status);
    };
    await assert.rejects(api.apiRequest("/api/units/1", { method: "DELETE" }), (error) => error instanceof api.ApiError && error.status === status);
    assert.equal(mutations, 1);
    assert.equal(events.at(-1).type, `bemfeito:api-${status}`);
  }
});
test("unauthenticated guards redirect without revealing protected content", async () => {
  globalThis.fetch = async () => response({ message: "Authentication required" }, 401);
  assert.equal(await guard.guardPage(), false);
  assert.deepEqual(redirects, ["./institution-login.html"]);
  assert.equal(pending, true);
});
test("non-admin users cannot reveal the global administration pages", async () => {
  document.documentElement.dataset.authRequired = "admin";
  globalThis.fetch = async () => response({ platformRole: "USER" });
  assert.equal(await guard.guardPage(), false);
  assert.deepEqual(redirects, ["./institution-selection.html"]);
  assert.equal(pending, true);
});
test("valid administrators pass the guard and get the admin login destination", async () => {
  document.documentElement.dataset.authRequired = "admin";
  globalThis.fetch = async () => response({ platformRole: "PLATFORM_ADMIN" });
  assert.equal(await guard.guardPage(), true);
  assert.equal(pending, false);
  assert.equal(guard.userDestination({ platformRole: "PLATFORM_ADMIN" }), "./admin-dashboard.html");
  assert.equal(guard.userDestination({ platformRole: "USER" }), "./institution-selection.html");
});
test("institution guards use memberships from the API and refuse a forged selection", async () => {
  location.pathname = "/pages/my-institution.html"; location.search = "?institutionId=2";
  globalThis.fetch = async (url) => response(url.endsWith("/me") ? { platformRole: "USER" } : [{ institutionId: 1, permissions: ["VIEW"] }]);
  assert.equal(await guard.guardPage(), false);
  assert.deepEqual(redirects, ["./institution-selection.html"]);
});
test("editor cannot access unit creation even when belonging to the institution", async () => {
  location.pathname = "/pages/unit-form.html"; location.search = "?institutionId=1";
  globalThis.fetch = async (url) => response(url.endsWith("/me") ? { platformRole: "USER" } : [{ institutionId: 1, permissions: ["VIEW"] }]);
  assert.equal(await guard.guardPage(), false);
  assert.equal(pending, true);
});
test("backend outage fails closed while showing a retry message", async () => {
  globalThis.fetch = async () => { throw new TypeError("Network unavailable"); };
  assert.equal(await guard.guardPage(), false);
  assert.match(replacedBody[0].textContent, /verificar sua sessão/);
});
test("logout waits for server invalidation before redirecting", async () => {
  let listener; let finish;
  const button = { disabled: false, addEventListener: (_name, callback) => { listener = callback; } };
  buttons = [button];
  globalThis.fetch = async (url) => {
    if (url.endsWith("/csrf")) return response(csrf());
    return new Promise((resolve) => { finish = () => resolve(response({ authenticated: false })); });
  };
  guard.initLogout(); const result = listener();
  while (!finish) await new Promise((resolve) => setImmediate(resolve));
  assert.deepEqual(redirects, []); assert.equal(button.disabled, true);
  finish(); await result;
  assert.deepEqual(redirects, ["./institution-login.html"]);
});
test("a page restored from browser history revalidates its session before revealing content", async () => {
  let listener; let finish;
  globalThis.window = { addEventListener: (_name, callback) => { listener = callback; } };
  globalThis.fetch = async () => new Promise((resolve) => { finish = () => resolve(response({ message: "Expired" }, 401)); });
  pending = false; guard.initSessionRecheck(); listener({ persisted: true });
  assert.equal(pending, true); assert.deepEqual(redirects, []);
  finish();
  while (!redirects.length) await new Promise((resolve) => setImmediate(resolve));
  assert.deepEqual(redirects, ["./institution-login.html"]);
  delete globalThis.window;
});

test("Live Server uses the same hostname for the API with both localhost and 127.0.0.1", async () => {
  const original = location.hostname;
  try {
    for (const hostname of ["localhost", "127.0.0.1"]) {
      location.hostname = hostname;
      let requested;
      globalThis.fetch = async (url, options) => {
        requested = { url, options };
        return response(csrf());
      };
      const isolatedClient = await import(`../../js/services/api-client.js?audit=${randomUUID()}`);
      await isolatedClient.getCsrfToken();
      assert.equal(requested.url, `http://${hostname}:8080/api/auth/csrf`);
      assert.equal(requested.options.credentials, "include");
    }
  } finally { location.hostname = original; }
});

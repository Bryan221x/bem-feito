import test from "node:test";
import assert from "node:assert/strict";
globalThis.location = { protocol: "http:", hostname: "localhost", search: "" };
globalThis.document = { querySelector: () => null };
const memory = new Map();
globalThis.localStorage = { getItem: k => memory.get(k), setItem: (k,v) => memory.set(k,v), removeItem: k => memory.delete(k) };
const { chooseInstitution, resolveInstitutionContext } = await import("../../js/services/institution-context.js");
const { setupSteps } = await import("../../js/components/institution-setup.js");
test("context validates URL, preference, revocation and account isolation", () => {
  memory.clear(); const user = { id: 1 }, other = { id: 2 };
  const a = { institutionId: 10, status: "ACTIVE", permissions: ["VIEW"] }, b = { institutionId: 20, status: "ACTIVE", permissions: ["EDIT_PROFILE"] };
  assert.equal(chooseInstitution(user,[a]),a);
  assert.equal(chooseInstitution(user,[a,b],20),b);
  assert.equal(chooseInstitution(user,[a,b]),b);
  assert.equal(chooseInstitution(other,[a,b]),a);
  assert.equal(chooseInstitution(user,[a]),a);
  assert.equal(chooseInstitution(user,[a,b],999),a);
  assert.equal(chooseInstitution(user,[]),null);
  assert.equal(memory.has("bemfeito:institution:1"),false);
});
test("API failure is propagated and does not erase preference", async () => {
  memory.set("bemfeito:institution:1","20"); globalThis.fetch = async () => { throw new TypeError("offline"); };
  await assert.rejects(resolveInstitutionContext({ user: { id:1, platformRole:"USER" }, explicit:null }));
  assert.equal(memory.get("bemfeito:institution:1"),"20");
});
test("setup derives progress and reuses existing main unit", () => {
  const empty = setupSteps({ id:1 }); assert.equal(empty.filter(s=>s.done).length,0);
  const fresh = setupSteps({ id:1, units:[{ id:8 }] }); assert.equal(fresh[2].done,true); assert.match(fresh[2].href,/unitId=8/);
  const full = setupSteps({ id:1, displayName:"A",description:"B",objective:"C",contacts:[{ isPublic:true }],historyImages:[{}],units:[{ id:8,openingHours:[{}],needs:[{ status:"ACTIVE" }] }] });
  assert.equal(full.every(s=>s.done),true);
});

import test from "node:test";
import assert from "node:assert/strict";
import { formatPhone, formatContact, contactHref } from "../../js/services/contact-format.js";
test("Brazilian mobile, landline and masked values are displayed without changing source",()=>{
  assert.equal(formatPhone("37900000003"),"(37) 90000-0003");assert.equal(formatPhone("3733334444"),"(37) 3333-4444");
  assert.equal(formatPhone("(37) 90000-0003"),"(37) 90000-0003");assert.equal(formatPhone("+55 37 90000-0003"),"+55 (37) 90000-0003");
});
test("international numbers are not assigned Brazilian country code; invalid input stays unlinked",()=>{
  assert.equal(contactHref({type:"PHONE",value:"+12025550123"}),"tel:+12025550123");assert.ok(formatPhone("+12025550123").startsWith("+1 "));
  for(const value of ["","123","abc37900000003","javascript:alert(1)"]) assert.equal(contactHref({type:"PHONE",value}),null);
  assert.equal(formatPhone("123"),"123");assert.equal(formatPhone(""),"");
});
test("WhatsApp, telephone and email links use normalized safe values",()=>{
  assert.equal(contactHref({type:"WHATSAPP",value:"(37) 90000-0003"}),"https://wa.me/5537900000003");
  assert.equal(contactHref({type:"WHATSAPP",value:"+55 37 90000-0003"}),"https://wa.me/5537900000003");
  assert.equal(contactHref({type:"EMAIL",value:"person@example.org"}),"mailto:person%40example.org");assert.equal(contactHref({type:"EMAIL",value:"a@example.org\nBcc:bad"}),null);
  assert.equal(formatContact({type:"EMAIL",value:"person@example.org"}),"person@example.org");
});

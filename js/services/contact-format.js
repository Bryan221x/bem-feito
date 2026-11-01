function phoneParts(value) {
  const raw=String(value ?? "").trim();
  if (!/^\+?[\d\s().-]+$/.test(raw)) return null;
  const digits=raw.replace(/\D/g,"");
  const international=raw.startsWith("+");
  let national=digits;
  if ((international || digits.length>11) && digits.startsWith("55") && [12,13].includes(digits.length)) national=digits.slice(2);
  const brazilian=/^[1-9][0-9](?:9[0-9]{8}|[2-5][0-9]{7})$/.test(national) && (!international || digits.startsWith("55"));
  if (brazilian) return { normalized:`+55${national}`, national };
  if (digits.startsWith("55") && (international || digits.length > 11)) return null;
  if (international && /^[1-9][0-9]{7,14}$/.test(digits)) return { normalized:`+${digits}` };
  return null;
}
export function formatPhone(value) {
  const phone=phoneParts(value), raw=String(value ?? "").trim();
  if (!phone) return raw;
  if(phone.national) { const n=phone.national;return `${raw.startsWith("+") ? "+55 " : ""}(${n.slice(0,2)}) ${n.slice(2,-4)}-${n.slice(-4)}`; }
  const digits=phone.normalized.slice(1), tail=digits.slice(-4), lead=digits.slice(0,-4);
  return `+${lead.replace(/\B(?=(\d{3})+(?!\d))/g," ")} ${tail}`;
}
export function formatContact(contact) { return ["PHONE","WHATSAPP"].includes(contact.type) ? formatPhone(contact.value) : String(contact.value ?? "").trim(); }
export function contactHref(contact) {
  const value=String(contact.value ?? "").trim();
  if(contact.type === "EMAIL") return /^[^\s@<>]+@[^\s@<>]+\.[^\s@<>]+$/.test(value) ? `mailto:${encodeURIComponent(value)}` : null;
  if(!["PHONE","WHATSAPP"].includes(contact.type)) return null;
  const phone=phoneParts(value);if(!phone) return null;
  return contact.type === "WHATSAPP" ? `https://wa.me/${phone.normalized.slice(1)}` : `tel:${phone.normalized}`;
}
export function contactIcon(type) {
  const paths={ EMAIL:'<path d="M3 5h18v14H3zM3 5l9 7 9-7"/>', PHONE:'<path d="M5 3h4l2 5-3 2c2 3 3 4 6 6l2-3 5 2v4c-9 4-22-9-16-16z"/>', WHATSAPP:'<path d="M21 11a9 9 0 0 1-13 8l-5 2 2-5a9 9 0 1 1 16-5zM8 8c1 4 3 6 7 7"/>' };
  return `<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.6" aria-hidden="true">${paths[type] || paths.PHONE}</svg>`;
}

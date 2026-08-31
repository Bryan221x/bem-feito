import { createUnitMap } from "../services/map-service.js";

/* Renderizador público: recebe somente dados já autorizados para publicação. */
export function renderInstitutionDetails(page, institution) {
  const displayName = String(institution?.displayName || institution?.name || "").trim();
  if (!page || !institution?.id || !displayName) throw new Error("A instituição precisa de identificador e nome de exibição.");
  const details = page.querySelector("[data-institution-details]");
  if (!details) return;

  setText(details, "[data-institution-name]", displayName);
  setOptionalText(details.querySelector("[data-institution-category]"), institution.category);
  setOptionalText(details.querySelector("[data-institution-objective]"), institution.objective);
  setOptionalText(details.querySelector("[data-institution-description]"), institution.description);
  renderWebsite(details, institution.website);
  renderGeneralContacts(details, institution.contacts);
  const markers = renderUnits(details, institution.units);
  renderMapArchitecture(details, markers);

  const pageTitle = page.querySelector("#institution-page-title");
  if (pageTitle) pageTitle.textContent = displayName;
  document.title = `${displayName} | Bem-Feito`;
  details.hidden = false;
}

function renderWebsite(details, website) {
  const link = details.querySelector("[data-institution-website]");
  if (!link || !website) return;
  link.href = String(website);
  link.hidden = false;
}

function renderGeneralContacts(details, contacts) {
  const section = details.querySelector("[data-institution-contact-section]");
  const container = details.querySelector("[data-institution-general-contacts]");
  const publicContacts = getPublicContacts(contacts);
  if (!section || !container) return;
  container.replaceChildren(...publicContacts.map(createContact));
  section.hidden = publicContacts.length === 0;
}

function renderUnits(details, units) {
  const container = details.querySelector("[data-institution-units]");
  const empty = details.querySelector("[data-institution-units-empty]");
  const validUnits = Array.isArray(units) ? units.filter((unit) => unit?.id && unit?.name) : [];
  if (!container || !empty) return [];
  const markers = [];
  container.replaceChildren(...validUnits.map((unit) => {
    const coordinates = getCoordinates(unit);
    if (coordinates) markers.push({ unitId: String(unit.id), name: String(unit.name), ...coordinates });
    return createUnit(unit);
  }));
  empty.hidden = validUnits.length > 0;
  return markers;
}

function createUnit(unit) {
  const card = document.createElement("article");
  card.className = "institution-unit-card";
  card.dataset.unitId = String(unit.id);
  const header = document.createElement("header");
  const title = document.createElement("h3"); title.textContent = unit.name;
  const address = document.createElement("p"); address.className = "institution-unit-card__address"; address.textContent = formatAddress(unit.address);
  header.append(title, address); card.append(header);

  const contacts = getPublicContacts(unit.contacts);
  if (contacts.length) card.append(createSubsection("Contatos", contacts.map(createContact), "institution-contact"));
  const areas = Array.isArray(unit.serviceAreas) ? unit.serviceAreas.filter(Boolean) : [];
  if (areas.length) card.append(createSubsection("Áreas de atuação", areas.map((area) => createTag(getServiceAreaLabel(area))), "institution-tags"));
  const needs = Array.isArray(unit.needs) ? unit.needs.filter((need) => need?.name && String(need.status || "ACTIVE").toUpperCase() === "ACTIVE") : [];
  if (needs.length) card.append(createSubsection("Necessidades atuais", needs.map(createNeed), "institution-unit-needs"));
  return card;
}

function createSubsection(titleText, children, className) {
  const section = document.createElement("section");
  section.className = "institution-unit-card__section";
  const title = document.createElement("h4"); title.textContent = titleText;
  const content = document.createElement("div"); content.className = className; content.append(...children);
  section.append(title, content); return section;
}

function createContact(contact) {
  const item = document.createElement("div"); item.className = "institution-contact__item";
  const label = document.createElement("span"); label.className = "institution-contact__label"; label.textContent = contact.label || getContactTypeLabel(contact.type);
  const value = document.createElement("span"); value.className = "institution-contact__value"; value.textContent = contact.value;
  item.append(label, value); return item;
}

function createTag(text) { const tag = document.createElement("span"); tag.className = "institution-needs__item"; tag.textContent = text; return tag; }
function createNeed(need) { const item = document.createElement("article"); item.className = "institution-unit-need"; const name = document.createElement("strong"); name.textContent = need.name; const priority = document.createElement("span"); priority.textContent = `Prioridade ${getPriorityLabel(need.priority)}`; item.append(name, priority); if (need.description) { const description = document.createElement("p"); description.textContent = need.description; item.append(description); } return item; }

function renderMapArchitecture(details, markers) {
  const section = details.querySelector("[data-institution-map-section]");
  const map = details.querySelector("[data-institution-map]");
  if (!section || !map) return;
  map.replaceChildren(...markers.map((marker) => {
    const point = document.createElement("span"); point.hidden = true; point.dataset.unitMapMarker = marker.unitId; point.dataset.latitude = String(marker.latitude); point.dataset.longitude = String(marker.longitude); return point;
  }));
  section.hidden = markers.length === 0;
  if (markers.length) details.dispatchEvent(new CustomEvent("bemfeito:institution-map-markers-ready", { bubbles: true, detail: { markers } }));
  if (markers.length) createUnitMap(map, markers);
}

function getPublicContacts(contacts) { return Array.isArray(contacts) ? contacts.filter((contact) => contact?.value && contact.isPublic !== false) : []; }
function getCoordinates(unit) { const latitude = Number(unit.coordinates?.latitude ?? unit.latitude); const longitude = Number(unit.coordinates?.longitude ?? unit.longitude); return Number.isFinite(latitude) && Number.isFinite(longitude) ? { latitude, longitude } : null; }
function formatAddress(address) { if (!address) return "Endereço não informado"; if (typeof address === "string") return address; const line = [address.street, address.number].filter(Boolean).join(", "); return [line, address.complement, address.neighborhood, [address.city, address.state].filter(Boolean).join(" - "), address.postalCode].filter(Boolean).join(" • ") || "Endereço não informado"; }
function getContactTypeLabel(type) { return { EMAIL: "E-mail", PHONE: "Telefone", WHATSAPP: "WhatsApp" }[String(type || "").toUpperCase()] || "Contato"; }
function getServiceAreaLabel(area) { return { FOOD: "Alimentação", CLOTHING: "Roupas", HYGIENE: "Higiene", CHILDREN: "Crianças", ELDERLY: "Idosos", ANIMALS: "Animais", OTHER: "Outros" }[String(area).toUpperCase()] || String(area); }
function getPriorityLabel(priority) { return { LOW: "baixa", MEDIUM: "média", HIGH: "alta" }[String(priority || "MEDIUM").toUpperCase()] || "não informada"; }
function setOptionalText(element, value) { if (!element || !value) return; element.textContent = String(value); element.hidden = false; }
function setText(root, selector, value) { const element = root.querySelector(selector); if (element) element.textContent = value; }

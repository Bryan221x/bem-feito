import { createUnitMap } from "../services/map-service.js";

const historyDialogStates = new WeakMap();

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
  renderHistory(details, institution);
  renderWebsite(details, institution.website);
  renderGeneralContacts(details, institution.contacts);
  const markers = renderUnits(details, institution.units);
  renderMapArchitecture(details, markers);

  const pageTitle = page.querySelector("#institution-page-title");
  if (pageTitle) pageTitle.textContent = displayName;
  document.title = `${displayName} | Bem-Feito`;
  details.hidden = false;
}

function renderHistory(details, institution) {
  const openButton = details.querySelector("[data-institution-history-open]");
  const dialog = details.querySelector("[data-institution-history-dialog]");
  const textContainer = details.querySelector("[data-institution-history-text]");
  const name = details.querySelector("[data-institution-history-name]");
  if (!openButton || !dialog || !textContainer || !name) return;

  const paragraphs = createHistoryParagraphs(institution.history);
  const hasHistory = paragraphs.length > 0;
  const images = hasHistory ? normalizePublicHistoryImages(institution.historyImages) : [];
  openButton.hidden = !hasHistory;
  textContainer.replaceChildren(...paragraphs);
  name.textContent = hasHistory ? String(institution.displayName || institution.name || "").trim() : "";

  const state = getHistoryDialogState(details, dialog, openButton);
  state.images = images;
  state.index = 0;
  renderHistoryGallery(details, state);

  if (!paragraphs.length && dialog.open) closeHistoryDialog(state);
}

function createHistoryParagraphs(history) {
  return String(history || "")
    .split(/\r?\n\s*\r?\n/)
    .map((text) => text.trim())
    .filter(Boolean)
    .map((text) => {
      const paragraph = document.createElement("p");
      paragraph.textContent = text;
      return paragraph;
    });
}

function normalizePublicHistoryImages(images) {
  if (!Array.isArray(images)) return [];
  return images
    .filter((image) => image?.url && isSafeImageUrl(image.url))
    .map((image, index) => ({
      url: String(image.url),
      alt: String(image.alt || ""),
      caption: String(image.caption || "").trim(),
      order: Number.isFinite(Number(image.order)) ? Number(image.order) : index,
    }))
    .sort((first, second) => first.order - second.order);
}

function isSafeImageUrl(value) {
  try {
    const url = new URL(String(value), window.location.href);
    return ["http:", "https:"].includes(url.protocol);
  } catch {
    return false;
  }
}

function getHistoryDialogState(details, dialog, openButton) {
  let state = historyDialogStates.get(dialog);
  if (state) return state;
  state = { details, dialog, openButton, images: [], index: 0, previousFocus: null };
  historyDialogStates.set(dialog, state);
  openButton.addEventListener("click", () => openHistoryDialog(state));
  details.querySelector("[data-institution-history-close]")?.addEventListener("click", () => closeHistoryDialog(state));
  details.querySelector("[data-history-previous]")?.addEventListener("click", () => moveHistoryImage(state, -1));
  details.querySelector("[data-history-next]")?.addEventListener("click", () => moveHistoryImage(state, 1));
  details.querySelector("[data-history-thumbnails]")?.addEventListener("click", (event) => {
    const button = event.target.closest("[data-history-image-index]");
    if (!button) return;
    state.index = Number(button.dataset.historyImageIndex);
    renderActiveHistoryImage(state);
  });
  dialog.addEventListener("cancel", (event) => {
    event.preventDefault();
    closeHistoryDialog(state);
  });
  dialog.addEventListener("click", (event) => {
    if (event.target === dialog) closeHistoryDialog(state);
  });
  dialog.addEventListener("keydown", (event) => handleHistoryDialogKeydown(event, state));
  dialog.addEventListener("close", () => restoreAfterHistoryDialog(state));
  return state;
}

function openHistoryDialog(state) {
  if (!state.openButton || state.openButton.hidden || state.dialog.open) return;
  state.previousFocus = document.activeElement;
  state.dialog.showModal();
  document.body.classList.add("institution-history-open");
  state.details.querySelector("[data-institution-history-close]")?.focus();
}

function closeHistoryDialog(state) {
  if (state.dialog.open) state.dialog.close();
}

function restoreAfterHistoryDialog(state) {
  document.body.classList.remove("institution-history-open");
  const target = state.previousFocus?.isConnected ? state.previousFocus : state.openButton;
  target?.focus();
  state.previousFocus = null;
}

function handleHistoryDialogKeydown(event, state) {
  if (event.key === "Escape") {
    event.preventDefault();
    closeHistoryDialog(state);
    return;
  }
  if (state.images.length > 1 && event.key === "ArrowLeft") {
    event.preventDefault();
    moveHistoryImage(state, -1);
  } else if (state.images.length > 1 && event.key === "ArrowRight") {
    event.preventDefault();
    moveHistoryImage(state, 1);
  } else if (event.key === "Tab") {
    trapDialogFocus(event, state.dialog);
  }
}

function trapDialogFocus(event, dialog) {
  const focusable = [...dialog.querySelectorAll('button:not([disabled]), a[href], [tabindex]:not([tabindex="-1"])')].filter((element) => element.getClientRects().length > 0);
  if (!focusable.length) return;
  const first = focusable[0];
  const last = focusable[focusable.length - 1];
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first.focus();
  }
}

function renderHistoryGallery(details, state) {
  const gallery = details.querySelector("[data-institution-history-gallery]");
  const layout = details.querySelector("[data-institution-history-layout]");
  const navigation = details.querySelector("[data-institution-history-navigation]");
  const thumbnails = details.querySelector("[data-history-thumbnails]");
  if (!gallery || !layout || !navigation || !thumbnails) return;
  const hasImages = state.images.length > 0;
  const hasMultiple = state.images.length > 1;
  gallery.hidden = !hasImages;
  layout.classList.toggle("institution-history-dialog__body--text-only", !hasImages);
  navigation.hidden = !hasMultiple;
  thumbnails.hidden = !hasMultiple;
  thumbnails.replaceChildren(...state.images.map((image, index) => createHistoryThumbnail(image, index)));
  const dots = details.querySelector("[data-history-dots]");
  dots?.replaceChildren(...state.images.map(() => document.createElement("span")));
  if (hasImages) renderActiveHistoryImage(state);
}

function createHistoryThumbnail(image, index) {
  const button = document.createElement("button");
  button.type = "button";
  button.dataset.historyImageIndex = String(index);
  button.setAttribute("aria-label", `Mostrar foto ${index + 1}`);
  const thumbnail = document.createElement("img");
  thumbnail.src = image.url;
  thumbnail.alt = "";
  button.append(thumbnail);
  return button;
}

function moveHistoryImage(state, amount) {
  if (state.images.length < 2) return;
  state.index = (state.index + amount + state.images.length) % state.images.length;
  renderActiveHistoryImage(state);
}

function renderActiveHistoryImage(state) {
  const image = state.images[state.index];
  if (!image) return;
  const mainImage = state.details.querySelector("[data-institution-history-image]");
  const caption = state.details.querySelector("[data-institution-history-caption]");
  const counter = state.details.querySelector("[data-history-counter]");
  if (mainImage) {
    mainImage.src = image.url;
    mainImage.alt = image.alt;
  }
  if (caption) {
    caption.textContent = image.caption;
    caption.hidden = !image.caption;
  }
  if (counter) counter.textContent = `${state.index + 1} / ${state.images.length}`;
  state.details.querySelectorAll("[data-history-image-index]").forEach((button, index) => {
    button.setAttribute("aria-current", index === state.index ? "true" : "false");
  });
  state.details.querySelectorAll("[data-history-dots] span").forEach((dot, index) => {
    dot.classList.toggle("is-active", index === state.index);
  });
}

function renderWebsite(details, website) {
  const link = details.querySelector("[data-institution-website]");
  if (!link) return;
  link.hidden = !website;
  if (!website) {
    link.removeAttribute("href");
    return;
  }
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

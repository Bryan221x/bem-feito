import { contactHref, formatContact, contactIcon } from "../services/contact-format.js";
import { sortNeeds } from "../services/need-order.js";
export { contactHref } from "../services/contact-format.js";
import { apiAssetUrl } from "../services/api-client.js";
import { createUnitMap, focusUnitMap, hasCoordinates, removeMap } from "../services/map-service.js";

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
  const summary = details.querySelector("[data-institution-summary]");
  const description = String(institution.description || "");
  setOptionalText(summary, description.length > 260 ? `${description.slice(0, 260)}…` : description);
  const objectiveSection = details.querySelector("[data-objective-section]"), descriptionSection = details.querySelector("[data-description-section]");
  if (objectiveSection) objectiveSection.hidden = !institution.objective;
  if (descriptionSection) descriptionSection.hidden = description.length <= 260;
  const cover = details.querySelector("[data-institution-cover]"), firstImage = normalizePublicHistoryImages(institution.historyImages)[0];
  if (cover) { cover.hidden = !firstImage; if (firstImage) { cover.src = firstImage.url; cover.alt = firstImage.alt; cover.onerror = () => { cover.hidden = true; }; } }
  renderHistory(details, institution);
  renderWebsite(details, institution.website);
  renderGeneralContacts(details, institution.contacts);
  details.hidden = false;
  renderDonationNeeds(details, institution);
  const markers = renderUnits(details, institution.units);
  renderMapArchitecture(details, markers);
  renderContactCta(details, institution);

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
  const images = normalizePublicHistoryImages(institution.historyImages);
  openButton.hidden = !hasHistory && !images.length;
  openButton.textContent = images.length ? "Ver fotos e história" : "Conheça nossa história";
  textContainer.replaceChildren(...paragraphs);
  name.textContent = String(institution.displayName || institution.name || "").trim();
  const story = dialog.querySelector(".institution-history-dialog__story"); if (story) story.hidden = !hasHistory;

  const state = getHistoryDialogState(details, dialog, openButton);
  state.images = images;
  state.index = 0;
  renderHistoryGallery(details, state);

  if (!paragraphs.length && !images.length && dialog.open) closeHistoryDialog(state);
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
    .filter((image) => image?.url && apiAssetUrl(image.url))
    .map((image, index) => ({
      url: apiAssetUrl(image.url),
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
  website = apiAssetUrl(website);
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

function renderContactCta(details, institution) {
  const cta = details.querySelector("[data-institution-contact-cta]");
  if (!cta) return;
  const general = getPublicContacts(institution.contacts).length > 0;
  const firstUnitWithContacts = (Array.isArray(institution.units) ? institution.units : []).find((unit) =>
    (!unit.status || unit.status === "ACTIVE") && unit.id != null && getPublicContacts(unit.contacts).length > 0,
  );
  cta.hidden = !general && !firstUnitWithContacts;
  if (cta.hidden) { cta.removeAttribute("href"); return; }
  cta.href = general ? "#institution-contacts" : `#unit-contacts-${encodeURIComponent(firstUnitWithContacts.id)}`;
}

function renderUnits(details, units) {
  const container = details.querySelector("[data-institution-units]");
  const empty = details.querySelector("[data-institution-units-empty]");
  const validUnits = Array.isArray(units) ? units.filter((unit) => unit?.id && unit?.name) : [];
  if (!container || !empty) return [];
  const markers = [];
  container.replaceChildren(...validUnits.map((unit) => {
    const coordinates = getCoordinates(unit);
    if (coordinates) markers.push({ unitId: String(unit.id), name: String(unit.name), addressLabel: formatAddress(unit.address), ...coordinates });
    return createUnit(unit, details);
  }));
  empty.hidden = validUnits.length > 0;
  return markers;
}

function createUnit(unit, details) {
  const card = document.createElement("article");
  card.className = "institution-unit-card";
  card.dataset.unitId = String(unit.id);
  const header = document.createElement("header");
  const title = document.createElement("h3"); title.textContent = unit.name;
  const address = document.createElement("p"); address.className = "institution-unit-card__address"; address.textContent = formatAddress(unit.address);
  header.append(title, address); card.append(header);
  if (hasCoordinates(unit)) {
    const actions = document.createElement("div"); actions.className = "institution-unit-card__actions";
    const showMap = document.createElement("button"); showMap.type = "button"; showMap.className = "button button--outline"; showMap.dataset.unitMapFocus = String(unit.id); showMap.textContent = "Ver no mapa";
    showMap.addEventListener("click", () => {
      const section = details.querySelector("[data-institution-map-section]");
      const map = details.querySelector("[data-institution-map]");
      const reduceMotion = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
      section?.scrollIntoView?.({ behavior: reduceMotion ? "auto" : "smooth", block: "start" });
      focusUnitMap(map, unit.id);
    });
    const directions = document.createElement("a"); directions.className = "button button--outline"; directions.textContent = "Como chegar";
    const destination = `${Number(unit.latitude)},${Number(unit.longitude)}`;
    directions.href = `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(destination)}`;
    directions.target = "_blank"; directions.rel = "noopener noreferrer";
    actions.append(showMap, directions); card.append(actions);
  } else { const note=document.createElement("p"); note.textContent="Localização no mapa ainda não confirmada."; card.append(note); }

  const contacts = getPublicContacts(unit.contacts);
  if (contacts.length) {
    const section = createSubsection("Contatos da unidade", contacts.map(createContact), "institution-contact");
    section.id = `unit-contacts-${String(unit.id)}`;
    card.append(section);
  }
  const areas = Array.isArray(unit.serviceAreas) ? unit.serviceAreas.filter(Boolean) : [];
  if (areas.length) card.append(createSubsection("Áreas de atuação", areas.map((area) => createTag(getServiceAreaLabel(area))), "institution-tags"));

  card.append(createSubsection("Horários", renderOpeningHours(unit.openingHours), "institution-hours"));
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
  const typeLabel = getContactTypeLabel(contact.type);
  const customLabel = String(contact.label || "").trim();
  if (customLabel && normalizeContactLabel(customLabel) !== normalizeContactLabel(typeLabel)) {
    const label = document.createElement("span"); label.className = "institution-contact__label"; label.textContent = customLabel; item.append(label);
  }
  const href = contactHref(contact);
  const value = document.createElement(href ? "a" : "span"); if (href) value.href = href; if (href?.startsWith("https:")) { value.target = "_blank"; value.rel = "noopener noreferrer"; } value.className = "institution-contact__value"; value.textContent = formatContact(contact);
  const icon=document.createElement("span"); icon.innerHTML=contactIcon(contact.type);
  const type=document.createElement("small"); type.textContent=typeLabel;
  item.append(icon,type,value); return item;
}

function normalizeContactLabel(value) { return String(value || "").normalize("NFD").replace(/\p{M}/gu, "").replace(/[^\p{L}\p{N}]/gu, "").toLocaleLowerCase("pt-BR"); }

function createTag(text) { const tag = document.createElement("span"); tag.className = "institution-needs__item"; tag.textContent = text; return tag; }
function createNeed(need) {
  const item = document.createElement("article"); item.className = "institution-unit-need";
  const header = document.createElement("header"); header.className = "institution-unit-need__header";
  const name = document.createElement("strong"); name.textContent = need.name;
  const priority = document.createElement("span"); priority.className = "need-priority";
  priority.textContent = `Prioridade ${getPriorityLabel(need.priority)}`;
  priority.dataset.priority = String(need.priority || "MEDIUM").toLowerCase();
  header.append(name, priority); item.append(header);
  const meta = [need.category, need.quantity != null ? `${need.quantity} ${need.unitLabel || ""}`.trim() : need.unitLabel].filter(Boolean);
  if (meta.length) { const metadata = document.createElement("p"); metadata.className = "institution-unit-need__meta"; metadata.textContent = meta.join(" • "); item.append(metadata); }
  if (need.description) { const description = document.createElement("p"); description.className = "institution-unit-need__description"; description.textContent = need.description; item.append(description); }
  return item;
}

function renderMapArchitecture(details, markers) {
  const section = details.querySelector("[data-institution-map-section]");
  const map = details.querySelector("[data-institution-map]");
  if (!section || !map) return;
  removeMap(map);
  map.replaceChildren();
  section.hidden = markers.length === 0;
  const feedback = details.querySelector("[data-institution-map-feedback]");
  if (feedback) { feedback.hidden = true; feedback.textContent = ""; }
  if (!markers.length) return;
  const unavailable = () => {
    if (!feedback) return;
    feedback.textContent = "O mapa está indisponível no momento. Os endereços e as opções Como chegar continuam disponíveis.";
    feedback.hidden = false;
  };
  const created = createUnitMap(map, markers, { onUnavailable: unavailable, popupContent: createUnitMapPopup });
  if (!created) unavailable();
}

function createUnitMapPopup(unit) {
  const popup = document.createElement("div");
  const name = document.createElement("strong"); name.textContent = String(unit.name || "Unidade");
  const address = document.createElement("p"); address.textContent = String(unit.addressLabel || "Endereço não informado");
  popup.append(name, address);
  return popup;
}

function getPublicContacts(contacts) { return Array.isArray(contacts) ? contacts.filter((contact) => contact?.value && contact.isPublic === true) : []; }
function getCoordinates(unit) { return hasCoordinates(unit) ? { latitude: Number(unit.latitude), longitude: Number(unit.longitude) } : null; }
function formatAddress(address) { if (!address) return "Endereço não informado"; if (typeof address === "string") return address; const line = [address.street, address.number].filter(Boolean).join(", "); return [line, address.complement, address.neighborhood, [address.city, address.state].filter(Boolean).join(" - "), address.postalCode].filter(Boolean).join(" • ") || "Endereço não informado"; }
function getContactTypeLabel(type) { return { EMAIL: "E-mail", PHONE: "Telefone", WHATSAPP: "WhatsApp" }[String(type || "").toUpperCase()] || "Contato"; }
function getServiceAreaLabel(area) { return area.name || ""; }
function getPriorityLabel(priority) { return { LOW: "baixa", MEDIUM: "média", HIGH: "alta" }[String(priority || "MEDIUM").toUpperCase()] || "não informada"; }
function setOptionalText(element, value) { if (!element) return; element.textContent = value ? String(value) : ""; element.hidden = !value; }
function setText(root, selector, value) { const element = root.querySelector(selector); if (element) element.textContent = value; }

export function renderOpeningHours(hours) {
  const days = ["Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira", "Sexta-feira", "Sábado", "Domingo"];
  const entries = Array.isArray(hours) ? hours : [];
  const hasPublishedHours = entries.some((item) => item && (item.closed === true || (item.opensAt && item.closesAt) || item.note));
  if (!hasPublishedHours) {
    const empty = document.createElement("p"); empty.textContent = "Horários não informados"; return [empty];
  }
  return days.map((day, index) => {
    const h = entries.find((item) => Number(item?.dayOfWeek) === index + 1);
    const times = h?.opensAt && h?.closesAt ? `${String(h.opensAt).slice(0,5)} às ${String(h.closesAt).slice(0,5)}` : null;
    const value = h?.closed === true ? "Fechado" : times || "Não informado";
    const p = document.createElement("p"); p.textContent = `${day}: ${value}${h?.note ? ` — ${h.note}` : ""}`; return p;
  });
}

export function renderDonationNeeds(details, institution) {
  const host=details.querySelector("[data-donation-needs]"); if(!host) return;
  const groups=[];
  for(const unit of institution.units || []) {
    if(unit.status && unit.status !== "ACTIVE") continue;
    const needs=sortNeeds((unit.needs || []).filter(need=>need.status === "ACTIVE" && (need.name || need.title)));
    if(!needs.length) continue;
    const section=document.createElement("section"), title=document.createElement("h3"), list=document.createElement("div");
    title.textContent=unit.name;list.className="institution-unit-needs";list.append(...needs.map(need=>createNeed({ ...need,name:need.name || need.title })));
    section.append(title,list);groups.push(section);
  }
  if(!groups.length) { const empty=document.createElement("p");empty.textContent="Esta instituição ainda não informou necessidades atuais.";groups.push(empty); }
  host.replaceChildren(...groups);
}

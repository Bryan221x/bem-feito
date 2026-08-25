/*
 * Contrato esperado para os detalhes recebidos futuramente pela API.
 *
 * Campos opcionais simplesmente não serão exibidos quando não
 * estiverem disponíveis no cadastro da instituição.
 */

/**
 * Preenche a página com dados de uma instituição validada.
 *
 * @param {HTMLElement} page
 * @param {Object} institution
 */
export function renderInstitutionDetails(page, institution) {
  if (!page || !institution?.id || !institution?.name) {
    throw new Error(
      "Não foi possível renderizar a instituição sem identificador e nome.",
    );
  }

  const details = page.querySelector("[data-institution-details]");
  const pageTitle = page.querySelector("#institution-page-title");

  if (!details) return;

  fillBasicInformation(details, institution);
  fillNeeds(details, institution.needs);
  fillContact(details, institution.contact);
  fillLocation(details, institution.address);

  /*
   * Quando houver uma instituição válida, seu nome se torna
   * a principal identificação textual da página.
   */
  if (pageTitle) {
    pageTitle.textContent = institution.name;
  }

  details.hidden = false;
}

function fillBasicInformation(details, institution) {
  const name = details.querySelector("[data-institution-name]");
  const category = details.querySelector("[data-institution-category]");
  const description = details.querySelector("[data-institution-description]");

  if (name) {
    name.textContent = institution.name;
  }

  setOptionalText(category, institution.category);
  setOptionalText(description, institution.description);
}

function fillNeeds(details, needs) {
  const section = details.querySelector("[data-institution-needs-section]");
  const list = details.querySelector("[data-institution-needs]");

  if (!section || !list || !Array.isArray(needs) || needs.length === 0) {
    return;
  }

  list.replaceChildren();

  const fragment = document.createDocumentFragment();

  needs.forEach((need) => {
    if (!need) return;

    const item = document.createElement("li");

    item.className = "institution-needs__item";
    item.textContent = String(need);

    fragment.appendChild(item);
  });

  if (!fragment.childNodes.length) return;

  list.appendChild(fragment);
  section.hidden = false;
}

function fillContact(details, contact) {
  const section = details.querySelector("[data-institution-contact-section]");
  const container = details.querySelector("[data-institution-contact]");

  if (!section || !container || !contact) return;

  container.replaceChildren();

  const entries = [
    ["Telefone", contact.phone],
    ["WhatsApp", contact.whatsapp],
    ["E-mail", contact.email],
  ];

  entries.forEach(([label, value]) => {
    if (!value) return;

    const item = document.createElement("div");
    item.className = "institution-contact__item";

    const itemLabel = document.createElement("span");
    itemLabel.className = "institution-contact__label";
    itemLabel.textContent = label;

    const itemValue = document.createElement("span");
    itemValue.className = "institution-contact__value";
    itemValue.textContent = String(value);

    item.append(itemLabel, itemValue);
    container.appendChild(item);
  });

  if (!container.children.length) return;

  section.hidden = false;
}

function fillLocation(details, address) {
  const section = details.querySelector("[data-institution-location-section]");
  const addressElement = details.querySelector("[data-institution-address]");

  if (!section || !addressElement || !address) return;

  addressElement.textContent = String(address);
  addressElement.hidden = false;

  section.hidden = false;
}

function setOptionalText(element, value) {
  if (!element || !value) return;

  element.textContent = String(value);
  element.hidden = false;
}

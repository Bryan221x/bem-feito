const selectionStates = new WeakMap();

export function initInstitutionSelectionPage() {
  const page = document.querySelector("[data-institution-selection-page]");

  if (!page) return;

  const state = {
    institutions: [],
    selectedInstitutionId: null,
  };

  selectionStates.set(page, state);
  initSelection(page, state);
  renderInstitutionSelection(page, []);
}

/*
 * Ponto de entrada da futura API. A página recebe somente instituições às
 * quais a conta autenticada possui acesso; nenhum registro é criado aqui.
 */
export function renderInstitutionSelection(
  page,
  institutions,
  selectedInstitutionId = null,
) {
  if (!page || !Array.isArray(institutions)) return;

  const state = selectionStates.get(page);

  if (!state) return;

  state.institutions = getValidInstitutions(institutions);
  state.selectedInstitutionId = getAvailableSelectedId(
    state.institutions,
    selectedInstitutionId,
  );

  if (state.institutions.length === 0) {
    renderEmptyState(page);
    return;
  }

  const cardList = page.querySelector("[data-institution-card-list]");

  if (!cardList) return;

  cardList.replaceChildren(
    ...state.institutions.map((institution, index) =>
      createInstitutionCard(
        institution,
        index,
        state.selectedInstitutionId,
      ),
    ),
  );

  showOnlyState(page, "options");
  updateSelectionNavigation(page, state.selectedInstitutionId);
  updateFeedback(page, "");
}

export function showInstitutionSelectionLoading(page) {
  if (!page) return;

  showOnlyState(page, "loading");
  updateFeedback(page, "");
}

export function showInstitutionSelectionError(page) {
  if (!page) return;

  showOnlyState(page, "error");
  updateFeedback(page, "");
}

function initSelection(page, state) {
  const options = page.querySelector("[data-institution-selection-options]");

  if (!options) return;

  options.addEventListener("change", (event) => {
    const input = event.target.closest("[data-institution-option]");

    if (!input) return;

    const institution = state.institutions.find(
      (item) => String(item.id) === input.value,
    );

    if (!institution) return;

    state.selectedInstitutionId = String(institution.id);
    updateSelectedCards(options, state.selectedInstitutionId);
    updateSelectionNavigation(page, state.selectedInstitutionId);
    updateFeedback(
      page,
      `Instituição selecionada: ${getInstitutionName(institution)}.`,
    );

    /*
     * Ponto de integração do contexto global. A futura camada de sessão deve
     * receber o ID, confirmar a permissão e então atualizar as demais páginas.
     */
    page.dispatchEvent(
      new CustomEvent("bemfeito:institution-context-selected", {
        bubbles: true,
        detail: { institutionId: institution.id },
      }),
    );
  });
}

function getValidInstitutions(institutions) {
  const uniqueInstitutions = new Map();

  institutions.forEach((institution) => {
    const hasIdentifier =
      institution?.id !== null &&
      institution?.id !== undefined &&
      institution?.id !== "";
    const hasName = getInstitutionName(institution).length > 0;

    if (!hasIdentifier || !hasName) return;

    uniqueInstitutions.set(String(institution.id), institution);
  });

  return [...uniqueInstitutions.values()];
}

function getAvailableSelectedId(institutions, selectedInstitutionId) {
  if (selectedInstitutionId === null || selectedInstitutionId === undefined) {
    return null;
  }

  const selectedInstitution = institutions.find(
    (institution) =>
      String(institution.id) === String(selectedInstitutionId),
  );

  return selectedInstitution ? String(selectedInstitution.id) : null;
}

function createInstitutionCard(institution, index, selectedInstitutionId) {
  const card = document.createElement("label");
  const input = document.createElement("input");
  const content = document.createElement("span");
  const indicator = createSelectionIndicator();
  const mark = createInstitutionMark(institution);
  const name = document.createElement("strong");
  const metadata = document.createElement("span");
  const documentText = document.createElement("span");
  const locationText = document.createElement("span");
  const status = document.createElement("span");
  const selectionText = document.createElement("span");
  const selectedLabel = document.createElement("span");
  const unselectedLabel = document.createElement("span");
  const institutionId = String(institution.id);
  const isSelected = institutionId === selectedInstitutionId;
  const statusData = getStatusData(institution);

  card.className = "institution-selection-card";
  card.classList.toggle("is-selected", isSelected);
  card.dataset.institutionId = institutionId;

  input.className = "sr-only";
  input.id = `managed-institution-${index + 1}`;
  input.type = "radio";
  input.name = "managedInstitution";
  input.value = institutionId;
  input.checked = isSelected;
  input.dataset.institutionOption = "";

  content.className = "institution-selection-card__content";
  name.className = "institution-selection-card__name";
  name.textContent = getInstitutionName(institution);

  metadata.className = "institution-selection-card__metadata";
  documentText.textContent = institution.document
    ? `CNPJ ${institution.document}`
    : "CNPJ não informado";
  locationText.textContent = getInstitutionLocation(institution);
  metadata.append(documentText, locationText);

  status.className = "institution-selection-card__status";
  status.dataset.status = statusData.code;
  status.textContent = statusData.label;

  selectionText.className = "institution-selection-card__selection-text";
  selectedLabel.className = "institution-selection-card__selected-label";
  selectedLabel.textContent = "Selecionada";
  unselectedLabel.className =
    "institution-selection-card__unselected-label";
  unselectedLabel.textContent = "Selecionar instituição";
  selectionText.append(selectedLabel, unselectedLabel);

  content.append(
    indicator,
    mark,
    name,
    metadata,
    status,
    selectionText,
  );
  card.append(input, content);

  return card;
}

function createSelectionIndicator() {
  const indicator = document.createElement("span");

  indicator.className = "institution-selection-card__indicator";
  indicator.setAttribute("aria-hidden", "true");
  indicator.innerHTML = `
    <svg viewBox="0 0 24 24">
      <path d="m6 12 4 4 8-8"></path>
    </svg>
  `;

  return indicator;
}

function createInstitutionMark(institution) {
  const mark = document.createElement("span");

  mark.className = "institution-selection-card__mark";
  mark.setAttribute("aria-hidden", "true");

  if (institution.logoUrl) {
    const image = document.createElement("img");
    image.src = String(institution.logoUrl);
    image.alt = "";
    mark.appendChild(image);
    return mark;
  }

  mark.innerHTML = `
    <svg viewBox="0 0 24 24">
      <path d="M4 21V8l8-5 8 5v13"></path>
      <path d="M9 21v-6h6v6"></path>
      <path d="M8 10h.01M16 10h.01"></path>
    </svg>
  `;

  return mark;
}

function updateSelectedCards(options, selectedInstitutionId) {
  options.querySelectorAll(".institution-selection-card").forEach((card) => {
    const isSelected = card.dataset.institutionId === selectedInstitutionId;
    const input = card.querySelector("[data-institution-option]");

    card.classList.toggle("is-selected", isSelected);

    if (input) {
      input.checked = isSelected;
    }
  });
}

function getInstitutionLocation(institution) {
  const location = [institution.city, institution.state]
    .filter(Boolean)
    .join(" - ");

  return location || "Localização não informada";
}

function getStatusData(institution) {
  const status = String(institution.status || "").toLowerCase();
  const statusLabels = {
    active: "Ativa",
    inactive: "Inativa",
    pending: "Em análise",
  };
  const hasKnownStatus = Object.hasOwn(statusLabels, status);

  return {
    code: hasKnownStatus ? status : "unknown",
    label:
      institution.statusLabel ||
      (hasKnownStatus ? statusLabels[status] : null) ||
      "Status não informado",
  };
}

function renderEmptyState(page) {
  const cardList = page.querySelector("[data-institution-card-list]");

  cardList?.replaceChildren();
  showOnlyState(page, "empty");
  updateFeedback(page, "");
  updateSelectionNavigation(page, null);
}

function updateSelectionNavigation(page, institutionId) {
  const container = page.querySelector(
    "[data-institution-selection-continue]",
  );
  const link = page.querySelector("[data-institution-continue-link]");

  if (!container || !link) return;

  if (!institutionId) {
    container.hidden = true;
    link.removeAttribute("href");
    return;
  }

  const params = new URLSearchParams({ institutionId });
  link.href = `./my-institution.html?${params.toString()}`;
  container.hidden = false;
}

function getInstitutionName(institution) {
  return String(institution?.displayName || institution?.name || "").trim();
}

function showOnlyState(page, activeState) {
  const states = {
    options: page.querySelector("[data-institution-selection-options]"),
    empty: page.querySelector("[data-institution-selection-empty]"),
    loading: page.querySelector("[data-institution-selection-loading]"),
    error: page.querySelector("[data-institution-selection-error]"),
  };

  Object.entries(states).forEach(([stateName, element]) => {
    if (element) {
      element.hidden = stateName !== activeState;
    }
  });
}

function updateFeedback(page, message) {
  const feedback = page.querySelector("[data-institution-selection-feedback]");

  if (feedback) {
    feedback.textContent = message;
  }
}

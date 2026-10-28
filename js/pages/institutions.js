import { getPublicInstitutions } from "../services/public-catalog-service.js";
import { createPagination } from "../components/api-ui.js";
import { createInstitutionCard } from "../components/institution-card.js";

export function initInstitutionsPage() {
  const page = document.querySelector(".institutions-page");

  // Este módulo só executa dentro do catálogo de instituições.
  if (!page) return;

  const cityId = getSelectedCityId();

  if (cityId) prepareSelectedCityState(page, cityId);
  let current = 0;
  const pagination = createPagination(page.querySelector("[data-institution-list]"), (number) => load(number));
  async function load(number = 0) {
    current = number; showInstitutionsLoading(page);
    page.querySelector("[data-institution-list]").replaceChildren(); pagination.busy();
    try { const data = await getPublicInstitutions(cityId, current); renderInstitutions(page, data.content); pagination.update(data); }
    catch { showInstitutionsError(page); pagination.failed(() => load(current)); }
  }
  void load();
}


function getSelectedCityId() {
  const params = new URLSearchParams(window.location.search);
  const cityId = params.get("cityId");

  if (!cityId) return null;

  const normalizedId = cityId.trim();

  return normalizedId || null;
}

function showCityRequiredState(page) {
  hidePageStates(page);

  const cityRequiredState = page.querySelector("[data-city-required]");

  if (cityRequiredState) {
    cityRequiredState.hidden = false;
  }
}


function prepareSelectedCityState(page, cityId) {
  page.dataset.cityId = cityId;
}

/*
 * Centraliza o controle dos estados do catálogo para evitar
 * que mensagens incompatíveis apareçam simultaneamente.
 */
function hidePageStates(page) {
  const states = page.querySelectorAll(
    "[data-city-required], [data-institutions-empty], [data-institutions-loading], [data-institutions-error]",
  );

  states.forEach((state) => {
    state.hidden = true;
  });
}


export function showInstitutionsLoading(page) {
  if (!page) return;

  hidePageStates(page);

  const loadingState = page.querySelector("[data-institutions-loading]");

  if (loadingState) {
    loadingState.hidden = false;
  }
}


export function showInstitutionsError(page) {
  if (!page) return;

  hidePageStates(page);

  const errorState = page.querySelector("[data-institutions-error]");

  if (errorState) {
    errorState.hidden = false;
  }
}

/*
 * Renderiza exclusivamente instituições fornecidas pela camada de dados.
 * Nenhuma instituição é criada ou preenchida manualmente neste módulo.
 */
export function renderInstitutions(page, institutions) {
  if (!page || !Array.isArray(institutions)) return;

  const list = page.querySelector("[data-institution-list]");

  if (!list) return;

  hidePageStates(page);
  list.replaceChildren();

  const validInstitutions = institutions.filter(
    (institution) =>
      institution?.id &&
      String(institution?.displayName || institution?.name || "").trim(),
  );

  if (validInstitutions.length === 0) {
    const emptyState = page.querySelector("[data-institutions-empty]");

    if (emptyState) {
      emptyState.hidden = false;
    }

    return;
  }

  /*
   * Recupera a cidade já preservada pela página para que
   * o detalhe da instituição saiba de qual catálogo o usuário veio.
   */
  const cityId = page.dataset.cityId || null;

  const fragment = document.createDocumentFragment();

  validInstitutions.forEach((institution) => {
    const card = createInstitutionCard(institution, cityId);

    fragment.appendChild(card);
  });

  list.appendChild(fragment);
}

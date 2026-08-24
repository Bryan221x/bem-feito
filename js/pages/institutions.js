import { createInstitutionCard } from "../components/institution-card.js";

export function initInstitutionsPage() {
  const page = document.querySelector(".institutions-page");

  // Este módulo só executa dentro do catálogo de instituições.
  if (!page) return;

  const cityId = getSelectedCityId();

  if (!cityId) {
    showCityRequiredState(page);
    return;
  }

  prepareSelectedCityState(page, cityId);
}

/*
 * Recupera o identificador da cidade informado pela navegação.
 * O valor será validado futuramente pela API antes de qualquer
 * instituição ser exibida.
 */
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

/*
 * Por enquanto apenas preservamos o identificador recebido.
 * A integração futura com a API será responsável por validar
 * a cidade antes de alterar o estado visual da página.
 */
function prepareSelectedCityState(page, cityId) {
  page.dataset.cityId = cityId;
}

/*
 * Centraliza o controle dos estados do catálogo para evitar que
 * mensagens incompatíveis apareçam simultaneamente.
 */
function hidePageStates(page) {
  const states = page.querySelectorAll(
    "[data-city-required], [data-institutions-empty], [data-institutions-loading], [data-institutions-error]",
  );

  states.forEach((state) => {
    state.hidden = true;
  });
}

/*
 * Será utilizado quando a futura API iniciar uma consulta.
 */
export function showInstitutionsLoading(page) {
  if (!page) return;

  hidePageStates(page);

  const loadingState = page.querySelector("[data-institutions-loading]");

  if (loadingState) {
    loadingState.hidden = false;
  }
}

/*
 * Será utilizado quando a futura consulta à API falhar.
 */
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

  if (institutions.length === 0) {
    const emptyState = page.querySelector("[data-institutions-empty]");

    if (emptyState) {
      emptyState.hidden = false;
    }

    return;
  }

  const fragment = document.createDocumentFragment();

  institutions.forEach((institution) => {
    const card = createInstitutionCard(institution);

    fragment.appendChild(card);
  });

  list.appendChild(fragment);
}

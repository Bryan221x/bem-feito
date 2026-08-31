import { renderInstitutionDetails } from "../components/institution-details.js";

export function initInstitutionPage() {
  const page = document.querySelector(".institution-page");

  // Este módulo só executa dentro da página de detalhes.
  if (!page) return;

  const institutionId = getSelectedInstitutionId();
  const cityId = getSelectedCityId();

  /*
   * Preserva a cidade de origem para permitir que o usuário
   * retorne ao mesmo catálogo de instituições.
   */
  if (cityId) {
    page.dataset.cityId = cityId;
  }

  updateBackLink(page, cityId);

  if (!institutionId) {
    showInstitutionRequiredState(page);
    return;
  }

  prepareSelectedInstitutionState(page, institutionId);
}

/*
 * Ponto de entrada para a futura camada de dados. Somente uma instituição
 * previamente validada pelo serviço deve ser entregue a este módulo.
 */
export function renderInstitution(page, institution) {
  if (
    !page ||
    !institution?.id ||
    !(institution?.displayName || institution?.name)
  ) return;

  hideInstitutionStates(page);
  prepareSelectedInstitutionState(page, String(institution.id));
  renderInstitutionDetails(page, institution);
}

/*
 * Recupera o identificador da instituição recebido pela URL.
 * A futura API será responsável por confirmar se ela existe.
 */
function getSelectedInstitutionId() {
  const params = new URLSearchParams(window.location.search);

  const institutionId = params.get("institutionId");

  if (!institutionId) return null;

  const normalizedId = institutionId.trim();

  return normalizedId || null;
}

/*
 * Recupera a cidade de origem recebida pela URL.
 * O identificador é preservado, mas sua validação continuará
 * sendo responsabilidade da futura API.
 */
function getSelectedCityId() {
  const params = new URLSearchParams(window.location.search);

  const cityId = params.get("cityId");

  if (!cityId) return null;

  const normalizedId = cityId.trim();

  return normalizedId || null;
}

/*
 * Mantém o botão de retorno ligado ao catálogo da cidade
 * anteriormente selecionada.
 */
function updateBackLink(page, cityId) {
  const backLink = page.querySelector("[data-institution-back]");

  if (!backLink) return;

  if (!cityId) {
    backLink.href = "./institutions.html";
    return;
  }

  const params = new URLSearchParams();
  params.set("cityId", cityId);

  backLink.href = `./institutions.html?${params.toString()}`;
}

/*
 * Oculta os estados mutuamente exclusivos da página.
 */
function hideInstitutionStates(page) {
  const states = page.querySelectorAll(
    "[data-institution-required], [data-institution-loading], [data-institution-not-found], [data-institution-error]",
  );

  states.forEach((state) => {
    state.hidden = true;
  });

  const details = page.querySelector("[data-institution-details]");

  if (details) {
    details.hidden = true;
  }
}

function showInstitutionRequiredState(page) {
  hideInstitutionStates(page);

  const requiredState = page.querySelector("[data-institution-required]");

  if (requiredState) {
    requiredState.hidden = false;
  }
}

/*
 * Neste momento o identificador é somente preservado.
 * Nenhuma instituição é considerada válida antes da API confirmá-la.
 */
function prepareSelectedInstitutionState(page, institutionId) {
  page.dataset.institutionId = institutionId;
}

/*
 * Será utilizado futuramente enquanto a API consulta a instituição.
 */
export function showInstitutionLoading(page) {
  if (!page) return;

  hideInstitutionStates(page);

  const loadingState = page.querySelector("[data-institution-loading]");

  if (loadingState) {
    loadingState.hidden = false;
  }
}

/*
 * Será utilizado quando a API confirmar que o identificador
 * não corresponde a uma instituição cadastrada.
 */
export function showInstitutionNotFound(page) {
  if (!page) return;

  hideInstitutionStates(page);

  const notFoundState = page.querySelector("[data-institution-not-found]");

  if (notFoundState) {
    notFoundState.hidden = false;
  }
}

/*
 * Será utilizado quando a consulta não puder ser concluída.
 */
export function showInstitutionError(page) {
  if (!page) return;

  hideInstitutionStates(page);

  const errorState = page.querySelector("[data-institution-error]");

  if (errorState) {
    errorState.hidden = false;
  }
}

export function initInstitutionPage() {
  const page = document.querySelector(".institution-page");

  // Este módulo só executa dentro da página de detalhes.
  if (!page) return;

  const institutionId = getSelectedInstitutionId();

  if (!institutionId) {
    showInstitutionRequiredState(page);
    return;
  }

  prepareSelectedInstitutionState(page, institutionId);
}

/*
 * Recupera o identificador recebido pela URL.
 * A futura API será responsável por confirmar se a instituição existe.
 */
function getSelectedInstitutionId() {
  const params = new URLSearchParams(window.location.search);
  const institutionId = params.get("institutionId");

  if (!institutionId) return null;

  const normalizedId = institutionId.trim();

  return normalizedId || null;
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

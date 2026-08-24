import { createCityMap } from "../services/map-service.js";

export function initCitiesPage() {
  const page = document.querySelector(".city-selection");

  // Este módulo só executa quando a tela de seleção de cidades existe.
  if (!page) return;

  initCitySearch(page);
  initCityMap(page);
}

/* =========================================================
   Pesquisa de cidades
   ========================================================= */

function initCitySearch(page) {
  const form = page.querySelector(".city-search");
  const input = page.querySelector("#city-search-input");
  const cityList = page.querySelector("[data-city-list]");

  const emptyState = page.querySelector("[data-city-empty]");
  const emptyMessage = page.querySelector("[data-city-empty-message]");

  if (!form || !input || !cityList || !emptyState || !emptyMessage) {
    return;
  }

  form.addEventListener("submit", (event) => {
    event.preventDefault();

    const query = normalizeText(input.value);

    filterCities(cityList, emptyState, emptyMessage, query);
  });

  /*
   * Quando o campo é completamente limpo, a listagem volta
   * automaticamente ao estado original.
   */
  input.addEventListener("input", () => {
    if (input.value.trim() !== "") return;

    filterCities(cityList, emptyState, emptyMessage, "");
  });
}

/*
 * A busca trabalha sobre os cards realmente presentes na página.
 * No futuro, esses cards poderão vir da API sem alterar esta lógica.
 */
function filterCities(cityList, emptyState, emptyMessage, query) {
  const cityCards = [...cityList.querySelectorAll("[data-city-name]")];

  if (cityCards.length === 0) {
    emptyMessage.textContent =
      "Ainda não há cidades com instituições cadastradas no momento.";

    emptyState.hidden = false;

    return;
  }

  let visibleCities = 0;

  cityCards.forEach((card) => {
    const cityName = normalizeText(card.dataset.cityName ?? "");

    const matchesSearch = cityName.includes(query);

    card.hidden = !matchesSearch;

    if (matchesSearch) {
      visibleCities += 1;
    }
  });

  if (visibleCities === 0) {
    emptyMessage.textContent = "Nenhuma cidade corresponde à sua pesquisa.";

    emptyState.hidden = false;

    return;
  }

  emptyState.hidden = true;
}

function normalizeText(value) {
  return value
    .trim()
    .toLocaleLowerCase("pt-BR")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");
}

/* =========================================================
   Mapa
   ========================================================= */

function initCityMap(page) {
  const mapContainer = page.querySelector("[data-city-map]");

  if (!mapContainer) return;

  /*
   * Evita criar uma segunda instância do Leaflet no mesmo elemento
   * caso a página venha a ser inicializada novamente no futuro.
   */
  if (mapContainer.dataset.mapInitialized === "true") return;

  const map = createCityMap(mapContainer);

  if (!map) {
    console.warn("O mapa do Bem-Feito não pôde ser inicializado.");
    return;
  }

  mapContainer.dataset.mapInitialized = "true";
}

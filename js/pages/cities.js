import { createUnitMap } from "../services/map-service.js";
import { getAvailableCities, getMapUnits } from "../services/public-catalog-service.js";
import { element, errorMessage } from "../components/api-ui.js";

export async function initCitiesPage() {
  const page = document.querySelector(".city-selection"); if (!page) return;
  const list = page.querySelector("[data-city-list]"), empty = page.querySelector("[data-city-empty]"), message = page.querySelector("[data-city-empty-message]");
  const input = page.querySelector("#city-search-input"); let cities = [];
  const status = element("p", "Carregando cidades…"); status.setAttribute("role", "status"); list.before(status); empty.hidden = true;
  function render() {
    const query = normalize(input.value); const filtered = cities.filter((city) => normalize(`${city.name} ${city.state.uf}`).includes(query));
    list.replaceChildren(...filtered.map((city) => {
      const link = element("a", `${city.name} — ${city.state.uf}`, "city-card"); link.href = `./institutions.html?cityId=${encodeURIComponent(city.id)}`; link.dataset.cityName = city.name; return link;
    }));
    empty.hidden = filtered.length > 0; message.textContent = cities.length ? "Nenhuma cidade corresponde à sua pesquisa." : "Ainda não há cidades com instituições publicadas.";
  }
  page.querySelector(".city-search").addEventListener("submit", (event) => { event.preventDefault(); render(); }); input.addEventListener("input", render);
  try { cities = await getAvailableCities(); status.textContent = ""; render(); }
  catch (error) { status.textContent = errorMessage(error); return; }
  const map = page.querySelector("[data-city-map]"); const mapStatus = element("p", "Carregando localizações…"); map.after(mapStatus);
  try {
    const data = await getMapUnits(); createUnitMap(map, data.content);
    mapStatus.textContent = data.content.length ? `${data.content.length} unidades com coordenadas no mapa.` : "Ainda não há unidades publicadas com coordenadas informadas.";
    if (data.totalPages > 1) {
      let number = 0; const units = [...data.content]; const more = element("button", "Carregar mais localizações", "button button--outline"); more.type = "button"; mapStatus.after(more);
      more.addEventListener("click", async () => { more.disabled = true; try { const result = await getMapUnits(null, number + 1); number++; units.push(...result.content); createUnitMap(map, units); mapStatus.textContent = `${units.length} unidades com coordenadas no mapa.`; more.hidden = number + 1 >= result.totalPages; } catch (error) { mapStatus.textContent = errorMessage(error); } finally { more.disabled = false; } });
    }
  } catch (error) { createUnitMap(map, []); mapStatus.textContent = errorMessage(error); }
}
function normalize(value) { return value.trim().toLocaleLowerCase("pt-BR").normalize("NFD").replace(/[\u0300-\u036f]/g, ""); }

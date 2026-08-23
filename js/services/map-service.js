/*
 * Centraliza a integração com o Leaflet.
 * A página não precisa conhecer detalhes internos da biblioteca de mapas.
 */
export function createCityMap(container) {
  if (!container || typeof L === "undefined") return null;

  const map = L.map(container, {
    zoomControl: true,
    scrollWheelZoom: true,
  }).setView([-14.235, -51.9253], 4);

  L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution:
      '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
  }).addTo(map);

  return map;
}

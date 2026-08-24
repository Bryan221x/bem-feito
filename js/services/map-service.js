/*
 * Centraliza a integração com o Leaflet.
 * A página não precisa conhecer detalhes internos da biblioteca de mapas.
 */
export function createCityMap(container) {
  if (!container || typeof L === "undefined") return null;

  /*
   * O enquadramento inicial mostra o Brasil e não representa
   * a localização de nenhuma instituição cadastrada.
   */
  const brazilCenter = [-14.235, -51.9253];

  const map = L.map(container, {
    zoomControl: true,

    // Evita que a rolagem normal da página seja capturada pelo mapa.
    scrollWheelZoom: false,
  }).setView(brazilCenter, 4);

  L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution:
      '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
  }).addTo(map);

  return map;
}

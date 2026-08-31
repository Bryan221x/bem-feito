const unitMaps = new WeakMap();

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

/*
 * Cria o mapa público de uma instituição usando uma marca por unidade física.
 * As coordenadas devem vir prontas da API; este serviço não geocodifica dados.
 */
export function createUnitMap(container, units) {
  if (!container || typeof L === "undefined" || !Array.isArray(units)) {
    return null;
  }

  const validUnits = units.filter(
    (unit) =>
      Number.isFinite(Number(unit.latitude)) &&
      Number.isFinite(Number(unit.longitude)),
  );

  if (!validUnits.length) return null;

  unitMaps.get(container)?.remove();

  const map = L.map(container, {
    zoomControl: true,
    scrollWheelZoom: false,
  });

  L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution:
      '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
  }).addTo(map);

  const bounds = [];

  validUnits.forEach((unit) => {
    const coordinates = [Number(unit.latitude), Number(unit.longitude)];
    const marker = L.marker(coordinates).addTo(map);

    if (unit.name) marker.bindPopup(String(unit.name));

    bounds.push(coordinates);
  });

  map.fitBounds(bounds, { padding: [24, 24], maxZoom: 15 });

  unitMaps.set(container, map);

  return map;
}

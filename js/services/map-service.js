const unitMaps = new WeakMap();

/*
 * Centraliza a integração com o Leaflet.
 * A página não precisa conhecer detalhes internos da biblioteca de mapas.
 */
export function createCityMap(container, { onUnavailable = () => {} } = {}) {
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

  unitMaps.set(container, map);
  L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution:
      '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
  }).on("tileerror", onUnavailable).addTo(map);

  return map;
}

/*
 * Cria o mapa público de uma instituição usando uma marca por unidade física.
 * As coordenadas devem vir prontas da API; este serviço não geocodifica dados.
 */
export function createUnitMap(container, units, { onUnavailable = () => {} } = {}) {
  if (!container || typeof L === "undefined" || !Array.isArray(units)) {
    return null;
  }

  try {
    unitMaps.get(container)?.remove();
    unitMaps.delete(container);
    const validUnits = units.filter(hasCoordinates);
    if (!validUnits.length) return createCityMap(container, { onUnavailable });

    const map = L.map(container, {
      zoomControl: true,
      scrollWheelZoom: false,
    });

    unitMaps.set(container, map);
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).on("tileerror", onUnavailable).addTo(map);

    const bounds = [];

    validUnits.forEach((unit) => {
      const coordinates = [Number(unit.latitude), Number(unit.longitude)];
      const marker = L.marker(coordinates).addTo(map);

      if (unit.name) {
        const text = document.createElement("div"), name = document.createElement("p");
        name.textContent = [unit.institutionName, unit.name].filter(Boolean).join(" — "); text.append(name);
        if (unit.institutionId) { const link = document.createElement("a"); link.textContent = "Conhecer instituição"; link.href = `./institution.html?id=${encodeURIComponent(unit.institutionId)}`; text.append(link); }
        marker.bindPopup(text);
      }

      bounds.push(coordinates);
    });

    map.fitBounds(bounds, { padding: [24, 24], maxZoom: 15 });

    unitMaps.set(container, map);

    return map;
  } catch {
    try { unitMaps.get(container)?.remove(); } catch { /* Falha de inicialização do mapa. */ }
    unitMaps.delete(container);
    onUnavailable();
    return null;
  }
}

export function hasCoordinates(unit) {
  return unit?.latitude != null && unit?.longitude != null && String(unit.latitude).trim() !== "" && String(unit.longitude).trim() !== "" &&
    Number.isFinite(Number(unit.latitude)) && Number.isFinite(Number(unit.longitude)) && Math.abs(Number(unit.latitude)) <= 90 && Math.abs(Number(unit.longitude)) <= 180;
}

export function removeMap(container) {
  unitMaps.get(container)?.remove(); unitMaps.delete(container);
}
export function createLocationPicker(container, coordinates, onChange, onUnavailable) {
  try {
    removeMap(container);
    const map = createCityMap(container, { onUnavailable });
    if (!map) { onUnavailable(); return null; }
    let marker;
    const select = point => {
      if (!hasCoordinates({ latitude:point.lat, longitude:point.lng })) return;
      if (marker) marker.setLatLng(point);
      else marker = L.marker(point, { draggable:true }).addTo(map).on("dragend", () => onChange(marker.getLatLng()));
      onChange(point);
    };
    if (hasCoordinates(coordinates)) { const point = { lat:Number(coordinates.latitude), lng:Number(coordinates.longitude) }; select(point); map.setView(point,16); }
    map.on("click", event => select(event.latlng));
    return map;
  } catch { onUnavailable(); return null; }
}

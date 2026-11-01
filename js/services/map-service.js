const unitMaps = new WeakMap();
const unitMapStates = new WeakMap();

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
export function createUnitMap(container, units, { onUnavailable = () => {}, popupContent = null, onUnitSelect = () => {} } = {}) {
  if (!container || typeof L === "undefined" || !Array.isArray(units)) {
    return null;
  }

  try {
    removeMap(container);
    const validUnits = units.filter(hasCoordinates);
    if (!validUnits.length) return createCityMap(container, { onUnavailable });

    const map = L.map(container, {
      zoomControl: true,
      scrollWheelZoom: false,
    });

    unitMaps.set(container, map);
    const state = { map, markers: new Map(), selectedId: null };
    unitMapStates.set(container, state);
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).on("tileerror", onUnavailable).addTo(map);

    const bounds = [];

    validUnits.forEach((unit, index) => {
      const coordinates = [Number(unit.latitude), Number(unit.longitude)];
      const marker = L.marker(coordinates).addTo(map);

      if (popupContent) marker.bindPopup(popupContent(unit));
      else if (unit.name) {
        const text = document.createElement("div"), name = document.createElement("p");
        name.textContent = [unit.institutionName, unit.name].filter(Boolean).join(" — "); text.append(name);
        if (unit.institutionId) { const link = document.createElement("a"); link.textContent = "Conhecer instituição"; link.href = `./institution.html?id=${encodeURIComponent(unit.institutionId)}`; text.append(link); }
        marker.bindPopup(text);
      }

      const id = String(unit.unitId ?? unit.id ?? index);
      state.markers.set(id, { marker, coordinates });
      marker.on?.("click", () => {
        selectUnitMarker(state, id, false);
        onUnitSelect(unit);
      });

      bounds.push(coordinates);
    });

    map.fitBounds(bounds, { padding: [24, 24], maxZoom: 15 });

    unitMaps.set(container, map);

    return map;
  } catch {
    try { unitMaps.get(container)?.remove(); } catch { /* Falha de inicialização do mapa. */ }
    unitMaps.delete(container);
    unitMapStates.delete(container);
    onUnavailable();
    return null;
  }
}

export function hasCoordinates(unit) {
  return unit?.latitude != null && unit?.longitude != null && String(unit.latitude).trim() !== "" && String(unit.longitude).trim() !== "" &&
    Number.isFinite(Number(unit.latitude)) && Number.isFinite(Number(unit.longitude)) && Math.abs(Number(unit.latitude)) <= 90 && Math.abs(Number(unit.longitude)) <= 180;
}

export function removeMap(container) {
  unitMaps.get(container)?.remove(); unitMaps.delete(container); unitMapStates.delete(container);
}

export function focusUnitMap(container, unitId) {
  const state = unitMapStates.get(container);
  const id = String(unitId ?? "");
  if (!state || !state.markers.has(id)) return false;
  selectUnitMarker(state, id, true);
  return true;
}

function selectUnitMarker(state, id, openPopup) {
  const selected = state.markers.get(id);
  if (!selected) return;
  if (state.selectedId && state.selectedId !== id) {
    const previous = state.markers.get(state.selectedId)?.marker;
    previous?.setZIndexOffset?.(0);
    previous?.getElement?.()?.classList.remove("is-selected");
  }
  state.selectedId = id;
  selected.marker.setZIndexOffset?.(1000);
  selected.marker.getElement?.()?.classList.add("is-selected");
  state.map.setView(selected.coordinates, Math.max(Number(state.map.getZoom?.() || 0), 16));
  if (openPopup) selected.marker.openPopup?.();
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

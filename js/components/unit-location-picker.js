import { createLocationPicker, hasCoordinates, removeMap } from "../services/map-service.js";
import { parseGoogleMapsLink } from "../services/google-maps-link.js";
import { geocodeUnitAddress } from "../services/unit-service.js";

export function initUnitLocationPicker(page) {
  const open = page.querySelector("[data-open-location]");
  if (!open) return;
  const panel = page.querySelector("[data-location-panel]"), map = page.querySelector("[data-location-picker]"), feedback = page.querySelector("[data-location-feedback]");
  const latitude = page.querySelector('[name="latitude"]'), longitude = page.querySelector('[name="longitude"]');
  const link = page.querySelector("[data-google-maps-link]"), locate = page.querySelector("[data-locate-google-maps]");
  const fallback = page.querySelector("[data-google-maps-fallback]"), openGoogleMaps = page.querySelector("[data-open-google-maps]");
  const findAddress = page.querySelector("[data-find-address]"), resultsPanel = page.querySelector("[data-address-search-results]");
  const resultSelect = page.querySelector("[data-address-result]"), resultNote = page.querySelector("[data-address-result-note]");
  const useResult = page.querySelector("[data-use-address-result]"), confirm = page.querySelector("[data-confirm-location]");
  let pending = null, candidates = [], chosenCandidate = null, searchSequence = 0;

  open.disabled = latitude.disabled; confirm.disabled = latitude.disabled;
  if (locate) locate.disabled = latitude.disabled;
  if (findAddress) findAddress.disabled = latitude.disabled;
  const unavailable = () => { feedback.textContent = "Mapa indisponível. Confira o endereço ou informe as coordenadas manualmente e confirme o ponto."; };

  const showMap = (coordinates, qualityHint = null) => {
    panel.hidden = false;
    open.textContent = "Cancelar seleção no mapa";
    createLocationPicker(map, coordinates, point => {
      pending = point;
      feedback.textContent = qualityHint || "Confira o marcador no mapa. Você pode clicar em outro ponto e confirmar quando estiver correto.";
    }, unavailable);
  };

  findAddress?.addEventListener("click", async () => {
    if (latitude.disabled) return;
    const city = page.querySelector('[name="cityId"]');
    const request = {
      street: page.querySelector('[name="address"]')?.value.trim(),
      number: page.querySelector('[name="number"]')?.value.trim(),
      neighborhood: page.querySelector('[name="neighborhood"]')?.value.trim(),
      postalCode: page.querySelector('[name="postalCode"]')?.value.trim(),
      city: city?.selectedOptions?.[0]?.textContent?.trim(),
      state: page.querySelector('[name="state"]')?.value.trim(),
    };
    if (!request.street || !request.city || !request.state) {
      feedback.textContent = "Preencha logradouro, cidade e estado antes de procurar o endereço.";
      return;
    }
    const sequence = ++searchSequence;
    findAddress.disabled = true;
    findAddress.textContent = "Procurando endereço…";
    resultsPanel.hidden = true; resultNote.textContent = ""; useResult.disabled = true;
    candidates = []; chosenCandidate = null; pending = null;
    panel.hidden = true; removeMap(map); open.textContent = "Prefiro selecionar no mapa";
    feedback.textContent = "Consultando o OpenStreetMap. O endereço só é enviado quando você solicita a busca.";
    try {
      const found = await geocodeUnitAddress(request);
      if (sequence !== searchSequence) return;
      candidates = Array.isArray(found) ? found : [];
      if (!candidates.length) {
        feedback.textContent = "Não encontramos esse endereço. Confira os dados ou escolha o ponto manualmente no mapa.";
        return;
      }
      resultSelect.replaceChildren(new Option("Selecione o endereço correspondente", ""), ...candidates.map((candidate, index) => {
        const detail = candidate.approximate ? " — resultado aproximado" : " — número informado encontrado; confira o ponto";
        return new Option(`${candidate.label}${detail}`, String(index));
      }));
      resultsPanel.hidden = false;
      feedback.textContent = candidates.length > 1 ? "Encontramos mais de uma opção. Escolha a que corresponde à unidade." : "Encontramos uma opção. Confira os detalhes e selecione-a para ver o ponto no mapa.";
    } catch (error) {
      if (sequence !== searchSequence) return;
      feedback.textContent = error.status === 429
        ? "A busca foi feita há pouco. Aguarde um segundo e tente novamente."
        : "A busca de endereço está temporariamente indisponível. Confira o endereço ou escolha o ponto no mapa.";
    } finally {
      if (sequence === searchSequence) {
        findAddress.disabled = latitude.disabled;
        findAddress.textContent = "Encontrar endereço no mapa";
      }
    }
  });

  resultSelect?.addEventListener("change", () => {
    const index = Number(resultSelect.value);
    chosenCandidate = resultSelect.value !== "" && Number.isInteger(index) ? candidates[index] || null : null;
    useResult.disabled = !chosenCandidate;
    if (!chosenCandidate) { resultNote.textContent = ""; return; }
    resultNote.textContent = chosenCandidate.approximate
      ? "Este resultado é aproximado: pode indicar a rua ou a área, não a entrada exata. Confira e ajuste o marcador."
      : "O número informado aparece neste resultado. Confira se o marcador corresponde à entrada correta.";
  });

  useResult?.addEventListener("click", () => {
    if (!chosenCandidate) return;
    const note = chosenCandidate.approximate
      ? "Resultado aproximado. Ajuste o marcador se necessário e confirme o ponto."
      : "Resultado com o número informado. Confira o marcador e confirme o ponto.";
    showMap({ latitude: chosenCandidate.latitude, longitude: chosenCandidate.longitude }, note);
  });

  open.addEventListener("click", () => {
    if (latitude.disabled) return;
    if (!panel.hidden) {
      panel.hidden = true; removeMap(map); pending = null; chosenCandidate = null;
      open.textContent = "Prefiro selecionar no mapa";
      feedback.textContent = "Seleção cancelada. A localização salva continua igual.";
      return;
    }
    pending = null; chosenCandidate = null;
    feedback.textContent = "Clique no mapa ou arraste o marcador para escolher o ponto. Confirme antes de salvar.";
    showMap({ latitude: latitude.value, longitude: longitude.value });
  });

  locate?.addEventListener("click", () => {
    if (latitude.disabled) return;
    fallback.hidden = true; openGoogleMaps.removeAttribute("href");
    const result = parseGoogleMapsLink(link.value);
    if (result.status !== "found") {
      const messages = {
        empty: "Cole primeiro o link compartilhado do Google Maps.",
        invalid: "Esse link não parece válido. Copie novamente o endereço compartilhado do Google Maps.",
        unsupported: "Use um link HTTPS do Google Maps. Esse domínio ou formato não é aceito.",
        "short-link": "Este link curto não mostra as coordenadas, mas você pode abri-lo no Google Maps e escolher o mesmo ponto no mapa.",
        "no-coordinates": "Não encontramos coordenadas confiáveis neste link. O centro exibido pelo Google Maps pode ser apenas a visualização; selecione o ponto manualmente.",
        "invalid-coordinates": "As coordenadas do link não são válidas. Confira o link ou selecione o ponto manualmente.",
      };
      if (result.status === "short-link") { openGoogleMaps.href = result.url; fallback.hidden = false; }
      feedback.textContent = messages[result.status] || "Não foi possível identificar a localização. Selecione o ponto manualmente.";
      return;
    }
    pending = null; chosenCandidate = null;
    feedback.textContent = "Localização identificada. Confira o marcador e confirme o ponto para usar.";
    showMap(result.point);
  });

  [latitude, longitude].forEach(input => input.addEventListener("input", () => { pending = null; page.dataset.coordinatesConfirmed = "false"; }));
  confirm.addEventListener("click", () => {
    if (latitude.disabled) return;
    const point = pending ? { latitude: pending.lat, longitude: pending.lng } : { latitude: latitude.value, longitude: longitude.value };
    if (!hasCoordinates(point)) { feedback.textContent = "Selecione um ponto no mapa ou informe coordenadas válidas antes de confirmar."; return; }
    latitude.value = Number(point.latitude).toFixed(8); longitude.value = Number(point.longitude).toFixed(8);
    latitude.dispatchEvent(new Event("input", { bubbles: true })); longitude.dispatchEvent(new Event("input", { bubbles: true }));
    page.dataset.coordinatesConfirmed = "true";
    feedback.textContent = "Ponto confirmado. Salve a unidade para publicar a localização.";
    panel.hidden = true; removeMap(map); pending = null; open.textContent = "Prefiro selecionar no mapa";
  });

  return () => {
    searchSequence++; candidates = []; chosenCandidate = null; pending = null;
    resultsPanel.hidden = true; useResult.disabled = true;
    resultSelect.replaceChildren(new Option("Selecione o endereço correspondente", "")); resultNote.textContent = "";
    panel.hidden = true; removeMap(map); open.textContent = "Prefiro selecionar no mapa";
    feedback.textContent = "Endereço alterado. Confirme novamente a localização; sem confirmação, ela será removida do mapa ao salvar.";
  };
}

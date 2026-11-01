import { createLocationPicker, hasCoordinates, removeMap } from "../services/map-service.js";
export function initUnitLocationPicker(page) {
  const open = page.querySelector("[data-open-location]"); if (!open) return;
  const panel=page.querySelector("[data-location-panel]"), map=page.querySelector("[data-location-picker]"), feedback=page.querySelector("[data-location-feedback]");
  const latitude=page.querySelector('[name="latitude"]'), longitude=page.querySelector('[name="longitude"]');
  let pending=null;
  const confirm=page.querySelector("[data-confirm-location]");
  open.disabled=latitude.disabled;confirm.disabled=latitude.disabled;
  const unavailable=()=>{ feedback.textContent="Mapa indisponível. Informe as coordenadas manualmente e confirme o ponto."; };
  open.addEventListener("click",()=>{
    if(latitude.disabled) return;
    panel.hidden=!panel.hidden;
    if(panel.hidden) { removeMap(map);pending=null;return; }
    pending=null;feedback.textContent="Clique no mapa ou arraste o marcador. Confirme o ponto antes de salvar.";
    createLocationPicker(map,{ latitude:latitude.value,longitude:longitude.value },point=>{pending=point;feedback.textContent=`Ponto selecionado: ${point.lat.toFixed(6)}, ${point.lng.toFixed(6)}. Confirme para usar.`;},unavailable);
  });
  [latitude,longitude].forEach(input=>input.addEventListener("input",()=>{pending=null;page.dataset.coordinatesConfirmed="false";}));
  confirm.addEventListener("click",()=>{
    if(latitude.disabled) return;
    const point=pending ? { latitude:pending.lat,longitude:pending.lng } : { latitude:latitude.value,longitude:longitude.value };
    if(!hasCoordinates(point)) { feedback.textContent="Informe latitude e longitude válidas ou selecione um ponto no mapa.";return; }
    latitude.value=Number(point.latitude).toFixed(8);longitude.value=Number(point.longitude).toFixed(8);
    latitude.dispatchEvent(new Event("input",{bubbles:true}));longitude.dispatchEvent(new Event("input",{bubbles:true}));
    page.dataset.coordinatesConfirmed="true";feedback.textContent="Ponto confirmado. Salve a unidade para publicar a localização.";
    panel.hidden=true;removeMap(map);pending=null;
  });
  return () => { pending=null;panel.hidden=true;removeMap(map);feedback.textContent="Endereço alterado. Confirme novamente a localização; sem confirmação, ela será removida do mapa ao salvar."; };
}

import test from 'node:test';
import assert from 'node:assert/strict';
import { initUnitLocationPicker } from '../../js/components/unit-location-picker.js';

function button() { return { disabled: false, textContent: '', listeners: {}, addEventListener(type, fn) { this.listeners[type] = fn; }, click() { return this.listeners.click?.({ currentTarget: this }); } }; }
function input(value = '') { return { value, disabled: false, listeners: {}, addEventListener(type, fn) { this.listeners[type] = fn; }, dispatchEvent(event) { this.listeners[event.type]?.(event); }, removeAttribute(name) { delete this[name]; } }; }

function setup({ saved = null } = {}) {
  const open = button(), locate = button(), confirm = button();
  const link = input(), latitude = input(saved?.latitude ?? ''), longitude = input(saved?.longitude ?? '');
  const panel = { hidden: true }, fallback = { hidden: true }, openGoogleMaps = { removeAttribute(name) { delete this[name]; } };
  const findAddress = button(), resultsPanel = { hidden: true }, resultNote = { textContent: '' }, useResult = button();
  const resultSelect = { value: '', options: [], listeners: {}, addEventListener(type, fn) { this.listeners[type] = fn; }, replaceChildren(...options) { this.options = options; this.value = ''; }, change(value) { this.value = value; this.listeners.change(); } };
  const fields = { address: input('Praça da Sé'), number: input('10'), neighborhood: input('Sé'), postalCode: input('01001-000'), cityId: { selectedOptions: [{ textContent: 'São Paulo' }] }, state: input('SP') };
  const feedback = { textContent: '' }, mapContainer = { handlers: {} };
  const elements = new Map([
    ['[data-open-location]', open], ['[data-location-panel]', panel], ['[data-location-picker]', mapContainer],
    ['[data-location-feedback]', feedback], ['[name="latitude"]', latitude], ['[name="longitude"]', longitude],
    ['[data-google-maps-link]', link], ['[data-locate-google-maps]', locate], ['[data-google-maps-fallback]', fallback],
    ['[data-open-google-maps]', openGoogleMaps], ['[data-confirm-location]', confirm],
    ['[data-find-address]', findAddress], ['[data-address-search-results]', resultsPanel], ['[data-address-result]', resultSelect],
    ['[data-address-result-note]', resultNote], ['[data-use-address-result]', useResult],
    ...Object.entries(fields).map(([name, element]) => [`[name="${name}"]`, element]),
  ]);
  const page = { dataset: { coordinatesConfirmed: saved ? 'true' : 'false' }, querySelector(selector) { return elements.get(selector) || null; } };
  const maps = [];
  globalThis.Option = class { constructor(text, value) { this.textContent = text; this.value = value; } };
  globalThis.L = {
    map(container) { const map = { handlers: {}, on(type, fn) { this.handlers[type] = fn; return this; }, setView() { return this; }, remove() {} }; maps.push(map); return map; },
    tileLayer() { return { on() { return this; }, addTo() { return this; } }; },
    marker(point) { let current = point; return { addTo() { return this; }, on() { return this; }, setLatLng(value) { current = value; }, getLatLng() { return current; } }; },
  };
  initUnitLocationPicker(page);
  return { open, locate, confirm, link, latitude, longitude, panel, fallback, openGoogleMaps, feedback, maps, page, findAddress, resultsPanel, resultSelect, resultNote, useResult };
}

test('short link fallback opens only the official link and leaves the saved location untouched until manual confirmation', () => {
  const ui = setup({ saved: { latitude: '-23.55000000', longitude: '-46.63000000' } });
  ui.link.value = 'https://maps.app.goo.gl/example';
  ui.locate.click();
  assert.equal(ui.fallback.hidden, false);
  assert.equal(ui.openGoogleMaps.href, 'https://maps.app.goo.gl/example');
  assert.equal(ui.openGoogleMaps.target, undefined); // Target and rel are fixed in the HTML, not derived from user input.
  assert.equal(ui.latitude.value, '-23.55000000');
  assert.equal(ui.longitude.value, '-46.63000000');

  ui.open.click();
  const map = ui.maps.at(-1);
  map.handlers.click({ latlng: { lat: -23.56, lng: -46.64 } });
  assert.equal(ui.latitude.value, '-23.55000000');
  assert.equal(ui.longitude.value, '-46.63000000');
  ui.confirm.click();
  assert.equal(ui.latitude.value, '-23.56000000');
  assert.equal(ui.longitude.value, '-46.64000000');
  assert.equal(ui.page.dataset.coordinatesConfirmed, 'true');
});

test('address search is explicit, offers candidates, labels approximate matches, and requires selection before preview', async () => {
  const ui = setup({ saved: { latitude: '-23.55000000', longitude: '-46.63000000' } });
  const previousFetch = globalThis.fetch;
  let requested = null;
  globalThis.fetch = async (url, options = {}) => {
    if (String(url).endsWith('/api/auth/csrf')) return { ok: true, status: 200, json: async () => ({ token: 'csrf-test', headerName: 'X-CSRF-TOKEN' }) };
    requested = { url: new URL(url, 'https://bem-feito.test'), options };
    return { ok: true, status: 200, json: async () => [
      { latitude: -23.56, longitude: -46.64, label: 'Praça da Sé, São Paulo', houseNumber: null, approximate: true },
      { latitude: -23.55, longitude: -46.63, label: 'Praça da Sé, 10, São Paulo', houseNumber: '10', approximate: false },
    ] };
  };
  try {
    assert.equal(requested, null, 'opening the form must not trigger geocoding');
    await ui.findAddress.click();
    assert.equal(requested.url.pathname, '/api/units/geocode');
    assert.equal(requested.options.method, 'POST');
    assert.equal(requested.options.headers.get('X-CSRF-TOKEN'), 'csrf-test');
    assert.deepEqual(JSON.parse(requested.options.body), { street: 'Praça da Sé', number: '10', neighborhood: 'Sé', postalCode: '01001-000', city: 'São Paulo', state: 'SP' });
    assert.equal(ui.resultsPanel.hidden, false);
    assert.equal(ui.resultSelect.options.length, 3);
    assert.match(ui.resultSelect.options[1].textContent, /aproximado/);
    assert.equal(ui.latitude.value, '-23.55000000');

    ui.resultSelect.change('0');
    assert.equal(ui.useResult.disabled, false);
    assert.match(ui.resultNote.textContent, /aproximado/);
    ui.useResult.click();
    assert.equal(ui.panel.hidden, false);
    assert.equal(ui.latitude.value, '-23.55000000', 'a candidate preview must not overwrite saved coordinates');
    ui.open.click();
    assert.equal(ui.latitude.value, '-23.55000000', 'canceling the preview must preserve saved coordinates');
    assert.equal(ui.page.dataset.coordinatesConfirmed, 'true');
  } finally { globalThis.fetch = previousFetch; }
});

test('canceling manual map selection preserves previously saved coordinates', () => {
  const ui = setup({ saved: { latitude: '-23.55000000', longitude: '-46.63000000' } });
  ui.open.click();
  ui.maps.at(-1).handlers.click({ latlng: { lat: -23.56, lng: -46.64 } });
  ui.open.click();
  assert.equal(ui.latitude.value, '-23.55000000');
  assert.equal(ui.longitude.value, '-46.63000000');
  assert.equal(ui.page.dataset.coordinatesConfirmed, 'true');
});

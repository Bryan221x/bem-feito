import test from 'node:test';
import assert from 'node:assert/strict';
import { parseGoogleMapsLink } from '../../js/services/google-maps-link.js';

test('accepts explicit place coordinates from a full Google Maps place URL', () => {
  assert.deepEqual(parseGoogleMapsLink('https://www.google.com/maps/place/Local/@-23.5,-46.6,16z/data=!3m1!4b1!4m6!3m5!1sabc!8m2!3d-23.55052!4d-46.633308'), {
    status: 'found', point: { latitude: -23.55052, longitude: -46.633308 },
  });
});

test('accepts explicit coordinate searches on official Google Maps URLs', () => {
  assert.deepEqual(parseGoogleMapsLink('https://google.com/maps?q=-23.55%2C-46.63'), {
    status: 'found', point: { latitude: -23.55, longitude: -46.63 },
  });
});

test('does not treat viewport coordinates as a selected place', () => {
  assert.equal(parseGoogleMapsLink('https://www.google.com/maps/@-23.55,-46.63,15z').status, 'no-coordinates');
});

test('does not resolve short links or allow untrusted hosts and schemes', () => {
  assert.deepEqual(parseGoogleMapsLink('https://maps.app.goo.gl/abc123'), {
    status: 'short-link', url: 'https://maps.app.goo.gl/abc123',
  });
  assert.equal(parseGoogleMapsLink('https://evil.example/maps?q=1,2').status, 'unsupported');
  assert.equal(parseGoogleMapsLink('http://google.com/maps?q=1,2').status, 'unsupported');
  assert.equal(parseGoogleMapsLink('https://google.com.evil.example/maps?q=1,2').status, 'unsupported');
});

test('parses a coordinate-bearing destination after a Google Maps short-link redirect', () => {
  // This is the destination URL observed after opening a public maps.app.goo.gl example.
  assert.deepEqual(parseGoogleMapsLink('https://www.google.com.mx/maps/search/19.402177,+-99.170853?entry=tts'), {
    status: 'found', point: { latitude: 19.402177, longitude: -99.170853 },
  });
});

test('rejects invalid coordinates and malformed URLs', () => {
  assert.equal(parseGoogleMapsLink('https://google.com/maps?q=91,181').status, 'invalid-coordinates');
  assert.equal(parseGoogleMapsLink('not a URL').status, 'invalid');
  assert.equal(parseGoogleMapsLink('').status, 'empty');
});

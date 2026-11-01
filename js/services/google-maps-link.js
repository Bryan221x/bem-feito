import { hasCoordinates } from "./map-service.js";

const GOOGLE_HOST = /^(?:(?:www|maps)\.)?google\.(?:com(?:\.[a-z]{2})?|co\.[a-z]{2}|[a-z]{2,3})$/;

/** Extracts coordinates only when the URL labels a place, never from the map viewport (@lat,lng). */
export function parseGoogleMapsLink(value) {
  const text = String(value || "").trim();
  if (!text) return { status: "empty" };

  let url;
  try { url = new URL(text); } catch { return { status: "invalid" }; }
  const host = url.hostname.toLowerCase();
  if (url.protocol !== "https:" || !(host === "maps.app.goo.gl" || GOOGLE_HOST.test(host)) || url.username || url.password || url.port) {
    return { status: "unsupported" };
  }
  if (url.hostname === "maps.app.goo.gl") return { status: "short-link", url: url.href };

  // Google Maps embeds a selected place coordinate as !3dLAT!4dLNG.
  let decodedPath = url.pathname;
  try { decodedPath = decodeURIComponent(decodedPath); } catch { return { status: "invalid" }; }
  const pathPair = decodedPath.match(/\/maps\/search\/(-?\d+(?:\.\d+)?),\+?(-?\d+(?:\.\d+)?)(?:\/|$)/i);
  const dataPlace = decodedPath + url.search;
  const place = dataPlace.match(/!3d(-?\d+(?:\.\d+)?)!4d(-?\d+(?:\.\d+)?)/i);
  const query = url.searchParams.get("query") || url.searchParams.get("q");
  const queryPair = query && query.match(/^\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*$/);
  const match = place || pathPair || queryPair;
  if (!match) return { status: "no-coordinates" };

  const point = { latitude: Number(match[1]), longitude: Number(match[2]) };
  if (!hasCoordinates(point)) return { status: "invalid-coordinates" };
  return { status: "found", point };
}

// Apenas CSRF fica em memória; a autenticação reside no cookie HttpOnly da sessão.
const configuredBase =
  typeof document === "undefined"
    ? null
    : document.querySelector('meta[name="bemfeito-api-base"]')?.content;

const localDevelopment =
  typeof location !== "undefined" &&
  ["localhost", "127.0.0.1"].includes(location.hostname) &&
  location.protocol === "http:" &&
  location.port === "5500";

const baseUrl = (
  configuredBase || (localDevelopment ? `http://${location.hostname}:8080` : "")
).replace(/\/+$/, "");
let csrf = null;
let csrfLoading = null;

export class ApiError extends Error {
  constructor(status, body) {
    super(body?.message || "Não foi possível concluir a solicitação.");
    this.status = status;
    this.fields = body?.fields || {};
  }
}

export function clearCsrfToken() {
  csrf = null;
  csrfLoading = null;
}

export async function getCsrfToken() {
  if (csrf) return csrf;
  if (!csrfLoading) {
    csrfLoading = apiRequest("/api/auth/csrf", {
      csrfRequired: false,
      notifyAuth: false,
    })
      .then((value) => {
        csrf = value;
        return value;
      })
      .finally(() => {
        csrfLoading = null;
      });
  }
  return csrfLoading;
}

export async function apiRequest(
  path,
  {
    method = "GET",
    body,
    headers = {},
    csrfRequired = true,
    notifyAuth = true,
  } = {},
) {
  const requestHeaders = new Headers(headers);
  const mutable = !["GET", "HEAD", "OPTIONS"].includes(method.toUpperCase());
  if (mutable && csrfRequired) {
    const token = await getCsrfToken();
    requestHeaders.set(token.headerName, token.token);
  }
  let payload = body;
  if (body instanceof URLSearchParams)
    requestHeaders.set(
      "Content-Type",
      "application/x-www-form-urlencoded;charset=UTF-8",
    );
  else if (typeof FormData !== "undefined" && body instanceof FormData) {
    requestHeaders.delete("Content-Type");
  } else if (body !== undefined) {
    requestHeaders.set("Content-Type", "application/json");
    payload = JSON.stringify(body);
  }
  const response = await fetch(`${baseUrl}${path}`, {
    method,
    body: payload,
    headers: requestHeaders,
    credentials: "include",
  });
  let result = null;
  if (response.status !== 204) {
    try {
      result = await response.json();
    } catch {
      /* Erros sem corpo também são representados por ApiError. */
    }
  }
  if (!response.ok) {
    if ([401, 403].includes(response.status)) clearCsrfToken();
    if (
      notifyAuth &&
      [401, 403].includes(response.status) &&
      typeof document !== "undefined"
    ) {
      document.dispatchEvent(
        new CustomEvent(`bemfeito:api-${response.status}`, {
          detail: { path },
        }),
      );
    }
    // Não repetir mutações automaticamente: uma falha de permissão não é pedido de retry.
    throw new ApiError(response.status, result);
  }
  return result;
}

export function apiAssetUrl(value) {
  if (typeof value !== "string") return null;
  if (value.startsWith("/api/history-media/")) return `${baseUrl}${value}`;
  try {
    const url = new URL(value);
    return ["http:", "https:"].includes(url.protocol) &&
      !url.username &&
      !url.password
      ? url.href
      : null;
  } catch {
    return null;
  }
}

export async function lookupPostalCode(value) {
  const cep = String(value).replace(/\D/g, "");
  if (!/^[0-9]{8}$/.test(cep))
    throw new Error("Informe um CEP com oito dígitos.");
  const response = await fetch(`https://viacep.com.br/ws/${cep}/json/`, {
    credentials: "omit",
    signal: AbortSignal.timeout(8000),
    referrerPolicy: "no-referrer",
  });
  if (!response.ok)
    throw new Error("Consulta de CEP indisponível. Preencha manualmente.");
  const data = await response.json();
  if (data.erro)
    throw new Error("CEP não encontrado. Confira ou preencha manualmente.");
  return data;
}

import { getCurrentUser, getMyInstitutions, logout } from "./auth-service.js";
let eventsBound = false;

export function userDestination(user) {
  return user.platformRole === "PLATFORM_ADMIN" ? "./admin-dashboard.html" : "./institution-selection.html";
}

export async function guardPage() {
  const requirement = document.documentElement.dataset.authRequired;
  if (!requirement) return true;
  try {
    const user = await getCurrentUser();
    if (requirement === "admin" && user.platformRole !== "PLATFORM_ADMIN") {
      location.replace("./institution-selection.html"); return false;
    }
    const pageName = location.pathname.split("/").pop();
    if (user.platformRole !== "PLATFORM_ADMIN" && ["my-institution.html", "unit-form.html"].includes(pageName)) {
      const institutionId = new URLSearchParams(location.search).get("institutionId");
      const memberships = await getMyInstitutions();
      const selected = memberships.find((item) => String(item.institutionId) === institutionId);
      if (!selected || (pageName === "unit-form.html" && !new URLSearchParams(location.search).has("unitId")
          && !selected.permissions.includes("CREATE_UNIT"))) {
        location.replace("./institution-selection.html"); return false;
      }
    }
    document.documentElement.classList.remove("auth-pending");
    if (!eventsBound) {
      eventsBound = true;
      document.addEventListener("bemfeito:api-401", () => location.replace("./institution-login.html"));
      document.addEventListener("bemfeito:api-403", () => {
        let message = document.querySelector("[data-access-denied]");
        if (!message) {
          message = document.createElement("p"); message.dataset.accessDenied = ""; message.setAttribute("role", "alert");
          document.body.prepend(message);
        }
        message.textContent = "Acesso negado ou sessão de segurança expirada.";
      });
    }
    return true;
  } catch (error) {
    if (error.status === 401) location.replace("./institution-login.html");
    else {
      // Falha fechada: não revelar o conteúdo protegido quando o backend estiver indisponível.
      const message = document.createElement("p");
      message.setAttribute("role", "alert");
      message.textContent = "Não foi possível verificar sua sessão. Atualize a página para tentar novamente.";
      document.body.replaceChildren(message);
      document.documentElement.classList.remove("auth-pending");
    }
    return false;
  }
}

export function initSessionRecheck() {
  if (typeof window === "undefined") return;
  window.addEventListener("pageshow", (event) => {
    if (!event.persisted || !document.documentElement.dataset.authRequired) return;
    document.documentElement.classList.add("auth-pending");
    void guardPage();
  });
}

export function initLogout() {
  document.querySelectorAll("[data-auth-logout]").forEach((button) => button.addEventListener("click", async () => {
    if (button.disabled) return;
    button.disabled = true;
    try { await logout(); location.replace("./institution-login.html"); }
    catch (error) {
      if (error.status === 401) { location.replace("./institution-login.html"); return; }
      button.disabled = false;
      let message = document.querySelector("[data-auth-logout-error]");
      if (!message) {
        message = document.createElement("p"); message.dataset.authLogoutError = ""; message.setAttribute("role", "alert");
        button.after(message);
      }
      message.textContent = "Não foi possível encerrar a sessão. Tente novamente.";
    }
  }));
}

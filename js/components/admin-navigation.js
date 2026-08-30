export function initAdminNavigation() {
  const sidebar = document.querySelector("[data-admin-sidebar]");

  const toggle = document.querySelector("[data-admin-sidebar-toggle]");

  const overlay = document.querySelector("[data-admin-sidebar-overlay]");

  if (!sidebar || !toggle || !overlay) {
    return;
  }

  const desktopBreakpoint = window.matchMedia("(min-width: 64.0625rem)");

  function openNavigation() {
    sidebar.classList.add("is-open");
    overlay.classList.add("is-visible");

    toggle.setAttribute("aria-expanded", "true");

    toggle.setAttribute("aria-label", "Fechar menu administrativo");

    document.body.classList.add("admin-navigation-open");
  }

  function closeNavigation() {
    sidebar.classList.remove("is-open");
    overlay.classList.remove("is-visible");

    toggle.setAttribute("aria-expanded", "false");

    toggle.setAttribute("aria-label", "Abrir menu administrativo");

    document.body.classList.remove("admin-navigation-open");
  }

  toggle.addEventListener("click", () => {
    const isOpen = toggle.getAttribute("aria-expanded") === "true";

    if (isOpen) {
      closeNavigation();
      return;
    }

    openNavigation();
  });

  overlay.addEventListener("click", closeNavigation);

  document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape") return;

    closeNavigation();
    toggle.focus();
  });

  desktopBreakpoint.addEventListener("change", (event) => {
    if (event.matches) {
      closeNavigation();
    }
  });
}

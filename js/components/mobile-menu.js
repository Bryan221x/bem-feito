export function initMobileMenu() {
  const menuToggle = document.querySelector("[data-menu-toggle]");
  const menu = document.querySelector("[data-menu]");

  if (!menuToggle || !menu) return;

  const mobileBreakpoint = window.matchMedia("(max-width: 48rem)");

  function closeMenu() {
    menu.classList.remove("is-open");

    menuToggle.setAttribute("aria-expanded", "false");
    menuToggle.setAttribute("aria-label", "Abrir menu");
  }

  function toggleMenu() {
    const isOpen = menuToggle.getAttribute("aria-expanded") === "true";

    menu.classList.toggle("is-open", !isOpen);

    menuToggle.setAttribute("aria-expanded", String(!isOpen));

    menuToggle.setAttribute(
      "aria-label",
      isOpen ? "Abrir menu" : "Fechar menu",
    );
  }

  menuToggle.addEventListener("click", toggleMenu);

  /*
   * Fecha o menu quando o usuário escolhe um link.
   */
  menu.addEventListener("click", (event) => {
    if (event.target.closest("a")) {
      closeMenu();
    }
  });

  /*
   * Fecha o menu ao clicar fora da navegação.
   */
  document.addEventListener("click", (event) => {
    const clickedToggle = menuToggle.contains(event.target);
    const clickedMenu = menu.contains(event.target);

    if (!clickedToggle && !clickedMenu) {
      closeMenu();
    }
  });

  /*
   * Permite fechar a navegação pelo teclado.
   */
  document.addEventListener("keydown", (event) => {
    const isOpen = menuToggle.getAttribute("aria-expanded") === "true";

    if (event.key !== "Escape" || !isOpen) return;

    closeMenu();
    menuToggle.focus();
  });

  /*
   * Remove o estado mobile aberto quando a página volta
   * para o layout desktop.
   */
  mobileBreakpoint.addEventListener("change", (event) => {
    if (!event.matches) {
      closeMenu();
    }
  });
}

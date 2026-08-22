export function initMobileMenu() {
    const menuToggle = document.querySelector("[data-menu-toggle]");
    const menu = document.querySelector("[data-menu]");

    if (!menuToggle || !menu) {
        return;
    }

    function closeMenu() {
        menu.classList.remove("is-open");

        menuToggle.setAttribute("aria-expanded", "false");
        menuToggle.setAttribute("aria-label", "Abrir menu");
    }

    function toggleMenu() {
        const isOpen =
            menuToggle.getAttribute("aria-expanded") === "true";

        menu.classList.toggle("is-open", !isOpen);
        menuToggle.setAttribute(
            "aria-expanded",
            String(!isOpen)
        );

        menuToggle.setAttribute(
            "aria-label",
            isOpen ? "Abrir menu" : "Fechar menu"
        );
    }

    menuToggle.addEventListener("click", toggleMenu);

    menu.addEventListener("click", (event) => {
        if (event.target.closest("a")) {
            closeMenu();
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            closeMenu();
        }
    });
}
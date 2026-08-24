import { initMobileMenu } from "./components/mobile-menu.js";
import { initCitiesPage } from "./pages/cities.js";
import { initHomePage } from "./pages/home.js";
import { initInstitutionsPage } from "./pages/institutions.js";

document.addEventListener("DOMContentLoaded", () => {
  initMobileMenu();

  initHomePage();
  initCitiesPage();
  initInstitutionsPage();
});

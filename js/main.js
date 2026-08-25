import { initMobileMenu } from "./components/mobile-menu.js";
import { initCitiesPage } from "./pages/cities.js";
import { initHomePage } from "./pages/home.js";
import { initHowItWorksPage } from "./pages/how-it-works.js";
import { initInstitutionPage } from "./pages/institution.js";
import { initInstitutionsPage } from "./pages/institutions.js";

document.addEventListener("DOMContentLoaded", () => {
  initMobileMenu();

  initHomePage();
  initHowItWorksPage();
  initCitiesPage();
  initInstitutionsPage();
  initInstitutionPage();
});
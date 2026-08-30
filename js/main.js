import { initInstitutionLoginPage } from "./pages/institution-login.js";
import { initMobileMenu } from "./components/mobile-menu.js";
import { initCitiesPage } from "./pages/cities.js";
import { initHomePage } from "./pages/home.js";
import { initHowItWorksPage } from "./pages/how-it-works.js";
import { initInstitutionPage } from "./pages/institution.js";
import { initInstitutionsPage } from "./pages/institutions.js";
import { initAboutPage } from "./pages/about.js";
import { initAdminNavigation } from "./components/admin-navigation.js";

document.addEventListener("DOMContentLoaded", () => {
  initMobileMenu();

  initHomePage();
  initHowItWorksPage();
  initCitiesPage();
  initInstitutionsPage();
  initInstitutionPage();
  initAboutPage();
  initInstitutionLoginPage();
  initAdminNavigation();
});

import { initInstitutionLoginPage } from "./pages/institution-login.js";
import { initRegistrationRequestPage } from "./pages/registration-request.js";
import { initInstitutionSelectionPage } from "./pages/institution-selection.js";
import { initAdminDashboardPage } from "./pages/admin-dashboard.js";
import { initMobileMenu } from "./components/mobile-menu.js";
import { initCitiesPage } from "./pages/cities.js";
import { initHomePage } from "./pages/home.js";
import { initHowItWorksPage } from "./pages/how-it-works.js";
import { initInstitutionPage } from "./pages/institution.js";
import { initInstitutionsPage } from "./pages/institutions.js";
import { initAboutPage } from "./pages/about.js";
import { initAdminNavigation } from "./components/admin-navigation.js";
import { initMyInstitutionPage } from "./pages/my-institution.js";
import { initUnitFormPage } from "./pages/unit-form.js";
import { initInstitutionAccessRequestPage } from "./pages/institution-access-request.js";
import { initAdminInstitutionsPage } from "./pages/admin-institutions.js";
import { initAdminRequestsPage } from "./pages/admin-requests.js";

document.addEventListener("DOMContentLoaded", () => {
  initMobileMenu();

  initHomePage();
  initHowItWorksPage();
  initCitiesPage();
  initInstitutionsPage();
  initInstitutionPage();
  initAboutPage();
  initInstitutionLoginPage();
  initRegistrationRequestPage();
  initInstitutionSelectionPage();
  initAdminDashboardPage();
  initMyInstitutionPage();
  initUnitFormPage();
  initInstitutionAccessRequestPage();
  initAdminInstitutionsPage();
  initAdminRequestsPage();
  initAdminNavigation();
});

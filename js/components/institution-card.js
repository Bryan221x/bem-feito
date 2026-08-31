/*
 * Estrutura esperada para uma instituição recebida pela API.
 * O Front-End não define os dados: apenas renderiza o que vier do sistema.
 *
 * @typedef {Object} Institution
 * @property {string|number} id
 * @property {string} displayName
 * @property {string} [category]
 * @property {string} [description]
 */

/**
 * Cria o card visual de uma instituição cadastrada.
 *
 * @param {Institution} institution
 * @param {string|null} cityId
 * @returns {HTMLElement}
 */
export function createInstitutionCard(institution, cityId = null) {
  const displayName = String(
    institution?.displayName || institution?.name || "",
  ).trim();

  if (!institution?.id || !displayName) {
    throw new Error("A instituição precisa possuir identificador e nome.");
  }

  const card = document.createElement("article");
  card.className = "institution-card";
  card.dataset.institutionId = String(institution.id);

  const content = document.createElement("div");
  content.className = "institution-card__content";

  if (institution.category) {
    const category = document.createElement("span");
    category.className = "institution-card__category";
    category.textContent = institution.category;

    content.appendChild(category);
  }

  const title = document.createElement("h2");
  title.className = "institution-card__title";
  title.textContent = displayName;

  content.appendChild(title);

  if (institution.description) {
    const description = document.createElement("p");
    description.className = "institution-card__description";
    description.textContent = institution.description;

    content.appendChild(description);
  }

  const params = new URLSearchParams();

  if (cityId) {
    params.set("cityId", cityId);
  }

  params.set("institutionId", String(institution.id));

  const link = document.createElement("a");
  link.className = "button button--outline institution-card__link";

  link.href = `./institution.html?${params.toString()}`;
  link.textContent = "Ver instituição";

  content.appendChild(link);
  card.appendChild(content);

  return card;
}

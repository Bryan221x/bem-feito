/*
 * Estrutura esperada para uma instituição recebida pela API.
 * O Front-End não define os dados: apenas renderiza o que vier do sistema.
 *
 * @typedef {Object} Institution
 * @property {string|number} id
 * @property {string} name
 * @property {string} [category]
 * @property {string} [description]
 */

/**
 * Cria o card visual de uma instituição cadastrada.
 *
 * @param {Institution} institution
 * @returns {HTMLElement}
 */
export function createInstitutionCard(institution) {
  if (!institution?.id || !institution?.name) {
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
  title.textContent = institution.name;

  content.appendChild(title);

  if (institution.description) {
    const description = document.createElement("p");
    description.className = "institution-card__description";
    description.textContent = institution.description;

    content.appendChild(description);
  }

  const link = document.createElement("a");
  link.className = "button button--outline institution-card__link";
  link.href = `./institution.html?institutionId=${encodeURIComponent(
    institution.id,
  )}`;
  link.textContent = "Ver instituição";

  content.appendChild(link);
  card.appendChild(content);

  return card;
}

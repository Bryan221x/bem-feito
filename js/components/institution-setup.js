export function setupSteps(institution, { admin = false, permissions = [] } = {}) {
  const units = institution.units || [], main = units.find(unit => unit.isMain) || units[0];
  const edit = main ? `./unit-form.html?institutionId=${institution.id}&unitId=${main.id}` : `./unit-form.html?institutionId=${institution.id}`;
  const canUnit = !!main || admin || permissions.includes("CREATE_UNIT");
  return [
    { title: "Revisar informações gerais", done: !!(institution.displayName && institution.description && institution.objective), href: "#general-data-title" },
    { title: "Conferir contatos públicos", done: (institution.contacts || []).some(c => c.isPublic), href: "#contacts-title" },
    { title: "Cadastrar ou revisar a unidade principal", done: !!main, href: canUnit ? edit : "#units" },
    { title: "Informar horários de atendimento", done: !!units.length && units.every(u => u.openingHours?.length), href: canUnit ? edit + "#hours" : "#units" },
    { title: "Adicionar necessidades", done: units.some(u => u.needs?.some(n => n.status === "ACTIVE")), href: canUnit ? edit + "#needs" : "#units" },
    { title: "Adicionar imagens", done: !!institution.historyImages?.length, href: "#images" },
  ];
}
export function renderInstitutionSetup(page, institution, access) {
  const host = page.querySelector("[data-institution-setup]"); if (!host) return;
  const steps = setupSteps(institution, access), complete = steps.filter(s => s.done).length;
  const heading = document.createElement("h2"); heading.textContent = "Configuração da instituição";
  const message = document.createElement("p"); message.textContent = complete === steps.length ? "Seu perfil possui as informações essenciais. Mantenha-as atualizadas." : `${complete} de ${steps.length} sugestões atendidas. Complete no seu ritmo; os dados já cadastrados foram considerados.`;
  const list = document.createElement("ul");
  for (const step of steps) { const item = document.createElement("li"), link = document.createElement("a"); link.href = step.href; link.textContent = `${step.done ? "✓" : "○"} ${step.title}`; item.append(link); list.append(item); }
  host.replaceChildren(heading, message); if (complete !== steps.length) host.append(list);
  host.hidden = false;
}

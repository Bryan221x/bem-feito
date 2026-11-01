const priorityOrder = { HIGH: 0, MEDIUM: 1, LOW: 2 };
const names = new Intl.Collator("pt-BR", { sensitivity: "base", numeric: true });

// Sort a copy: presentation must never mutate the saved records or mix units.
export function sortNeeds(needs) {
  return [...needs].sort((a, b) =>
    (priorityOrder[a.priority] ?? 3) - (priorityOrder[b.priority] ?? 3) ||
    names.compare(a.name || a.title || "", b.name || b.title || "") ||
    String(a.id ?? "").localeCompare(String(b.id ?? ""), "en", { numeric: true })
  );
}

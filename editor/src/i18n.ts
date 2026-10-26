import { createModuleI18n } from "@artisan/editor-sdk";

const en = {
  kindLabel: "Store categories",
  collection: "Catalog",
  category: "Category",
  name: "Name",
  icon: "Icon",
  iconHint: "What the main menu shows for this category.",
  discount: "Discount (%)",
  categoryDiscountHint: "Taken off every article of the category, added to the store's and the article's.",
  order: "Position in the store",
  orderHint: "Lower first; empty: after the others, by id.",
  items: "Articles",
  addItem: "New article",
  newItemName: "New article",
  noItems: "No article yet.",
  open: "Edit",
  remove: "Remove",
  back: "Back to the category",
  item: "Article",
  id: "Id",
  idHint: "Names the article in purchases: unique across the store.",
  idTaken: "Another article of the store has this id: a purchase could buy the wrong one.",
  itemIcon: "Icon",
  itemIconHint: "What the player sees in the category's menu: item, name, lore.",
  price: "Price",
  itemDiscountHint: "Taken off this article only.",
  paid: "The player pays {{price}}",
  actions: "After payment",
  actionsHint: "What the purchase gives: commands, items, messages…",
  kept: "Kept as is: {{fields}}",
  itemCount: "{{count}} articles",
  itemCountOne: "{{count}} article",
  hoverPrice: "Price: {{price}}",
  hoverPaid: "Paid: {{price}} (-{{percent}}%)",
};

type Key = keyof typeof en;

const fr: Record<Key, string> = {
  kindLabel: "Catégories de boutique",
  collection: "Catalogue",
  category: "Catégorie",
  name: "Nom",
  icon: "Icône",
  iconHint: "Ce que le menu principal affiche pour cette catégorie.",
  discount: "Remise (%)",
  categoryDiscountHint:
    "Retirée de chaque article de la catégorie, en plus de celle de la boutique et de l'article.",
  order: "Position dans la boutique",
  orderHint: "Les plus petites d'abord ; vide : après les autres, par id.",
  items: "Articles",
  addItem: "Nouvel article",
  newItemName: "Nouvel article",
  noItems: "Aucun article pour l'instant.",
  open: "Modifier",
  remove: "Supprimer",
  back: "Retour à la catégorie",
  item: "Article",
  id: "Id",
  idHint: "Nomme l'article dans les achats : unique dans toute la boutique.",
  idTaken: "Un autre article de la boutique a déjà cet id : un achat pourrait acheter le mauvais.",
  itemIcon: "Icône",
  itemIconHint: "Ce que voit le joueur dans le menu de la catégorie : item, nom, lore.",
  price: "Prix",
  itemDiscountHint: "Retirée de cet article seulement.",
  paid: "Le joueur paie {{price}}",
  actions: "Après le paiement",
  actionsHint: "Ce que l'achat donne : commandes, items, messages…",
  kept: "Conservé tel quel : {{fields}}",
  itemCount: "{{count}} articles",
  itemCountOne: "{{count}} article",
  hoverPrice: "Prix : {{price}}",
  hoverPaid: "Payé : {{price}} (-{{percent}} %)",
};

export const LABELS = { en: en.kindLabel, fr: fr.kindLabel };
export const COLLECTION_LABELS = { en: en.collection, fr: fr.collection };

export const useT = createModuleI18n<Key>({ en, fr });
export type T = ReturnType<typeof useT>;

/** The same strings outside a component (the board's callbacks), in `lang`. */
export function translator(lang: string): T {
  const dict: Partial<Record<Key, string>> = lang === "fr" ? fr : en;
  return (key, vars) => {
    const raw = dict[key] ?? en[key] ?? key;
    return vars ? raw.replace(/\{\{(\w+)\}\}/g, (all, name) => String(vars[name] ?? all)) : raw;
  };
}

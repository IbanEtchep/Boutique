import { textValue } from "@artisan/editor-sdk";
import { shopSettings, wholeShopDiscount } from "./board";
import { type Category, KIND, asNumber, finalPrice } from "./model";

interface ProjectLike {
  documents?: Record<string, unknown[]>;
  defaultLang?: string;
}

function formatPrice(price: number, paid: number, settings: Record<string, unknown>): string {
  const formats = settings.placeholders as Record<string, unknown> | undefined;
  const discounted = paid < Math.round(price);
  const configured = formats?.[discounted ? "discount_price_display" : "price_display"];
  const template =
    typeof configured === "string" && configured.trim()
      ? configured
      : discounted
        ? "&m%old_price% &f%price% tokens (-%discount_percent%%)"
        : "%price% tokens";
  const base = Math.round(price);
  return template
    .replaceAll("%old_price%", String(base))
    .replaceAll("%discount_percent%", String(base <= 0 ? 0 : Math.round(((base - paid) * 100) / base)))
    .replaceAll("%price%", String(paid));
}

/** The categories in the shop's order: `order` first (lower first), then by id. */
export function orderedCategories(categories: Category[]): Category[] {
  return [...categories].sort((a, b) => {
    const ra = a.order ?? Number.POSITIVE_INFINITY;
    const rb = b.order ?? Number.POSITIVE_INFINITY;
    return ra !== rb ? ra - rb : a.id.localeCompare(b.id);
  });
}

/**
 * Authoring-time rows of the shop's data sources, from the folder's `boutique_category`
 * Documents: the board and the library show the catalogue being edited before any deploy,
 * and without a live server. Row shapes and the price formula MUST mirror the plugin's
 * `BoutiqueModule.categoryRow` / `itemRow`. Null for a foreign ref or a project without
 * categories (the host then falls back to the live server snapshot).
 */
export function boutiqueLocalRows(project: unknown, ref: string): Record<string, unknown>[] | null {
  if (ref !== "boutique:categories" && ref !== "boutique:items") return null;
  const docs = (project as ProjectLike).documents?.[KIND] as Category[] | undefined;
  if (!docs || docs.length === 0) return null;
  const lang = (project as ProjectLike).defaultLang ?? "fr";
  const settings = shopSettings(project);
  const whole = wholeShopDiscount(project);
  const categories = orderedCategories(docs);

  const itemsOf = (c: Category) =>
    c.items.map((i) => {
      const price = asNumber(i.price);
      const paid = finalPrice(price, whole, c.discount, i.discount);
      return {
        id: i.id,
        name: textValue(project, i.name.key, lang),
        icon: i.icon,
        category: c.id,
        price,
        final_price: paid,
        price_display: formatPrice(price, paid, settings),
        lore: "",
      };
    });

  if (ref === "boutique:items") return categories.flatMap(itemsOf);
  return categories.map((c) => ({
    id: c.id,
    name: textValue(project, c.name.key, lang),
    icon: c.icon,
    items: itemsOf(c),
  }));
}

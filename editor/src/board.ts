import type { BoardItem } from "@artisan/editor-sdk";
import { textValue } from "@artisan/editor-sdk";
import { translator } from "./i18n";
import { type Category, asMap, asNumber, finalPrice, itemRowIds } from "./model";

/** The node's colour: the shop's gold. */
export const SHOP_ACCENT = "#d97706";

interface ProjectLike {
  dataSources?: { id?: unknown; shape?: unknown; value?: unknown }[];
  documents?: Record<string, unknown[]>;
  defaultLang?: string;
}

/** The shop's settings (`data/shop_settings.yaml`), as the folder's draft holds them. */
export function shopSettings(project: unknown): Record<string, unknown> {
  const source = (project as ProjectLike | undefined)?.dataSources?.find(
    (d) => String(d.id) === "shop_settings",
  );
  return asMap(source?.value);
}

/** The discount every article gets (Settings), in percent. */
export function wholeShopDiscount(project: unknown): number {
  return asNumber(shopSettings(project).whole_shop_discount);
}

/** A name as the board shows it: colour codes and MiniMessage tags left out. */
export function plainText(text: string): string {
  return text
    .replace(/<[^<>]*>/g, "")
    .replace(/[&§]#[0-9a-fA-F]{6}/g, "")
    .replace(/#[0-9a-fA-F]{6}/g, "")
    .replace(/[&§][0-9a-fk-orx]/gi, "")
    .trim();
}

/** "450 (-10%)" when a discount applies, else "500". */
export function priceLabel(price: number, paid: number): string {
  const base = Math.round(price);
  if (paid >= base) return String(base);
  const percent = base <= 0 ? 0 : Math.round(((base - paid) * 100) / base);
  return `${paid} (-${percent}%)`;
}

/** One row per article: its icon, its name, what it costs once discounted. */
export function categoryBoardItems(
  category: Category,
  project: unknown,
  lang: string,
): BoardItem[] {
  const t = translator(lang);
  const whole = wholeShopDiscount(project);
  const ids = itemRowIds(category);
  return category.items.map((item, i) => {
    const id = ids[i]!;
    const name = plainText(textValue(project, item.name.key, lang)) || item.id;
    const paid = finalPrice(item.price, whole, category.discount, item.discount);
    const lines = [t("hoverPrice", { price: String(Math.round(item.price)) })];
    const off = whole + category.discount + item.discount;
    if (off > 0) lines.push(t("hoverPaid", { price: String(paid), percent: String(off) }));
    return {
      id,
      label: name,
      detail: priceLabel(item.price, paid),
      icon: item.icon,
      hover: { title: name, lines },
    };
  });
}

/** The node's second line: "3 articles · -10%". */
export function categoryBoardSummary(category: Category, lang: string): string {
  const t = translator(lang);
  const count = t(category.items.length === 1 ? "itemCountOne" : "itemCount", {
    count: String(category.items.length),
  });
  return category.discount > 0 ? `${count} · -${category.discount}%` : count;
}

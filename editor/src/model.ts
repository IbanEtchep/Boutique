import type {
  DocumentParseContext,
  DocumentParseResult,
  LTextSiteContribution,
} from "@artisan/editor-sdk";
import { fieldText, newTextKey } from "@artisan/editor-sdk";

/**
 * A `boutique_category/v1` Document in the editor: one file per category of the shop
 * (`catalogue/<id>.yml` in the module's folder), holding its articles. Names are
 * text nodes (`{key}`, the host's `LText`), the other fields typed, and what the editor does
 * not know is kept verbatim in `rest` so a round trip loses nothing.
 *
 * The plugin's reader (`ShopRepo.loadCategories`) is the reference: what it ignores, the
 * editor never needs to write.
 */

export const KIND = "boutique_category";
export const DIR = "catalogue";
export const SCHEMA = "boutique_category/v1";
/** The module's folder: the only project where categories are read, hence created. */
export const PROJECT = "boutique_addon";

export type Raw = Record<string, unknown>;
export interface Text {
  key: string;
}

export interface ShopItem {
  id: string;
  name: Text;
  /** An item value: a material, `item:<captured id>`, or `{extends, title, lore…}`. */
  icon: unknown;
  price: number;
  /** Percent off this article, added to the category's and the shop's. */
  discount: number;
  /** What runs after payment: a keyed action chain, or a legacy Action DSL string. */
  actions: unknown;
  rest: Raw;
}

export interface Category {
  id: string;
  projectId: string;
  name: Text;
  icon: unknown;
  discount: number;
  /** Rank in the shop's main menu, lower first; categories without one follow, by id. */
  order: number | undefined;
  items: ShopItem[];
  rest: Raw;
}

// ------------------------------------------------------------------ helpers

export function asMap(value: unknown): Raw {
  return value && typeof value === "object" && !Array.isArray(value) ? { ...(value as Raw) } : {};
}

function asList(value: unknown): unknown[] {
  return Array.isArray(value) ? value : [];
}

export function asString(value: unknown): string {
  return typeof value === "string" || typeof value === "number" || typeof value === "boolean"
    ? String(value)
    : "";
}

export function asNumber(value: unknown, fallback = 0): number {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value.trim() !== "" && Number.isFinite(Number(value)))
    return Number(value);
  return fallback;
}

function without(map: Raw, ...keys: string[]): Raw {
  const out = { ...map };
  for (const k of keys) delete out[k];
  return out;
}

/** A key as it is typed: lowercase, anything outside `[a-z0-9_]` becomes `_`. */
export function typedKey(input: string): string {
  return input.toLowerCase().replace(/[^a-z0-9_]/g, "_");
}

/** `base`, else `base_2`, `base_3`… : the first not in `taken`. */
export function freeKey(base: string, taken: Iterable<string>): string {
  const used = new Set(taken);
  if (!used.has(base)) return base;
  for (let n = 2; ; n++) if (!used.has(`${base}_${n}`)) return `${base}_${n}`;
}

/** The price a player pays: every discount added, never below zero, rounded like the plugin. */
export function finalPrice(price: number, ...discounts: number[]): number {
  const off = discounts.reduce((sum, d) => sum + d, 0);
  return Math.round(Math.max(0, price * (1 - off / 100)));
}

// ------------------------------------------------------------------ parse

export function parseCategory(src: Raw, ctx: DocumentParseContext): DocumentParseResult {
  const i18nKeys: Record<string, Record<string, string>> = {};
  const unsupported: string[] = [];

  // Same three shapes as every Artisan text site (ADR `field-local-translations`).
  function text(raw: unknown): Text {
    if (raw && typeof raw === "object" && !Array.isArray(raw)) {
      const key = newTextKey();
      const langs: Record<string, string> = {};
      for (const [lang, v] of Object.entries(raw as Raw)) langs[lang] = asString(v);
      i18nKeys[key] = langs;
      return { key };
    }
    const ref = typeof raw === "string" ? /^\{i18n:(.+)\}$/.exec(raw) : null;
    if (ref) {
      const tr = ctx.translations?.[ref[1]!];
      if (tr) i18nKeys[ref[1]!] = { ...tr };
      return { key: ref[1]! };
    }
    const key = newTextKey();
    i18nKeys[key] = raw == null || raw === "" ? {} : { [ctx.lang]: asString(raw) };
    return { key };
  }

  const items = asList(src.items).map((entry, i): ShopItem => {
    const it = asMap(entry);
    return {
      id: asString(it.id) || `item_${i + 1}`,
      name: text(it.name),
      icon: it.icon ?? "",
      price: asNumber(it.price),
      discount: asNumber(it.discount),
      actions: it.actions ?? null,
      rest: without(it, "id", "name", "icon", "price", "discount", "actions"),
    };
  });

  const order = src.order == null ? undefined : asNumber(src.order, Number.NaN);
  const doc: Category = {
    id: asString(src.id),
    projectId: ctx.projectId,
    name: text(src.name),
    icon: src.icon ?? "",
    discount: asNumber(src.discount),
    order: order === undefined || Number.isNaN(order) ? undefined : order,
    items,
    rest: without(src, "id", "schema", "name", "icon", "discount", "order", "items"),
  };
  return { doc, i18nKeys, unsupported };
}

// ------------------------------------------------------------------ serialize

function isEmptyChain(actions: unknown): boolean {
  if (actions == null) return true;
  if (typeof actions === "string") return actions.trim() === "";
  return Array.isArray(actions) && actions.length === 0;
}

export function serializeCategory(category: Category, project: unknown): Raw {
  const t = (node: Text) => fieldText(project, node.key);
  const out: Raw = {
    id: category.id,
    schema: SCHEMA,
    name: t(category.name),
    icon: category.icon === "" ? undefined : category.icon,
    discount: category.discount,
    order: category.order,
    ...category.rest,
    items: category.items.map((item) => {
      const row: Raw = {
        id: item.id,
        name: t(item.name),
        icon: item.icon === "" ? undefined : item.icon,
        price: item.price,
        discount: item.discount === 0 ? undefined : item.discount,
        ...item.rest,
        actions: isEmptyChain(item.actions) ? undefined : item.actions,
      };
      for (const key of Object.keys(row)) if (row[key] === undefined) delete row[key];
      return row;
    }),
  };
  for (const key of Object.keys(out)) if (out[key] === undefined) delete out[key];
  return out;
}

// ------------------------------------------------------------------ text sites

/** Each text node and its canonical key. Articles are addressed by id: a reorder moves nothing. */
export function* categoryTextSites(category: Category): Iterable<LTextSiteContribution> {
  const base = `${DIR}.${category.id}`;
  yield { lText: category.name, key: `${base}.name`, path: "name", kind: "boutique_category.name" };
  const seen = new Set<string>();
  for (let i = 0; i < category.items.length; i++) {
    const item = category.items[i]!;
    const id = seen.has(item.id) ? `${item.id}_${i}` : item.id;
    seen.add(item.id);
    yield {
      lText: item.name,
      key: `${base}.items.${id}.name`,
      path: `items[${i}].name`,
      kind: "boutique_item.name",
    };
  }
}

// ------------------------------------------------------------------ icons

/**
 * The icon as the editor's item form edits it (`ItemInput`: `material` plus the menu
 * appearance fields). The file keeps the catalogue's form: a bare material, or the base item
 * under `extends` with the presentation beside it.
 */
export function iconToSpec(icon: unknown, lang: string): Raw {
  if (typeof icon === "string") return icon ? { material: icon } : {};
  const map = asMap(icon);
  const material = map.extends ?? map.item ?? map.material;
  const out: Raw = without(map, "extends", "item", "material");
  if (material !== undefined) out.material = typeof material === "string" ? material : "";
  // A text per language (a migrated multi-language catalogue): the form edits one.
  for (const field of ["title", "lore"]) {
    const value = out[field];
    if (value && typeof value === "object" && !Array.isArray(value)) {
      const langs = value as Raw;
      out[field] = langs[lang] ?? Object.values(langs)[0];
    }
  }
  return out;
}

export function specToIcon(spec: Raw): unknown {
  const material = typeof spec.material === "string" ? spec.material : "";
  const rest = without(spec, "material");
  for (const key of Object.keys(rest)) {
    const v = rest[key];
    if (v === undefined || v === "" || (Array.isArray(v) && v.length === 0)) delete rest[key];
  }
  if (Object.keys(rest).length === 0) return material;
  return { extends: material, ...rest };
}

/** The material of an icon, for a short label ("DIAMOND_SWORD"). */
export function iconMaterial(icon: unknown): string {
  if (typeof icon === "string") return icon;
  const map = asMap(icon);
  const base = map.extends ?? map.item ?? map.material;
  return base === undefined ? "" : iconMaterial(base);
}

/**
 * The id of each article's row on the board (`BoardItem.id`, what the panel receives as
 * `item`): its id, made unique by its rank while two articles share one.
 */
export function itemRowIds(category: Category): string[] {
  const seen = new Set<string>();
  return category.items.map((item, i) => {
    const id = seen.has(item.id) ? `${item.id}#${i}` : item.id;
    seen.add(item.id);
    return id;
  });
}

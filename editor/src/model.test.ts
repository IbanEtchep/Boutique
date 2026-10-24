import { type EditorHost, installEditorHost } from "@artisan/editor-sdk";
import { beforeEach, describe, expect, it } from "vitest";
import { categoryBoardItems, categoryBoardSummary, plainText, priceLabel } from "./board";
import { boutiqueLocalRows, orderedCategories } from "./localRows";
import {
  type Category,
  finalPrice,
  iconToSpec,
  itemRowIds,
  parseCategory,
  serializeCategory,
  specToIcon,
} from "./model";

/** A project as the host holds it: texts by key and language, the folder's sources and Documents. */
interface FakeProject {
  i18nKeys: Record<string, Record<string, string>>;
  defaultLang: string;
  dataSources: { id: string; shape: string; value: unknown }[];
  documents: Record<string, unknown[]>;
}

let counter = 0;
beforeEach(() => {
  counter = 0;
  installEditorHost({
    newTextKey: () => `k${++counter}`,
    text: (project: unknown, key: string, lang: string) => {
      const p = project as FakeProject;
      return p.i18nKeys[key]?.[lang] ?? p.i18nKeys[key]?.[p.defaultLang] ?? "";
    },
    fieldText: (project: unknown, key: string) => {
      const langs = (project as FakeProject).i18nKeys[key] ?? {};
      const entries = Object.entries(langs);
      return entries.length === 1 && entries[0]![0] === "fr" ? entries[0]![1] : langs;
    },
  } as unknown as EditorHost);
});

const FILE = {
  id: "les_cles",
  schema: "boutique_category/v1",
  name: "Les Clés",
  icon: "TRIPWIRE_HOOK",
  discount: 10,
  order: 2,
  items: [
    {
      id: "cle_legendaire",
      name: { fr: "Clé Légendaire", en: "Legendary key" },
      icon: { extends: "TRIPWIRE_HOOK", lore: [">> 1 clé"] },
      price: 500,
      actions: 'run_command "crate key give {player} legendary 1" as console',
      stock: 3,
    },
  ],
  note: "kept",
};

function parse(src: Record<string, unknown> = FILE) {
  const res = parseCategory(src, { projectId: "boutique_addon", lang: "fr" });
  const project: FakeProject = {
    i18nKeys: res.i18nKeys,
    defaultLang: "fr",
    dataSources: [{ id: "shop_settings", shape: "config", value: { whole_shop_discount: 5 } }],
    documents: { boutique_category: [res.doc] },
  };
  return { category: res.doc as Category, project };
}

describe("boutique_category file", () => {
  it("round-trips a category, its texts and what the editor does not know", () => {
    const { category, project } = parse();
    expect(category.items[0]!.price).toBe(500);
    expect(serializeCategory(category, project)).toEqual({
      id: "les_cles",
      schema: "boutique_category/v1",
      name: "Les Clés",
      icon: "TRIPWIRE_HOOK",
      discount: 10,
      order: 2,
      note: "kept",
      items: [
        {
          id: "cle_legendaire",
          name: { fr: "Clé Légendaire", en: "Legendary key" },
          icon: { extends: "TRIPWIRE_HOOK", lore: [">> 1 clé"] },
          price: 500,
          stock: 3,
          actions: 'run_command "crate key give {player} legendary 1" as console',
        },
      ],
    });
  });

  it("leaves out an empty chain, a zero article discount and a missing position", () => {
    const { category, project } = parse({ id: "x", name: "X", items: [{ id: "a", name: "A", price: 1, discount: 0, actions: [] }] });
    const out = serializeCategory(category, project);
    expect(out).not.toHaveProperty("order");
    expect((out.items as Record<string, unknown>[])[0]).toEqual({ id: "a", name: "A", price: 1 });
  });
});

describe("icons", () => {
  it("edits a bare material and an extended item through the item form, and writes them back", () => {
    expect(iconToSpec("DIAMOND", "fr")).toEqual({ material: "DIAMOND" });
    expect(specToIcon({ material: "DIAMOND" })).toBe("DIAMOND");
    const spec = iconToSpec({ extends: "item:7", title: { fr: "Titre", en: "Title" } }, "en");
    expect(spec).toEqual({ material: "item:7", title: "Title" });
    expect(specToIcon({ material: "item:7", lore: ["a"], title: "" })).toEqual({
      extends: "item:7",
      lore: ["a"],
    });
  });
});

describe("prices", () => {
  it("adds every discount like the plugin, and labels the paid price", () => {
    expect(finalPrice(500, 5, 10, 0)).toBe(425);
    expect(finalPrice(10, 80, 30)).toBe(0);
    expect(priceLabel(500, 425)).toBe("425 (-15%)");
    expect(priceLabel(500, 500)).toBe("500");
  });
});

describe("board node", () => {
  it("lists each article with its icon, name and discounted price", () => {
    const { category, project } = parse();
    const [row] = categoryBoardItems(category, project, "fr");
    expect(row).toMatchObject({
      id: "cle_legendaire",
      label: "Clé Légendaire",
      detail: "425 (-15%)",
      icon: { extends: "TRIPWIRE_HOOK", lore: [">> 1 clé"] },
    });
    expect(categoryBoardSummary(category, "fr")).toBe("1 article · -10%");
  });

  it("keeps row ids unique while two articles share an id", () => {
    const { category } = parse({ id: "x", items: [{ id: "a" }, { id: "a" }] });
    expect(itemRowIds(category)).toEqual(["a", "a#1"]);
  });

  it("shows a name without its colour codes", () => {
    expect(plainText("&6&lClé <bold>rare</bold> #f0932bok")).toBe("Clé rare ok");
  });
});

describe("live rows", () => {
  it("feeds the shop's sources from the folder's categories, in the shop's order", () => {
    const { project } = parse();
    const other = parseCategory({ id: "a_first", name: "Premier", order: 1, items: [] }, {
      projectId: "boutique_addon",
      lang: "fr",
    });
    Object.assign(project.i18nKeys, other.i18nKeys);
    project.documents.boutique_category!.push(other.doc);
    const cats = boutiqueLocalRows(project, "boutique:categories")!;
    expect(cats.map((c) => c.id)).toEqual(["a_first", "les_cles"]);
    const items = boutiqueLocalRows(project, "boutique:items")!;
    expect(items[0]).toMatchObject({
      id: "cle_legendaire",
      name: "Clé Légendaire",
      category: "les_cles",
      final_price: 425,
      price_display: "&m500 &f425 tokens (-15%)",
    });
    expect(boutiqueLocalRows(project, "other:ref")).toBeNull();
    expect(orderedCategories([])).toEqual([]);
  });
});

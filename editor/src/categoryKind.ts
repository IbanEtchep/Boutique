import type { DocumentKindContribution } from "@artisan/editor-sdk";
import { textValue } from "@artisan/editor-sdk";
import { CategoryEditor } from "./CategoryEditor";
import { SHOP_ACCENT, categoryBoardItems, categoryBoardSummary, plainText } from "./board";
import { COLLECTION_LABELS, LABELS } from "./i18n";
import {
  type Category,
  DIR,
  KIND,
  PROJECT,
  SCHEMA,
  categoryTextSites,
  parseCategory,
  serializeCategory,
} from "./model";

/**
 * The `boutique_category` kind: one file per category of the shop, holding its articles.
 * Reserved to the module's folder, the only one the plugin reads its catalogue from.
 */
export const categoryKind: DocumentKindContribution = {
  kind: KIND,
  dir: DIR,
  schemaVersion: SCHEMA,
  // A category's id names it in the menus (`category` of shop_category): typed by hand.
  projects: [PROJECT],
  // The catalogue (SDK 1.9): one entry and one page, off the board, its settings on top.
  // The categories stay in boutique_categories/ at the folder's root, the only place read.
  collection: { labels: COLLECTION_LABELS, screen: "boutique:settings" },
  label: LABELS.en,
  labels: LABELS,
  component: CategoryEditor,
  title: (doc, { project, lang }) =>
    plainText(textValue(project, (doc as Category).name.key, lang)),
  create: ({ id, projectId }) =>
    ({
      id,
      projectId,
      // An empty key: the host mints it at its canonical address.
      name: { key: "" },
      icon: "CHEST",
      discount: 0,
      order: undefined,
      items: [],
      rest: {},
    }) satisfies Category,
  parse: (src, ctx) => parseCategory(src, ctx),
  serialize: (doc, project) => serializeCategory(doc as Category, project),
  ltextSites: (doc) => categoryTextSites(doc as Category),
  board: {
    size: { width: 260, height: 48 },
    accent: SHOP_ACCENT,
    items: (doc, { project, lang }) => categoryBoardItems(doc as Category, project, lang),
    summary: (doc, { lang }) => categoryBoardSummary(doc as Category, lang),
  },
};

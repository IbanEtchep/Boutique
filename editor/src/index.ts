import { type FrontendModule, SDK_VERSION } from "@artisan/editor-sdk";
import { categoryKind } from "./categoryKind";
import { boutiqueLocalRows } from "./localRows";

/**
 * The editor view of Boutique (presence id `boutique`): the `boutique_category` kind and the
 * live rows of its data sources. Built as a bundle (`npm run build`) that the plugin's jar
 * serves, so the view always matches the installed plugin.
 */
const boutiqueModule: FrontendModule = {
  id: "boutique",
  displayName: "Boutique",
  schemaVersion: "boutique/v1",
  sdk: SDK_VERSION,
  sidebar: { icon: () => null, label: "Boutique", to: "/m/boutique" },
  // Categories are Documents of the folder; the settings are a screen the plugin declares.
  routes: [],
  documents: [categoryKind],
  localPreviewRows: boutiqueLocalRows,
  dataSourceModels: {
    "boutique:items": "boutique:item",
    "boutique:categories": "boutique:category",
  },
};

export default boutiqueModule;

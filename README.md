
### Module folder

Boutique needs Artisan with the add-on API 2.8.0 or newer. Its menus, dialogs, commands, texts, catalogue and settings live in `plugins/Artisan/modules/boutique_addon/`, the module's folder, listed under the Boutique module in the editor. The folder keeps the former project id, so Versions, drafts and qualified references (`boutique_addon:…`) carry over. On the first start with this version, Artisan moves an existing `plugins/Boutique/editor/` there once; while both folders hold files, the old one stays in use and the log says so. The `project_id` line older versions wrote in `manifest.yaml` is removed at startup. Migration archives (`shop.yaml.migrated`, `catalog-icon-backup/`) stay in `plugins/Boutique/`.

### Catalogue icon lore

Product presentation belongs to `icon` (title, lore, quantity and attributes). The item model no longer declares a separate product `lore`. At startup, `CatalogIconMigrator` moves existing row lore into an icon extension before Artisan loads the project. The captured item lore is preserved without a placeholder: Artisan keeps the base item lore and renders the icon lines below it. Original files are backed up in `plugins/Boutique/catalog-icon-backup/`, outside the editor project; each replacement is atomic and the migration is idempotent. Canonical translations are copied to the new icon addresses; old keys remain available to legacy references. Generated templates inherit the icon lore and append the price.

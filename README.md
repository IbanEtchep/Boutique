
### Module folder

Boutique needs Artisan with the add-on API 2.12.0 or newer. Build with `./gradlew build` (tests, then `build/libs/Boutique-<version>.jar` with HikariCP shaded in); the API version is `artisanApiVersion` in `gradle.properties`. Its menus, dialogs, texts, catalogue and settings live in `plugins/Artisan/modules/boutique_addon/`, the module's folder, listed under the Boutique module in the editor. The folder keeps the former project id, so Versions, drafts and qualified references (`boutique_addon:…`) carry over. On the first start with this version, Artisan moves an existing `plugins/Boutique/editor/` there once; while both folders hold files, the old one stays in use and the log says so. The `project_id` line older versions wrote in `manifest.yaml` is removed at startup. Migration archives (`shop.yaml.migrated`, `catalog-icon-backup/`) stay in `plugins/Boutique/`.

### Catalogue icon lore

Product presentation belongs to `icon` (title, lore, quantity and attributes). The item model no longer declares a separate product `lore`. At startup, `CatalogIconMigrator` moves existing row lore into an icon extension before Artisan loads the project. The captured item lore is preserved without a placeholder: Artisan keeps the base item lore and renders the icon lines below it. Original files are backed up in `plugins/Boutique/catalog-icon-backup/`, outside the editor project; each replacement is atomic and the migration is idempotent. Canonical translations are copied to the new icon addresses; old keys remain available to legacy references. Generated templates inherit the icon lore and append the price.

### The `/boutique` command

`/boutique` is a command of the module (`boutique:open`, API 2.11.0): it opens `boutique_addon:shop_main`, and is always reachable as `/boutique:boutique`. Its name, aliases, permission, description and behaviour change from its page in the editor's System section (a `command_override/v1` file); the editor shows it on the folder's board and in its command list. At startup, the `commands/boutique.yaml` that older versions wrote is removed with its `manifest.yaml` entry while it is still the shipped one. A changed one is kept and keeps the name `/boutique`, and the log says how to move its changes to the override.

The menus and the dialog the code opens by id (`menus/shop_main.yaml`, `menus/shop_category.yaml`, `dialogs/confirm_purchase.yaml`) are system elements of the folder (API 2.12.0): Artisan writes them from the jar when they are missing and replaces them on a new version while they are untouched. An edited one is kept, and the editor says the module ships another version. The editor never deletes or moves them; Reset brings back the shipped file. `SYSTEM_MENUS_VERSION` in `BoutiqueModule` goes up with every change to these files.


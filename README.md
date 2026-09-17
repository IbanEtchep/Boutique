
### Catalogue icon lore

Product presentation belongs to `icon` (title, lore, quantity and attributes). The item model no longer declares a separate product `lore`. At startup, `CatalogIconMigrator` moves existing row lore into an icon extension before Artisan loads the project. The captured item lore is preserved without a placeholder: Artisan keeps the base item lore and renders the icon lines below it. Original files are backed up in `plugins/Boutique/catalog-icon-backup/`, outside the editor project; each replacement is atomic and the migration is idempotent. Canonical translations are copied to the new icon addresses; old keys remain available to legacy references. Generated templates inherit the icon lore and append the price.

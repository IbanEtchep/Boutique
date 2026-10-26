package net.artisanmc.store.artisan;

import net.artisanmc.modules.api.editor.EditorBundleDeclaration;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** The jar carries the editor view it hands to Artisan, built against the SDK it announces. */
class EditorBundleTest {

    @Test
    void theBundledViewIsAValidDeclaration() {
        EditorBundleDeclaration bundle = EditorBundleDeclaration.fromResource(
                StoreModule.class.getClassLoader(), StoreModule.EDITOR_BUNDLE, StoreModule.EDITOR_SDK);
        String script = new String(bundle.getScript(), StandardCharsets.UTF_8);
        // Built by editor/ (npm run build): the module and its kind are in it.
        assertTrue(script.contains("boutique_category"), "the bundle declares the categories kind");
        assertTrue(script.contains("\"" + StoreModule.EDITOR_SDK + "\""), "built against the announced SDK");
    }

    @Test
    void theKindLivesInItsOwnFolder() {
        var kind = StoreModule.categoryKind();
        assertEquals(StoreModule.CATEGORY_KIND, kind.getKind());
        assertEquals(CategoryKindMigrator.DIR, kind.getDir());
        assertEquals(CategoryKindMigrator.SCHEMA, kind.getCanonicalSchema());
    }
}

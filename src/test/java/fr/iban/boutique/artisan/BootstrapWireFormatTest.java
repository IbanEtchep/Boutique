package fr.iban.boutique.artisan;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verrou anti-dérive du wire Artisan. Le core lit les menus/commandes/dialogues
 * de façon TOLÉRANTE PAR FICHIER : un fichier au format périmé est loggé puis
 * skippé, jamais fatal — une install fraîche se retrouve alors silencieusement
 * sans menus ni commande. C'est exactement ce qui est arrivé quand l'Action DSL
 * (phase 2) a remplacé la liste de steps par une CHAÎNE : les ressources
 * bundlées sont restées sur l'ancienne forme pendant des semaines.
 *
 * Ces assertions tiennent la forme canonique sans dépendre du parseur Kotlin du
 * core (hors classpath de l'add-on).
 */
class BootstrapWireFormatTest {

    private static final List<String> ACTION_BEARING = List.of(
            "menus/shop_main.yaml", "menus/shop_category.yaml",
            "commands/boutique.yaml", "dialogs/confirm_purchase.yaml");

    private static Object load(String res) throws Exception {
        try (InputStream in = Bootstrapper.class.getResourceAsStream("/bootstrap/" + res)) {
            assertNotNull(in, () -> "ressource bundlée manquante : " + res);
            return new Yaml().load(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    /** `steps:` était la liste d'actions pré-DSL — le core ne l'accepte plus que
     *  comme CHAÎNE legacy. Aucune ressource ne doit encore l'employer. */
    @Test
    void noResourceStillCarriesAPreDslStepList() throws Exception {
        for (String res : ACTION_BEARING) {
            walk(load(res), (key, value) -> {
                if ("steps".equals(key)) {
                    fail(res + " : clé `steps` (forme pré-DSL) — le wire canonique est "
                            + "une chaîne Action DSL sous `click.<bouton>` / `actions`");
                }
            });
        }
    }

    /** `actions` (commande, bouton de dialogue, ligne d'article) = chaîne DSL. */
    @Test
    void everyActionsValueIsADslString() throws Exception {
        for (String res : ACTION_BEARING) {
            walk(load(res), (key, value) -> {
                if ("actions".equals(key)) {
                    assertInstanceOf(String.class, value,
                            res + " : `actions` doit être une chaîne Action DSL, pas " + value);
                }
            });
        }
    }

    /** `click: {left: "<dsl>"}` — le handler EST la chaîne (forme collapsée). */
    @Test
    void everyClickHandlerIsADslString() throws Exception {
        for (String res : List.of("menus/shop_main.yaml", "menus/shop_category.yaml")) {
            walk(load(res), (key, value) -> {
                if (!"click".equals(key)) return;
                assertInstanceOf(Map.class, value, res + " : `click` doit mapper bouton → chaîne DSL");
                ((Map<?, ?>) value).forEach((button, handler) -> assertInstanceOf(String.class, handler,
                        res + " : click." + button + " doit être une chaîne Action DSL, pas " + handler));
            });
        }
    }

    /** Forme canonique depuis l'aplatissement des calques : `elements` au top-level. */
    @Test
    void menusUseTheFlatElementList() throws Exception {
        for (String res : List.of("menus/shop_main.yaml", "menus/shop_category.yaml")) {
            Map<?, ?> menu = (Map<?, ?>) load(res);
            assertNull(menu.get("layers"), res + " : `layers` est la forme legacy — utiliser `elements`");
            assertInstanceOf(List.class, menu.get("elements"), res + " : `elements` manquant");
        }
    }

    private interface Visitor { void visit(String key, Object value); }

    private static void walk(Object node, Visitor visitor) {
        if (node instanceof Map<?, ?> map) {
            map.forEach((k, v) -> {
                visitor.visit(String.valueOf(k), v);
                walk(v, visitor);
            });
        } else if (node instanceof List<?> list) {
            list.forEach(el -> walk(el, visitor));
        }
    }
}

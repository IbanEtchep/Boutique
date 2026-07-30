package fr.iban.boutique.artisan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Project lang files (`lang/<code>.yaml`) are authored as YAML TREES — each `.`
 * of a translation key is a level — while the runtime addresses them by dotted
 * key. This folds a parsed tree back to dotted keys.
 *
 * Mirror of Artisan core's `net.artisanmc.plugin.yaml.LangFlattener` (vector:
 * `testdata/lang-tree-conformance.json`). Tolerant by design: a file may be
 * fully nested, still flat, or any mix. Non-string scalars coerce, {@code null}
 * is the "" sentinel, lists are skipped.
 */
public final class LangFlattener {

    private LangFlattener() {}

    public static Map<String, String> flatten(Object tree) {
        Map<String, String> out = new HashMap<>();
        walk(tree, "", out);
        return out;
    }

    private static void walk(Object node, String path, Map<String, String> out) {
        if (node instanceof List<?>) return;
        if (node == null) {
            if (!path.isEmpty()) out.put(path, "");
            return;
        }
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> e : map.entrySet()) {
                String seg = String.valueOf(e.getKey());
                walk(e.getValue(), path.isEmpty() ? seg : path + "." + seg, out);
            }
            return;
        }
        if (!path.isEmpty()) out.put(path, node.toString());
    }
}

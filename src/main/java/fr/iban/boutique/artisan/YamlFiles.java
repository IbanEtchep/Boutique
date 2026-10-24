package fr.iban.boutique.artisan;

import java.io.File;
import java.util.List;

/**
 * Un fichier YAML du dossier du module sous l'une ou l'autre extension : Artisan lit {@code .yml}
 * et {@code .yaml} depuis l'API 2.15.0, et ses fichiers passent en {@code .yml}. Le fichier de
 * module est {@code artisan-module.yml}, anciennement {@code manifest.yaml}.
 */
final class YamlFiles {
    /** Par ordre de préférence, comme Artisan les lit. */
    static final List<String> MODULE_FILES = List.of("artisan-module.yml", "manifest.yml", "manifest.yaml");

    private YamlFiles() {}

    static boolean isYaml(String name) {
        return name.endsWith(".yml") || name.endsWith(".yaml");
    }

    /** {@code name} sans son extension YAML, ou {@code null}. */
    static String stem(String name) {
        if (name.endsWith(".yml")) return name.substring(0, name.length() - ".yml".length());
        if (name.endsWith(".yaml")) return name.substring(0, name.length() - ".yaml".length());
        return null;
    }

    /** {@code <dir>/<stem>.yml}, sinon {@code <dir>/<stem>.yaml}, sinon {@code null}. */
    static File find(File dir, String stem) {
        File yml = new File(dir, stem + ".yml");
        if (yml.isFile()) return yml;
        File yaml = new File(dir, stem + ".yaml");
        return yaml.isFile() ? yaml : null;
    }

    /**
     * Le fichier où écrire {@code stem} : celui qui existe, sinon un nouveau sous l'extension du
     * dossier ({@code .yml} dès qu'il en contient un, {@code .yaml} sinon), pour ne jamais poser
     * un {@code x.yaml} à côté d'un {@code x.yml}.
     */
    static File target(File dir, String stem) {
        File existing = find(dir, stem);
        if (existing != null) return existing;
        String[] yml = dir.list((d, n) -> n.endsWith(".yml"));
        return new File(dir, stem + (yml != null && yml.length > 0 ? ".yml" : ".yaml"));
    }

    /**
     * Les fichiers YAML de {@code dir} (pas de sous-dossier, pas de fichier caché), par nom sans
     * extension, triés : un nom présent sous les deux extensions est lu depuis son {@code .yml}.
     */
    static java.util.SortedMap<String, File> list(File dir) {
        java.util.SortedMap<String, File> out = new java.util.TreeMap<>();
        File[] files = dir.listFiles((d, n) -> isYaml(n) && !n.startsWith("."));
        if (files == null) return out;
        for (File f : files) {
            if (!f.isFile()) continue;
            String stem = stem(f.getName());
            if (!out.containsKey(stem) || f.getName().endsWith(".yml")) out.put(stem, f);
        }
        return out;
    }

    /** Le fichier de module du dossier, ou {@code null} : le dossier n'est pas encore un module. */
    static File moduleFile(File projectDir) {
        for (String name : MODULE_FILES) {
            File f = new File(projectDir, name);
            if (f.isFile()) return f;
        }
        return null;
    }
}

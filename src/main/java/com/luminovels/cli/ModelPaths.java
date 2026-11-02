package com.luminovels.cli;

import java.nio.file.Path;

/**
 * Resolves where model files live: the flag, then LUMINOVELS_MODEL_DIR, then a gitignored models folder.
 */
final class ModelPaths {

    static final String ENV_DIR = "LUMINOVELS_MODEL_DIR";

    private ModelPaths() {
    }

    static Path dir(Args args) {
        String flag = args.get("model-dir");
        if (flag != null) {
            return Path.of(flag);
        }
        String env = System.getenv(ENV_DIR);
        return Path.of(env == null || env.isBlank() ? "models" : env);
    }

    static Path file(Args args, String defaultName) {
        String explicit = args.get("model");
        return explicit != null ? Path.of(explicit) : dir(args).resolve(defaultName);
    }
}

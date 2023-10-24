package pascal.taie.project;

import pascal.taie.World;

public abstract class AbstractFileContainer implements FileContainer {
    public AnalysisFile locate(ClassLocation restLocation) {
        assert restLocation.hasNext() : "If a ClassLocation is terminated, never pass it to another locate call.";

        String current = restLocation.next();
        if (restLocation.hasNext()) {
            // If classPath.hasNext() then current is a package name.
            // There should exist at most 1 container with the same name.
            var fileContainer = containers().stream()
                    .filter(c -> c.className().equals(current))
                    .findAny();
            return fileContainer.map(c -> c.locate(restLocation)).orElse(null);
        } else {
            // else then current is a class name.
            // There should exist at most 1 file with the same name.
            var file = files().stream().
                    filter(f -> isTarget(f, current))
                    .findAny();
            return file.orElse(null);
        }
    }

    protected static boolean isTarget(AnalysisFile file, String className) {
        if (!(file instanceof ClassFile) && ! (!World.get().getOptions().getNoAppendJava()
            && file instanceof JavaSourceFile)) {
            return false;
        }

        int endIndex = file.fileName().indexOf('.');
        return file.fileName().substring(0, endIndex).equals(className);
    }
}

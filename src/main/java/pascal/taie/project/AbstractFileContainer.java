package pascal.taie.project;

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
        if (!(file instanceof ClassFile) && ! (file instanceof JavaSourceFile)) {
            return false;
        }

        // For inner class file, its class name is after $
        int dollarIndex = file.fileName().lastIndexOf('$');
        int startIndex = dollarIndex == -1 ? 0 : dollarIndex;
        int endIndex = file.fileName().indexOf('.');

        return file.fileName().substring(startIndex, endIndex).equals(className);
    }
}

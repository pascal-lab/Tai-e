package pascal.taie.frontend.newfrontend;

import pascal.taie.project.Project;

public class DepResolver implements ClassResolver {

    public DepResolver() {
    }

    @Override
    public ClassSource getClassSource(String binaryName) {
        return null;
    }

    @Override
    public int getTotalClasses() {
        return 0;
    }

    @Override
    public void resolve(Project p) {
    }
}

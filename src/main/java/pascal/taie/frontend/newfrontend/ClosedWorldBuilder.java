package pascal.taie.frontend.newfrontend;

import pascal.taie.project.Project;

public interface ClosedWorldBuilder {

    /**
     * Get Class Source of a given binary name
     * @param binaryName e.g. a.b.C$D
     */
    ClassSource getClassSource(String binaryName);

    /**
     * Get the number of total Classes in the closed-world
     */
    int getTotalClasses();

    /**
     * make the closed-world
     */
    void resolve(Project p);
}

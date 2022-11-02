package pascal.taie.frontend.newfrontend;

import pascal.taie.project.Project;

import java.util.Collection;

public interface ClosedWorldBuilder {


    /**
     * Get the number of total Classes in the closed-world
     */
    int getTotalClasses();

    /**
     * Get the closed-world, i.e., all classes needed in analysis
     */
    Collection<ClassSource> getClosedWorld();

    /**
     * make the closed-world
     */
    void build(Project p);
}

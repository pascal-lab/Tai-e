package pascal.taie.frontend.newfrontend;

import pascal.taie.project.AnalysisFile;
import pascal.taie.project.Project;

import java.util.Set;

public interface ClassResolver {

    public Set<AnalysisFile> resolve(Project p);

}

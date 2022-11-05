package pascal.taie.frontend.newfrontend;

import pascal.taie.language.classes.ClassHierarchy;

import java.util.Collection;

public interface ClassHierarchyBuilder {
    ClassHierarchy build(Collection<ClassSource> sources);
}

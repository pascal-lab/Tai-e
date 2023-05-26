package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.ClassReader;
import pascal.taie.project.ClassFile;
import pascal.taie.project.FileContainer;
import pascal.taie.project.Project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AllClassesCWBuilder implements ClosedWorldBuilder {

    private Project project;

    List<ClassSource> allClasses;

    @Override
    public int getTotalClasses() {
        return allClasses.size();
    }

    @Override
    public Collection<ClassSource> getClosedWorld() {
        return allClasses;
    }

    @Override
    public void build(Project p) {
        project = p;
        allClasses = new ArrayList<>();
        allClasses.addAll(outPutAll(p.getAppRootContainers()));
        allClasses.addAll(outPutAll(p.getLibRootContainers()));
    }

    private List<ClassSource> outPutAll(FileContainer container) {
        boolean isAppRoot = project.getAppRootContainers().contains(container);
        List<ClassSource> res = new ArrayList<>(container.files().stream()
                .filter(f -> f instanceof ClassFile)
                .map(c -> {
                    try {
                        var r = new ClassReader(c.resource().getContent());
                        String fullClassName = r.getClassName().replaceAll("/", ".");
                        assert !fullClassName.contains("/");
                        boolean isApplication = isAppRoot
                                || project.getInputClasses().contains(fullClassName)
                                || fullClassName.equals(project.getMainClass());
                        return new AsmSource(r, isApplication);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList());
        res.addAll(outPutAll(container.containers()));
        return res;
    }

    private List<ClassSource> outPutAll(List<FileContainer> containers) {
        return containers.stream()
                .flatMap(c -> outPutAll(c).stream())
                .toList();
    }
}

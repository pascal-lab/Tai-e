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
        allClasses = new ArrayList<>();
        allClasses.addAll(outPutAll(p.getAppRootContainers()));
        allClasses.addAll(outPutAll(p.getLibRootContainers()));
    }

    private List<ClassSource> outPutAll(FileContainer container) {
        List<ClassSource> res = new ArrayList<>(container.files().stream()
                .filter(f -> f instanceof ClassFile)
                .map(c -> {
                    try {
                        return new AsmSource(new ClassReader(c.resource().getContent()));
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

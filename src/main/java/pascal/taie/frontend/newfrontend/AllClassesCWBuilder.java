package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.ClassReader;
import pascal.taie.project.ClassFile;
import pascal.taie.project.FileContainer;
import pascal.taie.project.Project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AllClassesCWBuilder {

    private static final List<String> excluded = List.of("android$widget$RemoteViews$BaseReflectionAction.class");

    public static List<String> outPutAll(FileContainer root) {
        return outPutAll("", root, true);
    }

    private static List<String> outPutAll(String current, FileContainer container, boolean isRoot) {
        String currentNext = (isRoot) ? "" : current + container.className() + ".";
        List<String> res = new ArrayList<>(container.files().stream()
                .filter(f -> f instanceof ClassFile)
                .filter(f -> ! excluded.contains(f.fileName()))
                .filter(f -> ! f.fileName().equals("module-info.class"))
                .map(c -> {
                    String fullClassName = currentNext + ((ClassFile) c).getClassName();
                    assert !fullClassName.contains("/");
                    return fullClassName;
                })
                .toList());
        res.addAll(outPutAll(currentNext, container.containers()));
        return res;
    }

    private static List<String> outPutAll(String current, List<FileContainer> containers) {
        return containers.stream()
                .flatMap(c -> outPutAll(current, c, false).stream())
                .toList();
    }
}

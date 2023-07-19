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

    private final static List<String> excluded = List.of("android$widget$RemoteViews$BaseReflectionAction.class");

    public static List<String> outPutAll(FileContainer container) {
        List<String> res = new ArrayList<>(container.files().stream()
                .filter(f -> f instanceof ClassFile)
                .filter(f -> ! excluded.contains(f.fileName()))
                .map(c -> {
                    try {
                        var r = new ClassReader(c.resource().getContent());
                        String fullClassName = r.getClassName().replaceAll("/", ".");
                        assert !fullClassName.contains("/");
                        return fullClassName;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList());
        res.addAll(outPutAll(container.containers()));
        return res;
    }

    private static List<String> outPutAll(List<FileContainer> containers) {
        return containers.stream()
                .flatMap(c -> outPutAll(c).stream())
                .toList();
    }
}

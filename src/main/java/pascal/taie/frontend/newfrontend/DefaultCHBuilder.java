package pascal.taie.frontend.newfrontend;

import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassHierarchyImpl;
import pascal.taie.language.classes.JClass;
import pascal.taie.util.collection.Maps;

import java.util.Collection;
import java.util.Map;

public class DefaultCHBuilder implements ClassHierarchyBuilder {

    @Override
    public ClassHierarchy build(Collection<ClassSource> sources) {
        ClassHierarchyImpl ch = new ClassHierarchyImpl();
        DefaultClassLoader dcl = new DefaultClassLoader();
        Map<String, JClass> m = Maps.newConcurrentMap();
        sources.parallelStream().forEach(i -> {
            String name = i.getClassName();
            m.put(name, new JClass(dcl, name));
        });

        m.values().parallelStream().forEach(i -> {
            AsmClassBuilder asb = new AsmClassBuilder();
            i.build(asb);
        });

        for (var i : m.values()) {
            ch.addClass(i);
        }

        return ch;
    }
}

package pascal.taie.frontend.newfrontend;

import pascal.taie.language.classes.*;
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

        sources.parallelStream().forEach(i -> {
            JClass klass = m.getOrDefault(i.getClassName(), null);
            if (klass == null) {
                throw new IllegalStateException();
            }
            JClassBuilder asb = getClassBuilder(i, dcl, m, klass);
            asb.build(klass);
        });

        for (var i : m.values()) {
            ch.addClass(i);
        }

        return ch;
    }

    private JClassBuilder getClassBuilder(
            ClassSource source, JClassLoader loader,
            Map<String, JClass> jClassMap, JClass jClass) {
        if (source instanceof AsmSource i) {
            return new AsmClassBuilder(i, loader, jClassMap, jClass);
        } else{
            // TODO: fill in here
            throw new IllegalStateException();
        }
    }
}

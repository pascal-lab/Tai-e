package pascal.taie.frontend.newfrontend;

import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassHierarchyImpl;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassBuilder;
import pascal.taie.util.collection.Maps;

import java.util.Collection;
import java.util.Map;

public class DefaultCHBuilder implements ClassHierarchyBuilder {

    @Override
    public ClassHierarchy build(Collection<ClassSource> sources) {
        ClassHierarchyImpl ch = new ClassHierarchyImpl();
        DefaultClassLoader dcl = new DefaultClassLoader();
        Map<String, JClass> m = Maps.newConcurrentMap();
        dcl.setMapping(m);
        sources.parallelStream().forEach(i -> {
            String name = i.getClassName();
            m.put(name, new JClass(dcl, name));
        });

        ch.setDefaultClassLoader(dcl);
        ch.setBootstrapClassLoader(dcl);
        BuildContext.make(dcl);

        sources.parallelStream().forEach(i -> {
            JClass klass = m.getOrDefault(i.getClassName(), null);
            if (klass == null) {
                throw new IllegalStateException();
            }
            JClassBuilder asb = getClassBuilder(i, klass);
            asb.build(klass);
        });

        for (var i : m.values()) {
            if (i.getIndex() == -1) {
                ch.addClass(i);
            }
        }

        BuildContext.get().setHierarchy(ch);
        return ch;
    }

    private JClassBuilder getClassBuilder(
            ClassSource source,  JClass jClass) {
        if (source instanceof AsmSource i) {
            return new AsmClassBuilder(i, jClass);
        } else{
            // TODO: fill in here
            throw new IllegalStateException();
        }
    }
}

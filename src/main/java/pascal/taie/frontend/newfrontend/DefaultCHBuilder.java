package pascal.taie.frontend.newfrontend;

import pascal.taie.frontend.newfrontend.java.JavaClassBuilder;
import pascal.taie.World;
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
        DefaultClassLoader dcl = new DefaultClassLoader(ch, World.get().getOptions().isAllowPhantom());
        Map<String, JClass> m = Maps.newMap();
        dcl.setMapping(m);
        sources.forEach(i -> {
            String name = i.getClassName();
            m.put(name, new JClass(dcl, name));
        });

        ch.setDefaultClassLoader(dcl);
        ch.setBootstrapClassLoader(dcl);
        BuildContext.make(dcl);

        boolean preBuild = World.get().getOptions().isPreBuildIR();
        sources.parallelStream().forEach(i -> {
            JClass klass = m.getOrDefault(i.getClassName(), null);
            if (klass == null) {
                throw new IllegalStateException();
            }
            JClassBuilder asb = getClassBuilder(i, klass);
            asb.build(klass);
            if (!preBuild && i instanceof AsmSource as) {
                BuildContext.get().noticeClassSource(klass, as);
            }
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
        } else if (source instanceof JavaSource j) {
            return new JavaClassBuilder(j, jClass);
        } else{
            throw new UnsupportedOperationException();
        }
    }
}

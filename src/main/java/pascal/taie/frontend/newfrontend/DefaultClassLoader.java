package pascal.taie.frontend.newfrontend;

import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassLoader;

import java.util.Collection;
import java.util.Map;

public class DefaultClassLoader implements JClassLoader {

    private final ClassHierarchy hierarchy;

    private final boolean allowPhantom;

    Map<String, JClass> mapping;

    DefaultClassLoader(ClassHierarchy hierarchy, boolean allowPhantom) {
        this.hierarchy = hierarchy;
        this.allowPhantom = allowPhantom;
    }

    @Override
    public JClass loadClass(String name) {
        return loadClass(name, allowPhantom);
    }

    @Override
    public JClass loadClass(String name, boolean allowPhantom) {
        JClass jclass = mapping.get(name);
        if (jclass == null) {
            if (this.allowPhantom && allowPhantom) {
                // phantom class
                jclass = new JClass(this, name, null); // what should a moduleName for a phantom class be?
                mapping.put(name, jclass);
                new PhantomClassBuilder(name).build(jclass);
                hierarchy.addClass(jclass);
            } else {
                return null;
            }
        }

        // TODO: add warning for missing classes
        return jclass;
    }

    public void setMapping(Map<String, JClass> mapping) {
        this.mapping = mapping;
    }

    @Override
    public Collection<JClass> getLoadedClasses() {
        return null;
    }
}

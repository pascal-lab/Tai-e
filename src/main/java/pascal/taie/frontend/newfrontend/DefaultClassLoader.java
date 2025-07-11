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

    private final Object phantomLock = new Object();

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
        if (jclass == null && this.allowPhantom && allowPhantom) {
            synchronized (phantomLock) {
                jclass = mapping.get(name);
                if (jclass == null) {
                    // phantom class
                    // what should a moduleName for a phantom class be?
                    jclass = new JClass(this, name, null);
                    mapping.put(name, jclass); // mapping itself is a concurrent map
                    new PhantomClassBuilder(name).build(jclass);
                    // Here is the only point where hierarchy could be concurrently added
                    // if there is no mutex.
                    hierarchy.addClass(jclass);
                }
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

package pascal.taie.frontend.newfrontend;

import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassLoader;

import java.util.Collection;

public class DefaultClassLoader implements JClassLoader {

    @Override
    public JClass loadClass(String name) {
        return null;
    }

    @Override
    public Collection<JClass> getLoadedClasses() {
        return null;
    }
}

package pascal.taie.frontend.newfrontend;

import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassLoader;

import java.util.Collection;
import java.util.Map;

public class DefaultClassLoader implements JClassLoader {

    Map<String, JClass> mapping;

    @Override
    public JClass loadClass(String name) {
        return mapping.get(name);
    }

    public void setMapping(Map<String, JClass> mapping) {
        this.mapping = mapping;
    }

    @Override
    public Collection<JClass> getLoadedClasses() {
        return null;
    }
}

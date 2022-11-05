package pascal.taie.frontend.newfrontend;

import pascal.taie.language.annotation.AnnotationHolder;
import pascal.taie.language.classes.*;
import pascal.taie.language.type.ClassType;

import java.util.Collection;
import java.util.Set;

public class AsmClassBuilder implements JClassBuilder {

    @Override
    public void build(JClass jclass) {
    }

    @Override
    public Set<Modifier> getModifiers() {
        return null;
    }

    @Override
    public String getSimpleName() {
        return null;
    }

    @Override
    public ClassType getClassType() {
        return null;
    }

    @Override
    public JClass getSuperClass() {
        return null;
    }

    @Override
    public Collection<JClass> getInterfaces() {
        return null;
    }

    @Override
    public JClass getOuterClass() {
        return null;
    }

    @Override
    public Collection<JField> getDeclaredFields() {
        return null;
    }

    @Override
    public Collection<JMethod> getDeclaredMethods() {
        return null;
    }

    @Override
    public AnnotationHolder getAnnotationHolder() {
        return null;
    }

    @Override
    public boolean isApplication() {
        return false;
    }

    @Override
    public boolean isPhantom() {
        return false;
    }
}

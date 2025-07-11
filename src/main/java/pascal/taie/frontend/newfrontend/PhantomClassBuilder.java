package pascal.taie.frontend.newfrontend;

import pascal.taie.language.annotation.AnnotationHolder;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassBuilder;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Modifier;
import pascal.taie.language.generics.ClassGSignature;
import pascal.taie.language.type.ClassType;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

class PhantomClassBuilder implements JClassBuilder {

    private final String name;

    PhantomClassBuilder(String name) {
        this.name = name;
    }

    @Override
    public void build(JClass jclass) {
        jclass.build(this);
    }

    @Override
    public Set<Modifier> getModifiers() {
        // WARNING: all phantom classes that were 'implement'ed would not be set as interface.
        // Finer grained initialization should be taken.
        return EnumSet.noneOf(Modifier.class);
    }

    @Override
    public String getSimpleName() {
        int i = name.lastIndexOf('.');
        if (i > 0) {
            return name.substring(i + 1);
        } else {
            return name;
        }
    }

    @Override
    public ClassType getClassType() {
        return BuildContext.get().getTypeSystem().getClassType(name);
    }

    @Override
    public JClass getSuperClass() {
        return Utils.getObject().getJClass(); // Object for phantom class. However, is it better to fake a "ILL" supertype?
    }

    @Override
    public Collection<JClass> getInterfaces() {
        return List.of();
    }

    @Override
    public JClass getOuterClass() {
        return null;
    }

    @Override
    public Collection<JField> getDeclaredFields() {
        return List.of();
    }

    @Override
    public Collection<JMethod> getDeclaredMethods() {
        return List.of();
    }

    @Override
    public AnnotationHolder getAnnotationHolder() {
        return AnnotationHolder.emptyHolder();
    }

    @Override
    public boolean isApplication() {
        // TODO
        return true; // Temporarily, true for safety
    }

    @Override
    public boolean isPhantom() {
        return true;
    }

    @Nullable
    @Override
    public ClassGSignature getGSignature() {
        // for phantom class, no generic signature should be provided.
        return null;
    }
}

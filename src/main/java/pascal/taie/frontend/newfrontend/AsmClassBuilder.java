package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.*;
import pascal.taie.language.annotation.AnnotationHolder;
import pascal.taie.language.classes.*;
import pascal.taie.language.type.ClassType;

import java.util.*;

public class AsmClassBuilder implements JClassBuilder {

    private final AsmSource source;

    private final JClassLoader loader;

    private final Map<String, JClass> classMap;

    private final JClass jClass;

    private Set<Modifier> modifiers;

    private JClass superClass;

    private List<JClass> interfaces;

    private JClass innerClass;

    private JClass outerClass;

    private List<JField> fields;

    public AsmClassBuilder(
            AsmSource source, JClassLoader loader,
            Map<String, JClass> classMap, JClass jClass) {
        this.source = source;
        this.loader = loader;
        this.classMap = classMap;
        this.jClass = jClass;
    }

    @Override
    public void build(JClass jclass) {
        buildAll();
        if (jclass != this.jClass) {
            throw new IllegalArgumentException();
        }
        jclass.build(this);
    }

    @Override
    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    @Override
    public String getSimpleName() {
        return getSimpleName(source.getClassName());
    }

    @Override
    public ClassType getClassType() {
        return new ClassType(loader, source.getClassName());
    }

    @Override
    public JClass getSuperClass() {
        return superClass;
    }

    @Override
    public Collection<JClass> getInterfaces() {
        return interfaces;
    }

    @Override
    public JClass getOuterClass() {
        return outerClass;
    }

    @Override
    public Collection<JField> getDeclaredFields() {
        return fields;
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
        // TODO: handle phantom class
        return false;
    }

    private void buildAll() {
        BuildVisitor visitor = new BuildVisitor();
        source.r().accept(visitor, ClassReader.SKIP_FRAMES);
    }

    private String getSimpleName(String binaryName) {
        return binaryName.substring(binaryName.indexOf("."));
    }

    private JClass getClassByName(String internalName) {
        return classMap.get(Utils.getBinaryName(internalName));
    }

    class BuildVisitor extends ClassVisitor {

        public BuildVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(
                int version,
                int access,
                String name,
                String signature,
                String superName,
                String[] interfaces) {
            if (superName != null) {
                superClass = getClassByName(superName);
            }

            AsmClassBuilder.this.interfaces = Arrays.stream(interfaces)
                    .map(AsmClassBuilder.this::getClassByName)
                    .toList();

            modifiers = Utils.fromAsmModifier(access);
        }

        @Override
        public void visitAttribute(Attribute attribute) {
            // TODO: check what attribute is needed.
        }

        @Override
        public FieldVisitor visitField(
                int access,
                String name,
                String descriptor,
                String signature,
                Object value) {
            // TODO: complete it
            fields.add(new JField(jClass, name,
                    Utils.fromAsmModifier(access), null, null));
            return null;
        }


    }
}

package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

import pascal.taie.util.collection.Sets;

import java.util.Arrays;
import java.util.Set;

// TODO: 1. Add AnnotationVisitor
//       2. Check generic
public class DepClassVisitor extends ClassVisitor {

    // TODO: handle Ldc
    private class DepMethodVisitor extends MethodVisitor {

        public DepMethodVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            addDescriptor(descriptor);
            return null;
        }


        @Override
        public AnnotationVisitor visitTypeAnnotation(
                int typeRef,
                TypePath typePath,
                String descriptor,
                boolean visible) {
           addDescriptor(descriptor);
           return null;
        }

        @Override
        public void visitFieldInsn(
                int opcode,
                String owner,
                String name,
                String descriptor) {
            addDescriptor(descriptor);
        }

        @Override
        public AnnotationVisitor visitInsnAnnotation(
                int typeRef,
                TypePath typePath,
                String descriptor,
                boolean visible) {
            addDescriptor(descriptor);
            return null;
        }

        @Override
        public void visitLocalVariable(
                String name,
                String descriptor,
                String signature,
                Label start, Label end, int index) {
            addDescriptor(descriptor);
        }

        @Override
        public AnnotationVisitor visitLocalVariableAnnotation(
                int typeRef,
                TypePath typePath,
                Label[] start, Label[] end,
                int[] index,
                String descriptor,
                boolean visible) {
            addDescriptor(descriptor);
            return null;
        }

        @Override
        public void visitMethodInsn(
                int opcode,
                String owner,
                String name,
                String descriptor,
                boolean isInterface) {
            addDescriptor(descriptor);
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            addDescriptor(descriptor);
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(
                int parameter,
                String descriptor,
                boolean visible) {
            addDescriptor(descriptor);
            return null;
        }

        @Override
        public void	visitTryCatchBlock(
                Label start, Label end, Label handler,
                String type) {
            addBinaryName(type);
        }

        @Override
        public AnnotationVisitor visitTryCatchAnnotation(
                int typeRef,
                TypePath typePath,
                String descriptor,
                boolean visible) {
            addDescriptor(descriptor);
            return null;
        }

        @Override
        public void visitTypeInsn(int opCode, String type) {
            addBinaryName(type);
        }

    }

    public Set<String> getBinaryNames() {
        return binaryNames;
    }

    private final Set<String> binaryNames;

    public DepClassVisitor() {
        super(Opcodes.ASM9);
        binaryNames = Sets.newSet();
    }

    @Override
    public FieldVisitor visitField(
            int access,
            java.lang.String name,
            java.lang.String descriptor,
            java.lang.String signature,
            java.lang.Object value) {
        addDescriptor(descriptor);
        return null;
    }

    @Override
    public MethodVisitor visitMethod(
            int access,
            String name,
            String descriptor,
            String signature,
            String[] exceptions) {
        addMethodDescriptor(descriptor);

        if (exceptions != null) {
            binaryNames.addAll(Arrays.asList(exceptions));
        }

        return new DepMethodVisitor();
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        addDescriptor(descriptor);
        return null;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
            int typeRef,
            TypePath typePath,
            String descriptor,
            boolean visible) {
        addDescriptor(descriptor);
        return null;
    }

    @Override
    public RecordComponentVisitor visitRecordComponent(
            String name,
            String descriptor,
            String signature) {
        addDescriptor(descriptor);
        return null;
    }

    @Override
    public void visitNestHost(String nestHost) {
        binaryNames.add(nestHost);
    }

    @Override
    public void visitNestMember(String member) {
        binaryNames.add(member);
    }

    private void addBinaryName(String binaryName) {
        binaryNames.add(binaryName);
    }

    private void addDescriptor(String descriptor) {
        binaryNames.add(getBinaryName(descriptor));
    }

    private void addMethodDescriptor(String descriptor) {
        Type type = Type.getType(descriptor);

        for (var i : type.getArgumentTypes()) {
            binaryNames.add(i.getClassName());
        }
        binaryNames.add(type.getReturnType().getClassName());
    }

    private static String getBinaryName(String descriptor) {
        return Type.getType(descriptor).getClassName();
    }
}

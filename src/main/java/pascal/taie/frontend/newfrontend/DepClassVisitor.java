package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import pascal.taie.util.collection.Sets;

import java.util.Set;

// TODO: 1. Add AnnotationVisitor
//       2. Check generic
//       3. Check inner class
public class DepClassVisitor extends ClassVisitor {

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
            addInternalName(owner);
            addDescriptor(descriptor);
        }

        @Override
        public void visitLdcInsn(Object value) {
            if (value instanceof Type t) {
                addType(t);
            } else if (value instanceof Handle handle) {
                addInternalName(handle.getOwner());
            } else if (value instanceof ConstantDynamic cd) {
                addDescriptor(cd.getDescriptor());
            }
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
            addInternalName(type);
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
            addInternalName(type);
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
    public void visit(int version, int access, String name,
                      String signature, String superName, String[] interfaces) {
        addInternalName(superName);
        for (var i : interfaces) {
            addInternalName(i);
        }
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
        addDescriptor(descriptor);

        if (exceptions != null) {
            for (var i : exceptions) {
                addInternalName(i);
            }
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
        addInternalName(nestHost);
    }

    @Override
    public void visitNestMember(String member) {
        addInternalName(member);
    }

    private void addBinaryName(String binaryName) {
        binaryNames.add(binaryName);
    }

    private void addDescriptor(String descriptor) {
        addType(Type.getType(descriptor));
    }

    private void addInternalName(String internalName) {
        if (internalName == null) {
            return;
        }
        addBinaryName(Utils.getBinaryName(internalName));
    }

    private void addType(Type t) {
        if (t.getSort() == Type.ARRAY) {
            addType(t.getElementType());
        } else if (t.getSort() == Type.OBJECT) {
            addBinaryName(t.getClassName());
        } else if (t.getSort() == Type.METHOD) {
            for (var i : t.getArgumentTypes()) {
                addType(i);
            }
            addType(t.getReturnType());
        }
    }
}

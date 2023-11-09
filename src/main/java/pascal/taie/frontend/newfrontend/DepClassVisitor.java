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
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeAnnotationNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * It seems that this class is slower than {@link ConstantTableReader}
 * <br/>
 * So we use {@link ConstantTableReader} instead.
 */
// TODO: 1. Add AnnotationVisitor
//       2. Check generic
//       3. Check inner class
public class DepClassVisitor extends ClassVisitor {

    public static Collection<String> getDependencies(ClassNode node) {
        // NOTE: inlined from ClassReader.accept
        DepClassVisitor visitor = new DepClassVisitor();
        // Visit the header.
        String[] interfacesArray = new String[node.interfaces.size()];
        node.interfaces.toArray(interfacesArray);
        visitor.visit(node.version, node.access, node.name,
                node.signature, node.superName, interfacesArray);
        // Visit the source., skipped
        // if (node.sourceFile != null || node.sourceDebug != null) {
        // visitor.visitSource(node.sourceFile, node.sourceDebug);
        // }
        // Visit the module., skipped
        // if (node.module != null) {
        //  node.module.accept(visitor);
        // }
        // Visit the nest host class.
        if (node.nestHostClass != null) {
          ((ClassVisitor) visitor).visitNestHost(node.nestHostClass);
        }
        // Visit the outer class.
        if (node.outerClass != null) {
          visitor.visitOuterClass(node.outerClass, node.outerMethod, node.outerMethodDesc);
        }
        // Visit the annotations.
        if (node.visibleAnnotations != null) {
          for (int i = 0, n = node.visibleAnnotations.size(); i < n; ++i) {
            AnnotationNode annotation = node.visibleAnnotations.get(i);
            annotation.accept(((ClassVisitor) visitor).visitAnnotation(annotation.desc, true));
          }
        }
        if (node.invisibleAnnotations != null) {
          for (int i = 0, n = node.invisibleAnnotations.size(); i < n; ++i) {
            AnnotationNode annotation = node.invisibleAnnotations.get(i);
            annotation.accept(((ClassVisitor) visitor).visitAnnotation(annotation.desc, false));
          }
        }
        if (node.visibleTypeAnnotations != null) {
          for (int i = 0, n = node.visibleTypeAnnotations.size(); i < n; ++i) {
            TypeAnnotationNode typeAnnotation = node.visibleTypeAnnotations.get(i);
            typeAnnotation.accept(
                ((ClassVisitor) visitor).visitTypeAnnotation(
                    typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, true));
          }
        }
        if (node.invisibleTypeAnnotations != null) {
          for (int i = 0, n = node.invisibleTypeAnnotations.size(); i < n; ++i) {
            TypeAnnotationNode typeAnnotation = node.invisibleTypeAnnotations.get(i);
            typeAnnotation.accept(
                ((ClassVisitor) visitor).visitTypeAnnotation(
                    typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, false));
          }
        }
        // Visit the non standard attributes.
        if (node.attrs != null) {
          for (int i = 0, n = node.attrs.size(); i < n; ++i) {
            visitor.visitAttribute(node.attrs.get(i));
          }
        }
        // Visit the nest members.
        if (node.nestMembers != null) {
          for (int i = 0, n = node.nestMembers.size(); i < n; ++i) {
            ((ClassVisitor) visitor).visitNestMember(node.nestMembers.get(i));
          }
        }
        // Visit the permitted subclasses.
        if (node.permittedSubclasses != null) {
          for (int i = 0, n = node.permittedSubclasses.size(); i < n; ++i) {
            visitor.visitPermittedSubclass(node.permittedSubclasses.get(i));
          }
        }
        // Visit the inner classes.
        for (int i = 0, n = node.innerClasses.size(); i < n; ++i) {
          node.innerClasses.get(i).accept(visitor);
        }
        // Visit the record components.
        if (node.recordComponents != null) {
          for (int i = 0, n = node.recordComponents.size(); i < n; ++i) {
            node.recordComponents.get(i).accept(visitor);
          }
        }
        // Visit the fields.
        for (int i = 0, n = node.fields.size(); i < n; ++i) {
          node.fields.get(i).accept(visitor);
        }
        // Visit the methods.
        for (int i = 0, n = node.methods.size(); i < n; ++i) {
            MethodNode methodNode = node.methods.get(i);
            visitor.addDescriptor(methodNode.desc);

            if (methodNode.exceptions != null) {
                for (var i1 : methodNode.exceptions) {
                    visitor.addInternalName(i1);
                }
            }

            if (methodNode.visibleAnnotations != null) {
                for (AnnotationNode node1 : methodNode.visibleAnnotations) {
                    visitor.addDescriptor(node1.desc);
                }
            }
            if (methodNode.invisibleAnnotations != null) {
                for (AnnotationNode node1 : methodNode.invisibleAnnotations) {
                    visitor.addDescriptor(node1.desc);
                }
            }
            if (methodNode.visibleTypeAnnotations != null) {
                for (TypeAnnotationNode node1 : methodNode.visibleTypeAnnotations) {
                    visitor.addDescriptor(node1.desc);
                }
            }
            if (methodNode.invisibleTypeAnnotations != null) {
                for (TypeAnnotationNode node1 : methodNode.invisibleTypeAnnotations) {
                    visitor.addDescriptor(node1.desc);
                }
            }
            if (methodNode.instructions != null) {
                AbstractInsnNode[] array = methodNode.instructions.toArray();
                for (AbstractInsnNode currentInsn : array) {
                    if (currentInsn instanceof InvokeDynamicInsnNode insn) {
                        visitor.addDescriptor(insn.desc);
                        visitor.addHandle(insn.bsm);
                        for (Object o : insn.bsmArgs) {
                            if (o instanceof Type t) {
                                visitor.addType(t);
                            } else if (o instanceof Handle handle) {
                                visitor.addHandle(handle);
                            } else if (o instanceof ConstantDynamic cd) {
                                visitor.addConstDyn(cd);
                            }
                        }
                    } else if (currentInsn instanceof LdcInsnNode ldc) {
                        if (ldc.cst instanceof Type t) {
                            visitor.addType(t);
                        } else if (ldc.cst instanceof Handle handle) {
                            visitor.addHandle(handle);
                        } else if (ldc.cst instanceof ConstantDynamic cd) {
                            visitor.addConstDyn(cd);
                        }
                    } else if (currentInsn instanceof MethodInsnNode mtd) {
                        visitor.addInternalName(mtd.owner);
                        visitor.addDescriptor(mtd.desc);
                    } else if (currentInsn instanceof TypeInsnNode typeInsnNode) {
                        visitor.addInternalName(typeInsnNode.desc);
                    } else if (currentInsn instanceof MultiANewArrayInsnNode mult) {
                        visitor.addDescriptor(mult.desc);
                    } else if (currentInsn instanceof FieldInsnNode fieldInsnNode) {
                        visitor.addDescriptor(fieldInsnNode.desc);
                    }
                }
            }
            for (TryCatchBlockNode node1 : methodNode.tryCatchBlocks) {
                visitor.addInternalName(node1.type);
            }
        }
        return visitor.getBinaryNames();
    }

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
        public void visitInvokeDynamicInsn(String name, String descriptor,
                                           Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            addDescriptor(descriptor);
            addHandle(bootstrapMethodHandle);
            for (Object o : bootstrapMethodArguments) {
                if (o instanceof Type t) {
                    addType(t);
                } else if (o instanceof Handle handle) {
                    addHandle(handle);
                } else if (o instanceof ConstantDynamic cd) {
                    addConstDyn(cd);
                }
            }
        }

        @Override
        public void visitLdcInsn(Object value) {
            if (value instanceof Type t) {
                addType(t);
            } else if (value instanceof Handle handle) {
                addHandle(handle);
            } else if (value instanceof ConstantDynamic cd) {
                addConstDyn(cd);
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
        binaryNames = new HashSet<>();
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

    private void addDescriptor(String descriptor) {
        InternalNameVisitor.visitDescriptor(descriptor, binaryNames);
    }

    private void addInternalName(String internalName) {
        if (internalName == null) {
            return;
        }
        InternalNameVisitor.visitInternalName(internalName, binaryNames);
    }

    private void addHandle(Handle handle) {
        addInternalName(handle.getOwner());
        addDescriptor(handle.getDesc());
    }

    private void addConstDyn(ConstantDynamic cd) {
        addDescriptor(cd.getDescriptor());
        addHandle(cd.getBootstrapMethod());
    }

    private void addType(Type t) {
        if (t.getSort() == Type.ARRAY) {
            addType(t.getElementType());
        } else if (t.getSort() == Type.OBJECT) {
            addInternalName(t.getInternalName());
        } else if (t.getSort() == Type.METHOD) {
            for (var i : t.getArgumentTypes()) {
                addType(i);
            }
            addType(t.getReturnType());
        }
    }
}

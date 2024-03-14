package pascal.taie.dumpjvm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import pascal.taie.World;
import pascal.taie.analysis.misc.IRDumper;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.frontend.newfrontend.Utils;
import pascal.taie.frontend.newfrontend.ssa.PhiStmt;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.ArrayLengthExp;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.BitwiseExp;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.ComparisonExp;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.DoubleLiteral;
import pascal.taie.ir.exp.FloatLiteral;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.InvokeInterface;
import pascal.taie.ir.exp.InvokeSpecial;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.InvokeVirtual;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.LongLiteral;
import pascal.taie.ir.exp.MethodHandle;
import pascal.taie.ir.exp.MethodType;
import pascal.taie.ir.exp.NegExp;
import pascal.taie.ir.exp.NewArray;
import pascal.taie.ir.exp.NewExp;
import pascal.taie.ir.exp.NewMultiArray;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.ShiftExp;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.UnaryExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Binary;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.Goto;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.InstanceOf;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.JumpStmt;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.LookupSwitch;
import pascal.taie.ir.stmt.Monitor;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.Nop;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.ir.stmt.SwitchStmt;
import pascal.taie.ir.stmt.TableSwitch;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.ir.stmt.Unary;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.annotation.AnnotationElement;
import pascal.taie.language.annotation.ArrayElement;
import pascal.taie.language.annotation.BooleanElement;
import pascal.taie.language.annotation.ClassElement;
import pascal.taie.language.annotation.DoubleElement;
import pascal.taie.language.annotation.Element;
import pascal.taie.language.annotation.EnumElement;
import pascal.taie.language.annotation.FloatElement;
import pascal.taie.language.annotation.IntElement;
import pascal.taie.language.annotation.LongElement;
import pascal.taie.language.annotation.StringElement;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Modifier;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.VoidType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BytecodeEmitter {
    private final int classWriterOptions;
    private final ClassWriter writer;

    private final Map<JClass, InnerClassNode> innerClassMap = Maps.newMap();

    public BytecodeEmitter(int classWriterOptions) {
        writer = new ClassWriter(classWriterOptions);
        this.classWriterOptions = classWriterOptions;
    }

    public BytecodeEmitter() {
        this(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    }

    public byte[] emit(JClass jClass) {
        if (jClass.getSuperClass() == null) {
            throw new IllegalArgumentException("Never dump java.lang.Class to bytecode");
        }
        Set<Modifier> modifiers = jClass.getModifiers();
        Set<Modifier> classPermittedModifiers = Set.of(Modifier.PUBLIC, Modifier.FINAL,
                Modifier.INTERFACE, Modifier.ABSTRACT, Modifier.SYNTHETIC,
                Modifier.ANNOTATION, Modifier.ENUM);
        Set<Modifier> classModifiers = modifiers.stream()
                .filter(classPermittedModifiers::contains)
                .collect(Collectors.toSet());
        int _access = Utils.toAsmModifier(classModifiers);
        int access = classModifiers.contains(Modifier.INTERFACE)
                ? _access
                : _access | Opcodes.ACC_SUPER;
        writer.visit(Opcodes.V17, access,
                getInternalName(jClass), null, getInternalName(jClass.getSuperClass()),
                jClass.getInterfaces()
                        .stream()
                        .map(this::getInternalName)
                        .toArray(String[]::new));
        noticeInnerClass(jClass);
        for (JMethod method : jClass.getDeclaredMethods()) {
            try {
                emitMethod(method);
            } catch (Exception e) {
                dumpDebugInfo();
                throw e;
            }
        }
        for (JField field : jClass.getDeclaredFields()) {
            writer.visitField(Utils.toAsmModifier(field.getModifiers()), field.getName(),
                    getDescriptor(field.getType()), null, null);
        }
        computeNest(jClass);
        for (InnerClassNode innerClass : innerClassMap.values()) {
            innerClass.accept(writer);
        }
        writer.visitEnd();
        return writer.toByteArray();
    }

    private void computeNest(JClass jClass) {
        JClass mostOuter = jClass;
        while (mostOuter.getOuterClass() != null) {
            mostOuter = mostOuter.getOuterClass();
        }
        if (mostOuter != jClass) {
            writer.visitNestHost(getInternalName(mostOuter));
        } else {
            // compute the closure of inner classes
            Set<JClass> closure = Sets.newSet();
            List<JClass> workList = new ArrayList<>(World.get().getClassHierarchy().getDirectInnerClassesOf(jClass));
            while (!workList.isEmpty()) {
                JClass inner = workList.remove(0);
                if (closure.add(inner)) {
                    workList.addAll(World.get().getClassHierarchy().getDirectInnerClassesOf(inner));
                }
            }
            for (JClass inner : closure) {
                writer.visitNestMember(getInternalName(inner));
            }
        }
    }

    private JMethod method;

    void emitMethod(JMethod method) {
        this.method = method;
        MethodVisitor mv = writer.visitMethod(
                Utils.toAsmModifier(method.getModifiers()), method.getName(),
                getDescriptor(method), null,
                method.getExceptions()
                        .stream()
                        .map(ClassType::getJClass)
                        .map(this::getInternalName)
                        .toArray(String[]::new));
        if (!method.isAbstract() && !method.isNative()) {
            IR ir = method.getIR();
            if (ir != null) {
                emitIR(ir, mv);
            }
        }

        for (Annotation annotation : method.getAnnotations()) {
            AnnotationVisitor annotationVisitor =
                    mv.visitAnnotation(annotation.getType(), true);
            emitAnnotation(annotationVisitor, annotation);
            annotationVisitor.visitEnd();
        }
        mv.visitEnd();
    }

    void emitAnnotation(AnnotationVisitor annotationVisitor, Annotation annotation) {
        for (Annotation.Entry entry : annotation.getElementEntries()) {
            String name = entry.name();
            Element ele = entry.element();
            emitElement(annotationVisitor, name, ele);
        }
        annotationVisitor.visitEnd();
    }

    void emitElement(AnnotationVisitor annotationVisitor, String name, Element ele) {
        if (ele instanceof AnnotationElement ae) {
            AnnotationVisitor av = annotationVisitor.visitAnnotation(name, ae.annotation().getType());
            emitAnnotation(av, ae.annotation());
            av.visitEnd();
        } else if (ele instanceof ArrayElement arr) {
            AnnotationVisitor av = annotationVisitor.visitArray(name);
            for (Element e : arr.elements()) {
                emitElement(av, null, e);
            }
            av.visitEnd();
        } else if (ele instanceof EnumElement ee) {
            annotationVisitor.visitEnum(name, ee.type(), ee.name());
        } else {
            annotationVisitor.visit(name, toObject(ele));
        }
    }

    void emitIR(IR ir, MethodVisitor mv) {
        Map<Stmt, LabelNode> labelMap = Maps.newMap();
        Map<Stmt, List<AbstractInsnNode>> insnMap = Maps.newMap();
        List<ExceptionEntry> exceptionEntries = ir.getExceptionEntries();
        for (ExceptionEntry entry : exceptionEntries) {
            createLabel(entry.start(), labelMap, insnMap);
            createLabel(entry.end(), labelMap, insnMap);
            createLabel(entry.handler(), labelMap, insnMap);
        }
        for (Stmt stmt : ir.getStmts()) {
            emitStmt(stmt, labelMap, insnMap);
        }
        InsnList list = new InsnList();
        for (Stmt stmt : ir.getStmts()) {
            List<AbstractInsnNode> insnList = insnMap.get(stmt);
            if (insnList != null) {
                for (AbstractInsnNode insn : insnList) {
                    list.add(insn);
                }
            }
        }
        mv.visitCode();
        list.accept(mv);
        for (ExceptionEntry entry : exceptionEntries) {
            LabelNode start = labelMap.get(entry.start());
            LabelNode end = labelMap.get(entry.end());
            LabelNode handler = labelMap.get(entry.handler());
            ClassType type = entry.catchType();
            mv.visitTryCatchBlock(start.getLabel(), end.getLabel(),
                    handler.getLabel(), getInternalName(type.getJClass()));
        }
        mv.visitMaxs(0, 0);
    }

    void emitStmt(Stmt stmt, Map<Stmt, LabelNode> labelMap, Map<Stmt, List<AbstractInsnNode>> insnMap) {
        List<AbstractInsnNode> nodeList = insnMap.computeIfAbsent(stmt, (stmt1) -> new ArrayList<>());
        if (stmt instanceof DefinitionStmt<?, ?>) {
            if (stmt instanceof Invoke invoke) {
                InvokeExp invokeExp = invoke.getInvokeExp();
                emitInvoke(invokeExp, nodeList);
                if (invoke.getResult() != null) {
                    nodeList.add(emitStore(invoke.getResult()));
                } else {
                    Type ret;
                    if (invokeExp instanceof InvokeDynamic dyn) {
                        ret = dyn.getMethodType().getReturnType();
                    } else {
                        ret = invokeExp.getMethodRef().resolve().getReturnType();
                    }
                    if (!(ret instanceof VoidType)) {
                        switch (ret.getName()) {
                            case "double", "long" -> nodeList.add(new InsnNode(Opcodes.POP2));
                            default -> nodeList.add(new InsnNode(Opcodes.POP));
                        }
                    }
                }
            } else if (stmt instanceof New _new) {
                NewExp newExp = _new.getRValue();
                ReferenceType type = newExp.getType();
                if (type instanceof ClassType classType) {
                    nodeList.add(new TypeInsnNode(Opcodes.NEW, getInternalName(classType.getJClass())));
                } else if (newExp instanceof NewMultiArray newMultiArray) {
                    ArrayType arrayType = newMultiArray.getType();
                    for (Var length : newMultiArray.getLengths()) {
                        emitMayConstLoad(length, nodeList);
                    }
                    nodeList.add(new MultiANewArrayInsnNode(getDescriptor(arrayType), newMultiArray.getLengthCount()));
                } else if (newExp instanceof NewArray newArray) {
                    ArrayType arrayType = newArray.getType();
                    emitMayConstLoad(newArray.getLength(), nodeList);
                    if (arrayType.elementType() instanceof PrimitiveType) {
                        nodeList.add(new IntInsnNode(Opcodes.NEWARRAY, switch (arrayType.elementType().getName()) {
                            case "int" -> Opcodes.T_INT;
                            case "long" -> Opcodes.T_LONG;
                            case "float" -> Opcodes.T_FLOAT;
                            case "double" -> Opcodes.T_DOUBLE;
                            case "byte" -> Opcodes.T_BYTE;
                            case "char" -> Opcodes.T_CHAR;
                            case "short" -> Opcodes.T_SHORT;
                            case "boolean" -> Opcodes.T_BOOLEAN;
                            default ->
                                    throw new IllegalArgumentException("Unknown primitive type: " + arrayType.elementType());
                        }));
                    } else if (arrayType.elementType() instanceof ReferenceType) {
                        nodeList.add(new TypeInsnNode(Opcodes.ANEWARRAY,
                                getInternalName(arrayType.elementType())));
                    }
                } else {
                    throw new IllegalArgumentException("Unknown reference type: " + type);
                }
                nodeList.add(emitStore(_new.getLValue()));
            } else if (stmt instanceof Cast cast) {
                Var v = cast.getRValue().getValue();
                if (cast.getRValue().getType() instanceof PrimitiveType p) {
                    if (cast.getRValue().getCastType() == v.getType()) {
                        // skip, but need to emit load and store
                        emitMayConstLoad(v, nodeList);
                        nodeList.add(emitStore(cast.getLValue()));
                        return;
                    }
                    // emit i2d, i2f ...
                    emitMayConstLoad(v, nodeList);
                    int[][] table = {
                            {Opcodes.I2B, Opcodes.I2C, Opcodes.I2S, -1, Opcodes.I2L, Opcodes.I2F, Opcodes.I2D},
                            {-1, -1, -1, Opcodes.L2I, -1, Opcodes.L2F, Opcodes.L2D},
                            {-1, -1, -1, Opcodes.F2I, Opcodes.F2L, -1, Opcodes.F2D},
                            {-1, -1, -1, Opcodes.D2I, Opcodes.D2L, Opcodes.D2F, -1}};
                    int op = table[getSize(v.getType())][getFineSize(p)];
                    assert op != -1;
                    nodeList.add(new InsnNode(op));
                } else {
                    emitMayConstLoad(v, nodeList);
                    nodeList.add(new TypeInsnNode(Opcodes.CHECKCAST,
                            getInternalName(cast.getRValue().getCastType())));
                }
                nodeList.add(emitStore(cast.getLValue()));
            } else if (stmt instanceof InstanceOf instanceOf) {
                Var v = instanceOf.getRValue().getValue();
                emitMayConstLoad(v, nodeList);
                nodeList.add(new TypeInsnNode(Opcodes.INSTANCEOF,
                        getInternalName(instanceOf.getRValue().getCheckedType())));
                nodeList.add(emitStore(instanceOf.getLValue()));
            } else if (stmt instanceof StoreField storeField) {
                int op = storeField.isStatic() ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD;
                if (!storeField.isStatic()) {
                    InstanceFieldAccess instanceFieldAccess = (InstanceFieldAccess) storeField.getFieldAccess();
                    nodeList.add(emitLoad(instanceFieldAccess.getBase()));
                }
                emitMayConstLoad(storeField.getRValue(), nodeList);
                nodeList.add(emitField(op, storeField.getFieldRef()));
            } else if (stmt instanceof LoadField loadField) {
                int op = loadField.isStatic() ? Opcodes.GETSTATIC : Opcodes.GETFIELD;
                if (!loadField.isStatic()) {
                    InstanceFieldAccess instanceFieldAccess = (InstanceFieldAccess) loadField.getFieldAccess();
                    nodeList.add(emitLoad(instanceFieldAccess.getBase()));
                }
                nodeList.add(emitField(op, loadField.getFieldRef()));
                nodeList.add(emitStore(loadField.getLValue()));
            } else if (stmt instanceof AssignLiteral assignLiteral) {
                Literal literal = assignLiteral.getRValue();
                emitConst(literal, nodeList);
                nodeList.add(emitStore(assignLiteral.getLValue()));
            } else if (stmt instanceof Copy copy) {
                Var rValue = copy.getRValue();
                emitMayConstLoad(rValue, nodeList);
                nodeList.add(emitStore(copy.getLValue()));
            } else if (stmt instanceof PhiStmt) {
                throw new IllegalArgumentException("Phi statement is not supported");
            } else if (stmt instanceof LoadArray loadArray) {
                nodeList.add(emitLoad(loadArray.getArrayAccess().getBase()));
                emitMayConstLoad(loadArray.getArrayAccess().getIndex(), nodeList);
                nodeList.add(emitArrayInsn(loadArray));
                nodeList.add(emitStore(loadArray.getLValue()));
            } else if (stmt instanceof StoreArray storeArray) {
                nodeList.add(emitLoad(storeArray.getLValue().getBase()));
                emitMayConstLoad(storeArray.getLValue().getIndex(), nodeList);
                emitMayConstLoad(storeArray.getRValue(), nodeList);
                nodeList.add(emitArrayInsn(storeArray));
            } else if (stmt instanceof Binary binary) {
                BinaryExp binaryExp = binary.getRValue();
                emitBinaryExp(binaryExp, nodeList);
                nodeList.add(emitStore(binary.getLValue()));
            } else if (stmt instanceof Unary unary) {
                UnaryExp unaryExp = unary.getRValue();
                if (unaryExp instanceof ArrayLengthExp arrayLengthExp) {
                    nodeList.add(emitLoad(arrayLengthExp.getBase()));
                    nodeList.add(new InsnNode(Opcodes.ARRAYLENGTH));
                } else if (unaryExp instanceof NegExp negExp) {
                    int[] table = {Opcodes.INEG, Opcodes.LNEG, Opcodes.FNEG, Opcodes.DNEG};
                    emitMayConstLoad(negExp.getValue(), nodeList);
                    nodeList.add(new InsnNode(table[getSize(negExp.getValue().getType())]));
                }
                nodeList.add(emitStore(unary.getLValue()));
            } else {
                throw new IllegalArgumentException("Unknown definition statement: " + stmt);
            }
        } else if (stmt instanceof Nop) {
            // skip;
        } else if (stmt instanceof Return r) {
            Var ret = r.getValue();
            emitReturn(ret, nodeList);
        } else if (stmt instanceof JumpStmt) {
            if (stmt instanceof If _if) {
                ConditionExp cond = _if.getCondition();
                LabelNode target = createLabel(_if.getTarget(), labelMap, insnMap);
                emitCond(cond, target, nodeList);
            } else if (stmt instanceof Goto _goto) {
                LabelNode target = createLabel(_goto.getTarget(), labelMap, insnMap);
                nodeList.add(new JumpInsnNode(Opcodes.GOTO, target));
            } else if (stmt instanceof SwitchStmt switchStmt) {
                nodeList.add(emitLoad(switchStmt.getVar()));
                LabelNode defaultTarget = createLabel(switchStmt.getDefaultTarget(), labelMap, insnMap);
                List<LabelNode> targets = switchStmt.getTargets()
                        .stream()
                        .map(target -> createLabel(target, labelMap, insnMap))
                        .toList();
                if (stmt instanceof TableSwitch tableSwitch) {
                    TableSwitchInsnNode insn = new TableSwitchInsnNode(
                            tableSwitch.getLowIndex(),
                            tableSwitch.getHighIndex(),
                            defaultTarget,
                            targets.toArray(new LabelNode[0]));
                    nodeList.add(insn);
                } else if (stmt instanceof LookupSwitch) {
                    LookupSwitchInsnNode insn = new LookupSwitchInsnNode(
                            defaultTarget,
                            switchStmt.getCaseValues().stream().mapToInt(i -> i).toArray(),
                            targets.toArray(new LabelNode[0]));
                    nodeList.add(insn);
                } else {
                    throw new IllegalArgumentException("Unknown switch statement: " + stmt);
                }
            } else {
                throw new IllegalArgumentException("Unknown jump statement: " + stmt);
            }
        } else if (stmt instanceof Catch _catch) {
            nodeList.add(emitStore(_catch.getExceptionRef()));
        } else if (stmt instanceof Monitor monitor) {
            int op = monitor.isEnter() ? Opcodes.MONITORENTER : Opcodes.MONITOREXIT;
            nodeList.add(emitLoad(monitor.getObjectRef()));
            nodeList.add(new InsnNode(op));
        } else if (stmt instanceof Throw throwStmt) {
            nodeList.add(emitLoad(throwStmt.getExceptionRef()));
            nodeList.add(new InsnNode(Opcodes.ATHROW));
        } else {
            throw new IllegalArgumentException("Unknown statement: " + stmt);
        }
    }

    LabelNode createLabel(Stmt stmt, Map<Stmt, LabelNode> labelMap,
                          Map<Stmt, List<AbstractInsnNode>> insnMap) {
        if (!labelMap.containsKey(stmt)) {
            labelMap.put(stmt, new LabelNode());
            List<AbstractInsnNode> insnList = insnMap.computeIfAbsent(stmt, __ -> new ArrayList<>());
            // insert label to the beginning of the list
            // insnList may be not empty
            insnList.add(0, labelMap.get(stmt));
        }
        return labelMap.get(stmt);
    }

    void emitInvoke(InvokeExp invokeExp, List<AbstractInsnNode> nodeList) {
        if (invokeExp instanceof InvokeDynamic invokeDynamic) {
            Handle handle = getHandle(invokeDynamic.getHandle());
            Object[] bootstrapArgs = invokeDynamic.getBootstrapArgs()
                    .stream()
                    .map(this::toObject)
                    .toArray();
            String desc = getDescriptor(invokeDynamic.getMethodType());
            String name = invokeDynamic.getMethodName();
            // push args
            for (Var arg : invokeDynamic.getArgs()) {
                emitMayConstLoad(arg, nodeList);
            }
            nodeList.add(new InvokeDynamicInsnNode(name, desc, handle, bootstrapArgs));
        } else {
            MethodRef ref = invokeExp.getMethodRef();
            if (!ref.isStatic()) {
                InvokeInstanceExp invokeInstanceExp = (InvokeInstanceExp) invokeExp;
                // push this
                nodeList.add(emitLoad(invokeInstanceExp.getBase()));
            }
            // push args
            for (Var arg : invokeExp.getArgs()) {
                emitMayConstLoad(arg, nodeList);
            }
            int op = getInvokeOp(invokeExp);
            nodeList.add(new MethodInsnNode(op,
                    getInternalName(ref.getDeclaringClass()),
                    ref.getName(),
                    getDescriptor(ref),
                    ref.isDeclaredInInterface()));

        }
    }

    void emitBinaryExp(BinaryExp binaryExp, List<AbstractInsnNode> nodeList) {
        Var op1 = binaryExp.getOperand1();
        Var op2 = binaryExp.getOperand2();
        emitMayConstLoad(op1, nodeList);
        emitMayConstLoad(op2, nodeList);
        BinaryExp.Op op = binaryExp.getOperator();
        int size = getSize(op1.getType());
        if (op instanceof ArithmeticExp.Op aop) {
            nodeList.add(new InsnNode(selectArithmeticOp(switch (aop) {
                case ADD -> 0;
                case SUB -> 1;
                case MUL -> 2;
                case DIV -> 3;
                case REM -> 4;
            }, size)));
        } else if (op instanceof BitwiseExp.Op bop) {
            nodeList.add(new InsnNode(selectBitwiseOp(switch (bop) {
                case AND -> 0;
                case OR -> 1;
                case XOR -> 2;
            }, size)));
        } else if (op instanceof ShiftExp.Op sop) {
            nodeList.add(new InsnNode(selectBitwiseOp(switch (sop) {
                case SHL -> 3;
                case SHR -> 4;
                case USHR -> 5;
            }, size)));
        } else if (op instanceof ComparisonExp.Op comprOp) {
            nodeList.add(new InsnNode(selectComparsionOp(switch (comprOp) {
                case CMP -> 0;
                case CMPL -> 1;
                case CMPG -> 2;
            }, size)));
        } else if (op instanceof ConditionExp.Op) {
            throw new IllegalArgumentException("Condition expression in (v := exp) is not supported");
        } else {
            throw new IllegalArgumentException("Unknown binary expression: " + binaryExp);
        }
    }

    static int selectArithmeticOp(int what, int size) {
        int[][] table = {
                {Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD},
                {Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB, Opcodes.DSUB},
                {Opcodes.IMUL, Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL},
                {Opcodes.IDIV, Opcodes.LDIV, Opcodes.FDIV, Opcodes.DDIV},
                {Opcodes.IREM, Opcodes.LREM, Opcodes.FREM, Opcodes.DREM}};
        return table[what][size];
    }

    static int selectBitwiseOp(int what, int size) {
        int[][] table = {
                {Opcodes.IAND, Opcodes.LAND},
                {Opcodes.IOR, Opcodes.LOR},
                {Opcodes.IXOR, Opcodes.LXOR},
                {Opcodes.ISHL, Opcodes.LSHL},
                {Opcodes.ISHR, Opcodes.LSHR},
                {Opcodes.IUSHR, Opcodes.LUSHR}};
        return table[what][size];
    }

    static int selectComparsionOp(int what, int size) {
        int[][] table = {
                {-1, Opcodes.LCMP},
                {-1, -1, Opcodes.FCMPL, Opcodes.DCMPL},
                {-1, -1, Opcodes.FCMPG, Opcodes.DCMPG}};
        return table[what][size];
    }

    static int getSize(Type type) {
        if (type instanceof PrimitiveType primitiveType) {
            return switch (primitiveType.getName()) {
                case "int", "short", "byte", "boolean", "char" -> 0;
                case "long" -> 1;
                case "float" -> 2;
                case "double" -> 3;
                default ->
                        throw new IllegalArgumentException("Unknown primitive type: " + primitiveType);
            };
        } else {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    private static int getInvokeOp(InvokeExp invokeExp) {
        int op;
        if (invokeExp instanceof InvokeStatic) {
            op = Opcodes.INVOKESTATIC;
        } else if (invokeExp instanceof InvokeSpecial) {
            op = Opcodes.INVOKESPECIAL;
        } else if (invokeExp instanceof InvokeVirtual) {
            op = Opcodes.INVOKEVIRTUAL;
        } else if (invokeExp instanceof InvokeInterface) {
            op = Opcodes.INVOKEINTERFACE;
        } else {
            throw new IllegalArgumentException("Unknown invoke expression: " + invokeExp);
        }
        return op;
    }

    void emitReturn(Var ret, List<AbstractInsnNode> nodeList) {
        if (ret == null) {
            nodeList.add(new InsnNode(Opcodes.RETURN));
        } else {
            emitMayConstLoad(ret, nodeList);
            Type type = ret.getType();
            if (type instanceof PrimitiveType) {
                nodeList.add(new InsnNode(switch (type.getName()) {
                    case "int", "short", "byte", "boolean", "char" -> Opcodes.IRETURN;
                    case "long" -> Opcodes.LRETURN;
                    case "float" -> Opcodes.FRETURN;
                    case "double" -> Opcodes.DRETURN;
                    default ->
                            throw new IllegalArgumentException("Unknown primitive type: " + type);
                }));
            } else if (type instanceof VoidType) {
                throw new IllegalArgumentException("Cannot return void type");
            } else if (type instanceof ReferenceType) {
                nodeList.add(new InsnNode(Opcodes.ARETURN));
            } else {
                throw new IllegalArgumentException("Unknown return type: " + type);
            }
        }
    }

    void emitConst(Literal literal, List<AbstractInsnNode> insnNodes) {
        if (literal instanceof IntLiteral intLiteral) {
            int val = intLiteral.getValue();
            if (val == -1) {
                insnNodes.add(new InsnNode(Opcodes.ICONST_M1));
            } else if (val >= 0 && val <= 5) {
                insnNodes.add(new InsnNode(Opcodes.ICONST_0 + val));
            } else if (val >= Byte.MIN_VALUE && val <= Byte.MAX_VALUE) {
                insnNodes.add(new IntInsnNode(Opcodes.BIPUSH, val));
            } else if (val >= Short.MIN_VALUE && val <= Short.MAX_VALUE) {
                insnNodes.add(new IntInsnNode(Opcodes.SIPUSH, val));
            } else {
                insnNodes.add(new LdcInsnNode(val));
            }
        } else if (literal instanceof NullLiteral) {
            insnNodes.add(new InsnNode(Opcodes.ACONST_NULL));
        } else {
            insnNodes.add(new LdcInsnNode(toObject(literal)));
        }
    }

    void emitMayConstLoad(Var var, List<AbstractInsnNode> insnNodes) {
        if (var.isConst()) {
            emitConst(var.getConstValue(), insnNodes);
        } else {
            insnNodes.add(emitLoad(var));
        }
    }

    AbstractInsnNode emitLoad(Var var) {
        Type type = var.getType();
        int index = computeIndex(var);
        if (type instanceof PrimitiveType primitiveType) {
            return switch (primitiveType.getName()) {
                case "int", "short", "byte", "boolean", "char" ->
                        new VarInsnNode(Opcodes.ILOAD, index);
                case "long" -> new VarInsnNode(Opcodes.LLOAD, index);
                case "float" -> new VarInsnNode(Opcodes.FLOAD, index);
                case "double" -> new VarInsnNode(Opcodes.DLOAD, index);
                default ->
                        throw new IllegalArgumentException("Unknown primitive type: " + primitiveType);
            };
        } else if (type instanceof ClassType) {
            return new VarInsnNode(Opcodes.ALOAD, index);
        } else if (type instanceof ArrayType) {
            return new VarInsnNode(Opcodes.ALOAD, index);
        } else if (type instanceof NullType) {
            return new InsnNode(Opcodes.ACONST_NULL);
        } else {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    AbstractInsnNode emitStore(Var var) {
        Type type = var.getType();
        int index = computeIndex(var);
        if (type instanceof PrimitiveType primitiveType) {
            return switch (primitiveType.getName()) {
                case "int", "short", "byte", "boolean", "char" ->
                        new VarInsnNode(Opcodes.ISTORE, index);
                case "long" -> new VarInsnNode(Opcodes.LSTORE, index);
                case "float" -> new VarInsnNode(Opcodes.FSTORE, index);
                case "double" -> new VarInsnNode(Opcodes.DSTORE, index);
                default ->
                        throw new IllegalArgumentException("Unknown primitive type: " + primitiveType);
            };
        } else if (type instanceof ClassType) {
            return new VarInsnNode(Opcodes.ASTORE, index);
        } else if (type instanceof ArrayType) {
            return new VarInsnNode(Opcodes.ASTORE, index);
        } else {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    AbstractInsnNode emitArrayInsn(Stmt stmt) {
        ArrayType arrayType;
        boolean isLoad;
        if (stmt instanceof LoadArray loadArray) {
            arrayType = (ArrayType) loadArray.getArrayAccess().getBase().getType();
            isLoad = true;
        } else if (stmt instanceof StoreArray storeArray) {
            arrayType = (ArrayType) storeArray.getArrayAccess().getBase().getType();
            isLoad = false;
        } else {
            throw new IllegalArgumentException("Unknown array statement: " + stmt);
        }
        int fineSize = getFineSize(arrayType.elementType());
        int[][] table = {
                {Opcodes.BALOAD, Opcodes.BASTORE},
                {Opcodes.SALOAD, Opcodes.SASTORE},
                {Opcodes.CALOAD, Opcodes.CASTORE},
                {Opcodes.IALOAD, Opcodes.IASTORE},
                {Opcodes.LALOAD, Opcodes.LASTORE},
                {Opcodes.FALOAD, Opcodes.FASTORE},
                {Opcodes.DALOAD, Opcodes.DASTORE},
                {Opcodes.AALOAD, Opcodes.AASTORE}};
        return new InsnNode(table[fineSize][isLoad ? 0 : 1]);
    }

    private static int getFineSize(Type t) {
        int fineSize;
        if (t instanceof PrimitiveType p) {
            fineSize = switch (p.getName()) {
                case "byte", "boolean" -> 0;
                case "short" -> 1;
                case "char" -> 2;
                case "int" -> 3;
                case "long" -> 4;
                case "float" -> 5;
                case "double" -> 6;
                default -> throw new IllegalArgumentException("Unknown primitive type: " + p);
            };
        } else {
            fineSize = 7;
        }
        return fineSize;
    }

    FieldInsnNode emitField(int op, FieldRef ref) {
        JClass jClass = ref.getDeclaringClass();
        String owner = getInternalName(jClass);
        String name = ref.getName();
        String desc = getDescriptor(ref.getType());
        return new FieldInsnNode(op, owner, name, desc);
    }

    int computeIndex(Var var) {
        if (method.getIR().isParam(var)) {
            return var.getIndex();
        } else {
            return var.getIndex() * 2;
        }
    }

    void emitCond(ConditionExp cond, LabelNode target, List<AbstractInsnNode> nodeList) {
        Var op1 = cond.getOperand1();
        Var op2 = cond.getOperand2();
        emitMayConstLoad(op1, nodeList);
        emitMayConstLoad(op2, nodeList);
        ConditionExp.Op op = cond.getOperator();
        boolean isRef = op1.getType() instanceof ReferenceType;
        int opCode = switch (op) {
            case EQ -> isRef ? Opcodes.IF_ACMPEQ : Opcodes.IF_ICMPEQ;
            case NE -> isRef ? Opcodes.IF_ACMPNE : Opcodes.IF_ICMPNE;
            case LT -> Opcodes.IF_ICMPLT;
            case GE -> Opcodes.IF_ICMPGE;
            case GT -> Opcodes.IF_ICMPGT;
            case LE -> Opcodes.IF_ICMPLE;
        };
        nodeList.add(new JumpInsnNode(opCode, target));
    }

    private String getDescriptor(MethodRef ref) {
        StringBuilder builder = new StringBuilder("(");
        for (Type type : ref.getParameterTypes()) {
            builder.append(getDescriptor(type));
        }
        builder.append(")");
        builder.append(getDescriptor(ref.getReturnType()));
        return builder.toString();
    }

    private String getDescriptor(JMethod method) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < method.getParamCount(); ++i) {
            sb.append(getDescriptor(method.getParamType(i)));
        }
        sb.append(")");
        sb.append(getDescriptor(method.getReturnType()));
        return sb.toString();
    }

    private String getDescriptor(MethodType methodType) {
        StringBuilder sb = new StringBuilder("(");
        for (Type type : methodType.getParamTypes()) {
            sb.append(getDescriptor(type));
        }
        sb.append(")");
        sb.append(getDescriptor(methodType.getReturnType()));
        return sb.toString();
    }

    String getDescriptor(Type type) {
        if (type instanceof ClassType ct) {
            noticeInnerClass(ct.getJClass());
        }
        return computeDescriptor(type);
    }

    static String computeDescriptor(Type type) {
        if (type instanceof ClassType) {
            return "L" + type.getName().replace('.', '/') + ";";
        } else if (type instanceof PrimitiveType) {
            return switch (type.getName()) {
                case "int" -> "I";
                case "long" -> "J";
                case "short" -> "S";
                case "byte" -> "B";
                case "char" -> "C";
                case "float" -> "F";
                case "double" -> "D";
                case "boolean" -> "Z";
                default -> throw new IllegalArgumentException("Unknown primitive type: " + type);
            };
        } else if (type instanceof VoidType) {
            return "V";
        } else if (type instanceof ArrayType arrayType) {
            return "[" + computeDescriptor(arrayType.elementType());
        } else {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    private void noticeInnerClass(JClass jClass) {
        if (jClass.getOuterClass() != null) {
            innerClassMap.computeIfAbsent(jClass, (__) -> {
                JClass outer = jClass.getOuterClass();
                return new InnerClassNode(computeInternalName(jClass), computeInternalName(outer),
                        computeRealSimpleName(jClass), Utils.toAsmModifier(jClass.getModifiers()));
            });
        }
    }

    String getInternalName(JClass jClass) {
        noticeInnerClass(jClass);
        return computeInternalName(jClass);
    }

    static String computeInternalName(JClass jClass) {
        return jClass.getName().replace('.', '/');
    }

    static String computeRealSimpleName(JClass jClass) {
        String name = jClass.getSimpleName();
        return name.substring(name.lastIndexOf('$') + 1);
    }

    String getInternalName(Type type) {
        if (type instanceof ClassType classType) {
            return getInternalName(classType.getJClass());
        } else if (type instanceof ArrayType arrayType) {
            return "[" + getDescriptor(arrayType.elementType());
        } else {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    Handle getHandle(MethodHandle handle) {
        MethodHandle.Kind kind = handle.getKind();
        int tag = switch (kind) {
            case REF_getField -> Opcodes.H_GETFIELD;
            case REF_getStatic -> Opcodes.H_GETSTATIC;
            case REF_putField -> Opcodes.H_PUTFIELD;
            case REF_putStatic -> Opcodes.H_PUTSTATIC;
            case REF_invokeVirtual -> Opcodes.H_INVOKEVIRTUAL;
            case REF_invokeStatic -> Opcodes.H_INVOKESTATIC;
            case REF_invokeSpecial -> Opcodes.H_INVOKESPECIAL;
            case REF_newInvokeSpecial -> Opcodes.H_NEWINVOKESPECIAL;
            case REF_invokeInterface -> Opcodes.H_INVOKEINTERFACE;
        };
        String owner;
        String name;
        String desc;
        boolean isInterface;
        if (handle.isMethodRef()) {
            owner = getInternalName(handle.getMethodRef().getDeclaringClass());
            name = handle.getMethodRef().getName();
            desc = getDescriptor(handle.getMethodRef());
            isInterface = handle.getMethodRef().getDeclaringClass().isInterface();
        } else {
            owner = getInternalName(handle.getFieldRef().getDeclaringClass());
            name = handle.getFieldRef().getName();
            desc = getDescriptor(handle.getFieldRef().getType());
            isInterface = handle.getFieldRef().getDeclaringClass().isInterface();
        }
        return new Handle(tag, owner, name, desc, isInterface);
    }

    Object toObject(Literal literal) {
        if (literal instanceof NullLiteral) {
            return null;
        } else if (literal instanceof IntLiteral intLiteral) {
            return intLiteral.getValue();
        } else if (literal instanceof LongLiteral integerLiteral) {
            return integerLiteral.getValue();
        } else if (literal instanceof DoubleLiteral doubleLiteral) {
            return doubleLiteral.getValue();
        } else if (literal instanceof FloatLiteral floatLiteral) {
            return floatLiteral.getValue();
        } else if (literal instanceof MethodType methodType) {
            return org.objectweb.asm.Type.getType(getDescriptor(methodType));
        } else if (literal instanceof StringLiteral stringLiteral) {
            return stringLiteral.getString();
        } else if (literal instanceof MethodHandle methodHandle) {
            return getHandle(methodHandle);
        } else if (literal instanceof ClassLiteral classLiteral) {
            return org.objectweb.asm.Type.getType(getDescriptor(classLiteral.getTypeValue()));
        } else {
            throw new IllegalArgumentException("Unknown literal: " + literal);
        }
    }

    Object toObject(Element element) {
        if (element instanceof BooleanElement b) {
            return b.value();
        } else if (element instanceof IntElement i) {
            return i.value();
        } else if (element instanceof LongElement l) {
            return l.value();
        } else if (element instanceof FloatElement f) {
            return f.value();
        } else if (element instanceof DoubleElement d) {
            return d.value();
        } else if (element instanceof StringElement s) {
            return s.value();
        } else if (element instanceof ClassElement c) {
            return org.objectweb.asm.Type.getType(c.classDescriptor());
        }  else {
            throw new IllegalArgumentException("Unknown element: " + element);
        }
    }

    void dumpDebugInfo() {
        IRDumper dumper = new IRDumper(AnalysisConfig.of(IRDumper.ID));
        dumper.analyze(method.getDeclaringClass());
        if ((this.classWriterOptions & ClassWriter.COMPUTE_FRAMES) != 0) {
            // re-emit without COMPUTE_FRAMES for debugging
            BytecodeEmitter emitter = new BytecodeEmitter(ClassWriter.COMPUTE_MAXS);
            byte[] data = emitter.emit(method.getDeclaringClass());
            try {
                Path p = Path.of("output", "bytecode-dbg").resolve(
                        method.getDeclaringClass()
                                .getName().replace(".", "/") + ".class");
                Files.createDirectories(p.getParent());
                Files.write(p, data);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}

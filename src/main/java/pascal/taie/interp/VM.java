package pascal.taie.interp;

import pascal.taie.World;
import pascal.taie.frontend.newfrontend.java.NewFrontendException;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.ArrayLengthExp;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.DoubleLiteral;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.FloatLiteral;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.InstanceOfExp;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.InvokeInterface;
import pascal.taie.ir.exp.InvokeSpecial;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.LongLiteral;
import pascal.taie.ir.exp.MethodType;
import pascal.taie.ir.exp.NegExp;
import pascal.taie.ir.exp.NewArray;
import pascal.taie.ir.exp.NewExp;
import pascal.taie.ir.exp.NewInstance;
import pascal.taie.ir.exp.NewMultiArray;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.StaticFieldAccess;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.Goto;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.Monitor;
import pascal.taie.ir.stmt.Nop;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.SwitchStmt;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.MethodNames;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;

public class VM {
    final World world;

    final Stack<Frame> frames;

    final Map<ClassType, JClassObject> classObjs;

    public VM() {
        world = World.get();
        frames = new Stack<>();
        classObjs = Maps.newMap();
    }

    public void exec() {
        JMethod main = world.getMainMethod();
        execIR(main.getIR(), Frame.mkNewFrame());
    }

    public JValue execIR(IR ir, Frame f) {
        frames.push(f);
        while (f.getPc() >= 0) {
            Stmt stmt = ir.getStmt(f.getPc());
            try {
                execStmt(stmt, ir, f);
            } catch (InterpreterException | NewFrontendException e) {
                throw e;
            } catch (Exception e) {
                Exception exception;
                if (e instanceof ClientException e1) {
                    exception = e1.internal;
                } else {
                    exception = e;
                }
                int currentPc = f.getPc();
                ClassType t;
                JObject aCatchObj;
                if (exception instanceof ClientDefinedException cde) {
                    t = cde.inner.getType();
                    aCatchObj = cde.inner;
                } else {
                    t = Utils.fromJVMClass(exception.getClass());
                    aCatchObj = new JVMObject((JVMClassObject) loadClass(t), exception);
                }
                Optional<ExceptionEntry> exceptionEntry = ir.getExceptionEntries().stream()
                        .filter(entry -> World.get().getTypeSystem().isSubtype(entry.catchType(), t) &&
                                currentPc >= entry.start().getIndex() &&
                                currentPc < entry.end().getIndex())
                        .findAny();
                if (exceptionEntry.isPresent()) {
                    Catch aCatch = exceptionEntry.get().handler();
                    f.getRegs().put(aCatch.getExceptionRef(), aCatchObj);
                    f.setPc(aCatch.getIndex());
                } else {
                    // TODO: this approach will cause client code catch some internal exception
                    //       correct this by explicitly throw all jvm exceptions

                    // throw to outer frame
                    frames.pop();
                    throw new ClientException(exception);
                }
            }
        }
        frames.pop();
        return f.getRets();
    }

    public JClassObject loadClass(ClassType t) {
        if (classObjs.containsKey(t)) {
            return classObjs.get(t);
        } else {
            JClassObject obj;
            if (Utils.isJVMClass(t)) {
                obj = new JVMClassObject(t);
                classObjs.put(t, obj);
            } else {
                for (var i : t.getJClass().getInterfaces()) {
                    if (!classObjs.containsKey(i.getType())) {
                        loadClass(i.getType());
                    }
                }
                JClass superClass = t.getJClass().getSuperClass();
                assert superClass != null; // java.lang.Object should not be loaded here
                if (!classObjs.containsKey(superClass.getType())) {
                    loadClass(superClass.getType());
                }
                JMethod clinit = t.getJClass().getClinit();
                obj = new JClassObject(t);
                classObjs.put(t, obj);
                if (clinit != null) {
                    execIR(clinit.getIR(), Frame.mkNewFrame());
                }
            }
            return obj;
        }
    }

    public void execStmt(Stmt stmt, IR ir, Frame f) {
        if (stmt instanceof Nop || stmt instanceof Catch || stmt instanceof Monitor) {
            // do nothing
        }
        else if (stmt instanceof Return r) {
            f.markEnd();
            if (r.getValue() != null) {
                f.setRets(evalExp(r.getValue(), ir, f));
            }
            return;
        } else if (stmt instanceof DefinitionStmt<?, ?> a) {
            LValue l = a.getLValue();
            RValue r = a.getRValue();
            JValue rValue = evalExp(r, ir, f);
            if (l != null) {
                if (l instanceof Var v) {
                    f.getRegs().put(v, rValue);
                } else if (l instanceof InstanceFieldAccess fa) {
                    FieldRef ref = fa.getFieldRef();
                    JObject obj  = JValue.getObject(f.getRegs().get(fa.getBase()));
                    obj.setField(this, ref, rValue);
                } else if (l instanceof StaticFieldAccess sfa) {
                    JClassObject obj = loadClass(sfa.getFieldRef().resolve()
                            .getDeclaringClass().getType());
                    obj.setStaticField(this, sfa.getFieldRef(), rValue);
                } else if (l instanceof ArrayAccess aa) {
                    JArray array = JValue.getJArray(evalExp(aa.getBase(), ir, f));
                    int idx = JValue.getInt(evalExp(aa.getIndex(), ir, f));
                    array.setIdx(idx, rValue);
                } else {
                    throw new InterpreterException();
                }
            }
        } else if (stmt instanceof Goto g) {
            f.setPc(g.getTarget().getIndex());
            return;
        } else if (stmt instanceof If i) {
            if (JValue.getInt(evalExp(i.getCondition(), ir, f)) == Utils.INT_TRUE) {
                f.setPc(i.getTarget().getIndex());
                return;
            }
        } else if (stmt instanceof Throw t) {
            Var v = t.getExceptionRef();
            JObject value = JValue.getObject(evalExp(v, ir, f));
            if (value instanceof JVMObject) {
                throw new ClientException((Exception) value.toJVMObj());
            } else {
                throw new ClientException(new ClientDefinedException(value));
            }
        } else if (stmt instanceof SwitchStmt s) {
            JValue v = evalExp(s.getVar(), ir, f);
            int i = JValue.getInt(v);
            Stmt target = s.getCaseTargets()
                    .stream()
                    .filter(e -> e.first() == i)
                    .findAny()
                    .orElse(new Pair<>(i, s.getDefaultTarget()))
                    .second();
            f.setPc(target.getIndex());
            return;
        } else {
            throw new InterpreterException();
        }

        if (f.getPc() < ir.getStmts().size()) {
            f.setPc(f.getPc() + 1);
        } else {
            f.markEnd();
        }
    }

    private JValue invokeStatic(InvokeStatic is, IR ir, Frame f) {
        List<JValue> args = is.getArgs().stream()
                .map(e -> evalExp(e, ir, f))
                .toList();
        JMethod mtd = is.getMethodRef().resolve();
        return loadClass(mtd.getDeclaringClass().getType())
                .invokeStatic(this, mtd, args);
    }

    private JValue invokeInstance(InvokeInstanceExp ii, IR ir, Frame f) {
        List<JValue> args = ii.getArgs().stream()
                .map(e -> evalExp(e, ir, f))
                .toList();
        JValue v = evalExp(ii.getBase(), ir, f);
        if (v instanceof JObject obj) {
            JMethod method;
            if (! (ii instanceof InvokeSpecial)) {
                method = World.get().getClassHierarchy()
                        .dispatch(obj.getType(), ii.getMethodRef());
            } else {
                // TODO: check default calls
                method = ii.getMethodRef().resolve();
            }
            assert method != null;
            if (obj instanceof JVMObject jvmObject && method.getName().equals(MethodNames.INIT)) {
                jvmObject.init(method, args);
                return null;
            }
            return obj.invokeInstance(this, method, args);
        } else if (v instanceof JArray arr) {
            if (Utils.isGetClass(ii.getMethodRef())) {
                return arr.mockGetClass(this);
            } else if (Utils.isClone(ii.getMethodRef())) {
                return new JArray(arr);
            } else if (Utils.isEquals(ii.getMethodRef())) {
                JValue value = evalExp(ii.getArg(0), ir, f);
                JValue base = evalExp(ii.getBase(), ir, f);
                return JPrimitive.getBoolean(base.equals(value));
            } else {
                throw new InterpreterException();
            }
        } else if (v instanceof JNull) {
            throw new ClientException(new NullPointerException());
        } else {
            throw new InterpreterException();
        }
    }

    private JValue invokeDynamic(InvokeDynamic id, IR ir, Frame f) {
        MethodRef bootStrap = id.getBootstrapMethodRef();
        JMethod bootMtd = bootStrap.resolve();
        assert bootMtd.isStatic();
        Method m = Utils.toJVMMethod(bootMtd);
        assert m.getParameterCount() == bootMtd.getParamCount();
        MethodHandles.Lookup k = MethodHandles.lookup();
        MethodType methodType = id.getMethodType();
        java.lang.invoke.MethodType methodType1 = java.lang.invoke.MethodType.methodType(
                Utils.toJVMType(methodType.getReturnType()),
                Utils.toJVMTypeList(methodType.getParamTypes()));
        List<JValue> bootstrapMtdArgs = id.getBootstrapArgs()
                .stream().map(l -> evalExp(l, ir, f)).toList();
        List<Object> args = new ArrayList<>();
        args.add(k);
        args.add(id.getMethodName());
        args.add(methodType1);
        for (int i = 3, j = 0; i < m.getParameterCount(); ++i, ++j) {
            if (i == m.getParameterCount() - 1 && bootMtd.getParamType(i) instanceof ArrayType at) {
                assert at.dimensions() == 1; // need not worry, only constants can reach here
                int size = bootstrapMtdArgs.size() - j;
                Object[] varargs = new Object[size];
                args.add(varargs);
                for (; j < bootstrapMtdArgs.size(); ++j) {
                    varargs[bootstrapMtdArgs.size() - j] = Utils.typedToJVMObj(
                            bootstrapMtdArgs.get(i), at.baseType());
                }
                break;
            }
            JValue now = bootstrapMtdArgs.get(j);
            Type t = bootMtd.getParamType(i);
            args.add(Utils.typedToJVMObj(now, t));
        }
        try {
            CallSite callSite = (CallSite)
                    m.invoke(null, args.toArray());
            java.lang.invoke.MethodHandle handle = callSite.getTarget();
            List<JValue> realArgs = id.getArgs().stream().map(e -> evalExp(e, ir, f)).toList();
            Object[] realJVMArgs = Utils.toJVMObjects(realArgs, methodType.getParamTypes());
            Object o = handle.invokeWithArguments(realJVMArgs);
            return Utils.fromJVMObject(this, o, methodType.getReturnType());
        } catch (Throwable e) {
            throw new InterpreterException(e);
        }
    }

    private JValue evalInvoke(InvokeExp ie, IR ir, Frame f) {
        if (ie instanceof InvokeStatic is) {
            return invokeStatic(is, ir, f);
        } else if (ie instanceof InvokeInstanceExp ii) {
            return invokeInstance(ii, ir, f);
        } else if (ie instanceof InvokeDynamic id) {
            return invokeDynamic(id, ir, f);
        } else {
            throw new InterpreterException();
        }
    }

    public JValue evalExp(Exp e, IR ir, Frame f) {
        if (e instanceof Literal l) {
            if (l instanceof IntLiteral intLiteral) {
                return JPrimitive.get(intLiteral.getValue());
            } else if (l instanceof FloatLiteral floatLiteral) {
                return JPrimitive.get(floatLiteral.getValue());
            } else if (l instanceof DoubleLiteral doubleLiteral) {
                return JPrimitive.get(doubleLiteral.getValue());
            } else if (l instanceof LongLiteral longLiteral) {
                return JPrimitive.get(longLiteral.getValue());
            } else if (l instanceof StringLiteral stringLiteral) {
                return new JVMObject(getSpecialClass(ClassNames.STRING),
                        stringLiteral.getString());
            } else if (l instanceof ClassLiteral classLiteral) {
                try {
                    return getClassLiteral(classLiteral);
                } catch (ClassNotFoundException ex) {
                    throw new InterpreterException(ex);
                }
            } else if (l instanceof NullLiteral) {
                return JNull.NULL;
            } else {
                throw new InterpreterException();
            }
        } else if (e instanceof Var v) {
            if (v.isConst()) {
                return evalExp(v.getConstValue(), ir, f);
            } else {
                return f.getRegs().get(v);
            }
        } else if (e instanceof BinaryExp b) {
            JValue v1 = evalExp(b.getOperand1(), ir, f);
            JValue v2 = evalExp(b.getOperand2(), ir, f);
            return BinaryEval.evalBinary(b.getOperator(), v1, v2);
        } else if (e instanceof NewExp n) {
            if (n instanceof NewInstance ni) {
                ClassType ct = ni.getType();
                JClassObject klass = loadClass(ct);
                if (klass instanceof JVMClassObject jvmClassObject) {
                    return new JVMObject(jvmClassObject);
                } else {
                    return createJObject(ct);
                }
            } else if (n instanceof NewArray na) {
                int count = JValue.getInt(evalExp(na.getLength(), ir, f));
                ArrayType at = na.getType();
                return JArray.createArray(count, at.baseType(), at.dimensions());
            } else if (n instanceof NewMultiArray nma) {
                List<Integer> dims = new ArrayList<>();
                for (var i : nma.getLengths()) {
                    dims.add(JValue.getInt(evalExp(i, ir, f)));
                }
                return JArray.createMultiArray(nma.getType(), dims, 0);
            } else {
                throw new InterpreterException();
            }
        } else if (e instanceof ArrayAccess aa) {
            JArray b = JValue.getJArray(evalExp(aa.getBase(), ir, f));
            int idx = JValue.getInt(evalExp(aa.getIndex(), ir, f));
            return b.getIdx(idx);
        } else if (e instanceof InstanceFieldAccess ifa) {
            JObject obj = JValue.getObject(evalExp(ifa.getBase(), ir, f));
            return obj.getField(this, ifa.getFieldRef());
        } else if (e instanceof StaticFieldAccess sfa) {
            JClassObject classObj = loadClass(sfa.getFieldRef().resolve()
                    .getDeclaringClass().getType());
            return classObj.getStaticField(this, sfa.getFieldRef());
        } else if (e instanceof InvokeExp ie) {
            return evalInvoke(ie, ir, f);
        } else if (e instanceof CastExp castExp) {
            Type t = castExp.getCastType();
            JValue v = evalExp(castExp.getValue(), ir, f);
            if (t instanceof PrimitiveType primitiveType) {
                return JPrimitive.get(performPrimitiveConv(v, primitiveType));
            } else if (t instanceof ReferenceType r) {
                if (world.getTypeSystem().isSubtype(r, v.getType())) {
                    return v;
                } else {
                    throw new ClientException(new ClassCastException());
                }
            } else {
                throw new InterpreterException();
            }
        } else if (e instanceof NegExp neg) {
            JValue value = evalExp(neg.getValue(), ir, f);
            assert value instanceof JPrimitive;
            return ((JPrimitive) value).getNegValue();
        } else if (e instanceof InstanceOfExp instanceOf) {
            JValue value = evalExp(instanceOf.getValue(), ir, f);
            if (value instanceof JNull) {
                return JPrimitive.getBoolean(false);
            }
            return JPrimitive.getBoolean(World.get().getTypeSystem()
                    .isSubtype(instanceOf.getCheckedType(), value.getType()));
        } else if (e instanceof ArrayLengthExp arrayLengthExp) {
            JValue v = evalExp(arrayLengthExp.getOperand(), ir, f);
            JArray array = JValue.getJArray(v);
            return JPrimitive.get(array.length());
        } else {
            throw new InterpreterException(e + " is not implemented");
        }
    }

    private JObject getClassLiteral(ClassLiteral classLiteral) throws ClassNotFoundException {
        Type t = classLiteral.getTypeValue();
        if (Utils.isJVMClass(t)) {
            return getClassLiteral(Utils.toJVMType(t));
        } else {
            if (t instanceof ClassType ct) {
                return new JMockClassObject(this, ct.getJClass());
            } else if (t instanceof ArrayType at) {
                assert at.baseType() instanceof ClassType;
                JClass klass = ((ClassType) at.baseType()).getJClass();
                return new JMockClassObject(this, klass, at.dimensions());
            } else {
                throw new InterpreterException();
            }
        }
    }

    JVMObject getClassLiteral(Class<?> klass) {
        return new JVMObject(getSpecialClass(ClassNames.CLASS), klass);
    }

    private Object performPrimitiveConv(JValue v, PrimitiveType type) {
        assert v instanceof JPrimitive;
        JPrimitive p = (JPrimitive) v;
        Object o = p.value;
        if (o instanceof Integer i) {
            return switch (type) {
                case BOOLEAN, BYTE, CHAR, SHORT -> Utils.getIntValue(Utils.downCastInt(i, type));
                case INT -> i;
                case LONG -> i.longValue();
                case FLOAT -> i.floatValue();
                case DOUBLE -> i.doubleValue();
            };
        } else if (o instanceof Long l) {
            return switch (type) {
                case BOOLEAN, BYTE, CHAR, SHORT -> throw new InterpreterException();
                case INT -> l.intValue();
                case LONG -> l;
                case FLOAT -> l.floatValue();
                case DOUBLE -> l.doubleValue();
            };
        } else if (o instanceof Float f) {
            return switch (type) {
                case BOOLEAN, BYTE, CHAR, SHORT -> throw new InterpreterException();
                case INT -> f.intValue();
                case LONG -> f.longValue();
                case FLOAT -> f;
                case DOUBLE -> f.doubleValue();
            };
        } else if (o instanceof Double d) {
            return switch (type) {
                case BOOLEAN, BYTE, CHAR, SHORT -> throw new InterpreterException();
                case INT -> d.intValue();
                case LONG -> d.longValue();
                case FLOAT -> d.floatValue();
                case DOUBLE -> d;
            };
        }
        throw new InterpreterException();
    }

    public JVMClassObject getSpecialClass(String name) {
        ClassType type = World.get().getTypeSystem().getClassType(name);
        return (JVMClassObject) loadClass(type);
    }

    private JObject createJObject(ClassType ct) {
        ClassType superType = Objects.requireNonNull(ct.getJClass().getSuperClass()).getType();
        JClassObject klass = loadClass(ct);
        if (Utils.isJVMClass(superType)) {
            return new JObject(this, klass);
        } else {
            return new JObject(this, klass, createJObject(superType));
        }
    }
}


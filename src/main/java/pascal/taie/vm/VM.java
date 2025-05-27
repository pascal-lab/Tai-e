/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.vm;

import pascal.taie.World;
import pascal.taie.frontend.newfrontend.java.NewFrontendException;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.ArrayLengthExp;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.BitwiseExp;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.ComparisonExp;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.DoubleLiteral;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.FloatLiteral;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.InstanceOfExp;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
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
import pascal.taie.ir.exp.PhiExp;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.ShiftExp;
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
import pascal.taie.ir.stmt.PhiStmt;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The virtual machine for executing the IR.
 * <p>
 * A normal usage will be
 * <pre>
 * {@code
 * VM vm = new VM(World.get());
 * vm.exec();
 * }
 * </pre>
 * <p>
 * Currently, the VM has many limitations:
 * <ul>
 *     <li>Only support a single thread</li>
 *     <li>We cannot handle quite a lot language features, e.g.,
 *         lambda expressions, method references, native methods, etc.</li>
 * </ul>
 *
 * @see JVMObject
 */
public class VM {
    private final World world;

    private final ArrayDeque<Frame> frames;

    private final Map<ClassType, JClassRep> classObjs;

    public VM(World world) {
        this.world = world;
        frames = new ArrayDeque<>();
        classObjs = Maps.newMap();
    }

    // ------------------- Execution (starts) -------------------
    /**
     * Execute the main method of the program.
     */
    public void exec() {
        JMethod main = world.getMainMethod();
        execIR(main.getIR(), Frame.makeNewFrame());
    }

    JValue execIR(IR ir, Frame f) {
        frames.push(f);
        while (f.getPC() >= 0) {
            int pc = f.getPC();
            Stmt stmt = ir.getStmt(pc);
            boolean exceptionTriggered = false;
            try {
                execStmt(stmt, ir, f);
            } catch (VMException | NewFrontendException e) {
                throw e;
            } catch (Exception e) {
                exceptionTriggered = true;
                Exception exception;
                if (e instanceof ClientException e1) {
                    exception = e1.internal;
                } else {
                    exception = e;
                }
                int currentPC = f.getPC();
                ClassType t;
                JObject aCatchObj;
                if (exception instanceof ClientDefinedException cde) {
                    t = (ClassType) cde.inner.getType();
                    aCatchObj = cde.inner;
                } else {
                    t = Utils.fromJVMClass(exception.getClass());
                    aCatchObj = new JVMObject((JVMClassRep) loadClass(t), exception);
                }
                Optional<ExceptionEntry> exceptionEntry = ir.getExceptionEntries().stream()
                        .filter(entry -> World.get().getTypeSystem().isSubtype(entry.catchType(), t) &&
                                currentPC >= entry.start().getIndex() &&
                                currentPC < entry.end().getIndex())
                        .findAny();
                if (exceptionEntry.isPresent()) {
                    Catch aCatch = exceptionEntry.get().handler();
                    f.getRegs().put(aCatch.getExceptionRef(), aCatchObj);
                    f.setPC(aCatch.getIndex());
                } else {
                    // TODO: this approach will cause client code catch some internal exception
                    //       correct this by explicitly throw all jvm exceptions

                    // throw to outer frame
                    frames.pop();
                    throw new ClientException(exception);
                }
            }
            if (!(exceptionTriggered || stmt instanceof PhiStmt || stmt instanceof Catch)) {
                f.setLastPC(pc);
            }
        }
        frames.pop();
        return f.getRets();
    }

    private void execStmt(Stmt stmt, IR ir, Frame f) {
        if (stmt instanceof Nop || stmt instanceof Catch || stmt instanceof Monitor) {
            // do nothing
        } else if (stmt instanceof Return r) {
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
                    JClassRep obj = loadClass(sfa.getFieldRef().resolve()
                            .getDeclaringClass().getType());
                    obj.setStaticField(this, sfa.getFieldRef(), rValue);
                } else if (l instanceof ArrayAccess aa) {
                    JArray array = JValue.getJArray(evalExp(aa.getBase(), ir, f));
                    int idx = JValue.getInt(evalExp(aa.getIndex(), ir, f));
                    array.setIdx(idx, rValue);
                } else {
                    throw new VMException();
                }
            }
        } else if (stmt instanceof Goto g) {
            f.setPC(g.getTarget().getIndex());
            return;
        } else if (stmt instanceof If i) {
            if (JValue.getInt(evalExp(i.getCondition(), ir, f)) == Utils.INT_TRUE) {
                f.setPC(i.getTarget().getIndex());
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
            f.setPC(target.getIndex());
            return;
        } else {
            throw new VMException();
        }

        if (f.getPC() < ir.getStmts().size()) {
            f.setPC(f.getPC() + 1);
        } else {
            f.markEnd();
        }
    }
    // ------------------- Execution (ends) -------------------


    // ------------------- Evaluation (starts) -------------------
    private JValue evalInvoke(InvokeExp ie, IR ir, Frame f) {
        if (ie instanceof InvokeStatic is) {
            return invokeStatic(is, ir, f);
        } else if (ie instanceof InvokeInstanceExp ii) {
            return invokeInstance(ii, ir, f);
        } else if (ie instanceof InvokeDynamic id) {
            return invokeDynamic(id, ir, f);
        } else {
            throw new VMException();
        }
    }

    private static JValue evalBinary(BinaryExp.Op op, JValue v1, JValue v2) {
        if (op instanceof ConditionExp.Op op1) {
            if (v1 instanceof JPrimitive && v2 instanceof JPrimitive) {
                Integer i1 = JValue.getInt(v1);
                Integer i2 = JValue.getInt(v2);
                return JPrimitive.getBoolean(
                        switch (op1) {
                            case EQ -> i1.equals(i2);
                            case GE -> i1 >= i2;
                            case GT -> i1 > i2;
                            case LE -> i1 <= i2;
                            case LT -> i1 < i2;
                            case NE -> ! i1.equals(i2);
                        });
            } else {
                boolean res;
                if (v1 instanceof JObject o1 && v2 instanceof JObject o2) {
                    if (v1 instanceof JVMObject vmo1 && v2 instanceof JVMObject vmo2) {
                        res = vmo1.toJVMObj() == vmo2.toJVMObj();
                    } else if (v1 instanceof JClassLiteralObject mockClassObject1
                            && v2 instanceof JClassLiteralObject mockClassObject2) {
                        res = mockClassObject1.klass == mockClassObject2.klass &&
                                mockClassObject1.dimensions == mockClassObject2.dimensions;
                    } else {
                        res = o1 == o2;
                    }
                } else if (v1 instanceof JArray arr1 && v2 instanceof JArray arr2) {
                    res = arr1 == arr2;
                } else if (v1 instanceof JNull || v2 instanceof JNull) {
                    res = v1 instanceof JNull && v2 instanceof JNull;
                } else {
                    throw new VMException();
                }

                if (op == ConditionExp.Op.NE) {
                    res = ! res;
                }
                return JPrimitive.getBoolean(res);
            }
        }

        JPrimitive primitive1 = (JPrimitive) v1;
        JPrimitive primitive2 = (JPrimitive) v2;
        Object pv1 = primitive1.value;
        Object pv2 = primitive2.value;
        if (op instanceof ArithmeticExp.Op op1) {
            return evalArithmetic(op1, pv1, primitive2.value);
        } else if (op instanceof ComparisonExp.Op op1) {
            if (pv1 instanceof Long l1 && pv2 instanceof Long l2) {
                return JPrimitive.get(l1.compareTo(l2));
            } else if (pv1 instanceof Float f1 && pv2 instanceof Float f2) {
                if (f1.floatValue() == f2.floatValue()) {
                    return JPrimitive.get(0);
                } else if (f1 > f2) {
                    return JPrimitive.get(1);
                } else if (f1 < f2) {
                    // IDEA may report a false positive warning.
                    // Law of trichotomy does not hold for `float` or `double` type
                    // E.g. NaN >  NaN ==> false
                    //      NaN == NaN ==> false
                    //      NaN <  NaN ==> false
                    return JPrimitive.get(-1);
                } else {
                    return switch (op1) {
                        case CMPG -> JPrimitive.get(1);
                        case CMPL -> JPrimitive.get(-1);
                        case CMP -> throw new VMException();
                    };
                }
            } else if (pv1 instanceof Double d1 && pv2 instanceof Double d2) {
                if (d1.doubleValue() == d2.doubleValue()) {
                    return JPrimitive.get(0);
                } else if (d1 > d2) {
                    return JPrimitive.get(1);
                } else if (d1 < d2) {
                    return JPrimitive.get(-1);
                } else {
                    return switch (op1) {
                        case CMPG -> JPrimitive.get(1);
                        case CMPL -> JPrimitive.get(-1);
                        case CMP -> throw new VMException();
                    };
                }
            } else {
                throw new VMException();
            }
        } else if (op instanceof ShiftExp.Op op1) {
            if (pv1 instanceof Long) {
                long ll1 = JValue.getLong(v1);
                int i2 = JValue.getInt(v2);
                return JPrimitive.get(
                        switch (op1) {
                            case SHL -> ll1 << i2;
                            case SHR -> ll1 >> i2;
                            case USHR -> ll1 >>> i2;
                        });
            } else {
                int i1 = JValue.getInt(v1);
                int i2 = JValue.getInt(v2);
                return JPrimitive.get(
                        switch (op1) {
                            case SHL -> i1 << i2;
                            case SHR -> i1 >> i2;
                            case USHR -> i1 >>> i2;
                        });
            }
        } else if (op instanceof BitwiseExp.Op op1) {
            if (pv1 instanceof Long) {
                long ll1 = JValue.getLong(v1);
                long ll2 = JValue.getLong(v2);
                return JPrimitive.get(
                        switch (op1) {
                            case OR -> ll1 | ll2;
                            case AND -> ll1 & ll2;
                            case XOR -> ll1 ^ ll2;
                        });
            } else {
                int i1 = JValue.getInt(v1);
                int i2 = JValue.getInt(v2);
                return JPrimitive.get(
                        switch (op1) {
                            case OR -> i1 | i2;
                            case AND -> i1 & i2;
                            case XOR -> i1 ^ i2;
                        });
            }
        } else {
            throw new VMException();
        }
    }

    private static JValue evalArithmetic(ArithmeticExp.Op op, Object v1, Object v2) {
        if (v1 instanceof Integer l1 && v2 instanceof Integer l2) {
            return new JPrimitive(
                    switch (op) {
                        case ADD -> l1 + l2;
                        case DIV -> l1 / l2;
                        case MUL -> l1 * l2;
                        case REM -> l1 % l2;
                        case SUB -> l1 - l2;
                    });
        } else if (v1 instanceof Long l1 && v2 instanceof Long l2) {
            return new JPrimitive(
                    switch (op) {
                        case ADD -> l1 + l2;
                        case SUB -> l1 - l2;
                        case MUL -> l1 * l2;
                        case DIV -> l1 / l2;
                        case REM -> l1 % l2;
                    });
        } else if (v1 instanceof Float f1 && v2 instanceof Float f2) {
            return new JPrimitive(
                    switch (op) {
                        case ADD -> f1 + f2;
                        case SUB -> f1 - f2;
                        case MUL -> f1 * f2;
                        case DIV -> f1 / f2;
                        case REM -> f1 % f2;
                    });
        } else if (v1 instanceof Double f1 && v2 instanceof Double f2) {
            return new JPrimitive(
                    switch (op) {
                        case ADD -> f1 + f2;
                        case SUB -> f1 - f2;
                        case MUL -> f1 * f2;
                        case DIV -> f1 / f2;
                        case REM -> f1 % f2;
                    });
        } else {
            throw new VMException();
        }
    }

    private JValue evalExp(Exp e, IR ir, Frame f) {
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
                    throw new VMException(ex);
                }
            } else if (l instanceof NullLiteral) {
                return JNull.NULL;
            } else if (l instanceof MethodType methodType) {
                return new JVMObject(getSpecialClass(ClassNames.METHOD_TYPE),
                        Utils.toJVMMethodType(methodType));
            } else {
                throw new VMException();
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
            return evalBinary(b.getOperator(), v1, v2);
        } else if (e instanceof NewExp n) {
            if (n instanceof NewInstance ni) {
                ClassType ct = ni.getType();
                JClassRep klass = loadClass(ct);
                if (klass instanceof JVMClassRep jvmClassObject) {
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
                throw new VMException();
            }
        } else if (e instanceof ArrayAccess aa) {
            JArray b = JValue.getJArray(evalExp(aa.getBase(), ir, f));
            int idx = JValue.getInt(evalExp(aa.getIndex(), ir, f));
            return b.getIdx(idx);
        } else if (e instanceof InstanceFieldAccess ifa) {
            JObject obj = JValue.getObject(evalExp(ifa.getBase(), ir, f));
            return obj.getField(this, ifa.getFieldRef());
        } else if (e instanceof StaticFieldAccess sfa) {
            JClassRep classObj = loadClass(sfa.getFieldRef().resolve()
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
                throw new VMException();
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
        } else if (e instanceof PhiExp phi) {
            int lastPC = f.getLastPC();
            Stmt lastInstr = ir.getStmt(lastPC);
            // The lastPC is not always the end of a block because of the exception mechanism.
            // For that, we find the closest def.
            var sourceAndVar = phi.getSourceAndVar();
            Comparator<Pair<Stmt, Var>> c = Comparator.comparing(p -> p.first().getIndex());
            int pos = Collections.binarySearch(sourceAndVar, new Pair<>(lastInstr, null), c);
            if (pos < 0) {
                // exception happens and not at a block exit.
                pos = -(pos + 1);
            }
            Var v = sourceAndVar.get(pos).second();
            assert v != null;
            return evalExp(v, ir, f);
        } else {
            throw new VMException(e + " is not implemented");
        }
    }
    // ------------------- Evaluation (ends) -------------------


    // ------------------- Method invocation (starts) -------------------
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
            if (! (ii instanceof InvokeSpecial) && ! (obj instanceof JVMObject)) {
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
                return getArrayClass(arr);
            } else if (Utils.isClone(ii.getMethodRef())) {
                return new JArray(arr);
            } else if (Utils.isEquals(ii.getMethodRef())) {
                JValue value = evalExp(ii.getArg(0), ir, f);
                JValue base = evalExp(ii.getBase(), ir, f);
                return JPrimitive.getBoolean(base.equals(value));
            } else {
                throw new VMException();
            }
        } else if (v instanceof JNull) {
            throw new ClientException(new NullPointerException());
        } else {
            throw new VMException();
        }
    }

    /**
     * Handle invokedynamic invocation.
     * <p>
     * The Current implementation cannot handle lambda expressions.
     * But it can handle method references.
     * </p>
     */
    private JValue invokeDynamic(InvokeDynamic id, IR ir, Frame f) {
        MethodRef bootStrap = id.getBootstrapMethodRef();
        JMethod bootMtd = bootStrap.resolve();
        assert bootMtd.isStatic();
        Method m = Utils.toJVMMethod(bootMtd);
        assert m.getParameterCount() == bootMtd.getParamCount();
        MethodHandles.Lookup k = MethodHandles.lookup();
        MethodType methodType = id.getMethodType();
        java.lang.invoke.MethodType methodType1 = Utils.toJVMMethodType(methodType);
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
            throw new VMException(e);
        }
    }
    // ------------------- Method invocation (ends) -------------------


    // ------------------- Class loading (starts) -------------------
    JVMClassRep loadJVMClass(Class<?> klass) {
        ClassType ct = Utils.fromJVMClass(klass);
        assert ct != null;
        if (classObjs.containsKey(ct)) {
            return (JVMClassRep) classObjs.get(ct);
        } else {
            JVMClassRep obj = new JVMClassRep(ct, klass);
            classObjs.put(ct, obj);
            return obj;
        }
    }

    public JClassRep loadClass(ClassType t) {
        if (classObjs.containsKey(t)) {
            return classObjs.get(t);
        } else {
            JClassRep obj;
            if (Utils.isJVMClass(t)) {
                obj = new JVMClassRep(t);
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
                obj = new JClassRep(t);
                classObjs.put(t, obj);
                if (clinit != null) {
                    execIR(clinit.getIR(), Frame.makeNewFrame());
                }
            }
            return obj;
        }
    }
    // ------------------- Class loading (ends) -------------------


    // ------------------- Helper functions (starts) -------------------
    private JObject getClassLiteral(ClassLiteral classLiteral) throws ClassNotFoundException {
        Type t = classLiteral.getTypeValue();
        if (Utils.isJVMClass(t)) {
            return getClassLiteral(Utils.toJVMType(t));
        } else {
            if (t instanceof ClassType ct) {
                return new JClassLiteralObject(this, ct.getJClass());
            } else if (t instanceof ArrayType at) {
                assert at.baseType() instanceof ClassType;
                JClass klass = ((ClassType) at.baseType()).getJClass();
                return new JClassLiteralObject(this, klass, at.dimensions());
            } else {
                throw new VMException();
            }
        }
    }

    private JVMObject getClassLiteral(Class<?> klass) {
        return new JVMObject(getSpecialClass(ClassNames.CLASS), klass);
    }

    private Object performPrimitiveConv(JValue v, PrimitiveType type) {
        assert v instanceof JPrimitive;
        JPrimitive p = (JPrimitive) v;
        Object o = p.value;
        int index = pascal.taie.frontend.newfrontend.Utils.getPrimitiveTypeIndex(type);
        if (o instanceof Integer i) {
            return switch (index) {
                case 0, 1, 2, 3 -> Utils.getIntValue(Utils.downCastInt(i, type));
                case 4 -> i;
                case 5 -> i.longValue();
                case 6 -> i.floatValue();
                case 7 -> i.doubleValue();
                default -> throw new VMException();
            };
        } else if (o instanceof Long l) {
            return switch (index) {
                case 4 -> l.intValue();
                case 5 -> l;
                case 6 -> l.floatValue();
                case 7 -> l.doubleValue();
                default -> throw new VMException();
            };
        } else if (o instanceof Float f) {
            return switch (index) {
                case 4 -> f.intValue();
                case 5 -> f.longValue();
                case 6 -> f;
                case 7 -> f.doubleValue();
                default -> throw new VMException();
            };
        } else if (o instanceof Double d) {
            return switch (index) {
                case 4 -> d.intValue();
                case 5 -> d.longValue();
                case 6 -> d.floatValue();
                case 7 -> d;
                default -> throw new VMException();
            };
        }
        throw new VMException();
    }

    JVMClassRep getSpecialClass(String name) {
        ClassType type = World.get().getTypeSystem().getClassType(name);
        return (JVMClassRep) loadClass(type);
    }

    private JObject createJObject(ClassType ct) {
        ClassType superType = Objects.requireNonNull(ct.getJClass().getSuperClass()).getType();
        JClassRep klass = loadClass(ct);
        if (Utils.isJVMClass(superType)) {
            return new JObject(this, klass);
        } else {
            return new JObject(this, klass, createJObject(superType));
        }
    }

    private JObject getArrayClass(JArray jArray) {
        if (Utils.isJVMClass(jArray.type)) {
            return getClassLiteral(Utils.toJVMType(jArray.type));
        } else {
            assert jArray.type.elementType() instanceof ClassType;
            return new JClassLiteralObject(this, ((ClassType) jArray.type.baseType()).getJClass());
        }
    }
    // ------------------- Helper functions (ends) -------------------
}


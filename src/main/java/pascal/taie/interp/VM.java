package pascal.taie.interp;

import pascal.taie.World;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.*;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.*;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.Maps;

import java.util.*;

public class VM {
    final World world;
    final Stack<Frame> frames;

    final Map<ClassType, JClassObj> classObjs;

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
        System.out.println(ir.getMethod().getName());
        frames.push(f);
        while (f.getPc() >= 0) {
            Stmt stmt = ir.getStmt(f.getPc());
            execStmt(stmt, ir, f);
        }
        frames.pop();
        return f.getRets();
    }

    private JClassObj loadClass(ClassType t) {
        if (classObjs.containsKey(t)) {
            return classObjs.get(t);
        } else {
            for (var i : t.getJClass().getInterfaces()) {
                if (!classObjs.containsKey(i.getType())) {
                    loadClass(i.getType());
                }
            }
            JClass superClass = t.getJClass().getSuperClass();
            if (superClass != null && ! classObjs.containsKey(superClass.getType())) {
                loadClass(superClass.getType());
            }
            JMethod clinit = t.getJClass().getClinit();
            JClassObj obj = new JClassObj(t);
            classObjs.put(t, obj);
            if (clinit != null) {
                execIR(clinit.getIR(), Frame.mkNewFrame());
            }
            return obj;
        }
    }

    public void execStmt(Stmt stmt, IR ir, Frame f) {
        if (stmt instanceof Nop) {
            return;
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
                    obj.setFields(ref.getName(), rValue);
                } else if (l instanceof StaticFieldAccess sfa) {
                    JClassObj obj = loadClass(sfa.getFieldRef().resolve()
                            .getDeclaringClass().getType());
                    obj.setFields(sfa.getFieldRef().getName(), rValue);
                } else if (l instanceof ArrayAccess aa) {
                    JArray array = JValue.getJArray(evalExp(aa.getBase(), ir, f));
                    int idx = JValue.getInt(evalExp(aa.getIndex(), ir, f));
                    array.setIdx(idx, rValue);
                } else {
                    throw new IllegalStateException("should not be here");
                }
            }
        } else if (stmt instanceof Goto g) {
            f.setPc(g.getTarget().getIndex());
            return;
        } else if (stmt instanceof If i) {
            List<Stmt> targets = i.getTargets();
            if (JValue.getInt(evalExp(i.getCondition(), ir, f)) == 0) {
                f.setPc(i.getTarget().getIndex());
                return;
            }
        }

        if (f.getPc() < ir.getStmts().size()) {
            f.setPc(f.getPc() + 1);
        } else {
            f.markEnd();
        }
    }

    private JValue invokeStatic(InvokeStatic is, IR ir, Frame f) {
        Map<Var, JValue> args = Maps.newMap();
        IR newIr = is.getMethodRef().resolve().getIR();
        for (int i = 0; i < is.getArgCount(); ++i) {
            args.put(newIr.getParam(i), evalExp(is.getArg(i), ir, f));
        }
        Frame newFrame = Frame.mkNewFrame(args);
        return execIR(newIr, newFrame);
    }

    private JValue invokeInstance(InvokeInstanceExp ii, IR ir, Frame f) {
        Map<Var, JValue> args = Maps.newMap();
        JObject obj = JValue.getObject(evalExp(ii.getBase(), ir, f));
        JMethod method = obj.getMethod(ii.getMethodRef().getSubsignature());
        IR newIr = method.getIR();
        Var jThis = newIr.getThis();
        args.put(jThis, evalExp(ii.getBase(), ir, f));
        for (int i = 0; i < ii.getArgCount(); ++i) {
            args.put(newIr.getParam(i), evalExp(ii.getArg(i), ir, f));
        }
        Frame newFrame = Frame.mkNewFrame(args);
        return execIR(newIr, newFrame);
    }

    public JValue evalExp(Exp e, IR ir, Frame f) {
        if (e instanceof Literal l) {
            return JLiteral.get(l);
        } else if (e instanceof Var v) {
            return f.getRegs().get(v);
        } else if (e instanceof BinaryExp b) {
            JValue v1 = evalExp(b.getOperand1(), ir, f);
            JValue v2 = evalExp(b.getOperand2(), ir, f);
            return BinaryEval.evalBinary(
                    b.getOperator(), v1, v2);
        } else if (e instanceof NewExp n) {
            if (n instanceof NewInstance ni) {
                return new JObject(ni.getType());
            } else if (n instanceof NewArray na) {
                var t = JValue.TypeToJValueType(na.getType());
                int dim = JValue.getInt(evalExp(na.getLength(), ir, f));
                return JArray.createArray(t, dim);
            } else if (n instanceof NewMultiArray nma) {
                var t = JValue.TypeToJValueType(nma.getType());
                List<Integer> dims = new ArrayList<>();
                for (var i : nma.getLengths()) {
                    dims.add(JValue.getInt(evalExp(i, ir, f)));
                }
                return JArray.createMultiArray(t, nma.getType(), dims, 0);
            } else throw new IllegalStateException("should not be here");
        } else if (e instanceof ArrayAccess aa) {
            JArray b = JValue.getJArray(evalExp(aa.getBase(), ir, f));
            int idx = JValue.getInt(evalExp(aa.getIndex(), ir, f));
            return b.getIdx(idx);
        } else if (e instanceof InstanceFieldAccess ifa) {
            JObject obj = JValue.getObject(evalExp(ifa.getBase(), ir, f));
            return obj.getFields(ifa.getFieldRef().getName());
        } else if (e instanceof StaticFieldAccess sfa) {
            JClassObj classObj = loadClass(sfa.getFieldRef().resolve()
                    .getDeclaringClass().getType());
            return classObj.getFields(sfa.getFieldRef().getName());
        } else if (e instanceof InvokeExp ie) {
            if (ie instanceof InvokeStatic is) {
                return invokeStatic(is, ir, f);
            } else if (ie instanceof InvokeInstanceExp ii) {
                return invokeInstance(ii, ir, f);
            } else {
                throw new IllegalStateException("should not be here");
            }
        } else {
            throw new UnsupportedOperationException(e + " is not implemented");
        }
    }
}

class Frame {
    private int pc;
    private Map<Var, JValue> regs;

    private JValue rets;

    public Frame(int pc, Map<Var, JValue> regs) {
        this.pc = pc;
        this.regs = regs;
    }

    public static Frame mkNewFrame() {
       return new Frame(0, Maps.newMap());
    }

    public static Frame mkNewFrame(Map<Var, JValue> args) {
        return new Frame(0, args);
    }

    public int getPc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public Map<Var, JValue> getRegs() {
        return regs;
    }

    public void setRegs(Map<Var, JValue> regs) {
        this.regs = regs;
    }

    public JValue getRets() {
        return rets;
    }

    public void setRets(JValue rets) {
        this.rets = rets;
    }

    public void markEnd() {
        this.pc = -1;
    }
}

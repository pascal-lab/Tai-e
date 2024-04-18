package pascal.taie.frontend.newfrontend;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.frontend.newfrontend.report.TaieCastingReporter;
import pascal.taie.frontend.newfrontend.ssa.Dominator;
import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.ExpModifier;
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.InvokeInterface;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StmtVisitor;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pascal.taie.frontend.newfrontend.Utils.*;

public class CastingInsert2 {

    private final AsmIRBuilder builder;

    private static final Logger logger = LogManager.getLogger("Casting");

    private Stmt currentStmt;

    private final Map<FlowTypeInfo, Var> flowTypeCache;

    private final Dominator<BytecodeBlock> dom;

    private final BytecodeGraph graph;

    public CastingInsert2(AsmIRBuilder builder) {
        this.builder = builder;
        this.dom = builder.getDom();
        this.graph = builder.getGraph();
        this.flowTypeCache = Maps.newHybridMap();
    }

    private Cast getNewCast(Var left, Var right, Type t) {
        logger.atTrace().log("[CASTING] Current stmt: " + currentStmt + "\n" +
                            "          Var " + right + " With Type: " + right.getType() + "\n" +
                            "          Excepted Type: " + t + "\n" +
                            "          In method: " + builder.method);
        TaieCastingReporter.get().reportCasting(new TaieCastingReporter.TaieCastingInfo(
                builder.method,
                currentStmt,
                t,
                right,
                right.getType()
        ));
        return new Cast(left, new CastExp(right, t));
    }

    private Type maySplitStmt(LValue l, RValue r) {
        Type lType = l.getType();
        Type rType = r.getType();
        if (! isAssignable(lType, rType)) {
            return lType;
        } else {
            return null;
        }
    }

    private Stmt ensureValidArrayType(ArrayAccess access, Stmt stmt, BytecodeBlock block, List<Stmt> newStmts) {
        Type t = BuildContext.get().getTypeSystem().getArrayType(getObject(), 1);
        if (access.getBase().getType() instanceof ArrayType) {
            return stmt;
        } else {
            Var local = requireFlowTypeVarOnDomTree(block, access.getBase(), t);
            if (local != null) {
                Lenses lenses = new Lenses(builder.method, Map.of(access.getBase(), local), Map.of());
                return lenses.subSt(stmt);
            } else {
                Var v1 = builder.manager.getTempVar();
                ExpModifier.setType(v1, t);
                Cast c = getNewCast(v1, access.getBase(), t);
                c.setLineNumber(stmt.getLineNumber());
                newStmts.add(c);
                Lenses lenses = new Lenses(builder.method, Map.of(access.getBase(), v1), Map.of());
                return lenses.subSt(stmt);
            }
        }
    }

    private Stmt ensureValidFieldAccess(FieldAccess access, Stmt stmt, BytecodeBlock block, List<Stmt> newStmts) {
        if (access instanceof InstanceFieldAccess instance) {
            Var base = instance.getBase();
            Type t = instance.getFieldRef().getDeclaringClass().getType();
            if (isAssignable(t, base.getType())) {
                return stmt;
            } else {
                Var v1 = requireFlowTypeVarOnDomTree(block, base, t);
                if (v1 != null) {
                    assert isAssignable(t, v1.getType());
                    Lenses lenses = new Lenses(builder.method, Map.of(base, v1), Map.of());
                    return lenses.subSt(stmt);
                } else {
                    v1 = builder.manager.getTempVar();
                    ExpModifier.setType(v1, t);
                    Cast c = getNewCast(base, v1, t);
                    c.setLineNumber(stmt.getLineNumber());
                    newStmts.add(c);
                    Lenses lenses = new Lenses(builder.method, Map.of(base, v1), Map.of());
                    return lenses.subSt(stmt);
                }
            }
        } else {
            return stmt;
        }
    }

    public void build() {
        // traverse in postOrder of domTree?
        for (BytecodeBlock block : builder.blockSortedList) {

            List<Stmt> oldStmts = getStmts(block);
            List<Stmt> newStmts = new ArrayList<>(block.getStmts().size());
            block.setStmts(newStmts);

            for (Stmt stmt : oldStmts) {
                currentStmt = stmt;
                Stmt newStmt = stmt.accept(new StmtVisitor<>() {
                    @Override
                    public Stmt visit(Copy stmt) {
                        Var right = stmt.getRValue();
                        Type t = maySplitStmt(stmt.getLValue(), right);
                        if (t == NullType.NULL) {
                            return new AssignLiteral(stmt.getLValue(), NullLiteral.get());
                        }
                        if (t != null) {
                            Var v = requireFlowTypeVarOnDomTree(block, right, t);
                            if (v != null) {
                                return stage1Transform(stmt, right, v);
                            }
                            Cast c = getNewCast(stmt.getLValue(), right, t);
                            c.setLineNumber(stmt.getLineNumber());
                            return c;
                        } else {
                            return stmt;
                        }
                    }

                    @Override
                    public Stmt visit(LoadArray stmt) {
                        stmt = (LoadArray) ensureValidArrayType(stmt.getArrayAccess(), stmt, block, newStmts);
                        Type t = maySplitStmt(stmt.getLValue(), stmt.getRValue());
                        if (t != null) {
                            Var v = builder.manager.getTempVar();
                            ExpModifier.setType(v, stmt.getRValue().getType());
                            stmt.getRValue().getBase().removeRelevantStmt(stmt);
                            newStmts.add(new LoadArray(v, stmt.getRValue()));
                            Cast c = getNewCast(stmt.getLValue(), v, t);
                            c.setLineNumber(stmt.getLineNumber());
                            return c;
                        } else {
                            return stmt;
                        }
                    }

                    @Override
                    public Stmt visit(StoreArray stmt) {
                        // stmt = (StoreArray) ensureValidArrayType(stmt.getArrayAccess(), stmt, block, newStmts);
                        // Type t = maySplitStmt(stmt.getLValue(), stmt.getRValue());
                        // if (t != null) {
                        //     Var v = builder.manager.getTempVar();
                        //     v.setType(t);
                        //     stmt.getLValue().getBase().removeRelevantStmt(stmt);
                        //     newStmts.add(getNewCast(v, stmt.getRValue(), t));
                        //     return new StoreArray(stmt.getLValue(), v);
                        // } else {
                        //     return stmt;
                        // }
                        // Ignore array store for now
                        // because it will throw `ArrayStoreException`,
                        // instead of `ClassCastException`,
                        // insert cast here may change runtime behavior
                        return stmt;
                    }

                    @Override
                    public Stmt visit(StoreField stmt) {
                        stmt = (StoreField) ensureValidFieldAccess(stmt.getFieldAccess(), stmt, block, newStmts);
                        Type t = maySplitStmt(stmt.getLValue(), stmt.getRValue());
                        if (t != null) {
                            Var v = builder.manager.getTempVar();
                            ExpModifier.setType(v, t);
                            if (stmt.getFieldAccess() instanceof InstanceFieldAccess access) {
                                access.getBase().removeRelevantStmt(stmt);
                            }
                            Cast c = getNewCast(v, stmt.getRValue(), t);
                            c.setLineNumber(stmt.getLineNumber());
                            newStmts.add(c);
                            return new StoreField(stmt.getLValue(), v);
                        } else {
                            return stmt;
                        }
                    }

                    @Override
                    public Stmt visit(LoadField stmt) {
                        return ensureValidFieldAccess(stmt.getFieldAccess(), stmt, block, newStmts);
                    }

                    @Override
                    public Stmt visit(Invoke stmt) {
                        Invoke prevStmt = stmt;
                        if (stmt.getRValue() instanceof InvokeInstanceExp invokeInstanceExp) {
                            JClass jClass = stmt.getRValue().getMethodRef().getDeclaringClass();
                            assert jClass != null;
                            Type t = jClass.getType();
                            Type baseType = invokeInstanceExp.getBase().getType();
                            Var base = invokeInstanceExp.getBase();
                            if (!isAssignable(t, baseType)) {
                                if (! (invokeInstanceExp instanceof InvokeInterface)) {
                                    // prev stmt is new
                                    // TODO: add tests for fallback
                                    Var v = requireFlowTypeVarOnDomTree(block, invokeInstanceExp.getBase(), t);
                                    if (v != null) {
                                        // if this file is a valid bytecode class, then it's checked
                                        // which means flowType is always assignable to required type
                                        // assert isAssignable(t, v.getType());
                                        return stage1Transform(stmt, base, v);
                                    } else {
                                        logger.atTrace().log("[CASTING] fallback solution for stage1");
                                    }
                                }

                                Var v = builder.manager.getTempVar();
                                ExpModifier.setType(v, t);
                                Cast c = getNewCast(v, invokeInstanceExp.getBase(), t);
                                c.setLineNumber(stmt.getLineNumber());
                                newStmts.add(c);
                                Lenses l = new Lenses(builder.method, Map.of(invokeInstanceExp.getBase(), v), Map.of());
                                prevStmt = (Invoke) l.subSt(stmt);
                            }
                        }
                        if (stmt.isDynamic()) {
                            return prevStmt;
                        }
                        Map<Var, Var> m = null;
                        for (int i = 0; i < prevStmt.getInvokeExp().getArgCount(); ++i) {
                            Var arg = prevStmt.getInvokeExp().getArg(i);
                            Type t = prevStmt.getMethodRef().getParameterTypes().get(i);
                            if (! isAssignable(t, arg.getType())) {
                                Var v = requireFlowTypeVarOnDomTree(block, arg, t);
                                if (v != null) {
                                    prevStmt = (Invoke) stage1Transform(prevStmt, arg, v);
                                } else {
                                    v = builder.manager.getTempVar();
                                    ExpModifier.setType(v, t);
                                    Cast c = getNewCast(v, arg, t);
                                    c.setLineNumber(stmt.getLineNumber());
                                    newStmts.add(c);
                                    if (m == null) {
                                        m = new HashMap<>();
                                    }
                                    m.put(arg, v);
                                }
                            }
                        }
                        if (m != null) {
                            Lenses l = new Lenses(builder.method, m, Map.of());
                            return l.subSt(prevStmt);
                        }
                        return prevStmt;
                    }

                    @Override
                    public Stmt visit(Return stmt) {
                        Type t = builder.method.getReturnType();
                        if (stmt.getValue() != null &&
                                !isAssignable(t, stmt.getValue().getType())) {
                            Var v = builder.manager.getTempVar();
                            Cast c = getNewCast(v, stmt.getValue(), t);
                            c.setLineNumber(stmt.getLineNumber());
                            newStmts.add(c);
                            builder.manager.getRetVars().remove(stmt.getValue());
                            builder.manager.getRetVars().add(v);
                            ExpModifier.setType(v, t);
                            return new Return(v);
                        } else {
                            return stmt;
                        }
                    }

                    @Override
                    public Stmt visitDefault(Stmt stmt) {
                        return stmt;
                    }
                });

                newStmts.add(newStmt);
            }
        }
    }

    private Stmt stage1Transform(Stmt stmt, Var base, Var v) {
        Lenses l = new Lenses(builder.method, Map.of(base, v), Map.of(base, v));
        return l.subSt(stmt);
    }

    List<Stmt> getStmts(BytecodeBlock block) {
        return block.getStmts();
    }

    Pair<DefinitionStmt<?, ?>, Integer> findNewInBlock(
            BytecodeBlock block, Var target) {
        for (int idx = block.getStmts().size() - 1; idx >= 0; idx--) {
            Stmt now = block.getStmts().get(idx);
            if (now instanceof DefinitionStmt<?, ?> newStmt && newStmt.getLValue() == target) {
                // cache here?
                return new Pair<>(newStmt, idx);
            }
        }
        return null;
    }

    /**
     * Use this function to do global -> local conversion
     * @return offered variable with local type, maybe null
     */
    private Var requireFlowTypeVarOnDomTree(
            BytecodeBlock start, Var globalVar, Type targetType) {
        if (builder.isUSE_SSA()) {
            return null;
        }
        boolean rootVisited = false;
        int[] domTree = dom.getDomTree();
        // Try to get cached var along the domTree
        for (int node = graph.getIndex(start); !rootVisited; node = domTree[node]) {
            if (node == domTree[node]) {
                rootVisited = true;
            }
            BytecodeBlock block = graph.getNode(node);
            FlowTypeInfo info = new FlowTypeInfo(block, globalVar);
            if (flowTypeCache.containsKey(info) && isReachableOnlyBy(globalVar, start, block)) {
                return flowTypeCache.get(info);
            }
        }

        // Var not found in the cache along the domTree. Try to compute one.
        // Compute in the start. Notice that we should search in `newStmts`
        Var flowTypeVar = computeFlowTypeVar(start, globalVar, targetType);
        if (flowTypeVar != null) {
            return flowTypeVar;
        }
        int startIdx = graph.getIndex(start);
        if (startIdx == domTree[startIdx]) {
            // start is the root of domTree, no more blocks needed to be computed for.
            return null;
        }

        rootVisited = false;
        for (int node = domTree[startIdx]; !rootVisited; node = domTree[node]) {
            if (node == domTree[node]) {
                rootVisited = true;
            }
            BytecodeBlock block = graph.getNode(node);
            flowTypeVar = computeFlowTypeVar(block, globalVar, targetType);
            if (flowTypeVar != null && isReachableOnlyBy(globalVar, start, block)) {
                return flowTypeVar;
            }
        }

        return null;
    }

    int find(int[] numbers, int target) {
        for (int index = 0; index < numbers.length; index++) {
            if (numbers[index] == target) {
                return index;
            }
        }
        return -1;
    }

    private boolean isReachableOnlyBy(Var var, BytecodeBlock to, BytecodeBlock from) {
        int[] postOrder = dom.getPostOrder();
        int start = find(postOrder, graph.getIndex(to));
        int end = find(postOrder, graph.getIndex(from));
        assert start <= end;
        for (int i = start + 1; i < end; i++) {
            BytecodeBlock block = graph.getNode(postOrder[i]);
            if (findNewInBlock(block, var) != null) {
                return false;
            }
        }
        return true;
    }

    private Var computeFlowTypeVar(BytecodeBlock block, Var globalVar, Type targetType) {
        // 1. try to split new in this block
        //    e.g. a : java.lang.Object = new int[];
        //      => v1 = new int[];
        //         a = v1;
        //         v1 is the "flow type var"
        Pair<DefinitionStmt<?, ?>, Integer> newInBlock = findNewInBlock(block, globalVar);
        if (newInBlock != null) {
            DefinitionStmt<?, ?> def = newInBlock.first();
            if (!isAssignable(targetType, def.getRValue().getType())) {
                return null;
            }

            if (def instanceof Copy copy) {
                return copy.getRValue();
            }
            Var v1 = builder.manager.getTempVar();
            ExpModifier.setType(v1, def.getRValue().getType());
            Lenses lenses = new Lenses(builder.method, Map.of(), Map.of(globalVar, v1));
            List<Stmt> stmts = block.getStmts();
            stmts.set(newInBlock.second(), lenses.subSt(def));
            stmts.add(newInBlock.second() + 1, new Copy(globalVar, v1));
            FlowTypeInfo info = new FlowTypeInfo(block, globalVar);
            flowTypeCache.put(info, v1);
            return v1;
        }

        // TODO: 2. if frame is available,
        if (builder.isFrameUsable()) {
            return null;
        }

        // can't get flow type, use worse solution
        return null;
    }

    private record FlowTypeInfo(BytecodeBlock block, Var var) { }
}

/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.frontend.soot;

import pascal.taie.ir.DefaultNewIR;
import pascal.taie.ir.ExceptionEntry;
import pascal.taie.ir.NewIR;
import pascal.taie.ir.ProgramPoint;
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
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.FloatLiteral;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.InstanceOfExp;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInterface;
import pascal.taie.ir.exp.InvokeSpecial;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.InvokeVirtual;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.LongLiteral;
import pascal.taie.ir.exp.NegExp;
import pascal.taie.ir.exp.NewArray;
import pascal.taie.ir.exp.NewExp;
import pascal.taie.ir.exp.NewInstance;
import pascal.taie.ir.exp.NewMultiArray;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.ShiftExp;
import pascal.taie.ir.exp.StaticFieldAccess;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.UnaryExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Binary;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.Goto;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.InstanceOf;
import pascal.taie.ir.stmt.Invoke;
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
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.classes.MethodRef;
import pascal.taie.java.types.ArrayType;
import pascal.taie.java.types.ClassType;
import pascal.taie.java.types.Type;
import soot.Body;
import soot.Local;
import soot.Trap;
import soot.Unit;
import soot.UnitBox;
import soot.Value;
import soot.jimple.AbstractConstantSwitch;
import soot.jimple.AbstractJimpleValueSwitch;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.AddExpr;
import soot.jimple.AndExpr;
import soot.jimple.AnyNewExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.ClassConstant;
import soot.jimple.CmpExpr;
import soot.jimple.CmpgExpr;
import soot.jimple.CmplExpr;
import soot.jimple.ConditionExpr;
import soot.jimple.Constant;
import soot.jimple.DivExpr;
import soot.jimple.DoubleConstant;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.EqExpr;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.FieldRef;
import soot.jimple.FloatConstant;
import soot.jimple.GeExpr;
import soot.jimple.GotoStmt;
import soot.jimple.GtExpr;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InstanceOfExpr;
import soot.jimple.IntConstant;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.LeExpr;
import soot.jimple.LengthExpr;
import soot.jimple.LongConstant;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.LtExpr;
import soot.jimple.MulExpr;
import soot.jimple.NeExpr;
import soot.jimple.NegExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NopStmt;
import soot.jimple.NullConstant;
import soot.jimple.OrExpr;
import soot.jimple.RemExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.ShlExpr;
import soot.jimple.ShrExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.StringConstant;
import soot.jimple.SubExpr;
import soot.jimple.TableSwitchStmt;
import soot.jimple.ThrowStmt;
import soot.jimple.UnopExpr;
import soot.jimple.UshrExpr;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.XorExpr;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static pascal.taie.util.CollectionUtils.freeze;
import static pascal.taie.util.CollectionUtils.newHybridMap;

/**
 * Convert Jimple to Tai-e IR.
 */
class MethodIRBuilder extends AbstractStmtSwitch {

    private final JMethod method;

    private final Converter converter;

    private VarManager varManager;

    private List<Stmt> stmts;

    private List<ExceptionEntry> exceptionEntries;

    MethodIRBuilder(JMethod method, Converter converter) {
        this.method = method;
        this.converter = converter;
    }

    NewIR build() {
        Body body = method.getSootMethod().retrieveActiveBody();
        varManager = new VarManager(converter);
        stmts = new ArrayList<>();
        if (!method.isStatic()) {
            buildThis(body.getThisLocal());
        }
        buildParams(body.getParameterLocals());
        buildStmts(body);
        buildExceptionEntries(body.getTraps());
        return new DefaultNewIR(method,
                varManager.getThis(), freeze(varManager.getParams()),
                freeze(varManager.getVars()), freeze(stmts),
                freeze(exceptionEntries));
    }

    private void buildThis(Local thisLocal) {
        varManager.addThis(thisLocal);
    }

    private void buildParams(List<Local> params) {
        params.forEach(varManager::addParam);
    }

    private void buildStmts(Body body) {
        if (!body.getTraps().isEmpty()) {
            trapUnits = body.getTraps()
                    .stream()
                    .map(Trap::getUnitBoxes)
                    .flatMap(List::stream)
                    .map(UnitBox::getUnit)
                    .collect(Collectors.toSet());
            trapUnitMap = new HashMap<>(body.getTraps().size() * 3);
        }
        body.getUnits().forEach(unit -> unit.apply(this));
        linkJumpTargets(jumpMap, jumpTargetMap);
    }

    private static void linkJumpTargets(
            Map<Unit, Stmt> jumpMap, Map<Unit, Stmt> jumpTargetMap) {
        jumpMap.forEach((unit, stmt) -> {
            if (unit instanceof GotoStmt) {
                GotoStmt jimpleGoto = (GotoStmt) unit;
                Goto taieGoto = (Goto) stmt;
                taieGoto.setTarget(jumpTargetMap.get(jimpleGoto.getTarget()));
            } else if (unit instanceof IfStmt) {
                IfStmt jimpleIf = (IfStmt) unit;
                If taieIf = (If) stmt;
                taieIf.setTarget(jumpTargetMap.get(jimpleIf.getTarget()));
            } else if (unit instanceof soot.jimple.SwitchStmt) {
                soot.jimple.SwitchStmt jimpleSwitch
                        = (soot.jimple.SwitchStmt) unit;
                SwitchStmt taieSwitch = (SwitchStmt) stmt;
                taieSwitch.setTargets(jimpleSwitch.getTargets()
                        .stream()
                        .map(jumpTargetMap::get)
                        .collect(Collectors.toList()));
                taieSwitch.setDefaultTarget(
                        jumpTargetMap.get(jimpleSwitch.getDefaultTarget()));
            }
        });
    }

    private void buildExceptionEntries(Chain<Trap> traps) {
        if (traps.isEmpty()) {
            exceptionEntries = Collections.emptyList();
        } else {
            exceptionEntries = new ArrayList<>(traps.size());
            for (Trap trap : traps) {
                Unit begin = trap.getBeginUnit();
                Unit end = trap.getEndUnit();
                Unit handler = trap.getHandlerUnit();
                soot.Type catchType = trap.getException().getType();
                exceptionEntries.add(new ExceptionEntry(trapUnitMap.get(begin),
                        trapUnitMap.get(end),
                        (Catch) trapUnitMap.get(handler),
                        (ClassType) converter.convertType(catchType)));
            }
        }
    }

    /**
     * Current Jimple unit being converted.
     */
    private Unit currentUnit;

    /**
     * Map from jump statements in Jimple to the corresponding Tai-e statements.
     */
    private final Map<Unit, Stmt> jumpMap = newHybridMap();

    /**
     * Map from jump target statements in Jimple to the corresponding Tai-e statements.
     */
    private final Map<Unit, Stmt> jumpTargetMap = newHybridMap();

    /**
     * All trap-related units of current Jimple body.
     */
    private Set<Unit> trapUnits = Collections.emptySet();

    /**
     * Map from trap beginning statements in Jimple to
     * the corresponding Tai-e statements.
     */
    private Map<Unit, Stmt> trapUnitMap = Collections.emptyMap();

    /**
     * If {@link #currentUnit} is a jump target (or trap begin unit,
     * which can be seen as the beginning target of exception scope) in Jimple,
     * then this field holds the corresponding statement in Tai-e IR.
     * This field is useful when converting the Jimple statements that
     * contain constant values, as Tai-e IR will emit {@link AssignLiteral}
     * for constant values before the actual corresponding {@link Stmt}.
     *
     * For example, consider following Jimple code:
     * if a > b goto label1;
     * label1:
     *     x = 1 + y;
     *
     * In Tai-e IR, above code will be converted to this:
     * if a > b goto label1;
     * label1:
     *     #intconstant0 = 1; // <-- tempJumpTarget
     *     x = #intconstant0 + y;
     *
     * Tai-e adds an {@link AssignLiteral} statement before the
     * corresponding addition, so we need to set the target
     * of {@link If} to the tempTarget instead of the addition.
     * We use this field to maintain temporary targets.
     */
    private Stmt tempTarget;

    private void addTempStmt(Stmt stmt) {
        if (tempTarget == null) {
            tempTarget = stmt;
        }
        processNewStmt(stmt);
    }

    private void addStmt(Stmt stmt) {
        if (!currentUnit.getBoxesPointingToThis().isEmpty()) {
            if (tempTarget != null) {
                jumpTargetMap.put(currentUnit, tempTarget);
            } else {
                jumpTargetMap.put(currentUnit, stmt);
            }
        }
        if (trapUnits.contains(currentUnit)) {
            if (tempTarget != null) {
                trapUnitMap.put(currentUnit, tempTarget);
            } else {
                trapUnitMap.put(currentUnit, stmt);
            }
        }
        tempTarget = null;
        processNewStmt(stmt);
    }

    private void processNewStmt(Stmt stmt) {
        stmt.setLineNumber(currentUnit.getJavaSourceStartLineNumber());
        stmt.setIndex(stmts.size());
        stmts.add(stmt);
    }

    /**
     * Convert Jimple Constants to Literals.
     */
    private final AbstractConstantSwitch constantConverter
            = new AbstractConstantSwitch() {

        @Override
        public void caseDoubleConstant(DoubleConstant v) {
            setResult(DoubleLiteral.get(v.value));
        }

        @Override
        public void caseFloatConstant(FloatConstant v) {
            setResult(FloatLiteral.get(v.value));
        }

        @Override
        public void caseIntConstant(IntConstant v) {
            setResult(IntLiteral.get(v.value));
        }

        @Override
        public void caseLongConstant(LongConstant v) {
            setResult(LongLiteral.get(v.value));
        }

        @Override
        public void caseNullConstant(NullConstant v) {
            setResult(NullLiteral.get());
        }

        @Override
        public void caseStringConstant(StringConstant v) {
            setResult(StringLiteral.get(v.value));
        }

        @Override
        public void caseClassConstant(ClassConstant v) {
            setResult(ClassLiteral.get(getType(v.value)));
        }

        @Override
        public void defaultCase(Object v) {
            throw new SootFrontendException(
                    "Cannot convert constant: " + v);
        }
    };

    @Override
    public void caseAssignStmt(AssignStmt stmt) {
        currentUnit = stmt;
        Value lhs = stmt.getLeftOp();
        Value rhs = stmt.getRightOp();
        if (rhs instanceof InvokeExpr) {
            buildInvoke((Local) lhs, stmt.getInvokeExpr());
            return;
        }
        if (lhs instanceof Local) {
            Local lvar = (Local) lhs;
            if (rhs instanceof Local) {
                addStmt(new Copy(getVar(lvar), getVar((Local) rhs)));
            } else if (rhs instanceof AnyNewExpr) {
                buildNew(lvar, (AnyNewExpr) rhs);
            } else if (rhs instanceof Constant) {
                rhs.apply(constantConverter);
                addStmt(new AssignLiteral(getVar(lvar),
                        (Literal) constantConverter.getResult()));
            } else if (rhs instanceof FieldRef) {
                addStmt(new LoadField(getVar(lvar),
                        getFieldAccess((FieldRef) rhs)));
            } else if (rhs instanceof ArrayRef) {
                addStmt(new LoadArray(getVar(lvar),
                        getArrayAccess((ArrayRef) rhs)));
            } else if (rhs instanceof BinopExpr) {
                buildBinary(lvar, (BinopExpr) rhs);
            } else if (rhs instanceof UnopExpr) {
                buildUnary(lvar, (UnopExpr) rhs);
            } else if (rhs instanceof InstanceOfExpr) {
                buildInstanceOf(lvar, (InstanceOfExpr) rhs);
            } else if (rhs instanceof CastExpr) {
                buildCast(lvar, (CastExpr) rhs);
            } else {
                throw new SootFrontendException(
                        "Cannot handle AssignStmt: " + stmt);
            }
        } else if (lhs instanceof FieldRef) {
            addStmt(new StoreField(
                    getFieldAccess((FieldRef) lhs), getLocalOrConstant(rhs)));
        } else if (lhs instanceof ArrayRef) {
            addStmt(new StoreArray(
                    getArrayAccess((ArrayRef) lhs), getLocalOrConstant(rhs)));
        } else {
            throw new SootFrontendException(
                    "Cannot handle AssignStmt: " + stmt);
        }
    }

    /**
     * Shortcut: obtain Jimple Value's Type and convert to Tai-e Type.
     */
    private Type getType(Value value) {
        return converter.convertType(value.getType());
    }

    private Type getType(String typeName) {
        throw new UnsupportedOperationException();
    }

    /**
     * Shortcut: convert Jimple Local to Var.
     */
    private Var getVar(Local local) {
        return varManager.getVar(local);
    }

    /**
     * Convert a Jimple Local or Constant to Var.
     * If <code>value</code> is Local, then directly return the corresponding Var.
     * If <code>value</code> is Constant, then add a temporary assignment,
     * e.g., x = 10 for constant 10, and return Var x.
     */
    private Var getLocalOrConstant(Value value) {
        if (value instanceof Local) {
            return getVar((Local) value);
        } else if (value instanceof Constant) {
            value.apply(constantConverter);
            Literal rvalue = (Literal) constantConverter.getResult();
            Var lvalue = varManager.newConstantVar(rvalue);
            addTempStmt(new AssignLiteral(lvalue, rvalue));
            return lvalue;
        }
        throw new SootFrontendException("Expected Local or Constant, given " + value);
    }

    /**
     * Convert Jimple FieldRef to FieldAccess.
     */
    private FieldAccess getFieldAccess(FieldRef fieldRef) {
        pascal.taie.java.classes.FieldRef jfieldRef =
                converter.convertFieldRef(fieldRef.getFieldRef());
        if (fieldRef instanceof InstanceFieldRef) {
            return new InstanceFieldAccess(jfieldRef,
                    getVar((Local) ((InstanceFieldRef) fieldRef).getBase()));
        } else {
            assert fieldRef instanceof StaticFieldRef;
            return new StaticFieldAccess(jfieldRef);
        }
    }

    /**
     * Convert Jimple ArrayRef to ArrayAccess.
     */
    private ArrayAccess getArrayAccess(ArrayRef arrayRef) {
        return new ArrayAccess(getVar((Local) arrayRef.getBase()),
                getLocalOrConstant(arrayRef.getIndex()));
    }

    /**
     * Convert Jimple NewExpr to NewExp
     */
    private final AbstractJimpleValueSwitch newExprConverter
            = new AbstractJimpleValueSwitch() {

        @Override
        public void caseNewExpr(NewExpr v) {
            setResult(new NewInstance((ClassType) getType(v)));
        }

        @Override
        public void caseNewArrayExpr(NewArrayExpr v) {
            setResult(new NewArray((ArrayType) getType(v),
                    getLocalOrConstant(v.getSize())));
        }

        @Override
        public void caseNewMultiArrayExpr(NewMultiArrayExpr v) {
            List<Var> lengths = v.getSizes()
                    .stream()
                    .map(MethodIRBuilder.this::getLocalOrConstant)
                    .collect(Collectors.toList());
            setResult(new NewMultiArray((ArrayType) getType(v), lengths));
        }
    };

    private void buildNew(Local lhs, AnyNewExpr rhs) {
        rhs.apply(newExprConverter);
        NewExp newExp = (NewExp) newExprConverter.getResult();
        New newStmt = new New(getVar(lhs), newExp);
        newExp.setAllocationSite(new ProgramPoint(method, newStmt));
        addStmt(newStmt);
    }

    /**
     * Extract BinaryExp.Op from Jimple BinopExpr.
     */
    private final AbstractJimpleValueSwitch binaryOpExtractor
            = new AbstractJimpleValueSwitch() {

        // ---------- Arithmetic expression ----------
        @Override
        public void caseAddExpr(AddExpr v) {
            setResult(ArithmeticExp.Op.ADD);
        }

        @Override
        public void caseSubExpr(SubExpr v) {
            setResult(ArithmeticExp.Op.SUB);
        }

        @Override
        public void caseMulExpr(MulExpr v) {
            setResult(ArithmeticExp.Op.MUL);
        }

        @Override
        public void caseDivExpr(DivExpr v) {
            setResult(ArithmeticExp.Op.DIV);
        }

        @Override
        public void caseRemExpr(RemExpr v) {
            setResult(ArithmeticExp.Op.REM);
        }

        // ---------- Bitwise expression ----------
        @Override
        public void caseAndExpr(AndExpr v) {
            setResult(BitwiseExp.Op.AND);
        }

        @Override
        public void caseOrExpr(OrExpr v) {
            setResult(BitwiseExp.Op.OR);
        }

        @Override
        public void caseXorExpr(XorExpr v) {
            setResult(BitwiseExp.Op.XOR);
        }

        // ---------- Comparison expression ----------
        @Override
        public void caseCmpExpr(CmpExpr v) {
            setResult(ComparisonExp.Op.CMP);
        }

        @Override
        public void caseCmplExpr(CmplExpr v) {
            setResult(ComparisonExp.Op.CMPL);
        }

        @Override
        public void caseCmpgExpr(CmpgExpr v) {
            setResult(ComparisonExp.Op.CMPG);
        }

        // ---------- Shift expression ----------
        @Override
        public void caseShlExpr(ShlExpr v) {
            setResult(ShiftExp.Op.SHL);
        }

        @Override
        public void caseShrExpr(ShrExpr v) {
            setResult(ShiftExp.Op.SHR);
        }

        @Override
        public void caseUshrExpr(UshrExpr v) {
            setResult(ShiftExp.Op.USHR);
        }

        @Override
        public void defaultCase(Object v) {
            throw new SootFrontendException(
                    "Expected binary expression, given " + v);
        }
    };

    private void buildBinary(Local lhs, BinopExpr rhs) {
        rhs.apply(binaryOpExtractor);
        BinaryExp.Op op = (BinaryExp.Op) binaryOpExtractor.getResult();
        BinaryExp binaryExp;
        Var v1 = getLocalOrConstant(rhs.getOp1());
        Var v2 = getLocalOrConstant(rhs.getOp2());
        if (op instanceof ArithmeticExp.Op) {
            binaryExp = new ArithmeticExp((ArithmeticExp.Op) op, v1, v2);
        } else if (op instanceof ComparisonExp.Op) {
            binaryExp = new ComparisonExp((ComparisonExp.Op) op, v1, v2);
        } else if (op instanceof BitwiseExp.Op) {
            binaryExp = new BitwiseExp((BitwiseExp.Op) op, v1, v2);
        } else if (op instanceof ShiftExp.Op) {
            binaryExp = new ShiftExp((ShiftExp.Op) op, v1, v2);
        } else {
            throw new SootFrontendException("Cannot handle BinopExpr: " + rhs);
        }
        addStmt(new Binary(getVar(lhs), binaryExp));
    }

    private void buildUnary(Local lhs, UnopExpr rhs) {
        Var v = getLocalOrConstant(rhs.getOp());
        UnaryExp unaryExp;
        if (rhs instanceof NegExpr) {
            unaryExp = new NegExp(v);
        } else if (rhs instanceof LengthExpr) {
            unaryExp = new ArrayLengthExp(v);
        } else {
            throw new SootFrontendException("Cannot handle UnopExpr: " + rhs);
        }
        addStmt(new Unary(getVar(lhs), unaryExp));
    }

    private void buildInstanceOf(Local lhs, InstanceOfExpr rhs) {
        InstanceOfExp instanceOfExp = new InstanceOfExp(
                getLocalOrConstant(rhs.getOp()),
                converter.convertType(rhs.getCheckType()));
        addStmt(new InstanceOf(getVar(lhs), instanceOfExp));
    }

    private void buildCast(Local lhs, CastExpr rhs) {
        CastExp castExp = new CastExp(
                getLocalOrConstant(rhs.getOp()),
                converter.convertType(rhs.getCastType()));
        addStmt(new Cast(getVar(lhs), castExp));
    }

    @Override
    public void caseGotoStmt(GotoStmt stmt) {
        currentUnit = stmt;
        Goto gotoStmt = new Goto();
        jumpMap.put(currentUnit, gotoStmt);
        addStmt(gotoStmt);
    }

    @Override
    public void caseIfStmt(IfStmt stmt) {
        currentUnit = stmt;
        ConditionExpr condition = (ConditionExpr) stmt.getCondition();
        ConditionExp.Op op;
        if (condition instanceof EqExpr) {
            op = ConditionExp.Op.EQ;
        } else if (condition instanceof NeExpr) {
            op = ConditionExp.Op.NE;
        } else if (condition instanceof LtExpr) {
            op = ConditionExp.Op.LT;
        } else if (condition instanceof GtExpr) {
            op = ConditionExp.Op.GT;
        } else if (condition instanceof LeExpr) {
            op = ConditionExp.Op.LE;
        } else if (condition instanceof GeExpr) {
            op = ConditionExp.Op.GE;
        } else {
            throw new SootFrontendException(
                    "Expected conditional expression, given: " + condition);
        }
        Var v1 = getLocalOrConstant(condition.getOp1());
        Var v2 = getLocalOrConstant(condition.getOp2());
        If ifStmt = new If(new ConditionExp(op, v1, v2));
        jumpMap.put(currentUnit, ifStmt);
        addStmt(ifStmt);
    }

    @Override
    public void caseLookupSwitchStmt(LookupSwitchStmt stmt) {
        currentUnit = stmt;
        Var var = getLocalOrConstant(stmt.getKey());
        List<Integer> caseValues = stmt.getLookupValues()
                .stream()
                .map(v -> v.value)
                .collect(Collectors.toList());
        LookupSwitch lookupSwitch = new LookupSwitch(var, caseValues);
        jumpMap.put(currentUnit, lookupSwitch);
        addStmt(lookupSwitch);

    }

    @Override
    public void caseTableSwitchStmt(TableSwitchStmt stmt) {
        currentUnit = stmt;
        Var var = getLocalOrConstant(stmt.getKey());
        TableSwitch tableSwitch = new TableSwitch(var,
                stmt.getLowIndex(), stmt.getHighIndex());
        jumpMap.put(currentUnit, tableSwitch);
        addStmt(tableSwitch);
    }

    @Override
    public void caseInvokeStmt(InvokeStmt stmt) {
        currentUnit = stmt;
        buildInvoke(null, stmt.getInvokeExpr());
    }

    private void buildInvoke(Local lhs, InvokeExpr invokeExpr) {
        Var result = lhs == null ? null : getVar(lhs);
        InvokeExp invokeExp = getInvokeExp(invokeExpr);
        Invoke invoke = new Invoke(invokeExp, result);
        invokeExp.setCallSite(new ProgramPoint(method, invoke));
        addStmt(invoke);
    }

    /**
     * Convert Jimple InvokeExpr to InvokeExp.
     */
    private InvokeExp getInvokeExp(InvokeExpr invokeExpr) {
        MethodRef methodRef = converter
                .convertMethodRef(invokeExpr.getMethodRef());
        List<Var> args = invokeExpr.getArgs()
                .stream()
                .map(this::getLocalOrConstant)
                .collect(Collectors.toList());
        if (invokeExpr instanceof InstanceInvokeExpr) {
            Var base = getVar(
                    (Local) ((InstanceInvokeExpr) invokeExpr).getBase());
            if (invokeExpr instanceof VirtualInvokeExpr) {
                return new InvokeVirtual(methodRef, base, args);
            } else if (invokeExpr instanceof InterfaceInvokeExpr) {
                return new InvokeInterface(methodRef, base, args);
            } else if (invokeExpr instanceof SpecialInvokeExpr) {
                return new InvokeSpecial(methodRef, base, args);
            }
        } else if (invokeExpr instanceof StaticInvokeExpr) {
            return new InvokeStatic(methodRef, args);
        }
        // TODO: handle invokedynamic
        throw new SootFrontendException(
                "Cannot handle InvokeExpr: " + invokeExpr);
    }

    @Override
    public void caseReturnStmt(ReturnStmt stmt) {
        currentUnit = stmt;
        addStmt(new Return(getLocalOrConstant(stmt.getOp())));
    }

    @Override
    public void caseReturnVoidStmt(ReturnVoidStmt stmt) {
        currentUnit = stmt;
        addStmt(new Return());
    }

    @Override
    public void caseThrowStmt(ThrowStmt stmt) {
        currentUnit = stmt;
        addStmt(new Throw(getLocalOrConstant(stmt.getOp())));
    }

    @Override
    public void caseIdentityStmt(IdentityStmt stmt) {
        currentUnit = stmt;
        Value lhs = stmt.getLeftOp();
        Value rhs = stmt.getRightOp();
        if (rhs instanceof CaughtExceptionRef) {
            addStmt(new Catch(getVar((Local) lhs)));
        }
    }

    @Override
    public void caseEnterMonitorStmt(EnterMonitorStmt stmt) {
        currentUnit = stmt;
        addStmt(new Monitor(Monitor.Op.ENTER,
                getLocalOrConstant(stmt.getOp())));
    }

    @Override
    public void caseExitMonitorStmt(ExitMonitorStmt stmt) {
        currentUnit = stmt;
        addStmt(new Monitor(Monitor.Op.EXIT,
                getLocalOrConstant(stmt.getOp())));
    }

    @Override
    public void caseNopStmt(NopStmt stmt) {
        currentUnit = stmt;
        addStmt(new Nop());
    }

    @Override
    public void defaultCase(Object obj) {
        System.out.println("Unhandled Stmt: " + obj);
    }
}

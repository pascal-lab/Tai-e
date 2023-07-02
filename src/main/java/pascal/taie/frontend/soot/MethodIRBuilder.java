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

package pascal.taie.frontend.soot;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.ir.DefaultIR;
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
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.FloatLiteral;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.InstanceOfExp;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.InvokeExp;
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
import pascal.taie.ir.exp.NewInstance;
import pascal.taie.ir.exp.NewMultiArray;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.ShiftExp;
import pascal.taie.ir.exp.StaticFieldAccess;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.UnaryExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.proginfo.MemberRef;
import pascal.taie.ir.proginfo.MethodRef;
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
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.CollectionUtils;
import pascal.taie.util.collection.Lists;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Trap;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
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
import soot.jimple.DynamicInvokeExpr;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import static pascal.taie.language.type.VoidType.VOID;

/**
 * Converts Jimple to Tai-e IR.
 */
class MethodIRBuilder extends AbstractStmtSwitch<Void> {

    private static final Logger logger = LogManager.getLogger(MethodIRBuilder.class);

    private final JMethod method;

    private final Converter converter;

    private VarManager varManager;

    private Set<Var> returnVars;

    private List<Stmt> stmts;

    private List<ExceptionEntry> exceptionEntries;

    MethodIRBuilder(JMethod method, Converter converter) {
        this.method = method;
        this.converter = converter;
    }

    IR build() {
        SootMethod m = (SootMethod) method.getMethodSource();
        Body body = m.retrieveActiveBody();
        m.releaseActiveBody(); // release body to save memory
        varManager = new VarManager(method, converter);
        if (method.getReturnType().equals(VOID)) {
            returnVars = Set.of();
        } else {
            returnVars = Sets.newLinkedSet();
        }
        stmts = new ArrayList<>();
        if (!method.isStatic()) {
            buildThis(body.getThisLocal());
        }
        buildParams(body.getParameterLocals());
        preprocessTemps(body,
                tempToDef = Maps.newHybridMap(),
                unusedInvokeTempRets = Sets.newHybridSet());
        buildStmts(body);
        buildExceptionEntries(body);
        return new DefaultIR(method,
                varManager.getThis(), varManager.getParams(), returnVars,
                varManager.getVars(), stmts, exceptionEntries);
    }

    private void buildThis(Local thisLocal) {
        varManager.addThis(thisLocal);
    }

    private void buildParams(List<Local> params) {
        varManager.addParams(params);
    }

    private void buildStmts(Body body) {
        if (!body.getTraps().isEmpty()) {
            trapUnits = Sets.newSet();
            body.getTraps().forEach(trap -> {
                trapUnits.add(trap.getBeginUnit());
                trapUnits.add(trap.getEndUnit());
                trapUnits.add(findRealHandler(body, trap.getHandlerUnit()));
            });
            trapUnitMap = Maps.newMap(body.getTraps().size() * 3);
        }
        body.getUnits().forEach(unit -> unit.apply(this));
        linkJumpTargets(jumpMap, jumpTargetMap);
    }

    /**
     * Finds out the real exception handler in {@link Trap}.
     * When Soot uses Java frontend, the handler unit in {@link Trap} is
     * a {@link NopStmt} instead of catch statement, and the catch statement
     * follows the {@link NopStmt} in {@link Chain}. To ensure that the
     * exception handler of Tai-e IR is {@link Catch}, we find out the real
     * exception handler in Jimple and so that it can be mapped to {@link Catch}.
     *
     * @param body the method body being processed.
     * @param unit the handler unit in {@link Trap}.
     * @return the real exception handler, i.e., e = @caughtexception;
     */
    private static Unit findRealHandler(Body body, Unit unit) {
        while (!isJimpleCatch(unit)) {
            // if unit is not a Jimple catch statement, traverse the unit chain
            unit = body.getUnits().getSuccOf(unit);
        }
        return unit;
    }

    private static boolean isJimpleCatch(Unit unit) {
        return unit instanceof IdentityStmt identity &&
                identity.getRightOp() instanceof CaughtExceptionRef;
    }

    private static void linkJumpTargets(
            Map<Unit, Stmt> jumpMap, Map<Unit, Stmt> jumpTargetMap) {
        jumpMap.forEach((unit, stmt) -> {
            if (unit instanceof GotoStmt jimpleGoto) {
                Goto taieGoto = (Goto) stmt;
                taieGoto.setTarget(jumpTargetMap.get(jimpleGoto.getTarget()));
            } else if (unit instanceof IfStmt jimpleIf) {
                If taieIf = (If) stmt;
                taieIf.setTarget(jumpTargetMap.get(jimpleIf.getTarget()));
            } else if (unit instanceof soot.jimple.SwitchStmt jimpleSwitch) {
                SwitchStmt taieSwitch = (SwitchStmt) stmt;
                taieSwitch.setTargets(Lists.map(jimpleSwitch.getTargets(),
                        jumpTargetMap::get));
                taieSwitch.setDefaultTarget(
                        jumpTargetMap.get(jimpleSwitch.getDefaultTarget()));
            }
        });
    }

    private void buildExceptionEntries(Body body) {
        Chain<Trap> traps = body.getTraps();
        if (traps.isEmpty()) {
            exceptionEntries = List.of();
        } else {
            exceptionEntries = new ArrayList<>(traps.size());
            for (Trap trap : traps) {
                Unit begin = trap.getBeginUnit();
                Unit end = trap.getEndUnit();
                Unit handler = findRealHandler(body, trap.getHandlerUnit());
                soot.Type catchType = trap.getException().getType();
                exceptionEntries.add(new ExceptionEntry(
                        trapUnitMap.get(begin),
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
    private final Map<Unit, Stmt> jumpMap = Maps.newHybridMap();

    /**
     * Map from jump target statements in Jimple to the corresponding Tai-e statements.
     */
    private final Map<Unit, Stmt> jumpTargetMap = Maps.newHybridMap();

    /**
     * All trap-related units of current Jimple body.
     */
    private Set<Unit> trapUnits = Set.of();

    /**
     * Map from trap beginning statements in Jimple to
     * the corresponding Tai-e statements.
     */
    private Map<Unit, Stmt> trapUnitMap = Map.of();

    /**
     * If {@link #currentUnit} is a jump target (or trap begin unit,
     * which can be seen as the beginning target of exception scope) in Jimple,
     * then this field holds the corresponding statement in Tai-e IR.
     * This field is useful when converting the Jimple statements that
     * contain constant values, as Tai-e IR will emit {@link AssignLiteral}
     * for constant values before the actual corresponding {@link Stmt}.
     * <p>
     * For example, consider following Jimple code:
     * if a > b goto label1;
     * label1:
     * x = 1 + y;
     * <p>
     * In Tai-e IR, above code will be converted to this:
     * if a > b goto label1;
     * label1:
     * %intconst0 = 1; // &lt;-- tempTarget
     * x = %intconst0 + y;
     * <p>
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
     * Map from temp variable to its definition stmt, e.g.,
     * for temp$i = x, we map temp$i -> temp$i = x.
     */
    private Map<Local, AssignStmt> tempToDef;

    /**
     * Set of temp variables that have been processed in {@link #getVar(Local)}.
     */
    private final Set<Local> processedTemps = Sets.newHybridSet();

    /**
     * Set of temp variables that receives invocation results but never used,
     * e.g., temp$i = foo(), but temp$i is never used.
     */
    private Set<Local> unusedInvokeTempRets;

    /**
     * Collects information about special temp variables generated by Soot frontend:
     * (1) collects the temp variables and their relevant definition statements;
     * (2) collects the temp variables that receive invocation results but never used.
     * TODO: remove this step for body parsed from .class files.
     */
    private static void preprocessTemps(Body body,
                                        Map<Local, AssignStmt> tempToDef,
                                        Set<Local> unusedInvokeTempRets) {
        MultiMap<Local, AssignStmt> tempToAssigns = Maps.newMultiMap();
        MultiMap<Local, Unit> tempToUses = Maps.newMultiMap();
        for (Unit unit : body.getUnits()) {
            if (unit instanceof AssignStmt assign) {
                Value lhs = assign.getLeftOp();
                if (lhs instanceof Local var) {
                    if (var.getName().startsWith("temp$")) {
                        tempToAssigns.put(var, assign);
                        if (assign.containsInvokeExpr()) {
                            unusedInvokeTempRets.add(var);
                        }
                    }
                }
            }
            // collect the uses of temp variables
            unit.getUseBoxes()
                    .stream()
                    .map(ValueBox::getValue)
                    .forEach(value -> {
                        if (value instanceof Local var) {
                            if (var.getName().startsWith("temp$")) {
                                tempToUses.put(var, unit);
                                unusedInvokeTempRets.remove(var);
                            }
                        }
                    });
        }
        tempToAssigns.forEachSet((var, assigns) -> {
            if (assigns.size() == 1) {
                AssignStmt assign = CollectionUtils.getOne(assigns);
                Value rhs = assign.getRightOp();
                if ((rhs instanceof Constant ||
                        rhs instanceof Local ||
                        rhs instanceof BinopExpr) &&
                        tempToUses.get(var).size() <= 1) {
                    // if multiple units use a temp variable, then we don't
                    // inline the RHS expression, because the value of RHS may
                    // change between the two units, and lead to wrong result.
                    // y = ++x; could trigger this issue.
                    tempToDef.put(var, assign);
                }
            }
        });
    }

    private boolean isTempVar(Local local) {
        return tempToDef.containsKey(local);
    }

    /**
     * Converts Jimple Constants to Literals.
     */
    private final AbstractConstantSwitch<Literal> constantConverter
            = new AbstractConstantSwitch<>() {

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
            Type type = converter.convertType(v.toSootType());
            setResult(ClassLiteral.get(type));
        }

        @Override
        public void caseMethodHandle(soot.jimple.MethodHandle v) {
            MethodHandle.Kind kind = MethodHandle.Kind.get(v.getKind());
            MemberRef memberRef = v.isMethodRef() ?
                    converter.convertMethodRef(v.getMethodRef()) :
                    converter.convertFieldRef(v.getFieldRef());
            setResult(MethodHandle.get(kind, memberRef));
        }

        @Override
        public void caseMethodType(soot.jimple.MethodType v) {
            List<Type> paramTypes = Lists.map(v.getParameterTypes(),
                    converter::convertType);
            Type returnType = converter.convertType(v.getReturnType());
            setResult(MethodType.get(paramTypes, returnType));
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
        if (lhs instanceof Local lvar) {
            if (rhs instanceof Local rvar) {
                if (isTempVar(rvar)) {
                    // for assignment like x = temp$i, if we have recorded
                    // definition to temp$i, then we directly replace temp$i
                    // by its definition value.
                    // FIXME (?): this optimization reduces number of temporary
                    //  variables, but it does not work with
                    //  System.setProperty("ENABLE_JIMPLE_OPT", "true");
                    Value defValue = tempToDef.get(rvar).getRightOp();
                    if (defValue instanceof Constant) {
                        defValue.apply(constantConverter);
                        addStmt(new AssignLiteral(getVar(lvar),
                                constantConverter.getResult()));
                    } else if (defValue instanceof Local) {
                        addStmt(new Copy(getVar(lvar), getVar((Local) defValue)));
                    } else if (defValue instanceof BinopExpr) {
                        buildBinary(lvar, (BinopExpr) defValue);
                    }
                } else if (!isTempVar(lvar)) {
                    addStmt(new Copy(getVar(lvar), getVar(rvar)));
                }
            } else if (rhs instanceof AnyNewExpr) {
                buildNew(lvar, (AnyNewExpr) rhs);
            } else if (rhs instanceof Constant) {
                if (!isTempVar(lvar)) {
                    rhs.apply(constantConverter);
                    addStmt(new AssignLiteral(getVar(lvar),
                            constantConverter.getResult()));
                }
            } else if (rhs instanceof FieldRef) {
                addStmt(new LoadField(getVar(lvar),
                        getFieldAccess((FieldRef) rhs)));
            } else if (rhs instanceof ArrayRef) {
                addStmt(new LoadArray(getVar(lvar),
                        getArrayAccess((ArrayRef) rhs)));
            } else if (rhs instanceof BinopExpr) {
                if (!isTempVar(lvar)) {
                    buildBinary(lvar, (BinopExpr) rhs);
                }
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
     * Shortcut: obtains Jimple Value's Type and convert to Tai-e Type.
     */
    private Type getTypeOf(Value value) {
        return converter.convertType(value.getType());
    }

    /**
     * Shortcut: converts Jimple Local to Var.
     */
    private Var getVar(Local local) {
        if (tempToDef.containsKey(local)) {
            // handle the case when the given local is recorded in tempToDef
            AssignStmt def = tempToDef.get(local);
            Value defValue = def.getRightOp();
            if (defValue instanceof Local) {
                // return x for case temp$i = x
                return varManager.getVar((Local) defValue);
            }
            // add (skipped) assignment for the temp var if necessary
            if (!processedTemps.contains(local)) {
                processedTemps.add(local);
                Unit temp = currentUnit;
                currentUnit = def;
                if (defValue instanceof Constant) {
                    defValue.apply(constantConverter);
                    addStmt(new AssignLiteral(varManager.getVar(local),
                            constantConverter.getResult()));
                } else if (defValue instanceof BinopExpr) {
                    buildBinary(local, (BinopExpr) defValue);
                } else {
                    throw new SootFrontendException(
                            "Expected Local, Constant or BinopExpr, given " + defValue);
                }
                currentUnit = temp;
            }
        }
        return varManager.getVar(local);
    }

    /**
     * Caches variables that hold constant values, so that we don't need to
     * create multiple temp variables and assignments for the same constants
     * in the same method.
     */
    private final Map<Literal, Var> constantVars = Maps.newHybridMap();

    /**
     * Converts a Jimple Local or Constant to Var.
     * If <code>value</code> is Local, then directly return the corresponding Var.
     * If <code>value</code> is Constant, then add a temporary assignment,
     * e.g., x = 10 for constant 10, and return Var x.
     */
    private Var getLocalOrConstant(Value value) {
        if (value instanceof Local) {
            return getVar((Local) value);
        } else if (value instanceof Constant) {
            value.apply(constantConverter);
            Literal rvalue = constantConverter.getResult();
            return constantVars.computeIfAbsent(rvalue, v -> {
                Var lvalue = varManager.newConstantVar(v);
                if (!(rvalue instanceof NullLiteral)) {
                    // add temp assignment for non-null variable
                    addTempStmt(new AssignLiteral(lvalue, v));
                }
                return lvalue;
            });
        }
        throw new SootFrontendException("Expected Local or Constant, given " + value);
    }

    /**
     * Converts Jimple FieldRef to FieldAccess.
     */
    private FieldAccess getFieldAccess(FieldRef fieldRef) {
        pascal.taie.ir.proginfo.FieldRef jfieldRef =
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
     * Converts Jimple ArrayRef to ArrayAccess.
     */
    private ArrayAccess getArrayAccess(ArrayRef arrayRef) {
        return new ArrayAccess(getVar((Local) arrayRef.getBase()),
                getLocalOrConstant(arrayRef.getIndex()));
    }

    /**
     * Converts Jimple NewExpr to NewExp
     */
    private final AbstractJimpleValueSwitch<NewExp> newExprConverter
            = new AbstractJimpleValueSwitch<>() {

        @Override
        public void caseNewExpr(NewExpr v) {
            setResult(new NewInstance((ClassType) getTypeOf(v)));
        }

        @Override
        public void caseNewArrayExpr(NewArrayExpr v) {
            setResult(new NewArray((ArrayType) getTypeOf(v),
                    getLocalOrConstant(v.getSize())));
        }

        @Override
        public void caseNewMultiArrayExpr(NewMultiArrayExpr v) {
            List<Var> lengths = Lists.map(v.getSizes(),
                    MethodIRBuilder.this::getLocalOrConstant);
            setResult(new NewMultiArray((ArrayType) getTypeOf(v), lengths));
        }
    };

    private void buildNew(Local lhs, AnyNewExpr rhs) {
        rhs.apply(newExprConverter);
        NewExp newExp = newExprConverter.getResult();
        New newStmt = new New(method, getVar(lhs), newExp);
        addStmt(newStmt);
    }

    /**
     * Extracts BinaryExp.Op from Jimple BinopExpr.
     */
    private final AbstractJimpleValueSwitch<BinaryExp.Op> binaryOpExtractor
            = new AbstractJimpleValueSwitch<>() {

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
        BinaryExp.Op op = binaryOpExtractor.getResult();
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
                (ReferenceType) converter.convertType(rhs.getCheckType()));
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
        List<Integer> caseValues = Lists.map(stmt.getLookupValues(),
                v -> v.value);
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
        Var result = (lhs == null // remove unused temp variables that receive invoke result
                || unusedInvokeTempRets.contains(lhs))
                ? null : getVar(lhs);
        InvokeExp invokeExp = getInvokeExp(invokeExpr);
        Invoke invoke = new Invoke(method, invokeExp, result);
        addStmt(invoke);
    }

    /**
     * Converts Jimple InvokeExpr to InvokeExp.
     */
    private InvokeExp getInvokeExp(InvokeExpr invokeExpr) {
        if (invokeExpr instanceof DynamicInvokeExpr) {
            return getInvokeDynamic((DynamicInvokeExpr) invokeExpr);
        } else {
            MethodRef methodRef = converter
                    .convertMethodRef(invokeExpr.getMethodRef());
            List<Var> args = Lists.map(invokeExpr.getArgs(),
                    this::getLocalOrConstant);
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
            }
            return new InvokeStatic(methodRef, args);
        }
    }

    private InvokeDynamic getInvokeDynamic(DynamicInvokeExpr invokeExpr) {
        MethodRef bootstrapMethodRef = converter.convertMethodRef(
                invokeExpr.getBootstrapMethodRef());
        SootMethodRef sigInfo = invokeExpr.getMethodRef();
        String methodName = sigInfo.getName();
        List<Type> paramTypes = Lists.map(sigInfo.getParameterTypes(),
                converter::convertType);
        Type returnType = converter.convertType(sigInfo.getReturnType());
        MethodType methodType = MethodType.get(paramTypes, returnType);
        List<Literal> bootstrapArgs = Lists.map(invokeExpr.getBootstrapArgs(), v -> {
            v.apply(constantConverter);
            return constantConverter.getResult();
        });
        List<Var> args = Lists.map(invokeExpr.getArgs(),
                this::getLocalOrConstant);
        return new InvokeDynamic(bootstrapMethodRef, methodName, methodType,
                bootstrapArgs, args);
    }

    @Override
    public void caseReturnStmt(ReturnStmt stmt) {
        currentUnit = stmt;
        Var returnVar = getLocalOrConstant(stmt.getOp());
        returnVars.add(returnVar);
        addStmt(new Return(returnVar));
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
        logger.error("Unhandled Stmt: " + obj);
    }
}

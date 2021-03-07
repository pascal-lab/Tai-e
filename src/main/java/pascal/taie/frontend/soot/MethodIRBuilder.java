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
import pascal.taie.ir.NewIR;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.ArrayLengthExp;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.BitwiseExp;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.ComparisonExp;
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
import pascal.taie.ir.stmt.InstanceOf;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.ir.stmt.Unary;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.classes.MethodRef;
import pascal.taie.java.types.ArrayType;
import pascal.taie.java.types.ClassType;
import pascal.taie.java.types.Type;
import soot.Body;
import soot.Local;
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
import soot.jimple.Constant;
import soot.jimple.DivExpr;
import soot.jimple.DoubleConstant;
import soot.jimple.FieldRef;
import soot.jimple.FloatConstant;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InstanceOfExpr;
import soot.jimple.IntConstant;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.LengthExpr;
import soot.jimple.LongConstant;
import soot.jimple.MulExpr;
import soot.jimple.NegExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
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
import soot.jimple.ThrowStmt;
import soot.jimple.UnopExpr;
import soot.jimple.UshrExpr;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.XorExpr;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static pascal.taie.util.CollectionUtils.freeze;

class MethodIRBuilder {

    private final JMethod method;

    private final Converter converter;

    private VarManager varManager;

    private List<Stmt> stmts;

    MethodIRBuilder(JMethod method, Converter converter) {
        this.method = method;
        this.converter = converter;
    }

    NewIR build() {
        Body body = method.getSootMethod().retrieveActiveBody();
        varManager = new VarManager();
        stmts = new ArrayList<>();
        if (!method.isStatic()) {
            buildThis(body);
        }
        buildParams(body);
        buildStmts(body);
        return new DefaultNewIR(method,
                varManager.getThis(), freeze(varManager.getParams()),
                freeze(varManager.getVars()), freeze(stmts));
    }

    private void buildThis(Body body) {
        varManager.addThis(body.getThisLocal());
    }

    private void buildParams(Body body) {
        body.getParameterLocals().forEach(varManager::addParam);
    }

    private void buildStmts(Body body) {
        StmtBuilder builder = new StmtBuilder();
        body.getUnits().forEach(unit -> unit.apply(builder));
    }

    /**
     * Shortcut: convert Jimple Local to Var.
     */
    private Var getVar(Local local) {
        return varManager.getVar(local);
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

    private class StmtBuilder extends AbstractStmtSwitch {

        /**
         * Current Jimple statement being converted.
         */
        private soot.jimple.Stmt currentStmt;

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
                        .map(StmtBuilder.this::getLocalOrConstant)
                        .collect(Collectors.toList());
                setResult(new NewMultiArray((ArrayType) getType(v), lengths));
            }
        };

        private void buildNew(Local lhs, AnyNewExpr newExpr) {
            newExpr.apply(newExprConverter);
            addStmt(new New(getVar(lhs), (NewExp) newExprConverter.getResult()));
            // TODO: set allocation site
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

        private void buildAssignLiteral(Local lhs, Constant constant) {
            constant.apply(constantConverter);
            addStmt(new AssignLiteral(getVar(lhs),
                    (Literal) constantConverter.getResult()));
        }

        private void buildCopy(Local lhs, Local rhs) {
            addStmt(new Copy(getVar(lhs), getVar(rhs)));
        }

        private void buildLoadArray(Local lhs, ArrayRef arrayRef) {
            addStmt(new LoadArray(getVar(lhs), getArrayAccess(arrayRef)));
        }

        private void buildStoreArray(ArrayRef lhs, Value rhs) {
            addStmt(new StoreArray(
                    getArrayAccess(lhs), getLocalOrConstant(rhs)));
        }

        /**
         * Convert Jimple ArrayRef to ArrayAccess.
         */
        private ArrayAccess getArrayAccess(ArrayRef arrayRef) {
            return new ArrayAccess(getVar((Local) arrayRef.getBase()),
                    getLocalOrConstant(arrayRef.getIndex()));
        }

        private void buildLoadField(Local lhs, FieldRef fieldRef) {
            addStmt(new LoadField(getVar(lhs), getFieldAccess(fieldRef)));
        }

        private void buildStoreField(FieldRef lhs, Value rhs) {
            addStmt(new StoreField(
                    getFieldAccess(lhs), getLocalOrConstant(rhs)));
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

        private void buildInvoke(Local lhs, InvokeExpr invokeExpr) {
            Var result = lhs == null ? null : getVar(lhs);
            addStmt(new Invoke(getInvokeExp(invokeExpr), result));
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

        private void buildReturn(Value value) {
            Var var = value == null ? null : getLocalOrConstant(value);
            addStmt(new Return(var));
        }
        
        private void buildThrow(Value exception) {
            addStmt(new Throw(getLocalOrConstant(exception)));
        }
        
        private void buildCatch(Local exception) {
            addStmt(new Catch(getVar(exception)));
        }

        /**
         * Convert a Jimple Local or Constant to Var.
         * If {@param value} is Local, then directly return the corresponding Var.
         * If {@param value} is Constant, then add a temporary assignment,
         * e.g., x = 10 for constant 10, and return Var x.
         */
        private Var getLocalOrConstant(Value value) {
            if (value instanceof Local) {
                return getVar((Local) value);
            } else if (value instanceof Constant) {
                value.apply(constantConverter);
                Literal rvalue = (Literal) constantConverter.getResult();
                Var lvalue = varManager.newConstantVar(rvalue);
                addStmt(new AssignLiteral(lvalue, rvalue));
                return lvalue;
            }
            throw new SootFrontendException("Expected Local or Constant, given " + value);
        }

        private void addStmt(Stmt stmt) {
            // TODO: add more information to Stmt
            stmts.add(stmt);
        }

        @Override
        public void caseAssignStmt(AssignStmt stmt) {
            currentStmt = stmt;
            Value lhs = stmt.getLeftOp();
            Value rhs = stmt.getRightOp();
            if (rhs instanceof InvokeExpr) {
                buildInvoke((Local) lhs, (InvokeExpr) rhs);
                return;
            }
            if (lhs instanceof Local) {
                Local lvar = (Local) lhs;
                if (rhs instanceof Local) {
                    buildCopy(lvar, (Local) rhs);
                } else if (rhs instanceof AnyNewExpr) {
                    buildNew(lvar, (AnyNewExpr) rhs);
                } else if (rhs instanceof Constant) {
                    buildAssignLiteral(lvar, (Constant) rhs);
                } else if (rhs instanceof FieldRef) {
                    buildLoadField(lvar, (FieldRef) rhs);
                } else if (rhs instanceof ArrayRef) {
                    buildLoadArray(lvar, (ArrayRef) rhs);
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
                buildStoreField((FieldRef) lhs, rhs);
            } else if (lhs instanceof ArrayRef) {
                buildStoreArray((ArrayRef) lhs, rhs);
            } else {
                throw new SootFrontendException(
                        "Cannot handle AssignStmt: " + stmt);
            }
        }

        @Override
        public void caseInvokeStmt(InvokeStmt stmt) {
            currentStmt = stmt;
            buildInvoke(null, stmt.getInvokeExpr());
        }

        @Override
        public void caseReturnStmt(ReturnStmt stmt) {
            currentStmt = stmt;
            buildReturn(stmt.getOp());
        }

        @Override
        public void caseReturnVoidStmt(ReturnVoidStmt stmt) {
            currentStmt = stmt;
            buildReturn(null);
        }

        @Override
        public void caseThrowStmt(ThrowStmt stmt) {
            currentStmt = stmt;
            buildThrow(stmt.getOp());
        }

        @Override
        public void caseIdentityStmt(IdentityStmt stmt) {
            currentStmt = stmt;
            Value lhs = stmt.getLeftOp();
            Value rhs = stmt.getRightOp();
            if (rhs instanceof CaughtExceptionRef) {
                buildCatch((Local) lhs);
            }
        }

        @Override
        public void defaultCase(Object obj) {
            System.out.println("Unhandled Stmt: " + obj);
        }
    }

    private class VarManager {

        private final static String THIS = "#this";

        private final static String PARAM = "#param";

        private final static String STRING_CONSTANT = "#stringconstant";

        private final static String CLASS_CONSTANT = "#classconstant";

        private final static String NULL_CONSTANT = "#nullconstant";

        private final Map<Local, Var> varMap = new LinkedHashMap<>();

        private final List<Var> vars = new ArrayList<>();

        private Var thisVar;

        private final List<Var> params = new ArrayList<>();

        /**
         * Counter for temporary constant variables.
         */
        private int counter = 0;

        private void addThis(Local thisLocal) {
            thisVar = newVar(THIS, getType(thisLocal));
            varMap.put(thisLocal, thisVar);
        }

        private void addNativeThis(Type thisType) {
            thisVar = newVar(THIS, thisType);
            // TODO: add to varMap?
        }

        private void addParam(Local paramLocal) {
            params.add(getVar(paramLocal));
        }

        private void addNativeParam(Type paramType) {
            Var param = newVar(PARAM + params.size(), paramType);
            params.add(param);
            // TODO: add to varMap?
        }

        private Var getVar(Local local) {
            return varMap.computeIfAbsent(local, l ->
                    newVar(l.getName(), getType(l)));
        }

        /**
         * @return a new temporary variable that holds given literal value.
         */
        private Var newConstantVar(Literal literal) {
            String varName;
            if (literal instanceof StringLiteral) {
                varName = STRING_CONSTANT + counter++;
            } else if (literal instanceof ClassLiteral) {
                varName = CLASS_CONSTANT + counter++;
            } else if (literal instanceof NullLiteral) {
                varName = NULL_CONSTANT + counter++;
            } else {
                varName = "#" + literal.getType().getName() +
                        "constant" + counter++;
            }
            return newVar(varName, literal.getType());
        }

        private Var newVar(String name, Type type) {
            Var var = new Var(name, type);
            vars.add(var);
            return var;
        }

        private Var getThis() {
            return thisVar;
        }

        public List<Var> getParams() {
            return params;
        }

        private List<Var> getVars() {
            return vars;
        }
    }
}

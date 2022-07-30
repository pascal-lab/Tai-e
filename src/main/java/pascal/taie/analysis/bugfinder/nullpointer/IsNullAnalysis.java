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

package pascal.taie.analysis.bugfinder.nullpointer;

import pascal.taie.analysis.dataflow.analysis.AbstractDataflowAnalysis;
import pascal.taie.analysis.dataflow.analysis.AnalysisDriver;
import pascal.taie.analysis.dataflow.analysis.DataflowAnalysis;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.Edge;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ArrayLengthExp;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.ReferenceLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.Monitor;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StmtVisitor;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.ir.stmt.Unary;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.ReferenceType;

public class IsNullAnalysis extends AnalysisDriver<Stmt, IsNullFact> {

    public static final String ID = "isnull";

    public IsNullAnalysis(AnalysisConfig config) {
        super(config);
    }

    @Override
    protected DataflowAnalysis<Stmt, IsNullFact> makeAnalysis(CFG<Stmt> cfg) {
        return new Analysis(cfg);
    }

    private static class Analysis extends AbstractDataflowAnalysis<Stmt, IsNullFact> {

        public Analysis(CFG<Stmt> cfg) {
            super(cfg);
        }

        @Override
        public boolean isForward() {
            return true;
        }

        @Override
        public IsNullFact newBoundaryFact() {
            return newBoundaryFact(cfg.getIR());
        }

        public IsNullFact newBoundaryFact(IR ir) {
            IsNullFact entryFact = newInitialFact();
            ir.getParams()
                    .stream()
                    .filter(var -> var.getType() instanceof ReferenceType)
                    .forEach(p -> entryFact.update(p, IsNullValue.nonReportingNotNullValue()));

            if (ir.getThis() != null) {
                entryFact.update(ir.getThis(), IsNullValue.nonNullValue());
            }

            // use annotation info
            JMethod method = ir.getMethod();
            for (int paramIndex = 0; paramIndex < method.getParamCount(); ++paramIndex) {
                if (!(method.getParamType(paramIndex) instanceof ReferenceType)) {
                    continue;
                }

                IsNullValue value = null;
                NullnessAnnotation nullnessAnnotation =
                        NullnessAnnotation.resolveParameterAnnotation(method, paramIndex);
                if (nullnessAnnotation == NullnessAnnotation.CHECK_FOR_NULL) {
                    value = IsNullValue.nullOnSimplePathValue();
                } else if (nullnessAnnotation == NullnessAnnotation.NONNULL) {
                    value = IsNullValue.nonNullValue();
                } else {
                    value = IsNullValue.nonReportingNotNullValue();
                }

                entryFact.update(ir.getParam(paramIndex), value);
            }

            return entryFact;
        }

        @Override
        public IsNullFact newInitialFact() {
            return new IsNullFact();
        }

        @Override
        public void meetInto(IsNullFact fact, IsNullFact target) {
            if (!fact.isValid()) {
                return;
            }
            if (!target.isValid()) {
                target.copyFrom(fact);
                target.setValid();
                return;
            }
            fact.forEach((var, value) ->
                    target.update(var, IsNullValue.merge(value, target.get(var))));
        }

        @Override
        public boolean transferNode(Stmt stmt, IsNullFact in, IsNullFact out) {
            if (!in.isValid()) {
                boolean changed = out.isValid();
                out.setInvalid();
                return changed;
            }
            return stmt.accept(new StmtVisitor<>() {
                @Override
                public Boolean visit(New stmt) {
                    return updateLValueIfReferenceType(stmt, IsNullValue.nonNullValue());
                }

                @Override
                public Boolean visit(AssignLiteral stmt) {
                    Var lValue = stmt.getLValue();
                    if (lValue.getType() instanceof ReferenceType
                            && stmt.getRValue() instanceof ReferenceLiteral) {
                        IsNullFact oldOut = out.copy();
                        out.copyFrom(in);
                        if (stmt.getRValue() instanceof NullLiteral) {
                            out.update(lValue, IsNullValue.nullValue());
                        } else {
                            out.update(lValue, IsNullValue.nonNullValue());
                        }
                        return !out.equals(oldOut);
                    }
                    return out.copyFrom(in);
                }

                @Override
                public Boolean visit(Copy stmt) {
                    Var lValue = stmt.getLValue();
                    if (lValue.getType() instanceof ReferenceType) {
                        IsNullFact oldOut = out.copy();
                        out.copyFrom(in);

                        IsNullValue rNullValue = in.get(stmt.getRValue());
                        if (rNullValue != null) {
                            out.update(lValue, rNullValue);
                        }
                        return !out.equals(oldOut);
                    }
                    return out.copyFrom(in);
                }

                @Override
                public Boolean visit(LoadArray stmt) {
                    return updateLValueIfReferenceType(stmt, IsNullValue.nullOnComplexPathValue());
                }

                @Override
                public Boolean visit(LoadField stmt) {
                    return updateLValueIfReferenceType(stmt, IsNullValue.nullOnComplexPathValue());
                }

                @Override
                public Boolean visit(Invoke stmt) {
                    JMethod invokeMethod = stmt.getInvokeExp().getMethodRef().resolveNullable();
                    // todo: develop and use Unconditional dereference analysis
                    if (invokeMethod == null) {
                        return visitDefault(stmt);
                    }

                    IsNullFact oldOut = out.copy();
                    out.copyFrom(in);
                    if (isAssertionCall(invokeMethod)) {// downgrade null value after an assertion call
                        out.entries()
                                .filter(entry -> entry.getValue().isNullOnSomePath()
                                        || entry.getValue().isDefinitelyNull())
                                .forEach(entry -> entry.setValue(IsNullValue.nullOnComplexPathValue()));
                        return !out.equals(oldOut);
                    } else { // use parameter annotation info
                        for (int paramIndex = 0; paramIndex < invokeMethod.getParamCount(); ++paramIndex) {
                            NullnessAnnotation nullnessAnnotation =
                                    NullnessAnnotation.resolveParameterAnnotation(invokeMethod, paramIndex);
                            if (nullnessAnnotation == NullnessAnnotation.NONNULL) {
                                // todo: if arg is definitely null, should take special care for this case?
                                out.update(stmt.getInvokeExp().getArg(paramIndex), IsNullValue.nonNullValue());
                            }
                        }
                    }

                    if (stmt.getLValue() == null) {
                        return !out.equals(oldOut);
                    }

                    NullnessAnnotation returnAnnotation = NullnessAnnotation.resolveReturnValueAnnotation(invokeMethod);
                    IsNullValue value = IsNullValue.nonReportingNotNullValue();
                    if (returnAnnotation == NullnessAnnotation.CHECK_FOR_NULL) {
                        value = IsNullValue.nullOnSimplePathValue();
                    } else if (returnAnnotation == NullnessAnnotation.NONNULL) {
                        value = IsNullValue.nonNullValue();
                    }

                    return updateLValueIfReferenceType(stmt, value);
                }

                private Boolean updateLValueIfReferenceType(DefinitionStmt<Var, ?> stmt, IsNullValue newValue) {
                    Var lValue = stmt.getLValue();
                    if (lValue.getType() instanceof ReferenceType) {
                        IsNullFact oldOut = out.copy();
                        out.copyFrom(in);

                        out.update(lValue, newValue);
                        return !out.equals(oldOut);
                    }
                    return out.copyFrom(in);
                }

                @Override
                public Boolean visit(If stmt) {
                    // only update IsNullConditionDecision
                    ConditionExp.Op op = stmt.getCondition().getOperator();
                    if (op == ConditionExp.Op.EQ || op == ConditionExp.Op.NE) {
                        int refCount = 0;
                        Var var1 = stmt.getCondition().getOperand1();
                        Var var2 = stmt.getCondition().getOperand2();
                        Var reference = null;

                        if (var1.getType() instanceof ClassType || var1.getType() instanceof ArrayType) {
                            refCount++;
                            reference = var1;
                        }
                        if (var2.getType() instanceof ClassType || var2.getType() instanceof ArrayType) {
                            refCount++;
                            reference = var2;
                        }

                        if (refCount == 1) { // A reference object compare with null
                            IsNullValue referenceVal = in.get(reference);
                            boolean ifnull = op == ConditionExp.Op.EQ;
                            out.setDecision(handleIfNull(stmt, reference, referenceVal, ifnull));

                        } else if (refCount == 2) { // A reference object compare with another one
                            IsNullValue ifTrueDecision = null;
                            IsNullValue ifFalseDecision = null;
                            Var testedVar = var1;
                            boolean ifEq = op == ConditionExp.Op.EQ;

                            IsNullValue nullVal1 = in.get(var1);
                            IsNullValue nullVal2 = in.get(var2);

                            if (nullVal1.isDefinitelyNull() && nullVal2.isDefinitelyNull()) {
                                if (ifEq) {
                                    ifTrueDecision = IsNullValue.checkedNullValue();
                                } else {
                                    ifFalseDecision = IsNullValue.checkedNullValue();
                                }
                                out.setDecision(new IsNullConditionDecision(stmt, testedVar, ifTrueDecision, ifFalseDecision));
                            } else if (nullVal1.isDefinitelyNull()) {
                                out.setDecision(handleIfNull(stmt, var2, nullVal2, ifEq));
                            } else if (nullVal2.isDefinitelyNull()) {
                                out.setDecision(handleIfNull(stmt, var1, nullVal1, ifEq));

                            } else if (nullVal1.isDefinitelyNotNull() && !nullVal2.isDefinitelyNotNull()) {
                                // learn var2 is definitely non-null on one branch
                                testedVar = var2;
                                if (ifEq) {
                                    ifTrueDecision = nullVal1; // var1 == var2 -> var1 and var2 have same IsNullValues
                                    ifFalseDecision = nullVal2; // var1 != var2 -> var2 keeps its IsNullValues, will not change on this edge.
                                } else {
                                    ifTrueDecision = nullVal2;
                                    ifFalseDecision = nullVal1;
                                }
                                out.setDecision(new IsNullConditionDecision(stmt, testedVar, ifTrueDecision, ifFalseDecision));
                            } else if (!nullVal1.isDefinitelyNotNull() && nullVal2.isDefinitelyNotNull()) {
                                // learn var1 is definitely non-null on one branch
                                testedVar = var1;
                                if (ifEq) {
                                    ifTrueDecision = nullVal2;
                                    ifFalseDecision = nullVal1;
                                } else {
                                    ifTrueDecision = nullVal1;
                                    ifFalseDecision = nullVal2;
                                }
                                out.setDecision(new IsNullConditionDecision(stmt, testedVar, ifTrueDecision, ifFalseDecision));
                            }
                        }
                    }
                    return out.copyFrom(in);
                }

                private IsNullConditionDecision handleIfNull(If stmt, Var referenceVar, IsNullValue referenceVal, boolean ifnull) {
                    IsNullValue ifTrueDecision = null;
                    IsNullValue ifFalseDecision = null;

                    if (referenceVal.isDefinitelyNull()) {
                        if (ifnull) {
                            ifTrueDecision = IsNullValue.checkedNullValue();
                        } else {
                            ifFalseDecision = IsNullValue.checkedNullValue();
                        }
                    } else if (referenceVal.isDefinitelyNotNull()) {
                        if (ifnull) {
                            ifFalseDecision = referenceVal.isAKaBoom() ? referenceVal : IsNullValue.checkedNonNullValue();
                        } else {
                            ifTrueDecision = referenceVal.isAKaBoom() ? referenceVal : IsNullValue.checkedNonNullValue();
                        }
                    } else { // both branches feasible
                        if (ifnull) {
                            ifTrueDecision = IsNullValue.checkedNullValue();
                            ifFalseDecision = IsNullValue.checkedNonNullValue();
                        } else {
                            ifTrueDecision = IsNullValue.checkedNonNullValue();
                            ifFalseDecision = IsNullValue.checkedNullValue();
                        }
                    }
                    return new IsNullConditionDecision(stmt, referenceVar, ifTrueDecision, ifFalseDecision);
                }

                @Override
                public Boolean visitDefault(Stmt stmt) {
                    return out.copyFrom(in);
                }
            });
        }

        @Override
        public boolean needTransferEdge(Edge<Stmt> edge) {
            return true;
        }

        @Override
        public IsNullFact transferEdge(Edge<Stmt> edge, IsNullFact nodeFact) {
            if (!nodeFact.isValid()) {
                return nodeFact;
            }

            Stmt source = edge.getSource();
            IsNullFact resultFact = nodeFact;

            int nonExceptionSucessorNums = 0;
            for (Edge<Stmt> e : cfg.getOutEdgesOf(source)) {
                nonExceptionSucessorNums += e.isExceptional() ? 0 : 1;
            }
            // 1. downgrade on non-exception control splits
            if (!edge.isExceptional() && nonExceptionSucessorNums > 1) {
                resultFact.downgradeOnControlSplit();
            }
            // 2. downgrade NULL&NSP to do_not_report value for two special exceptions
            // todo: should our null value add an exception property?
            if (edge.getKind() == Edge.Kind.CAUGHT_EXCEPTION) {
                resultFact = nodeFact.copy();
                for (ClassType classType : edge.getExceptions()) {
                    if (classType.getName().equals(ClassNames.CLONE_NOT_SUPPORTED_EXCEPTION)
                            || classType.getName().equals(ClassNames.INTERRUPTED_EXCEPTION)) {
                        resultFact.entries()
                                .filter(entry -> entry.getValue().isDefinitelyNull() || entry.getValue().isNullOnSomePath())
                                .forEach(entry -> entry.setValue(IsNullValue.nullOnComplexPathValue()));
                    }
                }
            } else if (edge.getKind() == Edge.Kind.IF_TRUE || edge.getKind() == Edge.Kind.IF_FALSE) {
                // 3. use null comparison information
                // todo: handle instanceof operand?
                IsNullConditionDecision decision = nodeFact.getDecision();
                if (decision != null) {
                    if (!decision.isEdgeFeasible(edge.getKind())) {
                        // set this target basic block invalid, their facts should not affect analysis process
                        resultFact = nodeFact.copy();
                        resultFact.setInvalid();
                    } else {
                        Var varTested = decision.getVarTested();
                        if (varTested != null) {
                            IsNullValue decisionValue = decision.getDecision(edge.getKind());
                            assert decisionValue != null;

                            resultFact = nodeFact.copy();
                            // todo: use pta to update more variable
                            resultFact.update(varTested, decisionValue);
                            if (decisionValue.isDefinitelyNull()) {

                            }
                        }
                    }
                }
            } else if (edge.getKind() == Edge.Kind.FALL_THROUGH) {
                // 4. handle those statements which may raise NullPointerException
                Stmt target = edge.getTarget();
                Var derefVar = target.accept(new NPEVarVisitor());

                if (derefVar != null) {
                    IsNullValue derefVal = nodeFact.get(derefVar);

                    if (derefVal.isDefinitelyNull()) {
                        // then this edge is infeasible
                        resultFact = nodeFact.copy();
                        resultFact.setInvalid();
                    } else if (!derefVal.isDefinitelyNotNull()) {
                        // update the null value for the dereferenced value.
                        resultFact = nodeFact.copy();
                        // todo: use pta to update more Var
                        resultFact.update(derefVar, IsNullValue.noKaboomNonNullValue());
                    }
                }
            }
            return resultFact;
        }

        private boolean isAssertionCall(JMethod m) {
            String className = m.getDeclaringClass().getName();
            String methodName = m.getName();
            String returnType = m.getReturnType().getName();

            String classNameLC = className.toLowerCase();
            String methodNameLC = methodName.toLowerCase();

            return className.endsWith("Assert") && methodName.startsWith("is")
                    || (returnType.equals("void") || returnType.equals("boolean"))
                    && (classNameLC.contains("assert") || methodNameLC.startsWith("throw")
                    || methodName.startsWith("affirm") || methodName.startsWith("panic")
                    || "logTerminal".equals(methodName) || methodName.startsWith("logAndThrow")
                    || "insist".equals(methodNameLC) || "usage".equals(methodNameLC)
                    || "exit".equals(methodNameLC) || methodNameLC.startsWith("fail")
                    || methodNameLC.startsWith("fatal") || methodNameLC.contains("assert")
                    || methodNameLC.contains("legal") || methodNameLC.contains("error")
                    || methodNameLC.contains("abort")
                    // || methodNameLC.indexOf("check") >= 0
                    || methodNameLC.contains("failed"))
                    || "addOrThrowException".equals(methodName);
        }


        private enum NullnessAnnotation {
            CHECK_FOR_NULL,
            NONNULL,
            NULLABLE,
            NN_UNKNOWN;

            public static NullnessAnnotation resolveParameterAnnotation(JMethod method, int index) {
                // todo: make this resolve process as a independent analysis?
                if (index == 0) {
                    String subsignature = method.getSubsignature().toString();
                    if (subsignature.equals("boolean equals(java.lang.Object)")) {
                        return NullnessAnnotation.CHECK_FOR_NULL;
                    } else if (subsignature.equals("void main(java.lang.String[])")
                            && method.isStatic()) {
                        return NullnessAnnotation.NONNULL;
                    } else if (method.getName().equals("compareTo")
                            && method.getReturnType().getName().equals("boolean")) {
                        return NullnessAnnotation.NONNULL;
                    }
                }

                for (Annotation anno : method.getParamAnnotations(index)) {
                    NullnessAnnotation nullnessAnnotation = parse(anno);
                    if (nullnessAnnotation != NN_UNKNOWN) {
                        return nullnessAnnotation;
                    }
                }
                return NN_UNKNOWN;
            }

            public static NullnessAnnotation resolveReturnValueAnnotation(JMethod method) {
                for (Annotation anno : method.getAnnotations()) {
                    NullnessAnnotation nullnessAnnotation = parse(anno);
                    if (nullnessAnnotation != NN_UNKNOWN) {
                        return nullnessAnnotation;
                    }
                }
                return NN_UNKNOWN;
            }

            private static NullnessAnnotation parse(Annotation a) {
                String className = a.getType();
                if ("android.support.annotation.Nullable".equals(className)
                        || "androidx.annotation.Nullable".equals(className)
                        || "com.google.common.base.Nullable".equals(className)
                        || "org.eclipse.jdt.annotation.Nullable".equals(className)
                        || "org.jetbrains.annotations.Nullable".equals(className)
                        || "org.checkerframework.checker.nullness.qual.Nullable".equals(className)
                        || "org.checkerframework.checker.nullness.compatqual.NullableDecl".equals(className)
                        || className.endsWith("PossiblyNull")
                        || className.endsWith("CheckForNull")) {
                    return CHECK_FOR_NULL;
                } else if ("org.jetbrains.annotations.NotNull".equals(className)
                        || className.endsWith("Nonnull")
                        || className.endsWith("NonNull")) {
                    return NONNULL;
                } else if (className.endsWith("Nullable")) {
                    return NULLABLE;
                } else {
                    return NN_UNKNOWN;
                }
            }
        }

    }

    public static class NPEVarVisitor implements StmtVisitor<Var> {
        @Override
        public Var visitDefault(Stmt stmt) {
            return null;
        }

        @Override
        public Var visit(LoadField stmt) {
            return stmt.isStatic() ?
                    null : ((InstanceFieldAccess) stmt.getFieldAccess()).getBase();
        }

        @Override
        public Var visit(StoreField stmt) {
            return stmt.isStatic() ?
                    null : ((InstanceFieldAccess) stmt.getFieldAccess()).getBase();
        }

        @Override
        public Var visit(Unary stmt) {
            return stmt.getRValue() instanceof ArrayLengthExp ?
                    ((ArrayLengthExp) stmt.getRValue()).getBase() : null;
        }

        @Override
        public Var visit(Invoke stmt) {
            return stmt.isStatic() ?
                    null : ((InvokeInstanceExp) stmt.getInvokeExp()).getBase();
        }

        @Override
        public Var visit(Throw stmt) {
            return stmt.getExceptionRef();
        }

        @Override
        public Var visit(Monitor stmt) {
            return StmtVisitor.super.visit(stmt);
        }

        @Override
        public Var visit(LoadArray stmt) {
            return stmt.getArrayAccess().getBase();
        }

        @Override
        public Var visit(StoreArray stmt) {
            return stmt.getArrayAccess().getBase();
        }
    }
}

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
import pascal.taie.analysis.graph.cfg.CFGEdge;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.ReferenceLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StmtVisitor;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.ReferenceType;

import java.util.List;

public class IsNullAnalysis extends AnalysisDriver<Stmt, IsNullFact> {

    public static final String ID = "is-null";

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
                    .forEach(p -> entryFact.update(p, IsNullValue.UNKNOWN));

            if (ir.getThis() != null) {
                entryFact.update(ir.getThis(), IsNullValue.NONNULL);
            }

            // use annotation info
            JMethod method = ir.getMethod();
            for (int paramIndex = 0; paramIndex < method.getParamCount(); ++paramIndex) {
                if (!(method.getParamType(paramIndex) instanceof ReferenceType)) {
                    continue;
                }

                IsNullValue value;
                NullnessAnnotation nullnessAnnotation =
                        NullnessAnnotation.resolveParameterAnnotation(method, paramIndex);
                if (nullnessAnnotation == NullnessAnnotation.CHECK_FOR_NULL) {
                    value = IsNullValue.NSP;
                } else if (nullnessAnnotation == NullnessAnnotation.NONNULL) {
                    value = IsNullValue.NONNULL;
                } else {
                    value = IsNullValue.UNKNOWN;
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

        private static final List<String> keywordsAssertionMethodsContain =
                List.of("assert", "legal", "error", "abort", "failed");

        @Override
        public boolean transferNode(Stmt stmt, IsNullFact in, IsNullFact out) {
            if (!in.isValid()) {
                boolean changed = out.isValid();
                out.setInvalid();
                return changed;
            }
            return stmt.accept(new IsNullVisitor(out, in));
        }

        @Override
        public boolean needTransferEdge(CFGEdge<Stmt> edge) {
            return true;
        }

        @Override
        public IsNullFact transferEdge(CFGEdge<Stmt> edge, IsNullFact nodeFact) {
            if (!nodeFact.isValid()) {
                return nodeFact;
            }

            Stmt source = edge.source();
            IsNullFact resultFact = nodeFact;

            int nonExceptionSucessorNums = 0;
            for (CFGEdge<Stmt> e : cfg.getOutEdgesOf(source)) {
                nonExceptionSucessorNums += e.isExceptional() ? 0 : 1;
            }
            // 1. downgrade on non-exception control splits
            if (!edge.isExceptional() && nonExceptionSucessorNums > 1) {
                resultFact.downgradeOnControlSplit();
            }
            // 2. downgrade NULL&NSP to do_not_report value for two special exceptions
            // TODO: should our null value add an exception property?
            if (edge.getKind() == CFGEdge.Kind.CAUGHT_EXCEPTION) {
                resultFact = nodeFact.copy();
                for (ClassType classType : edge.getExceptions()) {
                    if (classType.getName().equals(ClassNames.CLONE_NOT_SUPPORTED_EXCEPTION)
                            || classType.getName().equals(ClassNames.INTERRUPTED_EXCEPTION)) {
                        resultFact.entries()
                                .filter(entry -> entry.getValue().isDefinitelyNull()
                                        || entry.getValue().isNullOnSomePath())
                                .forEach(entry ->
                                        entry.setValue(IsNullValue.NCP));
                    }
                }
            } else if (edge.getKind() == CFGEdge.Kind.IF_TRUE || edge.getKind() == CFGEdge.Kind.IF_FALSE) {
                // 3. use null comparison information
                // TODO: handle instanceof operand?
                IsNullConditionDecision decision = nodeFact.getDecision();
                if (decision != null) {
                    if (!decision.isEdgeFeasible(edge.getKind())) {
                        // set this target basic block invalid,
                        // their facts should not affect analysis process
                        resultFact = nodeFact.copy();
                        resultFact.setInvalid();
                    } else {
                        Var varTested = decision.getVarTested();
                        if (varTested != null) {
                            IsNullValue decisionValue = decision.getDecision(edge.getKind());
                            assert decisionValue != null;

                            resultFact = nodeFact.copy();
                            // TODO: use pta to update more variable
                            resultFact.update(varTested, decisionValue);
                        }
                    }
                }
            } else if (edge.getKind() == CFGEdge.Kind.FALL_THROUGH) {
                // 4. handle those statements which may raise NullPointerException
                Stmt target = edge.target();
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
                        // TODO: use pta to update more Var
                        resultFact.update(derefVar, IsNullValue.NO_KABOOM_NN);
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
                    || methodNameLC.startsWith("fatal") || keywordsAssertionMethodsContain
                    .stream().anyMatch(methodNameLC::contains))
                    || "addOrThrowException".equals(methodName);
        }


        private enum NullnessAnnotation {

            CHECK_FOR_NULL,
            NONNULL,
            NULLABLE,
            NN_UNKNOWN;

            private static final String EQUALS = "boolean equals(java.lang.Object)";
            private static final String MAIN = "void main(java.lang.String[])";
            private static final String CLONE = "java.lang.Object clone()";
            private static final String TO_STRING = "java.lang.String toString()";
            private static final String READ_RESOLVE = "java.lang.Object readResolve()";

            private static final List<String> checkForNullClasses =
                    List.of("android.support.annotation.Nullable",
                            "androidx.annotation.Nullable",
                            "com.google.common.base.Nullable",
                            "org.eclipse.jdt.annotation.Nullable",
                            "org.jetbrains.annotations.Nullable",
                            "org.checkerframework.checker.nullness.qual.Nullable",
                            "org.checkerframework.checker.nullness.compatqual.NullableDecl");

            public static NullnessAnnotation resolveParameterAnnotation(JMethod method, int index) {
                // TODO: make this resolve process as a independent analysis?
                if (index == 0) {
                    String subSignature = method.getSubsignature().toString();
                    if (subSignature.equals(EQUALS) && !method.isStatic()) {
                        return NullnessAnnotation.CHECK_FOR_NULL;
                    } else if (subSignature.equals(MAIN)
                            && method.isStatic()
                            && method.isPublic()) {
                        return NullnessAnnotation.NONNULL;
                    } else if (method.getName().equals("compareTo")
                            && method.getReturnType().getName().equals("boolean")
                            && !method.isStatic()) {
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

                String subSignature = method.getSubsignature().toString();
                if (!method.isStatic() &&
                        (subSignature.equals(CLONE)
                                || subSignature.equals(TO_STRING)
                                || (subSignature.equals(READ_RESOLVE) && method.isPrivate()))) {
                    return NONNULL;
                }
                return NN_UNKNOWN;
            }

            private static NullnessAnnotation parse(Annotation a) {
                String className = a.getType();
                if (checkForNullClasses.stream().anyMatch(className::equals)
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

        private class IsNullVisitor implements StmtVisitor<Boolean> {
            private final IsNullFact out;
            private final IsNullFact in;

            public IsNullVisitor(IsNullFact out, IsNullFact in) {
                this.out = out;
                this.in = in;
            }

            @Override
            public Boolean visit(New stmt) {
                return updateLValueIfReferenceType(stmt, IsNullValue.NONNULL);
            }

            @Override
            public Boolean visit(AssignLiteral stmt) {
                if (stmt.getRValue() instanceof NullLiteral) {
                    return updateLValueIfReferenceType(stmt, IsNullValue.NULL);
                } else if (stmt.getRValue() instanceof ReferenceLiteral) {
                    return updateLValueIfReferenceType(stmt, IsNullValue.NONNULL);
                }
                return out.copyFrom(in);
            }

            @Override
            public Boolean visit(Copy stmt) {
                return updateLValueIfReferenceType(stmt, in.get(stmt.getRValue()));
            }

            @Override
            public Boolean visit(Cast stmt) {
                return updateLValueIfReferenceType(stmt, in.get(stmt.getRValue().getValue()));
            }

            @Override
            public Boolean visit(LoadArray stmt) {
                return updateLValueIfReferenceType(stmt, IsNullValue.NCP);
            }

            @Override
            public Boolean visit(LoadField stmt) {
                return updateLValueIfReferenceType(stmt, IsNullValue.NCP);
            }

            @Override
            public Boolean visit(Invoke stmt) {
                if (stmt.isDynamic()) {
                    return false;
                }
                JMethod invokeMethod = stmt.getInvokeExp().getMethodRef().resolveNullable();
                // TODO: develop and use Unconditional dereference analysis
                if (invokeMethod == null) {
                    return visitDefault(stmt);
                }

                IsNullFact oldOut = out.copy();
                out.copyFrom(in);
                if (isAssertionCall(invokeMethod)) { // downgrade null value after an assertion call
                    out.entries()
                            .filter(entry -> entry.getValue().isNullOnSomePath()
                                    || entry.getValue().isDefinitelyNull())
                            .forEach(entry -> entry.setValue(IsNullValue.NCP));
                    return !out.equals(oldOut);
                } else { // use parameter annotation info
                    for (int paramIndex = 0; paramIndex < invokeMethod.getParamCount(); ++paramIndex) {
                        NullnessAnnotation nullnessAnnotation =
                                NullnessAnnotation.resolveParameterAnnotation(invokeMethod, paramIndex);
                        if (nullnessAnnotation == NullnessAnnotation.NONNULL) {
                            // TODO: if arg is definitely null, should take special care for this case?
                            out.update(stmt.getInvokeExp().getArg(paramIndex), IsNullValue.NONNULL);
                        }
                    }
                }

                if (stmt.getLValue() == null) {
                    return !out.equals(oldOut);
                }

                NullnessAnnotation returnAnnotation = NullnessAnnotation.resolveReturnValueAnnotation(invokeMethod);
                IsNullValue value = IsNullValue.UNKNOWN;
                if (returnAnnotation == NullnessAnnotation.CHECK_FOR_NULL) {
                    value = IsNullValue.NSP;
                } else if (returnAnnotation == NullnessAnnotation.NONNULL) {
                    value = IsNullValue.NONNULL;
                }

                return updateLValueIfReferenceType(stmt, value);
            }

            private Boolean updateLValueIfReferenceType(DefinitionStmt<Var, ?> stmt, IsNullValue newValue) {
                Var lValue = stmt.getLValue();
                assert lValue != null;
                if (lValue.getType() instanceof ReferenceType) {
                    boolean changed = false;
                    for (Var inVar : in.keySet()) {
                        if (!inVar.equals(lValue)) {
                            changed |= out.update(inVar, in.get(inVar));
                        }
                    }
                    return out.update(lValue, newValue) || changed;
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
                                ifTrueDecision = IsNullValue.CHECKED_NULL;
                            } else {
                                ifFalseDecision = IsNullValue.CHECKED_NULL;
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
                        ifTrueDecision = IsNullValue.CHECKED_NULL;
                    } else {
                        ifFalseDecision = IsNullValue.CHECKED_NULL;
                    }
                } else if (referenceVal.isDefinitelyNotNull()) {
                    if (ifnull) {
                        ifFalseDecision = referenceVal.isAKaBoom() ? referenceVal : IsNullValue.CHECKED_NN;
                    } else {
                        ifTrueDecision = referenceVal.isAKaBoom() ? referenceVal : IsNullValue.CHECKED_NN;
                    }
                } else { // both branches feasible
                    if (ifnull) {
                        ifTrueDecision = IsNullValue.CHECKED_NULL;
                        ifFalseDecision = IsNullValue.CHECKED_NN;
                    } else {
                        ifTrueDecision = IsNullValue.CHECKED_NN;
                        ifFalseDecision = IsNullValue.CHECKED_NULL;
                    }
                }
                return new IsNullConditionDecision(stmt, referenceVar, ifTrueDecision, ifFalseDecision);
            }

            @Override
            public Boolean visitDefault(Stmt stmt) {
                return out.copyFrom(in);
            }
        }
    }
}

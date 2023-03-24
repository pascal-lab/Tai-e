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

package pascal.taie.analysis.pta.plugin.reflection;

import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.NewArray;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.util.collection.Maps;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Collects type information, i.e., (1) cast type on return value from
 * reflective calls, and (2) declared type of argument assigned to the
 * argument array (if available), and use them to match return type and
 * parameter types of the given reflective targets.
 * This class only supports to match type information for following reflection APIs:
 * <ul>
 *     <li>Class.newInstance()</li>
 *     <li>Constructor.newInstance(Object[])</li>
 *     <li>Method.invoke(Object,Object[])</li>
 * </ul>
 */
class TypeMatcher {

    private record TypeInfo(@Nullable Type returnType, @Nullable List<Type> argumentTypes) {
    }

    private static final Map<String, Integer> argIndexes = Map.of(
            "invoke", 1, // Method.invoke(Object,Object[])
            "newInstance", 0 // Constructor.newInstance(Object[])
    );

    private static final TypeInfo UNKNOWN = new TypeInfo(null, null);

    private final TypeSystem typeSystem;

    private final Map<Invoke, TypeInfo> typeInfos = Maps.newMap();

    TypeMatcher(TypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    boolean isUnmatched(Invoke invoke, JMethod target) {
        TypeInfo typeInfo = getTypeInfo(invoke);
        if (typeInfo != UNKNOWN) {
            // check return type
            Type returnType = typeInfo.returnType();
            if (returnType != null) {
                if (target.isConstructor()) {
                    Type objType = target.getDeclaringClass().getType();
                    if (!typeSystem.isSubtype(returnType, objType)) {
                        return true;
                    }
                } else {
                    Type targetReturnType = target.getReturnType();
                    if (isUnmatched(returnType, targetReturnType)) {
                        return true;
                    }
                }
            }
            // check parameter types
            List<Type> argumentTypes = typeInfo.argumentTypes();
            if (argumentTypes != null) {
                if (argumentTypes.size() != target.getParamCount()) {
                    // argument number and parameter count do not match
                    return true;
                }
                for (int i = 0; i < argumentTypes.size(); ++i) {
                    // i-th argument and parameter do not match
                    Type argType = argumentTypes.get(i);
                    Type paramType = target.getParamType(i);
                    if (isUnmatched(paramType, argType)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isUnmatched(Type toType, Type fromType) {
        if (toType.equals(fromType)) {
            return false;
        }
        if (toType instanceof PrimitiveType type) {
            return !typeSystem.getBoxedType(type).equals(fromType);
        }
        return !typeSystem.isSubtype(toType, fromType);
    }

    boolean hasTypeInfo(Invoke invoke) {
        return getTypeInfo(invoke).argumentTypes() != null;
    }

    private TypeInfo getTypeInfo(Invoke invoke) {
        return typeInfos.computeIfAbsent(invoke, TypeMatcher::computeTypeInfo);
    }

    /**
     * Performs an intra-procedural analysis to compute available
     * type information for reflective call {@code invoke}.
     */
    private static TypeInfo computeTypeInfo(Invoke invoke) {
        List<Stmt> stmts = invoke.getContainer().getIR().getStmts();
        // search cast type on result of invoke
        Var result = invoke.getResult();
        Type returnType = null;
        if (result != null) {
            for (int i = invoke.getIndex() + 1; i < stmts.size(); ++i) {
                Stmt stmt = stmts.get(i);
                if (stmt.getUses().contains(result) && returnType != null) {
                    // found multiple usages of the result, give up
                    returnType = null;
                    break;
                }
                if (stmt instanceof Cast cast &&
                        cast.getRValue().getValue().equals(result)) {
                    assert returnType == null;
                    returnType = cast.getRValue().getCastType();
                }
            }
        }
        if (invoke.getInvokeExp().getArgCount() == 0) {
            // no argument, it means that invoke calls Class.newInstance()
            return new TypeInfo(returnType, List.of());
        }
        // search definition of args
        int argIndex = argIndexes.get(invoke.getMethodRef().getName());
        Var args = invoke.getInvokeExp().getArg(argIndex);
        Type[] argTypes = null;
        if (args.isConst()) {
            // if args is constant, it must be null, and for such case,
            // no argument is given.
            argTypes = new Type[0];
        } else {
            assert args.getType() instanceof ArrayType;
            DefinitionStmt<?, ?> argDef = null;
            for (int i = invoke.getIndex() - 1; i >= 0; --i) {
                Stmt stmt = stmts.get(i);
                if (stmt instanceof DefinitionStmt<?, ?> defStmt) {
                    LValue lValue = defStmt.getLValue();
                    if (args.equals(lValue)) { // found definition of args
                        if (argDef == null) {
                            argDef = defStmt;
                            int length = getArrayLength(defStmt.getRValue());
                            if (length != -1) { // found args = new Object[length];
                                argTypes = new Type[length];
                            } else { // args is defined by other ways, give up
                                break;
                            }
                        } else { // found multiple definitions of args, give up
                            argTypes = null;
                            break;
                        }
                    }
                }
            }
            if (argTypes != null) {
                // creation of args is analyzable, collect argument types
                for (int i = argDef.getIndex(); i < stmts.size(); ++i) {
                    Stmt stmt = stmts.get(i);
                    if (stmt instanceof StoreArray storeArray) {
                        ArrayAccess arrayAccess = storeArray.getArrayAccess();
                        if (arrayAccess.getBase().equals(args)) {
                            // args[*] = ...;
                            Var index = arrayAccess.getIndex();
                            if (index.isConst()) { // index is constant
                                int iIndex = ((IntLiteral) index.getConstValue()).getValue();
                                if (argTypes[iIndex] == null) {
                                    argTypes[iIndex] = storeArray.getRValue().getType();
                                } else { // found multiple definitions
                                    // on the same array index, give up
                                    argTypes = null;
                                    break;
                                }
                            } else { // index is not constant, give up
                                argTypes = null;
                                break;
                            }
                        }
                    }
                }
                if (argTypes != null) {
                    // fill NullType for non-assigned array indexes
                    for (int i = 0; i < argTypes.length; ++i) {
                        if (argTypes[i] == null) {
                            argTypes[i] = NullType.NULL;
                        }
                    }
                }
            }
        }
        List<Type> argumentTypes = (argTypes != null) ? List.of(argTypes) : null;
        return (returnType == null && argumentTypes == null)
                ? UNKNOWN : new TypeInfo(returnType, argumentTypes);
    }

    /**
     * Given a {@link RValue}, computes the length of the array it corresponds to.
     * @return the array length, or -1 if {@code rValue} does not correspond to
     * an analyzable array.
     */
    private static int getArrayLength(RValue rValue) {
        if (rValue instanceof NewArray newArray) {
            Var length = newArray.getLength();
            if (length.isConst()
                    && length.getConstValue() instanceof IntLiteral intLiteral) {
                // args = new Object[const];
                return intLiteral.getValue();
            }
        }
        if (rValue instanceof NullLiteral) {
            // args = null;
            return 0;
        }
        if (rValue instanceof CastExp castExp && castExp.getValue().isConst()) {
            // args = (Object[]) null;
            return 0;
        }
        return -1;
    }
}

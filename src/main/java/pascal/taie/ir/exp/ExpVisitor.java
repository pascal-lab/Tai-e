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

package pascal.taie.ir.exp;

/**
 * Exp visitor which may return a result after the visit.
 *
 * @param <T> type of the return value
 */
public interface ExpVisitor<T> {

    // var
    default T visit(Var var) {
        return visitDefault(var);
    }

    // literal
    default T visit(ClassLiteral literal) {
        return visitDefault(literal);
    }

    default T visit(DoubleLiteral literal) {
        return visitDefault(literal);
    }

    default T visit(FloatLiteral literal) {
        return visitDefault(literal);
    }

    default T visit(IntLiteral literal) {
        return visitDefault(literal);
    }

    default T visit(LongLiteral literal) {
        return visitDefault(literal);
    }

    default T visit(NullLiteral literal) {
        return visitDefault(literal);
    }

    default T visit(StringLiteral literal) {
        return visitDefault(literal);
    }

    default T visit(MethodHandle methodHandle) {
        return visitDefault(methodHandle);
    }

    default T visit(MethodType methodType) {
        return visitDefault(methodType);
    }

    // field access
    default T visit(InstanceFieldAccess fieldAccess) {
        return visitDefault(fieldAccess);
    }

    default T visit(StaticFieldAccess fieldAccess) {
        return visitDefault(fieldAccess);
    }

    // array access
    default T visit(ArrayAccess arrayAccess) {
        return visitDefault(arrayAccess);
    }

    // new
    default T visit(NewArray newArray) {
        return visitDefault(newArray);
    }

    default T visit(NewInstance newInstance) {
        return visitDefault(newInstance);
    }

    default T visit(NewMultiArray newMultiArray) {
        return visitDefault(newMultiArray);
    }

    // invoke
    default T visit(InvokeInterface invoke) {
        return visitDefault(invoke);
    }

    default T visit(InvokeSpecial invoke) {
        return visitDefault(invoke);
    }

    default T visit(InvokeStatic invoke) {
        return visitDefault(invoke);
    }

    default T visit(InvokeVirtual invoke) {
        return visitDefault(invoke);
    }

    default T visit(InvokeDynamic invoke) {
        return visitDefault(invoke);
    }

    // unary
    default T visit(ArrayLengthExp exp) {
        return visitDefault(exp);
    }

    default T visit(NegExp exp) {
        return visitDefault(exp);
    }

    // binary
    default T visit(ArithmeticExp exp) {
        return visitDefault(exp);
    }

    default T visit(BitwiseExp exp) {
        return visitDefault(exp);
    }

    default T visit(ComparisonExp exp) {
        return visitDefault(exp);
    }

    default T visit(ConditionExp exp) {
        return visitDefault(exp);
    }

    default T visit(ShiftExp exp) {
        return visitDefault(exp);
    }

    // instanceof
    default T visit(InstanceOfExp exp) {
        return visitDefault(exp);
    }

    // cast
    default T visit(CastExp exp) {
        return visitDefault(exp);
    }

    // default
    default T visitDefault(Exp exp) {
        return null;
    }
}

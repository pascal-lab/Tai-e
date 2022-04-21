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

import pascal.taie.World;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.proginfo.MemberRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.Hashes;

import java.lang.invoke.MethodHandleInfo;

import static pascal.taie.language.classes.ClassNames.METHOD_HANDLE;

/**
 * Representation of java.lang.invoke.MethodHandle instances.
 * A method handle is a typed, directly executable reference to an underlying
 * method, constructor, field or similar low-level operation.
 * See https://docs.oracle.com/javase/7/docs/api/java/lang/invoke/MethodHandle.html
 * for more details.
 */
public class MethodHandle implements ReferenceLiteral {

    private final Kind kind;

    private final MemberRef memberRef;

    private MethodHandle(Kind kind, MemberRef memberRef) {
        this.kind = kind;
        this.memberRef = memberRef;
        assert (isMethodRef() && memberRef instanceof MethodRef) ||
                (isFieldRef() && memberRef instanceof FieldRef) :
                "Member reference does not match method handle kind";
    }

    public static MethodHandle get(Kind kind, MemberRef memberRef) {
        return new MethodHandle(kind, memberRef);
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isMethodRef() {
        return switch (kind) {
            case REF_invokeStatic,
                    REF_invokeSpecial, REF_invokeVirtual,
                    REF_newInvokeSpecial, REF_invokeInterface -> true;
            default -> false;
        };
    }

    public MethodRef getMethodRef() {
        assert isMethodRef() : "This method handle is not method reference";
        return (MethodRef) memberRef;
    }

    public boolean isFieldRef() {
        return switch (kind) {
            case REF_getField, REF_getStatic,
                    REF_putField, REF_putStatic -> true;
            default -> false;
        };
    }

    public FieldRef getFieldRef() {
        assert isFieldRef() : "This method handle is not field reference";
        return (FieldRef) memberRef;
    }

    @Override
    public ClassType getType() {
        return World.get().getTypeSystem().getClassType(METHOD_HANDLE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodHandle that = (MethodHandle) o;
        return kind == that.kind && memberRef.equals(that.memberRef);
    }

    @Override
    public int hashCode() {
        return Hashes.hash(kind, memberRef);
    }

    @Override
    public String toString() {
        return String.format("MethodHandle[%s]: %s", kind, memberRef);
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public enum Kind {

        REF_getField(MethodHandleInfo.REF_getField),
        REF_getStatic(MethodHandleInfo.REF_getStatic),
        REF_putField(MethodHandleInfo.REF_putField),
        REF_putStatic(MethodHandleInfo.REF_putStatic),
        REF_invokeVirtual(MethodHandleInfo.REF_invokeVirtual),
        REF_invokeStatic(MethodHandleInfo.REF_invokeStatic),
        REF_invokeSpecial(MethodHandleInfo.REF_invokeSpecial),
        REF_newInvokeSpecial(MethodHandleInfo.REF_newInvokeSpecial),
        REF_invokeInterface(MethodHandleInfo.REF_invokeInterface),
        ;

        private final int value;

        Kind(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Kind get(int kind) {
            return switch (kind) {
                case MethodHandleInfo.REF_getField -> REF_getField;
                case MethodHandleInfo.REF_getStatic -> REF_getStatic;
                case MethodHandleInfo.REF_putField -> REF_putField;
                case MethodHandleInfo.REF_putStatic -> REF_putStatic;
                case MethodHandleInfo.REF_invokeVirtual -> REF_invokeVirtual;
                case MethodHandleInfo.REF_invokeStatic -> REF_invokeStatic;
                case MethodHandleInfo.REF_invokeSpecial -> REF_invokeSpecial;
                case MethodHandleInfo.REF_newInvokeSpecial -> REF_newInvokeSpecial;
                case MethodHandleInfo.REF_invokeInterface -> REF_invokeInterface;
                default -> throw new AnalysisException("No MethodHandle kind for " + kind);
            };
        }
    }
}

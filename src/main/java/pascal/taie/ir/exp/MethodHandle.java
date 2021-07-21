/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.ir.exp;

import pascal.taie.World;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.proginfo.MemberRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.HashUtils;

import java.lang.invoke.MethodHandleInfo;

import static pascal.taie.language.classes.StringReps.METHOD_HANDLE;

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
        switch (kind) {
            case REF_invokeStatic:
            case REF_invokeSpecial:
            case REF_invokeVirtual:
            case REF_newInvokeSpecial:
            case REF_invokeInterface:
                return true;
            default:
                return false;
        }
    }

    public MethodRef getMethodRef() {
        assert isMethodRef() : "This method handle is not method reference";
        return (MethodRef) memberRef;
    }

    public boolean isFieldRef() {
        switch (kind) {
            case REF_getField:
            case REF_getStatic:
            case REF_putField:
            case REF_putStatic:
                return true;
            default:
                return false;
        }
    }
    public FieldRef getFieldRef() {
        assert isFieldRef() : "This method handle is not field reference";
        return (FieldRef) memberRef;
    }

    @Override
    public ClassType getType() {
        return World.getTypeManager().getClassType(METHOD_HANDLE);
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
        return HashUtils.hash(kind, memberRef);
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
            switch (kind) {
                case MethodHandleInfo.REF_getField:
                    return REF_getField;
                case MethodHandleInfo.REF_getStatic:
                    return REF_getStatic;
                case MethodHandleInfo.REF_putField:
                    return REF_putField;
                case MethodHandleInfo.REF_putStatic:
                    return REF_putStatic;
                case MethodHandleInfo.REF_invokeVirtual:
                    return REF_invokeVirtual;
                case MethodHandleInfo.REF_invokeStatic:
                    return REF_invokeStatic;
                case MethodHandleInfo.REF_invokeSpecial:
                    return REF_invokeSpecial;
                case MethodHandleInfo.REF_newInvokeSpecial:
                    return REF_newInvokeSpecial;
                case MethodHandleInfo.REF_invokeInterface:
                    return REF_invokeInterface;
                default:
                    throw new AnalysisException("No MethodHandle kind for " + kind);
            }
        }
    }
}

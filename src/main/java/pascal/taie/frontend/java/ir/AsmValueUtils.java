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

package pascal.taie.frontend.java.ir;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.DoubleLiteral;
import pascal.taie.ir.exp.FloatLiteral;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.LongLiteral;
import pascal.taie.ir.exp.MethodHandle;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.proginfo.MemberRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.classes.JClass;
import pascal.taie.util.collection.Pair;

import java.util.List;

/**
 * Utility functions for frontend.
 */
public final class AsmValueUtils {

    private AsmValueUtils() {
    }

    public static Literal fromObject(FrontendTypeSystem typeSystem, Object o) {
        // TODO: handle MethodType / ConstantDynamic
        if (o instanceof Integer i) {
            return IntLiteral.get(i);
        } else if (o instanceof Long l) {
            return LongLiteral.get(l);
        } else if (o instanceof Float f) {
            return FloatLiteral.get(f);
        } else if (o instanceof Double d) {
            return DoubleLiteral.get(d);
        } else if (o instanceof String s) {
            return StringLiteral.get(s);
        } else if (o instanceof Type t) {
            if (t.getSort() == Type.METHOD) {
                return typeSystem.toMethodType(t);
            } else {
                return ClassLiteral.get(typeSystem.fromAsmType(t));
            }
        } else if (o instanceof Handle handle) {
            return fromAsmHandle(typeSystem, handle);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static MethodHandle fromAsmHandle(FrontendTypeSystem typeSystem, Handle handle) {
        MethodHandle.Kind kind = toMethodHandleKind(handle.getTag());
        MemberRef ref;
        JClass jClass = typeSystem.toJClass(handle.getOwner());
        if (isFieldKind(kind)) {
            pascal.taie.language.type.Type t = typeSystem.fromAsmTypeDesc(handle.getDesc());
            ref = FieldRef.get(jClass, handle.getName(), t, kind == MethodHandle.Kind.REF_getStatic || kind == MethodHandle.Kind.REF_putStatic);
        } else {
            Pair<List<pascal.taie.language.type.Type>, pascal.taie.language.type.Type> mtdType = typeSystem.fromAsmMethodDesc(handle.getDesc());
            ref = MethodRef.get(jClass, handle.getName(), mtdType.first(), mtdType.second(), kind == MethodHandle.Kind.REF_invokeStatic, handle.isInterface());
        }
        return MethodHandle.get(kind, ref);
    }

    static boolean isFieldKind(MethodHandle.Kind kind) {
        return switch (kind) {
            case REF_getField, REF_getStatic, REF_putField, REF_putStatic -> true;
            default -> false;
        };
    }

    static MethodHandle.Kind toMethodHandleKind(int opcode) {
        return switch (opcode) {
            case Opcodes.H_GETFIELD -> MethodHandle.Kind.REF_getField;
            case Opcodes.H_GETSTATIC -> MethodHandle.Kind.REF_getStatic;
            case Opcodes.H_PUTFIELD -> MethodHandle.Kind.REF_putField;
            case Opcodes.H_PUTSTATIC -> MethodHandle.Kind.REF_putStatic;
            case Opcodes.H_INVOKEVIRTUAL -> MethodHandle.Kind.REF_invokeVirtual;
            case Opcodes.H_INVOKESTATIC -> MethodHandle.Kind.REF_invokeStatic;
            case Opcodes.H_INVOKESPECIAL -> MethodHandle.Kind.REF_invokeSpecial;
            case Opcodes.H_NEWINVOKESPECIAL -> MethodHandle.Kind.REF_newInvokeSpecial;
            case Opcodes.H_INVOKEINTERFACE -> MethodHandle.Kind.REF_invokeInterface;
            default -> throw new IllegalArgumentException();
        };
    }
}

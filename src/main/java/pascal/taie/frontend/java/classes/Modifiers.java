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

package pascal.taie.frontend.java.classes;

import org.objectweb.asm.Opcodes;
import pascal.taie.language.classes.Modifier;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for converting between ASM modifiers and Tai-e modifiers.
 */
public class Modifiers {

    // Common modifier sets for fast lookup
    private static final Set<Modifier> PUB = EnumSet.of(Modifier.PUBLIC);
    private static final Set<Modifier> PRI = EnumSet.of(Modifier.PRIVATE);
    private static final Set<Modifier> PRO = EnumSet.of(Modifier.PROTECTED);
    private static final Set<Modifier> STA = EnumSet.of(Modifier.STATIC);
    private static final Set<Modifier> PUB_FINAL = EnumSet.of(Modifier.PUBLIC, Modifier.FINAL);
    private static final Set<Modifier> PRI_FINAL = EnumSet.of(Modifier.PRIVATE, Modifier.FINAL);
    private static final Set<Modifier> PRO_FINAL = EnumSet.of(Modifier.PROTECTED, Modifier.FINAL);

    // Class modifiers
    private static final List<Modifier> CLASS_MODIFIERS = List.of(
            Modifier.PUBLIC, Modifier.FINAL, Modifier.INTERFACE, Modifier.ABSTRACT,
            Modifier.SYNTHETIC, Modifier.ANNOTATION, Modifier.ENUM);

    private static final int[] CLASS_ASM_MODIFIERS =
            CLASS_MODIFIERS.stream().mapToInt(Modifiers::toAsm).toArray();

    // Field modifiers
    private static final List<Modifier> FIELD_MODIFIERS = List.of(
            Modifier.PUBLIC, Modifier.PRIVATE, Modifier.PROTECTED, Modifier.STATIC,
            Modifier.FINAL, Modifier.VOLATILE, Modifier.TRANSIENT, Modifier.SYNTHETIC,
            Modifier.ENUM);

    private static final int[] FIELD_ASM_MODIFIERS =
            FIELD_MODIFIERS.stream().mapToInt(Modifiers::toAsm).toArray();

    // Method modifiers
    private static final List<Modifier> METHOD_MODIFIERS = List.of(
            Modifier.PUBLIC, Modifier.PRIVATE, Modifier.PROTECTED, Modifier.STATIC,
            Modifier.FINAL, Modifier.SYNCHRONIZED, Modifier.BRIDGE, Modifier.VARARGS,
            Modifier.NATIVE, Modifier.ABSTRACT, Modifier.STRICTFP, Modifier.SYNTHETIC);

    private static final int[] METHOD_ASM_MODIFIERS =
            METHOD_MODIFIERS.stream().mapToInt(Modifiers::toAsm).toArray();

    static Set<Modifier> fromAsm(int opcodes, int[] asmModifiers,
                                 List<Modifier> taieModifiers) {
        switch (opcodes) {
            case Opcodes.ACC_PUBLIC:
                return PUB;
            case Opcodes.ACC_PRIVATE:
                return PRI;
            case Opcodes.ACC_PROTECTED:
                return PRO;
            case Opcodes.ACC_STATIC:
                return STA;
            case Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL:
                return PUB_FINAL;
            case Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL:
                return PRI_FINAL;
            case Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL:
                return PRO_FINAL;
        }
        Set<Modifier> res = EnumSet.noneOf(Modifier.class);
        for (int i = 0; i < taieModifiers.size(); i++) {
            if ((opcodes & asmModifiers[i]) != 0) {
                res.add(taieModifiers.get(i));
            }
        }
        return res;
    }

    static Set<Modifier> fromAsmClass(int opcodes) {
        return fromAsm(opcodes, CLASS_ASM_MODIFIERS, CLASS_MODIFIERS);
    }

    static Set<Modifier> fromAsmField(int opcodes) {
        return fromAsm(opcodes, FIELD_ASM_MODIFIERS, FIELD_MODIFIERS);
    }

    static Set<Modifier> fromAsmMethod(int opcodes) {
        return fromAsm(opcodes, METHOD_ASM_MODIFIERS, METHOD_MODIFIERS);
    }

    public static int toAsm(Set<Modifier> modifiers) {
        int res = 0;
        for (Modifier modifier : modifiers) {
            res |= toAsm(modifier);
        }
        return res;
    }

    private static int toAsm(Modifier modifier) {
        return switch (modifier) {
            case PUBLIC -> Opcodes.ACC_PUBLIC;
            case PRIVATE -> Opcodes.ACC_PRIVATE;
            case PROTECTED -> Opcodes.ACC_PROTECTED;
            case STATIC -> Opcodes.ACC_STATIC;
            case FINAL -> Opcodes.ACC_FINAL;
            case SYNCHRONIZED -> Opcodes.ACC_SYNCHRONIZED;
            case VOLATILE -> Opcodes.ACC_VOLATILE;
            case TRANSIENT -> Opcodes.ACC_TRANSIENT;
            case NATIVE -> Opcodes.ACC_NATIVE;
            case INTERFACE -> Opcodes.ACC_INTERFACE;
            case ABSTRACT -> Opcodes.ACC_ABSTRACT;
            case STRICTFP -> Opcodes.ACC_STRICT;
            case BRIDGE -> Opcodes.ACC_BRIDGE;
            case VARARGS -> Opcodes.ACC_VARARGS;
            case SYNTHETIC -> Opcodes.ACC_SYNTHETIC;
            case ANNOTATION -> Opcodes.ACC_ANNOTATION;
            case ENUM -> Opcodes.ACC_ENUM;
            case MANDATED -> Opcodes.ACC_MANDATED;
        };
    }
}

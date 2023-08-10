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


package pascal.taie.language.generics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GSignaturesTest {

    private static final String[][] CLASS_SIGNATURES = {
            {
                    "false",
                    "<E extends java.lang.Enum<E>>"
                            + " implements java.lang.Comparable<E>, java.io.Serializable",
                    "<E:Ljava/lang/Enum<TE;>;>"
                            + "Ljava/lang/Object;"
                            + "Ljava/lang/Comparable<TE;>;"
                            + "Ljava/io/Serializable;",
            },
            {
                    "true",
                    "<D extends java.lang.reflect.GenericDeclaration>"
                            + " extends java.lang.reflect.Type",
                    "<D::Ljava/lang/reflect/GenericDeclaration;>"
                            + "Ljava/lang/Object;"
                            + "Ljava/lang/reflect/Type;",
            },
            {
                    "false",
                    "<K, V>"
                            + " extends java.util.AbstractMap<K, V>"
                            + " implements java.util.concurrent.ConcurrentMap<K, V>," +
                            " java.io.Serializable",
                    "<K:Ljava/lang/Object;V:Ljava/lang/Object;>"
                            + "Ljava/util/AbstractMap<TK;TV;>;"
                            + "Ljava/util/concurrent/ConcurrentMap<TK;TV;>;"
                            + "Ljava/io/Serializable;",
            },
            {
                    "false",
                    "<K extends java.lang.Enum<K>, V>"
                            + " extends java.util.AbstractMap<K, V>"
                            + " implements java.io.Serializable, java.lang.Cloneable",
                    "<K:Ljava/lang/Enum<TK;>;V:Ljava/lang/Object;>"
                            + "Ljava/util/AbstractMap<TK;TV;>;"
                            + "Ljava/io/Serializable;"
                            + "Ljava/lang/Cloneable;",
            },
            {
                    "false",
                    "<T, R extends T>",
                    "<T:Ljava/lang/Object;R:TT;>Ljava/lang/Object;",
            },
            {
                    "false",
                    "<U0, U1 extends java.lang.Number,"
                            + " U2 extends java.util.List<java.lang.String>,"
                            + " U3 extends java.util.List<?>,"
                            + " U4 extends java.util.List<? extends java.lang.Number>,"
                            + " U5 extends java.util.List<? super java.lang.Number>,"
                            + " U6 extends java.lang.Number & java.lang.Runnable"
                            + " & java.lang.Cloneable>"
                            + " implements java.util.Comparator<java.lang.Integer>",
                    "<U0:Ljava/lang/Object;"
                            + "U1:Ljava/lang/Number;"
                            + "U2::Ljava/util/List<Ljava/lang/String;>;"
                            + "U3::Ljava/util/List<*>;U4::Ljava/util/List<+Ljava/lang/Number;>;"
                            + "U5::Ljava/util/List<-Ljava/lang/Number;>;"
                            + "U6:Ljava/lang/Number;:Ljava/lang/Runnable;:Ljava/lang/Cloneable;>"
                            + "Ljava/lang/Object;"
                            + "Ljava/util/Comparator<Ljava/lang/Integer;>;"
            },
    };

    private static final String[][] METHOD_SIGNATURES = {
            {
                    "void () throws E, F",
                    "()V^TE;^TF;",
            },
            {
                    "void (A<E>.B)",
                    "(LA<TE;>.B;)V",
            },
            {
                    "void (A<E>.B<F>)",
                    "(LA<TE;>.B<TF;>;)V",
            },
            {
                    "void (boolean, byte, char, short, int, float, long, double)",
                    "(ZBCSIFJD)V",
            },
            {
                    "<E extends java.lang.Class> java.lang.Class<? extends E> ()",
                    "<E:Ljava/lang/Class;>()Ljava/lang/Class<+TE;>;",
            },
            {
                    "<E extends java.lang.Class> java.lang.Class<? super E> ()",
                    "<E:Ljava/lang/Class;>()Ljava/lang/Class<-TE;>;",
            },
            {
                    "void (java.lang.String, java.lang.Class<?>, java.lang.reflect.Method[], java.lang.reflect.Method, java.lang.reflect.Method)",
                    "(Ljava/lang/String;Ljava/lang/Class<*>;[Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;)V",
            },
            {
                    "java.util.Map<java.lang.Object, java.lang.String> (java.lang.Object, java.util.Map<java.lang.Object, java.lang.String>)",
                    "(Ljava/lang/Object;Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;)Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;",
            },
            {
                    "<T> java.util.Map<java.lang.Object, java.lang.String> (java.lang.Object, java.util.Map<java.lang.Object, java.lang.String>, T)",
                    "<T:Ljava/lang/Object;>(Ljava/lang/Object;Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;TT;)Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;",
            },
            {
                    "<E, T extends java.lang.Comparable<E>> java.util.Map<java.lang.Object, java.lang.String> (java.lang.Object, java.util.Map<java.lang.Object, java.lang.String>, T)",
                    "<E:Ljava/lang/Object;T::Ljava/lang/Comparable<TE;>;>(Ljava/lang/Object;Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;TT;)Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;",
            },
            {
                    "<U0, U1 extends java.lang.Number,"
                            + " U2 extends java.util.List<java.lang.String>,"
                            + " U3 extends java.util.List<?>,"
                            + " U4 extends java.util.List<? extends java.lang.Number>,"
                            + " U5 extends java.util.List<? super java.lang.Number>,"
                            + " U6 extends java.lang.Number & java.lang.Runnable"
                            + " & java.lang.Cloneable,"
                            + " U7 extends java.lang.Exception,"
                            + " U8 extends java.io.IOException>"
                            + " void (java.util.List<U0>,"
                            + " java.util.List<U1[]>,"
                            + " java.util.List<U2[][]>,"
                            + " java.util.List<U3>,"
                            + " java.util.List<U4>,"
                            + " java.util.List<U5>,"
                            + " java.util.List<U6>,"
                            + " jdk5.AllStructures<U0, U1, U2, U3, U4, U5, U6>.InnerClass,"
                            +
                            " jdk5.AllStructures<U0, U1, U2, U3, U4, U5, U6>.GenericInnerClass<U1>)"
                            + " throws U7, U8",
                    "<U0:Ljava/lang/Object;"
                            + "U1:Ljava/lang/Number;"
                            + "U2::Ljava/util/List<Ljava/lang/String;>;"
                            + "U3::Ljava/util/List<*>;"
                            + "U4::Ljava/util/List<+Ljava/lang/Number;>;"
                            + "U5::Ljava/util/List<-Ljava/lang/Number;>;"
                            + "U6:Ljava/lang/Number;:Ljava/lang/Runnable;:Ljava/lang/Cloneable;"
                            + "U7:Ljava/lang/Exception;"
                            + "U8:Ljava/io/IOException;>"
                            + "(Ljava/util/List<TU0;>;"
                            + "Ljava/util/List<[TU1;>;"
                            + "Ljava/util/List<[[TU2;>;"
                            + "Ljava/util/List<TU3;>;"
                            + "Ljava/util/List<TU4;>;"
                            + "Ljava/util/List<TU5;>;"
                            + "Ljava/util/List<TU6;>;"
                            + "Ljdk5/AllStructures<TU0;TU1;TU2;TU3;TU4;TU5;TU6;>.InnerClass;"
                            +
                            "Ljdk5/AllStructures<TU0;TU1;TU2;TU3;TU4;TU5;TU6;>.GenericInnerClass<TU1;>;)"
                            + "V"
                            + "^TU7;"
                            + "^TU8;"
            },
    };

    private static final String[][] TYPE_SIGNATURES = {
            {"T", "TT;"},
            {"java.lang.Object", "Ljava/lang/Object;"},
            {"byte", "B"},
            {"char", "C"},
            {"double", "D"},
            {"float", "F"},
            {"int", "I"},
            {"long", "J"},
            {"short", "S"},
            {"boolean", "Z"},
            {"void", "V"},
            {"Generic<Open, Generic, Close>", "LGeneric<LOpen;LGeneric;LClose;>;"},
            {"java.util.HashMap<K, V>", "Ljava/util/HashMap<TK;TV;>;"},
            {
                    "Optional<java.util.Map<java.lang.String, java.lang.String>[][]>",
                    "LOptional<[[Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;>;"
            },
            {"java.lang.Object[][]", "[[Ljava/lang/Object;"},
            {"java.util.HashMap<K, V>[][]", "[[Ljava/util/HashMap<TK;TV;>;"},
            {"byte[][]", "[[B"},
            {"T[][]", "[[TT;"},
            {
                    "java.util.HashMap<K, V>.HashIterator<K>",
                    "Ljava/util/HashMap<TK;TV;>.HashIterator<TK;>;"
            },
    };

    static Stream<Arguments> classSignatures() {
        return Arrays.stream(CLASS_SIGNATURES)
                .map(values -> Arguments.of((Object[]) values));
    }

    static Stream<Arguments> methodSignatures() {
        return Arrays.stream(METHOD_SIGNATURES)
                .map(values -> Arguments.of((Object[]) values));
    }

    static Stream<Arguments> typeSignatures() {
        return Arrays.stream(TYPE_SIGNATURES)
                .map(values -> Arguments.of((Object[]) values));
    }

    @ParameterizedTest
    @MethodSource("classSignatures")
    void toClassSig(boolean isInterface, String expected, String input) {
        ClassGSignature gSig = GSignatures.toClassSig(isInterface, input);
        assertEquals(expected, gSig.toString());
    }

    @ParameterizedTest
    @MethodSource("methodSignatures")
    void toMethodSig(String expected, String input) {
        MethodGSignature gSig = GSignatures.toMethodSig(input);
        assertEquals(expected, gSig.toString());
    }

    @ParameterizedTest
    @MethodSource("typeSignatures")
    void toTypeSig(String expected, String input) {
        TypeGSignature gSig = GSignatures.toTypeSig(input);
        assertEquals(expected, gSig.toString());
    }

    @Test
    void testDeepArray() {
        for (int i = 1; i <= 48; i++) {
            ArrayTypeGSignature gSig = GSignatures.toTypeSig("[".repeat(i) + "TT;");
            assertEquals("T" + "[]".repeat(i), gSig.toString());
        }
    }

    @Test
    void testDeepGenerics() {
        for (int i = 1; i <= 48; i++) {
            ClassTypeGSignature gSig = GSignatures.toTypeSig(buildDeepGenerics(i, true));
            assertEquals(buildDeepGenerics(i, false), gSig.toString());
        }
    }

    private static String buildDeepGenerics(int depth, boolean isBytecodeFotmat) {
        return buildDeepGenerics(new StringBuilder(), depth, isBytecodeFotmat).toString();
    }

    private static StringBuilder buildDeepGenerics(final StringBuilder sb,
                                                   final int depth,
                                                   boolean isBytecodeFotmat) {
        sb.append(isBytecodeFotmat ? "LG" : "G");
        if (depth == 0) {
            if (isBytecodeFotmat) {
                sb.append(';');
            }
            return sb;
        }
        sb.append(isBytecodeFotmat ? "<LL;" : "<L, ");
        buildDeepGenerics(sb, depth - 1, isBytecodeFotmat);
        sb.append(isBytecodeFotmat ? "LR;>;" : ", R>");
        return sb;
    }
}

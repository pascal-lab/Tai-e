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

package pascal.taie.language.classes;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pascal.taie.language.classes.Pattern.ClassPattern;
import static pascal.taie.language.classes.Pattern.FieldPattern;
import static pascal.taie.language.classes.Pattern.MethodPattern;
import static pascal.taie.language.classes.Pattern.NAME_WILDCARD;
import static pascal.taie.language.classes.Pattern.NAME_WILDCARD_MARK;
import static pascal.taie.language.classes.Pattern.NamePattern;
import static pascal.taie.language.classes.Pattern.ParamUnit;
import static pascal.taie.language.classes.Pattern.Repeat;
import static pascal.taie.language.classes.Pattern.StringUnit;
import static pascal.taie.language.classes.Pattern.TypePattern;
import static pascal.taie.language.classes.Pattern.parseClassPattern;
import static pascal.taie.language.classes.Pattern.parseFieldPattern;
import static pascal.taie.language.classes.Pattern.parseMethodPattern;
import static pascal.taie.language.classes.Pattern.parseNamePattern;

// CHECKSTYLE:OFF
public class PatternTest {

    private static NamePattern NP(String... nps) {
        return new NamePattern(Stream.of(nps)
                .map(np -> switch (np) {
                    case NAME_WILDCARD_MARK -> NAME_WILDCARD;
                    default -> new StringUnit(np);
                })
                .toList());
    }

    @Test
    void testNamePattern() {
        assertEquals(NP("*"),
                parseNamePattern("*"));
        assertEquals(NP("ABC"),
                parseNamePattern("ABC"));
        assertEquals(NP("com", "*", "X"),
                parseNamePattern("com*X"));
        assertEquals(NP("com.example.", "*"),
                parseNamePattern("com.example.*"));
        assertEquals(NP("com.example.", "*", ".abc.", "*"),
                parseNamePattern("com.example.*.abc.*"));
        assertEquals(NP("com.example.", "*", ".abc.", "*"),
                parseNamePattern("com.example.*.abc.*"));
        assertEquals(NP("com.example.", "*", ".abc.", "*", ".def"),
                parseNamePattern("com.example.*.abc.*.def"));
    }

    /**
     * Class pattern, without subclasses.
     */
    private static ClassPattern CP1(String... nps) {
        return new ClassPattern(NP(nps), false);
    }

    /**
     * Class pattern, including subclasses.
     */
    private static ClassPattern CP2(String... nps) {
        return new ClassPattern(NP(nps), true);
    }

    @Test
    void testClassPattern() {
        assertEquals(CP1("com.example.", "*"),
                parseClassPattern("com.example.*"));
        assertEquals(CP1("com", "*", "X"),
                parseClassPattern("com*X"));
        assertEquals(CP1("com.example.", "*", ".abc.", "*"),
                parseClassPattern("com.example.*.abc.*"));
        assertEquals(CP1("com.example.", "*", ".abc.", "*"),
                parseClassPattern("com.example.*.abc.*"));
        assertEquals(CP1("com.example.", "*", ".abc.", "*", ".def"),
                parseClassPattern("com.example.*.abc.*.def"));
        assertEquals(CP2("com", "*", "X"),
                parseClassPattern("com*X^"));
        assertEquals(CP2("com.example.", "*", ".abc.", "*"),
                parseClassPattern("com.example.*.abc.*^"));
        assertEquals(CP2("com.example.", "*", ".abc.", "*"),
                parseClassPattern("com.example.*.abc.*^"));
        assertEquals(CP2("com.example.", "*", ".abc.", "*", ".def"),
                parseClassPattern("com.example.*.abc.*.def^"));
    }

    @Test
    void testClassPatternIsExactMatch() {
        assertTrue(parseClassPattern("com.example.X").isExactMatch());
        assertTrue(parseClassPattern("X").isExactMatch());
        assertFalse(parseClassPattern("com.example.*").isExactMatch());
        assertFalse(parseClassPattern("*.X").isExactMatch());
        assertFalse(parseClassPattern("com.example.X^").isExactMatch());
    }

    /**
     * Type pattern, without subtypes.
     */
    private static TypePattern TP1(String... nps) {
        return new TypePattern(NP(nps), false);
    }

    /**
     * Type pattern, including subtypes.
     */
    private static TypePattern TP2(String... nps) {
        return new TypePattern(NP(nps), true);
    }

    private static ParamUnit PU(TypePattern type, int min, int max) {
        return new ParamUnit(type, new Repeat(min, max));
    }

    private static ParamUnit PU(String... nps) {
        return new ParamUnit(TP1(nps), Repeat.ONCE);
    }

    @Test
    void testMethodPattern() {
        assertEquals(
                new MethodPattern(
                        CP1("com.example.", "*"),
                        TP1("int"),
                        NP("foo"),
                        List.of(PU("java.lang.String"), PU("int"))
                ),
                parseMethodPattern("<com.example.*: int foo(java.lang.String,int)>")
        );
        assertEquals(
                new MethodPattern(
                        CP1("com.example.", "*"),
                        TP1("int"),
                        NP("foo"),
                        List.of(PU("java.lang.String"),
                                PU(TP1("*"), 0, Repeat.MAX))
                ),
                parseMethodPattern("<com.example.*: int foo(java.lang.String,*{0+})>")
        );
        assertEquals(
                new MethodPattern(
                        CP1("org.example.", "*"),
                        TP1("boolean"),
                        NP("bar", "*"),
                        List.of(PU("java.lang.String"),
                                PU(TP1("*"), 2, 2),
                                PU("int"),
                                PU(TP1("*"), 1, 1),
                                PU("boolean"),
                                PU(TP1("*"), 1, 1))
                ),
                parseMethodPattern("<org.example.*: boolean bar*(java.lang.String,*{2},int,*{1},boolean,*{1})>")
        );
        assertEquals(
                new MethodPattern(
                        CP1("com.example.", "*"),
                        TP1("int"),
                        NP("foo", "*"),
                        List.of(PU("java.lang.String"),
                                PU(TP1("*"), 1, 1),
                                PU("int"),
                                PU(TP1("*"), 0, Repeat.MAX))
                ),
                parseMethodPattern("<com.example.*: int foo*(java.lang.String,*{1},int,*{0+})>")
        );
        assertEquals(
                new MethodPattern(
                        CP1("com.test.", "*"),
                        TP1("int"),
                        NP("test", "*"),
                        List.of(PU("java.lang.Character"),
                                PU(TP1("*"), 1, 2),
                                PU("java.lang.String"),
                                PU(TP1("*"), 0, 1))
                ),
                parseMethodPattern("<com.test.*: int test*(java.lang.Character,*{1-2},java.lang.String,*{0-1})>")
        );
        assertEquals(
                new MethodPattern(
                        CP1("example.test.", "*"),
                        TP1("float"),
                        NP("exampleMethod", "*"),
                        List.of(PU("java.lang.Float"),
                                PU(TP1("*", "X"), 3, 3),
                                PU("java.lang.Object"),
                                PU(TP2("com", "*", "X"), 0, 1))
                ),
                parseMethodPattern("<example.test.*: float exampleMethod*(java.lang.Float,*X{3},java.lang.Object,com*X^{0-1})>")
        );
        assertEquals(
                new MethodPattern(
                        CP1("com.example.", "*"),
                        TP1("void"),
                        NP("foo", "*"),
                        List.of(PU(TP2("java.util.Collection"), 1, 1),
                                PU(TP1("*"), 0, Repeat.MAX),
                                PU("java.lang.String"),
                                PU(TP1("*"), 0, Repeat.MAX))
                ),
                parseMethodPattern("<com.example.*: void foo*(java.util.Collection^,*{0+},java.lang.String,*{0+})>")
        );
    }

    @Test
    void testMethodPatternIsExactMatch() {
        assertTrue(parseMethodPattern("<A: B foo(C)>").isExactMatch());
        assertTrue(parseMethodPattern("<A: B foo(C,D)>").isExactMatch());
        assertTrue(parseMethodPattern("<A: B foo(C,D{1})>").isExactMatch());
        assertFalse(parseMethodPattern("<A*: B foo(C)>").isExactMatch());
        assertFalse(parseMethodPattern("<A: B* foo(C)>").isExactMatch());
        assertFalse(parseMethodPattern("<A: B foo*(C)>").isExactMatch());
        assertFalse(parseMethodPattern("<A: B foo(C*)>").isExactMatch());
        assertFalse(parseMethodPattern("<A: B foo(C^,D)>").isExactMatch());
        assertFalse(parseMethodPattern("<A: B foo(C,*{0+},D)>").isExactMatch());
        assertFalse(parseMethodPattern("<A: B foo(C,*{0+})>").isExactMatch());
    }

    @Test
    void testFieldPattern() {
        assertEquals(
                new FieldPattern(
                        CP1("com.example.", "*"),
                        TP1("int"),
                        NP("field", "*")
                ),
                parseFieldPattern("<com.example.*: int field*>")
        );
        assertEquals(
                new FieldPattern(
                        CP1("com", "*", "X"),
                        TP1("int"),
                        NP("field1")
                ),
                parseFieldPattern("<com*X: int field1>")
        );
        assertEquals(
                new FieldPattern(
                        CP1("com", "*", "X"),
                        TP2("java.util.Collection"),
                        NP("field1")
                ),
                parseFieldPattern("<com*X: java.util.Collection^ field1>")
        );
        assertEquals(
                new FieldPattern(
                        CP2("com", "*", "X"),
                        TP2("java.util.Collection"),
                        NP("field1")
                ),
                parseFieldPattern("<com*X^: java.util.Collection^ field1>")
        );
        assertEquals(
                new FieldPattern(
                        CP2("com", "*", "X"),
                        TP2("com.example.", "*"),
                        NP("field2")
                ),
                parseFieldPattern("<com*X^: com.example.*^ field2>")
        );
        assertEquals(
                new FieldPattern(
                        CP1("com.example.", "*"),
                        TP1("void"),
                        NP("field2")
                ),
                parseFieldPattern("<com.example.*: void field2>")
        );
    }

    @Test
    void testFieldPatternIsExactMatch() {
        assertTrue(parseFieldPattern("<A: B foo>").isExactMatch());
        assertFalse(parseFieldPattern("<A*: B foo>").isExactMatch());
        assertFalse(parseFieldPattern("<A^: B foo>").isExactMatch());
        assertFalse(parseFieldPattern("<A: B* foo>").isExactMatch());
        assertFalse(parseFieldPattern("<A: B^ foo>").isExactMatch());
    }
}

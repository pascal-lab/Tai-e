package pascal.taie.language.classes;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pascal.taie.language.classes.Pattern.*;

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

    @Test
    void testMethodPattern() {
        assertEquals(
                new MethodPattern(
                        CP1("com.example.", "*"),
                        TP1("int"),
                        NP("foo"),
                        List.of(TP1("java.lang.String"), TP1("int"))
                ),
                parseMethodPattern("<com.example.*: int foo(java.lang.String,int)>")
        );
        //TODO: The method pattern for *{} is not defined
        assertEquals(
                new MethodPattern(
                        CP1("com.example.", "*"),
                        TP1("int"),
                        NP("foo"),
                        List.of(TP1("java.lang.String"), PARAM_WILDCARD)
                ),
                parseMethodPattern("<com.example.*: int foo(java.lang.String,*{0+})>")
        );
        //TODO: The method pattern for *{} is not defined
        assertEquals(
                new MethodPattern(
                        CP1("com.example.", "*"),
                        TP1("int"),
                        NP("foo", "*"),
                        List.of(TP1("java.lang.String"), PARAM_WILDCARD,
                                TP1("int"), PARAM_WILDCARD)
                ),
                parseMethodPattern("<com.example.*: int foo*(java.lang.String,*{1},int,*{0+})>")
        );
        //TODO: The method pattern for *{} is not defined
        assertEquals(
                new MethodPattern(
                        CP1("com.example.", "*"),
                        TP1("void"),
                        NP("foo", "*"),
                        List.of(TP2("java.util.Collection"), PARAM_WILDCARD,
                                TP1("java.lang.String"), PARAM_WILDCARD)
                ),
                parseMethodPattern("<com.example.*: void foo*(java.util.Collection^,*{0+},java.lang.String,*{0+})>")
        );
    }

    @Test
    void testMethodPatternIsExactMatch() {
        assertTrue(parseMethodPattern("<A: B foo(C)>").isExactMatch());
        assertTrue(parseMethodPattern("<A: B foo(C,D)>").isExactMatch());
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

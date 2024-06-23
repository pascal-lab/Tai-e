package pascal.taie.language.classes;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static pascal.taie.language.classes.Pattern.*;

public class PatternTest {

    private static NamePattern NP(String... nps) {
        return new NamePattern(Stream.of(nps)
                .map(np -> switch (np) {
                    case "**" -> STARSTAR;
                    case "*" -> STAR;
                    default -> new StringUnit(np);
                })
                .toList());
    }

    @Test
    void testNamePattern() {
        assertEquals(NP("**"), parseNamePattern("**"));
        assertEquals(NP("*"), parseNamePattern("*"));
        assertEquals(NP("ABC"), parseNamePattern("ABC"));
        assertEquals(NP("com", "**", "X"), parseNamePattern("com**X"));
        assertEquals(NP("com.example.", "*"), parseNamePattern("com.example.*"));
        assertEquals(NP("com.example.", "*", ".abc.", "**"), parseNamePattern("com.example.*.abc.**"));
        assertEquals(NP("com.example.", "**", ".abc.", "*"), parseNamePattern("com.example.**.abc.*"));
        assertEquals(NP("com.example.", "**", ".abc.", "*", ".def"), parseNamePattern("com.example.**.abc.*.def"));
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
        assertEquals(CP1("com.example.", "*"), ofC("com.example.*"));
        assertEquals(CP1("com", "**", "X"), ofC("com**X"));
        assertEquals(CP1("com.example.", "*", ".abc.", "**"), ofC("com.example.*.abc.**"));
        assertEquals(CP1("com.example.", "**", ".abc.", "*"), ofC("com.example.**.abc.*"));
        assertEquals(CP1("com.example.", "**", ".abc.", "*", ".def"), ofC("com.example.**.abc.*.def"));
        assertEquals(CP2("com", "**", "X"), ofC("com**X^"));
        assertEquals(CP2("com.example.", "*", ".abc.", "**"), ofC("com.example.*.abc.**^"));
        assertEquals(CP2("com.example.", "**", ".abc.", "*"), ofC("com.example.**.abc.*^"));
        assertEquals(CP2("com.example.", "**", ".abc.", "*", ".def"), ofC("com.example.**.abc.*.def^"));
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
                ofM("<com.example.*: int foo(java.lang.String,int)>")
        );
        assertEquals(
                new MethodPattern(
                        CP1("com.example.", "*"),
                        TP1("int"),
                        NP("foo"),
                        List.of(TP1("java.lang.String"), WILDCARD)
                ),
                ofM("<com.example.*: int foo(java.lang.String,~)>")
        );
        assertEquals(
                new MethodPattern(
                        CP1("com.example.", "*"),
                        TP1("int"),
                        NP("foo", "*"),
                        List.of(TP1("java.lang.String"), WILDCARD,
                                TP1("int"), WILDCARD)
                ),
                ofM("<com.example.*: int foo*(java.lang.String,~,int,~)>")
        );
        assertEquals(
                new MethodPattern(
                        CP1("com.example.", "*"),
                        TP1("void"),
                        NP("foo", "*"),
                        List.of(TP2("java.util.Collection"), WILDCARD,
                                TP1("java.lang.String"), WILDCARD)
                ),
                ofM("<com.example.*: void foo*(java.util.Collection^,~,java.lang.String,~)>")
        );
    }
}

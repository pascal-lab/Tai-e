package pascal.taie.language.classes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

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
        Map.of(
                NP("**"), parseNamePattern("**"),
                NP("*"), parseNamePattern("*"),
                NP("ABC"), parseNamePattern("ABC"),
                NP("com", "**", "X"), parseNamePattern("com**X"),
                NP("com.example.", "*"), parseNamePattern("com.example.*"),
                NP("com.example.", "*", ".abc.", "**"), parseNamePattern("com.example.*.abc.**"),
                NP("com.example.", "**", ".abc.", "*"), parseNamePattern("com.example.**.abc.*"),
                NP("com.example.", "**", ".abc.", "*", ".def"), parseNamePattern("com.example.**.abc.*.def")
        ).forEach(Assertions::assertEquals);
    }

    @Test
    void testOfC() {
        Map.of(
                new ClassPattern(NP("com.example.", "*"), false), ofC("com.example.*"),
                new ClassPattern(NP("com", "**", "X"), false), ofC("com**X"),
                new ClassPattern(NP("com.example.", "*", ".abc.", "**"), false), ofC("com.example.*.abc.**"),
                new ClassPattern(NP("com.example.", "**", ".abc.", "*"), false), ofC("com.example.**.abc.*"),
                new ClassPattern(NP("com.example.", "**", ".abc.", "*", ".def"), false), ofC("com.example.**.abc.*.def"),
                new ClassPattern(NP("com", "**", "X"), true), ofC("com**X^"),
                new ClassPattern(NP("com.example.", "*", ".abc.", "**"), true), ofC("com.example.*.abc.**^"),
                new ClassPattern(NP("com.example.", "**", ".abc.", "*"), true), ofC("com.example.**.abc.*^"),
                new ClassPattern(NP("com.example.", "**", ".abc.", "*", ".def"), true), ofC("com.example.**.abc.*.def^")
        ).forEach(Assertions::assertEquals);
    }
    @Test
    void testOfM() {
        Map.of(
                new MethodPattern(
                        new ClassPattern(NP("com.example.", "*"), false),
                        new TypePattern(NP("int"), false),
                        NP("foo", "*"),
                        new ArrayList<>(Arrays.asList(new TypePattern(NP("java.lang.String"), false),
                                new TypePattern(NP("int"), false)))),
                ofM("<com.example.*: int foo(java.lang.String, int)>"),
                new MethodPattern(
                        new ClassPattern(NP("com.example.", "*"), false),
                        new TypePattern(NP("int"), false),
                        NP("foo", "*"),
                        new ArrayList<>(Arrays.asList(new TypePattern(NP("java.lang.String"), false),
                                WILDCARD))),
                ofM("<com.example.*: int foo(java.lang.String, ~)>"),
                new MethodPattern(
                        new ClassPattern(NP("com.example.", "*"), false),
                        new TypePattern(NP("int"), false),
                        NP("foo", "*"),
                        new ArrayList<>(Arrays.asList(new TypePattern(NP("java.lang.String"), false),
                                WILDCARD,
                                new TypePattern(NP("java.lang.String"), false),
                                WILDCARD))),
                ofM("<com.example.*: int foo(java.lang.String, ~, int, ~)>"),
                new MethodPattern(
                        new ClassPattern(NP("com.example.", "*"), false),
                        new TypePattern(NP("void"), false),
                        NP("foo", "*"),
                        new ArrayList<>(Arrays.asList(new TypePattern(NP("java.util.Collection"), true),
                                WILDCARD,
                                new TypePattern(NP("java.lang.String"), false),
                                WILDCARD))),
                ofM("<com.example.*: void foo(java.util.Collection^, ~, int, ~)>")
        ).forEach(Assertions::assertEquals);
    }


    private static ClassPattern CP(boolean includeSubclasses, String... nps) {
        return new ClassPattern(NP(nps), includeSubclasses);
    }
}

package pascal.taie.language.classes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Stream;

import static pascal.taie.language.classes.Pattern.NamePattern;
import static pascal.taie.language.classes.Pattern.STAR;
import static pascal.taie.language.classes.Pattern.STARSTAR;
import static pascal.taie.language.classes.Pattern.StringUnit;
import static pascal.taie.language.classes.Pattern.parseNamePattern;

public class PatternTest {

    private static NamePattern NP(String... nps) {
        return new NamePattern(Stream.of(nps)
                .map(np -> switch (np) {
                    case "**" -> STARSTAR;
                    case "*" -> STAR;
                    default -> new StringUnit(np);})
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
}

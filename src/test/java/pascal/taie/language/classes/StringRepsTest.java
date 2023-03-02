package pascal.taie.language.classes;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StringRepsTest {

    @Test
    public void isJavaClassName() {
        assertTrue(StringReps.isJavaClassName("C"));
        assertTrue(StringReps.isJavaClassName("Cc"));
        assertTrue(StringReps.isJavaClassName("b.C"));
        assertTrue(StringReps.isJavaClassName("b.Cc"));
        assertTrue(StringReps.isJavaClassName("aAa.b.Cc"));
        assertTrue(StringReps.isJavaClassName("a.b.Cc"));
        assertTrue(StringReps.isJavaClassName("a.b.C_c"));
        assertTrue(StringReps.isJavaClassName("a.b.C$c"));
        assertTrue(StringReps.isJavaClassName("a.b.C9"));

        assertFalse("cannot start with a dot",
                StringReps.isJavaClassName(".C"));
        assertFalse("cannot end with a dot",
                StringReps.isJavaClassName("C."));
        assertFalse("cannot have two dots following each other",
                StringReps.isJavaClassName("b..C"));
        assertFalse("cannot start with a number ",
                StringReps.isJavaClassName("b.9C"));
    }

    @Test
    public void isJavaIdentifier() {
        assertTrue(StringReps.isJavaIdentifier("C"));
        assertTrue(StringReps.isJavaIdentifier("Cc"));
        assertTrue(StringReps.isJavaIdentifier("cC"));
        assertTrue(StringReps.isJavaIdentifier("c9"));
        assertTrue(StringReps.isJavaIdentifier("c_"));
        assertTrue(StringReps.isJavaIdentifier("_c"));
        assertTrue(StringReps.isJavaIdentifier("c$"));
        assertTrue(StringReps.isJavaIdentifier("$c"));
        assertTrue(StringReps.isJavaIdentifier("c9_"));
        assertTrue(StringReps.isJavaIdentifier("c9$"));
        assertTrue(StringReps.isJavaIdentifier("c_9"));
        assertTrue(StringReps.isJavaIdentifier("c$_"));
        assertTrue(StringReps.isJavaIdentifier("c$_9"));
        assertTrue(StringReps.isJavaIdentifier("c$_9$"));
        assertTrue(StringReps.isJavaIdentifier("c$_9_"));
        assertTrue(StringReps.isJavaIdentifier("c$_9_9"));
        assertTrue(StringReps.isJavaIdentifier("c$_9_9$"));

        assertFalse("cannot start with a number",
                StringReps.isJavaIdentifier("9C"));
        assertFalse("cannot start with a dot",
                StringReps.isJavaIdentifier(".C"));
        assertFalse("cannot end with a dot",
                StringReps.isJavaIdentifier("C."));
        assertFalse("cannot have two dots following each other",
                StringReps.isJavaIdentifier("b..C"));
    }
}

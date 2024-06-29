package pascal.taie.language.classes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pascal.taie.Main;
import pascal.taie.World;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MatcherTest {

    private static final String CLASS_PATH = "src/test/resources/sigmatcher";

    private static final String MAIN_CLASS = "com.example.X";

    private static ClassHierarchy hierarchy;

    private static Matcher matcher;

    @BeforeAll
    public static void buildWorld() {
        Main.buildWorld("-cp", CLASS_PATH, "-m", MAIN_CLASS);
        hierarchy = World.get().getClassHierarchy();
        matcher = new Matcher(hierarchy);
    }

    @Test
    void testGetClasses() {
        JClass e_x = hierarchy.getClass("com.example.X");
        JClass e_x1 = hierarchy.getClass("com.example.X1");
        JClass y = hierarchy.getClass("com.example.Y");
        JClass XFather = hierarchy.getClass("com.example.XFather");
        JClass e1_x = hierarchy.getClass("com.example1.X");
        JClass e1_x1 = hierarchy.getClass("com.example1.X1");

        assertEquals(Set.of(e_x, e1_x),
                matcher.getClasses(Pattern.parseClassPattern("com*X")));
        assertEquals(Set.of(e_x, y, XFather, e_x1),
                matcher.getClasses(Pattern.parseClassPattern("com.example.*")));
        assertEquals(Set.of(e1_x, e1_x1),
                matcher.getClasses(Pattern.parseClassPattern("com.example1.*")));
        assertEquals(Set.of(e_x1, e1_x1),
                matcher.getClasses(Pattern.parseClassPattern("com.*.X1")));
        assertEquals(Set.of(e_x, e_x1, y, XFather, e1_x, e1_x1),
                matcher.getClasses(Pattern.parseClassPattern("*example*")));
        assertEquals(Set.of(XFather),
                matcher.getClasses(Pattern.parseClassPattern("*.XFather")));
        assertEquals(Set.of(e_x, e_x1, y, XFather, e1_x, e1_x1),
                matcher.getClasses(Pattern.parseClassPattern("*"))
                        .stream()
                        .filter(JClass::isApplication)
                        .collect(Collectors.toSet()));
        assertEquals(Set.of(XFather, e_x, e_x1),
                matcher.getClasses(Pattern.parseClassPattern("com.example.XFather^")));
        assertEquals(Set.of(e_x, e_x1, y, XFather, e1_x, e1_x1),
                matcher.getClasses(Pattern.parseClassPattern("com.example*.*^")));
    }

    @Test
    void testGetMethodFromPattern() {
        ClassHierarchy classHierarchy = World.get().getClassHierarchy();
        Matcher matcher = new Matcher(classHierarchy);
        JMethod x_foo_str = classHierarchy.getMethod("<com.example.X: void foo(java.lang.String)>");
        JMethod x_foo_int = classHierarchy.getMethod("<com.example.X: void foo(int)>");
        JMethod XFather_foo = classHierarchy.getMethod("<com.example.XFather: void foo()>");
        JMethod XFather_foo_str = classHierarchy.getMethod("<com.example.XFather: void foo(java.lang.String)>");
        JMethod XFather_foo_int = classHierarchy.getMethod("<com.example.XFather: void foo(int)>");
        JMethod XFather_foo_str_and_int = classHierarchy.getMethod("<com.example.XFather: void foo(java.lang.String,int)>");
        JMethod y_fun_X = classHierarchy.getMethod("<com.example.Y: void fun(com.example.X)>");
        JMethod y_fun_X1 = classHierarchy.getMethod("<com.example.Y: void fun(com.example.X1)>");
        JMethod y_fun_XFather = classHierarchy.getMethod("<com.example.Y: void fun(com.example.XFather)>");
        JMethod y_foo_X = classHierarchy.getMethod("<com.example.Y: com.example.X foo(com.example.X)>");
        JMethod y_foo_X1 = classHierarchy.getMethod("<com.example.Y: com.example.X1 foo(com.example.X1)>");
        JMethod y_foo_XFather = classHierarchy.getMethod("<com.example.Y: com.example.XFather foo(com.example.XFather)>");

        assertEquals(Set.of(x_foo_str, x_foo_int), matcher.getMethods(Pattern.parseMethodPattern("<com.example.X: void foo(*{1+})>")));
        assertEquals(Set.of(XFather_foo, XFather_foo_str, XFather_foo_int, XFather_foo_str_and_int), matcher.getMethods(Pattern.parseMethodPattern("<com.example.XFather: void foo(*{0+})>")));
        assertEquals(Set.of(XFather_foo_str, XFather_foo_str_and_int), matcher.getMethods(Pattern.parseMethodPattern("<com.example.XFather: void foo(java.lang.String,*{0+})>")));
        assertEquals(Set.of(XFather_foo_str_and_int), matcher.getMethods(Pattern.parseMethodPattern("<com.example.XFather: void foo(java.lang.String,*{1},*{0+})>")));
        assertEquals(Set.of(XFather_foo_int, XFather_foo_str, XFather_foo_str_and_int), matcher.getMethods(Pattern.parseMethodPattern("<com.example.XFather: void foo(*{1-2})>")));
        assertEquals(Set.of(x_foo_int, XFather_foo_int), matcher.getMethods(Pattern.parseMethodPattern("<com.example.X*: void foo(int)>")));
        assertEquals(Set.of(x_foo_str, XFather_foo_str), matcher.getMethods(Pattern.parseMethodPattern("<com.example.XFather^: void foo(java.lang.String)>")));
        assertEquals(Set.of(y_fun_X, y_fun_X1, y_fun_XFather), matcher.getMethods(Pattern.parseMethodPattern("<com.example.Y: void fun(com.example.*)>")));
        assertEquals(Set.of(y_foo_X, y_foo_X1, y_foo_XFather), matcher.getMethods(Pattern.parseMethodPattern("<com.example.Y: com.example.XFather^ foo(*{1+})>")));
        assertEquals(Set.of(x_foo_str, x_foo_int, XFather_foo, XFather_foo_int, XFather_foo_int, XFather_foo_str_and_int, y_foo_X, y_foo_X1, y_foo_XFather), matcher.getMethods(Pattern.parseMethodPattern("<*: * foo(*{0+})>")));
    }


    @Test
    void testGetFieldFromPattern() {
        ClassHierarchy classHierarchy = World.get().getClassHierarchy();
        Matcher matcher = new Matcher(classHierarchy);
        JField x_XFather = classHierarchy.getField("<com.example.X: com.example.XFather xFatherField>");
        JField x_x1 = classHierarchy.getField("<com.example.X: com.example.X1 x1Field>");
        JField x_e1_x1 = classHierarchy.getField("<com.example.X: com.example1.X1 x11Field>");
        JField x_e1_x = classHierarchy.getField("<com.example.X: com.example1.X xxField>");
        JField y_XFather = classHierarchy.getField("<com.example.Y: com.example.XFather xFatherField>");
        JField y_y = classHierarchy.getField("<com.example.Y: com.example.Y yField>");
        JField y_x1 = classHierarchy.getField("<com.example.Y: com.example.X1 x1Field>");
        JField y_e1_x1 = classHierarchy.getField("<com.example.Y: com.example1.X1 x11Field>");
        JField y_e1_x = classHierarchy.getField("<com.example.Y: com.example1.X xxField>");
        JField XFather_XFather = classHierarchy.getField("<com.example.XFather: com.example.XFather xFatherField>");
        JField XFather_x1 = classHierarchy.getField("<com.example.XFather: com.example.X1 x1Field>");
        JField XFather_e1_x1 = classHierarchy.getField("<com.example.XFather: com.example1.X1 x11Field>");
        JField XFather_e1_x = classHierarchy.getField("<com.example.XFather: com.example1.X xxField>");
        JField e1_x1_x_x1 = classHierarchy.getField("<com.example1.X1: com.example1.X1 x1Field>");
        JField e1_x1_x_x = classHierarchy.getField("<com.example1.X1: com.example1.X xField>");

        assertEquals(Set.of(x_XFather, XFather_XFather), matcher.getFields(Pattern.parseFieldPattern("<com.example.XFather^: com.example.XFather xFatherField>")));
        assertEquals(Set.of(x_XFather, XFather_XFather, y_XFather), matcher.getFields(Pattern.parseFieldPattern("<*: com.example.XFather xFatherField>")));
        assertEquals(Set.of(x_x1, y_x1, XFather_x1), matcher.getFields(Pattern.parseFieldPattern("<*: com.example.X1 x1Field>")));
        assertEquals(Set.of(x_x1, x_e1_x1, y_x1, y_e1_x1, XFather_x1, XFather_e1_x1, e1_x1_x_x1), matcher.getFields(Pattern.parseFieldPattern("<*: *X1 *>")));
        assertEquals(Set.of(x_e1_x, y_e1_x, XFather_e1_x, e1_x1_x_x), matcher.getFields(Pattern.parseFieldPattern("<*: *X *>")));
        assertEquals(Set.of(x_XFather, x_x1, y_XFather, y_x1, XFather_XFather, XFather_x1), matcher.getFields(Pattern.parseFieldPattern("<*: com.example.XFather^ *>")));
        assertEquals(Set.of(y_y), matcher.getFields(Pattern.parseFieldPattern("<*: * yField>")));
        assertEquals(Set.of(x_e1_x, y_e1_x, XFather_e1_x), matcher.getFields(Pattern.parseFieldPattern("<*: * xx*>")));
        assertEquals(Set.of(e1_x1_x_x1, e1_x1_x_x), matcher.getFields(Pattern.parseFieldPattern("<com.example1.X1: * *>")));
    }
}

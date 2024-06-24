package pascal.taie.language.classes;

import org.junit.jupiter.api.Test;
import pascal.taie.Main;
import pascal.taie.World;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MatcherTest {

    private static final String CLASS_PATH = "src/test/resources/sigmatcher/";

    private static final String MAIN_CLASS = "com.example.X";

    static void buildWorld() {
        Main.buildWorld("-cp", CLASS_PATH, "-m", MAIN_CLASS);
    }

    @Test
    void testGetClassFromPattern() {
        buildWorld();
        ClassHierarchy classHierarchy = World.get().getClassHierarchy();
        Matcher matcher = new Matcher(classHierarchy);
        JClass e_x = classHierarchy.getClass("com.example.X");
        JClass e_x1 = classHierarchy.getClass("com.example.X1");
        JClass y = classHierarchy.getClass("com.example.Y");
        JClass XFather = classHierarchy.getClass("com.example.XFather");
        JClass e1_x = classHierarchy.getClass("com.example1.X");
        JClass e1_x1 = classHierarchy.getClass("com.example1.X1");
        assertEquals(Set.of(e_x, e1_x), matcher.getClasses(Pattern.parseClassPattern("com**X")));
        assertEquals(Set.of(e_x, y, XFather, e_x1), matcher.getClasses(Pattern.parseClassPattern("com.example.*")));
        assertEquals(Set.of(e1_x, e1_x1), matcher.getClasses(Pattern.parseClassPattern("com.example1.*")));
        assertEquals(Set.of(e_x1, e1_x1), matcher.getClasses(Pattern.parseClassPattern("com.*.X1")));
    }

    @Test
    void testGetMethodFromPattern() {
        buildWorld();
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

        assertEquals(Set.of(x_foo_str, x_foo_int), matcher.getMethods(Pattern.parseMethodPattern("<com.example.X: void foo(~)>")));
        assertEquals(Set.of(XFather_foo_str, XFather_foo_int,XFather_foo_str_and_int), matcher.getMethods(Pattern.parseMethodPattern("<com.example.XFather: void foo(~)>")));
        assertEquals(Set.of(XFather_foo_str_and_int), matcher.getMethods(Pattern.parseMethodPattern("<com.example.XFather: void foo(java.lang.String,~)>")));
        assertEquals(Set.of(x_foo_int, XFather_foo_int), matcher.getMethods(Pattern.parseMethodPattern("<com.example.X*: void foo(int)>")));
        assertEquals(Set.of(x_foo_str, XFather_foo_str), matcher.getMethods(Pattern.parseMethodPattern("<com.example.XFather^: void foo(java.lang.String)>")));
        assertEquals(Set.of(y_fun_X, y_fun_X1,y_fun_XFather), matcher.getMethods(Pattern.parseMethodPattern("<com.example.Y: void fun(com.example.*)>")));
        assertEquals(Set.of(y_foo_X, y_foo_X1,y_foo_XFather), matcher.getMethods(Pattern.parseMethodPattern("<com.example.Y: com.example.XFather^ foo(~)>")));
    }


    @Test
    void testGetFieldFromPattern() {

    }
}

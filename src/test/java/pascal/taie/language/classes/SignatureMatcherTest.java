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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pascal.taie.Main;
import pascal.taie.World;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SignatureMatcherTest {

    private static final String CLASS_PATH = "src/test/resources/sigmatcher";

    private static final String MAIN_CLASS = "com.example.X";

    private static ClassHierarchy hierarchy;

    private static SignatureMatcher matcher;

    @BeforeAll
    public static void buildWorld() {
        Main.buildWorld("-cp", CLASS_PATH, "-m", MAIN_CLASS);
        hierarchy = World.get().getClassHierarchy();
        matcher = new SignatureMatcher(hierarchy);
    }

    @Test
    // CHECKSTYLE:OFF
    void testGetClasses() {
        JClass e_x = hierarchy.getClass("com.example.X");
        JClass e_x1 = hierarchy.getClass("com.example.X1");
        JClass y = hierarchy.getClass("com.example.Y");
        JClass XFather = hierarchy.getClass("com.example.XFather");
        JClass e1_x = hierarchy.getClass("com.example1.X");
        JClass e1_x1 = hierarchy.getClass("com.example1.X1");

        assertEquals(Set.of(e_x, e1_x),
                matcher.getClasses("com*X"));
        assertEquals(Set.of(e_x, y, XFather, e_x1),
                matcher.getClasses("com.example.*"));
        assertEquals(Set.of(e1_x, e1_x1),
                matcher.getClasses("com.example1.*"));
        assertEquals(Set.of(e_x1, e1_x1),
                matcher.getClasses("com.*.X1"));
        assertEquals(Set.of(e_x, e_x1, y, XFather, e1_x, e1_x1),
                matcher.getClasses("*example*"));
        assertEquals(Set.of(XFather),
                matcher.getClasses("*.XFather"));
        assertEquals(Set.of(e_x, e_x1, y, XFather, e1_x, e1_x1),
                matcher.getClasses("*")
                        .stream()
                        .filter(JClass::isApplication)
                        .collect(Collectors.toSet()));
        assertEquals(Set.of(XFather, e_x, e_x1),
                matcher.getClasses("com.example.XFather^"));
        assertEquals(Set.of(e_x, e_x1, y, XFather, e1_x, e1_x1),
                matcher.getClasses("com.example*.*^"));
    }

    @Test
    void testGetMethods() {
        JMethod x_foo_str = hierarchy.getMethod("<com.example.X: void foo(java.lang.String)>");
        JMethod x_foo_int = hierarchy.getMethod("<com.example.X: void foo(int)>");
        JMethod XFather_foo = hierarchy.getMethod("<com.example.XFather: void foo()>");
        JMethod XFather_foo_str = hierarchy.getMethod("<com.example.XFather: void foo(java.lang.String)>");
        JMethod XFather_foo_int = hierarchy.getMethod("<com.example.XFather: void foo(int)>");
        JMethod XFather_foo_str_and_int = hierarchy.getMethod("<com.example.XFather: void foo(java.lang.String,int)>");
        JMethod y_fun_X = hierarchy.getMethod("<com.example.Y: void fun(com.example.X)>");
        JMethod y_fun_X1 = hierarchy.getMethod("<com.example.Y: void fun(com.example.X1)>");
        JMethod y_fun_XFather = hierarchy.getMethod("<com.example.Y: void fun(com.example.XFather)>");
        JMethod y_foo_X = hierarchy.getMethod("<com.example.Y: com.example.X foo(com.example.X)>");
        JMethod y_foo_X1 = hierarchy.getMethod("<com.example.Y: com.example.X1 foo(com.example.X1)>");
        JMethod y_foo_XFather = hierarchy.getMethod("<com.example.Y: com.example.XFather foo(com.example.XFather)>");

        assertEquals(Set.of(x_foo_str, x_foo_int),
                matcher.getMethods("<com.example.X: void foo(*{1+})>"));
        assertEquals(Set.of(XFather_foo, XFather_foo_str, XFather_foo_int, XFather_foo_str_and_int),
                matcher.getMethods("<com.example.XFather: void foo(*{0+})>"));
        assertEquals(Set.of(XFather_foo_str, XFather_foo_str_and_int),
                matcher.getMethods("<com.example.XFather: void foo(java.lang.String,*{0+})>"));
        assertEquals(Set.of(XFather_foo_str_and_int),
                matcher.getMethods("<com.example.XFather: void foo(java.lang.String,*{1},*{0+})>"));
        assertEquals(Set.of(XFather_foo_int, XFather_foo_str, XFather_foo_str_and_int),
                matcher.getMethods("<com.example.XFather: void foo(*{1-2})>"));
        assertEquals(Set.of(x_foo_int, XFather_foo_int),
                matcher.getMethods("<com.example.X*: void foo(int)>"));
        assertEquals(Set.of(x_foo_str, XFather_foo_str),
                matcher.getMethods("<com.example.XFather^: void foo(java.lang.String)>"));
        assertEquals(Set.of(y_fun_X, y_fun_X1, y_fun_XFather),
                matcher.getMethods("<com.example.Y: void fun(com.example.*)>"));
        assertEquals(Set.of(y_fun_X, y_fun_X1, y_fun_XFather),
                matcher.getMethods("<com.example.Y: void fun(com.example.XFather^{1+})>"));
        assertEquals(Set.of(y_foo_X, y_foo_X1, y_foo_XFather),
                matcher.getMethods("<com.example.Y: com.example.XFather^ foo(*{1+})>"));
        assertEquals(Set.of(XFather_foo, XFather_foo_str, XFather_foo_int, XFather_foo_str_and_int,
                        x_foo_str, x_foo_int,
                        y_foo_X, y_foo_X1, y_foo_XFather),
                matcher.getMethods("<*: * foo(*{0+})>"));
    }

    @Test
    void testGetMethodFromPatternOfMultiParam() {
        JMethod XFather_multi1 = hierarchy.getMethod("<com.example.XFather: void multi(java.lang.String,int,int)>");
        JMethod XFather_multi2 = hierarchy.getMethod("<com.example.XFather: void multi(java.lang.String,int,char)>");
        JMethod XFather_multi3 = hierarchy.getMethod("<com.example.XFather: void multi(java.lang.String,int,char,int)>");
        JMethod XFather_multi4 = hierarchy.getMethod("<com.example.XFather: void multi(java.lang.String,int,char,int,byte)>");

        assertEquals(Set.of(XFather_multi1, XFather_multi3),
                matcher.getMethods("<com.example.XFather: void multi(java.lang.String,*{0+},int,*{0+},int)>"));
        assertEquals(Set.of(XFather_multi3),
                matcher.getMethods("<com.example.XFather: void multi(java.lang.String,*{0+},int,*{1+},int)>"));
        assertEquals(Set.of(XFather_multi2, XFather_multi3, XFather_multi4),
                matcher.getMethods("<com.example.XFather: void multi(java.lang.String,*{0+},int,char,*{0+})>"));
        assertEquals(Set.of(XFather_multi4),
                matcher.getMethods("<com.example.XFather: void multi(java.lang.String,*{0+},int,byte,*{0+})>"));
        assertEquals(Set.of(XFather_multi3, XFather_multi4),
                matcher.getMethods("<com.example.XFather: void multi(java.lang.String,*{0+},int,char,*{1+})>"));
        assertEquals(Set.of(XFather_multi1),
                matcher.getMethods("<com.example.XFather: void multi(java.lang.String,int{2+})>"));
    }

    @Test
    void testGetFields() {
        JField x_XFather = hierarchy.getField("<com.example.X: com.example.XFather xFatherField>");
        JField x_x1 = hierarchy.getField("<com.example.X: com.example.X1 x1Field>");
        JField x_e1_x1 = hierarchy.getField("<com.example.X: com.example1.X1 x11Field>");
        JField x_e1_x = hierarchy.getField("<com.example.X: com.example1.X xxField>");
        JField y_XFather = hierarchy.getField("<com.example.Y: com.example.XFather xFatherField>");
        JField y_y = hierarchy.getField("<com.example.Y: com.example.Y yField>");
        JField y_x1 = hierarchy.getField("<com.example.Y: com.example.X1 x1Field>");
        JField y_e1_x1 = hierarchy.getField("<com.example.Y: com.example1.X1 x11Field>");
        JField y_e1_x = hierarchy.getField("<com.example.Y: com.example1.X xxField>");
        JField XFather_XFather = hierarchy.getField("<com.example.XFather: com.example.XFather xFatherField>");
        JField XFather_x1 = hierarchy.getField("<com.example.XFather: com.example.X1 x1Field>");
        JField XFather_e1_x1 = hierarchy.getField("<com.example.XFather: com.example1.X1 x11Field>");
        JField XFather_e1_x = hierarchy.getField("<com.example.XFather: com.example1.X xxField>");
        JField e1_x1_x_x1 = hierarchy.getField("<com.example1.X1: com.example1.X1 x1Field>");
        JField e1_x1_x_x = hierarchy.getField("<com.example1.X1: com.example1.X xField>");

        assertEquals(Set.of(x_XFather, XFather_XFather),
                matcher.getFields("<com.example.XFather^: com.example.XFather xFatherField>"));
        assertEquals(Set.of(x_XFather, XFather_XFather, y_XFather),
                matcher.getFields("<*: com.example.XFather xFatherField>"));
        assertEquals(Set.of(x_x1, y_x1, XFather_x1),
                matcher.getFields("<*: com.example.X1 x1Field>"));
        assertEquals(Set.of(x_x1, x_e1_x1, y_x1, y_e1_x1, XFather_x1, XFather_e1_x1, e1_x1_x_x1),
                matcher.getFields("<*: *X1 *>"));
        assertEquals(Set.of(x_e1_x, y_e1_x, XFather_e1_x, e1_x1_x_x),
                matcher.getFields("<*: *X *>"));
        assertEquals(Set.of(x_XFather, x_x1, y_XFather, y_x1, XFather_XFather, XFather_x1),
                matcher.getFields("<*: com.example.XFather^ *>"));
        assertEquals(Set.of(y_y),
                matcher.getFields("<*: * yField>"));
        assertEquals(Set.of(x_e1_x, y_e1_x, XFather_e1_x),
                matcher.getFields("<*: * xx*>"));
        assertEquals(Set.of(e1_x1_x_x1, e1_x1_x_x),
                matcher.getFields("<com.example1.X1: * *>"));
    }
}

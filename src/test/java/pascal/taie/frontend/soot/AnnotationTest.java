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

package pascal.taie.frontend.soot;

import org.junit.jupiter.api.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.language.classes.JClass;

public class AnnotationTest {

    private static void buildWorld(String main) {
        Main.buildWorld("-pp", "-cp", "src/test/resources/world", "--input-classes", main);
    }

    @Test
    void testAnnotation() {
        buildWorld("Annotated");
        JClass main = World.get().getClassHierarchy().getClass("Annotated");
        AnnotationPrinter.print(main);
    }

    @Test
    void testAnnotationJava() {
        buildWorld("AnnotatedJava");
        JClass main = World.get().getClassHierarchy().getClass("AnnotatedJava");
        AnnotationPrinter.print(main);
    }
}

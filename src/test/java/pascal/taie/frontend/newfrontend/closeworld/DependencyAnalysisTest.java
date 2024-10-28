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

package pascal.taie.frontend.newfrontend.closeworld;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.language.classes.JClass;

public class DependencyAnalysisTest {
    @Test
    public void testAnnotation() {
        Main.buildWorld("-java", "17",
                "-cp", "src/test/resources/frontend/newfrontend/closeworld/my-annotation-demo-1.0-SNAPSHOT.jar",
                "-m", "com.example.Main"
                );
        JClass annotatedClass = World.get().getClassHierarchy().getClass("com.example.AnnotatedClass");
        Assertions.assertNotNull(annotatedClass);

        JClass myAnnotation = World.get().getClassHierarchy().getClass("com.example.MyAnnotation");
        Assertions.assertNotNull(myAnnotation);

        JClass classValue = World.get().getClassHierarchy().getClass("com.example.ClassValue");
        Assertions.assertNotNull(classValue);
    }
}

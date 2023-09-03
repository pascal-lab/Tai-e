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

package pascal.taie.analysis.pta;

import org.junit.jupiter.api.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.CallGraphBuilder;
import pascal.taie.ir.IR;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Java9StringConcatTest {

    @Test
    void test() {
        Main.main("-pp",
                "-cp", "src/test/resources/pta/invokedynamic",
                "-m", "Java9StringConcatenation",
                "-a", "pta=implicit-entries:false",
                "-a", "cg");
        ClassHierarchy hierarchy = World.get().getClassHierarchy();
        JClass main = hierarchy.getClass("Java9StringConcatenation");
        assert main != null;
        // check the return value of string concatenation
        PointerAnalysisResult ptaResult = World.get().getResult(PointerAnalysis.ID);
        ClassType string = World.get().getTypeSystem().getClassType(ClassNames.STRING);
        main.getDeclaredMethods()
                .stream()
                .filter(m -> m.getReturnType().equals(string))
                .map(JMethod::getIR)
                .map(IR::getReturnVars)
                .flatMap(Collection::stream)
                .forEach(retVar -> assertFalse(ptaResult.getPointsToSet(retVar).isEmpty()));
        // check the reachability to toString() methods
        CallGraph<Invoke, JMethod> cg = World.get().getResult(CallGraphBuilder.ID);
        JMethod myClassToString = hierarchy.getMethod(
                "<Java9StringConcatenation$MyClass: java.lang.String toString()>");
        JMethod myClassNullToString = hierarchy.getMethod(
                "<Java9StringConcatenation$MyClassNull: java.lang.String toString()>");
        assertTrue(cg.contains(myClassToString));
        assertTrue(cg.contains(myClassNullToString));
    }

}

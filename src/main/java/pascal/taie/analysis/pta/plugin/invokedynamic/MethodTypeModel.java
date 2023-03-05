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

package pascal.taie.analysis.pta.plugin.invokedynamic;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.AbstractModel;
import pascal.taie.analysis.pta.plugin.util.CSObjs;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.MethodType;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

import java.util.List;

/**
 * Models invocations to MethodType.methodType(*);
 */
class MethodTypeModel extends AbstractModel {

    MethodTypeModel(Solver solver) {
        super(solver);
    }

    @Override
    protected void registerVarAndHandler() {
        JMethod mt1Class = hierarchy.getJREMethod("<java.lang.invoke.MethodType: java.lang.invoke.MethodType methodType(java.lang.Class)>");
        registerRelevantVarIndexes(mt1Class, 0);
        registerAPIHandler(mt1Class, this::methodType1Class);

        JMethod mt2Classes = hierarchy.getJREMethod("<java.lang.invoke.MethodType: java.lang.invoke.MethodType methodType(java.lang.Class,java.lang.Class)>");
        registerRelevantVarIndexes(mt2Classes, 0, 1);
        registerAPIHandler(mt2Classes, this::methodType2Classes);

        JMethod mtClassMT = hierarchy.getJREMethod("<java.lang.invoke.MethodType: java.lang.invoke.MethodType methodType(java.lang.Class,java.lang.invoke.MethodType)>");
        registerRelevantVarIndexes(mtClassMT, 0, 1);
        registerAPIHandler(mtClassMT, this::methodTypeClassMT);
    }

    /**
     * Handles MethodType.methodType(java.lang.Class)
     */
    private void methodType1Class(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            Context context = csVar.getContext();
            pts.forEach(obj -> {
                Type retType = CSObjs.toType(obj);
                if (retType != null) {
                    MethodType mt = MethodType.get(List.of(), retType);
                    Obj mtObj = heapModel.getConstantObj(mt);
                    solver.addVarPointsTo(context, result, mtObj);
                }
            });
        }
    }

    /**
     * Handles MethodType.methodType(java.lang.Class,java.lang.Class)
     */
    private void methodType2Classes(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            List<PointsToSet> args = getArgs(csVar, pts, invoke, 0, 1);
            PointsToSet retObjs = args.get(0);
            PointsToSet paramObjs = args.get(1);
            Context context = csVar.getContext();
            retObjs.forEach(retObj -> {
                Type retType = CSObjs.toType(retObj);
                if (retType != null) {
                    paramObjs.forEach(paramObj -> {
                        Type paramType = CSObjs.toType(paramObj);
                        if (paramType != null) {
                            MethodType mt = MethodType.get(List.of(paramType), retType);
                            Obj mtObj = heapModel.getConstantObj(mt);
                            solver.addVarPointsTo(context, result, mtObj);
                        }
                    });
                }
            });
        }
    }

    /**
     * Handles MethodType.methodType(java.lang.Class,java.lang.invoke.MethodType)
     */
    private void methodTypeClassMT(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            List<PointsToSet> args = getArgs(csVar, pts, invoke, 0, 1);
            PointsToSet retObjs = args.get(0);
            PointsToSet mtObjs = args.get(1);
            Context context = csVar.getContext();
            retObjs.forEach(retObj -> {
                Type retType = CSObjs.toType(retObj);
                if (retType != null) {
                    mtObjs.forEach(mtObj -> {
                        MethodType mt = CSObjs.toMethodType(mtObj);
                        if (mt != null) {
                            MethodType resultMT = MethodType.get(mt.getParamTypes(), retType);
                            Obj resultMTObj = heapModel.getConstantObj(resultMT);
                            solver.addVarPointsTo(context, result, resultMTObj);
                        }
                    });
                }
            });
        }
    }
}

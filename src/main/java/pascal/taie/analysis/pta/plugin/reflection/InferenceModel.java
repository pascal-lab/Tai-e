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

package pascal.taie.analysis.pta.plugin.reflection;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.AnalysisModelPlugin;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Reflections;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Base class for reflection inference.
 * This class provides methods to handle class- and member-retrieving APIs for
 * the cases where the both the name and the receiver class object are known.
 * TODO: take ClassLoader.loadClass(...) into account.
 */
abstract class InferenceModel extends AnalysisModelPlugin {

    protected final MetaObjHelper helper;

    protected final Set<Invoke> invokesWithLog;

    private final MultiMap<Invoke, JClass> forNameTargets = Maps.newMultiMap();

    InferenceModel(Solver solver, MetaObjHelper helper, Set<Invoke> invokesWithLog) {
        super(solver);
        this.helper = helper;
        this.invokesWithLog = invokesWithLog;
    }

    static InferenceModel getDummy(Solver solver) {
        return new InferenceModel(solver, null, null) {};
    }

    protected void classForNameKnown(
            Context context, Invoke forName, @Nullable String className) {
        if (className != null) {
            JClass clazz = hierarchy.getClass(className);
            if (clazz != null) {
                solver.initializeClass(clazz);
                Var result = forName.getResult();
                if (result != null) {
                    Obj classObj = helper.getMetaObj(clazz);
                    solver.addVarPointsTo(context, result, classObj);
                    forNameTargets.put(forName, clazz);
                }
            }
        }
    }

    MultiMap<Invoke, JClass> getForNameTargets() {
        return forNameTargets;
    }

    protected void classGetConstructorKnown(
            Context context, Invoke invoke, @Nullable JClass clazz) {
        if (clazz != null) {
            Var result = invoke.getResult();
            if (result != null) {
                Stream<JMethod> constructors = switch (invoke.getMethodRef().getName()) {
                    case "getConstructor" -> Reflections.getConstructors(clazz);
                    case "getDeclaredConstructor" -> Reflections.getDeclaredConstructors(clazz);
                    default -> throw new AnalysisException(
                            "Expected [getConstructor, getDeclaredConstructor], given " +
                                    invoke.getMethodRef());
                };
                constructors.map(helper::getMetaObj)
                        .forEach(ctorObj -> solver.addVarPointsTo(context, result, ctorObj));
            }
        }
    }

    protected void classGetMethodKnown(Context context, Invoke invoke,
                                       @Nullable JClass clazz, @Nullable String name) {
        if (clazz != null && name != null) {
            Var result = invoke.getResult();
            if (result != null) {
                Stream<JMethod> methods = switch (invoke.getMethodRef().getName()) {
                    case "getMethod" -> Reflections.getMethods(clazz, name);
                    case "getDeclaredMethod" -> Reflections.getDeclaredMethods(clazz, name);
                    default -> throw new AnalysisException(
                            "Expected [getMethod, getDeclaredMethod], given " +
                                    invoke.getMethodRef());
                };
                methods.map(helper::getMetaObj)
                        .forEach(mtdObj -> solver.addVarPointsTo(context, result, mtdObj));
            }
        }
    }
}

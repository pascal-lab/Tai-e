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

package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.List;
import java.util.Map;

/**
 * Handles sources in taint analysis.
 */
class SourceHandler {

    private final Solver solver;

    private final TaintManager manager;

    /**
     * Map from a source method to its result sources.
     */
    private final MultiMap<JMethod, CallSource> callSources = Maps.newMultiMap();

    /**
     * Map from a source method to its parameter sources.
     */
    private final MultiMap<JMethod, ParamSource> paramSources = Maps.newMultiMap();

    /**
     * Whether this handler needs to handle field sources.
     */
    private final boolean handleFieldSources;

    /**
     * Map from a source field taint objects generated from it.
     */
    private final Map<JField, Type> fieldSources = Maps.newMap();

    /**
     * Maps from a method to {@link LoadField} statements in the method
     * which loads a source field.
     */
    private final MultiMap<JMethod, LoadField> loadedFieldSources = Maps.newMultiMap();

    SourceHandler(Solver solver, TaintManager manager, List<Source> sources) {
        this.solver = solver;
        this.manager = manager;
        sources.forEach(src -> {
            if (src instanceof CallSource callSrc) {
                callSources.put(callSrc.method(), callSrc);
            } else if (src instanceof ParamSource paramSrc) {
                paramSources.put(paramSrc.method(), paramSrc);
            } else if (src instanceof FieldSource fieldSrc) {
                fieldSources.put(fieldSrc.field(), fieldSrc.type());
            }
        });
        handleFieldSources = !fieldSources.isEmpty();
    }

    void handleCallSource(Edge<CSCallSite, CSMethod> edge) {
        Invoke callSite = edge.getCallSite().getCallSite();
        JMethod callee = edge.getCallee().getMethod();
        // generate taint value from source call
        callSources.get(callee).forEach(source -> {
            int index = source.index();
            if (InvokeUtils.RESULT == index && callSite.getLValue() == null ||
                    InvokeUtils.RESULT != index && edge.getKind() == CallKind.OTHER) {
                return;
            }
            Var var = InvokeUtils.getVar(callSite, index);
            SourcePoint sourcePoint = new CallSourcePoint(callSite, index);
            Obj taint = manager.makeTaint(sourcePoint, source.type());
            solver.addVarPointsTo(edge.getCallSite().getContext(), var, taint);
        });
    }

    void handleParamSource(CSMethod csMethod) {
        JMethod method = csMethod.getMethod();
        if (paramSources.containsKey(method)) {
            Context context = csMethod.getContext();
            IR ir = method.getIR();
            paramSources.get(method).forEach(source -> {
                int index = source.index();
                Var param = ir.getParam(index);
                SourcePoint sourcePoint = new ParamSourcePoint(method, index);
                Type type = source.type();
                Obj taint = manager.makeTaint(sourcePoint, type);
                solver.addVarPointsTo(context, param, taint);
            });
        }
    }

    /**
     * Scans {@code method}'s IR to check if it loads any source fields.
     * If so, records the {@link LoadField} statements.
     */
    void handleFieldSource(JMethod method) {
        if (handleFieldSources) {
            method.getIR().forEach(stmt -> {
                if (stmt instanceof LoadField loadField) {
                    JField field = loadField.getFieldRef().resolveNullable();
                    if (fieldSources.containsKey(field)) {
                        loadedFieldSources.put(method, loadField);
                    }
                }
            });
        }
    }

    /**
     * If given method contains pre-recorded {@link LoadField} statements,
     * adds corresponding taint object to LHS of the {@link LoadField}.
     */
    void handleFieldSource(CSMethod csMethod) {
        if (handleFieldSources) {
            JMethod method = csMethod.getMethod();
            Context context = csMethod.getContext();
            loadedFieldSources.get(method).forEach(load -> {
                Var lhs = load.getLValue();
                SourcePoint sourcePoint = new FieldSourcePoint(method, load);
                JField field = load.getFieldRef().resolve();
                Type type = fieldSources.get(field);
                Obj taint = manager.makeTaint(sourcePoint, type);
                solver.addVarPointsTo(context, lhs, taint);
            });
        }
    }
}

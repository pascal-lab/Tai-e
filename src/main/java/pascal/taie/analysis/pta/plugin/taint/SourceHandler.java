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

import java.util.Map;
import java.util.Set;

/**
 * Handles sources in taint analysis.
 */
class SourceHandler extends OnFlyHandler {

    /**
     * Map from a source method to its result sources.
     */
    private final MultiMap<JMethod, CallSource> callSources = Maps.newMultiMap();

    /**
     * Map from a method to {@link Invoke} statements in the method
     * which matches any call source.
     * This map matters only when call-site mode is enabled.
     */
    private final MultiMap<JMethod, Invoke> callSiteSources = Maps.newMultiMap();

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

    SourceHandler(HandlerContext context) {
        super(context);
        context.config().sources().forEach(src -> {
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

    /**
     * Generates taint objects from call sources.
     */
    private void processCallSource(Context context, Invoke callSite, CallSource source) {
        int index = source.index();
        if (InvokeUtils.RESULT == index && callSite.getLValue() == null) {
            return;
        }
        Var var = InvokeUtils.getVar(callSite, index);
        SourcePoint sourcePoint = new CallSourcePoint(callSite, index);
        Obj taint = manager.makeTaint(sourcePoint, source.type());
        solver.addVarPointsTo(context, var, taint);
    }

    /**
     * Handles call sources.
     */
    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        if (edge.getKind() == CallKind.OTHER) {
            return;
        }
        Set<CallSource> sources = callSources.get(edge.getCallee().getMethod());
        if (!sources.isEmpty()) {
            Context context = edge.getCallSite().getContext();
            Invoke callSite = edge.getCallSite().getCallSite();
            sources.forEach(source -> processCallSource(context, callSite, source));
        }

    }

    @Override
    public void onNewMethod(JMethod method) {
        if (handleFieldSources) {
            handleFieldSource(method);
        }
        if (callSiteMode) {
            handleCallSource(method);
        }
    }

    /**
     * Handles field sources.
     * Scans {@code method}'s IR to check if it loads any source fields.
     * If so, records the {@link LoadField} statements.
     */
    private void handleFieldSource(JMethod method) {
        method.getIR().forEach(stmt -> {
            if (stmt instanceof LoadField loadField) {
                JField field = loadField.getFieldRef().resolveNullable();
                if (fieldSources.containsKey(field)) {
                    loadedFieldSources.put(method, loadField);
                }
            }
        });
    }

    /**
     * Handles call sources for the case when call-site mode is enabled.
     * Scans {@code method}'s IR to check if method references of any
     * {@link Invoke}s are resolved to call source method.
     * If so, records the {@link Invoke} statements.
     */
    private void handleCallSource(JMethod method) {
        method.getIR().invokes(false).forEach(callSite -> {
            JMethod callee = callSite.getMethodRef().resolveNullable();
            if (callSources.containsKey(callee)) {
                callSiteSources.put(method, callSite);
            }
        });
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        handleParamSource(csMethod);
        if (handleFieldSources) {
            handleFieldSource(csMethod);
        }
        if (callSiteMode) {
            handleCallSource(csMethod);
        }
    }

    private void handleParamSource(CSMethod csMethod) {
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
     * If given method contains pre-recorded {@link LoadField} statements,
     * adds corresponding taint object to LHS of the {@link LoadField}.
     */
    private void handleFieldSource(CSMethod csMethod) {
        JMethod method = csMethod.getMethod();
        Set<LoadField> loads = loadedFieldSources.get(method);
        if (!loads.isEmpty()) {
            Context context = csMethod.getContext();
            loads.forEach(load -> {
                Var lhs = load.getLValue();
                SourcePoint sourcePoint = new FieldSourcePoint(method, load);
                JField field = load.getFieldRef().resolve();
                Type type = fieldSources.get(field);
                Obj taint = manager.makeTaint(sourcePoint, type);
                solver.addVarPointsTo(context, lhs, taint);
            });
        }
    }

    /**
     * If given method contains pre-recorded {@link Invoke} statements,
     * call {@link #processCallSource} to generate taint objects.
     */
    private void handleCallSource(CSMethod csMethod) {
        JMethod method = csMethod.getMethod();
        Set<Invoke> callSites = callSiteSources.get(method);
        if (!callSites.isEmpty()) {
            Context context = csMethod.getContext();
            callSites.forEach(callSite -> {
                JMethod callee = callSite.getMethodRef().resolve();
                callSources.get(callee).forEach(source ->
                        processCallSource(context, callSite, source));
            });
        }
    }
}

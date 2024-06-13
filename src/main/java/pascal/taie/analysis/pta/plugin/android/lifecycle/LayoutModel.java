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

package pascal.taie.analysis.pta.plugin.android.lifecycle;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.android.AndroidTransferEdge;
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.Sets;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.BASE;
import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.RESULT;

public class LayoutModel extends LifecycleHandler {

    public LayoutModel(LifecycleContext context) {
        super(context);
    }

    @InvokeHandler(signature = {
            "<android.app.FragmentManager: android.app.Fragment findFragmentById(int)>",
            "<android.app.Activity: android.view.View findViewById(int)>"
    }, argIndexes = {BASE})
    public void findById(Context context, Invoke invoke, PointsToSet baseObj) {
        Var result = InvokeUtils.getVar(invoke, RESULT);
        if (result == null) {
            return;
        }
        Integer id = getInteger(invoke);
        String name = getString(id);

        // transfer name
        if (name == null && id != null) {
            ARSCFileParser.AbstractResource resource = handlerContext.apkInfo().findResource(id);
            if (resource != null) {
                if (resource.getResourceName().equals("button")) {
                    generateInvokeResultObj(context, invoke);
                }
                name = handlerContext.apkInfo().getPackageName() + "." + toCamelCase(resource.getResourceName());
            }
        }

        if (name != null) {
            JClass jClass = hierarchy.getClass(name);
            if (jClass != null) {
                Obj fragment = handlerContext.androidObjManager().getComponentObj(jClass);
                solver.addVarPointsTo(context, result, fragment);
            }
        }
    }

    @InvokeHandler(signature = "<android.app.Activity: void setContentView(int)>", argIndexes = {BASE})
    public void setContentView(Context context, Invoke invoke, PointsToSet pts) {
        Var base = InvokeUtils.getVar(invoke, BASE);
        if (base.getType() instanceof ClassType classType) {
            JClass decl = classType.getJClass();
            Set<String> layoutFileNames = Sets.newSet();
            String fileName = getString(getInteger(invoke));
            if (fileName != null) {
                layoutFileNames.add(fileName);
            }
            if (layoutFileNames.isEmpty()) {
                layoutFileNames.addAll(Stream.of(handlerContext.apkInfo().layoutCallbacks().keySet(),
                        handlerContext.apkInfo().layoutFragments().keySet(),
                        handlerContext.apkInfo().layoutViews().keySet()).flatMap(Set::stream).collect(Collectors.toSet()));
            }
            layoutFileNames.forEach(layoutFileName -> {
                processButton(decl, pts, layoutFileName);
                processFragmentAndView(csManager.getCSVar(context, base), layoutFileName);
            });
        }
    }

    @InvokeHandler(signature = "<android.content.Context: java.lang.String getString(int)>", argIndexes = {BASE})
    public void getString(Context context, Invoke invoke, PointsToSet pts) {
        Var result = InvokeUtils.getVar(invoke, RESULT);
        if (result == null) {
            return;
        }
        String name = getString(getInteger(invoke));
        if (name != null) {
            Obj nameObj = handlerContext.androidObjManager().getAndroidStringObj(StringLiteral.get(name), result);
            solver.addVarPointsTo(context, result, nameObj);
        }
    }

    public String getString(Integer id) {
        if (id != null) {
            ARSCFileParser.AbstractResource resource = handlerContext.apkInfo().findResource(id);
            if (resource instanceof ARSCFileParser.StringResource stringResource) {
                return stringResource.getValue();
            }
        }
        return null;
    }

    private Integer getInteger(Invoke invoke) {
        Var arg = invoke.getInvokeExp().getArg(0);
        if(arg.isConst() && arg.getConstValue() instanceof IntLiteral intLiteral) {
            return intLiteral.getValue();
        }
        return null;
    }

    private void processButton(JClass decl, PointsToSet pts, String layoutFileName) {
        handlerContext.apkInfo().layoutCallbacks().get(layoutFileName).forEach(callbackSubSig -> {
            JMethod callbackMethod = decl.getDeclaredMethod(callbackSubSig);
            if (callbackMethod != null){
                pts.forEach(thisObj ->  addEntryPoint(callbackMethod, thisObj.getObject()));
            }
        });
    }

    private void processFragmentAndView(CSVar activity, String layoutFileName) {
        Stream.concat(
                handlerContext.apkInfo().layoutFragments().entrySet().stream(),
                handlerContext.apkInfo().layoutViews().entrySet().stream())
                .collect(Collectors.toSet())
                .forEach(map -> {
                    if (map.getKey().equals(layoutFileName)) {
                        JClass c = map.getValue();
                        Obj thisObj = handlerContext.androidObjManager().getComponentObj(c);
                        handlerContext.lifecycleHelper()
                                .getLifeCycleMethods(c)
                                .forEach(em -> {
                                    addEntryPoint(em, thisObj);
                                    if (em.getSubsignature().equals(ON_ATTACH)) {
                                        CSVar paramVar = csManager.getCSVar(emptyContext, em.getIR().getParam(0));
                                        solver.addPFGEdge(new AndroidTransferEdge(activity, paramVar), paramVar.getType());
                                    }
                                });
                    }
                });
    }

    private static String toCamelCase(String input) {
        StringBuilder result = new StringBuilder();

        boolean convertNext = true;
        for (char ch : input.toCharArray()) {
            if (ch == '_') {
                convertNext = true;
            } else {
                if (convertNext) {
                    result.append(Character.toUpperCase(ch));
                    convertNext = false;
                } else {
                    result.append(ch);
                }
            }
        }
        return result.toString();
    }

}

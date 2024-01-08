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

package pascal.taie.analysis.pta.plugin.util;

import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Provides common functionalities for implementing IR-based API models.
 *
 * @deprecated Use {@link IRModelPlugin} instead.
 */
@Deprecated
public abstract class AbstractIRModel extends SolverHolder implements IRModel {

    private final MethodHandles.Lookup lookup = MethodHandles.lookup();

    protected final Map<JMethod, Function<Invoke, Collection<Stmt>>> handlers
            = Maps.newMap();

    protected final Map<JMethod, Collection<Stmt>> method2GenStmts
            = Maps.newHybridMap();

    protected AbstractIRModel(Solver solver) {
        super(solver);
        registerHandlersByAnnotation();
        registerHandlers();
    }

    protected void registerHandlersByAnnotation() {
        Class<?> clazz = getClass();
        for (Method method : clazz.getMethods()) {
            InvokeHandler[] invokeHandlers = method.getAnnotationsByType(InvokeHandler.class);
            if (invokeHandlers != null) {
                for (InvokeHandler invokeHandler : invokeHandlers) {
                    for (String signature : invokeHandler.signature()) {
                        JMethod api = hierarchy.getMethod(signature);
                        if (api != null) {
                            registerHandler(api, createHandler(method));
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates a handler function (of type {@link Function}) for given method.
     * @param method the actual handler method
     * @return the resulting {@link Function}.
     */
    private Function<Invoke, Collection<Stmt>> createHandler(Method method) {
        try {
            MethodHandle handler = lookup.unreflect(method);
            MethodType handlerType = MethodType.methodType(
                    method.getReturnType(), method.getParameterTypes());
            CallSite callSite = LambdaMetafactory.metafactory(lookup,
                    "apply",
                    MethodType.methodType(Function.class, this.getClass()),
                    handlerType.erase(), handler, handlerType);
            MethodHandle factory = callSite.getTarget().bindTo(this);
            @SuppressWarnings ("unchecked")
            var handlerFunction = (Function<Invoke, Collection<Stmt>>) factory.invoke();
            return handlerFunction;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access " + method +
                    ", please make sure that the IRModel class and the handler method" +
                    " are public", e);
        } catch (LambdaConversionException e) {
            throw new RuntimeException("Failed to create lambda function for " + method +
                    ", please make sure that the type of handler method" +
                    " is (Invoke)Collection<Stmt>", e);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    protected void registerHandlers() {
    }

    protected void registerHandler(JMethod api, Function<Invoke, Collection<Stmt>> handler) {
        if (handlers.containsKey(api)) {
            throw new RuntimeException(this + " registers multiple handlers for " +
                    api + " (in an IRModel, at most one handler can be registered for a method)");
        }
        handlers.put(api, handler);
    }

    @Override
    public Set<JMethod> getModeledAPIs() {
        return handlers.keySet();
    }

    @Override
    public void handleNewMethod(JMethod method) {
        List<Stmt> stmts = new ArrayList<>();
        method.getIR().invokes(false).forEach(invoke -> {
            JMethod target = invoke.getMethodRef().resolveNullable();
            if (target != null) {
                var handler = handlers.get(target);
                if (handler != null) {
                    stmts.addAll(handler.apply(invoke));
                }
            }
        });
        if (!stmts.isEmpty()) {
            method2GenStmts.put(method, List.copyOf(stmts));
        }
    }

    @Override
    public void handleNewCSMethod(CSMethod csMethod) {
        Collection<Stmt> genStmts = method2GenStmts.get(csMethod.getMethod());
        if (genStmts != null) {
            solver.addStmts(csMethod, genStmts);
        }
    }
}

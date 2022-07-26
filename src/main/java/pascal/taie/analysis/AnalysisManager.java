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

package pascal.taie.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.CallGraphBuilder;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.ConfigException;
import pascal.taie.config.Scope;
import pascal.taie.ir.IR;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.Timer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

/**
 * Creates and executes analyses based on given analysis plan.
 */
public class AnalysisManager {

    private static final Logger logger = LogManager.getLogger(AnalysisManager.class);

    private List<JClass> classScope;

    private List<JMethod> methodScope;

    /**
     * Executes the analysis plan.
     */
    public void execute(List<AnalysisConfig> analysisPlan) {
        analysisPlan.forEach(config -> Timer.runAndCount(
                () -> runAnalysis(config), config.getId()));
    }

    private void runAnalysis(AnalysisConfig config) {
        try {
            // Create analysis instance
            Class<?> clazz = Class.forName(config.getAnalysisClass());
            Constructor<?> ctor = clazz.getConstructor(AnalysisConfig.class);
            Object analysis = ctor.newInstance(config);
            // Run the analysis
            if (analysis instanceof ProgramAnalysis) {
                runProgramAnalysis((ProgramAnalysis<?>) analysis);
            } else if (analysis instanceof ClassAnalysis) {
                runClassAnalysis((ClassAnalysis<?>) analysis);
            } else if (analysis instanceof MethodAnalysis) {
                runMethodAnalysis((MethodAnalysis<?>) analysis);
            } else {
                throw new ConfigException(clazz + " is not an analysis class");
            }
        } catch (ClassNotFoundException  e) {
            throw new AnalysisException("Analysis class " +
                    config.getAnalysisClass() + " is not found", e);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AnalysisException("Failed to get constructor " +
                    config.getAnalysisClass() + "(AnalysisConfig), " +
                    "thus the analysis cannot be executed by Tai-e", e);
        } catch (InstantiationException | InvocationTargetException e) {
            throw new AnalysisException("Failed to initialize " +
                    config.getAnalysisClass(), e);
        }
    }

    private void runProgramAnalysis(ProgramAnalysis<?> analysis) {
        Object result = analysis.analyze();
        if (result != null) {
            World.get().storeResult(analysis.getId(), result);
        }
    }

    private void runClassAnalysis(ClassAnalysis<?> analysis) {
        getClassScope().parallelStream()
                .forEach(c -> {
                    Object result = analysis.analyze(c);
                    if (result != null) {
                        c.storeResult(analysis.getId(), result);
                    }
                });
    }

    private List<JClass> getClassScope() {
        if (classScope == null) {
            Scope scope = World.get().getOptions().getScope();
            classScope = switch (scope) {
                case APP -> World.get()
                        .getClassHierarchy()
                        .applicationClasses()
                        .toList();
                case ALL -> World.get()
                        .getClassHierarchy()
                        .allClasses()
                        .toList();
                case REACHABLE -> {
                    CallGraph<?, JMethod> callGraph = World.get().getResult(CallGraphBuilder.ID);
                    yield callGraph.reachableMethods()
                            .map(JMethod::getDeclaringClass)
                            .distinct()
                            .toList();
                }
            };
            logger.info("{} classes in scope ({}) of class analyses",
                    classScope.size(), scope);
        }
        return classScope;
    }

    private void runMethodAnalysis(MethodAnalysis<?> analysis) {
        getMethodScope()
                .parallelStream()
                .forEach(m -> {
                    IR ir = m.getIR();
                    Object result = analysis.analyze(ir);
                    if (result != null) {
                        ir.storeResult(analysis.getId(), result);
                    }
                });
    }

    private List<JMethod> getMethodScope() {
        if (methodScope == null) {
            Scope scope = World.get().getOptions().getScope();
            methodScope = switch (scope) {
                case APP, ALL -> getClassScope()
                        .stream()
                        .map(JClass::getDeclaredMethods)
                        .flatMap(Collection::stream)
                        .filter(m -> !m.isAbstract() && !m.isNative())
                        .toList();
                case REACHABLE -> {
                    CallGraph<?, JMethod> callGraph = World.get().getResult(CallGraphBuilder.ID);
                    yield callGraph.reachableMethods().toList();
                }
            };
            logger.info("{} methods in scope ({}) of method analyses",
                    methodScope.size(), scope);
        }
        return methodScope;
    }
}

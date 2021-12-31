/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
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
                runProgramAnalysis((ProgramAnalysis) analysis);
            } else if (analysis instanceof ClassAnalysis) {
                runClassAnalysis((ClassAnalysis) analysis);
            } else if (analysis instanceof MethodAnalysis) {
                runMethodAnalysis((MethodAnalysis) analysis);
            } else  {
                logger.warn(clazz + " is not an analysis");
            }
        } catch (ClassNotFoundException | NoSuchMethodException |
                InstantiationException | IllegalAccessException |
                InvocationTargetException e) {
            throw new AnalysisException("Failed to initialize " +
                    config.getAnalysisClass(), e);
        }
    }

    private void runProgramAnalysis(ProgramAnalysis analysis) {
        Object result = analysis.analyze();
        if (result != null) {
            World.storeResult(analysis.getId(), result);
        }
    }

    private void runClassAnalysis(ClassAnalysis analysis) {
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
            classScope = switch (World.getOptions().getScope()) {
                case Scope.APP -> World.getClassHierarchy()
                        .applicationClasses()
                        .toList();
                case Scope.ALL -> World.getClassHierarchy()
                        .allClasses()
                        .toList();
                case Scope.REACHABLE -> {
                    CallGraph<?, JMethod> callGraph = World.getResult(CallGraphBuilder.ID);
                    yield callGraph.reachableMethods()
                            .map(JMethod::getDeclaringClass)
                            .toList();
                }
                default -> throw new ConfigException(
                        "Unexpected scope option: " + World.getOptions().getScope());
            };
            logger.info("{} classes in scope ({}) of class analyses",
                    classScope.size(), World.getOptions().getScope());
        }
        return classScope;
    }

    private void runMethodAnalysis(MethodAnalysis analysis) {
        getMethodScope().parallelStream()
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
            methodScope = getClassScope().stream()
                    .map(JClass::getDeclaredMethods)
                    .flatMap(Collection::stream)
                    .filter(m -> !m.isAbstract() && !m.isNative())
                    .toList();
            logger.info("{} methods in scope ({}) of method analyses",
                    methodScope.size(), World.getOptions().getScope());
        }
        return methodScope;
    }
}

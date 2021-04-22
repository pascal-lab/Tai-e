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

package pascal.taie.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.InterproceduralAnalysis;
import pascal.taie.analysis.IntraproceduralAnalysis;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.AnalysisException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Initialize analyses based on given analysis plan.
 */
public class AnalysisManager {

    private static final Logger logger = LogManager.getLogger(AnalysisManager.class);

    /**
     * Execute the analysis plan.
     */
    public void execute(List<AnalysisConfig> analysisPlan) {
        analysisPlan.forEach(this::runAnalysis);
    }

    private void runAnalysis(AnalysisConfig config) {
        try {
            Class<?> clazz = Class.forName(config.getAnalysisClass());
            Constructor<?> ctor = clazz.getConstructor(AnalysisConfig.class);
            Object analysis = ctor.newInstance(config);
            if (analysis instanceof IntraproceduralAnalysis) {
                runIntraproceduralAnalysis((IntraproceduralAnalysis) analysis);
            } else if (analysis instanceof InterproceduralAnalysis) {
                runInterproceduralAnalysis((InterproceduralAnalysis) analysis);
            } else {
                logger.warn(clazz + " is not an analysis");
            }
        } catch (ClassNotFoundException | NoSuchMethodException |
                InstantiationException | IllegalAccessException |
                InvocationTargetException e) {
            throw new AnalysisException("Fail to initialize " +
                    config.getAnalysisClass(), e);
        }
    }

    private void runIntraproceduralAnalysis(IntraproceduralAnalysis analysis) {
        Stream<JMethod> methods = World.getClassHierarchy()
                .getAllClasses()
                .stream()
                .map(JClass::getDeclaredMethods)
                .flatMap(Collection::stream)
                .filter(m -> !m.isAbstract() && !m.isNative());
        // TODO: 1. parallelize analysis of different methods
        //       2. store result after analysis
        //       3. restrict analysis scope
        methods.forEach(m -> analysis.analyze(m.getIR()));
    }

    private void runInterproceduralAnalysis(InterproceduralAnalysis analysis) {
        analysis.analyze();
    }
}

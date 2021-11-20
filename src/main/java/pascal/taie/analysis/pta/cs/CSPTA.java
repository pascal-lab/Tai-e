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

package pascal.taie.analysis.pta.cs;

import pascal.taie.analysis.InterproceduralAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.selector.CISelector;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.AllocationSiteBasedModel;
import pascal.taie.analysis.pta.plugin.ResultProcessor;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.AnalysisOptions;
import pascal.taie.config.ConfigException;
import pascal.taie.util.Strings;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Context-sensitive pointer analysis.
 */
public class CSPTA extends InterproceduralAnalysis {

    public static final String ID = "cspta";

    public CSPTA(AnalysisConfig config) {
        super(config);
    }

    @Override
    public PointerAnalysisResult analyze() {
        AnalysisOptions options = getOptions();
        Solver solver = new Solver(options,
                new AllocationSiteBasedModel(options),
                getContextSelector(options.getString("cs")));
        solver.solve();
        PointerAnalysisResult result = solver.getResult();
        ResultProcessor.process(options, result);
        return result;
    }

    private static ContextSelector getContextSelector(String cs) {
        if (cs.equals("ci")) {
            return new CISelector();
        } else {
            try {
                // we expect that the argument of context-sensitivity variant
                // is of pattern k-kind, where k is limit of context length
                // and kind represents kind of context element (obj, type, etc.).
                String[] splits = cs.split("-");
                String k = splits[0];
                String kind = Strings.capitalize(splits[1]);
                String selectorName = "pascal.taie.analysis.pta.core.cs.selector." +
                        "_" + k + kind + "Selector";
                Class<?> c = Class.forName(selectorName);
                Constructor<?> ctor = c.getConstructor();
                return (ContextSelector) ctor.newInstance();
            } catch (RuntimeException e) {
                throw new ConfigException("Unexpected context-sensitivity variants: " + cs, e);
            } catch (ClassNotFoundException | NoSuchMethodException |
                    InvocationTargetException | InstantiationException |
                    IllegalAccessException e) {
                throw new ConfigException("Failed to initialize context selector: " + cs, e);
            }
        }
    }
}

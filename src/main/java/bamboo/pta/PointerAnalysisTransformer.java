/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta;

import bamboo.pta.analysis.context.ContextInsensitiveSelector;
import bamboo.pta.analysis.context.OneCallSelector;
import bamboo.pta.analysis.context.OneObjectSelector;
import bamboo.pta.analysis.context.OneTypeSelector;
import bamboo.pta.analysis.context.TwoCallSelector;
import bamboo.pta.analysis.data.CSMethod;
import bamboo.pta.analysis.data.CSVariable;
import bamboo.pta.analysis.data.InstanceField;
import bamboo.pta.analysis.data.MapBasedDataManager;
import bamboo.pta.analysis.data.Pointer;
import bamboo.pta.analysis.heap.AllocationSiteBasedModel;
import bamboo.pta.analysis.solver.PointerAnalysis;
import bamboo.pta.analysis.solver.PointerAnalysisImpl;
import bamboo.pta.jimple.JimplePointerAnalysis;
import bamboo.pta.jimple.JimpleProgramManager;
import bamboo.pta.set.HybridPointsToSet;
import bamboo.pta.set.PointsToSetFactory;
import bamboo.util.AnalysisException;
import soot.SceneTransformer;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import static bamboo.util.Stringify.streamToString;

public class PointerAnalysisTransformer extends SceneTransformer {

    private static final PointerAnalysisTransformer INSTANCE =
            new PointerAnalysisTransformer();

    public static PointerAnalysisTransformer v() {
        return INSTANCE;
    }

    private boolean isOutput = true;

    public void setOutput(boolean isOutput) {
        this.isOutput = isOutput;
    }

    private PointerAnalysisTransformer() {}

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        PointerAnalysis pta = new PointerAnalysisImpl();
        pta.setProgramManager(new JimpleProgramManager());
        setContextSensitivity(pta, options);
        pta.setHeapModel(new AllocationSiteBasedModel());
        PointsToSetFactory setFactory = new HybridPointsToSet.Factory();
        pta.setDataManager(new MapBasedDataManager(setFactory));
        pta.setPointsToSetFactory(setFactory);
        pta.solve();
        JimplePointerAnalysis.v().setPointerAnalysis(pta);
        if (isOutput) {
            System.out.println("---------- Reachable methods: ----------");
            pta.getCallGraph().getReachableMethods()
                    .stream()
                    .sorted(Comparator.comparing(CSMethod::toString))
                    .forEach(System.out::println);
            System.out.println("---------- Call graph edges: ----------");
            pta.getCallGraph().getAllEdges().forEach(System.out::println);
            printVariables(pta.getVariables());
            printInstanceFields(pta.getInstanceFields());
            System.out.println("----------------------------------------");
        }

        if (ResultChecker.isAvailable()) {
            ResultChecker.get().compare(pta);
        }
    }

    private void setContextSensitivity(
            PointerAnalysis pta, Map<String, String> options) {
        switch (options.get("cs")) {
            case "ci":
                pta.setContextSelector(new ContextInsensitiveSelector());
                break;
            case "1-call": case "1-cfa":
                pta.setContextSelector(new OneCallSelector());
                break;
            case "1-obj": case "1-object":
                pta.setContextSelector(new OneObjectSelector());
                break;
            case "1-type":
                pta.setContextSelector(new OneTypeSelector());
                break;
            case "2-call": case "2-cfa":
                pta.setContextSelector(new TwoCallSelector());
                break;
            default:
                throw new AnalysisException(
                        "Unknown context sensitivity variant: " + options.get("cs"));
        }
    }

    private void printVariables(Stream<CSVariable> vars) {
        System.out.println("---------- Points-to sets of all variables: ----------");
        vars.sorted(Comparator.comparing(CSVariable::toString))
                .forEach(this::printPointsToSet);
    }

    private void printInstanceFields(Stream<InstanceField> fields) {
        System.out.println("---------- Points-to sets of all instance fields: ----------");
        fields.sorted(Comparator.comparing(InstanceField::toString))
                .forEach(this::printPointsToSet);
    }

    private void printPointsToSet(Pointer pointer) {
        System.out.println(pointer + " -> "
                + streamToString(pointer.getPointsToSet().stream()));
    }
}

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

import bamboo.pta.core.context.ContextInsensitiveSelector;
import bamboo.pta.core.context.OneCallSelector;
import bamboo.pta.core.context.OneObjectSelector;
import bamboo.pta.core.context.OneTypeSelector;
import bamboo.pta.core.context.TwoCallSelector;
import bamboo.pta.core.context.TwoObjectSelector;
import bamboo.pta.core.context.TwoTypeSelector;
import bamboo.pta.core.cs.ArrayIndex;
import bamboo.pta.core.cs.CSMethod;
import bamboo.pta.core.cs.CSVariable;
import bamboo.pta.core.cs.InstanceField;
import bamboo.pta.core.cs.MapBasedDataManager;
import bamboo.pta.core.cs.Pointer;
import bamboo.pta.core.cs.StaticField;
import bamboo.pta.core.heap.AllocationSiteBasedModel;
import bamboo.pta.core.solver.PointerAnalysis;
import bamboo.pta.core.solver.PointerAnalysisImpl;
import bamboo.pta.jimple.JimplePointerAnalysis;
import bamboo.pta.jimple.JimpleProgramManager;
import bamboo.pta.set.HybridPointsToSet;
import bamboo.pta.set.PointsToSetFactory;
import bamboo.util.AnalysisException;
import soot.SceneTransformer;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import static bamboo.util.Stringify.streamToString;

public class PointerAnalysisTransformer extends SceneTransformer {

    private static final PointerAnalysisTransformer INSTANCE =
            new PointerAnalysisTransformer();
    /**
     * The print stream used to output key analysis results, i.e., the results
     * that are checked during testing.
     */
    private PrintStream out = System.out;
    /**
     * If output analysis results.
     */
    private boolean isOutput = true;

    private PointerAnalysisTransformer() {
    }

    public static PointerAnalysisTransformer v() {
        return INSTANCE;
    }

    public void setOut(PrintStream out) {
        this.out = out;
    }

    public void setOutput(boolean isOutput) {
        this.isOutput = isOutput;
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        PointerAnalysis pta = new PointerAnalysisImpl();
        pta.setProgramManager(new JimpleProgramManager());
        setContextSensitivity(pta, options);
        pta.setHeapModel(new AllocationSiteBasedModel());
        PointsToSetFactory setFactory = new HybridPointsToSet.Factory();
        pta.setDataManager(new MapBasedDataManager(setFactory));
        pta.setPointsToSetFactory(setFactory);
        pta.analyze();
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
            printArrayIndexes(pta.getArrayIndexes());
            printStaticFields(pta.getStaticFields());
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
            case "1-call":
            case "1-cfa":
                pta.setContextSelector(new OneCallSelector());
                break;
            case "1-obj":
            case "1-object":
                pta.setContextSelector(new OneObjectSelector());
                break;
            case "1-type":
                pta.setContextSelector(new OneTypeSelector());
                break;
            case "2-call":
            case "2-cfa":
                pta.setContextSelector(new TwoCallSelector());
                break;
            case "2-obj":
            case "2-object":
                pta.setContextSelector(new TwoObjectSelector());
                break;
            case "2-type":
                pta.setContextSelector(new TwoTypeSelector());
                break;
            default:
                throw new AnalysisException(
                        "Unknown context sensitivity variant: " + options.get("cs"));
        }
    }

    private void printVariables(Stream<CSVariable> vars) {
        out.println("---------- Points-to sets of all variables: ----------");
        vars.sorted(Comparator.comparing(CSVariable::toString))
                .forEach(this::printPointsToSet);
    }

    private void printInstanceFields(Stream<InstanceField> fields) {
        out.println("---------- Points-to sets of all instance fields: ----------");
        fields.sorted(Comparator.comparing(InstanceField::toString))
                .forEach(this::printPointsToSet);
    }

    private void printArrayIndexes(Stream<ArrayIndex> arrays) {
        out.println("---------- Points-to sets of all array indexes: ----------");
        arrays.sorted(Comparator.comparing(ArrayIndex::toString))
                .forEach(this::printPointsToSet);
    }

    private void printStaticFields(Stream<StaticField> fields) {
        out.println("---------- Points-to sets of all static fields: ----------");
        fields.sorted(Comparator.comparing(StaticField::toString))
                .forEach(this::printPointsToSet);
    }

    private void printPointsToSet(Pointer pointer) {
        out.println(pointer + " -> "
                + streamToString(pointer.getPointsToSet().stream()));
    }
}

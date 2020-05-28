/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C)  2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C)  2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.analysis.ci;

import bamboo.pta.analysis.heap.AllocationSiteBasedModel;
import bamboo.pta.element.Method;
import bamboo.pta.jimple.JimplePointerAnalysis;
import bamboo.pta.jimple.JimpleProgramManager;
import soot.SceneTransformer;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import static bamboo.pta.analysis.ci.Stringify.pointerToString;
import static bamboo.pta.analysis.ci.Stringify.pointsToSetToString;

public class CIPointerAnalysisTransformer extends SceneTransformer {

    private static final CIPointerAnalysisTransformer INSTANCE =
            new CIPointerAnalysisTransformer();

    public static CIPointerAnalysisTransformer v() {
        return INSTANCE;
    }

    private static boolean isOutput = true;

    public static void setOutput(boolean isOutput) {
        CIPointerAnalysisTransformer.isOutput = isOutput;
    }

    private CIPointerAnalysisTransformer() {};

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        CIPointerAnalysis pta = new CIPointerAnalysis();
        pta.setProgramManager(new JimpleProgramManager());
        pta.setHeapModel(new AllocationSiteBasedModel());
        pta.solve();
        JimplePointerAnalysis.v().setCIPointerAnalysis(pta);

        if (isOutput) {
            System.out.println("---------- Reachable methods: ----------");
            pta.getCallGraph().getReachableMethods()
                    .stream()
                    .sorted(Comparator.comparing(Method::toString))
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

    private void printVariables(Stream<Var> vars) {
        System.out.println("---------- Points-to sets of all variables: ----------");
        vars.sorted(Comparator.comparing(p -> pointerToString(p)))
                .forEach(this::printPointsToSet);
    }

    private void printInstanceFields(Stream<InstanceField> fields) {
        System.out.println("---------- Points-to sets of all instance fields: ----------");
        fields.sorted(Comparator.comparing(f -> pointerToString(f)))
                .forEach(this::printPointsToSet);
    }

    private void printPointsToSet(Pointer pointer) {
        System.out.println(pointerToString(pointer)
                + " -> {" + pointsToSetToString(pointer.getPointsToSet())
                + "}");
    }
}

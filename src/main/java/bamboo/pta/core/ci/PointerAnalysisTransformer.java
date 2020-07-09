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

package bamboo.pta.core.ci;

import bamboo.pta.core.heap.AllocationSiteBasedModel;
import bamboo.pta.element.Method;
import bamboo.pta.jimple.JimplePointerAnalysis;
import bamboo.pta.jimple.JimpleProgramManager;
import soot.SceneTransformer;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import static bamboo.util.Stringify.streamToString;

public class PointerAnalysisTransformer extends SceneTransformer {

    private static final PointerAnalysisTransformer INSTANCE =
            new PointerAnalysisTransformer();
    private boolean isOutput = true;

    private PointerAnalysisTransformer() {
    }

    public static PointerAnalysisTransformer v() {
        return INSTANCE;
    }

    public void setOutput(boolean isOutput) {
        this.isOutput = isOutput;
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        PointerAnalysis pta = new PointerAnalysis();
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
        vars.sorted(Comparator.comparing(Var::toString))
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

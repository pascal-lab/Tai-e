/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.analysis.oldpta.core.ci;

import pascal.taie.World;
import pascal.taie.analysis.oldpta.core.heap.AllocationSiteBasedModel;
import pascal.taie.frontend.soot.JimplePointerAnalysis;
import pascal.taie.frontend.soot.SootWorldBuilder;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.Strings;
import soot.Scene;
import soot.SceneTransformer;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

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
        new SootWorldBuilder(Scene.v()).build();

        PointerAnalysis pta = new PointerAnalysis();
        pta.setHeapModel(new AllocationSiteBasedModel(
                World.getTypeManager()));
        pta.solve();
        JimplePointerAnalysis.get().setCIPointerAnalysis(pta);

        if (isOutput) {
            System.out.println("---------- Reachable methods: ----------");
            pta.getCallGraph().getReachableMethods()
                    .stream()
                    .sorted(Comparator.comparing(JMethod::toString))
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
                + Strings.streamToString(pointer.getPointsToSet().stream()));
    }
}

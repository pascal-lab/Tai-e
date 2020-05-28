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
import bamboo.pta.element.Obj;
import bamboo.pta.jimple.JimplePointerAnalysis;
import bamboo.pta.jimple.JimpleProgramManager;
import soot.SceneTransformer;
import soot.jimple.AssignStmt;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

public class CIPointerAnalysisTransformer extends SceneTransformer {

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        CIPointerAnalysis pta = new CIPointerAnalysis();
        pta.setProgramManager(new JimpleProgramManager());
        pta.setHeapModel(new AllocationSiteBasedModel());
        pta.solve();
        JimplePointerAnalysis.v().setCIPointerAnalysis(pta);
        System.out.println("---------- Reachable methods: ----------");
        pta.getCallGraph().getReachableMethods()
                .stream()
                .sorted(Comparator.comparing(Method::toString))
                .forEach(System.out::println);
        System.out.println("---------- Call graph edges: ----------");
        pta.getCallGraph().getAllEdges().forEach(System.out::println);
        printVariables(pta.getVariables());
        printInstanceFields(pta.getInstanceFields());
    }

    private void printVariables(Stream<Var> vars) {
        System.out.println("---------- Points-to sets of all variables: ----------");
        vars.sorted(Comparator.comparing(p -> p.getVariable().toString()))
                .forEach(this::printPointsToSet);
    }

    private void printInstanceFields(Stream<InstanceField> fields) {
        System.out.println("---------- Points-to sets of all instance fields: ----------");
        fields.sorted(Comparator.comparing(f -> f.getBase().toString()))
                .forEach(this::printPointsToSet);
    }

    private void printPointsToSet(Pointer pointer) {
        String ptr;
        if (pointer instanceof InstanceField) {
            InstanceField f = (InstanceField) pointer;
            ptr = objToString(f.getBase()) + "." + f.getField().getName();
        } else {
            ptr = pointer.toString();
        }
        System.out.print(ptr + " -> {");
        pointer.getPointsToSet().stream()
                .sorted(Comparator.comparing(Obj::toString))
                .forEach(o -> System.out.print(objToString(o) + ","));
        System.out.println("}");
    }

    private String objToString(Obj obj) {
        StringBuilder sb = new StringBuilder();
        if (obj.getContainerMethod() != null) {
            sb.append(obj.getContainerMethod()).append('/');
        }
        Object allocation = obj.getAllocationSite();
        if (allocation instanceof AssignStmt) {
            AssignStmt alloc = (AssignStmt) allocation;
            sb.append(alloc.getRightOp())
                    .append('/')
                    .append(alloc.getJavaSourceStartLineNumber());
        } else {
            sb.append(allocation);
        }
        return sb.toString();
    }
}

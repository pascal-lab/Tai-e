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

package pascal.taie.pta.plugin;

import pascal.taie.pta.ResultChecker;
import pascal.taie.pta.core.cs.ArrayIndex;
import pascal.taie.pta.core.cs.CSMethod;
import pascal.taie.pta.core.cs.CSVariable;
import pascal.taie.pta.core.cs.InstanceField;
import pascal.taie.pta.core.cs.Pointer;
import pascal.taie.pta.core.cs.StaticField;
import pascal.taie.pta.core.solver.PointerAnalysis;
import pascal.taie.pta.options.Options;
import pascal.taie.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.stream.Stream;

import static pascal.taie.util.StringUtils.streamToString;

/**
 * Print pointer analysis results to specify output stream.
 * This class is also used by ResultChecker. To ease the access from
 * ResultChecker, this class is implemented as singleton.
 */
public enum ResultPrinter implements Plugin {

    INSTANCE;

    private final DecimalFormat formatter = new DecimalFormat("#,####");

    /**
     * The print stream used to output key analysis results, i.e., the results
     * that are checked during testing.
     */
    private PrintStream out = System.out;

    private PointerAnalysis pta;

    public static ResultPrinter v() {
        return INSTANCE;
    }

    public void setOut(PrintStream out) {
        this.out = out;
    }

    @Override
    public void setPointerAnalysis(PointerAnalysis pta) {
        this.pta = pta;
    }

    @Override
    public void postprocess() {
        printResults(pta);
        if (ResultChecker.isAvailable()) {
            ResultChecker.get().compare(pta);
        }
    }

    private void printResults(PointerAnalysis pta) {
        if (Options.get().isTestMode()) {
            printPointers(pta);
        } else if (Options.get().isOutputResults()) {
            File output = Options.get().getOutputFile();
            if (output != null) {
                try {
                    out = new PrintStream(new FileOutputStream(output),
                            false, "UTF-8");
                } catch (FileNotFoundException | UnsupportedEncodingException e) {
                    System.err.println("Failed to write output, caused by " + e);
                }
            }
            out.println("---------- Reachable methods: ----------");
            pta.getCallGraph().getReachableMethods()
                    .stream()
                    .sorted(Comparator.comparing(CSMethod::toString))
                    .forEach(out::println);
            out.println("---------- Call graph edges: ----------");
            pta.getCallGraph().getAllEdges().forEach(out::println);
            printPointers(pta);
            out.println("----------------------------------------");
        }
        printStatistics(pta);
    }

    private void printPointers(PointerAnalysis pta) {
        printVariables(pta.getVariables());
        printInstanceFields(pta.getInstanceFields());
        printArrayIndexes(pta.getArrayIndexes());
        printStaticFields(pta.getStaticFields());
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
//                + "\t" + pointer.getPointsToSet().size() + "\t"
                + streamToString(pointer.getPointsToSet().stream()));
    }

    private void printStatistics(PointerAnalysis pta) {
        int varInsens = (int) pta.getVariables()
                .map(CSVariable::getVariable)
                .distinct()
                .count();
        int varSens = (int) pta.getVariables().count();
        int vptSizeSens = pta.getVariables()
                .mapToInt(v -> v.getPointsToSet().size())
                .sum();
        int ifptSizeSens = pta.getInstanceFields()
                .mapToInt(f -> f.getPointsToSet().size())
                .sum();
        int aptSizeSens = pta.getArrayIndexes()
                .mapToInt(a -> a.getPointsToSet().size())
                .sum();
        int sfptSizeSens = pta.getStaticFields()
                .mapToInt(f -> f.getPointsToSet().size())
                .sum();
        int reachableInsens = (int) pta.getCallGraph()
                .getReachableMethods()
                .stream()
                .map(CSMethod::getMethod)
                .distinct()
                .count();
        int reachableSens = pta.getCallGraph()
                .getReachableMethods()
                .size();
        int callEdgeInsens = (int) pta.getCallGraph()
                .getAllEdges()
                .map(e -> new Pair<>(e.getCallSite().getCallSite(),
                        e.getCallee().getMethod()))
                .distinct()
                .count();
        int callEdgeSens = (int) pta.getCallGraph()
                .getAllEdges()
                .count();
        System.out.println("-------------- Pointer analysis statistics: --------------");
        System.out.printf("%-30s%s (insens) / %s (sens)%n", "#var pointers:",
                format(varInsens), format(varSens));
        System.out.printf("%-30s%s (sens)%n", "#var points-to:",
                format(vptSizeSens));
        System.out.printf("%-30s%s (sens)%n", "#instance field points-to:",
                format(ifptSizeSens));
        System.out.printf("%-30s%s (sens)%n", "#array points-to:",
                format(aptSizeSens));
        System.out.printf("%-30s%s (sens)%n", "#static field points-to:",
                format(sfptSizeSens));
        System.out.printf("%-30s%s (insens) / %s (sens)%n", "#reachable methods:",
                format(reachableInsens), format(reachableSens));
        System.out.printf("%-30s%s (insens) / %s (sens)%n", "#call graph edges:",
                format(callEdgeInsens), format(callEdgeSens));
        System.out.println("----------------------------------------");
    }

    private String format(int i) {
        return formatter.format(i);
    }
}

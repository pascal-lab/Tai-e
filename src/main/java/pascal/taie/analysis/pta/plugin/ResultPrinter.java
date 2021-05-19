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

package pascal.taie.analysis.pta.plugin;

import pascal.taie.analysis.pta.ResultChecker;
import pascal.taie.analysis.pta.core.cs.element.ArrayIndex;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.element.InstanceField;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.cs.element.StaticField;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.util.collection.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.stream.Stream;

import static pascal.taie.util.Strings.streamToString;

/**
 * Prints pointer analysis results to specify output stream.
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

    private Solver solver;

    public static ResultPrinter get() {
        return INSTANCE;
    }

    public void setOut(PrintStream out) {
        this.out = out;
    }

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
    }

    @Override
    public void onPostprocess() {
        printResults(solver);
        if (ResultChecker.isAvailable()) {
            ResultChecker.get().compare(solver);
        }
    }

    private void printResults(Solver solver) {
        if (solver.getOptions().getBoolean("print-pointers")) {
            printPointers(solver);
        }
        if (solver.getOptions().getBoolean("dump-results")) {
            String path = solver.getOptions().getString("output-file");
            if (path != null) {
                try {
                    File output = new File(path);
                    out = new PrintStream(new FileOutputStream(output),
                            false, StandardCharsets.UTF_8);
                } catch (FileNotFoundException e) {
                    System.err.println("Failed to write output, caused by " + e);
                }
            }
            out.println("---------- Reachable methods: ----------");
            solver.getCallGraph().reachableMethods()
                    .sorted(Comparator.comparing(CSMethod::toString))
                    .forEach(out::println);
            out.println("---------- Call graph edges: ----------");
            solver.getCallGraph().edges().forEach(out::println);
            printPointers(solver);
            out.println("----------------------------------------");
        }
        printStatistics(solver);
    }

    private void printPointers(Solver solver) {
        printVariables(solver.vars());
        printInstanceFields(solver.instanceFields());
        printArrayIndexes(solver.arrayIndexes());
        printStaticFields(solver.staticFields());
    }

    private void printVariables(Stream<CSVar> vars) {
        out.println("---------- Points-to sets of all variables: ----------");
        vars.sorted(Comparator.comparing(CSVar::toString))
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
                + streamToString(pointer.getPointsToSet().objects()));
    }

    private void printStatistics(Solver solver) {
        int varInsens = (int) solver.vars()
                .map(CSVar::getVar)
                .distinct()
                .count();
        int varSens = (int) solver.vars().count();
        int vptSizeSens = solver.vars()
                .mapToInt(v -> v.getPointsToSet().size())
                .sum();
        int ifptSizeSens = solver.instanceFields()
                .mapToInt(f -> f.getPointsToSet().size())
                .sum();
        int aptSizeSens = solver.arrayIndexes()
                .mapToInt(a -> a.getPointsToSet().size())
                .sum();
        int sfptSizeSens = solver.staticFields()
                .mapToInt(f -> f.getPointsToSet().size())
                .sum();
        int reachableInsens = (int) solver.getCallGraph()
                .reachableMethods()
                .map(CSMethod::getMethod)
                .distinct()
                .count();
        int reachableSens = (int) solver.getCallGraph()
                .reachableMethods()
                .count();
        int callEdgeInsens = (int) solver.getCallGraph()
                .edges()
                .map(e -> new Pair<>(e.getCallSite().getCallSite(),
                        e.getCallee().getMethod()))
                .distinct()
                .count();
        int callEdgeSens = (int) solver.getCallGraph()
                .edges()
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

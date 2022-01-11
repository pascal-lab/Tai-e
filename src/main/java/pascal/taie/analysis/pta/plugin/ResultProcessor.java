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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.taint.TaintFlow;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.config.AnalysisOptions;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.Strings;
import pascal.taie.util.collection.Lists;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static pascal.taie.util.collection.CollectionUtils.sum;

/**
 * Dump points-to set to file or compare the analysis result with
 * the ones read from input file.
 * Currently, the compare functionality is mainly for testing purpose.
 * It is not efficient and not recommended applying on large program.
 */
public class ResultProcessor implements Plugin {

    private static final Logger logger = LogManager.getLogger(ResultProcessor.class);

    private static final String HEADER = "Points-to sets of all ";

    /**
     * Separator between pointer and its points-to set.
     */
    private static final String SEP = " -> ";

    private static final DecimalFormat formatter = new DecimalFormat("#,####");

    private Solver solver;

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
    }

    @Override
    public void onFinish() {
        process(solver.getOptions(), solver.getResult());
    }

    public static void process(AnalysisOptions options,
                               PointerAnalysisResult result) {
        printStatistics(result);
        String action = options.getString("action");
        if (action == null) {
            return;
        }
        String file = options.getString("file");
        boolean taintEnabled = options.getString("taint-config") != null;
        switch (action) {
            case "dump":
                dumpPointsToSet(result, file, taintEnabled);
                break;
            case "compare":
                if (taintEnabled) {
                    // when taint analysis is enabled, we only compare
                    // detected taint flows
                    compareTaintFlows(result, file);
                } else {
                    comparePointsToSet(result, file);
                }
                break;
        }
    }

    private static void printStatistics(PointerAnalysisResult result) {
        int varInsens = (int) result.vars().count();
        int varSens = result.getCSVars().size();
        int vptSizeInsens = result.vars()
                .mapToInt(v -> result.getPointsToSet(v).size()).sum();
        int vptSizeSens = sum(result.getCSVars(), v -> v.getPointsToSet().size());
        int sfptSizeSens = sum(result.getStaticFields(), f -> f.getPointsToSet().size());
        int ifptSizeSens = sum(result.getInstanceFields(), f -> f.getPointsToSet().size());
        int aptSizeSens = sum(result.getArrayIndexes(), a -> a.getPointsToSet().size());
        int reachableInsens = result.getCallGraph().getNumberOfMethods();
        int reachableSens = result.getCSCallGraph().getNumberOfMethods();
        int callEdgeInsens = (int) result.getCallGraph()
                .edges().count();
        int callEdgeSens = (int) result.getCSCallGraph()
                .edges().count();
        System.out.println("-------------- Pointer analysis statistics: --------------");
        System.out.printf("%-30s%s (insens) / %s (sens)%n", "#var pointers:",
                format(varInsens), format(varSens));
        System.out.printf("%-30s%s (insens) / %s (sens)%n", "#var points-to:",
                format(vptSizeInsens), format(vptSizeSens));
        System.out.printf("%-30s%s (sens)%n", "#static field points-to:",
                format(sfptSizeSens));
        System.out.printf("%-30s%s (sens)%n", "#instance field points-to:",
                format(ifptSizeSens));
        System.out.printf("%-30s%s (sens)%n", "#array points-to:",
                format(aptSizeSens));
        System.out.printf("%-30s%s (insens) / %s (sens)%n", "#reachable methods:",
                format(reachableInsens), format(reachableSens));
        System.out.printf("%-30s%s (insens) / %s (sens)%n", "#call graph edges:",
                format(callEdgeInsens), format(callEdgeSens));
        System.out.println("----------------------------------------");
    }

    private static String format(int i) {
        return formatter.format(i);
    }

    private static void dumpPointsToSet(PointerAnalysisResult result,
                                        String output, boolean taintEnabled) {
        PrintStream out;
        if (output != null) {  // if output file is given, then dump to the file
            File outFile = new File(output);
            try {
                out = new PrintStream(new FileOutputStream(outFile));
                logger.info("Dumping points-to set to {} ...", outFile);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Failed to open output file", e);
            }
        } else {  // otherwise, dump to System.out
            out = System.out;
        }
        dumpPointers(out, result.getCSVars(), "variables");
        dumpPointers(out, result.getStaticFields(), "static fields");
        dumpPointers(out, result.getInstanceFields(), "instance fields");
        dumpPointers(out, result.getArrayIndexes(), "array indexes");
        if (taintEnabled) {
            dumpTaintFlows(out, result);
        }
        if (out != System.out) {
            out.close();
        }
    }

    private static void dumpPointers(
            PrintStream out, Collection<? extends Pointer> pointers, String desc) {
        out.println(HEADER + desc);
        pointers.stream()
                .sorted(Comparator.comparing(Pointer::toString))
                .forEach(p -> out.println(p + SEP + toString(p.getPointsToSet())));
        out.println();
    }

    private static void comparePointsToSet(PointerAnalysisResult result, String input) {
        logger.info("Comparing points-to set with {} ...", input);
        var inputs = readPointsToSets(input);
        Map<String, Pointer> pointers = new LinkedHashMap<>();
        addPointers(pointers, result.getCSVars());
        addPointers(pointers, result.getStaticFields());
        addPointers(pointers, result.getInstanceFields());
        addPointers(pointers, result.getArrayIndexes());
        List<String> mismatches = new ArrayList<>();
        pointers.forEach((pointerStr, pointer) -> {
            String given = toString(pointer.getPointsToSet());
            String expected = inputs.get(pointerStr);
            if (!given.equals(expected)) {
                mismatches.add(String.format("%s, expected: %s, given: %s",
                        pointerStr, expected, given));
            }
        });
        inputs.keySet()
                .stream()
                .filter(Predicate.not(pointers::containsKey))
                .forEach(pointerStr -> {
                    String expected = inputs.get(pointerStr);
                    mismatches.add(String.format("%s, expected: %s, given: null",
                            pointerStr, expected));
                });
        if (!mismatches.isEmpty()) {
            throw new AnalysisException("Mismatches of points-to set\n" +
                    String.join("\n", mismatches));
        }
    }

    private static Map<String, String> readPointsToSets(String input) {
        try {
            Map<String, String> result = new LinkedHashMap<>();
            Files.lines(Path.of(input))
                    .filter(line -> line.contains(SEP))
                    .map(line -> line.split(SEP))
                    .forEach(s -> result.put(s[0], s[1]));
            return result;
        } catch (IOException e) {
            throw new AnalysisException(
                    "Failed to read points-to set from " + input, e);
        }
    }

    private static void addPointers(Map<String, Pointer> map,
                                    Collection<? extends Pointer> pointers) {
        pointers.stream()
                .sorted(Comparator.comparing(Pointer::toString))
                .forEach(p -> map.put(p.toString(), p));
    }

    private static String toString(PointsToSet pts) {
        return Strings.toString(pts.objects());
    }

    private static void dumpTaintFlows(PrintStream out, PointerAnalysisResult result) {
        Set<TaintFlow> taintFlows = getTaintFlows(result);
        out.printf("Detected %d taint flow(s):%n", taintFlows.size());
        taintFlows.forEach(out::println);
        out.println();
    }

    /**
     * @return taint analysis result.
     */
    private static Set<TaintFlow> getTaintFlows(PointerAnalysisResult result) {
        for (String key : result.getKeys()) {
            if (key.contains("Taint")) { // adapt different taint analyses
                return result.getResult(key);
            }
        }
        throw new AnalysisException("Taint analysis result is absent");
    }

    private static void compareTaintFlows(PointerAnalysisResult result, String input) {
        logger.info("Comparing taint flows with {} ...", input);
        List<String> inputs = readTaintFlows(input);
        List<String> taintFlows = Lists.map(getTaintFlows(result),
                TaintFlow::toString);
        List<String> mismatches = new ArrayList<>();
        taintFlows.forEach(taintFlow -> {
            if (!inputs.contains(taintFlow)) {
                mismatches.add(taintFlow + " should NOT be included");
            }
        });
        inputs.forEach(expected -> {
            if (!taintFlows.contains(expected)) {
                mismatches.add(expected + " should be included");
            }
        });
        if (!mismatches.isEmpty()) {
            throw new AnalysisException("Mismatches of taint flow(s)\n" +
                    String.join("\n", mismatches));
        }
    }

    private static List<String> readTaintFlows(String input) {
        try {
            List<String> taintFlows = new ArrayList<>();
            Files.lines(Path.of(input))
                    .filter(line -> line.startsWith("TaintFlow{") && line.contains(SEP))
                    .forEach(taintFlows::add);
            return taintFlows;
        } catch (IOException e) {
            throw new AnalysisException(
                    "Failed to read taint flows from " + input, e);
        }
    }
}

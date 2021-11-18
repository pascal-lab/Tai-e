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
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.config.AnalysisOptions;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.Strings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Dump points-to set to file or compare the analysis result with
 * the ones read from input file.
 * Currently, the compare functionality is mainly for testing purpose.
 * It is not efficient and not recommended applying on large program.
 */
public class ResultProcessor {

    private static final Logger logger = LogManager.getLogger(ResultProcessor.class);

    private static final String HEADER = "Points-to sets of all ";

    /**
     * Separator between pointer and its points-to set.
     */
    private static final String SEP = " -> ";

    private static final DecimalFormat formatter = new DecimalFormat("#,####");

    public static void process(AnalysisOptions options,
                               PointerAnalysisResult result) {
        printStatistics(result);
        String action = options.getString("action");
        if (action == null) {
            return;
        }
        String file = options.getString("file");
        switch (action) {
            case "dump":
                dumpPointsToSet(result, file);
                break;
            case "compare":
                comparePointsToSet(result, file);
                break;
        }
    }

    private static void printStatistics(PointerAnalysisResult result) {
        int varInsens = (int) result.vars().count();
        int varSens = (int) result.csVars().count();
        int vptSizeInsens = result.vars()
                .mapToInt(v -> result.getPointsToSet(v).size()).sum();
        int vptSizeSens = result.csVars()
                .mapToInt(v -> v.getPointsToSet().size()).sum();
        int sfptSizeSens = result.staticFields()
                .mapToInt(f -> f.getPointsToSet().size()).sum();
        int ifptSizeSens = result.instanceFields()
                .mapToInt(f -> f.getPointsToSet().size()).sum();
        int aptSizeSens = result.arrayIndexes()
                .mapToInt(a -> a.getPointsToSet().size()).sum();
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

    private static void dumpPointsToSet(PointerAnalysisResult result, String output) {
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
        dumpPointers(out, result.csVars(), "variables");
        dumpPointers(out, result.staticFields(), "static fields");
        dumpPointers(out, result.instanceFields(), "instance fields");
        dumpPointers(out, result.arrayIndexes(), "array indexes");
        if (out != System.out) {
            out.close();
        }
    }

    private static void dumpPointers(PrintStream out, Stream<? extends Pointer> pointers, String desc) {
        out.println(HEADER + desc);
        pointers.sorted(Comparator.comparing(Pointer::toString))
                .forEach(p -> out.println(p + SEP + toString(p.getPointsToSet())));
        out.println();
    }

    private static void comparePointsToSet(PointerAnalysisResult result, String input) {
        logger.info("Comparing points-to set with {} ...", input);
        var inputs = readPointsToSets(input);
        Map<String, Pointer> pointers = new LinkedHashMap<>();
        addPointers(pointers, result.csVars());
        addPointers(pointers, result.staticFields());
        addPointers(pointers, result.instanceFields());
        addPointers(pointers, result.arrayIndexes());
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
                                    Stream<? extends Pointer> pointers) {
        pointers.sorted(Comparator.comparing(Pointer::toString))
                .forEach(p -> map.put(p.toString(), p));
    }

    private static String toString(PointsToSet pts) {
        return Strings.toString(pts.objects());
    }
}

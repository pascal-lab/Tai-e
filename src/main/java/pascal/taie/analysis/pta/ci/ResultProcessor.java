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

package pascal.taie.analysis.pta.ci;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.config.AnalysisOptions;
import pascal.taie.util.Strings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.stream.Stream;

class ResultProcessor {

    private static final Logger logger = LogManager.getLogger(ResultProcessor.class);

    private static final String HEADER = "Points-to sets of all ";

    /**
     * Separator between pointer and its points-to set.
     */
    private static final String SEP = " -> ";

    private static final DecimalFormat formatter = new DecimalFormat("#,###");

    private final AnalysisOptions options;

    public ResultProcessor(AnalysisOptions options) {
        this.options = options;
    }

    void process(CIPTAResult result) {
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

    private static void printStatistics(CIPTAResult result) {
        int vars = (int) result.vars().count();
        int vptSize = result.vars()
                .mapToInt(v -> result.getPointsToSet(v).size()).sum();
        int ifptSize = result.getPointerFlowGraph()
                .pointers()
                .filter(p -> p instanceof InstanceFieldPtr)
                .mapToInt(p -> p.getPointsToSet().size())
                .sum();
        int reachable = result.getCallGraph().getNumberOfMethods();
        int callEdges = (int) result.getCallGraph()
                .edges().count();
        System.out.println("-------------- Pointer analysis statistics: --------------");
        System.out.printf("%-30s%s%n", "#var pointers:", format(vars));
        System.out.printf("%-30s%s%n", "#var points-to:", format(vptSize));
        System.out.printf("%-30s%s%n", "#instance field points-to:", format(ifptSize));
        System.out.printf("%-30s%s%n", "#reachable methods:", format(reachable));
        System.out.printf("%-30s%s%n", "#call graph edges:", format(callEdges));
        System.out.println("----------------------------------------");
    }

    private static String format(int i) {
        return formatter.format(i);
    }

    private static void dumpPointsToSet(CIPTAResult result, String output) {
        File outFile = new File(output);
        try (PrintStream out =
                     new PrintStream(new FileOutputStream(outFile))) {
            logger.info("Dumping points-to set to {} ...", outFile);
            dumpPointers(out, result.getPointerFlowGraph()
                            .pointers()
                            .filter(p -> p instanceof VarPtr),
                    "variables");
            dumpPointers(out, result.getPointerFlowGraph()
                            .pointers()
                            .filter(p -> p instanceof InstanceFieldPtr),
                    "instance fields");
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump points-to set to " + outFile, e);
        }
    }

    private static void dumpPointers(PrintStream out, Stream<? extends Pointer> pointers, String desc) {
        out.println(HEADER + desc);
        pointers.sorted(Comparator.comparing(Pointer::toString))
                .forEach(p -> out.println(p + SEP + toString(p.getPointsToSet())));
        out.println();
    }

    private static String toString(PointsToSet pts) {
        return Strings.toString(pts.objects());
    }

    private void comparePointsToSet(CIPTAResult result, String file) {
    }
}

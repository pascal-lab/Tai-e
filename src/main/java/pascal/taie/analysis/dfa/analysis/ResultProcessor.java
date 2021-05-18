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

package pascal.taie.analysis.dfa.analysis;

import pascal.taie.analysis.IntraproceduralAnalysis;
import pascal.taie.analysis.dfa.fact.NodeResult;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.IRPrinter;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.MapUtils;
import pascal.taie.util.collection.Pair;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Special class for process the results of other analyses after they finishes.
 * This analysis should be placed after the other analyses.
 */
public class ResultProcessor extends IntraproceduralAnalysis {

    public static final String ID = "process-result";

    private Map<Pair<String, String>, Set<String>> inputs;

    private PrintStream out;

    private final boolean isDump;
    
    public ResultProcessor(AnalysisConfig config) {
        super(config);
        readInputs();
        isDump = getOptions().getString("action").equals("dump");
        if (isDump) {
            setupOut();
        }
    }

    private void readInputs() {
        String action = getOptions().getString("action");
        if (action.equals("compare")) {
            String inputFile = getOptions().getString("input-file");
            Path path = Paths.get(inputFile);
            try {
                inputs = MapUtils.newMap();
                BufferedReader reader = Files.newBufferedReader(path);
                String line;
                Pair<String, String> currentKey = null;
                while ((line = reader.readLine()) != null) {
                    Pair<String, String> key = extractKey(line);
                    if (key != null) {
                        currentKey = key;
                    } else if (!line.isEmpty()){
                        MapUtils.addToMapSet(inputs, currentKey, line);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read input file", e);
            }
        }
    }

    private static Pair<String, String> extractKey(String line) {
        if (line.startsWith("----------") && line.endsWith("----------")) {
            int ms = line.indexOf('<'); // method start
            int me = line.indexOf('>'); // method end
            String method = line.substring(ms, me + 1);
            int as = line.lastIndexOf('('); // analysis start
            int ae = line.lastIndexOf(')'); // analysis end
            String analysis = line.substring(as + 1, ae);
            return new Pair<>(method, analysis);
        } else {
            return null;
        }
    }

    private void setupOut() {
        String output = getOptions().getString("output-file");
        if (output != null) {
            try {
                out = new PrintStream(output);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Failed to open output file", e);
            }
        } else {
            out = System.out;
        }
    }

    @Override
    public Object analyze(IR ir) {
        if (isDump) {
            ((List<String>) getOptions().get("analyses")).forEach(id ->
                    dumpResult(ir, id));
        }
        return null;
    }

    private void dumpResult(IR ir, String id) {
        out.printf("-------------------- %s (%s) --------------------%n",
                ir.getMethod(), id);
        Object result = ir.getResult(id);
        if (result instanceof Set) {
            ((Set<?>) result).forEach(e -> out.println(toString(e)));
        } else if (result instanceof Map) {
            ((Map<?,?>) result).forEach((k, v) ->
                    out.println(toString(k) + "=" + toString(v)));
        } else if (result instanceof NodeResult) {
            NodeResult<Stmt, ?> nodeResult = (NodeResult<Stmt, ?>) result;
            ir.getStmts().forEach(stmt ->
                    out.println(toString(stmt) + " " +
                            toString(nodeResult.getOutFact(stmt))));
        } else {
            out.println(toString(result));
        }
        out.println();
    }

    /**
     * Converts an object to string representation.
     * Here we specially handle Stmt by calling IRPrint.toString().
     */
    private static String toString(Object o) {
        if (o instanceof Stmt) {
            return IRPrinter.toString((Stmt) o);
        } else {
            return Objects.toString(o);
        }
    }
}

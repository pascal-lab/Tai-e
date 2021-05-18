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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.InterproceduralAnalysis;
import pascal.taie.analysis.dfa.fact.NodeResult;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.IRPrinter;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.MapUtils;
import pascal.taie.util.collection.Pair;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pascal.taie.util.collection.CollectionUtils.getOne;

/**
 * Special class for process the results of other analyses after they finishes.
 * This analysis should be placed after the other analyses.
 */
public class ResultProcessor extends InterproceduralAnalysis {

    public static final String ID = "process-result";

    private static final Logger logger = LogManager.getLogger(ResultProcessor.class);

    private final String action;

    private PrintStream out;

    private Map<Pair<String, String>, Set<String>> inputs;

    private Set<String> mismatches;
    
    public ResultProcessor(AnalysisConfig config) {
        super(config);
        action = getOptions().getString("action");
        switch (action) {
            case "dump":
                setupOut();
                break;
            case "compare":
                readInputs();
                break;
        }
    }

    private void setupOut() {
        String output = getOptions().getString("file");
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

    private void readInputs() {
        String action = getOptions().getString("action");
        if (action.equals("compare")) {
            String input = getOptions().getString("file");
            Path path = Paths.get(input);
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
            int me = line.indexOf("> "); // method end
            String method = line.substring(ms, me + 1);
            int as = line.lastIndexOf('('); // analysis start
            int ae = line.lastIndexOf(')'); // analysis end
            String analysis = line.substring(as + 1, ae);
            return new Pair<>(method, analysis);
        } else {
            return null;
        }
    }

    @Override
    public Object analyze() {
        mismatches = new LinkedHashSet<>();
        processIntraResults();
        mismatches.forEach(System.out::println);
        return mismatches;
    }

    private void processIntraResults() {
        // process intra-procedural analysis results
        Stream<JMethod> methods = World.getClassHierarchy()
                .applicationClasses()
                .map(JClass::getDeclaredMethods)
                .flatMap(Collection::stream)
                .filter(m -> !m.isAbstract() && !m.isNative())
                .sorted(Comparator.comparing(m ->
                        m.getIR().getStmt(0).getLineNumber()));
        methods.forEach(m -> {
            IR ir = m.getIR();
            ((List<String>) getOptions().get("analyses")).forEach(id -> {
                switch (action) {
                    case "dump":
                        dumpResult(ir, id);
                        break;
                    case "compare":
                        compareResult(ir, id);
                        break;
                }
            });
        });
    }

    private void dumpResult(IR ir, String id) {
        out.printf("-------------------- %s (%s) --------------------%n",
                ir.getMethod(), id);
        Object result = ir.getResult(id);
        if (result instanceof Set) {
            ((Set<?>) result).forEach(e -> out.println(toString(e)));
        } else if (result instanceof NodeResult) {
            NodeResult<Stmt, ?> nodeResult = (NodeResult<Stmt, ?>) result;
            ir.getStmts().forEach(stmt ->
                    out.println(toString(stmt, nodeResult)));
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

    /**
     * Converts a stmt and its analysis result (flowing-out fact)
     * to the corresponding string representation.
     */
    private static String toString(Stmt stmt, NodeResult<Stmt, ?> result) {
        return toString(stmt) + " " + toString(result.getOutFact(stmt));
    }

    private void compareResult(IR ir, String id) {
        JMethod method = ir.getMethod();
        Set<String> inputResult = inputs.getOrDefault(
                new Pair<>(ir.getMethod().toString(), id), Set.of());
        Object result = ir.getResult(id);
        if (result instanceof Set) {
            Set<String> given = ((Set<?>) result)
                    .stream()
                    .map(ResultProcessor::toString)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            given.forEach(s -> {
                if (!inputResult.contains(s)) {
                    mismatches.add(method + " " + s  +
                            " should NOT be included");
                }
            });
            inputResult.forEach(s -> {
                if (!given.contains(s)) {
                    mismatches.add(method + " " + s +
                            " should be included");
                }
            });
        } else if (result instanceof NodeResult) {
            Set<String> lines = inputs.get(new Pair<>(method.toString(), id));
            NodeResult<Stmt, ?> nodeResult = (NodeResult<Stmt, ?>) result;
            ir.getStmts().forEach(stmt -> {
                String stmtStr = toString(stmt);
                String given = toString(stmt, nodeResult);
                for (String line : lines) {
                    if (line.startsWith(stmtStr) && !line.equals(given)) {
                        int idx = stmtStr.length();
                        mismatches.add(String.format("%s %s expected: %s, given: %s",
                                method, stmtStr, given.substring(idx + 1),
                                line.substring(idx + 1)));
                    }
                }
            });
        } else if (inputResult.size() == 1) {
            if (!toString(result).equals(getOne(inputResult))) {
                mismatches.add(String.format("%s expected: %s, given: %s",
                        method, toString(result), getOne(inputResult)));
            }
        } else {
            logger.warn("Cannot compare result of analysis {} for {}," +
                            " expected: {}, given: {}",
                    id, method, inputResult, result);
        }
    }
}

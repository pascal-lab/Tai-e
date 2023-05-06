/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.misc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.analysis.StmtResult;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.CallGraphBuilder;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IRPrinter;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.CollectionUtils;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static pascal.taie.util.collection.CollectionUtils.getOne;

/**
 * Special class for process the results of other analyses after they finish.
 * This class is designed mainly for testing purpose. Currently, it supports
 * input/output analysis results from/to file, and compare analysis results
 * with input results. This analysis should be specified as the last analysis.
 */
public class ResultProcessor extends ProgramAnalysis<Set<String>> {

    public static final String ID = "process-result";

    private static final Logger logger = LogManager.getLogger(ResultProcessor.class);

    private final boolean onlyApp;

    private final String action;

    private PrintStream out;

    private Map<Pair<String, String>, List<String>> inputs;

    private Set<String> mismatches;

    public ResultProcessor(AnalysisConfig config) {
        super(config);
        onlyApp = getOptions().getBoolean("only-app");
        action = getOptions().getString("action");
    }

    @Override
    public Set<String> analyze() {
        // initialization
        switch (action) {
            case "dump" -> setOutput();
            case "compare" -> readInputs();
        }
        mismatches = Sets.newLinkedSet();
        // Classify given analysis IDs into two groups,
        // one for ProgramAnalysis if present in the World,
        // and another for Class/MethodAnalysis otherwise.
        @SuppressWarnings("unchecked")
        List<String> analyses = (List<String>) getOptions().get("analyses");
        Map<Boolean, List<String>> groups = analyses.stream()
                .collect(Collectors.groupingBy(World.get()::hasResult));
        List<String> programAnalyses = groups.get(true);
        if (programAnalyses != null) {
            processProgramAnalysisResult(programAnalyses);
        }
        List<String> classMethodAnalyses = groups.get(false);
        if (classMethodAnalyses != null) {
            processClassMethodAnalysisResult(classMethodAnalyses);
        }
        if (getOptions().getBoolean("log-mismatches")) {
            mismatches.forEach(logger::info);
        }
        // close out stream
        if (action.equals("dump") && out != System.out) {
            out.close();
        }
        return mismatches;
    }

    private void setOutput() {
        String output = getOptions().getString("action-file");
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
        String input = getOptions().getString("action-file");
        Path path = Path.of(input);
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            inputs = Maps.newLinkedHashMap();
            String line;
            Pair<String, String> currentKey = null;
            while ((line = reader.readLine()) != null) {
                Pair<String, String> key = extractKey(line);
                if (key != null) {
                    currentKey = key;
                } else if (!line.isBlank()) {
                    assert currentKey != null;
                    inputs.computeIfAbsent(currentKey, __ -> new ArrayList<>())
                            .add(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read input file", e);
        }
    }

    private static Pair<String, String> extractKey(String line) {
        if (line.startsWith("----------") && line.endsWith("----------")) {
            int es = line.indexOf('<'); // entity (method) start
            int ee = line.lastIndexOf('>'); // entity (method) end
            if (es == -1 && ee == -1) { // method start/end not found,
                // so this should be a class analysis
                es = line.indexOf(' ') + 1; // entity (class) start
                ee = line.indexOf(' ', es) - 1; // entity (class) end
            }
            String entity = line.substring(es, ee + 1);
            int as = line.lastIndexOf('('); // analysis ID start
            int ae = line.lastIndexOf(')'); // analysis ID end
            String analysis = line.substring(as + 1, ae);
            return new Pair<>(entity, analysis);
        } else {
            return null;
        }
    }

    /**
     * Compares methods by their declaring classes and source code position.
     */
    private static final Comparator<JMethod> methodComp = (m1, m2) -> {
        if (m1.getDeclaringClass().equals(m2.getDeclaringClass())) {
            return m1.getIR().getStmt(0).getLineNumber() -
                    m2.getIR().getStmt(0).getLineNumber();
        } else {
            return m1.getDeclaringClass().toString()
                    .compareTo(m2.getDeclaringClass().toString());
        }
    };

    private void processProgramAnalysisResult(List<String> analyses) {
        // TODO: support class-level analysis?
        CallGraph<?, JMethod> cg = World.get().getResult(CallGraphBuilder.ID);
        List<JMethod> methods = cg.reachableMethods()
                .filter(m -> !onlyApp || m.isApplication())
                .sorted(methodComp)
                .toList();
        processResults(methods, analyses, (m, id) -> World.get().getResult(id));
    }

    private void processClassMethodAnalysisResult(List<String> analyses) {
        List<JClass> classes = World.get().getClassHierarchy()
                .allClasses()
                .filter(c -> !onlyApp || c.isApplication())
                .sorted(Comparator.comparing(JClass::toString))
                .toList();
        // Classify given analysis IDs into two groups,
        // one for ClassAnalysis if present in an arbitrarily-picked class c,
        // and another for MethodAnalysis otherwise.
        JClass c = CollectionUtils.getOne(classes);
        Map<Boolean, List<String>> groups = analyses.stream()
                .collect(Collectors.groupingBy(c::hasResult));
        List<String> classAnalyses = groups.get(true);
        if (classAnalyses != null) {
            processResults(classes, classAnalyses, JClass::getResult);
        }
        List<String> methodAnalyses = groups.get(false);
        if (methodAnalyses != null) {
            List<JMethod> methods = classes.stream()
                    .map(JClass::getDeclaredMethods)
                    .flatMap(Collection::stream)
                    .filter(m -> !m.isAbstract())
                    .sorted(methodComp)
                    .toList();
            processResults(methods, methodAnalyses,
                    (m, id) -> m.getIR().getResult(id));
        }
    }

    private <E> void processResults(List<E> entities, List<String> analyses,
                                    BiFunction<E, String, ?> resultGetter) {
        Set<Pair<String, String>> processed = Sets.newSet();
        for (E entity : entities) {
            for (String id : analyses) {
                switch (action) {
                    case "dump" -> dumpResult(entity, id, resultGetter);
                    case "compare" -> compareResult(entity, id, resultGetter);
                }
                processed.add(new Pair<>(entity.toString(), id));
            }
        }
        if (action.equals("compare")) {
            // check whether expected analysis results of some methods
            // are absent in given results.
            for (var key : inputs.keySet()) {
                if (!processed.contains(key)) {
                    mismatches.add(String.format("Expected \"%s\" result of %s" +
                                    " is absent in given results",
                            key.second(), key.first()));
                }
            }
        }
    }

    private <E> void dumpResult(
            E entity, String id, BiFunction<E, String, ?> resultGetter) {
        out.printf("-------------------- %s (%s) --------------------%n", entity, id);
        Object result = resultGetter.apply(entity, id);
        if (result instanceof Collection<?> c) {
            c.forEach(e -> out.println(toString(e)));
        } else if (result instanceof StmtResult<?> stmtResult) {
            JMethod method = (JMethod) entity;
            method.getIR()
                    .stmts()
                    .filter(stmtResult::isRelevant)
                    .forEach(stmt -> out.println(toString(stmt, stmtResult)));
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
        if (o instanceof Stmt s) {
            return IRPrinter.toString(s);
        } else if (o instanceof Collection<?> c) {
            return CollectionUtils.toString(c);
        } else {
            return Objects.toString(o);
        }
    }

    /**
     * Converts a stmt and its analysis result to the corresponding
     * string representation.
     */
    private static String toString(Stmt stmt, StmtResult<?> result) {
        return toString(stmt) + " " + toString(result.getResult(stmt));
    }

    private <E> void compareResult(
            E entity, String id, BiFunction<E, String, ?> resultGetter) {
        List<String> inputResult = inputs.getOrDefault(
                new Pair<>(entity.toString(), id), List.of());
        Object result = resultGetter.apply(entity, id);
        if (result instanceof Collection<?> c) {
            Set<String> given = c.stream()
                    .map(ResultProcessor::toString)
                    .collect(Collectors.toCollection(Sets::newLinkedSet));
            given.forEach(s -> {
                if (!inputResult.contains(s)) {
                    mismatches.add(entity + " " + s +
                            " should NOT be included");
                }
            });
            inputResult.forEach(s -> {
                if (!given.contains(s)) {
                    mismatches.add(entity + " " + s +
                            " should be included");
                }
            });
        } else if (result instanceof StmtResult<?> stmtResult) {
            JMethod method = (JMethod) entity;
            List<String> lines = inputs.getOrDefault(
                    new Pair<>(method.toString(), id), List.of());
            for (Stmt stmt : method.getIR()) {
                if (stmtResult.isRelevant(stmt)) {
                    String stmtStr = toString(stmt);
                    String given = toString(stmt, stmtResult);
                    boolean foundExpected = false;
                    for (String line : lines) {
                        if (line.startsWith(stmtStr)) {
                            foundExpected = true;
                            if (!line.equals(given)) {
                                int idx = stmtStr.length();
                                mismatches.add(String.format("%s %s expected: %s, given: %s",
                                        method, stmtStr, line.substring(idx + 1),
                                        given.substring(idx + 1)));
                            }
                        }
                    }
                    if (!foundExpected) {
                        int idx = stmtStr.length();
                        mismatches.add(String.format("%s %s expected: null, given: %s",
                                method, stmtStr, given.substring(idx + 1)));
                    }
                }
            }
        } else if (inputResult.size() == 1) {
            if (!toString(result).equals(getOne(inputResult))) {
                mismatches.add(String.format("%s expected: %s, given: %s",
                        entity, getOne(inputResult), toString(result)));
            }
        } else {
            logger.warn("Cannot compare result of analysis {} for {}," +
                            " expected: {}, given: {}",
                    id, entity, inputResult, result);
        }
    }
}

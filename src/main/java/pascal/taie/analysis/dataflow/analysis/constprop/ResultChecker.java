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

package pascal.taie.analysis.dataflow.analysis.constprop;

import soot.Body;
import soot.G;
import soot.Local;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.jimple.NopStmt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Boosts constant propagation and checks whether the analysis result
 * is correct by comparing it with prepared expected result.
 */
public class ResultChecker {

    // ---------- static members ----------
    /**
     * The current result checker
     */
    private static ResultChecker checker;
    private final Set<String> mismatches = new TreeSet<>();
    // ---------- instance members ----------
    private Map<String, Map<Integer, Map<String, String>>> expectedResult;

    ResultChecker(Path filePath) {
        readExpectedResult(filePath);
    }

    private static void setChecker(ResultChecker checker) {
        ResultChecker.checker = checker;
    }

    public static boolean isAvailable() {
        return checker != null;
    }

    public static ResultChecker get() {
        return checker;
    }

    /**
     * The entry function of whole checking mechanism.
     *
     * @param args the arguments for running Soot
     * @param path the path string of the expected result file
     * @return the mismatched information in form of set of strings
     */
    public static Set<String> check(String[] args, String path) {
        ResultChecker checker = new ResultChecker(Paths.get(path));
        setChecker(checker);
        ConstantPropagation.setOutput(false);

        G.reset(); // reset the whole Soot environment
        Main.main(args);
        return checker.getMismatches();
    }

    Set<String> getMismatches() {
        return mismatches;
    }

    /**
     * Compares the analysis result with expected result, and stores
     * any found mismatches.
     */
    public void compare(Body body, Map<Unit, FlowMap> analysisResult) {
        String method = body.getMethod().getSignature();
        UnitPatchingChain units = body.getUnits();
        units.forEach(u -> {
            int lineNumber = u.getJavaSourceStartLineNumber();
            if (!(u instanceof NopStmt) && isLastUnitOfItsLine(units, u)) {
                // only compare the data flow at the last unit of the same line
                doCompare(method, lineNumber, analysisResult.get(u));
            }
        });
    }

    /**
     * Returns if the given unit is the last unit of its source code line.
     * TODO - make this more robust
     */
    private boolean isLastUnitOfItsLine(UnitPatchingChain units, Unit unit) {
        Unit succ = units.getSuccOf(unit);
        return succ == null
                || succ.getJavaSourceStartLineNumber() != unit.getJavaSourceStartLineNumber();
    }

    /**
     * Compares the analysis result with expected result at specific method
     * and specific line. If the values do not match, then adds relevant
     * mismatch information to mismatches.
     */
    private void doCompare(String method, int lineNumber, FlowMap analysisResult) {
        Map<String, String> expectedResult = getValuesAt(method, lineNumber);
        // This comparison should be linear, but now it is quadratic.
        // But the computation amount is very small, so it should be find.
        expectedResult.forEach((var, expected) -> {
            boolean found = false;
            for (Map.Entry<Local, Value> entry : analysisResult.entrySet()) {
                if (entry.getKey().getName().equals(var)) {
                    found = true;
                    Value value = entry.getValue();
                    if (!value.toString().equals(expected)) {
                        mismatches.add(String.format(
                                "%n%s:L%d, '%s', expected: %s, given: %s",
                                method, lineNumber, var, expected, value.toString()));
                    }
                }
            }
            if (!found && !expected.equals("UNDEF")) {
                // An expected non-undefined value of var is absent (undefined)
                // in analysis result, which is also a mismatch.
                mismatches.add(String.format(
                        "%n%s:L%d, '%s', expected: %s, given: UNDEF",
                        method, lineNumber, var, expected));
            }
        });
    }

    /**
     * Reads expected result from given file path.
     */
    private void readExpectedResult(Path filePath) {
        expectedResult = new TreeMap<>();
        String currentMethod = null;  // method signature
        try {
            for (String line : Files.readAllLines(filePath)) {
                if (isMethodSignature(line)) {
                    currentMethod = line;
                } else if (!isEmpty(line)) {
                    parseLine(currentMethod, line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + filePath
                    + " caused by " + e);
        }
    }

    private boolean isMethodSignature(String line) {
        return line.startsWith("<");
    }

    private boolean isEmpty(String line) {
        return line.trim().equals("");
    }

    /**
     * Parses expected from a line with format:
     * L:X=V,X'=V',... where
     * L is line number
     * X is variable number
     * V is expected value
     */
    private void parseLine(String method, String line) {
        String[] splits = line.split(":");
        int lineNumber = Integer.parseInt(splits[0]);
        String[] pairs = splits[1].split(",");
        for (String pair : pairs) {
            String[] varValue = pair.split("=");
            String var = varValue[0];
            String value = varValue[1];
            addValue(method, lineNumber, var, value);
        }
    }

    private void addValue(String method, int lineNumber, String var, String value) {
        String old = expectedResult.computeIfAbsent(method, (k) -> new TreeMap<>())
                .computeIfAbsent(lineNumber, (k) -> new TreeMap<>())
                .put(var, value);
        if (old != null) {
            throw new RuntimeException("Value of " + method
                    + ":" + lineNumber
                    + ":" + var + " already exists");
        }
    }

    /**
     * Obtains the var-value map at specific location.
     */
    private Map<String, String> getValuesAt(String method, int lineNumber) {
        return expectedResult.getOrDefault(method, Collections.emptyMap())
                .getOrDefault(lineNumber, Collections.emptyMap());
    }
}

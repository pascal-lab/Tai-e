/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.panda.pta.core.ci;

import pascal.panda.util.StringUtils;
import soot.G;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Boosts context-insensitive pointer analysis and checks whether the
 * analysis result is correct by comparing it with prepared expected result.
 */
public class ResultChecker {

    // ---------- static members ----------
    /**
     * The current result checker
     */
    private static ResultChecker checker;
    private final Set<String> mismatches = new TreeSet<>();
    /**
     * Map from pointer to its points-to set
     */
    private Map<String, String> expectedResults;

    ResultChecker(Path filePath) {
        readExpectedResult(filePath);
    }

    private static void setChecker(ResultChecker checker) {
        ResultChecker.checker = checker;
    }

    // ---------- instance members ----------

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
        PointerAnalysisTransformer.v().setOutput(false);

        G.reset(); // reset the whole Soot environment
        Main.main(args);
        return checker.getMismatches();
    }

    public Set<String> getMismatches() {
        return mismatches;
    }

    public void compare(PointerAnalysis pta) {
        Set<String> givenPointers = new TreeSet<>();
        pta.getVariables()
                .sorted(Comparator.comparing(Var::toString))
                .forEach(p -> comparePointer(p, givenPointers));
        pta.getInstanceFields()
                .sorted(Comparator.comparing(f -> f.getBase().toString()))
                .forEach(f -> comparePointer(f, givenPointers));
        expectedResults.forEach((p, pts) -> {
            if (!givenPointers.contains(p)) {
                mismatches.add(String.format(
                        "\n %s, expected: %s, given: pointer has not been added to PFG", p, pts));
            }
        });
    }

    private void comparePointer(Pointer p, Set<String> givenPointers) {
        String ptr = p.toString();
        String given = StringUtils.streamToString(p.getPointsToSet().stream());
        String expected = expectedResults.get(ptr);
        if (!Objects.equals(given, expected)) {
            mismatches.add(String.format(
                    "\n %s, expected: %s, given: %s",
                    ptr, expected, given));
        }
        givenPointers.add(ptr);
    }

    /**
     * Reads expected result from given file path.
     */
    private void readExpectedResult(Path filePath) {
        expectedResults = new TreeMap<>();
        try {
            for (String line : Files.readAllLines(filePath)) {
                if (isPointsToSet(line)) {
                    String[] splits = line.split(" -> ");
                    String pointer = splits[0];
                    String pts = splits[1];
                    expectedResults.put(pointer, pts);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + filePath
                    + " caused by " + e);
        }
    }

    private boolean isPointsToSet(String line) {
        return line.startsWith("<");
    }
}

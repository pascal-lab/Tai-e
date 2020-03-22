package sa.dataflow.analysis.constprop;

import soot.Body;
import soot.G;
import soot.Unit;
import soot.UnitPatchingChain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Boosts constant propagation and checks whether the analysis result
 * is correct by comparing it with prepared expected result.
 */
class ResultChecker {

    // ---------- static members ----------
    /**
     * The current result checker
     */
    private static ResultChecker checker;

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
     * @param args the arguments for running Soot
     * @param path the path string of the expected result file
     * @return the mismatched information in form of set of strings
     */
    static Set<String> check(String[] args, String path) {
        ResultChecker checker = new ResultChecker(Paths.get(path));
        setChecker(checker);

        G.reset(); // reset the whole Soot environment
        Main.main(args);
        return checker.getMismatches();
    }

    // ---------- instance members ----------
    private Map<String, Map<Integer, Map<String, String>>> resultMap;

    private Set<String> mismatches = new TreeSet<>();

    ResultChecker(Path filePath) {
        readExpectedResult(filePath);
    }

    Set<String> getMismatches() {
        return mismatches;
    }

    Map<String, Map<Integer, Map<String, String>>> getResultMap() {
        return resultMap;
    }

    /**
     * Compares the analysis result with expected result, and stores
     * any found mismatches.
     */
    public void compare(Body body, Map<Unit, FlowMap> result) {
        String method = body.getMethod().getSignature();
        UnitPatchingChain units = body.getUnits();
        units.forEach(u -> {
            int lineNumber = u.getJavaSourceStartLineNumber();
            if (isLastUnitOfItsLine(units, u)) {
                // only compare the data flow at the last unit of the same line
                doCompare(method, lineNumber, result.get(u));
            }
        });
    }

    /**
     * Returns if the given unit is the last unit of its source code line.
     */
    private boolean isLastUnitOfItsLine(UnitPatchingChain units, Unit unit) {
        Unit succ = units.getSuccOf(unit);
        return succ == null ||
                succ.getJavaSourceStartLineNumber() != unit.getJavaSourceStartLineNumber();
    }

    /**
     * Compares the analysis result with expected result at specific method
     * and specific line. If the values do not match, then adds relevant
     * mismatch information to mismatches.
     */
    private void doCompare(String method, int lineNumber,
                           FlowMap analysisResult) {
        analysisResult.forEach((local, value) -> {
            String var = local.getName();
            String expected = getValue(method, lineNumber, var);
            if (expected != null) {
                if (!value.toString().equals(expected)) {
                    mismatches.add(String.format(
                            "\n%s:L%d, '%s', expected: %s, given: %s",
                            method, lineNumber, var, expected, value.toString()));
                }
            }
        });
    }

    /**
     * Reads expected result from given file path.
     */
    private void readExpectedResult(Path filePath) {
        resultMap = new HashMap<>();
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
        String old = resultMap.computeIfAbsent(method, (k) -> new HashMap<>())
                .computeIfAbsent(lineNumber, (k) -> new HashMap<>())
                .put(var, value);
        if (old != null) {
            throw new RuntimeException("Value of " + method
                    + ":" + lineNumber
                    + ":" + var + " already exists");
        }
    }

    /**
     * Gets expected value of specific method, line, and variable name.
     * If var does not exist in result map, e.g., var is temporary variable,
     * then returns null.
     */
    private String getValue(String method, int lineNumber, String var) {
        return resultMap.getOrDefault(method, Collections.emptyMap())
                .getOrDefault(lineNumber, Collections.emptyMap())
                .get(var);
    }
}

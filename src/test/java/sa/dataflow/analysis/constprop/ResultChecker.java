package sa.dataflow.analysis.constprop;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Checks whether the analysis result is correct by comparing it
 * with prepared expected result.
 */
public class ResultChecker {

    ExpectedResult readExpectedResult(Path filePath) {
        ExpectedResult result = new ExpectedResult();
        String currentMethod = null;  // method signature
        try {
            for (String line : Files.readAllLines(filePath)) {
                if (isMethodSignature(line)) {
                    currentMethod = line;
                } else if (!isEmpty(line)) {
                    parseLine(result, currentMethod, line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + filePath
                    + " caused by " + e);
        }
        return result;
    }

    private boolean isMethodSignature(String line) {
        return line.startsWith("<");
    }

    private boolean isEmpty(String line) {
        return line.trim().equals("");
    }

    private void parseLine(ExpectedResult result, String method, String line) {
        String[] splits = line.split(":");
        int lineNumber = Integer.parseInt(splits[0]);
        String[] pairs = splits[1].split(",");
        for (String pair : pairs) {
            String[] varValue = pair.split("=");
            String var = varValue[0];
            String value = varValue[1];
            result.addValue(method, lineNumber, var, value);
        }
    }

    class ExpectedResult {

        private Map<String, Map<Integer, Map<String, String>>> map =
                new HashMap<>();

        void addValue(String method, int lineNumber, String var, String value) {
            String old = map.computeIfAbsent(method, (k) -> new HashMap<>())
                    .computeIfAbsent(lineNumber, (k) -> new HashMap<>())
                    .put(var, value);
            if (old != null) {
                throw new RuntimeException("Value of " + method
                        + ":" + lineNumber
                        + ":" + var + " already exists");
            }
        }

        String getValue(String method, int lineNumber, String var) {
            return map.getOrDefault(method, Collections.emptyMap())
                    .getOrDefault(lineNumber, Collections.emptyMap())
                    .get(var);
        }

        Map<String, Map<Integer, Map<String, String>>> getMap() {
            return map;
        }
    }
}

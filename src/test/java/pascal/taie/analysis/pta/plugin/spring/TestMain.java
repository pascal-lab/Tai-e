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

package pascal.taie.analysis.pta.plugin.spring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import pascal.taie.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMain {

    /**
     * Whether generate expected results or not.
     */
    private static final boolean GENERATE_EXPECTED_RESULTS = false;

    private static final String SPRING_TEST_DIR = "src/test/resources/spring/";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String TESTCASE_DIR = SPRING_TEST_DIR + "testcase/";
    private static final String LIB_DIR = SPRING_TEST_DIR + "lib";
    private static final String GROUND_TRUTH_DIR = SPRING_TEST_DIR + "groundtruth/";


    enum TestKind {
        DI("di", pascal.taie.analysis.pta.plugin.spring.di.DiResultProcessor.RESULT_FILE_NAME),
        WEC("wec", pascal.taie.analysis.pta.plugin.spring.wec.WecResultProcessor.RESULT_FILE_NAME);

        private final String dir;
        private final String metaExpectedFileName;

        TestKind(String dir, String metaExpectedFileName) {
            this.dir = dir;
            this.metaExpectedFileName = metaExpectedFileName;
        }

        public String getDir() {
            return dir;
        }

        public String getMetaExpectedFileName() {
            return metaExpectedFileName;
        }
    }


    public static void testDi(String testcaseName) {
        testDi(testcaseName, "1-obj");
    }

    public static void testDi(String testcaseName, String context) {
        testPTA(TestKind.DI, testcaseName, context);
    }

    public static void testWec(String testcaseName) {
        testWec(testcaseName, "1-obj");
    }

    public static void testWec(String testcaseName, String context) {
        testPTA(TestKind.WEC, testcaseName, context);
    }

    private static void testPTA(TestKind testKind, String testcaseName, String context) {
        String testCasePathSuffix = testKind.getDir() + "/" + testcaseName;

        // prepare args
        List<String> args = new ArrayList<>();

        // add misc options
        Collections.addAll(args, "-java", "8");
        Collections.addAll(args, "--output-dir", "output/" + testCasePathSuffix);

        // add -acp and -cp options
        String acp = TESTCASE_DIR + testCasePathSuffix;
        Collections.addAll(args, "-acp", acp);
        Collections.addAll(args, "-cp", LIB_DIR);

        String ptaExpectedFileName = pascal.taie.analysis.pta.plugin.ResultProcessor.RESULTS_FILE;
        Path ptaExpectedFilePath = Path.of(GROUND_TRUTH_DIR, testCasePathSuffix, ptaExpectedFileName);

        String metaExpectedFileName = testKind.getMetaExpectedFileName();
        Path metaExpectedFilePath = Path.of(GROUND_TRUTH_DIR, testCasePathSuffix, metaExpectedFileName);

        Collections.addAll(args,
                "-a", """
                        pta=
                        implicit-entries:false;
                        distinguish-string-constants:app;
                        dump:true;
                        expected-file:%s;
                        only-app:true;
                        handle-invokedynamic:true;
                        reflection-inference:null;
                        cs:%s;
                        spring:true;
                        time-limit:86400;
                        """.formatted(
                                GENERATE_EXPECTED_RESULTS ? null : ptaExpectedFilePath, context),
                "-a", """
                        cg=
                        algorithm:pta;
                        dump-methods:true;
                        dump-call-edges:true;
                        """
        );
        // run!
        boolean dumpResults = SpringAnalysis.DUMP_RESULTS;
        SpringAnalysis.DUMP_RESULTS = true;
        try {
            pascal.taie.Main.main(args.toArray(new String[0]));
        } finally {
            SpringAnalysis.DUMP_RESULTS = dumpResults;
        }

        Path outputDir = World.get().getOptions().getOutputDir().toPath();
        if (GENERATE_EXPECTED_RESULTS) {
            moveOutputFile(outputDir, ptaExpectedFileName, ptaExpectedFilePath);
            moveOutputFile(outputDir, metaExpectedFileName, metaExpectedFilePath);
        } else {
            compareJson(testKind, outputDir.resolve(metaExpectedFileName), metaExpectedFilePath);
        }
    }

    private static void moveOutputFile(Path outputDir, String fileName,
                                       Path expectedFilePath) {
        try {
            Path from = outputDir.resolve(fileName);
            var ignore = expectedFilePath.getParent().toFile().mkdirs();
            Files.move(from, expectedFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void compareJson(TestKind testKind, Path actualFile,
                                    Path expectedFile) {
        try {
            JsonNode actual = normalize(testKind, OBJECT_MAPPER.readTree(actualFile.toFile()));
            JsonNode expected = normalize(testKind, OBJECT_MAPPER.readTree(expectedFile.toFile()));
            assertEquals(expected, actual,
                    "Unexpected Spring metadata in " + actualFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static JsonNode normalize(TestKind testKind, JsonNode json) {
        if (!json.isArray()) {
            return json;
        }
        List<JsonNode> nodes = new ArrayList<>();
        json.forEach(nodes::add);
        ArrayNode result = OBJECT_MAPPER.createArrayNode();
        nodes.stream()
                .sorted((n1, n2) -> metadataSortKey(testKind, n1)
                        .compareTo(metadataSortKey(testKind, n2)))
                .forEach(result::add);
        return result;
    }

    private static String metadataSortKey(TestKind testKind, JsonNode json) {
        return switch (testKind) {
            case DI -> {
                JsonNode factoryMethod = json.get("factoryMethodSignature");
                if (factoryMethod != null && !factoryMethod.isNull()) {
                    yield factoryMethod.asText();
                }
                JsonNode beanNames = json.get("beanNames");
                yield beanNames != null && beanNames.isArray() && !beanNames.isEmpty()
                        ? beanNames.get(0).asText()
                        : "";
            }
            case WEC -> json.path("containingClass").asText()
                    + json.path("handlerMethod").asText();
        };
    }

}

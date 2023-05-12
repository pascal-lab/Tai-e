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

package pascal.taie.analysis.pta.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.taint.TaintFlow;
import pascal.taie.config.AnalysisOptions;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.Lists;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Streams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pascal.taie.util.collection.CollectionUtils.sum;

/**
 * Dump points-to set to file or compare the analysis result with
 * the ones read from input file.
 * Currently, the compare functionality is mainly for testing purpose.
 * It is not efficient and not recommended applying on large program.
 */
public class ResultProcessor implements Plugin {

    private static final Logger logger = LogManager.getLogger(ResultProcessor.class);

    public static final String RESULTS_FILE = "pta-results.txt";

    public static final String RESULTS_YAML_FILE = "pta-results.yml";

    private static final String CI_RESULTS_FILE = "pta-ci-results.txt";

    private static final String HEADER = "Points-to sets of all ";

    /**
     * Separator between pointer and its points-to set.
     */
    private static final String SEP = " -> ";

    private static final DecimalFormat formatter = new DecimalFormat("#,####");

    private Solver solver;

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
    }

    @Override
    public void onFinish() {
        process(solver.getOptions(), solver.getResult());
    }

    public static void process(AnalysisOptions options,
                               PointerAnalysisResult result) {
        logStatistics(result);

        boolean taintEnabled = options.getString("taint-config") != null;
        if (options.getBoolean("dump")) {
            dumpPointsToSet(result, taintEnabled);
        }

        if (options.getBoolean("dump-ci")) {
            dumpCIPointsToSet(result);
        }

        if (options.getBoolean("dump-yaml")) {
            dumpPointsToSetInYaml(result);
        }

        String expectedFile = options.getString("expected-file");
        if (expectedFile != null) {
            if (taintEnabled) {
                // when taint analysis is enabled, we only compare
                // detected taint flows
                compareTaintFlows(result, expectedFile);
            } else {
                comparePointsToSet(result, expectedFile);
            }
        }
    }

    private static void logStatistics(PointerAnalysisResult result) {
        ToIntFunction<Pointer> getSize = p -> p.getObjects().size();
        logger.info("-------------- Pointer analysis statistics: --------------");
        int varInsens = result.getVars().size();
        int varSens = result.getCSVars().size();
        logger.info(String.format("%-30s%s (insens) / %s (sens)", "#var pointers:",
                format(varInsens), format(varSens)));
        int objInsens = result.getObjects().size();
        int objSens = result.getCSObjects().size();
        logger.info(String.format("%-30s%s (insens) / %s (sens)", "#objects:",
                format(objInsens), format(objSens)));
        long vptSizeInsens = sum(result.getVars(), v -> result.getPointsToSet(v).size());
        long vptSizeSens = sum(result.getCSVars(), getSize);
        logger.info(String.format("%-30s%s (insens) / %s (sens)", "#var points-to:",
                format(vptSizeInsens), format(vptSizeSens)));
        long sfptSizeSens = sum(result.getStaticFields(), getSize);
        logger.info(String.format("%-30s%s (sens)", "#static field points-to:",
                format(sfptSizeSens)));
        long ifptSizeSens = sum(result.getInstanceFields(), getSize);
        logger.info(String.format("%-30s%s (sens)", "#instance field points-to:",
                format(ifptSizeSens)));
        long aptSizeSens = sum(result.getArrayIndexes(), getSize);
        logger.info(String.format("%-30s%s (sens)", "#array points-to:",
                format(aptSizeSens)));
        int reachableInsens = result.getCallGraph().getNumberOfMethods();
        int reachableSens = result.getCSCallGraph().getNumberOfMethods();
        logger.info(String.format("%-30s%s (insens) / %s (sens)", "#reachable methods:",
                format(reachableInsens), format(reachableSens)));
        long callEdgeInsens = result.getCallGraph().edges().count();
        long callEdgeSens = result.getCSCallGraph().edges().count();
        logger.info(String.format("%-30s%s (insens) / %s (sens)", "#call graph edges:",
                format(callEdgeInsens), format(callEdgeSens)));
        logger.info("----------------------------------------");
    }

    private static String format(long i) {
        return formatter.format(i);
    }

    private static void dumpPointsToSet(PointerAnalysisResult result,
                                        boolean taintEnabled) {
        File outFile = new File(World.get().getOptions().getOutputDir(), RESULTS_FILE);
        try (PrintStream out = new PrintStream(new FileOutputStream(outFile))) {
            logger.info("Dumping points-to set (with contexts) to {}",
                    outFile.getAbsolutePath());
            dumpPointers(out, result.getCSVars(), "variables");
            dumpPointers(out, result.getStaticFields(), "static fields");
            dumpPointers(out, result.getInstanceFields(), "instance fields");
            dumpPointers(out, result.getArrayIndexes(), "array indexes");
            if (taintEnabled) {
                dumpTaintFlows(out, result);
            }
        } catch (FileNotFoundException e) {
            logger.error("Failed to open output file {}", outFile);
        }
    }

    private static void dumpPointers(
            PrintStream out, Collection<? extends Pointer> pointers, String desc) {
        out.println(HEADER + desc);
        pointers.stream()
                .sorted(Comparator.comparing(Pointer::toString))
                .forEach(p -> out.println(p + SEP + Streams.toString(p.objects())));
        out.println();
    }

    private static void dumpPointsToSetInYaml(PointerAnalysisResult result) {
        File outFile = new File(World.get().getOptions().getOutputDir(), RESULTS_YAML_FILE);
        logger.info("Dumping points-to set (with contexts) in YAML to {}",
                outFile.getAbsolutePath());

        // some local useful functions
        Function<Pointer, List<String>> getObjs = p -> p.getObjects().stream()
                .map(CSObj::toString).sorted().toList();
        Comparator<Obj> objComparator = Comparator.comparing((Obj o) -> o.getContainerMethod()
                .map(JMethod::getSignature).orElse(""))
                        .thenComparing(Obj::toString);

        // prepare variables in YAML format like:
        // variables:
        //  "<A: A m()>":
        //    - var: "$r1"
        //      pts:
        //        - context: "[]"
        //          objects:
        //            - "[]:NewObj{<A: A m()>[0@L1] new A}"
        final var variables = result.getCSVars()
                .stream()
                .collect(Collectors.groupingBy(csVar -> csVar.getVar().getMethod().getSignature(),
                        Maps::newOrderedMap,
                        Collectors.collectingAndThen(
                                Collectors.groupingBy(csVar -> csVar.getVar().getName(),
                                        Maps::newOrderedMap,
                                        Collectors.toMap(csVar -> csVar.getContext().toString(),
                                                getObjs,
                                                (o1, o2) -> o1, Maps::newOrderedMap)),
                                m -> m.entrySet()
                                      .stream()
                                      .map(e1 -> Maps.ofLinkedHashMap(
                                              "var", e1.getKey(),
                                              "pts", e1.getValue()
                                                       .entrySet()
                                                       .stream().map(e2 -> Maps.ofLinkedHashMap(
                                                              "context", e2.getKey(),
                                                              "objects", e2.getValue())
                                                      ).toList())
                                      ).toList()
                        ))
                );

        // prepare static fields in YAML format like:
        // static-fields:
        //  "<A>":
        //    - field: "<A: java.lang.String sField>"
        //      objects:
        //        - "[]:NewObj{<A: A m()>[0@L1] new String}"
        final var staticFields = result.getStaticFields()
                .stream()
                .collect(Collectors.groupingBy(sField -> sField.getField().getDeclaringClass().getName(),
                        Maps::newOrderedMap,
                        Collectors.mapping(sField -> Maps.ofLinkedHashMap(
                                "field", sField.getField().toString(),
                                "objects", getObjs.apply(sField)),
                                Collectors.toList()))
                );

        // prepare instance fields in YAML format like:
        // instance-fields:
        //  "NewObj{<A: A m()>[0@L1] new A}":
        //    - field: "<A: java.lang.String iField>"
        //      pts:
        //        - context: "[]"
        //          objects:
        //            - "[]:NewObj{<A: A m()>[0@L1] new String}"
        final var instanceFields = result.getInstanceFields()
                .stream()
                .collect(Collectors.groupingBy(iField -> iField.getBase().getObject(),
                                () -> Maps.newOrderedMap(objComparator),
                                Collectors.collectingAndThen(
                                        Collectors.groupingBy(iField -> iField.getField().toString(),
                                                Maps::newOrderedMap,
                                                Collectors.toMap(iField -> iField.getBase().getContext().toString(),
                                                        getObjs, (o1, o2) -> o1, Maps::newOrderedMap)),
                                        m -> m.entrySet()
                                              .stream()
                                              .map(e1 -> Maps.ofLinkedHashMap(
                                                      "field", e1.getKey(),
                                                      "pts", e1.getValue()
                                                               .entrySet()
                                                               .stream().map(e2 -> Maps.ofLinkedHashMap(
                                                                      "context", e2.getKey(),
                                                                      "objects", e2.getValue())
                                                              ).toList())
                                              ).toList())
                        )
                );

        // prepare array indexes in YAML format like:
        // array-indexes:
        //  "NewObj{<A: A m()>[0@L1] newarray java.lang.String[%intconst1]}":
        //    - context: "[]"
        //      objects:
        //        - "ConstantObj{java.lang.String: \"hello\"}"
        final var arrayIndexes = result.getArrayIndexes()
                .stream()
                .collect(Collectors.groupingBy(ai -> ai.getArray().getObject(),
                             () -> Maps.newOrderedMap(objComparator),
                             Collectors.collectingAndThen(
                                     Collectors.toMap(ai -> ai.getArray().getContext().toString(), getObjs),
                                     m -> m.entrySet()
                                           .stream()
                                           .map(e -> Maps.ofLinkedHashMap(
                                                   "context", e.getKey(),
                                                   "objects", e.getValue())
                                           ).toList()
                             )
                     )
                );

        // dump to file
        Map<String, Object> dumpData = Maps.newLinkedHashMap();
        dumpData.put("variables", variables);
        dumpData.put("static-fields", staticFields);
        dumpData.put("instance-fields", instanceFields);
        dumpData.put("array-indexes", arrayIndexes);
        try (FileWriter writer = new FileWriter(outFile)) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory()
                    .enable(YAMLGenerator.Feature.INDENT_ARRAYS)
                    .enable(YAMLGenerator.Feature.ALLOW_LONG_KEYS)
                    .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR)
                    .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                    .disable(YAMLGenerator.Feature.SPLIT_LINES)
            );
            mapper.writeValue(writer, dumpData);
        } catch (IOException e) {
            logger.error("Failed to open output file {}", outFile);
        }
    }

    /**
     * Dumps points-to sets for all variables (without contexts).
     */
    private static void dumpCIPointsToSet(PointerAnalysisResult result) {
        File outFile = new File(World.get().getOptions().getOutputDir(), CI_RESULTS_FILE);
        try (PrintStream out = new PrintStream(new FileOutputStream(outFile))) {
            logger.info("Dumping points-to set (without contexts) to {}",
                    outFile.getAbsolutePath());
            Function<Var, String> toString =
                    v -> v.getMethod().toString() + '/' + v.getName();
            result.getVars()
                    .stream()
                    .sorted(Comparator.comparing(toString))
                    .forEach(v -> {
                        Set<Obj> pts = result.getPointsToSet(v);
                        if (!pts.isEmpty()) {
                            out.printf("%s:%n", toString.apply(v));
                            pts.forEach(o -> out.printf("    %s%n", o));
                        }
                    });
        } catch (FileNotFoundException e) {
            logger.error("Failed to open output file {}", outFile);
        }
    }

    private static void comparePointsToSet(PointerAnalysisResult result, String input) {
        logger.info("Comparing points-to set with {}", input);
        var inputs = readPointsToSets(input);
        Map<String, Pointer> pointers = Maps.newLinkedHashMap();
        addPointers(pointers, result.getCSVars());
        addPointers(pointers, result.getStaticFields());
        addPointers(pointers, result.getInstanceFields());
        addPointers(pointers, result.getArrayIndexes());
        List<String> mismatches = new ArrayList<>();
        pointers.forEach((pointerStr, pointer) -> {
            String given = Streams.toString(pointer.objects());
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
        try (Stream<String> lines = Files.lines(Path.of(input))) {
            Map<String, String> result = Maps.newLinkedHashMap();
            lines.filter(line -> line.contains(SEP))
                    .map(line -> line.split(SEP))
                    .forEach(s -> result.put(s[0], s[1]));
            return result;
        } catch (IOException e) {
            throw new AnalysisException(
                    "Failed to read points-to set from " + input, e);
        }
    }

    private static void addPointers(Map<String, Pointer> map,
                                    Collection<? extends Pointer> pointers) {
        pointers.stream()
                .sorted(Comparator.comparing(Pointer::toString))
                .forEach(p -> map.put(p.toString(), p));
    }

    private static void dumpTaintFlows(PrintStream out, PointerAnalysisResult result) {
        Set<TaintFlow> taintFlows = getTaintFlows(result);
        out.printf("Detected %d taint flow(s):%n", taintFlows.size());
        taintFlows.forEach(out::println);
        out.println();
    }

    /**
     * @return taint analysis result.
     */
    private static Set<TaintFlow> getTaintFlows(PointerAnalysisResult result) {
        for (String key : result.getKeys()) {
            if (key.contains("Taint")) { // adapt different taint analyses
                return result.getResult(key);
            }
        }
        throw new AnalysisException("Taint analysis result is absent");
    }

    private static void compareTaintFlows(PointerAnalysisResult result, String input) {
        logger.info("Comparing taint flows with {}", input);
        List<String> inputs = readTaintFlows(input);
        List<String> taintFlows = Lists.map(getTaintFlows(result),
                TaintFlow::toString);
        List<String> mismatches = new ArrayList<>();
        taintFlows.forEach(taintFlow -> {
            if (!inputs.contains(taintFlow)) {
                mismatches.add(taintFlow + " should NOT be included");
            }
        });
        inputs.forEach(expected -> {
            if (!taintFlows.contains(expected)) {
                mismatches.add(expected + " should be included");
            }
        });
        if (!mismatches.isEmpty()) {
            throw new AnalysisException("Mismatches of taint flow(s)\n" +
                    String.join("\n", mismatches));
        }
    }

    private static List<String> readTaintFlows(String input) {
        try (Stream<String> lines = Files.lines(Path.of(input))) {
            List<String> taintFlows = new ArrayList<>();
            lines.filter(line -> line.startsWith("TaintFlow{") && line.contains(SEP))
                    .forEach(taintFlows::add);
            return taintFlows;
        } catch (IOException e) {
            throw new AnalysisException(
                    "Failed to read taint flows from " + input, e);
        }
    }
}

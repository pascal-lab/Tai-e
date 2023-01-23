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

package pascal.taie.config;

import pascal.taie.analysis.graph.callgraph.CallGraphBuilder;
import pascal.taie.util.collection.CollectionUtils;
import pascal.taie.util.collection.Lists;
import pascal.taie.util.graph.Graph;
import pascal.taie.util.graph.SCC;
import pascal.taie.util.graph.SimpleGraph;
import pascal.taie.util.graph.TopoSorter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import static pascal.taie.util.collection.Sets.newSet;

/**
 * Makes analysis plan based on given plan configs and analysis configs.
 */
public class AnalysisPlanner {

    private final ConfigManager manager;

    /**
     * Set of IDs for the analyses whose results are kept.
     */
    private final Set<String> keepResult;

    public AnalysisPlanner(ConfigManager manager, Set<String> keepResult) {
        this.manager = manager;
        this.keepResult = keepResult;
    }

    /**
     * This method makes a plan by converting given list of PlanConfig
     * to AnalysisConfig. It will be used when analysis plan is specified
     * by configuration file.
     *
     * @return the analysis plan consists of a list of analysis config.
     * @throws ConfigException if the given planConfigs are invalid.
     */
    public Plan makePlan(List<PlanConfig> planConfigs,
                         boolean reachableScope) {
        List<AnalysisConfig> analyses = covertConfigs(planConfigs);
        validateAnalyses(analyses, reachableScope);
        Graph<AnalysisConfig> graph = buildDependenceGraph(analyses);
        validateDependenceGraph(graph);
        return new Plan(analyses, graph, keepResult);
    }

    /**
     * Converts a list of PlanConfigs to the list of corresponding AnalysisConfigs.
     */
    private List<AnalysisConfig> covertConfigs(List<PlanConfig> planConfigs) {
        return planConfigs.stream()
                .map(pc -> manager.getConfig(pc.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Checks if the given analysis sequence is valid.
     *
     * @param analyses       the given analysis sequence
     * @param reachableScope whether the analysis scope is set to reachable
     * @throws ConfigException if the given analyses is invalid
     */
    private void validateAnalyses(List<AnalysisConfig> analyses, boolean reachableScope) {
        // check if all required analyses are placed in front of
        // their requiring analyses
        for (int i = 0; i < analyses.size(); ++i) {
            AnalysisConfig config = analyses.get(i);
            for (AnalysisConfig required : manager.getRequiredConfigs(config)) {
                int rindex = analyses.indexOf(required);
                if (rindex == -1) {
                    // required analysis is missing
                    throw new ConfigException(String.format(
                            "'%s' is required by '%s' but missing in analysis plan",
                            required, config));
                } else if (rindex >= i) {
                    // invalid analysis order: required analysis runs
                    // after current analysis
                    throw new ConfigException(String.format(
                            "'%s' is required by '%s' but it runs after '%s'",
                            required, config, config));
                }
            }
        }
        if (reachableScope) { // analysis scope is set to reachable
            // check if given analyses include call graph builder
            AnalysisConfig cg = CollectionUtils.findFirst(analyses,
                    AnalysisPlanner::isCG);
            if (cg == null) {
                throw new ConfigException(String.format("Scope is reachable" +
                                " but call graph builder (%s) is not given in analyses",
                        CallGraphBuilder.ID));
            }
            // check if call graph builder is executed as early as possible
            Set<AnalysisConfig> cgRequired = manager.getAllRequiredConfigs(cg);
            for (AnalysisConfig config : analyses) {
                if (config.equals(cg)) {
                    break;
                }
                if (!cgRequired.contains(config)) {
                    throw new ConfigException(String.format(
                            "Scope is reachable, thus '%s' " +
                                    "should be placed after call graph builder (%s)",
                            config, CallGraphBuilder.ID));
                }
            }
        }
    }

    private static boolean isCG(AnalysisConfig config) {
        return config.getId().equals(CallGraphBuilder.ID);
    }

    /**
     * This method makes an analysis plan based on given plan configs,
     * and it will automatically add required analyses (which are not in
     * the given plan) to the resulting plan.
     * It will be used when analysis plan is specified by command line options.
     *
     * @return the analysis plan consisting of a list of analysis config.
     * @throws ConfigException if the specified planConfigs is invalid.
     */
    public Plan expandPlan(List<PlanConfig> planConfigs,
                           boolean reachableScope) {
        List<AnalysisConfig> configs = covertConfigs(planConfigs);
        if (reachableScope) { // complete call graph builder
            AnalysisConfig cg = CollectionUtils.findFirst(configs,
                    AnalysisPlanner::isCG);
            if (cg == null) {
                // if analysis scope is reachable and call graph builder is
                // not given, then we automatically add it
                configs.add(manager.getConfig(CallGraphBuilder.ID));
            }
        }
        Graph<AnalysisConfig> graph = buildDependenceGraph(configs);
        validateDependenceGraph(graph);
        List<AnalysisConfig> analyses = new TopoSorter<>(graph, configs).get();
        if (reachableScope) {
            analyses = shiftCG(analyses);
        }
        return new Plan(analyses, graph, keepResult);
    }

    /**
     * Shifts call graph builder (cg) in given sequence to ensure that
     * it will run before all the analyses that it does not require.
     */
    private List<AnalysisConfig> shiftCG(List<AnalysisConfig> analyses) {
        AnalysisConfig cg = CollectionUtils.findFirst(analyses,
                AnalysisPlanner::isCG);
        Set<AnalysisConfig> required = manager.getAllRequiredConfigs(cg);
        List<AnalysisConfig> notRequired = new ArrayList<>();
        // obtain the analyses that run before cg but not required by cg
        for (AnalysisConfig c : analyses) {
            if (c.equals(cg)) {
                break;
            }
            if (!required.contains(c)) {
                notRequired.add(c);
            }
        }
        List<AnalysisConfig> result = new ArrayList<>(analyses.size());
        // add analyses that are required by cg
        for (AnalysisConfig c : analyses) {
            if (required.contains(c)) {
                result.add(c);
            }
            if (c.equals(cg)) { // found cg, break
                break;
            }
        }
        result.add(cg); // add cg
        // add analyses that are not required by cg but placed before cg
        // in the original sequence
        result.addAll(notRequired);
        // add remaining analyses
        for (int i = analyses.indexOf(cg) + 1; i < analyses.size(); ++i) {
            result.add(analyses.get(i));
        }
        return result;
    }

    /**
     * Builds a dependence graph for AnalysisConfigs.
     * This method traverses relevant AnalysisConfigs starting from the ones
     * specified by given configs. During the traversal, if it finds that
     * analysis A1 is required by A2, then it adds an edge A1 -> A2 and
     * nodes A1 and A2 to the resulting graph.
     * <p>
     * The resulting graph contains the given analyses (planConfigs) and
     * all their (directly and indirectly) required analyses.
     */
    private Graph<AnalysisConfig> buildDependenceGraph(List<AnalysisConfig> configs) {
        SimpleGraph<AnalysisConfig> graph = new SimpleGraph<>();
        Set<AnalysisConfig> visited = newSet();
        Queue<AnalysisConfig> workList = new ArrayDeque<>(configs);
        while (!workList.isEmpty()) {
            AnalysisConfig config = workList.poll();
            graph.addNode(config);
            visited.add(config);
            manager.getRequiredConfigs(config).forEach(required -> {
                graph.addEdge(required, config);
                if (!visited.contains(required)) {
                    workList.add(required);
                }
            });
        }
        return graph;
    }

    /**
     * Checks if the given dependence graph is valid.
     *
     * @throws ConfigException if the given plan is invalid
     */
    private void validateDependenceGraph(Graph<AnalysisConfig> graph) {
        // Check if the dependence graph is self-contained, i.e.,
        // every required analysis is included in the graph
        graph.forEach(config -> {
            List<AnalysisConfig> missing = Lists.filter(
                    manager.getRequiredConfigs(config),
                    c -> !graph.hasNode(c));
            if (!missing.isEmpty()) {
                throw new ConfigException("Invalid analysis plan: " +
                        missing + " are missing");
            }
        });
        // Check if the dependence graph contains cycles
        SCC<AnalysisConfig> scc = new SCC<>(graph);
        if (!scc.getTrueComponents().isEmpty()) {
            throw new ConfigException("Invalid analysis plan: " +
                    scc.getTrueComponents() + " are mutually dependent");
        }
    }
}

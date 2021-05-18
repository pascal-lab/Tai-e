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

package pascal.taie.config;

import pascal.taie.util.graph.Graph;
import pascal.taie.util.graph.SCC;
import pascal.taie.util.graph.SimpleGraph;
import pascal.taie.util.graph.TopoSorter;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static pascal.taie.util.collection.SetUtils.newSet;

/**
 * Makes analysis plan based on given plan configs and analysis configs.
 */
public class AnalysisPlanner {

    private final ConfigManager manager;

    public AnalysisPlanner(ConfigManager manager) {
        this.manager = manager;
    }

    /**
     * This method makes a plan by converting given list of PlanConfig
     * to AnalysisConfig. It will be used when analysis plan is specified
     * by configuration file.
     * @return the analysis plan consists of a list of analysis config.
     * @throws ConfigException if the given planConfigs are invalid.
     */
    public List<AnalysisConfig> makePlan(List<PlanConfig> planConfigs) {
        List<AnalysisConfig> plan = covertConfigs(planConfigs);
        validatePlan(plan);
        return plan;
    }

    /**
     * Converts a list of PlanConfigs to the list of corresponding AnalysisConfigs.
     */
    private List<AnalysisConfig> covertConfigs(List<PlanConfig> planConfigs) {
        return planConfigs.stream()
                .map(pc -> manager.getConfig(pc.getId()))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Checks if the given analysis plan is valid.
     * @throws ConfigException if the given plan is invalid
     */
    private void validatePlan(List<AnalysisConfig> plan) {
        for (int i = 0; i < plan.size(); ++i) {
            AnalysisConfig config = plan.get(i);
            for (AnalysisConfig required : manager.getRequiredConfigs(config)) {
                int rindex = plan.indexOf(required);
                if (rindex == -1) {
                    // required analysis is missing
                    throw new ConfigException("Invalid configuration: " +
                            required + " is required by " + config +
                            " but missing in analysis plan");
                } else if (rindex >= i) {
                    // invalid analysis order: required analysis runs
                    // after current analysis
                    throw new ConfigException("Invalid configuration: " +
                            required + " is required by " + config +
                            " but it runs after " + config);
                }
            }
        }
    }

    /**
     * This method makes an analysis plan based on given plan configs,
     * and it will automatically add required analyses (which are not in
     * the given plan) to the resulting plan.
     * It will be used when analysis plan is specified by command line options.
     * @return the analysis plan consisting of a list of analysis config.
     * @throws ConfigException if the specified planConfigs is invalid.
     */
    public List<AnalysisConfig> expandPlan(List<PlanConfig> planConfigs) {
        Graph<AnalysisConfig> graph = buildRequireGraph(planConfigs);
        validateRequireGraph(graph);
        return new TopoSorter<>(graph, covertConfigs(planConfigs)).get();
    }

    /**
     * Builds a require graph for AnalysisConfigs.
     * This method traverses relevant AnalysisConfigs starting from the ones
     * specified by given PlanConfigs. During the traversal, if it finds that
     * analysis A1 is required by A2, then it adds an edge A1 -> A2 and
     * nodes A1 and A2 to the resulting graph.
     *
     * The resulting graph contains the given analyses (planConfigs) and
     * all their (directly and indirectly) required analyses.
     */
    private Graph<AnalysisConfig> buildRequireGraph(List<PlanConfig> planConfigs) {
        SimpleGraph<AnalysisConfig> graph = new SimpleGraph<>();
        Queue<AnalysisConfig> workList = new LinkedList<>();
        Set<AnalysisConfig> visited = newSet();
        planConfigs.forEach(pc -> workList.add(manager.getConfig(pc.getId())));
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
     * Checks if the given require graph is valid.
     * @throws ConfigException if the given plan is invalid
     */
    private void validateRequireGraph(Graph<AnalysisConfig> graph) {
        // Check if the require graph is self-contained, i.e., every required
        // analysis is included in the graph
        graph.nodes().forEach(config -> {
            List<AnalysisConfig> missing = manager.getRequiredConfigs(config)
                    .stream()
                    .filter(Predicate.not(graph::hasNode))
                    .collect(Collectors.toList());
            if (!missing.isEmpty()) {
                throw new ConfigException("Invalid analysis plan: " +
                        missing + " are missing");
            }
        });
        // Check if the require graph contains cycles
        SCC<AnalysisConfig> scc = new SCC<>(graph);
        if (!scc.getTrueComponents().isEmpty()) {
            throw new ConfigException("Invalid analysis plan: " +
                    scc.getTrueComponents() + " are mutually dependent");
        }
    }
}

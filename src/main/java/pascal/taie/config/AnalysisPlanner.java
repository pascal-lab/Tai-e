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
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Make analysis plan based on given plan configs and analysis configs.
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
     * @throws ConfigException if the given planConfigs is invalid.
     */
    public List<AnalysisConfig> makePlan(List<PlanConfig> planConfigs) {
        List<AnalysisConfig> plan = planConfigs.stream()
                .map(pc -> manager.getConfig(pc.getId()))
                .collect(Collectors.toUnmodifiableList());
        validatePlan(plan);
        return plan;
    }

    /**
     * Check if the given analysis plan is valid.
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
                            " but missing");
                } else if (rindex >= i) {
                    // required analysis runs after current analysis
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
     * @return the analysis plan consists of a list of analysis config.
     * @throws ConfigException if the specified planConfigs is invalid.
     */
    public List<AnalysisConfig> expandPlan(List<PlanConfig> planConfigs) {
        Graph<AnalysisConfig> graph = buildRequireGraph(planConfigs);
        validateRequireGraph(graph);
        return new TopoSorter<>(graph, true).get();
    }

    private Graph<AnalysisConfig> buildRequireGraph(List<PlanConfig> planConfigs) {
        SimpleGraph<AnalysisConfig> graph = new SimpleGraph<>();
        Queue<AnalysisConfig> workList = new LinkedList<>();
        planConfigs.forEach(pc -> workList.add(manager.getConfig(pc.getId())));
        while (!workList.isEmpty()) {
            AnalysisConfig config = workList.poll();
            graph.addNode(config);
            manager.getRequiredConfigs(config).forEach(required -> {
                graph.addEdge(config, required);
                if (!graph.hasNode(required)) {
                    workList.add(required);
                }
            });
        }
        return graph;
    }

    /**
     * Check if the given require graph is valid.
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

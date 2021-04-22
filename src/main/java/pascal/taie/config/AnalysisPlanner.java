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
class AnalysisPlanner {

    private final ConfigManager manager;

    private final List<PlanConfig> planConfigs;

    public AnalysisPlanner(ConfigManager manager, List<PlanConfig> planConfigs) {
        this.manager = manager;
        this.planConfigs = planConfigs;
    }

    List<AnalysisConfig> makePlan() {
        Graph<AnalysisConfig> graph = buildRequireGraph();
        validateRequireGraph(graph);
        return new TopoSorter<>(graph, true).get();
    }

    private Graph<AnalysisConfig> buildRequireGraph() {
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

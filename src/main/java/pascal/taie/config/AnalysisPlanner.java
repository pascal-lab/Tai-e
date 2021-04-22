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
import pascal.taie.util.graph.SimpleGraph;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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
        throw new UnsupportedOperationException();
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
                workList.add(required);
            });
        }
        return graph;
    }
}

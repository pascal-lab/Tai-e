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

import pascal.taie.util.collection.Sets;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static pascal.taie.util.collection.Maps.newMap;

/**
 * Manages a collection of {@link AnalysisConfig}.
 */
public class ConfigManager {

    /**
     * Map from analysis id to corresponding AnalysisConfig.
     */
    private final Map<String, AnalysisConfig> configs = new LinkedHashMap<>();

    /**
     * Map from AnalysisConfig to its required AnalysisConfigs.
     */
    private final Map<AnalysisConfig, List<AnalysisConfig>> requires = newMap();

    public ConfigManager(List<AnalysisConfig> configs) {
        configs.forEach(this::addConfig);
    }

    private void addConfig(AnalysisConfig config) {
        if (configs.containsKey(config.getId())) {
            throw new ConfigException("There are multiple analyses for the same id " +
                    config.getId() + " in " + Configs.getAnalysisConfigURL());
        }
        configs.put(config.getId(), config);
    }

    /**
     * Given an analysis id, returns the corresponding AnalysisConfig.
     *
     * @throws ConfigException when the manager does not contain
     *                         the AnalysisConfig for the given id.
     */
    AnalysisConfig getConfig(String id) {
        AnalysisConfig config = configs.get(id);
        if (config == null) {
            throw new ConfigException("Analysis \"" + id + "\" is not found in " +
                    Configs.getAnalysisConfigURL());
        }
        return config;
    }

    /**
     * Overwrites the AnalysisConfig.options by corresponding PlanConfig.options.
     */
    public void overwriteOptions(List<PlanConfig> planConfigs) {
        planConfigs.forEach(pc ->
                getConfig(pc.getId()).getOptions()
                        .update(pc.getOptions()));
    }

    /**
     * Obtains the required analyses of given analysis (represented by AnalysisConfig).
     * This computation is based on the options given in PlanConfig,
     * thus this method should be called after invoking {@link #overwriteOptions}.
     * NOTE: we should obtain required configs by this method, instead of
     * {@link AnalysisConfig#getRequires()}.
     */
    List<AnalysisConfig> getRequiredConfigs(AnalysisConfig config) {
        return requires.computeIfAbsent(config, c ->
                c.getRequires()
                        .stream()
                        .filter(required -> {
                            String conditions = Configs.extractConditions(required);
                            return Configs.satisfyConditions(conditions, c.getOptions());
                        })
                        .map(required -> getConfig(Configs.extractId(required)))
                        .collect(Collectors.toUnmodifiableList()));
    }

    /**
     * @return all configs (directly and indirectly) required by the given config
     */
    Set<AnalysisConfig> getAllRequiredConfigs(AnalysisConfig config) {
        Set<AnalysisConfig> visited = Sets.newHybridSet();
        Deque<AnalysisConfig> queue = new ArrayDeque<>(
                getRequiredConfigs(config));
        while (!queue.isEmpty()) {
            AnalysisConfig curr = queue.pop();
            visited.add(curr);
            getRequiredConfigs(curr)
                    .stream()
                    .filter(c -> !visited.contains(c))
                    .forEach(queue::add);
        }
        return Collections.unmodifiableSet(visited);
    }
}

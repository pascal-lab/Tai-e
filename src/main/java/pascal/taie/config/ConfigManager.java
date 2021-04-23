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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pascal.taie.util.collection.CollectionUtils.newMap;

public class ConfigManager {

    private final Map<String, AnalysisConfig> configs = new LinkedHashMap<>();

    private final Map<AnalysisConfig, List<AnalysisConfig>> requires = newMap();

    public ConfigManager(List<AnalysisConfig> configs) {
        configs.forEach(this::addConfig);
    }

    private void addConfig(AnalysisConfig config) {
        if (configs.containsKey(config.getId())) {
            // TODO: obtain analysis config file path in a better way
            throw new ConfigException(
                    "There are multiple analyses for the same id " +
                            config.getId() + " in tai-e-analyses.yml");
        }
        configs.put(config.getId(), config);
    }

    AnalysisConfig getConfig(String id) {
        return configs.get(id);
    }

    Stream<AnalysisConfig> configs() {
        return configs.values().stream();
    }

    /**
     * Overwrite the AnalysisConfig.options by corresponding PlanConfig.options.
     */
    public void overwriteOptions(List<PlanConfig> planConfigs) {
        planConfigs.forEach(pc -> {
            AnalysisConfig ac = getConfig(pc.getId());
            if (ac == null) {
                // TODO: obtain analysis config file path in a better way
                throw new ConfigException(pc.getId() +
                        " is not configured in tai-e-analyses.yml");
            }
            pc.getOptions().forEach((key, value) ->
                    ac.getOptions().merge(key, value, (v1, v2) -> v2));
        });
    }

    /**
     * Obtain the required configs of given config. This computation is
     * based on the options in PlanConfig, thus this method should be called
     * after invoking {@link #overwriteOptions(List<PlanConfig>)}.
     * NOTE: we should obtain required configs by this method, instead of
     * {@link AnalysisConfig#getRequires()}.
     */
    List<AnalysisConfig> getRequiredConfigs(AnalysisConfig config) {
        return requires.computeIfAbsent(config, c ->
                c.getRequires()
                        .stream()
                        .filter(required -> {
                            String conditions = ConfigUtils.extractConditions(required);
                            return ConfigUtils.satisfyConditions(conditions, c.getOptions());
                        })
                        .map(required -> getConfig(ConfigUtils.extractId(required)))
                        .collect(Collectors.toUnmodifiableList()));
    }
}

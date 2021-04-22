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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException {
        // TODO: encapsulate Jackson exception to ConfigException
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File configFile = new File(classLoader.getResource("tai-e-analyses.yml").getFile());
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        List<AnalysisConfig> analysisConfigs = Arrays.asList(
                mapper.readValue(configFile, AnalysisConfig[].class));
        ConfigManager manager = new ConfigManager(analysisConfigs);
        manager.configs().forEach(c -> System.out.println(c.toDetailedString()));

        File planFile = new File(classLoader.getResource("tai-e-plan.yml").getFile());
        List<PlanConfig> planConfigs = Arrays.asList(
                mapper.readValue(planFile, PlanConfig[].class));
        planConfigs.forEach(System.out::println);

        manager.overwriteOptions(planConfigs);
        manager.configs().forEach(c -> System.out.println(c.toDetailedString()));
        System.out.println();
        manager.configs().forEach(c -> {
            List<AnalysisConfig> requires = manager.getRequiredConfigs(c);
            if (!requires.isEmpty()) {
                System.out.print(c + " requires ");
                System.out.println(requires.stream()
                        .map(AnalysisConfig::getId)
                        .collect(Collectors.toList()));
            }
        });

        AnalysisPlanner planner = new AnalysisPlanner(manager);
        List<AnalysisConfig> plan = planner.expandPlan(planConfigs);
        System.out.println(plan);
//        System.out.println(planner.makePlan(planConfigs));
        AnalysisManager analysisManager = new AnalysisManager();
        analysisManager.execute(plan);
    }
}

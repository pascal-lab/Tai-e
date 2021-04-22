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
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File config = new File(classLoader.getResource("tai-e-analyses.yml").getFile());
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        AnalysisConfig[] analysisConfigs = mapper.readValue(config, AnalysisConfig[].class);
        ConfigManager manager = new ConfigManager(analysisConfigs);
        manager.configs().forEach(System.out::println);

        File plan = new File(classLoader.getResource("tai-e-plan.yml").getFile());
        PlanConfig[] planConfigs = mapper.readValue(plan, PlanConfig[].class);
        Arrays.stream(planConfigs).forEach(System.out::println);

        manager.overwriteOptions(planConfigs);
        manager.configs().forEach(System.out::println);
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
    }
}

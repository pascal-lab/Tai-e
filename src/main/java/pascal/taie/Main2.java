/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.AnalysisManager;
import pascal.taie.config.AnalysisPlanner;
import pascal.taie.config.ConfigManager;
import pascal.taie.config.ConfigUtils;
import pascal.taie.config.PlanConfig;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Main2 {

    private static final Logger logger = LogManager.getLogger(Main2.class);

    public static void main(String[] args) {
        Options2 options = processArgs(args);
        List<AnalysisConfig> plan = processConfigs(options);
        if (plan.isEmpty()) {
            logger.info("No analyses are specified");
            System.exit(0);
        }
        buildWorld(options);
        executePlan(plan);
    }

    /**
     * If the given options specify to print help or version information,
     * then print them and exit immediately.
     */
    private static Options2 processArgs(String[] args) {
        Options2 options = Options2.parse(args);
        if (options.isPrintHelp() || args.length == 0) {
            options.printHelp();
            System.exit(0);
        } else if (options.isPrintVersion()) {
            options.printVersion();
            System.exit(0);
        }
        return options;
    }

    private static List<AnalysisConfig> processConfigs(Options2 options) {
        File configFile = ConfigUtils.getDefaultAnalysisConfig();
        List<AnalysisConfig> analysisConfigs = AnalysisConfig.readFromFile(configFile);
        ConfigManager manager = new ConfigManager(analysisConfigs);
        AnalysisPlanner planner = new AnalysisPlanner(manager);
        if (!options.getAnalyses().isEmpty()) {
            // Analyses are specified by cmd options
            List<PlanConfig> planConfigs = PlanConfig.readFromOptions(options);
            manager.overwriteOptions(planConfigs);
            List<AnalysisConfig> plan = planner.expandPlan(planConfigs);
            if (options.getGenPlanFile() != null) {
                // This run only generates plan file but not executes it
                // For outputting purpose, we first convert AnalysisConfigs
                // in the expanded plan to PlanConfigs
                List<PlanConfig> configs = plan.stream()
                        .map(ac -> {
                            PlanConfig pc = new PlanConfig();
                            pc.setId(ac.getId());
                            pc.setOptions(ac.getOptions());
                            return pc;
                        })
                        .collect(Collectors.toUnmodifiableList());
                PlanConfig.writeToFile(configs, options.getGenPlanFile());
            } else {
                return plan;
            }
        } else if (options.getPlanFile() != null) {
            // Analyses are specified by file
            List<PlanConfig> planConfigs = PlanConfig.readFromFile(options.getPlanFile());
            manager.overwriteOptions(planConfigs);
            return planner.makePlan(planConfigs);
        }
        // No analyses are specified
        return Collections.emptyList();
    }

    /**
     * Convenient method for building the world from String arguments.
     */
    public static void buildWorld(String... args) {
        buildWorld(Options.parse(args));
    }

    private static void buildWorld(Options options) {
        Class<? extends WorldBuilder> builderClass = options.getWorldBuilderClass();
        try {
            Constructor<? extends WorldBuilder> builderCtor = builderClass.getConstructor();
            WorldBuilder builder = builderCtor.newInstance();
            builder.build(options);
        } catch (InstantiationException | IllegalAccessException |
                NoSuchMethodException | InvocationTargetException e) {
            System.err.println("Failed to build world due to " + e);
            System.exit(1);
        }
    }

    private static void executePlan(List<AnalysisConfig> plan) {
        AnalysisManager analysisManager = new AnalysisManager();
        analysisManager.execute(plan);
    }
}

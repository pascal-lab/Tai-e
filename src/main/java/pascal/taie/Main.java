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

package pascal.taie;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.AnalysisManager;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.AnalysisPlanner;
import pascal.taie.config.ConfigManager;
import pascal.taie.config.ConfigUtils;
import pascal.taie.config.Options;
import pascal.taie.config.PlanConfig;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        Options options = processArgs(args);
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
    private static Options processArgs(String[] args) {
        Options options = Options.parse(args);
        if (options.isPrintHelp() || args.length == 0) {
            options.printHelp();
            System.exit(0);
        } else if (options.isPrintVersion()) {
            options.printVersion();
            System.exit(0);
        }
        return options;
    }

    private static List<AnalysisConfig> processConfigs(Options options) {
        File configFile = ConfigUtils.getAnalysisConfig();
        List<AnalysisConfig> analysisConfigs = AnalysisConfig.readConfigs(configFile);
        ConfigManager manager = new ConfigManager(analysisConfigs);
        AnalysisPlanner planner = new AnalysisPlanner(manager);
        if (!options.getAnalyses().isEmpty()) {
            // Analyses are specified by options
            List<PlanConfig> planConfigs = PlanConfig.readConfigs(options);
            manager.overwriteOptions(planConfigs);
            List<AnalysisConfig> plan = planner.expandPlan(planConfigs);
            // Output analysis plan to file.
            // For outputting purpose, we first convert AnalysisConfigs
            // in the expanded plan to PlanConfigs
            List<PlanConfig> configs = plan.stream()
                    .map(ac -> new PlanConfig(ac.getId(), ac.getOptions()))
                    .collect(Collectors.toUnmodifiableList());
            // TODO: turn off output in test mode?
            PlanConfig.writeConfigs(configs, ConfigUtils.getDefaultPlan());
            if (!options.isOnlyGenPlan()) {
                // This run not only generates plan file but also executes it
               return plan;
            }
        } else if (options.getPlanFile() != null) {
            // Analyses are specified by file
            List<PlanConfig> planConfigs = PlanConfig.readConfigs(options.getPlanFile());
            manager.overwriteOptions(planConfigs);
            return planner.makePlan(planConfigs);
        }
        // No analyses are specified
        return List.of();
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

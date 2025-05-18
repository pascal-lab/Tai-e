/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.frontend.newfrontend.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.AbstractWorldBuilder;
import pascal.taie.World;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.Options;
import pascal.taie.frontend.newfrontend.FrontendContext;
import pascal.taie.frontend.newfrontend.closedworld.ClosedWorldBuilder;
import pascal.taie.frontend.newfrontend.closedworld.DependencyCWBuilder;
import pascal.taie.frontend.newfrontend.exception.FrontendException;
import pascal.taie.frontend.newfrontend.hierarchy.ClassHierarchyBuilder;
import pascal.taie.frontend.newfrontend.hierarchy.DefaultCHBuilder;
import pascal.taie.frontend.newfrontend.report.FrontendStats;
import pascal.taie.frontend.newfrontend.report.FrontendStatsResult;
import pascal.taie.frontend.newfrontend.report.FrontendTimer;
import pascal.taie.frontend.newfrontend.source.ClassSource;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.project.OptionsProjectBuilder;
import pascal.taie.project.Project;
import pascal.taie.project.ProjectBuilder;
import pascal.taie.util.collection.Maps;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * The world builder for new frontend. This class is the entry point of the frontend processing.
 */
public class JavaWorldBuilder extends AbstractWorldBuilder {

    private static final Logger logger = LogManager.getLogger(JavaWorldBuilder.class);

    @Override
    public void build(Options options, List<AnalysisConfig> analyses) {
        try {
            build(options);
        } catch (FrontendException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void build(Options options) throws FrontendException {
        World.reset();
        World world = new World();
        World.set(world);

        // options will be used during World building, thus it should be
        // set at first.
        world.setOptions(options);

        // initialize build context
        FrontendContext ctx = new FrontendContext(options.isSSA());

        ctx.setPhase(TaiePhase.PROJECT_LOADING);
        // initialize class hierarchy
        FrontendTimer timer = new FrontendTimer();
        timer.start();
        ProjectBuilder projectBuilder = new OptionsProjectBuilder(options);
        Project project = projectBuilder.build();
        timer.stop();
        long projectBuildingTime = timer.inMilliseconds();

        ctx.setPhase(TaiePhase.CLOSED_WORLD_ANALYSIS);
        timer.start();
        ClosedWorldBuilder closedWorldBuilder = new DependencyCWBuilder(ctx); // Configurable
        closedWorldBuilder.build(project);
        Collection<ClassSource> closedWorld = closedWorldBuilder.getClosedWorld();
        timer.stop();
        long closedWorldBuildingTime = timer.inMilliseconds();

        ctx.setPhase(TaiePhase.CLASS_HIERARCHY_ANALYSIS);
        timer.start();
        ClassHierarchyBuilder hierarchyBuilder = new DefaultCHBuilder(ctx);
        ClassHierarchy hierarchy = hierarchyBuilder.build(closedWorld);
        timer.stop();
        long classHierarchyBuildingTime = timer.inMilliseconds();
        world.setClassHierarchy(hierarchy);

        FrontendStats stats = new FrontendStats(
                projectBuildingTime,
                closedWorldBuildingTime,
                classHierarchyBuildingTime,
                Maps.newConcurrentMap(),
                Maps.newConcurrentMap(),
                Maps.newConcurrentMap(),
                Maps.newConcurrentMap());
        ctx.setStats(stats);


        TypeSystem typeSystem = ctx.getTypeSystem(); // the singleton context was built in hierarchyBuilder.build
        world.setTypeSystem(typeSystem);

        // classes has been built in hierarchyBuilder.build()

        // set main method
        String mainClassName = options.getMainClass();
        if (mainClassName != null) {
            JClass mainClass = hierarchy.getClass(mainClassName);
            if (mainClass != null) {
                JMethod mainMethod = mainClass.getDeclaredMethod(Subsignature.getMain());
                if (mainMethod != null) {
                    world.setMainMethod(mainMethod);
                } else {
                    logger.warn("Warning: main class '{}'" +
                                    " does not have main(String[]) method!",
                            options.getMainClass());
                }
            } else {
                logger.warn("Warning: main class is not found in class hierarchy!");
            }
        } else {
            logger.warn("Warning: main class was not given!");
        }

        // set implicit entries
        world.setImplicitEntries(implicitEntries.stream()
                .map(hierarchy::getJREMethod)
                // some implicit entries may not exist in certain JDK version,
                // thus we filter out null
                .filter(Objects::nonNull)
                .toList());

        // initialize IR builder
        world.setNativeModel(getNativeModel(typeSystem, hierarchy, options));
        world.setIRBuilder(ctx.getIRBuilder());
        if (options.isPreBuildIR()) {
            ctx.setPhase(TaiePhase.PREBUILDING_IR);
            ctx.getIRBuilder().buildAll(hierarchy);
        }

        FrontendStatsResult.setStats(stats);

        ctx.setPhase(TaiePhase.ANALYSIS);
    }
}

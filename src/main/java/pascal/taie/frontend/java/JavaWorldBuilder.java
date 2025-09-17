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

package pascal.taie.frontend.java;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.AbstractWorldBuilder;
import pascal.taie.World;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.Options;
import pascal.taie.frontend.java.classes.ClassHierarchyBuilder;
import pascal.taie.frontend.java.classes.ClassSource;
import pascal.taie.frontend.java.closedworld.ClosedWorldBuilder;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.project.OptionsProjectBuilder;
import pascal.taie.project.Project;

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
            throw new RuntimeException(e);
        }
    }

    private void build(Options options) throws FrontendException {
        World.reset();
        World world = new World();
        World.set(world);

        // options will be used during World building, thus it should be
        // set at first.
        world.setOptions(options);
        // build project from options
        Project project = new OptionsProjectBuilder(options).build();
        // build closed world
        Collection<ClassSource> closedWorld = new ClosedWorldBuilder(project).build();
        // initialize build context
        FrontendContext ctx = new FrontendContext(options.isSSA());
        // set up class hierarchy, classes are built in this phase
        ClassHierarchy hierarchy = new ClassHierarchyBuilder(ctx).build(closedWorld);
        world.setClassHierarchy(hierarchy);
        // set up type system
        // TODO: check type system here, maybe replace temp type system
        TypeSystem typeSystem = ctx.getTypeSystem(); // the singleton context was built in hierarchyBuilder.build
        world.setTypeSystem(typeSystem);
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
            ctx.getIRBuilder().buildAll(hierarchy);
        }
    }
}

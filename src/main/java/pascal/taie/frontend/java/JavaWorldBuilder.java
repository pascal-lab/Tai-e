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
import pascal.taie.config.Options;
import pascal.taie.frontend.java.classes.AsmClassSource;
import pascal.taie.frontend.java.classes.BytecodeClassBuilder;
import pascal.taie.frontend.java.classes.DefaultClassLoader;
import pascal.taie.frontend.java.classes.PhantomClassBuilder;
import pascal.taie.frontend.java.classes.PhantomClassSource;
import pascal.taie.frontend.java.closedworld.ClosedWorldBuilder;
import pascal.taie.frontend.java.ir.FastIRBuilder;
import pascal.taie.frontend.java.project.OptionsProjectBuilder;
import pascal.taie.frontend.java.project.Project;
import pascal.taie.ir.IRBuilder;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassHierarchyImpl;
import pascal.taie.language.classes.ClassSource;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassBuilder;
import pascal.taie.language.classes.JClassLoader;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;

import java.util.Collection;
import java.util.Objects;

/**
 * The world builder for new frontend. This class is the entry point of the frontend processing.
 */
public class JavaWorldBuilder extends AbstractWorldBuilder {

    private static final Logger logger = LogManager.getLogger(JavaWorldBuilder.class);

    @Override
    public void build(Options options) {
        try {
            runWorldBuilder(options);
        } catch (FrontendException e) {
            throw new RuntimeException(e);
        }
    }

    private void runWorldBuilder(Options options) throws FrontendException {
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

        // ============ Build frontend components ============
        // create two most basic classes in frontend: class loader and type system
        JClassLoader loader = new DefaultClassLoader(closedWorld);
        FrontendTypeSystem typeSystem = new FrontendTypeSystem(loader);
        // build classes
        // the hierarchy-related API in typeSystem should not be called here, as the class hierarchy is not built
        closedWorld.parallelStream().forEach(source -> {
            JClass jclass = loader.loadClass(source.className());
            JClassBuilder builder;
            if (source instanceof AsmClassSource asmSource) {
                builder = new BytecodeClassBuilder(typeSystem, loader, asmSource, jclass);
            } else if (source instanceof PhantomClassSource pSource) {
                builder = new PhantomClassBuilder(typeSystem, pSource);
            } else {
                throw new UnsupportedOperationException();
            }
            builder.build(jclass);
        });
        // create class hierarchy
        ClassHierarchy hierarchy = new ClassHierarchyImpl(loader);
        typeSystem.setHierarchy(hierarchy);

        // ============ End of building frontend components ============

        // set up class hierarchy
        world.setClassHierarchy(hierarchy);
        // set up type system
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
        IRBuilder irBuilder = new FastIRBuilder(typeSystem);
        world.setIRBuilder(irBuilder);
        if (options.isPreBuildIR()) {
            irBuilder.buildAll(hierarchy);
        }
    }
}

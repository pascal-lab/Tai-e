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

package pascal.taie.frontend.newfrontend.asyncir;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.config.Options;
import pascal.taie.frontend.newfrontend.context.BuildContext;
import pascal.taie.frontend.newfrontend.bcir.AsmIRBuilder;
import pascal.taie.frontend.newfrontend.main.NewFrontendComponent;
import pascal.taie.frontend.newfrontend.report.StageTimer;
import pascal.taie.frontend.newfrontend.java.JavaMethodIRBuilder;
import pascal.taie.frontend.newfrontend.source.AsmMethodSource;
import pascal.taie.frontend.newfrontend.source.JavaMethodSource;
import pascal.taie.ir.IR;
import pascal.taie.ir.IRBuildHelper;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.Timer;

import java.util.List;

/**
 * This class implements the IRBuilder interface and is responsible for
 * building Intermediate Representations (IR) for Java methods.
 * It supports building IR for JVM Bytecode ({@link AsmMethodSource})
 * and Java ({@link JavaMethodSource}) method sources.
 */
public class IRBuilder extends NewFrontendComponent
        implements pascal.taie.ir.IRBuilder {

    private static final Logger logger = LogManager.getLogger(IRBuilder.class);

    public IRBuilder(BuildContext context) {
        super(context);
    }

    /**
     * Builds the Intermediate Representation (IR) for the given Java method.
     *
     * @param method the Java method for which to build the IR
     * @return the built IR
     */
    @Override
    public IR buildIR(JMethod method) {
        try {
            // TODO: Add more IRBuilder for different types of source.
            Object source = method.getMethodSource();
            if (source instanceof AsmMethodSource asmMethodSource) {
                AsmIRBuilder builder = new AsmIRBuilder(ctx(), method, asmMethodSource);
                builder.build();
                return builder.getIr();
            } else if (source == null) {
                return ctx().getIrService().loadingAndGetIR(method);
            } else if (source instanceof JavaMethodSource javaMethodSource) {
                JavaMethodIRBuilder builder = new JavaMethodIRBuilder(javaMethodSource, method);
                return builder.build();
            } else {
                throw new UnsupportedOperationException();
            }
        } catch (RuntimeException e) {
            if (e.getStackTrace()[0].getClassName().startsWith("Asm")) {
                logger.warn("ASM bytecode front failed to build method body for {}," +
                        " constructs an empty IR instead", method);
                return new IRBuildHelper(method).buildEmpty();
            } else {
                throw e;
            }
        }
    }

    /**
     * Builds IR for all methods in given class hierarchy.
     * TODO: currently copied from soot.IRBuilder
     * Considering a abstract class to be the common supertype.
     */
    @Override
    public void buildAll(ClassHierarchy hierarchy) {
        Timer timer = new Timer("Build IR for all methods");
        timer.start();
        List<JClass> classes;
        classes = hierarchy.allClasses().toList();
        classes.parallelStream().forEach(c -> {
            for (JMethod m : c.getDeclaredMethods()) {
                if (! m.isAbstract() && ! m.isNative()) {
                    m.getIR();
                }
            }
        });
        timer.stop();
        logger.info(timer);
        StageTimer.getInstance().reportIRTime((long)
                (timer.inSecond() * 1000));
    }
}

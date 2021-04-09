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

import pascal.taie.pass.Pass;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class Main {

    public static void main(String[] args) {
        Options options = processArgs(args);
        buildWorld(options);
        runPasses(options);
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

    private static void runPasses(Options options) {
        options.getPassClasses().forEach(className -> {
            try {
                Class<?> c = Class.forName(className);
                Constructor<?> ctor = c.getConstructor();
                Pass pass = (Pass) ctor.newInstance();
                pass.run();
            } catch (ClassNotFoundException | InstantiationException |
                    IllegalAccessException | NoSuchMethodException |
                    InvocationTargetException e) {
                System.err.println("Failed to run " + className + " due to " + e);
            }
        });
    }
}

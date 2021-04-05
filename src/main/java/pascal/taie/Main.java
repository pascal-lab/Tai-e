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
    public static void buildWorld(String[] args) {
        buildWorld(Options.parse(args));
    }

    private static void buildWorld(Options options) {
        Class<? extends WorldBuilder> wbClass = options.getWorldBuilderClass();
        try {
            WorldBuilder builder = wbClass.newInstance();
            builder.build(options);
        } catch (InstantiationException | IllegalAccessException e) {
            System.err.println("Failed to build world due to " + e);
            System.exit(1);
        }
    }

    private static void runPasses(Options options) {
        options.getPassClasses().forEach(cname -> {
            try {
                Class<?> c = Class.forName(cname);
                Pass pass = (Pass) c.newInstance();
                pass.run();
            } catch (ClassNotFoundException |
                    InstantiationException | IllegalAccessException e) {
                System.err.println("Failed to run " + cname + " due to " + e);
            }
        });
    }
}

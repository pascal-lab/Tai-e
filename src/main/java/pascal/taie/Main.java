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
        // parse options
        Options options = Options.parse(args);
        if (options.shouldShowHelp()) {
            options.printHelp();
            return;
        } else if (options.shouldShowVersion()) {
            options.printVersion();
            return;
        }
        buildWorld(options);
        runPasses(options);
    }

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

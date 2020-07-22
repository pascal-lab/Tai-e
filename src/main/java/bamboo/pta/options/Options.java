/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.options;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "Bamboo", mixinStandardHelpOptions = true,
        description = "A pointer analysis framework for Java",
        version = "0.1")
public class Options implements Runnable {

    private static Options options;

    public static Options get() {
        return options;
    }

    public static void set(Options options) {
        Options.options = options;
    }

    @Option(names = "--no-implicit-entries",
            description = "Analyze implicit reachable entry methods",
            defaultValue = "true", negatable = true)
    private boolean implicitEntries;

    @Option(names = { "-cs", "--context-sensitivity" },
            description = "Context sensitivity of pointer analysis",
            defaultValue = "ci")
    private String contextSensitivity;

    @Override
    public void run() {
        set(this);
    }

    public boolean isImplicitEntries() {
        return implicitEntries;
    }

    public String getContextSensitivity() {
        return contextSensitivity;
    }
}

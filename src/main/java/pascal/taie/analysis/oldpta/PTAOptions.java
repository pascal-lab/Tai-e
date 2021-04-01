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

package pascal.taie.analysis.oldpta;

import pascal.taie.Options;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;

@Command(name = "PTAOptions",
        description = "Pointer analysis options",
        showEndOfOptionsDelimiterInUsageHelp = true,
        version = "0.1")
public class PTAOptions extends Options {

    // Default options
    private static PTAOptions options = CommandLine.populateCommand(new PTAOptions());
    // ---------- Soot options ----------
    @SuppressWarnings("FieldMayBeFinal")
    @Parameters(description = "Arguments for Soot")
    private String[] sootArgs = new String[0];

    public static PTAOptions get() {
        return options;
    }

    public static void set(PTAOptions options) {
        PTAOptions.options = options;
    }

    /**
     * Parse arguments and set new Options object.
     */
    public static void parseOptions(String... args) {
        options = CommandLine.populateCommand(new PTAOptions(), args);
    }

    public String[] getSootArgs() {
        return sootArgs;
    }
}

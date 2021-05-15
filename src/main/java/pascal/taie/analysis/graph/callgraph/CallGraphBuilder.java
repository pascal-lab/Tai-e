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

package pascal.taie.analysis.graph.callgraph;

import pascal.taie.analysis.InterproceduralAnalysis;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.ConfigException;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.language.classes.JMethod;

public class CallGraphBuilder extends InterproceduralAnalysis {

    public static final String ID = "cg";

    private final String algorithm;

    private final boolean isDump;

    public CallGraphBuilder(AnalysisConfig config) {
        super(config);
        algorithm = config.getOptions().getString("algorithm");
        isDump = config.getOptions().getBoolean("dump");
    }

    @Override
    public CallGraph<InvokeExp, JMethod> analyze() {
        CGBuilder<InvokeExp, JMethod> builder;
        switch (algorithm) {
            case "cha":
                builder = new CHABuilder();
                break;
            case "pta":
                builder = new PTABasedBuilder();
                break;
            default:
                throw new ConfigException("Unknown call graph building algorithm: " + algorithm);
        }
        CallGraph<InvokeExp, JMethod> callGraph = builder.build();
        if (isDump) {
            CGUtils.dumpCallGraph(callGraph);
        }
        return builder.build();
    }
}

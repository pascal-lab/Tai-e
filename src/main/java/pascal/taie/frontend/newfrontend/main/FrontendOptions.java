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

import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Sets;
import picocli.CommandLine;

import java.util.Set;

/**
 * Additional Options for frontend
 */

@CommandLine.Command
public class FrontendOptions {

    @CommandLine.Option(names = {"--ssa"},
            description = "Enable SSA (Static Single Assignment)",
            defaultValue = "false",
            negatable = true)
    private boolean isSSA;

    @CommandLine.Option(names = {"--useTypingAlgo2"},
            description = "Use Typing Algorithm 2",
            defaultValue = "true",
            negatable = true)
    private boolean useTypingAlgo2;

    @CommandLine.Option(names = {"--useParallelHierarchy"},
            description = "Use Parallel Hierarchy",
            defaultValue = "true",
            negatable = true)
    private boolean useParallelHierarchy;

    @CommandLine.Option(names = {"--debugOn"},
            description = "Methods to debug", split = ",")
    private Set<String> debugOn = Sets.newSet();

    // Getters and methods
    public boolean isSSA() {
        return isSSA;
    }

    public boolean isUseTypingAlgo2() {
        return useTypingAlgo2;
    }

    public boolean isUseParallelHierarchy() {
        return useParallelHierarchy;
    }

    public boolean debugOn(JMethod method) {
        String sig = method.getSignature();
        return debugOn != null && debugOn.contains(sig);
    }

    public Set<String> getDebugOn() {
        return debugOn;
    }
}

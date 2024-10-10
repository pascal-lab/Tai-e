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

package pascal.taie.frontend.newfrontend;

import pascal.taie.World;

import java.util.Map;

public class FrontendOptions {

    private final boolean isSSA;

    private final boolean useTypingAlgo2;

    private final boolean useParallelHierarchy;

    private static FrontendOptions instance;

    private FrontendOptions(boolean isSSA, boolean useTypingAlgo2, boolean useParallelHierarchy) {
        this.isSSA = isSSA;
        this.useTypingAlgo2 = useTypingAlgo2;
        this.useParallelHierarchy = useParallelHierarchy;
    }

    public synchronized static FrontendOptions get() {
        if (instance == null) {
            instance = parse();
        }
        return instance;
    }

    private static FrontendOptions parse() {
        Map <String, String> options = World.get().getOptions().getFrontendOptions();
        boolean isSSA = Boolean.parseBoolean(options.getOrDefault("ssa", "false"));
        boolean useTypingAlgo2 = Boolean.parseBoolean(options.getOrDefault("useTypingAlgo2", "true"));
        boolean useParallelHierarchy = Boolean.parseBoolean(
                options.getOrDefault("useParallelHierarchy", "true"));
        return new FrontendOptions(isSSA, useTypingAlgo2, useParallelHierarchy);
    }

    static {
        World.registerResetCallback(FrontendOptions::reset);
    }

    static void reset() {
        instance = null;
    }

    public boolean isSSA() {
        return isSSA;
    }

    public boolean isUseTypingAlgo2() {
        return useTypingAlgo2;
    }

    public boolean isUseParallelHierarchy() {
        return useParallelHierarchy;
    }
}

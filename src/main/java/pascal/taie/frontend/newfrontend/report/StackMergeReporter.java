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

package pascal.taie.frontend.newfrontend.report;

import pascal.taie.World;

public class StackMergeReporter {
    private long totalBlocks;

    private long pessimisticBlocks;

    private long pessimisticPhis;

    private long pessimisticLivePhis;

    private static StackMergeReporter instance;

    static {
        World.registerResetCallback(() -> {
            instance = null;
        });
    }

    public void reportStats(long totalBlocks, long pessimisticBlocks,
                            long pessimisticPhis, long pessimisticLivePhis) {
        this.totalBlocks += totalBlocks;
        this.pessimisticBlocks += pessimisticBlocks;
        this.pessimisticPhis += pessimisticPhis;
        this.pessimisticLivePhis += pessimisticLivePhis;
    }

    public void showStats() {
        System.out.println("total basic blocks:         " + totalBlocks  + "\n" +
                           "pessimistic basic blocks:   " + pessimisticBlocks + ", " + (double) pessimisticBlocks / totalBlocks + "\n" +
                           "                            " + pessimisticPhis + ", " + pessimisticLivePhis
                );
    }

    public static StackMergeReporter get() {
        if (instance == null) {
            instance = new StackMergeReporter();
        }
        return instance;
    }
}

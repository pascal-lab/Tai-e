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

package pascal.taie.frontend.newfrontend.ssa;

import pascal.taie.frontend.newfrontend.IBasicBlock;
import pascal.taie.ir.exp.Var;
import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Def-use information
 */
public class DUInfo {
    List<Set<IBasicBlock>> defBlocks; // For performance, you may change the implementation for set

    public DUInfo(int varCount) {
        defBlocks = new ArrayList<>(varCount);
        for (int i = 0; i < varCount; i++) {
            defBlocks.add(Sets.newSet());
        }
    }

    public void addDefBlock(Var v, IBasicBlock b) {
        int i = v.getIndex();
        while (i >= defBlocks.size()) {
            defBlocks.add(Sets.newSet());
        }
        defBlocks.get(i).add(b);
    }

    public Set<IBasicBlock> getDefBlock(Var v) {
        int i = v.getIndex();
        while (i >= defBlocks.size()) {
            // Maybe we can directly return an empty list?
            defBlocks.add(Sets.newSet());
        }
        return defBlocks.get(i);
    }
}

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

package pascal.taie.vm;

import pascal.taie.ir.exp.Var;
import pascal.taie.util.collection.Maps;

import java.util.Map;

/**
 * A JVM stack frame
 */
class Frame {
    private int pc;

    private int lastPC; // Only used for phi selection.

    private final Map<Var, JValue> regs;

    private JValue rets;

    static final int METHOD_ENTRY = -1; // Should we define a uniform index for entry?

    private Frame(int pc, Map<Var, JValue> regs) {
        this.pc = pc;
        this.lastPC = METHOD_ENTRY;
        this.regs = regs;
    }

    static Frame makeNewFrame() {
        return new Frame(0, Maps.newMap());
    }

    static Frame makeNewFrame(Map<Var, JValue> args) {
        return new Frame(0, args);
    }

    int getPC() {
        return pc;
    }

    void setPC(int pc) {
        this.pc = pc;
    }

    int getLastPC() {
        return lastPC;
    }

    void setLastPC(int lastPC) {
        this.lastPC = lastPC;
    }

    Map<Var, JValue> getRegs() {
        return regs;
    }

    JValue getRets() {
        return rets;
    }

    void setRets(JValue rets) {
        this.rets = rets;
    }

    void markEnd() {
        this.pc = -1;
    }
}

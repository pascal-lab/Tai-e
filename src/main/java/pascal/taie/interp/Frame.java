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

package pascal.taie.interp;

import pascal.taie.ir.exp.Var;
import pascal.taie.util.collection.Maps;

import java.util.Map;

public class Frame {
    private int pc;

    private int lastPc; // Only used for phi selection.

    private final Map<Var, JValue> regs;

    private JValue rets;

    public static final int METHOD_ENTRY = -1; // Should we define a uniform index for entry?

    public Frame(int pc, Map<Var, JValue> regs) {
        this.pc = pc;
        this.lastPc = METHOD_ENTRY;
        this.regs = regs;
    }

    public static Frame mkNewFrame() {
        return new Frame(0, Maps.newMap());
    }

    public static Frame mkNewFrame(Map<Var, JValue> args) {
        return new Frame(0, args);
    }

    public int getPc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public int getLastPc() {
        return lastPc;
    }

    public void setLastPc(int lastPc) {
        this.lastPc = lastPc;
    }

    public Map<Var, JValue> getRegs() {
        return regs;
    }

    public JValue getRets() {
        return rets;
    }

    public void setRets(JValue rets) {
        this.rets = rets;
    }

    public void markEnd() {
        this.pc = -1;
    }
}

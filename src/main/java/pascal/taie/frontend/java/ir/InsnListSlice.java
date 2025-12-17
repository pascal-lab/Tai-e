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

package pascal.taie.frontend.java.ir;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

import javax.annotation.Nonnull;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An immutable view representing a contiguous slice of bytecode instructions
 * from an ASM {@link InsnList}. The slice is defined by a start index (inclusive)
 * and an end index (exclusive).
 */
class InsnListSlice extends AbstractList<AbstractInsnNode> {

    /**
     * The backing instruction list from which this slice is derived.
     */
    private final InsnList list;

    /**
     * Start index of the slice (inclusive).
     */
    private final int start;

    /**
     * End index of the slice (exclusive).
     */
    private final int end;

    InsnListSlice(InsnList list, int start, int end) {
        this.list = list;
        this.start = start;
        this.end = end;
    }

    @Override
    public int size() {
        return Math.max(end - start, 0);
    }

    @Override
    public AbstractInsnNode get(int index) {
        return list.get(index + start);
    }

    @Override
    @Nonnull
    public Iterator<AbstractInsnNode> iterator() {
        return new Iterator<>() {
            int cursor = start;

            @Override
            public boolean hasNext() {
                return cursor < end;
            }

            @Override
            public AbstractInsnNode next() {
                return list.get(cursor++);
            }
        };
    }

    @Override
    public String toString() {
        List<String> instr = new ArrayList<>();
        for (AbstractInsnNode insn : this) {
            instr.add(BytecodeDumper.formatInsn(insn));
        }
        return String.join("\n", instr);
    }

    int getStart() {
        return start;
    }
}

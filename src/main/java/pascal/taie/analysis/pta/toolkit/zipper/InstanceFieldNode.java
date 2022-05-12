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

package pascal.taie.analysis.pta.toolkit.zipper;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.language.classes.JField;
import pascal.taie.util.Hashes;

class InstanceFieldNode extends OFGNode {

    private final Obj base;

    private final JField field;

    public InstanceFieldNode(Obj base, JField field) {
        this.base = base;
        this.field = field;
    }

    public Obj getBase() {
        return base;
    }

    public JField getField() {
        return field;
    }

    @Override
    public int hashCode() {
        return Hashes.hash(base, field);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof InstanceFieldNode otherNode)) {
            return false;
        }
        return base.equals(otherNode.base) &&
            field.equals(otherNode.field);
    }

    @Override
    public String toString() {
        return "InstanceFieldNode{" + base + "." + field + "}";
    }
}

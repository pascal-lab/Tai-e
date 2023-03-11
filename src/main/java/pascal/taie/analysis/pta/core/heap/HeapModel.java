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

package pascal.taie.analysis.pta.core.heap;

import pascal.taie.ir.exp.ReferenceLiteral;
import pascal.taie.ir.stmt.New;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.Indexer;

import java.util.Collection;

/**
 * Represents of heap models for heap objects.
 * Pointer analysis should ALWAYS obtain {@link Obj} via this interface.
 *
 * @see Obj
 */
public interface HeapModel extends Indexer<Obj> {

    /**
     * @return the abstract object for given new statement.
     */
    Obj getObj(New allocSite);

    /**
     * @return the constant object for given value.
     */
    Obj getConstantObj(ReferenceLiteral value);

    /**
     * @return {@code true} if {@code obj} represents a string constant.
     */
    boolean isStringConstant(Obj obj);

    Obj getMockObj(Descriptor desc, Object alloc, Type type,
                   JMethod container, boolean isFunctional);

    /**
     * @return the mock object for given arguments.
     */
    default Obj getMockObj(Descriptor desc, Object alloc, Type type, JMethod container) {
        return getMockObj(desc, alloc, type, container, true);
    }

    /**
     * @return the mock object for given arguments.
     */
    default Obj getMockObj(Descriptor desc, Object alloc, Type type, boolean isFunctional) {
        return getMockObj(desc, alloc, type, null, isFunctional);
    }

    /**
     * @return the mock object for given arguments.
     */
    default Obj getMockObj(Descriptor desc, Object alloc, Type type) {
        return getMockObj(desc, alloc, type, null);
    }

    /**
     * @return all objects in this heap model.
     */
    Collection<Obj> getObjects();
}

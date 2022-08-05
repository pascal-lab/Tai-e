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

import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

import java.util.Optional;
import java.util.Set;

import static pascal.taie.util.collection.Sets.newSet;

/**
 * Represents a set of merged objects.
 */
public class MergedObj extends Obj {

    private final String name;

    private final Type type;

    /**
     * Set of objects represented by this merged object.
     */
    private final Set<Obj> representedObjs = newSet();

    /**
     * The representative object of this merged object. It is the first
     * object added.
     */
    private Obj representative;

    public MergedObj(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    public void addRepresentedObj(Obj obj) {
        setRepresentative(obj);
        representedObjs.add(obj);
    }

    private void setRepresentative(Obj obj) {
        if (representative == null) {
            representative = obj;
        }
    }

    @Override
    public Set<Obj> getAllocation() {
        return representedObjs;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Optional<JMethod> getContainerMethod() {
        return representative != null ?
                representative.getContainerMethod() :
                Optional.empty();
    }

    @Override
    public Type getContainerType() {
        return representative != null ?
                representative.getContainerType() : type;
    }

    @Override
    public String toString() {
        return "MergedObj{" + name + "}";
    }
}

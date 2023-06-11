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

package pascal.taie.language.classes;

import pascal.taie.World;
import pascal.taie.language.type.Type;
import pascal.taie.util.InternalCanonicalized;
import pascal.taie.util.collection.Maps;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * Method name and descriptor.
 */
@InternalCanonicalized
public class Subsignature implements Serializable {

    // Subsignatures of special methods
    public static final String CLINIT = "void <clinit>()";

    public static final String NO_ARG_INIT = "void <init>()";

    private static final ConcurrentMap<String, Subsignature> map
            = Maps.newConcurrentMap();

    static {
        World.registerResetCallback(map::clear);
    }

    private final String subsig;

    public static Subsignature get(
            String name, List<Type> parameterTypes, Type returnType) {
        return get(StringReps.toSubsignature(name, parameterTypes, returnType));
    }

    public static Subsignature get(String subsig) {
        return map.computeIfAbsent(subsig, Subsignature::new);
    }

    /**
     * @return subsignature of no-arg constructor.
     */
    public static Subsignature getNoArgInit() {
        return get(NO_ARG_INIT);
    }

    /**
     * @return subsignature of static initializer (clinit).
     */
    public static Subsignature getClinit() {
        return get(CLINIT);
    }

    private Subsignature(String subsig) {
        this.subsig = subsig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Subsignature that)) {
            return false;
        }

        return subsig.equals(that.subsig);
    }

    @Override
    public int hashCode() {
        return subsig.hashCode();
    }

    @Override
    public String toString() {
        return subsig;
    }
}

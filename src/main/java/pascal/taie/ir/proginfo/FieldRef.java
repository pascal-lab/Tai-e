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

package pascal.taie.ir.proginfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.Type;
import pascal.taie.util.InternalCanonicalized;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * Represents field references in IR.
 */
@InternalCanonicalized
public class FieldRef extends MemberRef {

    private static final Logger logger = LogManager.getLogger(FieldRef.class);

    private static final ConcurrentMap<Key, FieldRef> map =
            Maps.newConcurrentMap(4096);

    /**
     * Records the FieldRef that fails to be resolved.
     */
    private static final Set<FieldRef> resolveFailures =
            Sets.newConcurrentSet();

    static {
        World.registerResetCallback(map::clear);
        World.registerResetCallback(resolveFailures::clear);
    }

    private final Type type;

    /**
     * Caches the resolved field for this reference to avoid redundant
     * field resolution.
     *
     * @see #resolve()
     * @see #resolveNullable()
     */
    @Nullable
    private transient JField field;

    public static FieldRef get(
            JClass declaringClass, String name, Type type, boolean isStatic) {
        Key key = new Key(declaringClass, name, type);
        return map.computeIfAbsent(key, k -> new FieldRef(k, isStatic));
    }

    private FieldRef(Key key, boolean isStatic) {
        super(key.declaringClass, key.name, isStatic);
        this.type = key.type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public JField resolve() {
        if (field == null) {
            field = World.get().getClassHierarchy()
                    .resolveField(this);
            if (field == null) {
                throw new FieldResolutionFailedException(
                        "Cannot resolve " + this);
            }
        }
        return field;
    }

    @Override
    @Nullable
    public JField resolveNullable() {
        if (field == null) {
            field = World.get().getClassHierarchy()
                    .resolveField(this);
            if (field == null && resolveFailures.add(this)) {
                logger.warn("Failed to resolve {}", this);
            }
        }
        return field;
    }

    @Override
    public String toString() {
        return StringReps.getFieldSignature(
                getDeclaringClass(), getName(), type);
    }

    /**
     * Uses as keys to identify {@link FieldRef}s in cache.
     */
    private record Key(JClass declaringClass, String name, Type type) {
    }
}

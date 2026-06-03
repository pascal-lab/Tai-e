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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pascal.taie.World;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.Type;
import pascal.taie.util.Hashes;
import pascal.taie.util.collection.Sets;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;

/**
 * Represents field references in IR.
 */
public class FieldRef extends MemberRef {

    private static final Logger logger = LoggerFactory.getLogger(FieldRef.class);

    /**
     * Records the FieldRef that fails to be resolved.
     */
    private static final Set<FieldRef> resolveFailures =
            Sets.newConcurrentSet();

    static {
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

    private transient int cachedHash = 0;

    public static FieldRef get(
            JClass declaringClass, String name, Type type, boolean isStatic) {
        return new FieldRef(declaringClass, name, type, isStatic);
    }

    private FieldRef(JClass declaringClass, String name, Type type, boolean isStatic) {
        super(declaringClass, name, isStatic);
        this.type = type;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FieldRef that)) {
            return false;
        }
        return this.isStatic() == that.isStatic()
                && Objects.equals(this.getDeclaringClass(), that.getDeclaringClass())
                && Objects.equals(this.getName(), that.getName())
                && Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        if (cachedHash == 0) {
            int result = Hashes.hash(
                    getDeclaringClass(),
                    getName(),
                    type
            );
            result = 31 * result + (isStatic() ? 1 : 0);
            cachedHash = result;
        }
        return cachedHash;
    }
}

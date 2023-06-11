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
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.Type;
import pascal.taie.util.InternalCanonicalized;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import static pascal.taie.language.classes.ClassNames.METHOD_HANDLE;
import static pascal.taie.language.classes.ClassNames.VAR_HANDLE;

/**
 * Represents method references in IR.
 */
@InternalCanonicalized
public class MethodRef extends MemberRef {

    private static final Logger logger = LogManager.getLogger(MethodRef.class);

    private static final ConcurrentMap<Key, MethodRef> map =
            Maps.newConcurrentMap(4096);

    /**
     * Records the MethodRef that fails to be resolved.
     */
    private static final Set<MethodRef> resolveFailures =
            Sets.newConcurrentSet();

    static {
        World.registerResetCallback(map::clear);
        World.registerResetCallback(resolveFailures::clear);
    }

    // Method names of polymorphic signature methods.
    private static final Set<String> METHOD_HANDLE_METHODS = Set.of(
            "invokeExact",
            "invoke",
            "invokeBasic",
            "linkToVirtual",
            "linkToStatic",
            "linkToSpecial",
            "linkToInterface"
    );

    private static final Set<String> VAR_HANDLE_METHODS = Set.of(
            "get",
            "set",
            "getVolatile",
            "setVolatile",
            "getOpaque",
            "setOpaque",
            "getAcquire",
            "setRelease",
            "compareAndSet",
            "compareAndExchange",
            "compareAndExchangeAcquire",
            "compareAndExchangeRelease",
            "weakCompareAndSetPlain",
            "weakCompareAndSet",
            "weakCompareAndSetAcquire",
            "weakCompareAndSetRelease",
            "getAndSet",
            "getAndSetAcquire",
            "getAndSetRelease",
            "getAndAdd",
            "getAndAddAcquire",
            "getAndAddRelease",
            "getAndBitwiseOr",
            "getAndBitwiseOrAcquire",
            "getAndBitwiseOrRelease",
            "getAndBitwiseAnd",
            "getAndBitwiseAndAcquire",
            "getAndBitwiseAndRelease",
            "getAndBitwiseXor",
            "getAndBitwiseXorAcquire",
            "getAndBitwiseXorRelease"
    );

    private final List<Type> parameterTypes;

    private final Type returnType;

    private final Subsignature subsignature;

    /**
     * Caches the resolved method for this reference to avoid redundant
     * method resolution.
     *
     * @see #resolve()
     * @see #resolveNullable()
     */
    @Nullable
    private transient JMethod method;

    public static MethodRef get(
            JClass declaringClass, String name,
            List<Type> parameterTypes, Type returnType,
            boolean isStatic) {
        Subsignature subsignature = Subsignature.get(
                name, parameterTypes, returnType);
        Key key = new Key(declaringClass, subsignature);
        return map.computeIfAbsent(key, k ->
                new MethodRef(k, name, parameterTypes, returnType, isStatic));
    }

    private MethodRef(
            Key key, String name, List<Type> parameterTypes, Type returnType,
            boolean isStatic) {
        super(key.declaringClass, name, isStatic);
        this.parameterTypes = List.copyOf(parameterTypes);
        this.returnType = returnType;
        this.subsignature = key.subsignature;
    }

    public List<Type> getParameterTypes() {
        return parameterTypes;
    }

    public Type getReturnType() {
        return returnType;
    }

    /**
     * @return the subsignature of the method reference.
     */
    public Subsignature getSubsignature() {
        return subsignature;
    }

    /**
     * @return true if this is a reference to polymorphic signature method,
     * otherwise false.
     * See JLS (11 Ed.), 15.12.3 for the definition of polymorphic signature method.
     */
    public boolean isPolymorphicSignature() {
        if (METHOD_HANDLE.equals(getDeclaringClass().getName())) {
            return METHOD_HANDLE_METHODS.contains(getName());
        }
        if (VAR_HANDLE.equals(getDeclaringClass().getName())) {
            return VAR_HANDLE_METHODS.contains(getName());
        }
        return false;
    }

    @Override
    public JMethod resolve() {
        if (method == null) {
            method = World.get().getClassHierarchy()
                    .resolveMethod(this);
            if (method == null) {
                throw new MethodResolutionFailedException(
                        "Cannot resolve " + this);
            }
        }
        return method;
    }

    @Override
    @Nullable
    public JMethod resolveNullable() {
        if (method == null) {
            method = World.get().getClassHierarchy()
                    .resolveMethod(this);
            if (method == null && resolveFailures.add(this)) {
                logger.debug("Failed to resolve {}", this);
            }
        }
        return method;
    }

    @Override
    public String toString() {
        return StringReps.getMethodSignature(getDeclaringClass(), getName(),
                parameterTypes, returnType);
    }

    /**
     * Uses as keys to identify {@link MethodRef}s in cache.
     */
    private record Key(JClass declaringClass, Subsignature subsignature) {
    }
}

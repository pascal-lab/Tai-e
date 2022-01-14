/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.language.classes;

import pascal.taie.World;
import pascal.taie.language.type.Type;
import pascal.taie.util.InternalCanonicalized;
import pascal.taie.util.collection.Maps;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * Method name and descriptor.
 */
@InternalCanonicalized
public class Subsignature {

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
    public String toString() {
        return subsig;
    }
}

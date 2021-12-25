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

package pascal.taie.ir.proginfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.Type;
import pascal.taie.util.InternalCanonicalized;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentMap;

import static pascal.taie.util.collection.Maps.newConcurrentMap;

/**
 * Represents field references in IR.
 */
@InternalCanonicalized
public class FieldRef extends MemberRef {

    private static final Logger logger = LogManager.getLogger(FieldRef.class);

    private static final ConcurrentMap<Key, FieldRef> map =
            newConcurrentMap(4096);

    private final Type type;

    /**
     * Caches the resolved field for this reference to avoid redundant
     * field resolution.
     */
    private JField field;

    public static FieldRef get(
            JClass declaringClass, String name, Type type, boolean isStatic) {
        Key key = new Key(declaringClass, name, type);
        return map.computeIfAbsent(key, k -> new FieldRef(k, isStatic));
    }

    public static void reset() {
        map.clear();
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
            field = World.getClassHierarchy()
                    .resolveField(this);
            if (field == null) {
                throw new FieldResolutionFailedException(
                        "Cannot resolve " + this);
            }
        }
        return field;
    }

    @Override
    public @Nullable
    JField resolveNullable() {
        if (field == null) {
            field = World.getClassHierarchy()
                    .resolveField(this);
            if (field == null) {
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

    private record Key(JClass declaringClass, String name, Type type) {
    }
}

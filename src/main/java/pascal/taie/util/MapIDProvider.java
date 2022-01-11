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

package pascal.taie.util;

import pascal.taie.util.collection.Maps;

import java.util.Map;

/**
 * Map-based ID provider.
 */
public class MapIDProvider<T> implements IDProvider<T> {

    private final Map<T, Integer> map = Maps.newMap();

    private int count = 0;

    @Override
    public int getID(T e) {
        return map.computeIfAbsent(e, unused -> count++);
    }
}

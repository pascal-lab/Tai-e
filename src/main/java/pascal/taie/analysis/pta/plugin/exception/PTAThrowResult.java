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

package pascal.taie.analysis.pta.plugin.exception;

import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;

import java.util.Map;
import java.util.Optional;

public class PTAThrowResult {

    private final Map<JMethod, MethodThrowResult> results = Maps.newMap(1024);

    MethodThrowResult getOrCreateResult(JMethod method) {
        return results.computeIfAbsent(method, MethodThrowResult::new);
    }

    public Optional<MethodThrowResult> getResult(JMethod method) {
        return Optional.ofNullable(results.get(method));
    }
}

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

package pascal.taie.config;

import java.util.Map;

class Utils {

    private Utils() {
    }

    static String extractId(String require) {
        int index = require.indexOf('(');
        return index == -1 ? require :
                require.substring(0, index);
    }

    static String extractConditions(String require) {
        int index = require.indexOf('(');
        return index == -1 ? null :
                require.substring(index + 1, require.length() - 1);
    }

    /**
     * @return if the given options satisfy the given conditions.
     * TODO: allow sophisticated conditions? Currently only support
     *  conjunctions, may support disjunctions?
     */
    static boolean satisfyConditions(String conditions, Map<String, Object> options) {
        if (conditions != null) {
            for (String conds : conditions.split(",")) {
                String[] splits = conds.split("=");
                String key = splits[0];
                String value = splits[1];
                if (!options.get(key).toString().equals(value)) {
                    return false;
                }
            }
        }
        return true;
    }
}

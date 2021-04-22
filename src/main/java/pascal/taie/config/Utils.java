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
     * Examples of conditions:
     * - a=b
     * - a=b,x=y
     * - a=b|c|d,x=y
     * @return if the given options satisfy the given conditions.
     * TODO: comprehensive error handling for invalid conditions
     */
    static boolean satisfyConditions(String conditions, Map<String, Object> options) {
        if (conditions != null) {
            outer:
            for (String conds : conditions.split(",")) {
                String[] splits = conds.split("=");
                String key = splits[0];
                String value = splits[1];
                if (value.contains("|")) { // a=b|c
                    // Check each individual value, if one match,
                    // then this condition can be satisfied.
                    for (String v : value.split("\\|")) {
                        if (options.get(key).toString().equals(v)) {
                            continue outer;
                        }
                    }
                    return false;
                } else if (!options.get(key).toString().equals(value)) { // a=b
                    return false;
                }
            }
        }
        return true;
    }
}

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

package pascal.taie.analysis;

import java.util.Map;

public class AnalysisOptions {

    private final Map<String, Object> options;

    public AnalysisOptions(Map<String, Object> options) {
        this.options = options;
    }

    public Object get(String key) {
        return options.get(key);
    }

    public String getString(String key) {
        return (String) get(key);
    }

    public boolean getBoolean(String key) {
        return (Boolean) get(key);
    }

    public int getInt(String key) {
        return (Integer) get(key);
    }

    public float getFloat(String key) {
        return (Float) get(key);
    }

    @Override
    public String toString() {
        return "AnalysisOptions" + options;
    }
}

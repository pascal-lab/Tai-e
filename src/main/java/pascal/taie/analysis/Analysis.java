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

import pascal.taie.config.AnalysisConfig;

import java.util.Map;
import java.util.Set;

import static pascal.taie.util.collection.CollectionUtils.newHybridSet;

public abstract class Analysis {

    private final String id;

    // private boolean isStoreResult;

    private final AnalysisOptions options;

    protected Analysis(AnalysisConfig config) {
        this.id = config.getId();
        this.options = new AnalysisOptions(config.getOptions());
    }

    public String getId() {
        return id;
    }

    public AnalysisOptions getOptions() {
        return options;
    }
}

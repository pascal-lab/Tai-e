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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.AnalysisOptions;
import pascal.taie.config.ConfigException;

import java.lang.reflect.Field;

/**
 * Abstract base class for all analyses.
 */
public abstract class Analysis {

    private static final Logger logger = LogManager.getLogger(Analysis.class);

    /**
     * Configuration of this analysis.
     */
    private final AnalysisConfig config;

    // private boolean isStoreResult;

    protected Analysis(AnalysisConfig config) {
        this.config = config;
        validateId();
    }

    public String getId() {
        return config.getId();
    }

    public AnalysisOptions getOptions() {
        return config.getOptions();
    }

    /**
     * Checks if the ID in the config file and the ID in the program
     * of an analysis are consistent.
     *
     * We assume that each analysis class has a static field named "ID",
     * which is its analysis id used in Tai-e.
     */
    private void validateId() {
        Class<?> analysisClass = getClass();
        try {
            Field idField = analysisClass.getField("ID");
            idField.setAccessible(true);
            String id = (String) idField.get(null);
            if (!id.equals(getId())) {
                throw new ConfigException(String.format(
                        "Config ID (%s) and analysis ID (%s) of %s are not matched",
                        getId(), id, analysisClass));
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.warn("Failed to obtain analysis ID of {}", analysisClass);
        }
    }
}

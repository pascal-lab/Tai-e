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
     * <p>
     * We assume that each analysis class has a static field named "ID",
     * which is its analysis id used in Tai-e.
     */
    private void validateId() {
        Class<?> analysisClass = getClass();
        try {
            Field idField = analysisClass.getField("ID");
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

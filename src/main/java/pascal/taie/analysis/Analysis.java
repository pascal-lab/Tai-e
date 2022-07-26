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

import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.AnalysisOptions;
import pascal.taie.config.ConfigException;
import pascal.taie.util.AnalysisException;

import java.lang.reflect.Field;

/**
 * Abstract base class for all analyses.
 */
public abstract class Analysis {

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
     * Checks if this analysis class declares a public static field "ID"
     * and its value is identical to the analysis id in the configuration.
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
            throw new AnalysisException(String.format("Failed to get analysis ID of %s," +
                    " please add public static field 'ID' in %s" +
                    analysisClass, analysisClass));
        }
    }
}

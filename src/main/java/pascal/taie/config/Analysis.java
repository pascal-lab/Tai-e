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

import pascal.taie.util.HashUtils;

import java.util.Collections;
import java.util.Map;

import static pascal.taie.util.collection.CollectionUtils.newHybridMap;

public class Analysis {

    private final String description;

    private final String analysisName;

    private Class<?> analysisClass;

    private final String id;

    private final String resultName;

    private Class<?> resultClass;

    private Map<Analysis, String> requires = newHybridMap();

    private final Map<String, Object> options;

    public Analysis(ConfigItem item) {
        this.description = item.getDescription();
        this.analysisName = item.getAnalysisClass();
        this.id = item.getId();
        this.resultName = item.getResultClass();
        this.options = item.getOptions();
    }

    public String getDescription() {
        return description;
    }

    public Class<?> getAnalysisClass() {
        if (analysisClass == null) {
            try {
                analysisClass = Class.forName(analysisName);
            } catch (ClassNotFoundException e) {
                throw new ConfigException(analysisName + " is not found");
            }
        }
        return analysisClass;
    }

    public String getId() {
        return id;
    }

    public Class<?> getResultClass() {
        if (resultClass == null) {
            try {
                resultClass = Class.forName(resultName);
            } catch (ClassNotFoundException e) {
                throw new ConfigException(resultName + " is not found");
            }
        }
        return resultClass;
    }

    void addRequire(Analysis analysis, String conditions) {
        requires.put(analysis, conditions);
    }

    public Map<Analysis, String> getRequires() {
        return Collections.unmodifiableMap(requires);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Analysis analysis = (Analysis) o;
        return id.equals(analysis.id) &&
                analysisName.equals(analysis.analysisName);
    }

    @Override
    public int hashCode() {
        return HashUtils.hash(id, analysisName);
    }

    @Override
    public String toString() {
        return "Analysis{" +
                "analysisName='" + analysisName + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}

/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.analysis.pta.plugin.taint;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.config.ConfigException;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TaintAnalysis implements Plugin {

    private Solver solver;

    private ClassHierarchy hierarchy;

    private Set<JMethod> sources;

    private Set<JMethod> sinks;

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        this.hierarchy = solver.getHierarchy();
        readSourcesSinks(solver.getOptions().getString("taint.sources-sinks"));;
    }

    private void readSourcesSinks(String path) {
        Objects.requireNonNull(path, "taint.sources-sinks is not given");
        File file = new File(path);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JavaType type = mapper.getTypeFactory()
                .constructMapType(Map.class, String.class, List.class);
        try {
            Map<String, List<String>> sourcesSinks = mapper.readValue(file, type);
            sources = sourcesSinks.get("sources")
                    .stream()
                    .map(hierarchy::getMethod)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toUnmodifiableSet());
            sinks = sourcesSinks.get("sinks")
                    .stream()
                    .map(hierarchy::getMethod)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException | NullPointerException e) {
            throw new ConfigException(
                    "Failed to read sources and sinks from " + file, e);
        }
    }
}

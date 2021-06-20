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

package pascal.taie.analysis.pta.plugin.reflection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.MemberRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;
import pascal.taie.util.collection.MapUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class LogBasedModel extends MetaObjModel {

    private static final Logger logger = LogManager.getLogger(LogBasedModel.class);

    private final Map<String, String> fullNames = Map.of(
            "Class", StringReps.CLASS,
            "Constructor", StringReps.CONSTRUCTOR,
            "Method", StringReps.METHOD,
            "Field", StringReps.FIELD
    );

    private Map<Invoke, Set<JClass>> classTargets;

    private Map<Invoke, Set<MemberRef>> memberTargets;

    LogBasedModel(Solver solver) {
        super(solver);
        loadReflectionLog(solver.getOptions().getString("reflection-log"));
    }

    @Override
    void handleNewCSMethod(CSMethod csMethod) {

    }

    private void loadReflectionLog(String path) {
        classTargets = MapUtils.newMap();
        memberTargets = MapUtils.newMap();
        readItems(path).forEach(this::addItem);
    }

    private List<Item> readItems(String path) {
        try {
            return Files.readAllLines(Path.of(path)).stream()
                    .map(line -> {
                        String[] split = line.split(";");
                        String api = split[0];
                        String target = split[1];
                        String caller = split[2];
                        String s3 = split[3];
                        int lineNumber = s3.isBlank() ?
                                Item.UNKNOWN : Integer.parseInt(s3);
                        return new Item(api, target, caller, lineNumber);
                    })
                    .collect(Collectors.toUnmodifiableList());
        } catch (IOException e) {
            logger.error("Failed to load reflection log from {}", path);
            return List.of();
        }
    }

    private void addItem(Item item) {
        switch (item.api) {
            case "Class.newInstance": {
                break;
            }
            case "Constructor.newInstance":
            case "Method.invoke": {
                break;
            }
        }
    }

    private List<Invoke> getMatchedInvokes(Item item) {
        int lastDot = item.caller.lastIndexOf('.');
        String callerClass = item.caller.substring(0, lastDot);
        String callerMethod = item.caller.substring(lastDot + 1);
        JClass klass = hierarchy.getClass(callerClass);
        if (klass == null) {
            logger.warn("Class '{}' is absent", callerClass);
            return List.of();
        }
        List<Invoke> invokes = new ArrayList<>();
        klass.getDeclaredMethods()
                .stream()
                .filter(m -> m.getName().equals(callerMethod) && !m.isAbstract())
                .forEach(caller -> {
                    caller.getIR().getStmts()
                            .stream()
                            .filter(s -> s instanceof Invoke)
                            .forEach(s -> {
                                Invoke invoke = (Invoke) s;
                                if (isMatched(item, invoke)) {
                                    invokes.add(invoke);
                                }
                            });
                });
        if (invokes.isEmpty()) {
            logger.warn("No matched invokes found for {}/{}",
                    item.caller, item.lineNumber);
        }
        return invokes;
    }
    
    private boolean isMatched(Item item, Invoke invoke) {
        if (invoke.isDynamic()) {
            return false;
        }
        int lastDot = item.api.lastIndexOf('.');
        String apiClass = fullNames.get(item.api.substring(0, lastDot));
        String apiMethod = item.api.substring(lastDot + 1);
        JMethod callee = invoke.getMethodRef().resolve();
        return callee.getDeclaringClass().getName().equals(apiClass) &&
                callee.getName().equals(apiMethod) &&
                (item.lineNumber == Item.UNKNOWN ||
                        item.lineNumber == invoke.getLineNumber());
    }

    /**
     * Represents log items.
     */
    private static class Item {

        private final String api;

        private final String target;

        private final String caller;

        private final int lineNumber;

        private static final int UNKNOWN = -1;

        private Item(String api, String target, String caller, int lineNumber) {
            this.api = api;
            this.target = target;
            this.caller = caller;
            this.lineNumber = lineNumber;
        }
    }

    @Override
    protected void registerVarAndHandler() {
    }

    @Override
    public void handleNewInvoke(Invoke invoke) {
    }

    @Override
    public boolean isRelevantVar(Var var) {
        return false;
    }

    @Override
    public void handleNewPointsToSet(CSVar csVar, PointsToSet pts) {
    }
}

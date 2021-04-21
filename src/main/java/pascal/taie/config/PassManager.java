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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class PassManager {

    private final Map<String, Pass> passes = new LinkedHashMap<>();

    public PassManager(ConfigItem[] items) {
        for (ConfigItem item : items) {
            addAnalysis(item);
        }
        buildRequires(items);
    }

    private void addAnalysis(ConfigItem item) {
        if (passes.containsKey(item.getId())) {
            throw new ConfigException(String.format(
                    "Adding analysis %s failed: %s already exists.",
                    item, item.getId()));
        }
        passes.put(item.getId(), new Pass(item));
    }

    private void buildRequires(ConfigItem[] items) {
        for (ConfigItem item : items) {
            item.getRequires().forEach(required -> {
                String id = Utils.extractId(required);
                String conditions = Utils.extractConditions(required);
                passes.get(item.getId())
                        .addRequire(passes.get(id), conditions);
            });
        }
    }

    public Pass getPass(String id) {
        return passes.get(id);
    }

    public Stream<Pass> passes() {
        return passes.values().stream();
    }
}

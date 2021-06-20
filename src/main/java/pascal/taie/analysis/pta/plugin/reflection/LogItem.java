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

package pascal.taie.analysis.pta.plugin.reflection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents log items.
 */
public class LogItem {

    private static final Logger logger = LogManager.getLogger(LogItem.class);

    public final String api;

    public final String target;

    public final String caller;

    public final int lineNumber;

    public static final int UNKNOWN = -1;

    private LogItem(String api, String target, String caller, int lineNumber) {
        this.api = api;
        this.target = target;
        this.caller = caller;
        this.lineNumber = lineNumber;
    }

    public static List<LogItem> load(String path) {
        try {
            return Files.readAllLines(Path.of(path))
                    .stream()
                    .map(line -> {
                        String[] split = line.split(";");
                        String api = split[0];
                        String target = split[1];
                        String caller = split[2];
                        String s3 = split[3];
                        int lineNumber = s3.isBlank() ?
                                LogItem.UNKNOWN : Integer.parseInt(s3);
                        return new LogItem(api, target, caller, lineNumber);
                    })
                    .collect(Collectors.toUnmodifiableList());
        } catch (IOException e) {
            logger.error("Failed to load reflection log from {}", path);
            return List.of();
        }
    }
}

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

package pascal.taie.analysis.pta.plugin.reflection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
                        String[] split = line.split(";", -1);
                        String api = split[0];
                        if (api.startsWith("Field")) {
                            api = api.replace("get*", "get")
                                    .replace("set*", "set");
                        }
                        String target = split[1];
                        String caller = split[2];
                        String s3 = split[3];
                        int lineNumber = s3.isBlank() ?
                                LogItem.UNKNOWN : Integer.parseInt(s3);
                        return new LogItem(api, target, caller, lineNumber);
                    })
                    .toList();
        } catch (IOException e) {
            logger.error("Failed to load reflection log from {}", path);
            return List.of();
        }
    }
}

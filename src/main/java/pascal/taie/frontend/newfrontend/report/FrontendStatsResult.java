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

package pascal.taie.frontend.newfrontend.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import pascal.taie.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FrontendStatsResult {
    private static FrontendStats stats;

    public static void setStats(FrontendStats stats) {
        FrontendStatsResult.stats = stats;
    }

    public static FrontendStats getStats() {
        return stats;
    }

    public static void dumpStats() throws IOException {
        Path p = Path.of("output/frontend_stats.yml");
        // dump with jackson
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.writeValue(p.toFile(), stats);
    }

    public static void dumpCastingInfo() throws IOException {
        for (var castingInfo : stats.castingInfos().values()) {
            Path p = Path.of("output/casting/" + castingInfo.method().getName() + ".yml");
            // mkdir -p
            if (!Files.exists(p.getParent()) || !Files.isDirectory(p.getParent())) {
                Files.createDirectories(p.getParent());
            }
            // dump with jackson
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            objectMapper.writeValue(p.toFile(), castingInfo);
            TaieCastingHierarchyDumper.writeCastingToDot(
                    Path.of("output/casting"), castingInfo);
        }
    }

    static {
        World.registerResetCallback(() -> {
            stats = null;
        });
    }
}

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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.util.List;
import java.util.Map;

/**
 * FrontendStats is a record that holds various statistics related to the frontend
 * @param projectBuildingTime in milliseconds
 * @param closedWorldConstructionTime in milliseconds
 * @param classHierarchyConstructionTime in milliseconds
 * @param bytecodeParsingTime a map of JClass to the time taken to parse the bytecode in nanoseconds
 * @param irConstructionTime a map of JMethod to IRConstructionTime
 * @param stackMergeStats a map of JMethod to StackMergeStats
 * @param castingInfos a map of JMethod to TaieCastingInfo
 */
@JsonSerialize
public record FrontendStats(
        long projectBuildingTime,
        long closedWorldConstructionTime,
        long classHierarchyConstructionTime,
        Map<JClass, Long> bytecodeParsingTime,
        Map<JMethod, IRConstructionTime> irConstructionTime,
        Map<JMethod, StackMergeStats> stackMergeStats,
        Map<JMethod, TaieCastingInfo> castingInfos
) {
    /**
     * Get the total time taken for IR construction in milliseconds
     */
    public long totalBCSSATime() {
        return irConstructionTime.values().stream()
                .mapToLong(IRConstructionTime::bcSSATime)
                .sum() / 1000;
    }

    /**
     * Get the total time taken for BC3A construction in milliseconds
     */
    public long totalBC3ACTime() {
        return irConstructionTime.values().stream()
                .mapToLong(IRConstructionTime::bc3ACTime)
                .sum() / 1000;
    }

    /**
     * Get the total time taken for typeless IR time in milliseconds
     */
    public long totalTypelessTime() {
        return totalBC3ACTime() + totalBCSSATime();
    }

    /**
     * Get the total time taken for type inference in milliseconds
     */
    public long totalTypeInferenceTime() {
        return irConstructionTime.values().stream()
                .mapToLong(IRConstructionTime::typeInferenceTime)
                .sum() / 1000;
    }

    /**
     * Get the total time taken for bytecode parsing in milliseconds
     */
    public long totalBytecodeParsingTime() {
        return bytecodeParsingTime.values().stream()
                .mapToLong(Long::longValue)
                .sum() / 1000;
    }

    public List<TaieCastingInfo> totalCastingInfos() {
        return castingInfos.values().stream()
                .toList();
    }
}

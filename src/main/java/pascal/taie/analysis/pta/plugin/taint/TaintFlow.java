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

package pascal.taie.analysis.pta.plugin.taint;

import javax.annotation.Nonnull;
import java.util.Comparator;

/**
 * Each instance represents a taint flow from source to sink.
 */
public record TaintFlow(SourcePoint sourcePoint, SinkPoint sinkPoint)
        implements Comparable<TaintFlow> {

    private static final Comparator<TaintFlow> COMPARATOR =
            Comparator.comparing(TaintFlow::sourcePoint)
                    .thenComparing(TaintFlow::sinkPoint);

    @Override
    public int compareTo(@Nonnull TaintFlow other) {
        return COMPARATOR.compare(this, other);
    }

    @Override
    public String toString() {
        return String.format("TaintFlow{%s -> %s}", sourcePoint, sinkPoint);
    }
}

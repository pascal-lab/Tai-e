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

package pascal.taie.analysis.sideeffect;

import pascal.taie.util.collection.Maps;
import pascal.taie.util.graph.Graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Topologically sorts a directed graph using BFS.
 * It is assumed that the given graph is a direct acyclic graph (DAG). If not, the results will not
 * contain any nodes in the cycles.
 *
 * @param <N> type of nodes
 */
class TopologicalSorter<N> {

    private final List<N> sortedList;

    TopologicalSorter(Graph<N> graph, boolean reverse) {
        Map<N, Long> inDegreeMap = Maps.newMap();
        Queue<N> queue = new ArrayDeque<>();
        for (N node : graph.getNodes()) {
            long inDegree = graph.getInDegreeOf(node);
            inDegreeMap.put(node, inDegree);
            if (inDegree == 0) {
                queue.add(node);
            }
        }
        sortedList = new ArrayList<>(graph.getNumberOfNodes());
        while (!queue.isEmpty()) {
            N curr = queue.poll();
            sortedList.add(curr);
            for (N pred : graph.getSuccsOf(curr)) {
                long inDegree = inDegreeMap.get(pred) - 1;
                inDegreeMap.put(pred, inDegree);
                if (inDegree == 0) {
                    queue.add(pred);
                }
            }
        }
        if (reverse) {
            Collections.reverse(sortedList);
        }
    }

    /**
     * @return the topologically sorted list.
     */
    public List<N> get() {
        return sortedList;
    }
}

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

package pascal.taie.analysis.pta.toolkit.mahjong;

import org.apache.logging.log4j.Level;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.config.AnalysisOptions;
import pascal.taie.language.type.Type;
import pascal.taie.util.Timer;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.UnionFindSet;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class Mahjong {

    private static final DFAEquivChecker dfaEqChecker = new DFAEquivChecker();

    private DFAFactory dfaFactory;

    /**
     * This map may be manipulated by multiple threads simultaneously.
     */
    private ConcurrentMap<Obj, Boolean> canMerged;

    public static HeapModel run(PointerAnalysisResult pta,
                                AnalysisOptions options) {
        return new Mahjong().buildHeapModel(pta, options);
    }

    HeapModel buildHeapModel(PointerAnalysisResult pta,
                             AnalysisOptions options) {
        FieldPointsToGraph fpg = Timer.runAndCount(
                () -> new FieldPointsToGraph(pta),
                "Building field points-to graph", Level.INFO);
        dfaFactory = Timer.runAndCount(() -> new DFAFactory(fpg),
                "Building DFA", Level.INFO);
        UnionFindSet<Obj> uf = Timer.runAndCount(
                () -> mergeTypeConsistentObjects(fpg),
                "Merging type-consistent objects", Level.INFO);
        // build resulting heap model based on merge map
        return new MahjongHeapModel(options, uf.getDisjointSets());
    }

    private UnionFindSet<Obj> mergeTypeConsistentObjects(FieldPointsToGraph fpg) {
        Set<Obj> allObjs = fpg.getObjects();
        canMerged = Maps.newConcurrentMap(allObjs.size());
        UnionFindSet<Obj> uf = new UnionFindSet<>(allObjs);
        // group the objects by their types
        Map<Type, Set<Obj>> groupedObjs = allObjs.stream()
                .collect(Collectors.groupingBy(
                        Obj::getType, Collectors.toSet()));
        // compute object merging, and store results in a union-find set
        groupedObjs.entrySet()
                .parallelStream()
                .forEach(entry -> {
                    Set<Obj> objs = entry.getValue();
                    DFAMap dfaMap = new DFAMap();
                    for (Obj o1 : objs) {
                        if (canBeMerged(o1, dfaMap)) {
                            for (Obj o2 : objs) {
                                if (canBeMerged(o2, dfaMap)) {
                                    if (o1.getIndex() < o2.getIndex()
                                            && !uf.isConnected(o1, o2)
                                            && canBeMerged(o1, o2, dfaMap)) {
                                        uf.union(o1, o2);
                                    }
                                }
                            }
                        }
                    }
                });
        return uf;
    }

    /**
     * @return {@code true} if o1 and o2 can be merged.
     */
    private boolean canBeMerged(Obj o1, Obj o2, DFAMap dfaMap) {
        DFA dfa1 = dfaMap.getDFA(o1);
        DFA dfa2 = dfaMap.getDFA(o2);
        return dfaEqChecker.isEquivalent(dfa1, dfa2);
    }

    /**
     * @return {@code true} if o can be merged with other objects.
     */
    private boolean canBeMerged(Obj o, DFAMap dfaMap) {
        if (!canMerged.containsKey(o)) {
            boolean result = true;
            // Check whether the types of objects pointed (directly/indirectly)
            // by o are single.
            DFA dfa = dfaMap.getDFA(o);
            for (DFAState s : dfa.getStates()) {
                if (dfa.outputOf(s).size() > 1) {
                    // o (directly/indirectly) points to objects of multiple types
                    result = false;
                    break;
                }
            }
            canMerged.put(o, result);
        }
        return canMerged.get(o);
    }

    /**
     * During equivalence check, each thread holds a DFAMap which contains
     * the DFA of the objects of the type.
     */
    private class DFAMap {

        private final Map<Obj, DFA> map = Maps.newMap();

        private DFA getDFA(Obj o) {
            return map.computeIfAbsent(o, dfaFactory::getDFA);
        }
    }
}

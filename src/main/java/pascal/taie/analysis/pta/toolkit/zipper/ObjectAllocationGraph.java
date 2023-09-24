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

package pascal.taie.analysis.pta.toolkit.zipper;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.toolkit.PointerAnalysisResultEx;
import pascal.taie.analysis.pta.toolkit.util.OAGs;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.Type;
import pascal.taie.util.Canonicalizer;
import pascal.taie.util.Indexer;
import pascal.taie.util.collection.IndexerBitSet;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.graph.MergedNode;
import pascal.taie.util.graph.MergedSCCGraph;
import pascal.taie.util.graph.SimpleGraph;
import pascal.taie.util.graph.TopologicalSorter;

import java.util.Map;
import java.util.Set;

/**
 * Object allocation graph tailored for Zipper.
 */
class ObjectAllocationGraph extends SimpleGraph<Obj> {

    private final Map<Obj, Set<Obj>> obj2Allocatees = Maps.newMap();

    private final Map<Type, Set<Obj>> type2Allocatees = Maps.newConcurrentMap();

    private Indexer<Obj> objIndexer;

    ObjectAllocationGraph(PointerAnalysisResultEx pta) {
        OAGs.computeInvokedMethods(pta).forEach((obj, methods) -> {
            addNode(obj);
            methods.stream()
                    .map(pta::getObjectsAllocatedIn)
                    .flatMap(Set::stream)
                    .forEach(succ -> {
                        if (!(obj.getType() instanceof ArrayType)) {
                            addEdge(obj, succ);
                        }
                    });
        });
        objIndexer = pta.getBase().getObjectIndexer();
        computeAllocatees(pta);
        objIndexer = null;
        assert getNumberOfNodes() == pta.getBase().getObjects().size();
    }

    Set<Obj> getAllocateesOf(Type type) {
        return type2Allocatees.get(type);
    }

    private Set<Obj> getAllocateesOf(Obj obj) {
        return obj2Allocatees.get(obj);
    }

    private void computeAllocatees(PointerAnalysisResultEx pta) {
        // compute allocatees of objects
        MergedSCCGraph<Obj> mg = new MergedSCCGraph<>(this);
        TopologicalSorter<MergedNode<Obj>> sorter = new TopologicalSorter<>(mg, true);
        Canonicalizer<Set<Obj>> canonicalizer = new Canonicalizer<>();
        sorter.get().forEach(node -> {
            Set<Obj> allocatees = canonicalizer.get(getAllocatees(node, mg));
            node.getNodes().forEach(obj -> obj2Allocatees.put(obj, allocatees));
        });
        // compute allocatees of types
        pta.getObjectTypes().parallelStream().forEach(type -> {
            Set<Obj> allocatees = new IndexerBitSet<>(objIndexer, true);
            pta.getObjectsOf(type)
                    .forEach(o -> allocatees.addAll(getAllocateesOf(o)));
            type2Allocatees.put(type, canonicalizer.get(allocatees));
        });
    }

    private Set<Obj> getAllocatees(
            MergedNode<Obj> node, MergedSCCGraph<Obj> mg) {
        Set<Obj> allocatees = new IndexerBitSet<>(objIndexer, true);
        mg.getSuccsOf(node).forEach(n -> {
            // direct allocatees
            allocatees.addAll(n.getNodes());
            // indirect allocatees
            Obj o = n.getNodes().get(0);
            allocatees.addAll(getAllocateesOf(o));
        });
        Obj obj = node.getNodes().get(0);
        if (node.getNodes().size() > 1 ||
                getSuccsOf(obj).contains(obj)) { // self-loop
            // The merged node is a true SCC
            allocatees.addAll(node.getNodes());
        }
        return allocatees;
    }
}

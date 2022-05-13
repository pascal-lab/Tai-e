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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.toolkit.PointerAnalysisResultEx;
import pascal.taie.analysis.pta.toolkit.PointerAnalysisResultExImpl;
import pascal.taie.ir.stmt.New;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.Timer;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.graph.Reachability;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Zipper {

    private static final Logger logger = LogManager.getLogger(Zipper.class);

    private static final float DEFAULT_THRESHOLD = 0.05f;

    private final PointerAnalysisResultEx pta;

    private final boolean isExpress;

    private final float expressThreshold;

    private final ObjectAllocationGraph oag;

    private final PotentialContextElement pce;

    private final ObjectFlowGraph ofg;

    private AtomicInteger analyzedClasses;

    private AtomicInteger totalPFGNodes;

    private AtomicInteger totalPFGEdges;

    private Map<Type, Collection<JMethod>> pcmMap;


    public Zipper(PointerAnalysisResult ptaBase, boolean isExpress) {
        this(ptaBase, isExpress, DEFAULT_THRESHOLD);
    }

    public Zipper(PointerAnalysisResult ptaBase,
                  boolean isExpress, float expressThreshold) {
        this.pta = new PointerAnalysisResultExImpl(ptaBase);
        this.isExpress = isExpress;
        this.expressThreshold = expressThreshold;
        this.oag = Timer.runAndCount(() -> new ObjectAllocationGraph(pta),
            "Building OAG", Level.INFO);
        this.pce = Timer.runAndCount(() -> new PotentialContextElement(pta, oag),
            "Building PCE", Level.INFO);
        this.ofg = Timer.runAndCount(() -> new ObjectFlowGraph(ptaBase),
            "Building OFG", Level.INFO);
    }

    /**
     * @return a set of precision-critical methods that should be analyzed
     * context-sensitively.
     */
    public Set<JMethod> selectPrecisionCriticalMethods() {
//        FlowGraphDumper.dump(ofg,
//            "output/" + World.get().getMainMethod().getDeclaringClass() + "-ofg.dot");

        analyzedClasses = new AtomicInteger(0);
        totalPFGNodes = new AtomicInteger(0);
        totalPFGEdges = new AtomicInteger(0);
        pcmMap = new ConcurrentHashMap<>(1024);
        List<Type> types = pta.getBase().getObjects()
            .stream()
            .map(Obj::getType)
            .distinct()
            .collect(Collectors.toList());
        types.forEach(this::analyze);
        return collectAllPrecisionCriticalMethods();
    }

    private void analyze(Type type) {
        PrecisionFlowGraph pfg = new PFGBuilder(pta, ofg, oag, pce, type).build();
//        FlowGraphDumper.dump(pfg,
//            "output/" + World.get().getMainMethod().getDeclaringClass() + "-" + type + "-pfg.dot");
        pcmMap.put(type, getPrecisionCriticalMethods(pfg));
    }

    private Set<JMethod> getPrecisionCriticalMethods(PrecisionFlowGraph pfg) {
        return getFlowNodes(pfg).stream()
            .map(this::node2Method)
            .filter(Objects::nonNull)
            .filter(pce.PCEMethodsOf(pfg.getType())::contains)
            .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<OFGNode> getFlowNodes(PrecisionFlowGraph pfg) {
        Reachability<OFGNode> reachability = new Reachability<>(pfg);
        Set<OFGNode> nodes = Sets.newSet();
        for (VarNode outNode : pfg.getOutNodes()) {
            nodes.addAll(reachability.nodesReach(outNode));
        }
        return nodes;
    }

    /**
     * @return containing method of {@code node}.
     */
    private @Nullable JMethod node2Method(OFGNode node) {
        if (node instanceof VarNode varNode) {
            return varNode.getVar().getMethod();
        } else {
            Obj base = ((InstanceNode) node).getBase();
            if (base.getAllocation() instanceof New newStmt) {
                return newStmt.getContainer();
            }
        }
        return null;
    }

    private Set<JMethod> collectAllPrecisionCriticalMethods() {
        Set<JMethod> pcm = Sets.newSet();
        pcmMap.forEach((unused, pcms) -> {
//            if (isExpress &&
//                getAccumulativePointsToSetSize(pcms) > pcmThreshold) {
//                return;
//            }
            pcm.addAll(pcms);
        });
        logger.info("#precision-critical methods: {}", pcm.size());
        return pcm;
    }
}

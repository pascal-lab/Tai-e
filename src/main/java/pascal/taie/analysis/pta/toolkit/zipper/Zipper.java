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
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.Timer;
import pascal.taie.util.graph.Graph;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
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
        FlowGraphDumper.dump(ofg,
            "output/" + World.get().getMainMethod().getDeclaringClass() + "-ofg.dot");
        List<Type> types = pta.getBase().getObjects()
            .stream()
            .map(Obj::getType)
            .distinct()
            .sorted(Comparator.comparing(Type::toString))
            .collect(Collectors.toList());
        types.forEach(t -> {
            PFGBuilder builder = new PFGBuilder(pta, ofg, oag, pce, t);
            Graph<OFGNode> pfg = Timer.runAndCount(builder::build,
                "Building PFG for " + t, Level.INFO);
            FlowGraphDumper.dump(pfg,
                "output/" + World.get().getMainMethod().getDeclaringClass() + "-" + t + "-pfg.dot");
        });
        return Set.of();
    }
}

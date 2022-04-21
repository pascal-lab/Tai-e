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

package pascal.taie.analysis.pta.toolkit.scaler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.toolkit.PointerAnalysisResultEx;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.graph.Graph;

import java.util.List;
import java.util.Set;

class _2TypeContextComputer extends ContextComputer {

    private static final Logger logger = LogManager.getLogger(_2TypeContextComputer.class);

    private final Graph<Obj> oag;

    _2TypeContextComputer(PointerAnalysisResultEx pta, Graph<Obj> oag) {
        super(pta);
        this.oag = oag;
    }

    @Override
    String getVariantName() {
        return "2-type";
    }

    @Override
    int computeContextNumberOf(JMethod method) {
        if (pta.getReceiverObjectsOf(method).isEmpty()) {
            logger.debug("Empty receiver: {}", method);
            return 1;
        }
        Set<List<Type>> contexts = Sets.newHybridSet();
        for (Obj recv : pta.getReceiverObjectsOf(method)) {
            int inDegree = oag.getInDegreeOf(recv);
            if (inDegree > 0) {
                oag.getPredsOf(recv).forEach(pred ->
                        contexts.add(List.of(pred.getContainerType(),
                                recv.getContainerType())));
            } else { // without allocator, back to 1-type
                contexts.add(List.of(recv.getContainerType()));
            }
        }
        return contexts.size();
    }
}

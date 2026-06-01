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

package pascal.taie.analysis.pta.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.exp.Var;
import pascal.taie.util.Indexer;
import pascal.taie.util.collection.IndexerBitSet;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MayAliasPair extends ProgramAnalysis<MayAliasPair.MayAliasPairResult> {

    private static final Logger logger = LogManager.getLogger(MayAliasPair.class);

    public static final String ID = "may-alias-pair";

    private PointerAnalysisResult ptaResult;

    public MayAliasPair(AnalysisConfig config) {
        super(config);
    }

    @Override
    public MayAliasPairResult analyze() {
        ptaResult = World.get().getResult(PointerAnalysis.ID);
        Set<Var> vars = Sets.newSet(ptaResult.getVars());
        Set<Var> appVars = vars.stream()
                .filter(MayAliasPair::isApp)
                .collect(Collectors.toUnmodifiableSet());

        long nAliasPairs = computeMayAliasPairs(vars);
        long nVars = vars.size();

        long nAppAliasPairs = computeMayAliasPairs(appVars);
        long nAppVars = appVars.size();

        // Log statistics
        logger.info("#{}: found {} in {} variable pairs",
                ID, nAliasPairs, (nVars - 1) * nVars / 2);
        logger.info("#{}: found {} in {} variable pairs (app)",
                ID, nAppAliasPairs, (nAppVars - 1) * nAppVars / 2);

        return new MayAliasPairResult(nAliasPairs, nAppAliasPairs);
    }

    private long computeMayAliasPairs(Set<Var> vars) {
        VarIndexer indexer = new VarIndexer(vars);
        Map<Obj, Set<Var>> obj2Vars = Maps.newMap();
        vars.forEach(v -> {
            for (Obj obj : ptaResult.getPointsToSet(v)) {
                obj2Vars.computeIfAbsent(obj, k -> indexer.makeIndexerBitSet())
                        .add(v);
            }
        });

        // mayAlias(u, v) if
        //   exists o, s.t. in(o, pts(u)) and in(o, pts(v))
        long nAliasPairs = vars.parallelStream()
                .mapToLong(v -> {
                    Set<Var> aliasVars = indexer.makeIndexerBitSet();
                    for (Obj o : ptaResult.getPointsToSet(v)) {
                        aliasVars.addAll(obj2Vars.getOrDefault(o, Collections.emptySet()));
                    }
                    // v may alias to itself, but we do not count in this case
                    aliasVars.remove(v);
                    return aliasVars.size();
                }).sum();
        // mayAlias(u, v) iff mayAlias(v, u), so a pair is counted twice
        return nAliasPairs / 2;
    }

    private static boolean isApp(Var v) {
        return v.getMethod().isApplication();
    }

    public record MayAliasPairResult(long aliasPairs, long appAliasPairs) {
    }

    // A global indexer for Vars
    private static class VarIndexer implements Indexer<Var> {

        private static final boolean IS_SPARSE = true;

        private final Var[] vars;

        private final Map<Var, Integer> varIndexMap = Maps.newMap();

        VarIndexer(Collection<Var> vars) {
            this.vars = vars.stream()
                    .distinct()
                    .toArray(Var[]::new);
            for (int index = 0; index < this.vars.length; index++) {
                varIndexMap.put(this.vars[index], index);
            }
        }

        public IndexerBitSet<Var> makeIndexerBitSet() {
            return new IndexerBitSet<>(this, IS_SPARSE);
        }

        @Override
        public int getIndex(Var o) {
            return varIndexMap.get(o);
        }

        @Override
        public Var getObject(int index) {
            return vars[index];
        }
    }
}

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

package pascal.taie.analysis.bugfinder.security.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.bugfinder.BugInstance;
import pascal.taie.analysis.bugfinder.security.SecurityBugInfo;
import pascal.taie.analysis.bugfinder.security.injection.InjectionDetector;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.taint.Sink;
import pascal.taie.analysis.pta.plugin.taint.TaintAnalysis;
import pascal.taie.analysis.pta.plugin.taint.TaintConfig;
import pascal.taie.analysis.pta.plugin.taint.TaintFlow;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.Maps;

import javax.annotation.Nullable;
import java.util.*;

public class TaintResultHandler {

    private final Map<Sink, SecurityBugInfo> sinkInfoMap = Maps.newMap();

    private static final Logger logger = LogManager.getLogger(TaintResultHandler.class);


    public TaintResultHandler(List<SecurityBugInfo> bugInfoList, ClassHierarchy hierarchy, TypeSystem typeSystem) {
        bugInfoList.forEach(
                bugInfo -> TaintConfig.loadConfig(bugInfo.getConfigPath(), hierarchy, typeSystem)
                        .sinks()
                        .forEach(sink -> sinkInfoMap.put(sink, bugInfo)));
    }

    public Set<BugInstance> handle() {
        Set<TaintFlow> taintFlows = getTaintResult();
        if (taintFlows == null)
        {throw new AnalysisException("Taint analysis should be run before TaintResultHandler starts");}
        return matchBugs(taintFlows);
    }

    @Nullable
    private Set<TaintFlow> getTaintResult() {
        PointerAnalysisResult ptaResult = World.get().getResult(PointerAnalysis.ID);
        if (ptaResult != null && ptaResult.hasResult(TaintAnalysis.class.getName())) {
            return Collections.unmodifiableSet(ptaResult.getResult(TaintAnalysis.class.getName()));
        } else {
            return null;
        }
    }

    private Set<BugInstance> matchBugs(Set<TaintFlow> taintFlows) {
        if (taintFlows.isEmpty()) {return Set.of();}
        Set<BugInstance> bugSet = new TreeSet<>();
        for (TaintFlow taintFlow :
                taintFlows.stream().filter(t -> t.sinkPoint().sinkCall().getContainer().isApplication())
                                    .filter(t -> t.sourcePoint().getContainer().isApplication())
                                    .toList()) {
            Sink sink = new Sink(taintFlow.sinkPoint().sinkCall().getMethodRef().resolve(), taintFlow.sinkPoint().index());
            if (sinkInfoMap.containsKey(sink)) {
                SecurityBugInfo bugInfo = sinkInfoMap.get(sink);
                Invoke sinkCall = taintFlow.sinkPoint().sinkCall();
                //logger.info(taintFlow.sourcePoint().getContainer() + "    " + taintFlow.sourcePoint() + "|||||||||||||" + sinkCall + "at line " + sinkCall.getLineNumber());
                bugSet.add(new TaintBugInstance(bugInfo.getBugType(), bugInfo.getSeverity(), sinkCall.getContainer(), taintFlow.sourcePoint())
                        .setSourceLine(sinkCall.getLineNumber()));
            }
        }
        return bugSet;
    }
}





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

package pascal.taie.analysis.bugfinder.security.insecureapi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.MethodAnalysis;
import pascal.taie.analysis.bugfinder.BugInstance;
import pascal.taie.analysis.bugfinder.security.SecurityBugInfo;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public class InsecureAPIUsageDetector extends MethodAnalysis<Set<BugInstance>> {

    public static final String ID = "insecure-api";

    private static final Logger logger = LogManager.getLogger(InsecureAPIUsageDetector.class);

    /**
     Store the map from methodRef(String) to parameters(Set<String>)
    */
    private final MultiMap<String, String> paramPatternMap;

    /**
     Store the map from InsecureAPI to APIBugInfo
     it's convenient to get the information to create
     the BugInstance via this map
    */
    private final Map<InsecureAPI, SecurityBugInfo> bugInfoMap;

    /**
     * Store all methodRef in the configuration file
     */
    private final Set<String> insecureMethodRef;

    public InsecureAPIUsageDetector(AnalysisConfig config){
        super(config);
        String configPath = config.getOptions().get("config-dir").toString();
        InsecureAPIBugConfig bugConfig = InsecureAPIBugConfig.readConfig(configPath);
        this.paramPatternMap = Maps.newMultiMap();
        this.bugInfoMap = Maps.newMap();
        this.insecureMethodRef = Sets.newSet();

        bugConfig.getBugSet().forEach(bug ->
                bug.getInsecureAPISet().forEach(insecureAPI -> {
                    if(insecureAPI.paramRegex() != null) {
                        paramPatternMap.put(insecureAPI.reference(), insecureAPI.paramRegex());
                    }
                    bugInfoMap.put(insecureAPI, bug);
                    insecureMethodRef.add(insecureAPI.reference());
        }));
    }

    @Override
    public Set<BugInstance> analyze(IR ir) {
        Set<BugInstance> bugInstances = Sets.newHybridSet();
        ir.invokes(false)
                .filter(invoke -> insecureMethodRef.contains(invoke.getMethodRef().toString()))
                .forEach(invoke -> {
            SecurityBugInfo info = getBugInfo(invoke);
            if(info != null){
                bugInstances.add(new BugInstance(info.getBugType(), info.getSeverity(), ir.getMethod())
                        .setSourceLine(invoke.getLineNumber()));
            }
        });
        return bugInstances;
    }

    /**
     Use the information of invoke to get APIBugInfo.
     APIBugInfo may be null, which means matching failed

     @return corresponding APIBugInfo if matching successful, otherwise null
     */
    @Nullable
    private SecurityBugInfo getBugInfo(Invoke invoke){
        String matchedPattern = null;
        for(String patternRegex : paramPatternMap.get(invoke.getMethodRef().toString())){
            if(ParamCondPredictor.test(invoke.getInvokeExp().getArgs(), patternRegex)){
                matchedPattern = patternRegex;
                break;
            }
        }
        return bugInfoMap.get(new InsecureAPI(invoke.getMethodRef().toString(), matchedPattern));
    }
}

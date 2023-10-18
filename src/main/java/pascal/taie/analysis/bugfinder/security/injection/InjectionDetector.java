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

package pascal.taie.analysis.bugfinder.security.injection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.analysis.bugfinder.BugInstance;
import pascal.taie.analysis.bugfinder.security.SecurityBugInfo;
import pascal.taie.analysis.bugfinder.security.detector.Detector;
import pascal.taie.analysis.bugfinder.security.taint.TaintResultHandler;
import pascal.taie.config.AnalysisConfig;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class InjectionDetector extends ProgramAnalysis<Set<BugInstance>> implements Detector {

    public static final String ID = "injection";

    private static final Logger logger = LogManager.getLogger(InjectionDetector.class);

    private final TaintResultHandler taintResultHandler;

    public InjectionDetector(AnalysisConfig config) throws IOException {
        super(config);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        List<SecurityBugInfo> bugInfoList = mapper.readValue(
                new File(config.getOptions().getString("bugInfoList")),
                new TypeReference<>() {});
        this.taintResultHandler = new TaintResultHandler(
                bugInfoList, World.get().getClassHierarchy(), World.get().getTypeSystem());
    }

    @Override
    public Set<BugInstance> analyze() {
        Set<BugInstance> bugInstances = taintResultHandler.handle();
        logger.info("Detected {} injection point(s):", bugInstances.size());
        bugInstances.forEach(logger::info);
        return bugInstances;
    }
}

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

package pascal.taie.analysis.pta.plugin.spring.wec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.solver.DeclaredParamProvider;
import pascal.taie.analysis.pta.core.solver.EntryPoint;
import pascal.taie.analysis.pta.core.solver.ParamProvider;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.core.solver.SpecifiedParamProvider;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.plugin.spring.util.AnnotationManager;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;

public class WecAnalysis implements Plugin {

    private static final Logger logger = LoggerFactory.getLogger(WecAnalysis.class);

    private final WecCollector wecCollector;

    private final Solver solver;

    public WecAnalysis(Solver solver, AnnotationManager annotationManager) {
        this.wecCollector = new WecCollector(solver.getHierarchy(), annotationManager);
        this.solver = solver;
        this.wecCollector.work();
    }

    public void onNewBeanObj(CSObj beanObj) {
        wecCollector.getWebEndpoints(((ClassType) beanObj.getObject().getType()).getJClass())
                .forEach(wec -> {
                    JMethod handlerMethod = wec.handlerMethod();
                    ParamProvider paramProvider = new SpecifiedParamProvider.Builder(handlerMethod)
                            .setDelegate(new DeclaredParamProvider(
                                    handlerMethod, solver.getHeapModel(), 1))
                            .addThisObj(beanObj.getObject())
                            .build();
                    logger.info("[WEC Analysis] Adding entry point: {}",
                            handlerMethod);
                    solver.addEntryPoint(new EntryPoint(handlerMethod, paramProvider));
                });
    }

    @Override
    public void onFinish() {
        wecCollector.processResult(solver.getOptions());
    }
}

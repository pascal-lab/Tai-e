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

package pascal.taie.analysis.pta.plugin.spring;

import pascal.taie.World;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.CompositePlugin;
import pascal.taie.analysis.pta.plugin.spring.di.DiAnalysis;
import pascal.taie.analysis.pta.plugin.spring.util.AnnotationManager;
import pascal.taie.analysis.pta.plugin.spring.util.DirectoryTraverser;
import pascal.taie.analysis.pta.plugin.spring.util.XmlConfiguration;
import pascal.taie.analysis.pta.plugin.spring.wec.WecAnalysis;
import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class SpringAnalysis extends CompositePlugin {

    /**
     * Whether dump Spring analysis results (DI and WEC) or not.
     * This is mainly used for tests and debugging.
     */
    static boolean DUMP_RESULTS = false;

    private DiAnalysis diAnalysis;

    private WecAnalysis wecAnalysis;

    private final List<Consumer<CSObj>> pendingBeanObjListeners = new ArrayList<>();

    /**
     * Registers a listener that will be notified when a new bean object is created.
     * Can be called before or after {@link #setSolver(Solver)}.
     */
    public void addNewBeanObjListener(Consumer<CSObj> listener) {
        pendingBeanObjListeners.add(listener);
    }

    public static boolean shouldDumpResults() {
        return DUMP_RESULTS;
    }

    @Override
    public void setSolver(Solver solver) {
        List<String> appClassPaths = World.get().getOptions().getAppClassPath();
        Set<String> appClassNames = Sets.newSet();
        appClassPaths.parallelStream()
                .map(DirectoryTraverser::listClasses)
                .forEach(classes -> {
                    synchronized (appClassNames) {
                        appClassNames.addAll(classes);
                    }
                });

        AnnotationManager annotationManager = new AnnotationManager(appClassNames);
        XmlConfiguration xmlConfiguration = new XmlConfiguration(appClassPaths);
        annotationManager.initialize();
        xmlConfiguration.initialize();

        diAnalysis = new DiAnalysis(solver, annotationManager, xmlConfiguration);
        wecAnalysis = new WecAnalysis(solver, annotationManager);

        addPlugin(diAnalysis, wecAnalysis);
        super.setSolver(solver);
    }

    @Override
    public void onStart() {
        diAnalysis.registerNewBeanObjListener(wecAnalysis::onNewBeanObj);
        pendingBeanObjListeners.forEach(diAnalysis::registerNewBeanObjListener);
        pendingBeanObjListeners.clear();
        super.onStart();
    }

}

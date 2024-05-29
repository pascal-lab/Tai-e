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

package pascal.taie.analysis.pta.plugin.android;

import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.CompositePlugin;
import pascal.taie.analysis.pta.plugin.android.icc.ICCAnalysis;
import pascal.taie.analysis.pta.plugin.android.lifecycle.LifecycleAnalysis;
import pascal.taie.analysis.pta.plugin.android.misc.AndroidMiscAnalysis;

import java.util.List;

public class AndroidAnalysis extends CompositePlugin {

    private Solver solver;

    private static final List<String> ANDROID_SYSTEM_PACKAGES =
            List.of("com.android.",
                    "com.google.",
                    "android.",
                    "androidx.");

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        AndroidContext androidContext = new AndroidContext(solver);
        addPlugin(
                new LifecycleAnalysis(androidContext),
                new ICCAnalysis(androidContext),
                new AndroidMiscAnalysis(androidContext)
        );
    }

    @Override
    public void onStart() {
        super.onStart();
        solver.getHierarchy().allClasses().forEach(c -> {
            if (isAndroidSystemClass(c.getName())) {
                c.getDeclaredMethods().forEach(solver::addIgnoredMethod);
            }
        });
    }

    public static boolean isAndroidSystemClass(String name) {
        return ANDROID_SYSTEM_PACKAGES.stream().anyMatch(name::startsWith);
    }

}

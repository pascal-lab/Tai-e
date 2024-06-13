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

import java.util.List;

public class AnalysisPreProcess extends AndroidHandler {

    public static final List<String> ANDROID_SYSTEM_PACKAGES =
            List.of("com.android.",
                    "android.",
                    "androidx.");

    public AnalysisPreProcess(AndroidContext androidContext) {
        super(androidContext);
    }

    @Override
    public void onStart() {
        // Avoid analysis crashes caused by analyzing fields not found in Android class
        solver.getHierarchy().allClasses().forEach(c -> {
            if (isAndroidSystemClass(c.getName())) {
                c.getDeclaredMethods().forEach(solver::addIgnoredMethod);
            }
        });
    }

    protected boolean isAndroidSystemClass(String name) {
        return ANDROID_SYSTEM_PACKAGES.stream().anyMatch(name::startsWith);
    }

}

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

package pascal.taie.android.util;

import soot.jimple.infoflow.android.manifest.ProcessManifest;

import java.util.Map;

public final class AndroidJavaVersionInfer {

    private static final int DEFAULT_JAVA_VERSION = 6;

    private static final Map<Integer, Integer> SDK_TO_JDK = Map.ofEntries(
            Map.entry(34, 17),
            Map.entry(33, 11),
            Map.entry(32, 11),
            Map.entry(31, 11),
            Map.entry(30, 8),
            Map.entry(29, 8),
            Map.entry(28, 8),
            Map.entry(27, 8),
            Map.entry(26, 8),
            Map.entry(25, 8),
            Map.entry(24, 8),
            Map.entry(23, 7),
            Map.entry(22, 7),
            Map.entry(21, 6),
            Map.entry(20, 6),
            Map.entry(19, 6),
            Map.entry(18, 6),
            Map.entry(17, 6),
            Map.entry(16, 6),
            Map.entry(15, 6)
    );

    private AndroidJavaVersionInfer() {
    }

    public static int inferFromApk(String apkPath) {
        try (ProcessManifest manifest = new ProcessManifest(apkPath)) {
            return inferFromTargetSdk(manifest.getTargetSdkVersion());
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to infer Java version from APK manifest: " + apkPath, e);
        }
    }

    public static int inferFromTargetSdk(int targetSdkVersion) {
        return SDK_TO_JDK.getOrDefault(targetSdkVersion, DEFAULT_JAVA_VERSION);
    }
}
